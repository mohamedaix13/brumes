package com.mumu.avatarlord

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.*

private data class ALHud(
    val life: Int, val mana: Int, val realm: String, val flying: Boolean,
    val crystals: Int, val crystalsMax: Int, val enemies: Int, val purified: Boolean,
    val spell: String, val combo: String, val bossHp: Int, val score: Int,
    val night: Boolean, val fps: Int, val quality: String
)

private data class ALProp(val x: Float, val y: Float, val z: Float, val sx: Float, val sy: Float, val sz: Float, val rot: Float, val kind: Int, val r: Float, val g: Float, val b: Float)
private data class ALMob(var x: Float, var z: Float, val boss: Boolean, val ranged: Boolean, var hp: Float, var hpMax: Float, var phase: Float, var cool: Float, var hitT: Float, var respawn: Float)
private data class ALCrystal(val x: Float, val z: Float, var taken: Boolean)
private data class ALSpark(var x: Float, var y: Float, var z: Float, var vx: Float, var vy: Float, var vz: Float, var life: Float, val maxLife: Float, val r: Float, val g: Float, val b: Float, val size: Float, val grav: Float)

private class Rng(var s: Long) {
    fun next(): Float { s = s * 6364136223846793005L + 1442695040888963407L; return ((s shr 33) and 0xFFFFFF) / 16777216f }
}

class GameAvatarLord : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var game: AvatarView
    private lateinit var hudView: TextView
    private var menu: View? = null
    private val gold = Color.rgb(232, 200, 130)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun panel(alpha: Int = 185) = GradientDrawable().apply {
        setColor(Color.argb(alpha, 7, 11, 16)); cornerRadius = dp(13).toFloat(); setStroke(dp(1), Color.argb(130, 232, 200, 130))
    }
    private fun text(t: String, size: Float = 11f) = TextView(this).apply {
        this.text = t; textSize = size; setTextColor(Color.rgb(245, 242, 234)); setPadding(dp(9), dp(5), dp(9), dp(5))
    }
    private fun button(t: String, w: Int = 70, onClick: () -> Unit) = Button(this).apply {
        text = t; textSize = 9.5f; isAllCaps = false; setTextColor(gold); backgroundTintList = null; background = panel(178)
        setPadding(dp(3), 0, dp(3), 0); minWidth = 0; minHeight = 0; setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(dp(w), dp(42))
    }
    private fun noHover(v: View) {
        v.isHoverable = false
        if (v is ViewGroup) for (i in 0 until v.childCount) noHover(v.getChildAt(i))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        root = FrameLayout(this).apply { isMotionEventSplittingEnabled = true }
        game = AvatarView(this); root.addView(game, FrameLayout.LayoutParams(-1, -1))
        hudView = text("AVATARLORD", 10.5f).apply { background = panel(178) }
        root.addView(hudView, FrameLayout.LayoutParams(dp(310), dp(78), Gravity.TOP or Gravity.START).apply { setMargins(dp(9), dp(7), 0, 0) })
        root.addView(button("☰", 50) { openMenu() }, FrameLayout.LayoutParams(dp(54), dp(42), Gravity.TOP or Gravity.END).apply { setMargins(0, dp(7), dp(9), 0) })
        val left = ALStick(this) { x, y -> game.move(x, y) }
        root.addView(left, FrameLayout.LayoutParams(dp(122), dp(122), Gravity.BOTTOM or Gravity.START).apply { setMargins(dp(10), 0, 0, dp(9)) })
        val right = ALStick(this) { x, y -> game.look(x, y) }
        root.addView(right, FrameLayout.LayoutParams(dp(108), dp(108), Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, dp(10), dp(9)) })
        val spells = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val elNames = arrayOf("🔥 Feu", "💧 Eau", "🌪 Air", "🪨 Terre", "⚡ Foudre", "🌫 Brume", "✨ Lumière")
        for (i in 0..6) spells.addView(button(elNames[i], 64) { game.element(i) })
        root.addView(spells, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, dp(45), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { setMargins(0, 0, 0, dp(8)) })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        actions.addView(button("➤ LANCER", 86) { game.castAction() })
        actions.addView(button("✦ Esquive", 86) { game.dashAction() }, LinearLayout.LayoutParams(dp(86), dp(42)).apply { topMargin = dp(4) })
        actions.addView(button("☁ Vol", 86) { game.flightAction() }, LinearLayout.LayoutParams(dp(86), dp(42)).apply { topMargin = dp(4) })
        root.addView(actions, FrameLayout.LayoutParams(dp(92), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END).apply { setMargins(0, 0, dp(11), 0) })
        game.onHud = { s ->
            hudView.text = "AVATARLORD 1.0 · ${s.realm}${if (s.night) " 🌙" else ""}\nVie ${s.life}   Mana ${s.mana}   Score ${s.score}   ${if (s.flying) "VOL" else "SOL"}\nCristaux ${s.crystals}/${s.crystalsMax}${if (s.purified) " ✦ PURIFIÉ" else ""} · ${s.enemies} ennemis${if (s.bossHp >= 0) " · Boss ${s.bossHp} PV" else ""}\n${s.spell}${if (s.combo.isNotEmpty()) "  ·  " + s.combo else ""} · ${s.fps} FPS · ${s.quality}"
        }
        setContentView(root)
        noHover(root)
    }
    override fun onPause() { game.onPause(); game.saveNow(); super.onPause() }
    override fun onResume() { super.onResume(); game.onResume() }
    @Deprecated("Deprecated in Java") override fun onBackPressed() { if (menu == null) openMenu() else closeMenu() }
    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.action == KeyEvent.ACTION_DOWN || e.action == KeyEvent.ACTION_UP) {
            val down = e.action == KeyEvent.ACTION_DOWN
            val used = when (e.keyCode) {
                KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_W -> { game.key(0, down); true }
                KeyEvent.KEYCODE_S -> { game.key(1, down); true }
                KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_A -> { game.key(2, down); true }
                KeyEvent.KEYCODE_D -> { game.key(3, down); true }
                KeyEvent.KEYCODE_SPACE -> { if (down && e.repeatCount == 0) game.pressSpace(); true }
                in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_7 -> { if (down && e.repeatCount == 0) game.element(e.keyCode - KeyEvent.KEYCODE_1); true }
                else -> false
            }
            if (used) return true
        }
        return super.dispatchKeyEvent(e)
    }
    private var mouseInit = false
    private var lastMX = 0f
    private var lastMY = 0f
    override fun dispatchGenericMotionEvent(e: MotionEvent): Boolean {
        if (menu != null) return super.dispatchGenericMotionEvent(e)
        if ((e.source and InputDevice.SOURCE_MOUSE) != 0) {
            when (e.actionMasked) {
                MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_MOVE -> {
                    if (mouseInit) game.mouseLook(e.x - lastMX, e.y - lastMY)
                    lastMX = e.x; lastMY = e.y; mouseInit = true
                    return true
                }
                MotionEvent.ACTION_SCROLL -> { game.wheel(e.getAxisValue(MotionEvent.AXIS_VSCROLL)); return true }
                MotionEvent.ACTION_BUTTON_PRESS -> {
                    when (e.actionButton) {
                        MotionEvent.BUTTON_PRIMARY -> game.castAction()
                        MotionEvent.BUTTON_SECONDARY -> game.cycleElement(1)
                        MotionEvent.BUTTON_TERTIARY -> game.dashAction()
                    }
                    return true
                }
            }
        }
        return super.dispatchGenericMotionEvent(e)
    }
    private fun closeMenu() { menu?.let { root.removeView(it) }; menu = null; game.setPaused(false) }
    private fun openMenu() {
        if (menu != null) return
        game.setPaused(true)
        val shade = FrameLayout(this).apply { setBackgroundColor(0xC3000000.toInt()); isClickable = true }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(18), dp(14), dp(18), dp(14)); background = panel(238) }
        body.addView(text("A V A T A R L O R D", 24f).apply { gravity = Gravity.CENTER; setTextColor(gold) })
        body.addView(text("1.0 · Samsung Galaxy Tab A9+ (SM-X210) · Snapdragon 695 / Adreno 619 · rendu supersamplé jusqu'à 3200×2000", 10.5f).apply { gravity = Gravity.CENTER })
        val q = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        q.addView(button("Natif", 92) { game.setQuality(0) })
        q.addView(button("Élevé 2560×1600", 118) { game.setQuality(1) }, LinearLayout.LayoutParams(dp(118), dp(42)).apply { leftMargin = dp(5) })
        q.addView(button("Ultra 3200×2000", 122) { game.setQuality(2) }, LinearLayout.LayoutParams(dp(122), dp(42)).apply { leftMargin = dp(5) })
        body.addView(q, LinearLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, dp(46)).apply { topMargin = dp(6) })
        body.addView(button("REPRENDRE", 205) { closeMenu() }, LinearLayout.LayoutParams(dp(205), dp(46)).apply { topMargin = dp(8) })
        body.addView(text("Tactile : joystick gauche = bouger · droit = caméra · 2 éléments = combo · LANCER / Esquive / Vol.\nClavier 5 touches : Z Q S D (ou W A S D) = déplacer · ESPACE = lancer le sort choisi (ou esquiver) · 1..7 = choisir un élément.\nSouris : bouger = tourner la caméra · clic gauche = lancer · clic droit ou molette = changer d'élément · clic milieu = esquive.\nTraverse les 4 portails du Nexus vers les royaumes élémentaires. 3 cristaux purifient un royaume.", 10f).apply { gravity = Gravity.CENTER; setPadding(dp(4), dp(8), dp(4), dp(4)) })
        body.addView(button("NOUVELLE PARTIE", 205) { game.resetSave(); closeMenu() }, LinearLayout.LayoutParams(dp(205), dp(46)).apply { topMargin = dp(6) })
        shade.addView(body, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        root.addView(shade, FrameLayout.LayoutParams(-1, -1)); menu = shade
    }
}

