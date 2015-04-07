package org.giosoft.RxPatterns

import android.graphics.Bitmap
import android.graphics.Canvas

import scala.collection.mutable
import scala.util.Random

class Sprite(view: RxPatternsView, frames: Array[Bitmap]) {
  var x = 0
  var y = 0
  var dx = 0
  var dy = 0
  var currentFrame = 0

  def update() {
    currentFrame += 1
    if (currentFrame == frames.length)
      currentFrame = 0
  }

  def onDraw(canvas: Canvas): Unit = {
    update()
    canvas.drawBitmap(frames(currentFrame), x , y, null)
  }

  def collides(that: Sprite): Boolean =
    this.x < that.x + that.width &&
    this.x + this.width > that.x &&
    this.y < that.y + that.height &&
    this.height + this.y > that.y

  def width = frames(currentFrame).getWidth
  def height = frames(currentFrame).getHeight
}

class Player(view: RxPatternsView, frames: Array[Bitmap], bulletFrames: Array[Bitmap]) extends Sprite(view, frames) {
  val bullets = mutable.ListBuffer[Bullet]()
  var lastFireTime = 0l

  val RELOAD_TIME = 100

  override def update(): Unit = {
    super.update()
    if (x > view.getWidth - frames(currentFrame).getWidth - dx || x + dx < 0)
      dx = 0
    if (y > view.getHeight - frames(currentFrame).getHeight - dy || y + dy < 0)
      dy = 0
    x += dx
    y += dy
  }

  override def onDraw(canvas: Canvas): Unit = {
    super.onDraw(canvas)
    bullets.foreach(_.onDraw(canvas))
    bullets --= bullets.filter(!_.isLive)
  }

  def fire(): Unit = {
    val now = System.currentTimeMillis()
    if (now - lastFireTime > RELOAD_TIME) {
      lastFireTime = now
      bullets += new Bullet(x, y, view, bulletFrames)
    }
  }
}

class Enemy(view: RxPatternsView, frames: Array[Bitmap]) extends Sprite(view, frames) {
  init()

  def init(): Unit = {
    x = view.getWidth
    y = Random.nextInt(view.getHeight-frames(currentFrame).getHeight)
    dx = - (3 + Random.nextInt(4))
  }

  override def update(): Unit = {
    super.update()
    x += dx
    if (x < -frames(currentFrame).getWidth) {
      x = view.getWidth
      init()
    }
  }
}

class Bullet(xPos: Int, yPos: Int, view: RxPatternsView, frames: Array[Bitmap]) extends Sprite(view, frames) {
  x = xPos
  y = yPos
  dx = (frames(currentFrame).getWidth * 0.5).toInt

  def isLive = x < view.getWidth

  override def update(): Unit = {
    super.update()
    x += dx
  }
}

class Explosion(xPos: Int, yPos: Int, view: RxPatternsView, frames: Array[Bitmap]) extends Sprite(view, frames) {
  x = xPos
  y = yPos

  def isLive = currentFrame < frames.length-1
}
