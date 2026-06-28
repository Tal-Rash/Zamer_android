package ru.depo.zamerykp.voice

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VoskModelStore(private val context: Context) {
    suspend fun prepareModel(): String? = withContext(Dispatchers.IO) {
        val targetDir = File(context.filesDir, TARGET_DIR_NAME)
        val markerFile = File(targetDir, MARKER_FILE_NAME)

        if (targetDir.isDirectory && markerFile.exists()) {
            val marker = runCatching { markerFile.readText() }.getOrNull().orEmpty()
            if (marker == MODEL_ASSET_DIR) {
                return@withContext targetDir.absolutePath
            }
        }

        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        targetDir.mkdirs()
        copyAssetTree(MODEL_ASSET_DIR, targetDir)
        markerFile.writeText(MODEL_ASSET_DIR)
        targetDir.absolutePath
    }

    private fun copyAssetTree(assetPath: String, targetDir: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) return

        children.forEach { child ->
            val childAssetPath = "$assetPath/$child"
            val childTarget = File(targetDir, child)
            val nestedChildren = context.assets.list(childAssetPath).orEmpty()
            if (nestedChildren.isEmpty()) {
                context.assets.open(childAssetPath).use { input ->
                    childTarget.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                childTarget.mkdirs()
                copyAssetTree(childAssetPath, childTarget)
            }
        }
    }

    private companion object {
        const val MODEL_ASSET_DIR = "vosk-model-small-ru-0.22"
        const val TARGET_DIR_NAME = "vosk-model"
        const val MARKER_FILE_NAME = "bundled.version"
    }
}
