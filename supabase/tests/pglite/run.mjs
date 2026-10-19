// Applies every migration to an in-process Postgres (PGlite) with Supabase stubs, then checks
// constraints and row level security as real API roles. Exit code 1 on any failure.
//
//   cd supabase/tests/pglite && npm ci && npm test

import { PGlite } from '@electric-sql/pglite';
import { readdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const migrationsDir = join(here, '..', '..', 'migrations');

const ALICE = '00000000-0000-4000-8000-00000000000a';
const BOB = '00000000-0000-4000-8000-00000000000b';

const db = new PGlite();
let failures = 0;
let passes = 0;

function report(ok, name, detail = '') {
  if (ok) {
    passes++;
    console.log(`  ok    ${name}`);
  } else {
    failures++;
    console.log(`  FAIL  ${name}${detail ? ` — ${detail}` : ''}`);
  }
}

/** Runs [fn] as an API role. userId = null means the anon role. */
async function as(userId, fn) {
  if (userId) {
    const claims = JSON.stringify({ sub: userId, role: 'authenticated' });
    await db.query(`select set_config('request.jwt.claims', $1, false)`, [claims]);
    await db.exec('set role authenticated');
  } else {
    await db.query(`select set_config('request.jwt.claims', '', false)`);
    await db.exec('set role anon');
  }
  try {
    return await fn();
  } finally {
    await db.exec('reset role');
  }
}

async function expectRows(name, userId, sql, params, expected) {
  try {
    const { rows } = await as(userId, () => db.query(sql, params));
    report(rows.length === expected, name, `expected ${expected} row(s), got ${rows.length}`);
    return rows;
  } catch (e) {
    report(false, name, `unexpected error ${e.code}: ${e.message}`);
    return [];
  }
}

async function expectError(name, userId, sql, params, code) {
  try {
    await as(userId, () => db.query(sql, params));
    report(false, name, `expected error ${code}, but it succeeded`);
  } catch (e) {
    report(e.code === code, name, `expected ${code}, got ${e.code}: ${e.message}`);
  }
}

async function expectValue(name, userId, sql, params, expected) {
  try {
    const { rows } = await as(userId, () => db.query(sql, params));
    const actual = rows[0] ? Object.values(rows[0])[0] : undefined;
    report(actual === expected, name, `expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
  } catch (e) {
    report(false, name, `unexpected error ${e.code}: ${e.message}`);
  }
}

// ── Setup ──────────────────────────────────────────────────────────────
await db.exec(readFileSync(join(here, 'supabase_stubs.sql'), 'utf8'));
const migrations = readdirSync(migrationsDir).filter((f) => f.endsWith('.sql')).sort();
for (const file of migrations) {
  await db.exec(readFileSync(join(migrationsDir, file), 'utf8'));
  console.log(`applied ${file}`);
}
await db.query(`insert into auth.users (id, email) values ($1, 'alice@example.com'), ($2, 'bob@example.com')`, [ALICE, BOB]);

// ── New users ──────────────────────────────────────────────────────────
console.log('\nnew users');
{
  const { rows } = await db.query('select count(*)::int as n from public.profiles');
  report(rows[0].n === 2, 'trigger creates a profile per user');
  const settings = await db.query('select count(*)::int as n from public.user_privacy_settings');
  report(settings.rows[0].n === 2, 'trigger creates privacy settings per user');
}

// ── Profiles: visibility and ownership ────────────────────────────────
console.log('\nprofiles');
await expectRows('user sees only their own profile', ALICE, 'select id from public.profiles', [], 1);
await expectError('anon cannot read profiles', null, 'select id from public.profiles', [], '42501');
await expectRows('user can set their own username', ALICE,
  `update public.profiles set username = 'alice', display_name = 'Alice' where id = $1 returning id`, [ALICE], 1);
await expectRows("user can't update someone else's profile", ALICE,
  `update public.profiles set username = 'mallory' where id = $1 returning id`, [BOB], 0);
await expectError("user can't change their profile id", ALICE,
  `update public.profiles set id = gen_random_uuid() where id = $1`, [ALICE], '42501');
await expectError("user can't change timestamps", ALICE,
  `update public.profiles set created_at = now() where id = $1`, [ALICE], '42501');
await expectError("user can't insert profiles", ALICE,
  `insert into public.profiles (id) values (gen_random_uuid())`, [], '42501');
await expectError("user can't delete profiles", ALICE,
  `delete from public.profiles where id = $1`, [ALICE], '42501');

// ── Username rules ────────────────────────────────────────────────────
console.log('\nusername rules');
await expectError('uppercase is rejected', ALICE, `update public.profiles set username = 'Alice' where id = $1`, [ALICE], '23514');
await expectError('too short is rejected', ALICE, `update public.profiles set username = 'al' where id = $1`, [ALICE], '23514');
await expectError('consecutive periods are rejected', ALICE, `update public.profiles set username = 'a..b' where id = $1`, [ALICE], '23514');
await expectError('leading underscore is rejected', ALICE, `update public.profiles set username = '_alice' where id = $1`, [ALICE], '23514');
await expectError('reserved name is rejected with RC001', ALICE, `update public.profiles set username = 'admin' where id = $1`, [ALICE], 'RC001');
await expectError('taken name is rejected with 23505', BOB, `update public.profiles set username = 'alice' where id = $1`, [BOB], '23505');
await expectRows('valid name with period and underscore is accepted', BOB,
  `update public.profiles set username = 'bob.the_builder' where id = $1 returning id`, [BOB], 1);

// ── check_username RPC ────────────────────────────────────────────────
console.log('\ncheck_username');
await expectValue("someone else's username is taken", BOB, `select public.check_username('alice')`, [], 'taken');
await expectValue('your own username is available to you', ALICE, `select public.check_username('alice')`, [], 'available');
await expectValue('reserved names are reported', ALICE, `select public.check_username('support')`, [], 'reserved');
await expectValue('free names are available', ALICE, `select public.check_username('carol_99')`, [], 'available');
await expectValue('malformed names are invalid', ALICE, `select public.check_username('Carol')`, [], 'invalid');
await expectError('anon cannot call check_username', null, `select public.check_username('alice')`, [], '42501');

// ── Other profile fields ──────────────────────────────────────────────
console.log('\nprofile fields');
await expectRows('avatar in your own folder is accepted', ALICE,
  `update public.profiles set avatar_path = $1 || '/a.webp' where id = $1 returning id`, [ALICE], 1);
await expectError("avatar in someone else's folder is rejected", ALICE,
  `update public.profiles set avatar_path = $2 || '/a.webp' where id = $1`, [ALICE, BOB], '23514');
await expectError('display name with surrounding spaces is rejected', ALICE,
  `update public.profiles set display_name = ' Alice ' where id = $1`, [ALICE], '23514');
await expectError('display name over 50 chars is rejected', ALICE,
  `update public.profiles set display_name = repeat('a', 51) where id = $1`, [ALICE], '23514');
await expectError('bio over 160 chars is rejected', ALICE,
  `update public.profiles set bio = repeat('b', 161) where id = $1`, [ALICE], '23514');
await expectRows('bio of 160 chars is accepted', ALICE,
  `update public.profiles set bio = repeat('b', 160) where id = $1 returning id`, [ALICE], 1);

// ── Privacy settings ──────────────────────────────────────────────────
console.log('\nprivacy settings');
await expectRows('user sees only their own settings', ALICE, 'select user_id from public.user_privacy_settings', [], 1);
await expectRows('user can update their own settings', ALICE,
  `update public.user_privacy_settings set stealth_mode = true where user_id = $1 returning user_id`, [ALICE], 1);
await expectRows("user can't update someone else's settings", ALICE,
  `update public.user_privacy_settings set stealth_mode = true where user_id = $1 returning user_id`, [BOB], 0);
await expectError("user can't insert settings rows", ALICE,
  `insert into public.user_privacy_settings (user_id) values ($1)`, [ALICE], '42501');
await expectError("user can't read the reserved-name list", ALICE,
  'select name from private.reserved_usernames', [], '42501');

// ── Avatar storage ────────────────────────────────────────────────────
console.log('\navatar storage');
await expectRows('upload into your own folder is allowed', ALICE,
  `insert into storage.objects (bucket_id, name) values ('avatars', $1 || '/x.webp') returning id`, [ALICE], 1);
await expectError("upload into someone else's folder is denied", ALICE,
  `insert into storage.objects (bucket_id, name) values ('avatars', $1 || '/x.webp')`, [BOB], '42501');
await expectRows("user can't delete someone else's avatar", BOB,
  `delete from storage.objects where bucket_id = 'avatars' and name = $1 || '/x.webp' returning id`, [ALICE], 0);
await expectRows('user can delete their own avatar', ALICE,
  `delete from storage.objects where bucket_id = 'avatars' and name = $1 || '/x.webp' returning id`, [ALICE], 1);

// ── Account deletion cascades ─────────────────────────────────────────
console.log('\naccount deletion');
await db.query('delete from auth.users where id = $1', [ALICE]);
{
  const p = await db.query('select count(*)::int as n from public.profiles where id = $1', [ALICE]);
  const s = await db.query('select count(*)::int as n from public.user_privacy_settings where user_id = $1', [ALICE]);
  report(p.rows[0].n === 0 && s.rows[0].n === 0, 'deleting the auth user removes profile and settings');
}

console.log(`\n${passes} passed, ${failures} failed`);
await db.close();
process.exit(failures === 0 ? 0 : 1);
