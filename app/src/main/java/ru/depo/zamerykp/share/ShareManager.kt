package ru.depo.zamerykp.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class ShareManager(private val context: Context) {
    fun shareJson(fileName: String, json: String) {
        shareTextFile(
            fileName = fileName,
            text = json,
            mimeType = "application/json",
            subject = "Замеры колесных пар",
            chooserTitle = "Отправить файл замеров"
        )
    }

    fun shareBackup(fileName: String, json: String) {
        shareTextFile(
            fileName = fileName,
            text = json,
            mimeType = "application/json",
            subject = "Резервная копия Замеры КП",
            chooserTitle = "Отправить резервную копию"
        )
    }

    private fun shareTextFile(
        fileName: String,
        text: String,
        mimeType: String,
        subject: String,
        chooserTitle: String,
    ) {
        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(dir, fileName)
        file.writeText(text, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
