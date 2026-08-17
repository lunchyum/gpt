package com.lunchyum.neiswallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NeisLiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = NeisEngine()

    private inner class NeisEngine : Engine() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL) }
        private var visible = false
        private var snapshot: WallpaperSnapshot? = null

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) draw()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            draw()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            super.onSurfaceDestroyed(holder)
        }

        private fun draw() {
            snapshot = WallpaperStorage.load(applicationContext)
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) render(canvas, snapshot)
            } finally { if (canvas != null) holder.unlockCanvasAndPost(canvas) }
        }

        private fun render(canvas: Canvas, data: WallpaperSnapshot?) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            canvas.drawColor(Color.rgb(18, 18, 21))
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)
            paint.textSize = w * 0.065f
            canvas.drawText(data?.schoolName ?: "학교 배경화면", 52f, 82f, paint)

            paint.typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)
            paint.textSize = w * 0.034f
            paint.color = Color.rgb(205, 196, 255)
            val dateLabel = data?.date?.let { runCatching { SimpleDateFormat("yyyyMMdd", Locale.KOREA).parse(it) }.getOrNull() }
                ?.let { SimpleDateFormat("M월 d일 (E)", Locale.KOREA).format(it) }
                ?: SimpleDateFormat("M월 d일 (E)", Locale.KOREA).format(Date())
            canvas.drawText(dateLabel, 54f, 122f, paint)

            val left = 48f
            val right = w - 48f
            var y = 175f
            val cardGap = 22f
            if (data?.mode == "TIMETABLE" || data?.mode == "BOTH" || data == null) {
                y = drawPanel(canvas, left, right, y, "시간표", data?.lessons?.joinToString("\n") { "${it.period}교시  ${it.subject}" } ?: "앱에서 나이스 데이터를 불러오세요.")
                y += cardGap
            }
            if (data?.mode == "MEAL" || data?.mode == "BOTH" || data == null) {
                val mealText = data?.meals?.joinToString("\n\n") { meal -> "${meal.type}${meal.kcal?.let { "  ·  $it" } ?: ""}\n${meal.items.joinToString(" · ")}" }
                    ?: "급식 정보를 불러오면 이곳에 표시됩니다."
                drawPanel(canvas, left, right, y.coerceAtMost(h - 180f), "급식", mealText)
            }

            paint.color = Color.argb(160, 255, 255, 255)
            paint.textSize = w * 0.027f
            canvas.drawText("NEIS • 매일 새로고침 권장", 52f, h - 42f, paint)
        }

        private fun drawPanel(canvas: Canvas, left: Float, right: Float, top: Float, title: String, body: String): Float {
            val maxHeight = (canvas.height * 0.34f).coerceAtLeast(240f)
            paint.color = Color.rgb(38, 37, 44)
            canvas.drawRoundRect(left, top, right, top + maxHeight, 34f, 34f, paint)
            paint.color = Color.WHITE
            paint.typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)
            paint.textSize = canvas.width * 0.043f
            canvas.drawText(title, left + 28f, top + 52f, paint)
            paint.typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)
            paint.textSize = canvas.width * 0.029f
            paint.color = Color.rgb(232, 229, 238)
            val lines = body.split("\n")
            var lineY = top + 94f
            for (line in lines.take(10)) {
                val wrapped = wrap(line, 28)
                for (part in wrapped) {
                    canvas.drawText(part, left + 28f, lineY, paint)
                    lineY += canvas.width * 0.038f
                    if (lineY > top + maxHeight - 24f) break
                }
                if (lineY > top + maxHeight - 24f) break
            }
            return top + maxHeight
        }

        private fun wrap(text: String, maxChars: Int): List<String> {
            if (text.length <= maxChars) return listOf(text)
            return text.chunked(maxChars)
        }
    }
}
