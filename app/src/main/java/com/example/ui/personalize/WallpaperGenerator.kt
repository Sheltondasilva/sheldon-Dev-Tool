package com.example.ui.personalize

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class WallpaperStyle(val title: String, val subtitle: String) {
    MATRIX_RAIN("Matrix Digital Rain", "Cascading binary streams on terminal black"),
    CIRCUIT_BOARD("Cyber Circuit Board", "Glowing PCB tracks and semiconductor nodes"),
    DEV_TERMINAL("Hacker Terminal Prompt", "Realistic bash console with active dev stats"),
    HEX_GRID("Cyberpunk Hex Grid", "Glowing geometric digital honeycomb mesh"),
    MINIMAL_CODE("Minimal Monospace", "Clean dark aesthetic with developer syntax")
}

object WallpaperGenerator {

    suspend fun generateWallpaperBitmap(
        style: WallpaperStyle,
        width: Int = 1080,
        height: Int = 2400
    ): Bitmap = withContext(Dispatchers.Default) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (style) {
            WallpaperStyle.MATRIX_RAIN -> drawMatrixRain(canvas, width, height)
            WallpaperStyle.CIRCUIT_BOARD -> drawCircuitBoard(canvas, width, height)
            WallpaperStyle.DEV_TERMINAL -> drawTerminal(canvas, width, height)
            WallpaperStyle.HEX_GRID -> drawHexGrid(canvas, width, height)
            WallpaperStyle.MINIMAL_CODE -> drawMinimalCode(canvas, width, height)
        }

