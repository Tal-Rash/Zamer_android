package ru.depo.zamerykp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.depo.zamerykp.data.db.AppSettingsEntity
import ru.depo.zamerykp.data.db.SettingsDao

class SettingsRepository(private val settingsDao: SettingsDao) {
    fun observe(): Flow<AppSettingsEntity> =
        settingsDao.observe().map { it ?: AppSettingsEntity() }

    suspend fun save(settings: AppSettingsEntity) {
        settingsDao.upsert(settings)
    }

    suspend fun updateVoskModelUri(uri: String) {
        val current = settingsDao.get() ?: AppSettingsEntity()
        settingsDao.upsert(current.copy(voskModelUri = uri))
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        val current = settingsDao.get() ?: AppSettingsEntity()
        settingsDao.upsert(current.copy(keepScreenOn = enabled))
    }

    suspend fun updateSyncServerUrl(url: String) {
        val current = settingsDao.get() ?: AppSettingsEntity()
        settingsDao.upsert(current.copy(syncServerUrl = url))
    }

    suspend fun updateSyncPassword(password: String) {
        val current = settingsDao.get() ?: AppSettingsEntity()
        settingsDao.upsert(current.copy(syncPassword = password))
    }
}
