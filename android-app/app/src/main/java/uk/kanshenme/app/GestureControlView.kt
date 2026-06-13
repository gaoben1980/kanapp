package uk.kanshenme.app

import android.content.Context
import android.graphics.*
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.Window
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * 全屏横屏时的手势控制浮层：
 *   左半屏上下滑 → 调节屏幕亮度（无需权限，仅 in-app）
 *   右半屏上下滑 → 调节媒体音量（静默调节，不弹系统UI）
 * 非竖向手势透传给底层视频播放器。
 */
class GestureControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var window: Window? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    // 状态
    private var currentBrightness = 0.5f
    private var currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    private var volAccum = 0f

    // 手势判定
    private var startX = 0f
    private var startY = 0f
    private var lastY = 0f
    private var isVertical = false
    private var decided = false
    private val slop = dp(8f)

    // HUD
    private var hudType = 0   // 0=隐藏 1=亮度 2=音量
    private var hudValue = 0f // 0.0–1.0
    private var hudAlpha = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val scheduleHide = Runnable { startFade() }
    private val fadeStep = object : Runnable {
        override fun run() {
            hudAlpha = (hudAlpha - 0.07f).coerceAtLeast(0f)
            invalidate()
            if (hudAlpha > 0f) handler.postDelayed(this, 16)
            else hudType = 0
        }
    }

    // 画笔
    private val bgPaint    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    init {
        setWillNotDraw(false)
        isClickable = false
    }

    /** 进入全屏时调用，传入 window 以读写亮度 */
    fun attach(w: Window) {
        window = w
        val lp = w.attributes
        currentBrightness = if (lp.screenBrightness <= 0f) 0.5f else lp.screenBrightness
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volAccum = 0f
    }

    // ─── 手势拦截 ──────────────────────────────────────────────────────

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x; startY = ev.y; lastY = ev.y
                isVertical = false; decided = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!decided) {
                    val dx = abs(ev.x - startX)
                    val dy = abs(ev.y - startY)
                    if (dx > slop || dy > slop) {
                        decided = true
                        isVertical = dy > dx
                    }
                }
                if (isVertical) return true  // 竖向滑动：拦截给 onTouchEvent
            }
        }
        return false  // 其余事件透传给视频播放器
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isVertical) return false
        when (ev.action) {
            MotionEvent.ACTION_MOVE -> {
                val dy = lastY - ev.y  // 上滑为正 → 增大
                lastY = ev.y
                if (ev.x < width / 2f) handleBrightness(dy)
                else handleVolume(dy)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(scheduleHide)
                handler.postDelayed(scheduleHide, 1200)
            }
        }
        return true
    }

    // ─── 亮度 / 音量 ───────────────────────────────────────────────────

    private fun handleBrightness(dy: Float) {
        val delta = (dy / height) * 1.5f
        currentBrightness = (currentBrightness + delta).coerceIn(0.05f, 1.0f)
        window?.let { w ->
            w.attributes = w.attributes.also { it.screenBrightness = currentBrightness }
        }
        showHud(1, currentBrightness)
    }

    private fun handleVolume(dy: Float) {
        volAccum += (dy / height) * maxVolume * 1.5f
        val step = volAccum.toInt()
        if (abs(step) >= 1) {
            currentVolume = (currentVolume + step).coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
            volAccum -= step
        }
        showHud(2, currentVolume.toFloat() / maxVolume)
    }

    // ─── HUD ──────────────────────────────────────────────────────────

    private fun showHud(type: Int, value: Float) {
        hudType = type; hudValue = value; hudAlpha = 1f
        handler.removeCallbacks(scheduleHide)
        handler.removeCallbacks(fadeStep)
        invalidate()
    }

    private fun startFade() {
        handler.removeCallbacks(fadeStep)
        handler.post(fadeStep)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (hudType == 0 || hudAlpha <= 0f) return

        val a = (hudAlpha * 255).toInt()
        val pillW = dp(40f); val pillH = dp(150f); val r = dp(20f)
        val barW  = dp(4f);  val barH  = dp(96f);  val bR = dp(2f)

        val cx = if (hudType == 1) width * 0.25f else width * 0.75f
        val cy = height * 0.5f

        // 背景胶囊
        bgPaint.color = Color.argb((a * 0.82f).toInt(), 10, 10, 10)
        canvas.drawRoundRect(cx-pillW/2, cy-pillH/2, cx+pillW/2, cy+pillH/2, r, r, bgPaint)

        // 标签
        textPaint.color = Color.argb(a, 200, 200, 200)
        textPaint.textSize = dp(10f)
        canvas.drawText(if (hudType == 1) "亮度" else "音量", cx, cy - pillH/2 + dp(22f), textPaint)

        // 轨道
        val barTop  = cy - pillH/2 + dp(34f)
        val barBot  = barTop + barH
        val barLeft = cx - barW/2
        trackPaint.color = Color.argb((a * 0.35f).toInt(), 255, 255, 255)
        canvas.drawRoundRect(barLeft, barTop, barLeft+barW, barBot, bR, bR, trackPaint)

        // 填充（从底部向上）
        val fillTop = barBot - barH * hudValue.coerceIn(0f, 1f)
        fillPaint.color = Color.argb(a, 255, 255, 255)
        canvas.drawRoundRect(barLeft, fillTop, barLeft+barW, barBot, bR, bR, fillPaint)

        // 百分比
        textPaint.color = Color.argb(a, 180, 180, 180)
        canvas.drawText("${(hudValue * 100).toInt()}%", cx, barBot + dp(16f), textPaint)
    }

    private fun dp(v: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics
    )
}
