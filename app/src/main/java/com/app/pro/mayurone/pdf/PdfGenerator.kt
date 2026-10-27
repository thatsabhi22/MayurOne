package com.app.pro.mayurone.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.app.pro.mayurone.data.local.entity.EstimateEntity
import com.app.pro.mayurone.data.local.entity.EstimateItemEntity
import com.app.pro.mayurone.data.local.entity.EstimateRevisionEntity
import com.app.pro.mayurone.data.local.entity.TenantEntity
import com.app.pro.mayurone.data.local.model.CalculationBasis
import com.app.pro.mayurone.domain.CalculationEngine
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateEstimatePdf(
        context: Context,
        tenant: TenantEntity,
        estimate: EstimateEntity,
        revision: EstimateRevisionEntity,
        items: List<EstimateItemEntity>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint()
        val boldPaint = Paint()

        var y = 40f
        val leftMargin = 36f
        val rightMargin = 559f

        // Header Background
        paint.color = Color.parseColor("#1E3A8A") // Deep Blue
        canvas.drawRect(leftMargin, y, rightMargin, y + 60f, paint)

        // Header Text
        titlePaint.color = Color.WHITE
        titlePaint.textSize = 20f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(tenant.businessName, leftMargin + 16f, y + 28f, titlePaint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val subtitle = "${tenant.ownerName} | ${tenant.phone}"
        canvas.drawText(subtitle, leftMargin + 16f, y + 48f, paint)

        y += 75f

        // Document Details Section
        boldPaint.color = Color.BLACK
        boldPaint.textSize = 14f
        boldPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ESTIMATE", leftMargin, y, boldPaint)

        boldPaint.textSize = 10f
        boldPaint.color = Color.DKGRAY
        canvas.drawText("Number: ${estimate.estimateNumber}", rightMargin - 150f, y, boldPaint)

        y += 16f
        paint.color = Color.GRAY
        paint.textSize = 10f
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(revision.createdAt))
        canvas.drawText("Revision #${revision.revisionNumber} (${estimate.status})", leftMargin, y, paint)
        canvas.drawText("Date: $dateStr", rightMargin - 150f, y, paint)

        y += 24f

        // Divider
        paint.strokeWidth = 1f
        paint.color = Color.LTGRAY
        canvas.drawLine(leftMargin, y, rightMargin, y, paint)

        y += 20f

        // Customer Info Card
        paint.color = Color.parseColor("#F3F4F6")
        canvas.drawRoundRect(leftMargin, y, rightMargin, y + 60f, 8f, 8f, paint)

        boldPaint.textSize = 11f
        boldPaint.color = Color.parseColor("#1F2937")
        canvas.drawText("CUSTOMER DETAILS:", leftMargin + 12f, y + 18f, boldPaint)

        paint.textSize = 10f
        paint.color = Color.parseColor("#374151")
        canvas.drawText("Name: ${revision.customerName}", leftMargin + 12f, y + 34f, paint)
        if (revision.customerPhone.isNotBlank()) {
            canvas.drawText("Phone: ${revision.customerPhone}", leftMargin + 200f, y + 34f, paint)
        }
        if (revision.customerAddress.isNotBlank()) {
            canvas.drawText("Address: ${revision.customerAddress}", leftMargin + 12f, y + 48f, paint)
        }

        y += 80f

        // Table Header
        paint.color = Color.parseColor("#E5E7EB")
        canvas.drawRect(leftMargin, y, rightMargin, y + 24f, paint)

        boldPaint.textSize = 10f
        boldPaint.color = Color.BLACK
        canvas.drawText("#", leftMargin + 8f, y + 16f, boldPaint)
        canvas.drawText("Item Description", leftMargin + 30f, y + 16f, boldPaint)
        canvas.drawText("Basis", leftMargin + 250f, y + 16f, boldPaint)
        canvas.drawText("Qty / Wt", leftMargin + 320f, y + 16f, boldPaint)
        canvas.drawText("Rate", leftMargin + 390f, y + 16f, boldPaint)
        canvas.drawText("Amount", leftMargin + 460f, y + 16f, boldPaint)

        y += 24f

        // Table Rows
        paint.textSize = 10f
        paint.color = Color.BLACK

        for ((index, item) in items.withIndex()) {
            val rowY = y + 18f
            canvas.drawText("${index + 1}", leftMargin + 8f, rowY, paint)

            val desc = if (item.description.length > 32) item.description.take(30) + "..." else item.description
            canvas.drawText(desc, leftMargin + 30f, rowY, paint)

            val basisLabel = if (item.calculationBasis == CalculationBasis.QTY_X_RATE) "Qty x Rate" else "Wt x Rate"
            canvas.drawText(basisLabel, leftMargin + 250f, rowY, paint)

            val qtyWtStr = if (item.calculationBasis == CalculationBasis.QTY_X_RATE) "${item.quantity}" else "${item.weight} kg"
            canvas.drawText(qtyWtStr, leftMargin + 320f, rowY, paint)

            canvas.drawText("₹ ${item.rate}", leftMargin + 390f, rowY, paint)
            canvas.drawText("₹ ${item.amount}", leftMargin + 460f, rowY, paint)

            y += 24f
            paint.color = Color.parseColor("#F9FAFB")
            canvas.drawLine(leftMargin, y, rightMargin, y, paint)
            paint.color = Color.BLACK
        }

        y += 20f

        // Totals Box
        paint.color = Color.parseColor("#EFF6FF")
        canvas.drawRoundRect(rightMargin - 220f, y, rightMargin, y + 45f, 6f, 6f, paint)

        boldPaint.textSize = 12f
        boldPaint.color = Color.parseColor("#1E40AF")
        canvas.drawText("TOTAL ESTIMATE:", rightMargin - 210f, y + 28f, boldPaint)
        canvas.drawText(CalculationEngine.formatCurrency(revision.totalAmount, tenant.currencySymbol), rightMargin - 95f, y + 28f, boldPaint)

        y += 65f

        // Notes section if exists
        if (revision.notes.isNotBlank()) {
            boldPaint.textSize = 10f
            boldPaint.color = Color.DKGRAY
            canvas.drawText("Notes & Terms:", leftMargin, y, boldPaint)
            y += 14f
            paint.textSize = 9f
            paint.color = Color.GRAY
            canvas.drawText(revision.notes, leftMargin, y, paint)
        }

        // Footer
        paint.color = Color.LTGRAY
        canvas.drawLine(leftMargin, 800f, rightMargin, 800f, paint)
        paint.textSize = 8f
        paint.color = Color.GRAY
        canvas.drawText("Generated via MayurOne SaaS App - Digital Fabrication Estimate Manager", leftMargin, 815f, paint)

        pdfDocument.finishPage(page)

        val dir = File(context.cacheDir, "estimates")
        if (!dir.exists()) dir.mkdirs()

        val estNumClean = estimate.estimateNumber.trim().replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val rawCustomer = revision.customerName.trim()
        val isCustomCustomer = rawCustomer.isNotBlank() && !rawCustomer.equals("Walk-in Customer", ignoreCase = true)

        val fileName = if (isCustomCustomer) {
            val custNameClean = rawCustomer.replace("[^a-zA-Z0-9_-]".toRegex(), "_").replace("_+".toRegex(), "_")
            "${custNameClean}_${estNumClean}_Rev${revision.revisionNumber}.pdf"
        } else {
            "${estNumClean}_Rev${revision.revisionNumber}.pdf"
        }

        val file = File(dir, fileName)
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.close()
        pdfDocument.close()

        return file
    }

    fun getFileUri(context: Context, file: File): android.net.Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
