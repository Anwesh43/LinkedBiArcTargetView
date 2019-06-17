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
