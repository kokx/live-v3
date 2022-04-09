package org.icpclive.adminapi

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.ObjectSettings
import org.icpclive.api.ObjectStatus
import org.icpclive.api.TypeWithId
import org.icpclive.data.Manager

class Wrapper<SettingsType : ObjectSettings, DataType : TypeWithId>(
    private val createWidget: (SettingsType) -> DataType,
    private var settings: SettingsType,
    private val manager: Manager<DataType>,
    val id: Int? = null
) {
    private val mutex = Mutex()

    private var widgetId: String? = null

    suspend fun getStatus(): ObjectStatus<SettingsType> = mutex.withLock {
        return ObjectStatus(widgetId != null, settings, id)
    }

    //TODO: Use under mutex
    fun getSettings(): SettingsType {
        return settings
    }

    suspend fun set(newSettings: SettingsType) {
        mutex.withLock {
            settings = newSettings
        }
    }

    suspend fun show() {
        mutex.withLock {
            if (widgetId != null)
                return
            val widget = createWidget(settings)
            manager.add(widget)
            widgetId = widget.id
        }
    }

    suspend fun show(newSettings: SettingsType) {
        set(newSettings)
        show()
    }

    suspend fun hide() {
        mutex.withLock {
            widgetId?.let {
                manager.remove(it)
            }
            widgetId = null
        }
    }
}