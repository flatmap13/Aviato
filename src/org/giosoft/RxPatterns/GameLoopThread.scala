package org.giosoft.RxPatterns

import android.graphics.Canvas

import scala.util.Try

class GameLoopThread(view: RxPatternsView) extends Thread {
  var running = false
  val FPS = 60l

  override def run(): Unit = {
    val ticksPS = 1000l / FPS
    var startTime = 0l
    var sleepTime = 0l

    while (running) {
      var c: Canvas = null
      startTime = System.currentTimeMillis()
      try {
        c = view.getHolder.lockCanvas()
        view.getHolder.synchronized {
          if (c != null) view.onDraw(c)
        }
      } finally {
        if (c != null) view.getHolder.unlockCanvasAndPost(c)
      }
      sleepTime = ticksPS-(System.currentTimeMillis() - startTime)
      Try {
        if (sleepTime > 0) Thread.sleep(sleepTime)
        else Thread.sleep(10)
      }
    }
  }

}
