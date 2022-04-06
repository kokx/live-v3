package org.icpclive.adminapi

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import org.icpclive.api.*
import org.icpclive.data.Manager
import org.icpclive.data.TickerManager
import org.icpclive.data.WidgetManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private val jsonPrettyEncoder = Json { prettyPrint = true }

class PresetsManager<SettingsType : ObjectSettings, WidgetType : TypeWithId>(
    private val path: String,
    settingsSerializer: KSerializer<SettingsType>,
    private val createWidget: (SettingsType) -> WidgetType,
    private val manager: Manager<WidgetType>,
) {
    private val mutex = Mutex()
    private val serializer = ListSerializer(settingsSerializer)
    private var innerData = load()
    private var currentID = innerData.size

    suspend fun getStatus(): List<ObjectStatus<SettingsType>> = mutex.withLock {
        return innerData.map { it.getStatus() }
    }

    suspend fun append(settings: SettingsType) {
        mutex.withLock {
            innerData = innerData.plus(Wrapper(createWidget, settings, manager, ++currentID))
        }
        save()
    }

    suspend fun edit(id: Int, content: SettingsType) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id == id)
                    preset.set(content)
            }
        }
        save()
    }

    suspend fun delete(id: Int) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id != id)
                    continue
                preset.hide()
            }
            innerData = innerData.filterNot { it.id == id }
        }
        save()
    }

    suspend fun show(id: Int) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id != id)
                    continue
                preset.show()
                break
            }
        }
    }

    suspend fun hide(id: Int) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id != id)
                    continue
                preset.hide()
            }
        }
    }

    private fun load() = Json.decodeFromStream(serializer, FileInputStream(File(path))).mapIndexed { index, content ->
        Wrapper(createWidget, content, manager, index + 1)
    }

    private suspend fun save() {
        mutex.withLock {
            jsonPrettyEncoder.encodeToStream(serializer, innerData.map { it.getSettings() }, FileOutputStream(File(path)))
        }
    }
}


inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> widgetPresets(
    path: String,
    noinline createWidget: (SettingsType) -> WidgetType
) = PresetsManager(
        path,
        serializer(),
        createWidget,
        WidgetManager
    )

fun tickerPresets(
    path: String,
    createMessage: (TickerMessageSettings) -> TickerMessage
) = PresetsManager(
        path,
        serializer(),
        createMessage,
        TickerManager
    )