private class ALStick(activity: Activity, val out: (Float, Float) -> Unit) : View(activity) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG); private var dx = 0f; private var dy = 0f
    override fun onDraw(c: Canvas) {
        val r = min(width, height) * .39f; p.style = Paint.Style.FILL; p.color = 0x2410151B; c.drawCircle(width / 2f, height / 2f, r, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = 0x88E8C882.toInt(); c.drawCircle(width / 2f, height / 2f, r, p)
        p.style = Paint.Style.FILL; p.color = 0xA8E8C882.toInt(); c.drawCircle(width / 2f + dx * r, height / 2f + dy * r, r * .21f, p)
    }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                dx = (e.x - width / 2) / (width * .39f); dy = (e.y - height / 2) / (height * .39f); val l = max(1f, hypot(dx, dy)); dx /= l; dy /= l
            }
            else -> { dx = 0f; dy = 0f }
        }
        out(dx, dy); invalidate(); return true
    }
}
private class AvatarView(activity: Activity) : GLSurfaceView(activity) {
    internal var onHud: ((ALHud) -> Unit)? = null
    private val r = AvatarRenderer(activity, activity.getSharedPreferences("avatarlord_save", Context.MODE_PRIVATE)) { h -> post { onHud?.invoke(h) } }
    init {
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(r)
        renderMode = RENDERMODE_CONTINUOUSLY
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
    }
    fun move(x: Float, y: Float) { r.kx = x; r.ky = y }
    fun look(x: Float, y: Float) { r.lx = x; r.ly = y }
    fun element(i: Int) = queueEvent { r.element(i) }
    fun cycleElement(d: Int) = queueEvent { r.cycleElement(d) }
    fun castAction() = queueEvent { r.castAction() }
    fun dashAction() = queueEvent { r.dash() }
    fun flightAction() = queueEvent { r.toggleFlight() }
    fun pressSpace() = queueEvent { r.space() }
    fun key(i: Int, down: Boolean) { r.keys[i] = down }
    fun mouseLook(dx: Float, dy: Float) { r.mlx += dx; r.mly += dy }
    fun wheel(v: Float) = queueEvent { r.cycleElement(if (v < 0) 1 else -1) }
    fun setQuality(q: Int) = queueEvent { r.quality = q; r.savePrefs() }
    fun resetSave() = queueEvent { r.resetSave() }
    fun saveNow() = queueEvent { r.savePrefs() }
    fun setPaused(v: Boolean) = queueEvent { r.paused = v }
    private var pinch = 0f
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.pointerCount >= 2) {
            val d = hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1)); if (pinch > 0) r.zoom((pinch - d) / 230f); pinch = d
        } else if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) pinch = 0f
        return true
    }
}

