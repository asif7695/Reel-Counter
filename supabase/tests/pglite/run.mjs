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

// ── Phase 5: devices and activity uploads ─────────────────────────────
// Fresh users so earlier tests (e.g. Alice's stealth mode) don't interfere.
const CAROL = '00000000-0000-4000-8000-00000000000c';
const DAVE = '00000000-0000-4000-8000-00000000000d';
const ERIN = '00000000-0000-4000-8000-00000000000e';
const CAROL_PHONE = '10000000-0000-4000-8000-00000000000c';
const DAVE_PHONE = '10000000-0000-4000-8000-00000000000d';
await db.query(
  `insert into auth.users (id, email) values ($1, 'carol@example.com'), ($2, 'dave@example.com'), ($3, 'erin@example.com')`,
  [CAROL, DAVE, ERIN],
);

const today = new Date().toISOString().slice(0, 10);
const upsert = `
  insert into public.daily_activity_summaries
    (user_id, device_id, platform_id, activity_date, tz_id, foreground_ms, session_count,
     estimated_reels, estimated_reels_low, estimated_reels_high, quality, source, estimation_version, revision)
  values ($1, $2, $3, $4::date, 'Europe/Berlin', $5, 3, $6, $7, $8, $9::public.metric_quality, $10::public.activity_source, 1, $11)
  on conflict (user_id, device_id, platform_id, activity_date)
  do update set foreground_ms = excluded.foreground_ms, estimated_reels = excluded.estimated_reels,
                estimated_reels_low = excluded.estimated_reels_low, estimated_reels_high = excluded.estimated_reels_high,
                revision = excluded.revision
  returning id`;
const row = (user, device, { platform = 'tiktok', date = today, ms = 600000, est = 27, low = 14, high = 48, quality = 'ESTIMATED', source = 'usage_stats', revision = 1 } = {}) =>
  [user, device, platform, date, ms, est, low, high, quality, source, revision];

console.log('\ndevices');
await expectRows('a user registers their own device', CAROL,
  `insert into public.devices (id, user_id) values ($1, $2) returning id`, [CAROL_PHONE, CAROL], 1);
await expectRows('another user registers theirs', DAVE,
  `insert into public.devices (id, user_id) values ($1, $2) returning id`, [DAVE_PHONE, DAVE], 1);
await expectError("a device can't be registered for someone else", CAROL,
  `insert into public.devices (id, user_id) values (gen_random_uuid(), $1)`, [DAVE], '42501');
await expectError("a device can't be handed to another user", CAROL,
  `update public.devices set user_id = $2 where id = $1`, [CAROL_PHONE, DAVE], '42501');
await expectRows("users can't see each other's devices", DAVE, 'select id from public.devices', [], 1);

console.log('\nactivity uploads');
await expectRows('own estimated row from own device is accepted', CAROL, upsert, row(CAROL, CAROL_PHONE), 1);
await expectError("uploading from someone else's device is denied", CAROL, upsert, row(CAROL, DAVE_PHONE, { platform: 'instagram' }), '42501');
await expectError("uploading as someone else is denied", CAROL, upsert, row(DAVE, DAVE_PHONE), '42501');
await expectError('clients cannot claim VERIFIED', CAROL, upsert,
  row(CAROL, CAROL_PHONE, { platform: 'youtube', quality: 'VERIFIED', est: null, low: null, high: null }), '42501');
await expectError('clients cannot write data-portability rows', CAROL, upsert,
  row(CAROL, CAROL_PHONE, { platform: 'facebook', source: 'data_portability' }), '42501');
await expectError('ESTIMATED without its range is rejected', CAROL, upsert,
  row(CAROL, CAROL_PHONE, { platform: 'instagram', low: null }), '23514');
await expectError('more than 25 hours in a day is rejected', CAROL, upsert,
  row(CAROL, CAROL_PHONE, { platform: 'instagram', ms: 25 * 3600000 + 1 }), '23514');
await expectError('an implausible date is rejected', CAROL, upsert,
  row(CAROL, CAROL_PHONE, { platform: 'instagram', date: '2019-12-31' }), '23514');

await expectRows('a newer revision updates the row', CAROL, upsert, row(CAROL, CAROL_PHONE, { ms: 900000, est: 41, low: 21, high: 72, revision: 2 }), 1);
await as(CAROL, () => db.query(upsert, row(CAROL, CAROL_PHONE, { ms: 1, est: 0, low: 0, high: 0, revision: 1 })));
await expectValue('a stale revision is silently ignored', CAROL,
  `select foreground_ms::int from public.daily_activity_summaries where user_id = $1 and platform_id = 'tiktok'`, [CAROL], 900000);
await expectError('key columns are immutable', CAROL,
  `update public.daily_activity_summaries set activity_date = activity_date - 1, revision = 99 where user_id = $1`, [CAROL], '23514');

// ── Phase 5: who can see what ─────────────────────────────────────────
console.log('\nvisibility');
const carolActivity = `select id from public.daily_activity_summaries where user_id = '${CAROL}'`;
const carolProfile = `select id from public.profiles where id = '${CAROL}'`;
await expectRows('a stranger sees no activity', DAVE, carolActivity, [], 0);
await expectRows("a stranger can't see a friends-only profile", DAVE, carolProfile, [], 0);

await db.query(
  `insert into public.friendships (user_low, user_high, requested_by, status)
   values (least($1::uuid, $2::uuid), greatest($1::uuid, $2::uuid), $1, 'accepted')`, [CAROL, DAVE]);
