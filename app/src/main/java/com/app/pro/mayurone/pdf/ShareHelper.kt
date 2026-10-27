package com.app.pro.mayurone.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareHelper {

    fun sharePdf(context: Context, fileUri: Uri, targetWhatsApp: Boolean = false) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (targetWhatsApp) {
                setPackage("com.whatsapp")
            }
        }

        val chooser = if (targetWhatsApp) {
            shareIntent
        } else {
            Intent.createChooser(shareIntent, "Share Estimate Document")
        }

        runCatching {
            context.startActivity(chooser)
        }.onFailure {
            // Fallback to chooser if WhatsApp target application fails
            if (targetWhatsApp) {
                val fallbackIntent = Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Share Estimate Document"
                )
                context.startActivity(fallbackIntent)
            }
        }
    }
}
