package org.giosoft.RxPatterns

import java.io.{IOException, InputStream}
import java.lang.Math._

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics._
import android.os.Bundle
import android.view.SurfaceHolder.Callback
import android.view._
import android.widget.RelativeLayout
import org.giosoft.RxPatterns.JoystickView.OnJoystickMoveListener

import scala.collection.mutable
import scala.util.Random

class RxPatternsActivity extends Activity {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE)

    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    getWindow.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

    val layout = new RelativeLayout(this)

    val mainView: RxPatternsView = new RxPatternsView(layout.getContext)

    val joystickView = new JoystickView(layout.getContext)
    joystickView.setOnJoystickMoveListener(mainView, JoystickView.DEFAULT_LOOP_INTERVAL)

    layout.addView(mainView)
    layout.addView(joystickView)
    setContentView(layout)
  }
}

class RxPatternsView(ctx: Context) extends SurfaceView(ctx) with OnJoystickMoveListener {

  private val gameLoopThread = new GameLoopThread(this)

  private lazy val enemyFrames = bitmaps("Red/Enemy_animation", -90, 0.15f)
  private lazy val blueExplosionFrames = bitmaps("Effects/Blue Effects", 0, 0.2f)
  private lazy val redExplosionFrames = bitmaps("Effects/Red Explosion", 0, 0.2f)

  private lazy val player = new Player(this, bitmaps("Blue/PlayerShip", 0, 0.2f), bitmaps("Blue/Bullet", 90, 0.02f))
  private lazy val enemies = collection.mutable.ListBuffer.fill(2){new Enemy(this, enemyFrames)}
  private lazy val stars = collection.mutable.ListBuffer.fill(26){new Star(getWidth, getHeight)}
  private val explosions = new mutable.ListBuffer[Explosion]

  override def onWindowFocusChanged(hasFocus: Boolean): Unit = {
    super.onWindowFocusChanged(hasFocus)
    // TODO: pause game!
  }

  def bitmaps(folder: String, angle: Float, scale: Float): Array[Bitmap] =
    ctx.getAssets.list(folder).map(file => getBitmapFromAsset(folder + "/" + file, angle, scale))

  def getBitmapFromAsset(strName: String, angle: Float, scale: Float): Bitmap = {
    var istr: InputStream = null
    try {
      istr = ctx.getAssets.open(strName)
    } catch {
      case e: IOException => e.printStackTrace()
    }
    val matrix = new Matrix()
    matrix.postRotate(angle)
    val raw = BitmapFactory.decodeStream(istr)
    val ratio = raw.getHeight.toDouble / raw.getWidth.toDouble
    val width = getWidth * scale
    val height = width * ratio
    val scaled = Bitmap.createScaledBitmap(raw, width.toInt, height.toInt, false)
    Bitmap.createBitmap(scaled, 0, 0, scaled.getWidth, scaled.getHeight, matrix, true)
  }

  getHolder.addCallback(new Callback {
    override def surfaceCreated(holder: SurfaceHolder): Unit = {
      // pre-load sprites & frames
      blueExplosionFrames
      redExplosionFrames
      player
      enemies
      stars

      gameLoopThread.running = true
      gameLoopThread.start()
    }

    override def surfaceDestroyed(holder: SurfaceHolder): Unit = {
      var retry = true
      gameLoopThread.running = false
      Stream.from(0).takeWhile(_ => retry).foreach(_ => {
        try {
          gameLoopThread.join()
          retry = false
        } catch { case _ : InterruptedException => }
      })
    }

    override def surfaceChanged(holder: SurfaceHolder, format: Int,
                                width: Int, height: Int): Unit = {}
  })

  override def onDraw(canvas: Canvas): Unit = {
//    canvas.drawColor(Color.rgb(135,206,235))
    canvas.drawColor(Color.rgb(31,31,31))

    val starsPaint = new Paint()
    starsPaint.setColor(Color.WHITE)
    starsPaint.setStrokeWidth((getWidth*0.003).toInt)
    stars.foreach(p => {
      p.update()
      starsPaint.setAlpha(p.alpha)
      canvas.drawPoint(p.x, p.y, starsPaint)
    })

    val (fallen, survivors) = enemies.map(e => (e, player.bullets.find(_ collides e))).partition(_._2.isDefined)
    fallen.foreach {
      case (e, Some(b)) =>
        explosions += new Explosion(b.x, b.y, this, blueExplosionFrames)
        explosions += new Explosion(e.x, e.y, this, redExplosionFrames)
        enemies += new Enemy(this, enemyFrames)
        enemies -= e
        player.bullets -= b
      case _ =>
    }

    explosions --= explosions.filter(!_.isLive)

    enemies.foreach(_.onDraw(canvas))
    explosions.foreach(_.onDraw(canvas))
    player.onDraw(canvas)
  }

  // joystick input callback
  override def onValueChanged(angle: Int, power: Int, direction: Int): Unit = {
    if (power == 0)
      player.dx = (-getWidth * Magic.speedScale * 20).toInt
    else
      player.dx = (power * getWidth * Magic.speedScale * cos(toRadians(angle-90))).toInt

    player.dy = (power * getHeight * Magic.speedScale * sin(toRadians(angle-90))).toInt
  }

  override def onButtonPressed(button: Int): Unit = {
    player.fire()
  }
}

class Star(width: Int, height: Int) {
  var x = 0
  var y = 0
  var dx = 0
  var alpha = 0

  init()

  def init() = {
    x = Random.nextInt(width)
    y = Random.nextInt(height)
    while(dx == 0) // WHY?!?
      dx = (-width * 2 * Magic.speedScale * (5 + Random.nextInt(15))).toInt
    alpha = 100 + Random.nextInt(155)
  }

  def update(): Unit = {
    x += dx
    if (x < 0) {
      init()
      x = width
    }
  }
}