await expectRows('an accepted friend sees activity', DAVE, carolActivity, [], 1);
await expectRows('an accepted friend sees the profile', DAVE, carolProfile, [], 1);
await expectRows('an accepted friend sees per-user totals', DAVE,
  `select user_id from public.daily_activity_totals where user_id = '${CAROL}'`, [], 1);
await expectRows('a third person still sees nothing', ERIN, carolActivity, [], 0);

await as(CAROL, () => db.query(
  `insert into public.user_platform_settings (user_id, platform_id, visibility) values ($1, 'tiktok', 'private')`, [CAROL]));
await expectRows('a private platform is hidden even from friends', DAVE, carolActivity, [], 0);
await as(CAROL, () => db.query(`update public.user_platform_settings set visibility = 'friends' where user_id = $1`, [CAROL]));
await expectRows('making it friends-only again shows it', DAVE, carolActivity, [], 1);

await as(CAROL, () => db.query(`update public.user_privacy_settings set activity_visibility = 'private' where user_id = $1`, [CAROL]));
await expectRows('private account-level activity hides everything', DAVE, carolActivity, [], 0);
await as(CAROL, () => db.query(`update public.user_privacy_settings set activity_visibility = 'friends', stealth_mode = true where user_id = $1`, [CAROL]));
await expectRows('stealth mode hides activity from friends', DAVE, carolActivity, [], 0);
await expectRows("friends can't read whether stealth is on", DAVE,
  `select user_id from public.user_privacy_settings where user_id = '${CAROL}'`, [], 0);

await as(CAROL, () => db.query(
  `update public.user_privacy_settings set stealth_mode = false, activity_visibility = 'public', profile_visibility = 'public' where user_id = $1`, [CAROL]));
await as(CAROL, () => db.query(`update public.user_platform_settings set visibility = 'public' where user_id = $1`, [CAROL]));
await expectRows('public activity is visible to strangers', ERIN, carolActivity, [], 1);
await expectRows('a public profile is visible to strangers', ERIN, carolProfile, [], 1);

await db.query(`insert into public.user_blocks (blocker_id, blocked_id) values ($1, $2)`, [CAROL, ERIN]);
await expectRows('a blocked user sees nothing, even public activity', ERIN, carolActivity, [], 0);
await expectRows('a blocked user cannot see the profile', ERIN, carolProfile, [], 0);
await expectRows("the blocked user can't see the block", ERIN, 'select blocker_id from public.user_blocks', [], 0);
await expectRows('the blocker sees their block', CAROL, 'select blocked_id from public.user_blocks', [], 1);

console.log('\nsocial graph and settings');
await expectRows('both friends see their friendship', CAROL, 'select id from public.friendships', [], 1);
await expectRows('outsiders do not', ERIN, 'select id from public.friendships', [], 0);
await expectError("friendships can't be created directly (Phase 6 RPCs only)", ERIN,
  `insert into public.friendships (user_low, user_high, requested_by) values (least($1::uuid, $2::uuid), greatest($1::uuid, $2::uuid), $1)`,
  [ERIN, DAVE], '42501');
await db.query(
  `insert into public.friendships (user_low, user_high, requested_by) values (least($1::uuid, $2::uuid), greatest($1::uuid, $2::uuid), $1)`,
  [ERIN, DAVE]);
await expectRows('a pending request reveals the profile to its counterpart', ERIN,
  `select id from public.profiles where id = '${DAVE}'`, [], 1);
await expectRows('but not their activity', ERIN, `select id from public.daily_activity_summaries where user_id = '${DAVE}'`, [], 0);
await expectRows("platform settings are private to their owner", DAVE,
  `select platform_id from public.user_platform_settings where user_id = '${CAROL}'`, [], 0);
await expectRows('reference platforms are readable', ERIN, 'select id from public.platforms', [], 4);
await expectError('reference data is read-only', ERIN,
  `insert into public.platforms (id, display_name, android_packages, default_quality) values ('x', 'X', '{}', 'ESTIMATED')`, [], '42501');
await expectError('anon cannot read activity', null, 'select id from public.daily_activity_summaries', [], '42501');

// ── Account deletion cascades ─────────────────────────────────────────
console.log('\naccount deletion');
await db.query('delete from auth.users where id = $1', [CAROL]);
{
  const devices = await db.query('select count(*)::int as n from public.devices where user_id = $1', [CAROL]);
  const activity = await db.query('select count(*)::int as n from public.daily_activity_summaries where user_id = $1', [CAROL]);
  const graph = await db.query('select count(*)::int as n from public.friendships where $1 in (user_low, user_high)', [CAROL]);
  report(devices.rows[0].n === 0 && activity.rows[0].n === 0 && graph.rows[0].n === 0,
    'deleting a user removes their devices, activity and friendships');
}
await db.query('delete from auth.users where id = $1', [ALICE]);
{
  const p = await db.query('select count(*)::int as n from public.profiles where id = $1', [ALICE]);
  const s = await db.query('select count(*)::int as n from public.user_privacy_settings where user_id = $1', [ALICE]);
  report(p.rows[0].n === 0 && s.rows[0].n === 0, 'deleting the auth user removes profile and settings');
}

console.log(`\n${passes} passed, ${failures} failed`);
await db.close();
process.exit(failures === 0 ? 0 : 1);