private class AvatarRenderer(private val act: Activity, private val prefs: android.content.SharedPreferences, private val hud: (ALHud) -> Unit) : GLSurfaceView.Renderer {
    @Volatile var kx = 0f; @Volatile var ky = 0f; @Volatile var lx = 0f; @Volatile var ly = 0f
    @Volatile var paused = false
    @Volatile var quality = 1
    val keys = BooleanArray(4)
    @Volatile var mlx = 0f; @Volatile var mly = 0f
    private var program = 0
    private var aPos = 0; private var aNor = 0; private var aUv = 0
    private var uMvp = 0; private var uModel = 0; private var uColor = 0; private var uEye = 0; private var uGlow = 0
    private var uFog = 0; private var uFogColor = 0; private var uUseTex = 0; private var uSky = 0; private var uTex = 0
    private var uTime = 0; private var uWind = 0; private var uWater = 0; private var uDay = 0; private var uSunDir = 0; private var uAniso = 0
    private val proj = FloatArray(16); private val view = FloatArray(16); private val vp = FloatArray(16)
    private val model = FloatArray(16); private val mvp = FloatArray(16); private val ortho = FloatArray(16); private val tmpM = FloatArray(16)
    private lateinit var cube: FloatBuffer; private var cubeCount = 0
    private lateinit var sphere: FloatBuffer; private var sphereCount = 0
    private lateinit var quad: FloatBuffer
    private lateinit var quadXZ: FloatBuffer
    private val terrain = Array<FloatBuffer?>(5) { null }; private val terrainCount = IntArray(5)
    private val bladeBuf: FloatBuffer = ByteBuffer.allocateDirect(150 * 6 * 8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val props = Array(5) { mutableListOf<ALProp>() }
    private val mobs = Array(5) { mutableListOf<ALMob>() }
    private val crystals = Array(5) { mutableListOf<ALCrystal>() }
    private val purified = BooleanArray(5)
    private val sparks = mutableListOf<ALSpark>()
    private val realmNames = arrayOf("Nexus des Portails", "Royaume du Feu", "Royaume de l'Eau", "Royaume de l'Air", "Royaume de la Terre")
    private var texGrass = 0; private var texLava = 0; private var texSand = 0; private var texRock = 0; private var texObsidian = 0
    private var texWood = 0; private var texLeaves = 0; private var texMoss = 0; private var texWater = 0; private var texSnow = 0; private var texPortal = 0; private var texCrystal = 0
    private var realm = 0; private var px = 0f; private var py = 1.3f; private var pz = 10f; private var yaw = 0f; private var pitch = .22f; private var camZoom = 11f
    private var flying = false; private var flyBoost = 0f; private var life = 100f; private var mana = 100f; private var score = 0; private var kills = 0
    private var last = 0L; private var t = 0f; private var hudT = 0f; private var dayT = .55f
    private var sel0 = -1; private var sel1 = -1; private var selCount = 0; private var spell = "Aucun"; private var combo = ""
    private var dashFx = 0f; private var portalCooldown = 0f; private var spawnGrace = 6f
    private var sw = 1; private var sh = 1; private var fbo = 0; private var fboTex = 0; private var fboDepth = 0; private var fboW = 0; private var fboH = 0; private var fboOk = false
    private var fpsCount = 0; private var fpsT = 0f; private var fps = 0
    private val elNames = arrayOf("Feu", "Eau", "Air", "Terre", "Foudre", "Brume", "Lumière")
    private val elCol = arrayOf(
        floatArrayOf(1f, .30f, .06f), floatArrayOf(.10f, .55f, 1f), floatArrayOf(.80f, .90f, 1f), floatArrayOf(.72f, .50f, .26f),
        floatArrayOf(1f, .92f, .25f), floatArrayOf(.62f, .65f, .70f), floatArrayOf(1f, .97f, .82f)
    )

    private fun fb(a: FloatArray) = ByteBuffer.allocateDirect(a.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(a); position(0) }

    private fun cubeMesh(): FloatArray {
        val o = ArrayList<Float>(288)
        fun q(nx: Float, ny: Float, nz: Float, a: FloatArray, b: FloatArray, c: FloatArray, d: FloatArray) {
            val v = arrayOf(a, b, c, a, c, d); val uv = arrayOf(floatArrayOf(0f, 1f), floatArrayOf(1f, 1f), floatArrayOf(1f, 0f), floatArrayOf(0f, 1f), floatArrayOf(1f, 0f), floatArrayOf(0f, 0f))
            for (i in v.indices) { val p = v[i]; o += p[0]; o += p[1]; o += p[2]; o += nx; o += ny; o += nz; o += uv[i][0]; o += uv[i][1] }
        }
        q(0f, 0f, 1f, floatArrayOf(-1f, -1f, 1f), floatArrayOf(1f, -1f, 1f), floatArrayOf(1f, 1f, 1f), floatArrayOf(-1f, 1f, 1f))
        q(0f, 0f, -1f, floatArrayOf(1f, -1f, -1f), floatArrayOf(-1f, -1f, -1f), floatArrayOf(-1f, 1f, -1f), floatArrayOf(1f, 1f, -1f))
        q(1f, 0f, 0f, floatArrayOf(1f, -1f, 1f), floatArrayOf(1f, -1f, -1f), floatArrayOf(1f, 1f, -1f), floatArrayOf(1f, 1f, 1f))
        q(-1f, 0f, 0f, floatArrayOf(-1f, -1f, -1f), floatArrayOf(-1f, -1f, 1f), floatArrayOf(-1f, 1f, 1f), floatArrayOf(-1f, 1f, -1f))
        q(0f, 1f, 0f, floatArrayOf(-1f, 1f, 1f), floatArrayOf(1f, 1f, 1f), floatArrayOf(1f, 1f, -1f), floatArrayOf(-1f, 1f, -1f))
        q(0f, -1f, 0f, floatArrayOf(-1f, -1f, -1f), floatArrayOf(1f, -1f, -1f), floatArrayOf(1f, -1f, 1f), floatArrayOf(-1f, -1f, 1f))
        return o.toFloatArray()
    }

    private fun sphereMesh(seg: Int = 20, rings: Int = 13): FloatArray {
        val o = ArrayList<Float>()
        fun p(th: Float, ph: Float): FloatArray { val y = sin(ph); val rr = cos(ph); return floatArrayOf(rr * cos(th), y, rr * sin(th)) }
        for (j in 0 until rings) {
            val p0 = -PI.toFloat() / 2 + PI.toFloat() * j / rings; val p1 = -PI.toFloat() / 2 + PI.toFloat() * (j + 1) / rings
            for (i in 0 until seg) {
                val t0 = 2 * PI.toFloat() * i / seg; val t1 = 2 * PI.toFloat() * (i + 1) / seg
                val a = p(t0, p0); val b = p(t1, p0); val c = p(t1, p1); val d = p(t0, p1)
                val vv = arrayOf(a, b, c, a, c, d)
                val uv = arrayOf(floatArrayOf(i.toFloat() / seg, 1f - j.toFloat() / rings), floatArrayOf((i + 1f) / seg, 1f - j.toFloat() / rings), floatArrayOf((i + 1f) / seg, 1f - (j + 1f) / rings), floatArrayOf(i.toFloat() / seg, 1f - j.toFloat() / rings), floatArrayOf((i + 1f) / seg, 1f - (j + 1f) / rings), floatArrayOf(i.toFloat() / seg, 1f - (j + 1f) / rings))
                for (k in vv.indices) { val v = vv[k]; o += v[0]; o += v[1]; o += v[2]; o += v[0]; o += v[1]; o += v[2]; o += uv[k][0]; o += uv[k][1] }
            }
        }
        return o.toFloatArray()
    }

    private fun quadMesh(): FloatArray {
        val v = floatArrayOf(-.5f, -.5f, 0f, .5f, -.5f, 0f, .5f, .5f, 0f, -.5f, -.5f, 0f, .5f, .5f, 0f, -.5f, .5f, 0f)
        val o = ArrayList<Float>(48)
        for (i in 0 until 6) { o += v[i * 3]; o += v[i * 3 + 1]; o += v[i * 3 + 2]; o += 0f; o += 0f; o += 1f; o += (if (i % 3 == 1 || i == 5) 1f else 0f); o += (if (i < 3) 0f else 1f) }
        return o.toFloatArray()
    }

    private fun quadXZMesh(): FloatArray {
        val v = floatArrayOf(-.5f, 0f, -.5f, .5f, 0f, -.5f, .5f, 0f, .5f, -.5f, 0f, -.5f, .5f, 0f, .5f, -.5f, 0f, .5f)
        val o = ArrayList<Float>(48)
        for (i in 0 until 6) { o += v[i * 3]; o += v[i * 3 + 1]; o += v[i * 3 + 2]; o += 0f; o += 1f; o += 0f; o += (if (i == 1 || i == 2 || i == 5) 1f else 0f); o += (if (i >= 3) 1f else 0f) }
        return o.toFloatArray()
    }

    private fun ground(x: Float, z: Float, w: Int = realm): Float {
        return when (w) {
            0 -> sin(x * .065f) * 1.05f + cos(z * .058f) * .72f + sin((x + z) * .032f) * .42f
            1 -> sin(x * .09f + 1f) * .5f + cos(z * .085f) * .4f + sin((x - z) * .05f) * .3f
            2 -> .22f + sin(x * .11f) * .18f + cos(z * .1f) * .16f
            3 -> sin(x * .05f + 2f) * 1.6f + cos(z * .048f - .8f) * 1.2f + sin((x + z) * .03f) * .8f
            else -> sin(x * .07f) * .9f + cos(z * .066f + 1f) * .7f + sin((x - z) * .04f) * .5f
        }
    }

    private fun terrainMesh(w: Int): FloatArray {
        val o = ArrayList<Float>(); val n = 34; val step = 4f
        fun v(x: Float, z: Float): FloatArray {
            val y = ground(x, z, w); val e = .35f; val dx = ground(x + e, z, w) - ground(x - e, z, w); val dz = ground(x, z + e, w) - ground(x, z - e, w)
            var nx = -dx; var ny = e * 2; var nz = -dz; val l = max(.001f, sqrt(nx * nx + ny * ny + nz * nz)); nx /= l; ny /= l; nz /= l
            return floatArrayOf(x, y, z, nx, ny, nz, x * .09f, z * .09f)
        }
        for (iz in 0 until n) for (ix in 0 until n) {
            val x0 = (ix - n / 2) * step; val z0 = (iz - n / 2) * step; val x1 = x0 + step; val z1 = z0 + step
            val a = v(x0, z0); val b = v(x1, z0); val c = v(x1, z1); val d = v(x0, z1); for (q in arrayOf(a, b, c, a, c, d)) for (f in q) o += f
        }
        return o.toFloatArray()
    }

    private fun h2(x: Int, y: Int, s: Int): Float { var h = x * 374761393 + y * 668265263 + s * 1274126177; h = (h xor (h shr 13)) * 1274126177; return ((h xor (h shr 16)) and 0xFFFF) / 65535f }
    private fun vnoise(x: Float, y: Float, s: Int): Float {
        val xi = Math.floor(x.toDouble()).toInt(); val yi = Math.floor(y.toDouble()).toInt(); val fx = x - xi; val fy = y - yi
        val u = fx * fx * (3 - 2 * fx); val v = fy * fy * (3 - 2 * fy)
        val a = h2(xi, yi, s); val b = h2(xi + 1, yi, s); val c = h2(xi, yi + 1, s); val d = h2(xi + 1, yi + 1, s)
        return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v
    }

    private fun procTexture(size: Int, seed: Int, base: (f: Float, fx: Float, fy: Float, i: Int) -> FloatArray, repeat: Boolean = true): Int {
        val px = IntArray(size * size)
        for (yy in 0 until size) {
            val fy = yy.toFloat() / size
            for (xx in 0 until size) {
                val fx = xx.toFloat() / size
                var f = 0f; var amp = 1f; var tot = 0f; var sc = 5f; var o = 0
                while (o < 5) { f += vnoise(fx * sc + o * 17.3f, fy * sc + o * 9.7f, seed + o * 31) * amp; tot += amp; amp *= .5f; sc *= 2.13f; o++ }
                f /= tot
                val c = base(f, fx, fy, xx * 131 + yy * 17)
                val r = (c[0] * 255f).toInt().coerceIn(0, 255); val g = (c[1] * 255f).toInt().coerceIn(0, 255); val b = (c[2] * 255f).toInt().coerceIn(0, 255)
                px[yy * size + xx] = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            }
        }
        return uploadTexture(Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888), repeat)
    }

