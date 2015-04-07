package org.giosoft.RxPatterns

import android.graphics.{Color, Paint, Canvas}
import android.view.{MotionEvent, View}

class Joystick(x: Int, y: Int, radius: Int) {
  def onTouch(view: View, motionEvent: MotionEvent): Boolean = {
    val tx = motionEvent.getX
    val ty = motionEvent.getY
    if (tx > x && tx < x + radius && ty > y && ty < y + radius) {
      println("TOUCHHHHH")
    }
    true
  }

  def onDraw(canvas: Canvas): Unit = {
    val paint = new Paint()
    paint.setColor(Color.GREEN)
    paint.setStyle(Paint.Style.STROKE)
    paint.setStrokeWidth(3)
    canvas.drawCircle(x, y, radius, paint)
  }
}
