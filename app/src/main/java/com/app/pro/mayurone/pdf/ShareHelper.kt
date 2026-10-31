package com.app.pro.mayurone.pdf

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ShareHelper {

    fun sharePdf(context: Context, fileUri: Uri, targetWhatsApp: Boolean = false) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            clipData = ClipData.newRawUri("Estimate PDF", fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (targetWhatsApp) {
            val pm = context.packageManager
            val whatsappPackage = try {
                pm.getPackageInfo("com.whatsapp", 0)
                "com.whatsapp"
            } catch (_: Exception) {
                try {
                    pm.getPackageInfo("com.whatsapp.w4b", 0)
                    "com.whatsapp.w4b"
                } catch (_: Exception) {
                    null
                }
            }

            if (whatsappPackage != null) {
                val waIntent = Intent(shareIntent).apply {
                    setPackage(whatsappPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(waIntent)
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val chooser = Intent.createChooser(shareIntent, "Share Estimate Document").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to share PDF document", Toast.LENGTH_SHORT).show()
        }
    }
}

