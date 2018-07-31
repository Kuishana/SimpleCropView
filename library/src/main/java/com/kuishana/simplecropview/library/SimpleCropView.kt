/**
 * 简单的图像裁剪，头像裁切so easy!
 */
package com.kuishana.simplecropview.library

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*

const val MAX_OUT_PUT_SIZE = 256
const val BACKGROUND_COLOR_X = -0xa0909
const val MARK_COLOR = -0x80000000

class SimpleCropView(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : SurfaceView(context, attributeSet, defStyleAttr), Runnable, SurfaceHolder.Callback {
    private var maxOutPutSize = MAX_OUT_PUT_SIZE
    private var backgroundColorX = BACKGROUND_COLOR_X
    private var markColor = MARK_COLOR

    private val paintMark = Paint()
    private val rectFMark = RectF()
    private val rectFDstFull = RectF()
    private val rectFDstShow = RectF()

    private var movingOrScaling = false
    private val gestureDetector: GestureDetector
    private val scaleGestureDetector: ScaleGestureDetector

    private var surfaceLiving = false
    private var doRender = false
    private val renderTread: Thread

    private var canRender = false
    private val antialiasDrawFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private var bitmapRegionDecoder: BitmapRegionDecoder? = null

    private val rectSrcLast = Rect()
    private val rectSrcDraw = Rect()

    private var bitmap: Bitmap? = null
    private val options = BitmapFactory.Options()
    private val rectBitmap = Rect()

    init {
        attributeSet?.let {
            context.obtainStyledAttributes(it, R.styleable.SimpleCropView).run {
                maxOutPutSize = getInteger(R.styleable.SimpleCropView_simpleCropViewMaxOutPutSize, MAX_OUT_PUT_SIZE)
                backgroundColorX = getColor(R.styleable.SimpleCropView_simpleCropViewBackgroundColorX, BACKGROUND_COLOR_X)
                markColor = getColor(R.styleable.SimpleCropView_simpleCropViewMarkColor, MARK_COLOR)
                recycle()
            }
        }

        paintMark.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                var x = -distanceX
                var y = -distanceY
                if (x + rectFDstFull.left > rectFMark.left) {
                    x = rectFMark.left - rectFDstFull.left
                } else if (x + rectFDstFull.right < rectFMark.right) {
                    x = rectFMark.right - rectFDstFull.right
                }
                if (y + rectFDstFull.top > rectFMark.top) {
                    y = rectFMark.top - rectFDstFull.top
                } else if (y + rectFDstFull.bottom < rectFMark.bottom) {
                    y = rectFMark.bottom - rectFDstFull.bottom
                }
                rectFDstFull.offset(x, y)
                render()
                return true
            }
        })

        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            /**
             * 中心缩放（以指定点为中心缩放）
             * 思路：缩放前缩放中心点的位置s1，让图形按图形中心缩放后缩放中心点的位置变成s2，那么把图形按图形中心缩放后再移动s2-s1就行了
             */
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                //以两指中点为中心缩放
                var cx = detector.focusX
                var cy = detector.focusY
                var scale = detector.scaleFactor
                val minDstSize = Math.min(rectFDstFull.width(), rectFDstFull.height())
                if (minDstSize > 0.0f) {
                    if (scale * minDstSize < rectFMark.width())
                        scale = rectFMark.width() / minDstSize
                    if (scale != 1.0f) {
                        val scaleDiff = 1.0f - scale
                        if (rectFDstFull.left * scale + cx * scaleDiff > rectFMark.left) {
                            cx = (rectFMark.left - rectFDstFull.left * scale) / scaleDiff
                        } else if (rectFDstFull.right * scale + cx * scaleDiff < rectFMark.right) {
                            cx = (rectFMark.right - rectFDstFull.right * scale) / scaleDiff
                        }
                        if (rectFDstFull.top * scale + cy * scaleDiff > rectFMark.top) {
                            cy = (rectFMark.top - rectFDstFull.top * scale) / scaleDiff
                        } else if (rectFDstFull.bottom * scale + cy * scaleDiff < rectFMark.bottom) {
                            cy = (rectFMark.bottom - rectFDstFull.bottom * scale) / scaleDiff
                        }
                        rectFDstFull.left = rectFDstFull.left * scale + cx * scaleDiff
                        rectFDstFull.top = rectFDstFull.top * scale + cy * scaleDiff
                        rectFDstFull.right = rectFDstFull.right * scale + cx * scaleDiff
                        rectFDstFull.bottom = rectFDstFull.bottom * scale + cy * scaleDiff
                    }
                    render()
                }
                return true
            }
        })
        renderTread = Thread(this)
        holder.addCallback(this)
    }

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> movingOrScaling = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                movingOrScaling = false
                render()
            }
        }
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        return true
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        surfaceLiving = true
    }

    /**
     * 从win32过来的，习惯在onSizeChanged里处理跟大小有关的东西，SurfaceView里就在surfaceChanged中处理
     */
    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        val cx = width / 2.0f
        val cy = height / 2.0f
        val radius = Math.min(cx, cy) / 2.0f
        rectFMark.set(cx - radius, cy - radius, cx + radius, cy + radius)
        render()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        surfaceLiving = false
    }

    /**
     * 不知道在下面两个方法里处理view的生命周期对不对
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bitmapRegionDecoder?.takeUnless { it.isRecycled }?.let {
            doRender = true
            renderTread.takeUnless { it.isAlive }?.start()
        }
    }

    override fun onDetachedFromWindow() {
        doRender = false
        super.onDetachedFromWindow()
    }

    override fun run() {
        while (doRender) {
            if (canRender) {
                canRender = false
                if (surfaceLiving) {
                    holder.lockCanvas()?.run {
                        drawColor(backgroundColorX)
                        if (!movingOrScaling)
                            drawFilter = antialiasDrawFilter//如果用户正在缩放，不抗锯齿以加快绘制，感觉差别不大

                        bitmapRegionDecoder?.takeUnless { it.isRecycled or rectFDstFull.isEmpty }?.let {
                            //计算要绘制部分
                            rectFDstShow.set(rectFDstFull)
                            rectFDstShow.intersect(0.0f, 0.0f, width.toFloat(), height.toFloat())
                            val scale = it.width / rectFDstFull.width()
                            rectSrcDraw.set(((rectFDstShow.left - rectFDstFull.left) * scale).toInt(), ((rectFDstShow.top - rectFDstFull.top) * scale).toInt()
                                    , ((rectFDstShow.right - rectFDstFull.left) * scale).toInt(), ((rectFDstShow.bottom - rectFDstFull.top) * scale).toInt())
                            if (null == bitmap || !rectSrcDraw.isEmpty && rectSrcDraw != rectSrcLast) {
                                bitmap?.takeUnless { it.isRecycled }?.recycle()
                                options.inSampleSize = if (movingOrScaling) nxtPow2(scale) else prePow2(scale)//如果用户正在缩放就缩略采样，如果缩放完成则高清采样，参见BitmapFactory.Options
                                bitmap = it.decodeRegion(rectSrcDraw, options)//rectSrcDraw越大这里越慢，想要极致体验请参考系统照片查看器，看不懂#^_^#
                                rectBitmap.set(0, 0, bitmap!!.width, bitmap!!.height)
                                rectSrcLast.set(rectSrcDraw)
                            }

                            bitmap?.takeUnless { it.isRecycled }?.let { drawBitmap(it, rectBitmap, rectFDstShow, null) }
                        }

                        saveLayer(null, null, Canvas.ALL_SAVE_FLAG)
                        drawColor(markColor)
                        drawCircle(rectFMark.centerX(), rectFMark.centerY(), rectFMark.width() / 2.0f, paintMark)
                        restore()
                        holder.unlockCanvasAndPost(this)
                    }
                }
            }
        }
    }

    /**
     * 使用BitmapRegionDecoder，避免OOM
     */
    fun setBitmapRegionDecoder(bitmapRegionDecoder: BitmapRegionDecoder) {
        this.bitmapRegionDecoder?.takeUnless { it.isRecycled }?.recycle()
        this.bitmapRegionDecoder = bitmapRegionDecoder
        val srcWidth = bitmapRegionDecoder.width.toFloat()
        val srcHeight = bitmapRegionDecoder.height.toFloat()

        //使图像适应
        var scale = Math.min(width, height).toFloat() / Math.min(srcWidth, srcHeight) / 2.0f
        if (scale < 1.0f) {
            scale = Math.min(Math.max(Math.min(width / srcWidth, height / srcHeight), scale), 1.0f)
        }

        rectFDstFull.set(0.0f, 0.0f, srcWidth * scale, srcHeight * scale)
        rectFDstFull.offset((width - rectFDstFull.width()) / 2.0f, (height - rectFDstFull.height()) / 2.0f)
        doRender = true
        renderTread.takeUnless { it.isAlive }?.start()
        render()
    }

    fun crop(): Bitmap? {
        var bitmapDst: Bitmap? = null
        bitmapRegionDecoder?.takeUnless { it.isRecycled and rectFMark.isEmpty.not() and rectFDstFull.contains(rectFMark) }?.run {
            val scale = width.toFloat() / rectFDstFull.width()
            val rect = Rect(((rectFMark.left - rectFDstFull.left) * scale).toInt(), ((rectFMark.top - rectFDstFull.top) * scale).toInt()
                    , ((rectFMark.right - rectFDstFull.left) * scale).toInt(), ((rectFMark.bottom - rectFDstFull.top) * scale).toInt())
            //maxOutPutSize过大可能OOM，建议maxOutPutSize<=Math.min(screenWith, screenHeight)*2/3
            if (rect.width() <= maxOutPutSize) {
                bitmapDst = decodeRegion(rect, null)
            } else {
                options.inSampleSize = if (rect.width() > maxOutPutSize * 2) nxtPow2(rect.width() / maxOutPutSize / 2.0f) else 1
                bitmapDst = Bitmap.createBitmap(maxOutPutSize, maxOutPutSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmapDst)
                canvas.drawFilter = antialiasDrawFilter
                val bitmap = decodeRegion(rect, options)
                canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), Rect(0, 0, maxOutPutSize, maxOutPutSize), null)
                bitmap.recycle()
            }
        }
        return bitmapDst
    }

    private fun render() {
        canRender = true
    }

    private fun nxtPow2(v: Float): Int {
        var p = 1
        while (p > 0 && v > p) {
            p = p shl 1
        }
        return if (p > 0) p else p shr 1
    }

    private fun prePow2(v: Float): Int {
        val p = nxtPow2(v)
        return if (p > 1) p shr 1 else p
    }
}