    private fun uploadTexture(bmp: Bitmap, repeat: Boolean): Int {
        val ids = IntArray(1); GLES20.glGenTextures(1, ids, 0); val id = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, if (repeat) GLES20.GL_REPEAT else GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, if (repeat) GLES20.GL_REPEAT else GLES20.GL_CLAMP_TO_EDGE)
        val maxA = FloatArray(1); GLES20.glGetFloatv(0x84FF, maxA, 0); GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, 0x84FE, min(8f, maxA[0]))
        android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0); GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D); bmp.recycle(); return id
    }

    private fun ensureFbo(w: Int, h: Int) {
        val scale = if (quality >= 2) 1.667f else 1.333f
        val wantW = (w * scale).toInt().coerceIn(w, 3200); val wantH = (h * scale).toInt().coerceIn(h, 2000)
        if (fboOk && fboW == wantW && fboH == wantH) return
        if (fbo != 0) { GLES20.glDeleteFramebuffers(1, intArrayOf(fbo), 0); GLES20.glDeleteRenderbuffers(1, intArrayOf(fboDepth), 0); GLES20.glDeleteTextures(1, intArrayOf(fboTex), 0); fbo = 0; fboOk = false }
        val t = IntArray(1); GLES20.glGenTextures(1, t, 0); fboTex = t[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, wantW, wantH, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        val r = IntArray(1); GLES20.glGenRenderbuffers(1, r, 0); fboDepth = r[0]
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, fboDepth); GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, wantW, wantH)
        val f = IntArray(1); GLES20.glGenFramebuffers(1, f, 0); fbo = f[0]
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTex, 0)
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, fboDepth)
        fboOk = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) == GLES20.GL_FRAMEBUFFER_COMPLETE
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        if (fboOk) { fboW = wantW; fboH = wantH } else {
            GLES20.glDeleteFramebuffers(1, intArrayOf(fbo), 0); GLES20.glDeleteRenderbuffers(1, intArrayOf(fboDepth), 0); GLES20.glDeleteTextures(1, intArrayOf(fboTex), 0); fbo = 0
        }
    }
    override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, cfg: javax.microedition.khronos.egl.EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); GLES20.glEnable(GLES20.GL_BLEND); GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA); GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glClearColor(.05f, .07f, .10f, 1f)
        val vs = """
attribute vec3 a;attribute vec3 n;attribute vec2 uv;
uniform mat4 m;uniform mat4 model;uniform float time;uniform float wind;uniform float water;
varying vec3 N;varying vec3 W;varying vec2 T;
void main(){
 vec3 p=a;
 if(water>0.5){p.y+=sin(p.x*2.1+time*1.7)*0.10+cos(p.z*1.7+time*1.3)*0.08;}
 else if(wind>0.5){float h=clamp((p.y+1.0)/3.0,0.0,1.0);p.x+=sin(time*1.6+p.z*0.7+p.y*0.5)*0.12*h;p.z+=cos(time*1.3+p.x*0.6)*0.10*h;}
 vec4 wp=model*vec4(p,1.0);W=wp.xyz;N=normalize((model*vec4(n,0.0)).xyz);T=uv;gl_Position=m*vec4(p,1.0);
}"""
        val fs = """
precision mediump float;
uniform vec4 c;uniform vec3 eye;uniform float glow;uniform float fog;uniform vec3 fogColor;
uniform float useTex;uniform float sky;uniform float water;uniform float day;uniform vec3 sunDir;uniform float time;
uniform sampler2D tex;
varying vec3 N;varying vec3 W;varying vec2 T;
void main(){
 vec4 tx=texture2D(tex,T);
 if(sky>0.5){float d=max(dot(normalize(W-eye),normalize(sunDir)),0.0);float h=pow(d,340.0)*2.2+pow(d,24.0)*0.35;vec3 sc=tx.rgb*c.rgb+h*vec3(1.0,0.95,0.8)*day+vec3(0.0);gl_FragColor=vec4(sc,1.0);return;}
 vec3 base=c.rgb;
 if(useTex>0.5){base*=mix(vec3(1.0),tx.rgb*1.5,0.94);}
 vec3 nn=normalize(N);
 float d=max(dot(nn,normalize(sunDir)),0.0);
 float amb=mix(0.16,0.42,day);
 vec3 col=base*(amb+d*mix(0.35,0.85,day));
 if(water>0.5){float sp=pow(max(dot(reflect(-normalize(sunDir),nn),normalize(eye-W)),0.0),60.0)*day;col+=vec3(sp*0.7,sp*0.8,sp*0.9);col=mix(col,vec3(0.35,0.65,0.85),0.25+0.2*sin(T.x*40.0+T.y*40.0+time*2.0));}
 col+=base*glow*0.6;
 float f=1.0-exp(-pow(length(W-eye)*fog,2.0));
 gl_FragColor=vec4(mix(col,fogColor,clamp(f,0.0,0.88)),c.a*(useTex>0.5?tx.a:1.0));
}"""
        fun sh(type: Int, s: String): Int { val v = GLES20.glCreateShader(type); GLES20.glShaderSource(v, s); GLES20.glCompileShader(v); return v }
        val sv = sh(GLES20.GL_VERTEX_SHADER, vs); val sf = sh(GLES20.GL_FRAGMENT_SHADER, fs)
        program = GLES20.glCreateProgram(); GLES20.glAttachShader(program, sv); GLES20.glAttachShader(program, sf); GLES20.glLinkProgram(program); GLES20.glUseProgram(program)
        aPos = GLES20.glGetAttribLocation(program, "a"); aNor = GLES20.glGetAttribLocation(program, "n"); aUv = GLES20.glGetAttribLocation(program, "uv")
        uMvp = GLES20.glGetUniformLocation(program, "m"); uModel = GLES20.glGetUniformLocation(program, "model"); uColor = GLES20.glGetUniformLocation(program, "c")
        uEye = GLES20.glGetUniformLocation(program, "eye"); uGlow = GLES20.glGetUniformLocation(program, "glow"); uFog = GLES20.glGetUniformLocation(program, "fog")
        uFogColor = GLES20.glGetUniformLocation(program, "fogColor"); uUseTex = GLES20.glGetUniformLocation(program, "useTex"); uSky = GLES20.glGetUniformLocation(program, "sky")
        uTex = GLES20.glGetUniformLocation(program, "tex"); uTime = GLES20.glGetUniformLocation(program, "time"); uWind = GLES20.glGetUniformLocation(program, "wind")
        uWater = GLES20.glGetUniformLocation(program, "water"); uDay = GLES20.glGetUniformLocation(program, "day"); uSunDir = GLES20.glGetUniformLocation(program, "sunDir")
        val cm = cubeMesh(); cube = fb(cm); cubeCount = cm.size / 8
        val sm = sphereMesh(); sphere = fb(sm); sphereCount = sm.size / 8
        val qm = quadMesh(); quad = fb(qm)
        val qx = quadXZMesh(); quadXZ = fb(qx)
        for (w in 0..4) { val tm = terrainMesh(w); terrain[w] = fb(tm); terrainCount[w] = tm.size / 8 }
        val S = 1024
        texGrass = procTexture(S, 11) { f, _, _, _ -> floatArrayOf(.16f + .28f * f, .34f + .34f * f, .10f + .18f * f) }
        texLava = procTexture(S, 23) { f, fx, fy, _ -> val l = .5f + .5f * sin(fx * 40 + f * 9) * sin(fy * 34 - f * 7); floatArrayOf(.25f + .75f * l * f + .25f * f, .08f + .30f * l * f * f, .02f) }
        texSand = procTexture(S, 31) { f, _, _, _ -> floatArrayOf(.55f + .30f * f, .43f + .25f * f, .24f + .16f * f) }
        texRock = procTexture(S, 37) { f, _, _, _ -> val g = .30f + .34f * f; floatArrayOf(g, g * 1.02f, g * 1.06f) }
        texObsidian = procTexture(S, 41) { f, _, _, _ -> val g = .05f + .13f * f; floatArrayOf(g, g * .95f, g * 1.5f) }
        texWood = procTexture(S, 53) { f, fx, _, _ -> val ring = .5f + .5f * sin((fx * 24 + f * 5) * 3.14159f); floatArrayOf(.30f + .22f * ring + .08f * f, .20f + .14f * ring + .05f * f, .11f + .08f * ring) }
        texLeaves = procTexture(S, 71) { f, _, _, _ -> floatArrayOf(.08f + .24f * f, .26f + .34f * f, .07f + .16f * f) }
        texMoss = procTexture(S, 89) { f, _, _, _ -> floatArrayOf(.12f + .26f * f, .28f + .30f * f, .10f + .18f * f) }
        texWater = procTexture(S, 97) { f, fx, fy, _ -> val w = .5f + .5f * sin(fx * 30 + f * 6) * sin(fy * 26 - f * 5); floatArrayOf(.10f + .25f * w, .32f + .38f * w, .55f + .35f * w) }
        texSnow = procTexture(S, 103) { f, _, _, _ -> val g = .82f + .16f * f; floatArrayOf(g, g, g * 1.02f) }
        texPortal = procTexture(S, 107) { f, fx, fy, _ -> val cx = (fx - .5f) * 2; val cy = (fy - .5f) * 2; val d = sqrt(cx * cx + cy * cy); val ring = .5f + .5f * sin(d * 22 - f * 10); floatArrayOf(.2f + .8f * ring * f, .1f + .5f * ring, .9f * f + .1f) }
        texCrystal = procTexture(S, 113) { f, fx, fy, _ -> val d = abs(fx - .5f) + abs(fy - .5f); val c = (.4f + .6f * (1 - d * 2).coerceIn(0f, 1f)); floatArrayOf(.5f * c + .4f * f, .8f * c + .3f * f, 1f * c + .2f) }
        buildWorlds()
        loadPrefs()
    }

    private fun buildWorlds() {
        val rng = Rng(1234)
        for (w in 0..4) {
            val p = props[w]; p.clear(); val m = mobs[w]; m.clear(); crystals[w].clear()
            if (w == 0) {
                for (i in 0 until 14) {
                    val x = (rng.next() - .5f) * 110; val z = (rng.next() - .5f) * 110
                    if (hypot(x, z - 10) < 6) continue
                    val h = 2.6f + rng.next() * 1.6f
                    p += ALProp(x, 0f, z, .30f, h * .55f, .30f, 0f, 0, .8f, .7f, .55f)
                    p += ALProp(x, 0f, z, 1.4f, 1f, 1.4f, 0f, 1, .4f, .7f, .35f)
                }
                for (k in 0 until 3) {
                    val a = k * 2 * PI.toFloat() / 3 + .5f
                    crystals[0] += ALCrystal(cos(a) * 30f, sin(a) * 30f, false)
                }
                val eb = arrayOf(floatArrayOf(0f, 0f, 300f, 300f, 2f, false), floatArrayOf(-14f, 20f, 80f, 80f, .6f, true), floatArrayOf(14f, 20f, 80f, 80f, .6f, true), floatArrayOf(20f, -18f, 80f, 80f, .6f, true), floatArrayOf(-20f, -18f, 80f, 80f, .6f, true))
                for (e in eb) m += ALMob(e[0], e[1], e[4] > 1f, e[5], e[2], e[3], rng.next() * 9, 0f, 0f, 0f)
            } else {
                val n = 40 + w * 14
                for (i in 0 until n) {
                    val x = (rng.next() - .5f) * 110; val z = (rng.next() - .5f) * 110
                    if (hypot(x, z - 10) < 6) continue
                    val kind = when (w) {
                        1 -> if (rng.next() < .55) 2 else if (rng.next() < .5) 3 else 4
                        2 -> if (rng.next() < .4) 5 else 0
                        3 -> if (rng.next() < .3) 6 else 0
                        else -> if (rng.next() < .6) 7 else 8
                    }
                    val s = .6f + rng.next() * 1.8f
                    val c = when (w) { 1 -> floatArrayOf(.9f, .3f, .1f); 2 -> floatArrayOf(.2f, .5f, .8f); 3 -> floatArrayOf(.85f, .9f, .95f); else -> floatArrayOf(.55f, .45f, .3f) }
                    p += ALProp(x, 0f, z, s, s * (0.7f + rng.next()), s, rng.next() * 6.28f, kind, c[0], c[1], c[2])
                }
                for (k in 0 until 3) {
                    val a = rng.next() * 6.28f; val r = 24f + rng.next() * 14f
                    crystals[w] += ALCrystal(cos(a) * r, sin(a) * r, false)
                }
                val count = 4 + w
                for (i in 0 until count) {
                    val a = rng.next() * 6.28f; val r = 18f + rng.next() * 25f
                    m += ALMob(cos(a) * r, sin(a) * r, i == 0 && w > 0, w == 3, if (i == 0 && w > 0) 320f else 85f + w * 15f, 1f, rng.next() * 9, 0f, 0f, 0f)
                }
                mobs[w].forEach { it.hpMax = it.hp }
            }
        }
    }

    private fun loadPrefs() {
        try {
            realm = prefs.getInt("realm", 0); px = prefs.getFloat("px", 0f); pz = prefs.getFloat("pz", 10f)
            yaw = prefs.getFloat("yaw", 0f); life = prefs.getFloat("life", 100f); mana = prefs.getFloat("mana", 100f)
            score = prefs.getInt("score", 0); kills = prefs.getInt("kills", 0); dayT = prefs.getFloat("day", .55f); quality = prefs.getInt("quality", 1).coerceIn(0, 2)
            for (w in 0..4) { purified[w] = prefs.getBoolean("pur$w", false); for (i in crystals[w].indices) crystals[w][i].taken = prefs.getBoolean("cr${w}_$i", false) }
            py = ground(px, pz) + 1.25f
        } catch (_: Throwable) {}
    }

    fun savePrefs() {
        try {
            prefs.edit().apply {
                putInt("realm", realm); putFloat("px", px); putFloat("pz", pz); putFloat("yaw", yaw); putFloat("life", life); putFloat("mana", mana)
                putInt("score", score); putInt("kills", kills); putFloat("day", dayT); putInt("quality", quality)
                for (w in 0..4) { putBoolean("pur$w", purified[w]); for (i in crystals[w].indices) putBoolean("cr${w}_$i", crystals[w][i].taken) }
                apply()
            }
        } catch (_: Throwable) {}
    }

    fun resetSave() {
        realm = 0; px = 0f; pz = 10f; yaw = 0f; life = 100f; mana = 100f; score = 0; kills = 0; dayT = .55f; flying = false; sel0 = -1; sel1 = -1; spell = "Aucun"; combo = ""
        for (w in 0..4) { purified[w] = false; crystals[w].forEach { it.taken = false }; mobs[w].forEach { it.hp = it.hpMax; it.respawn = 0f } }
        py = ground(px, pz) + 1.25f; savePrefs()
    }

    override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, w: Int, h: Int) {
        sw = max(1, w); sh = max(1, h)
        Matrix.perspectiveM(proj, 0, 58f, w.toFloat() / max(1, h), .12f, 220f)
        Matrix.orthoM(ortho, 0, 0f, w.toFloat(), h.toFloat(), 0f, -1f, 1f)
        ensureFbo(sw, sh)
    }

    fun zoom(v: Float) { camZoom = (camZoom + v).coerceIn(6.5f, 18f) }
    private fun comboName(a: Int, b: Int): String {
        val x = min(a, b); val y = max(a, b)
        return when (x * 10 + y) {
            0 -> "Boule de feu"; 1 -> "Vapeur curative"; 2 -> "Tempête de braises"; 3 -> "Lave"; 4 -> "Orage de flammes"; 5 -> "Fumée aveuglante"; 6 -> "Aube ardente"
            11 -> "Torrent"; 12 -> "Givre"; 13 -> "Boue entravante"; 14 -> "Tempête électrique"; 15 -> "Brume glacée"; 16 -> "Eau lumineuse"
            22 -> "Cyclone"; 23 -> "Tempête de sable"; 24 -> "Cyclone chargé"; 25 -> "Nuée cendreuse"; 26 -> "Rayon zénithal"
            33 -> "Rocher vivant"; 34 -> "Séisme fulgurant"; 35 -> "Poussière voilée"; 36 -> "Éclat tellurique"
            44 -> "Éclair en cône"; 45 -> "Brume électrisée"; 46 -> "Éclair sacré"
            55 -> "Brume épaisse"; 56 -> "Voile radieux"
            66 -> "Nova radieuse"
            else -> "Arcane"
        }
    }

    fun cycleElement(d: Int) { sel0 = ((sel0 + d) % 7 + 7) % 7; selCount = if (sel0 >= 0) 1 else 0; combo = if (sel0 >= 0) elNames[sel0] else ""; spell = if (sel0 >= 0) "${elNames[sel0]} prêt" else "Aucun" }

    fun element(i: Int) {
        if (selCount == 0) { sel0 = i; selCount = 1; combo = elNames[i]; spell = "${elNames[i]} prêt" }
        else if (selCount == 1 && sel0 == i) { selCount = 0; sel0 = -1; combo = ""; cast(i, i) }
        else { val a = sel0; selCount = 0; sel0 = -1; combo = ""; cast(a, i) }
    }

    fun space() { if (selCount >= 1) cast(sel0, sel0) else dash() }

    fun castAction() { if (selCount >= 1) cast(sel0, sel0) else { val c = cycleAuto(); cast(c, c) } }

    private var autoI = 0
    private fun cycleAuto(): Int { autoI = (autoI + 1) % 7; return autoI }

    private fun cast(a: Int, b: Int) {
        if (mana < 8) { spell = "Mana insuffisant"; return }
        mana = max(0f, mana - 8f)
        spell = comboName(a, b)
        val col = when {
            a == 0 && b == 0 -> elCol[0]; a == 1 && b == 1 -> elCol[1]; a == 2 && b == 2 -> elCol[2]; a == 3 && b == 3 -> elCol[3]
            a == 4 && b == 4 -> elCol[4]; a == 5 && b == 5 -> elCol[5]; a == 6 && b == 6 -> elCol[6]
            else -> floatArrayOf((elCol[a][0] + elCol[b][0]) / 2, (elCol[a][1] + elCol[b][1]) / 2, (elCol[a][2] + elCol[b][2]) / 2)
        }
        val fx = px + sin(yaw) * 2f; val fz = pz - cos(yaw) * 2f
        burst(fx, py + .7f, fz, col[0], col[1], col[2], if ((a == 4 || b == 4) || (a == 6 && b == 6)) 60 else 42, .06f + (if (a == 3 || b == 3) .04f else 0f), 0f)
        val foudre = a == 4 || b == 4; val brume = a == 5 || b == 5; val lumiere = a == 6 || b == 6; val terre = a == 3 || b == 3
        for (m in mobs[realm]) if (m.hp > 0) {
            val d = hypot(m.x - px, m.z - pz)
            val dirDot = ((m.x - px) * sin(yaw) - (m.z - pz) * cos(yaw)) / max(.1f, d)
            val touch = if (foudre) d < 24f && d > .1f && dirDot > .4f else d < (if (terre) 11f else 9f)
            if (touch) {
                m.hp -= when { foudre -> 52f; terre -> 44f; lumiere -> 14f; brume -> 22f; else -> 36f }
                m.hitT = .3f; if (brume) m.cool = 2.5f
                burst(m.x, ground(m.x, m.z, realm) + 1.2f, m.z, 1f, .3f, .2f, 10, .05f, .5f)
            }
        }
        if (lumiere) life = min(100f, life + 16f)
    }

    private fun burst(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, count: Int, size: Float, grav: Float) {
        repeat(count) {
            val a = it * 2.399f + t; val sp = .8f + (it % 5) * .2f
            sparks += ALSpark(x, y, z, cos(a) * sp, ((it % 7) - 2) * .24f, sin(a) * sp, .6f + (it % 4) * .09f, .8f, r, g, b, size + (it % 3) * .025f, grav)
        }
        while (sparks.size > 220) sparks.removeAt(0)
    }

    fun dash() {
        var dx = sin(yaw) * -ky + cos(yaw) * kx; var dz = -cos(yaw) * -ky + sin(yaw) * kx
        if (hypot(dx, dz) < .18f) { dx = sin(yaw); dz = -cos(yaw) }
        val l = max(.01f, hypot(dx, dz)); dx /= l; dz /= l
        px = (px + dx * 7.2f).coerceIn(-62f, 62f); pz = (pz + dz * 7.2f).coerceIn(-62f, 62f); dashFx = .55f
        burst(px, py, pz, .9f, .8f, .4f, 22, .05f, 0f)
    }

    fun toggleFlight() {
        flying = !flying; flyBoost = if (flying) 7.5f else 0f; spell = if (flying) "Transplanage" else "Atterrissage"
        burst(px, py, pz, .12f, .12f, .16f, 18, .06f, 0f)
    }

    private fun realmAt(x: Float, z: Float): Int {
        if (realm == 0) {
            if (hypot(x - 26f, z) < 3f) return 1
            if (hypot(x + 26f, z) < 3f) return 2
            if (hypot(x, z - 26f) < 3f) return 3
            if (hypot(x, z + 26f) < 3f) return 4
        } else if (hypot(x, z + 20f) < 3f) return 0
        return -1
    }

    private fun checkPortals() {
        if (portalCooldown > 0) return
        val nr = realmAt(px, pz)
        if (nr >= 0) {
            realm = nr; px = 0f; pz = if (nr == 0) 10f else 12f; py = ground(px, pz) + 1.25f
            portalCooldown = 2.2f; spawnGrace = 4f; spell = realmNames[nr]
            savePrefs()
        }
    }

    private fun update(dt: Float) {
        if (paused) return
        t += dt; portalCooldown = max(0f, portalCooldown - dt); spawnGrace = max(0f, spawnGrace - dt); mana = min(100f, mana + 5.5f * dt)
        dayT = (dayT + dt / 240f) % 1f
        val day = (sin((dayT - .25f) * 2 * PI.toFloat()) * .5f + .5f)
        var mx = kx; var my = ky
        if (keys[0]) my = -1f; if (keys[1]) my = 1f; if (keys[2]) mx = -1f; if (keys[3]) mx = 1f
        if (mlx != 0f || mly != 0f) { yaw += mlx * .0042f; pitch = (pitch - mly * .0032f).coerceIn(-.25f, .66f); mlx = 0f; mly = 0f }
        yaw += lx * 1.85f * dt; pitch = (pitch - ly * .8f * dt).coerceIn(-.25f, .66f)
        val f = -my; val s = mx; val dx = sin(yaw) * f + cos(yaw) * s; val dz = -cos(yaw) * f + sin(yaw) * s
        val speed = if (flying) 8.2f else 5.8f
        px = (px + dx * speed * dt).coerceIn(-64f, 64f); pz = (pz + dz * speed * dt).coerceIn(-64f, 64f)
        if (flying) { flyBoost = max(0f, flyBoost - 14f * dt); py += (-ly * 5.3f + flyBoost) * dt; py = py.coerceIn(1.1f, 38f) } else py = ground(px, pz) + 1.25f
        if (dashFx > 0) dashFx = max(0f, dashFx - dt)
        val it = sparks.iterator(); while (it.hasNext()) { val s0 = it.next(); s0.x += s0.vx * dt; s0.y += s0.vy * dt; s0.z += s0.vz * dt; s0.vy -= s0.grav * 1.6f * dt; s0.life -= dt; if (s0.life <= 0 || s0.y < ground(s0.x, s0.z, realm) - .5f) it.remove() }
        val nightMul = if (day < .18f) 1.5f else 1f
        for (m in mobs[realm]) {
            if (m.hp <= 0) {
                if (m.respawn > 0) { m.respawn -= dt; if (m.respawn <= 0 && !purified[realm]) { val a = m.phase * 4.1f; m.x = cos(a) * 28f; m.z = sin(a) * 26f; m.hp = m.hpMax } }
                continue
            }
            m.phase += dt * .62f; m.cool = max(0f, m.cool - dt); m.hitT = max(0f, m.hitT - dt)
            val d = hypot(px - m.x, pz - m.z)
            if (d < (if (m.ranged) 22f else 12f) && spawnGrace <= 0 && !purified[realm]) {
                val ux = (px - m.x) / max(.1f, d); val uz = (pz - m.z) / max(.1f, d)
                if (m.ranged) {
                    if (d > 9f) { m.x += ux * 1.1f * dt; m.z += uz * 1.1f * dt }
                    if (d < 15f && m.cool <= 0) { m.cool = 2.2f; val sp = m.boss; sparks += ALSpark(m.x, ground(m.x, m.z, realm) + 1.6f, m.z, ux * (if (sp) 7f else 9f), 2.5f, uz * (if (sp) 7f else 9f), 1.1f, 1.1f, .9f, .3f, .25f, .12f, -.4f); if (d < 20f && hypot(px - (m.x + ux * 9f * .9f), pz - (m.z + uz * 9f * .9f)) < 1.6f) { life -= (if (m.boss) 9f else 5f) * nightMul; m.hitT = .1f } }
                } else {
                    m.x += ux * (if (m.boss) .9f else 1.45f) * dt; m.z += uz * (if (m.boss) .9f else 1.45f) * dt
                    if (d < 2.2f && m.cool <= 0) { life -= (if (m.boss) 12f else 3.5f) * nightMul; m.cool = 1.25f; burst(px, py + .5f, pz, .65f, .05f, .05f, 7, .05f, 0f) }
                }
            } else { m.x += cos(m.phase) * .3f * dt; m.z += sin(m.phase * .91f) * .3f * dt }
            if (m.hp <= 0) {
                kills++; score += if (m.boss) 150 else 25; m.respawn = 25f
                burst(m.x, ground(m.x, m.z, realm) + 1.2f, m.z, 1f, .85f, .3f, if (m.boss) 60 else 24, .07f, 0f)
                if (rng0(m.phase) < .3f) sparks += ALSpark(m.x, ground(m.x, m.z, realm) + .6f, m.z, 0f, 2f, 0f, 6f, 6f, .3f, 1f, .3f, .1f, 0f)
                if (rng0(m.phase + 1) < .3f) sparks += ALSpark(m.x, ground(m.x, m.z, realm) + .6f, m.z, 0f, 2f, 0f, 6f, 6f, .2f, .4f, 1f, .1f, 0f)
            }
        }
        for (c in crystals[realm]) if (!c.taken && hypot(px - c.x, pz - c.z) < 2f) {
            c.taken = true; score += 60; mana = min(100f, mana + 30f); life = min(100f, life + 25f)
            burst(c.x, ground(c.x, c.z, realm) + 1.6f, c.z, .6f, .9f, 1f, 40, .07f, 0f); spell = "Cristal purifié !"
            if (crystals[realm].all { it.taken }) { purified[realm] = true; score += 200; spell = "ROYAUME PURIFIÉ ✦"; savePrefs() }
            savePrefs()
        }
        if (life <= 0) { life = 100f; mana = max(mana, 60f); px = 0f; pz = 10f; realm = 0; py = ground(px, pz) + 1.25f; spawnGrace = 6f; spell = "Réapparition au Nexus"; savePrefs() }
        checkPortals()
        hudT += dt; if (hudT > .18f) { hudT = 0f; val boss = mobs[realm].firstOrNull { it.boss && it.hp > 0 }; hud(ALHud(life.roundToInt().coerceIn(0, 100), mana.roundToInt().coerceIn(0, 100), realmNames[realm], flying, crystals[realm].count { it.taken }, crystals[realm].size, mobs[realm].count { it.hp > 0 && !purified[realm] }, purified[realm], spell, combo, if (boss != null) boss.hp.roundToInt() else -1, score, day < .18f, fps, arrayOf("Natif", "Élevé 2560×1600", "Ultra 3200×2000")[quality])) }
        fpsCount++; fpsT += dt; if (fpsT > .5f) { fps = (fpsCount / fpsT).roundToInt(); fpsCount = 0; fpsT = 0f }
        if ((t * 10).toInt() != ((t - dt) * 10).toInt()) savePrefs()
    }

    private fun rng0(seed: Float): Float { val x = sin(seed * 127.1f) * 43758.5453f; return x - floor(x) }

    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
        val now = System.nanoTime(); val dt = if (last == 0L) 0f else min(.05f, (now - last) / 1_000_000_000f); last = now
        update(dt)
        val target = if (fboOk) fbo else 0
        if (fboOk) { GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo); GLES20.glViewport(0, 0, fboW, fboH) } else { GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0); GLES20.glViewport(0, 0, sw, sh) }
        val day = (sin((dayT - .25f) * 2 * PI.toFloat()) * .5f + .5f)
        val night = day < .22f
        val clear = when (realm) {
            0 -> if (night) floatArrayOf(.012f, .02f, .045f) else floatArrayOf(.13f, .20f, .28f)
            1 -> if (night) floatArrayOf(.05f, .015f, .01f) else floatArrayOf(.30f, .13f, .06f)
            2 -> if (night) floatArrayOf(.01f, .03f, .07f) else floatArrayOf(.10f, .22f, .38f)
            3 -> if (night) floatArrayOf(.02f, .025f, .05f) else floatArrayOf(.55f, .68f, .82f)
            else -> if (night) floatArrayOf(.03f, .02f, .015f) else floatArrayOf(.32f, .24f, .15f)
        }
        GLES20.glClearColor(clear[0], clear[1], clear[2], 1f); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)
        val eyeY = py + 2.4f + sin(pitch) * 3.2f; val ex = px - sin(yaw) * cos(pitch) * camZoom; val ez = pz + cos(yaw) * cos(pitch) * camZoom
        Matrix.setLookAtM(view, 0, ex, eyeY, ez, px, py + .75f, pz, 0f, 1f, 0f); Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
        GLES20.glUniform3f(uEye, ex, eyeY, ez)
        GLES20.glUniform1f(uFog, if (realm == 2) .012f else .0085f)
        val fc = when (realm) { 0 -> floatArrayOf(.22f, .28f, .31f); 1 -> floatArrayOf(.30f, .16f, .10f); 2 -> floatArrayOf(.12f, .26f, .40f); 3 -> floatArrayOf(.60f, .70f, .80f); else -> floatArrayOf(.32f, .24f, .16f) }
        GLES20.glUniform3f(uFogColor, fc[0] * (.35f + .65f * day), fc[1] * (.35f + .65f * day), fc[2] * (.35f + .65f * day))
        GLES20.glUniform1i(uTex, 0); GLES20.glUniform1f(uTime, t); GLES20.glUniform1f(uDay, day)
        val sunA = (dayT - .25f) * 2 * PI.toFloat(); GLES20.glUniform3f(uSunDir, cos(sunA) * .5f, sin(sunA), .35f)
        drawSky(ex, eyeY, ez, day)
        drawTerrain()
        drawWater()
        drawProps()
        drawCrystals()
        drawPortals()
        drawMobs()
        drawPlayer()
        drawBlades()
        drawSparks()
        if (fboOk) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0); GLES20.glViewport(0, 0, sw, sh)
            GLES20.glDisable(GLES20.GL_DEPTH_TEST); GLES20.glDisable(GLES20.GL_CULL_FACE)
            Matrix.setIdentityM(tmpM, 0); Matrix.multiplyMM(mvp, 0, ortho, 0, tmpM, 0)
            GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0); GLES20.glUniformMatrix4fv(uModel, 1, false, tmpM, 0)
            GLES20.glUniform4f(uColor, 1f, 1f, 1f, 1f); GLES20.glUniform1f(uGlow, 0f); GLES20.glUniform1f(uUseTex, 1f); GLES20.glUniform1f(uSky, 0f); GLES20.glUniform1f(uFog, 0f); GLES20.glUniform1f(uWater, 0f); GLES20.glUniform1f(uWind, 0f)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex)
            bindMesh(quad); GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST); GLES20.glEnable(GLES20.GL_CULL_FACE)
        }
    }
    private fun bindMesh(buf: FloatBuffer) {
        buf.position(0); GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 32, buf); GLES20.glEnableVertexAttribArray(aPos)
        buf.position(3); GLES20.glVertexAttribPointer(aNor, 3, GLES20.GL_FLOAT, false, 32, buf); GLES20.glEnableVertexAttribArray(aNor)
        buf.position(6); GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 32, buf); GLES20.glEnableVertexAttribArray(aUv)
    }

    private fun drawMesh(buf: FloatBuffer, count: Int, x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, rot: Float, r: Float, g: Float, b: Float, a: Float = 1f, tex: Int = 0, glow: Float = 0f, sky: Float = 0f, wind: Float = 0f, water: Float = 0f) {
        Matrix.setIdentityM(model, 0); Matrix.translateM(model, 0, x, y, z)
        if (rot != 0f) Matrix.rotateM(model, 0, rot * 57.29578f, 0f, 1f, 0f)
        Matrix.scaleM(model, 0, sx, sy, sz); Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0); GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES20.glUniform4f(uColor, r, g, b, a); GLES20.glUniform1f(uGlow, glow); GLES20.glUniform1f(uUseTex, if (tex != 0) 1f else 0f); GLES20.glUniform1f(uSky, sky)
        GLES20.glUniform1f(uWind, wind); GLES20.glUniform1f(uWater, water)
        if (tex != 0) { GLES20.glActiveTexture(GLES20.GL_TEXTURE0); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex) }
        bindMesh(buf); GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)
    }

    private fun box(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, rot: Float = 0f, r: Float, g: Float, b: Float, tex: Int = 0, glow: Float = 0f, a: Float = 1f, wind: Float = 0f) = drawMesh(cube, cubeCount, x, y, z, sx, sy, sz, rot, r, g, b, a, tex, glow, 0f, wind)
    private fun sph(x: Float, y: Float, z: Float, s: Float, r: Float, g: Float, b: Float, tex: Int = 0, glow: Float = 0f, a: Float = 1f) = drawMesh(sphere, sphereCount, x, y, z, s, s, s, 0f, r, g, b, a, tex, glow)

    private fun drawSky(ex: Float, ey: Float, ez: Float, day: Float) {
        GLES20.glDepthMask(false); GLES20.glDisable(GLES20.GL_CULL_FACE)
        val c = floatArrayOf(.35f + .55f * day, .45f + .45f * day, .62f + .38f * day)
        drawMesh(sphere, sphereCount, ex, ey, ez, 105f, 105f, 105f, 0f, c[0], c[1], c[2], 1f, 0, 0f, 1f)
        GLES20.glEnable(GLES20.GL_CULL_FACE); GLES20.glDepthMask(true); GLES20.glUniform1f(uSky, 0f)
        val sa = (dayT - .25f) * 2 * PI.toFloat()
        val sx = cos(sa) * 90f; val sy = sin(sa) * 90f
        if (day > .02f) sph(ex + sx, ey + sy, ez + 30f, 4.5f, 1f, .95f, .75f, 0, 1.2f)
        else sph(ex - sx, ey - sy, ez + 30f, 3.2f, .85f, .88f, 1f, 0, .8f)
        if (day < .5f) {
            GLES20.glDepthMask(false)
            val rng = Rng(777)
            repeat(90) { val a = rng.next() * 6.283f; val e = rng.next() * 1.2f + .2f; val rr = 100f; val st = ex + cos(a) * cos(e) * rr; val syy = ey + sin(e) * rr; val sz = ez + sin(a) * cos(e) * rr; val tw = .5f + .5f * sin(t * 3f + a * 40f); sph(st, syy, sz, .35f + tw * .3f, tw, tw, tw, 0, (1f - day * 1.8f).coerceIn(0f, 1f) * .9f) }
            GLES20.glDepthMask(true)
        }
    }

    private fun drawTerrain() {
        val tint = when (realm) { 0 -> floatArrayOf(.75f, .90f, .72f); 1 -> floatArrayOf(1f, .62f, .45f); 2 -> floatArrayOf(.65f, .85f, .95f); 3 -> floatArrayOf(1f, 1f, 1.05f); else -> floatArrayOf(.85f, .75f, .6f) }
        val tex = when (realm) { 0 -> texGrass; 1 -> texLava; 2 -> texSand; 3 -> texSnow; else -> texRock }
        val b = terrain[realm] ?: return
        drawMesh(b, terrainCount[realm], 0f, 0f, 0f, 1f, 1f, 1f, 0f, tint[0], tint[1], tint[2], 1f, tex)
    }

    private fun drawWater() {
        if (realm != 2 && realm != 0) return
        val w = if (realm == 2) .02f else -.35f
        drawMesh(quadXZ, 6, 0f, w, 0f, 130f, 1f, 130f, 0f, .5f, .8f, 1f, .55f, texWater, .35f, 0f, 0f, 1f)
    }

    private fun drawProps() {
        val list = props[realm]
        for (p in list) {
            if (hypot(p.x - px, p.z - pz) > 85f) continue
            val gy = ground(p.x, p.z, realm)
            when (p.kind) {
                0 -> { box(p.x, gy + p.sy, p.z, p.sx, p.sy, p.sz, p.rot, p.r, p.g, p.b, texWood); sph(p.x, gy + p.sy * 2 + 1f, p.z, 1.3f, p.r * .8f, p.g, p.b * .8f, texLeaves, 0f, 1f, 1f) }
                1 -> { sph(p.x, gy + p.sy, p.z, p.sx, p.r, p.g, p.b, texLeaves, 0f, 1f, 1f); box(p.x, gy + p.sy * .4f, p.z, p.sx * .3f, p.sy * .8f, p.sz * .3f, 0f, .5f, .4f, .25f, texWood) }
                2 -> { box(p.x, gy + p.sy * .5f, p.z, p.sx, p.sy, p.sz, p.rot, p.r, p.g, p.b, texObsidian, .25f + .2f * sin(t * 3f + p.x)) }
                3 -> { box(p.x, gy + p.sy * .5f, p.z, p.sx, p.sy, p.sz, p.rot, p.r, p.g, p.b, texLava, .8f + .2f * sin(t * 4f + p.z)); sph(p.x, gy + p.sy + .3f, p.z, .25f, 1f, .5f, .1f, 0, 1f) }
                4 -> { box(p.x, gy + p.sy * .5f, p.z, p.sx, p.sy, p.sz, p.rot, p.r, p.g, p.b, texRock, 0f) }
                5 -> { box(p.x, gy + p.sy * .5f, p.z, p.sx, p.sy, p.sz, p.rot, p.r, p.g, p.b, texSand, 0f); sph(p.x, gy + p.sy + .2f, p.z, .3f, .8f, .9f, 1f, 0, .4f) }
                6 -> { sph(p.x, gy + p.sy, p.z, p.sx, .9f, .92f, 1f, 0, .1f); box(p.x, gy + p.sy * .5f, p.z, p.sx * .3f, p.sy, p.sz * .3f, 0f, .7f, .75f, .8f, texRock) }
                7 -> { box(p.x, gy + p.sy * .5f, p.z, p.sx, p.sy, p.sz, p.rot, p.r, p.g, p.b, texMoss, 0f); box(p.x, gy + p.sy * 1.1f, p.z, p.sx * .7f, p.sy * .3f, p.sz * .7f, p.rot, p.r, p.g, p.b, texMoss) }
                else -> { box(p.x, gy + p.sy * .5f, p.z, p.sx, p.sy, p.sz, p.rot, p.r, p.g, p.b, texRock, .1f); sph(p.x, gy + p.sy + .3f, p.z, .3f, p.r, p.g, p.b, texCrystal, .6f) }
            }
        }
    }

    private fun drawCrystals() {
        for (c in crystals[realm]) {
            if (c.taken) continue
            val gy = ground(c.x, c.z, realm); val bob = sin(t * 2f + c.x) * .25f
            val s = 1f + .12f * sin(t * 3f + c.z)
            box(c.x, gy + 1.4f + bob, c.z, .35f * s, 1.6f * s, .35f * s, t * .8f, .55f, .85f, 1f, texCrystal, .9f)
            box(c.x, gy + 1.4f + bob, c.z, .6f * s, .8f * s, .6f * s, t * .8f + .5f, .7f, .95f, 1f, texCrystal, 1.2f)
            sph(c.x, gy + .15f, c.z, 1.1f, .3f, .6f, 1f, 0, .25f + .15f * sin(t * 2f), .25f)
        }
    }

    private fun drawPortals() {
        fun portal(x: Float, z: Float, r: Float, g: Float, b: Float) {
            val gy = ground(x, z, realm)
            box(x - 1.55f, gy + 1.65f, z, .34f, 1.65f, .45f, 0f, .72f, .74f, .78f, texRock)
            box(x + 1.55f, gy + 1.65f, z, .34f, 1.65f, .45f, 0f, .72f, .74f, .78f, texRock)
            box(x, gy + 3.15f, z, 1.9f, .30f, .45f, 0f, .72f, .74f, .78f, texRock)
            box(x, gy + 1.65f, z, .07f, 1.4f, 1.3f, 0f, r, g, b, texPortal, 1f, .85f)
            for (k in 0 until 6) { val a = t * 1.5f + k * PI.toFloat() / 3f; sph(x + cos(a) * 1.3f, gy + 1.65f + sin(a * .7f) * .9f, z + .2f, .09f, r, g, b, 0, 1f) }
        }
        if (realm == 0) { portal(26f, 0f, 1f, .35f, .1f); portal(-26f, 0f, .2f, .5f, 1f); portal(0f, -26f, .9f, .95f, 1f); portal(0f, 26f, .65f, .5f, .3f) }
        else portal(0f, 20f, .35f, .75f, 1f)
    }

    private fun drawHumanoid(x: Float, z: Float, gy: Float, body: FloatArray, boss: Boolean, hit: Float, phase: Float) {
        val sc = if (boss) 1.8f else 1f
        val wob = sin(phase * 6f) * .05f
        box(x, gy + .78f * sc, z, .36f * sc, .78f * sc, .25f * sc, wob, body[0], body[1], body[2], texRock, hit)
        sph(x, gy + (1.08f + .1f) * sc, z, .34f * sc, .84f, .70f, .58f, 0, hit)
        box(x - .48f * sc, gy + .62f * sc, z, .12f * sc, .62f * sc, .12f * sc, wob, body[0] * .9f, body[1] * .9f, body[2] * .9f, 0, hit)
        box(x + .48f * sc, gy + .62f * sc, z, .12f * sc, .62f * sc, .12f * sc, wob, body[0] * .9f, body[1] * .9f, body[2] * .9f, 0, hit)
        box(x - .20f * sc, gy - .42f * sc, z, .13f * sc, .42f * sc, .14f * sc, 0f, .10f, .11f, .13f, 0, hit)
        box(x + .20f * sc, gy - .42f * sc, z, .13f * sc, .42f * sc, .14f * sc, 0f, .10f, .11f, .13f, 0, hit)
        if (boss) sph(x, gy + (1.08f + .55f) * sc, z, .3f, 1f, .8f, .2f, 0, .8f)
    }

    private fun drawMobs() {
        for (m in mobs[realm]) {
            if (m.hp <= 0) continue
            val gy = ground(m.x, m.z, realm) + 1.05f
            val body = if (m.boss) floatArrayOf(.55f, .05f, .05f) else when (realm) { 1 -> floatArrayOf(.6f, .15f, .05f); 2 -> floatArrayOf(.1f, .3f, .6f); 3 -> floatArrayOf(.75f, .8f, .9f); 4 -> floatArrayOf(.4f, .3f, .2f); else -> floatArrayOf(.48f, .08f, .08f) }
            drawHumanoid(m.x, m.z, gy, body, m.boss, if (m.hitT > 0) .8f else 0f, m.phase)
            val bw = (if (m.boss) 2.2f else 1.1f); val frac = (m.hp / m.hpMax).coerceIn(0f, 1f)
            val bx = m.x; val by = gy + (if (m.boss) 2.6f else 1.9f)
            box(bx, by, m.z, bw * .55f, .07f, .02f, 0f, .1f, .1f, .1f, 0, 0f, .8f)
            box(bx - (bw * .5f) * (1 - frac), by, m.z, bw * .5f * frac, .06f, .03f, 0f, 1f, .15f, .15f, 0, .6f, .9f)
        }
    }

    private fun drawPlayer() {
        val body = floatArrayOf(.25f, .32f, .42f)
        drawHumanoid(px, pz, py, body, false, if (dashFx > 0) .3f else 0f, t)
        sph(px, py + 1.62f, pz, .1f, .5f, .8f, 1f, texCrystal, .9f)
        if (flying || dashFx > 0) for (i in 1..6) { val d = i * .34f; sph(px - sin(yaw) * d, py + .55f, pz + cos(yaw) * d, .16f + i * .025f, .04f, .04f, .055f, 0, .45f, .55f) }
    }

    private fun drawBlades() {
        if (realm != 0 && realm != 4) return
        val rng = Rng(99)
        val base = FloatArray(150 * 18)
        var n = 0
        for (i in 0 until 150) {
            val a = rng.next() * 6.283f; val r = 3f + rng.next() * 16f
            val x = px + cos(a) * r; val z = pz + sin(a) * r
            val gy = ground(x, z, realm)
            val h = .5f + rng.next() * .7f; val w = .06f + rng.next() * .05f
            val lean = sin(t * 2f + a * 20f) * .18f
            val o = i * 18
            val v = floatArrayOf(x, gy, z, x + lean, gy + h, z, x + w, gy, z, x + w, gy, z, x + lean, gy + h, z, x + lean + w, gy + h, z)
            for (k in 0 until 18) base[o + k] = v[k]
            n++
        }
        bladeBuf.clear(); bladeBuf.put(base, 0, n * 18)
        Matrix.setIdentityM(model, 0); Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0); GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES20.glUniform4f(uColor, .3f, .6f, .25f, 1f); GLES20.glUniform1f(uGlow, 0f); GLES20.glUniform1f(uUseTex, 0f); GLES20.glUniform1f(uSky, 0f); GLES20.glUniform1f(uWind, 1f); GLES20.glUniform1f(uWater, 0f)
        bladeBuf.position(0)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, bladeBuf)
        GLES20.glEnableVertexAttribArray(aPos)
        val nrm = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(floatArrayOf(0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f)); position(0) }
        GLES20.glVertexAttribPointer(aNor, 3, GLES20.GL_FLOAT, false, 0, nrm)
        GLES20.glEnableVertexAttribArray(aNor)
        GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 0, nrm)
        GLES20.glEnableVertexAttribArray(aUv)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, n * 6)
    }

    private fun drawSparks() {
        for (s in sparks) {
            val a = (s.life / s.maxLife).coerceIn(0f, 1f)
            sph(s.x, s.y, s.z, s.size * (0.6f + a), s.r, s.g, s.b, 0, .95f, a)
        }
    }
}
