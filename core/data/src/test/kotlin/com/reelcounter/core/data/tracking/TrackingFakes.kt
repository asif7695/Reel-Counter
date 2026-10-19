package com.reelcounter.core.data.tracking

import com.reelcounter.core.datastore.TrackingPreferences
import com.reelcounter.core.datastore.TrackingPreferencesStore
import com.reelcounter.core.domain.tracking.CURRENT_DISCLOSURE_VERSION
import com.reelcounter.core.domain.tracking.InstalledApps
import com.reelcounter.core.domain.tracking.TimeSource
import com.reelcounter.core.domain.tracking.UsageAccessMonitor
import com.reelcounter.core.domain.tracking.UsageEvent
import com.reelcounter.core.domain.tracking.UsageEventSource
import com.reelcounter.core.domain.tracking.UsageSession
import com.reelcounter.core.domain.tracking.ZoneChange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

/** In-memory TrackingLocalStore with the same semantics as the Room implementation. */
internal class FakeTrackingLocalStore : TrackingLocalStore {
    val sessions = mutableMapOf<Pair<String, Long>, UsageSession>()
    val rows = MutableStateFlow<Map<Pair<LocalDate, String>, DailyRow>>(emptyMap())
    val coverage = mutableMapOf<LocalDate, DayCoverage>()
    val settings = MutableStateFlow<Map<String, PlatformSettings>>(emptyMap())
    val zones = mutableListOf<ZoneChange>()
    var state: IngestState? = null
    private val lastSuccess = MutableStateFlow<Long?>(null)

    override suspend fun <T> transaction(block: suspend () -> T): T = block()

    override suspend fun ingestState() = state

    override suspend fun saveIngestState(state: IngestState) {
        this.state = state
        lastSuccess.value = state.lastSuccessMs
    }

    override fun observeLastSuccess(): Flow<Long?> = lastSuccess

    override suspend fun earliestOpenSessionStart() = sessions.values.filter { it.isOpen }.minOfOrNull { it.startMs }

    override suspend fun replaceSessionsFrom(fromMs: Long, sessions: List<UsageSession>) {
        this.sessions.keys.removeAll { it.second >= fromMs }
        sessions.forEach { this.sessions[it.packageName to it.startMs] = it }
    }

    override suspend fun sessionsOverlapping(fromMs: Long, toMs: Long) =
        sessions.values.filter { it.endMs > fromMs && it.startMs < toMs }.sortedBy { it.startMs }

    override suspend fun closeOpenSessions() {
        sessions.replaceAll { _, s -> s.copy(isOpen = false) }
    }

    override suspend fun pruneSessionsEndedBefore(beforeMs: Long) {
        sessions.values.removeAll { it.endMs < beforeMs }
    }

    override suspend fun zoneChanges() = zones.toList()

    override suspend fun latestZone() = zones.maxByOrNull { it.atMs }

    override suspend fun recordZone(change: ZoneChange) {
        zones += change
    }

    override suspend fun dailyRows(dates: Collection<LocalDate>) = rows.value.values.filter { it.date in dates }

    override suspend fun nonFinalRows() = rows.value.values.filterNot { it.isFinal }

    override suspend fun saveDailyRows(rows: List<DailyRow>) {
        this.rows.value = this.rows.value + rows.associateBy { it.date to it.platformId }
    }

    override suspend fun finalizeBefore(date: LocalDate, revision: Long) {
        rows.value = rows.value.mapValues { (_, row) ->
            if (row.date < date && !row.isFinal) row.copy(isFinal = true, dirty = true, revision = revision) else row
        }
    }

    override fun observeDay(date: LocalDate): Flow<List<DailyRow>> = rows.map { all -> all.values.filter { it.date == date } }

    override suspend fun coverage(dates: Collection<LocalDate>) = coverage.values.filter { it.date in dates }

    override suspend fun saveCoverage(rows: List<DayCoverage>) {
        rows.forEach { coverage[it.date] = it }
    }

    override suspend fun platformSettings() = settings.value.values.toList()

    override suspend fun savePlatformSettings(settings: PlatformSettings, atMs: Long) {
        this.settings.value = this.settings.value + (settings.platformId to settings)
    }

    override fun observePlatformSettings(): Flow<List<PlatformSettings>> = settings.map { it.values.toList() }

    fun row(date: LocalDate, platformId: String): DailyRow? = rows.value[date to platformId]
}

internal class FakeUsageEventSource : UsageEventSource {
    /** The whole "system log"; queries return the slice they ask for, minus anything expired. */
    val log = mutableListOf<UsageEvent>()

    /** Events before this are gone, as when the system's retention window has passed. */
    var retainedFromMs = Long.MIN_VALUE
    var unlocked = true
    val queries = mutableListOf<LongRange>()

    override suspend fun query(fromMs: Long, toMs: Long): List<UsageEvent> {
        queries += fromMs..toMs
        return log.filter { it.timestampMs in fromMs until toMs && it.timestampMs >= retainedFromMs }
    }

    override fun isUserUnlocked() = unlocked
}

internal class FakeUsageAccess(granted: Boolean = true) : UsageAccessMonitor {
    val granted = MutableStateFlow(granted)
    override val isGranted: Flow<Boolean> = this.granted
    override fun isGrantedNow() = granted.value
}

internal class FakeTrackingPreferences(
    initial: TrackingPreferences = TrackingPreferences(disclosureAcceptedVersion = CURRENT_DISCLOSURE_VERSION),
) : TrackingPreferencesStore {
    val state = MutableStateFlow(initial)
    override val preferences: Flow<TrackingPreferences> = state

    override suspend fun acceptDisclosure(version: Int, atMs: Long) {
        state.value = state.value.copy(disclosureAcceptedVersion = version, disclosureAcceptedAtMs = atMs)
    }

    override suspend fun setPaused(paused: Boolean, atMs: Long) {
        state.value = state.value.copy(paused = paused, resumedAtMs = if (paused) state.value.resumedAtMs else atMs)
    }
}

internal class FakeTimeSource(var nowMs: Long, var zone: ZoneId) : TimeSource {
    override fun nowMs() = nowMs
    override fun zone() = zone
}

internal class FakeInstalledApps(private val installed: Boolean = true) : InstalledApps {
    override fun isAnyInstalled(packages: Set<String>) = installed
}