        bitmap
    }

    private fun drawMatrixRain(canvas: Canvas, w: Int, h: Int) {
        // Deep background
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#040B06") }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val charPaint = Paint().apply {
            textSize = 34f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }

        val chars = "01アイウエオカキクケコサシスセソタチツテト010110<>{}/*="
        val columns = w / 40

        for (col in 0 until columns) {
            val startY = (Math.random() * h).toFloat()
            val streamLen = (15..35).random()
            val x = col * 40f + 10f

            for (i in 0 until streamLen) {
                val y = (startY + i * 44f) % h
                val alpha = ((streamLen - i).toFloat() / streamLen * 255).toInt().coerceIn(40, 255)
                val c = chars[(Math.random() * chars.length).toInt()]

                if (i == 0) {
                    charPaint.color = android.graphics.Color.WHITE
                    charPaint.setShadowLayer(14f, 0f, 0f, android.graphics.Color.parseColor("#00FF66"))
                } else {
                    charPaint.color = android.graphics.Color.argb(alpha, 0, 255, 100)
                    charPaint.clearShadowLayer()
                }
                canvas.drawText(c.toString(), x, y, charPaint)
            }
        }
    }

    private fun drawCircuitBoard(canvas: Canvas, w: Int, h: Int) {
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#080D1A") }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val linePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#00F0FF")
            strokeWidth = 4f
            isAntiAlias = true
            style = Paint.Style.STROKE
            setShadowLayer(10f, 0f, 0f, android.graphics.Color.parseColor("#00F0FF"))
        }

        val nodePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#FF007F")
            isAntiAlias = true
            style = Paint.Style.FILL
            setShadowLayer(12f, 0f, 0f, android.graphics.Color.parseColor("#FF007F"))
        }

        // Draw circuit traces
        val step = 120
        for (y in 200..h - 200 step step) {
            var x = 80f
            val path = android.graphics.Path()
            path.moveTo(x, y.toFloat())

            while (x < w - 80) {
                val nextX = (x + (80..180).random()).coerceAtMost(w - 80f)
                val deltaY = listOf(-60f, 0f, 60f).random()
                val nextY = (y + deltaY).coerceIn(150f, h - 150f)
                
                path.lineTo(nextX, nextY)
                canvas.drawCircle(nextX, nextY, 7f, nodePaint)
                x = nextX
            }
            canvas.drawPath(path, linePaint)
        }
    }

    private fun drawTerminal(canvas: Canvas, w: Int, h: Int) {
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#0F141C") }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val headerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1B2332")
        }
        canvas.drawRect(40f, 180f, w - 40f, 260f, headerPaint)

        // Window buttons
        val btnPaint = Paint().apply { isAntiAlias = true }
        btnPaint.color = android.graphics.Color.parseColor("#FF5F56")
        canvas.drawCircle(80f, 220f, 12f, btnPaint)
        btnPaint.color = android.graphics.Color.parseColor("#FFBD2E")
        canvas.drawCircle(120f, 220f, 12f, btnPaint)
        btnPaint.color = android.graphics.Color.parseColor("#27C93F")
        canvas.drawCircle(160f, 220f, 12f, btnPaint)

        // Terminal text
        val textPaint = Paint().apply {
            textSize = 32f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }

        val lines = listOf(
            Pair("#88C0D0", "dev@android-host:~$ devtools --status"),
            Pair("#A3BE8C", "[OK] Kernel: Linux 6.1-android-aarch64"),
            Pair("#EBCB8B", "[OK] Security Shield: Real-time active"),
            Pair("#D8DEE9", "[OK] Runtimes: Java 21, Python 3.12, C++17"),
            Pair("#B48EAD", "dev@android-host:~$ git status"),
            Pair("#A3BE8C", "On branch main. All systems operational."),
            Pair("#88C0D0", "dev@android-host:~$ cat /etc/motd"),
            Pair("#FFFFFF", "\"First, solve the problem. Then, write the code.\""),
            Pair("#5E81AC", "dev@android-host:~$ ./deploy_production.sh"),
            Pair("#A3BE8C", "Deployment successful: 100% complete.")
        )

        var curY = 320f
        for ((colorHex, text) in lines) {
            textPaint.color = android.graphics.Color.parseColor(colorHex)
            canvas.drawText(text, 60f, curY, textPaint)
            curY += 56f
        }
    }

    private fun drawHexGrid(canvas: Canvas, w: Int, h: Int) {
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#0B0C10") }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val hexPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1F2833")
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val glowPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#45A29E")
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
            setShadowLayer(8f, 0f, 0f, android.graphics.Color.parseColor("#66FCF1"))
        }

        val r = 60f
        val dy = r * 1.5f
        val dx = Math.sqrt(3.0).toFloat() * r

        for (y in 100..(h + 100) step dy.toInt()) {
            for (x in -50..(w + 100) step dx.toInt()) {
                val isGlow = Math.random() < 0.08
                drawPolygon(canvas, x.toFloat(), y.toFloat(), 6, r, if (isGlow) glowPaint else hexPaint)
            }
        }
    }

    private fun drawPolygon(canvas: Canvas, cx: Float, cy: Float, sides: Int, radius: Float, paint: Paint) {
        val path = android.graphics.Path()
        for (i in 0 until sides) {
            val angle = 2.0 * Math.PI / sides * i
            val x = (cx + radius * Math.cos(angle)).toFloat()
            val y = (cy + radius * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawMinimalCode(canvas: Canvas, w: Int, h: Int) {
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#090A0F") }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val logoPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#BD93F9")
            textSize = 72f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
            setShadowLayer(16f, 0f, 0f, android.graphics.Color.parseColor("#FF79C6"))
        }

        canvas.drawText("</DEV_TOOLS>", w / 2f, h / 2f - 60f, logoPaint)

        val subPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#6272A4")
            textSize = 34f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("while(alive) { code(); test(); push(); }", w / 2f, h / 2f + 20f, subPaint)
    }

    /**
     * Applies bitmap directly to Android System Wallpaper
     */
    suspend fun applyToWallpaper(
        context: Context,
        bitmap: Bitmap,
        target: Int // WallpaperManager.FLAG_SYSTEM, FLAG_LOCK, or both
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val wm = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wm.setBitmap(bitmap, null, true, target)
            } else {
                wm.setBitmap(bitmap)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
