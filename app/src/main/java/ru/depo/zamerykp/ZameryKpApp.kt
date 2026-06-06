package ru.depo.zamerykp

import android.app.Application
import ru.depo.zamerykp.data.db.AppDatabase
import ru.depo.zamerykp.data.db.DatabaseSeeder
import ru.depo.zamerykp.data.repository.BackupRepository
import ru.depo.zamerykp.data.repository.ExportRepository
import ru.depo.zamerykp.data.repository.LocomotiveRepository
import ru.depo.zamerykp.data.repository.MeasurementRepository
import ru.depo.zamerykp.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ZameryKpApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.create(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            DatabaseSeeder(db).seedIfEmpty()
        }
        container = AppContainer(
            locomotiveRepository = LocomotiveRepository(db.locomotiveDao(), db.wheelPairProfileDao()),
            measurementRepository = MeasurementRepository(db.measurementDao(), db.locomotiveDao(), db.wheelPairProfileDao()),
            exportRepository = ExportRepository(db.measurementDao(), db.locomotiveDao(), db.wheelPairProfileDao()),
            backupRepository = BackupRepository(db),
            settingsRepository = SettingsRepository(db.settingsDao()),
        )
    }
}

data class AppContainer(
    val locomotiveRepository: LocomotiveRepository,
    val measurementRepository: MeasurementRepository,
    val exportRepository: ExportRepository,
    val backupRepository: BackupRepository,
    val settingsRepository: SettingsRepository,
)
