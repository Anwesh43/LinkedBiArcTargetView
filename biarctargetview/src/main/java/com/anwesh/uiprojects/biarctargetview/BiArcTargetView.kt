package com.anwesh.uiprojects.biarctargetview

/**
 * Created by anweshmishra on 17/06/19.
 */

import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Canvas

val nodes : Int = 5
val parts : Int = 2
val lines : Int = 3
val rotDeg : Float = 90f
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#283593")
val backColor : Int = Color.parseColor("#BDBDBD")
val delay : Long = 20

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a: Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawTargetArc(i : Int, sc : Float, size : Float, paint : Paint) {
    val r : Float = size / 2
    val sci : Float = sc.divideScale(i, parts)
    val sf : Float = 1f - 2 * i
    save()
    scale(sf, 1f)
    drawArc(RectF(-r, -r, r, r), -90f, 180f * sci, false, paint)
    restore()
}

fun Canvas.drawBiArc(sc : Float, size : Float, paint : Paint) {
    for (j in 0..(parts - 1)) {
        drawTargetArc(j, sc, size, paint)
    }
}

fun Canvas.drawTargetLines(sc : Float, size : Float, paint : Paint) {
    val offsetY : Float = -size / 2
    var currDeg : Float = 0f
    drawLine(0f, offsetY, 0f, 2 * offsetY, paint)
    for (j in 0..(lines - 1)) {
        val scj : Float = sc.divideScale(j, lines)
        currDeg += rotDeg * scj
        save()
        rotate(currDeg)
        drawLine(0f, offsetY, 0f, 2 * offsetY, paint)
        restore()
    }
}

fun Canvas.drawBATNode(i : Int, scale : Float, paint : Paint) {
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    paint.style = Paint.Style.STROKE
    save()
    translate(w / 2, gap * (i + 1))
    drawBiArc(sc1, size, paint)
    drawTargetLines(sc2, size, paint)
    restore()
}

class BiArcTargetView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, parts, lines)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class BATNode(var i : Int, val state : State = State()) {

        private var next : BATNode? = null
        private var prev : BATNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = BATNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawBATNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : BATNode {
            var curr : BATNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class BiArcTarget(var i : Int) {

        private val root : BATNode = BATNode(0)
        private var curr : BATNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : BiArcTargetView) {

        private val animator : Animator = Animator(view)
        private val bat : BiArcTarget = BiArcTarget(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            bat.draw(canvas, paint)
            animator.animate {
                bat.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            bat.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : BiArcTargetView {
            val view : BiArcTargetView = BiArcTargetView(activity)
            activity.setContentView(view)
            return view
        }
    }
}