package com.mumu.brumes

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.*

private data class HudState052(
    val life: Int = 100,
    val mana: Int = 100,
    val worldName: String = "Brumes",
    val enemies: Int = 0,
    val combo: String = "Aucun",
    val flying: Boolean = false,
    val ruins: Int = 0
)

class GameActivityV052 : Activity() {
    private lateinit var game: BrumesV052View
    private lateinit var root: FrameLayout
    private var menu: View? = null
    private val gold = Color.rgb(218, 181, 108)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun panel(alpha: Int = 235) = android.graphics.drawable.GradientDrawable().apply {
        setColor(Color.argb(alpha, 14, 18, 26))
        cornerRadius = dp(12).toFloat()
        setStroke(dp(1), Color.rgb(104, 90, 67))
    }

    private fun label(textValue: String, size: Float = 14f) = TextView(this).apply {
        text = textValue
        textSize = size
        setTextColor(Color.rgb(240, 235, 224))
        setPadding(dp(10), dp(6), dp(10), dp(6))
    }

    private fun button(textValue: String, onClick: () -> Unit) = Button(this).apply {
        text = textValue
        textSize = 11f
        isAllCaps = false
        setTextColor(gold)
        backgroundTintList = null
        background = panel(215)
        setOnClickListener { onClick() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        root = FrameLayout(this).apply { isMotionEventSplittingEnabled = true }
        game = BrumesV052View(this)
        root.addView(game, FrameLayout.LayoutParams(-1, -1))

        val hud = label("BRUMES 0.5.2", 12f).apply { background = panel() }
        root.addView(
            hud,
            FrameLayout.LayoutParams(dp(300), dp(82), Gravity.TOP or Gravity.START).apply {
                setMargins(dp(10), dp(8), 0, 0)
            }
        )

        root.addView(
            button("☰ MENU") { openMenu() },
            FrameLayout.LayoutParams(dp(96), dp(44), Gravity.TOP or Gravity.END).apply {
                setMargins(0, dp(8), dp(10), 0)
            }
        )

        val left = StickView052(this) { x, y -> game.move(x, y) }
        root.addView(
            left,
            FrameLayout.LayoutParams(dp(144), dp(144), Gravity.BOTTOM or Gravity.START).apply {
                setMargins(dp(12), 0, 0, dp(10))
            }
        )

        val right = StickView052(this) { x, y -> game.lookStick(x, y) }
        root.addView(
            right,
            FrameLayout.LayoutParams(dp(132), dp(132), Gravity.BOTTOM or Gravity.END).apply {
                setMargins(0, 0, dp(12), dp(92))
            }
        )

        val spells = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        listOf("🔥 Feu", "💧 Eau", "🌪 Air", "🪨 Terre").forEachIndexed { i, name ->
            spells.addView(
                button(name) { game.element(i) },
                LinearLayout.LayoutParams(dp(82), dp(52)).apply { setMargins(dp(3), 0, dp(3), 0) }
            )
        }
        root.addView(
            spells,
            FrameLayout.LayoutParams(-2, dp(58), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                setMargins(0, 0, 0, dp(8))
            }
        )

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        actions.addView(button("✦ ESQUIVE") { game.dash() }, LinearLayout.LayoutParams(dp(110), dp(48)))
        actions.addView(
            button("☁ TRANSPLANAGE") { game.toggleFlight() },
            LinearLayout.LayoutParams(dp(110), dp(48)).apply { setMargins(0, dp(4), 0, 0) }
        )
        actions.addView(
            button("◉ RUINE") { game.fastTravel() },
            LinearLayout.LayoutParams(dp(110), dp(44)).apply { setMargins(0, dp(4), 0, 0) }
        )
        root.addView(
            actions,
            FrameLayout.LayoutParams(dp(116), -2, Gravity.CENTER_VERTICAL or Gravity.END).apply {
                setMargins(0, 0, dp(10), 0)
            }
        )

        game.onHud = { state ->
            hud.text = "BRUMES 0.5.2 · ${state.worldName}\n" +
                "Vie ${state.life}   Mana ${state.mana}   ${if (state.flying) "VOL" else "SOL"}\n" +
                "${state.enemies} ennemis · ${state.ruins}/3 ruines · ${state.combo}"
        }

        setContentView(root)
        openMenu()
    }

    override fun onPause() {
        game.pauseGame(true)
        game.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        game.onResume()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (menu == null) openMenu() else closeMenu()
    }

    private fun closeMenu() {
        menu?.let { root.removeView(it) }
        menu = null
        game.pauseGame(false)
    }

    private fun openMenu() {
        if (menu != null) return
        game.pauseGame(true)

        val shade = FrameLayout(this).apply {
            setBackgroundColor(0xDD070B10.toInt())
            isClickable = true
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = panel()
        }
        body.addView(label("B R U M E S   0.5.2", 26f).apply {
            setTextColor(gold)
            gravity = Gravity.CENTER
        })
        body.addView(label("Version S25 corrigée · trois mondes · magie · PNJ · vol · ruines", 13f).apply {
            gravity = Gravity.CENTER
        })
        body.addView(button("JOUER / REPRENDRE") { closeMenu() }, LinearLayout.LayoutParams(-1, dp(48)))
        body.addView(
            label(
                "Joystick gauche : déplacement. Joystick droit : caméra et hauteur en vol.\n" +
                    "ESQUIVE : téléportation courte. TRANSPLANAGE : impulsion puis vol libre.\n" +
                    "Pince l'écran pour zoomer. Choisis deux éléments pour combiner la magie.",
                12f
            )
        )
        body.addView(
            label(
                "Combinaisons : Feu+Eau Vapeur · Feu+Air Braises · Feu+Terre Lave · " +
                    "Eau+Air Givre · Eau+Terre Boue · Air+Terre Sable",
                12f
            )
        )

        shade.addView(
            body,
            FrameLayout.LayoutParams(-1, -2, Gravity.CENTER).apply {
                setMargins(dp(28), dp(18), dp(28), dp(18))
            }
        )
        root.addView(shade, FrameLayout.LayoutParams(-1, -1))
        menu = shade
    }
}

private class StickView052(activity: Activity, private val output: (Float, Float) -> Unit) : View(activity) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var dx = 0f
    private var dy = 0f

    override fun onDraw(canvas: Canvas) {
        val radius = width * 0.42f
        paint.style = Paint.Style.FILL
        paint.color = 0x55434D54
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = 0xCCDAB56C.toInt()
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)
        paint.style = Paint.Style.FILL
        paint.color = 0xDDDAB56C.toInt()
        canvas.drawCircle(width / 2f + dx * radius, height / 2f + dy * radius, radius * 0.28f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                dx = (event.x - width / 2f) / max(1f, width * 0.42f)
                dy = (event.y - height / 2f) / max(1f, height * 0.42f)
                val length = max(1f, hypot(dx, dy))
                dx /= length
                dy /= length
            }
            else -> {
                dx = 0f
                dy = 0f
            }
        }
        output(dx, dy)
        invalidate()
        return true
    }
}

private data class WorldObj052(
    val x: Float, val y: Float, val z: Float,
    val sx: Float, val sy: Float, val sz: Float,
    val r: Float, val g: Float, val b: Float
)

private data class Actor052(
    var x: Float,
    var z: Float,
    var hp: Float = 100f,
    var cooldown: Float = 0f,
    var wander: Float = 0f,
    var phase: Float = 0f
)

private class BrumesV052View(activity: Activity) : GLSurfaceView(activity) {
    internal var onHud: ((HudState052) -> Unit)? = null
    private val renderer052 = Renderer052(activity.assets) { state -> post { onHud?.invoke(state) } }
    private var lastDistance = 0f

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer052)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }

    fun move(x: Float, y: Float) {
        renderer052.moveX = x
        renderer052.moveZ = y
    }

    fun lookStick(x: Float, y: Float) {
        renderer052.lookX = x
        renderer052.lookY = y
    }

    fun element(index: Int) = queueEvent { renderer052.element(index) }
    fun dash() = queueEvent { renderer052.dash() }
    fun toggleFlight() = queueEvent { renderer052.toggleFlight() }
    fun fastTravel() = queueEvent { renderer052.fastTravel() }
    fun pauseGame(value: Boolean) = queueEvent { renderer052.paused = value }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount >= 2) {
            val distance = hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1))
            if (lastDistance > 0f) renderer052.zoom((lastDistance - distance) / 180f)
            lastDistance = distance
        } else if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            lastDistance = 0f
        }
        return true
    }
}

private class Renderer052(
    private val assets: android.content.res.AssetManager,
    private val hud: (HudState052) -> Unit
) : GLSurfaceView.Renderer {

    @Volatile var moveX = 0f
    @Volatile var moveZ = 0f
    @Volatile var lookX = 0f
    @Volatile var lookY = 0f
    @Volatile var paused = true

    private var program = 0
    private var aPos = 0
    private var aNormal = 0
    private var uMvp = 0
    private var uModel = 0
    private var uScale = 0
    private var uColor = 0
    private var uEye = 0
    private var uGlow = 0

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val vp = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    // 12 triangles, 36 vertices, 108 floats. The previous build accidentally had 107 floats,
    // which made the normal generator read past the end of the array during Activity startup.
    private val cube = floatArrayOf(
        // front
        -1f,-1f, 1f,  1f,-1f, 1f,  1f, 1f, 1f,
        -1f,-1f, 1f,  1f, 1f, 1f, -1f, 1f, 1f,
        // back
         1f,-1f,-1f, -1f,-1f,-1f, -1f, 1f,-1f,
         1f,-1f,-1f, -1f, 1f,-1f,  1f, 1f,-1f,
        // left
        -1f,-1f,-1f, -1f,-1f, 1f, -1f, 1f, 1f,
        -1f,-1f,-1f, -1f, 1f, 1f, -1f, 1f,-1f,
        // right
         1f,-1f, 1f,  1f,-1f,-1f,  1f, 1f,-1f,
         1f,-1f, 1f,  1f, 1f,-1f,  1f, 1f, 1f,
        // top
        -1f, 1f, 1f,  1f, 1f, 1f,  1f, 1f,-1f,
        -1f, 1f, 1f,  1f, 1f,-1f, -1f, 1f,-1f,
        // bottom
        -1f,-1f,-1f,  1f,-1f,-1f,  1f,-1f, 1f,
        -1f,-1f,-1f,  1f,-1f, 1f, -1f,-1f, 1f
    )

    private val cubeBuffer = buffer(cube)
    private val normalBuffer = normals(cube)

    private val worlds = Array(3) { mutableListOf<WorldObj052>() }
    private val enemies = Array(3) { mutableListOf<Actor052>() }
    private val npcs = Array(3) { mutableListOf<Actor052>() }
    private val worldNames = arrayOf("Brumes", "Cité d'Éther", "Île Forêt")
    private val ruinPositions = arrayOf(
        arrayOf(floatArrayOf(-45f, 15f), floatArrayOf(0f, 48f), floatArrayOf(48f, -18f)),
        arrayOf(floatArrayOf(-28f, -15f), floatArrayOf(8f, 20f), floatArrayOf(35f, -25f)),
        arrayOf(floatArrayOf(-38f, 10f), floatArrayOf(5f, 32f), floatArrayOf(42f, -18f))
    )

    private var world = 0
    private var px = 0f
    private var py = 0f
    private var pz = 0f
    private var yaw = 0f
    private var pitch = 0.20f
    private var cameraZoom = 11f
    private var flying = false
    private var flySpeed = 0f
    private var life = 100f
    private var mana = 100f
    private var selected = -1
    private var combo = "Aucun"
    private var comboFx = 0f
    private var dashFx = 0f
    private var dashFromX = 0f
    private var dashFromZ = 0f
    private var dashToX = 0f
    private var dashToZ = 0f
    private var time = 0f
    private var hudAt = 0f
    private var lastNanos = 0L
    private var portalCooldown = 0f
    private val ruinMask = IntArray(3)
    private var ruinCursor = 0

    private fun buffer(values: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(values)
            position(0)
        }

    private fun normals(vertices: FloatArray): FloatBuffer {
        val out = FloatArray(vertices.size)
        var i = 0
        while (i + 8 < vertices.size) {
            val ax = vertices[i + 3] - vertices[i]
            val ay = vertices[i + 4] - vertices[i + 1]
            val az = vertices[i + 5] - vertices[i + 2]
            val bx = vertices[i + 6] - vertices[i]
            val by = vertices[i + 7] - vertices[i + 1]
            val bz = vertices[i + 8] - vertices[i + 2]
            val nx = ay * bz - az * by
            val ny = az * bx - ax * bz
            val nz = ax * by - ay * bx
            val length = max(0.001f, sqrt(nx * nx + ny * ny + nz * nz))
            for (v in 0..2) {
                val base = i + v * 3
                out[base] = nx / length
                out[base + 1] = ny / length
                out[base + 2] = nz / length
            }
            i += 9
        }
        return buffer(out)
    }

    private fun loadWorld(assetName: String): MutableList<WorldObj052> {
        val out = mutableListOf<WorldObj052>()
        return try {
            val text = assets.open(assetName).bufferedReader().use { it.readText() }
            val array = JSONObject(text).getJSONArray("objects")
            for (i in 0 until array.length()) {
                val a = array.getJSONArray(i)
                if (a.length() < 9) continue
                out += WorldObj052(
                    a.getDouble(0).toFloat(),
                    a.getDouble(1).toFloat(),
                    a.getDouble(2).toFloat(),
                    max(0.15f, a.getDouble(3).toFloat() / 2f),
                    max(0.15f, a.getDouble(4).toFloat() / 2f),
                    max(0.15f, a.getDouble(5).toFloat() / 2f),
                    a.getInt(6).coerceIn(0, 255) / 255f,
                    a.getInt(7).coerceIn(0, 255) / 255f,
                    a.getInt(8).coerceIn(0, 255) / 255f
                )
            }
            out
        } catch (_: Throwable) {
            out
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw IllegalStateException("Shader error: $log")
        }
        return shader
    }

    override fun onSurfaceCreated(
        gl: javax.microedition.khronos.opengles.GL10?,
        config: javax.microedition.khronos.egl.EGLConfig?
    ) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        val vertexSource = """
            attribute vec3 a;
            attribute vec3 n;
            uniform mat4 m;
            uniform mat4 model;
            uniform vec3 scale;
            varying vec3 wn;
            varying vec3 wp;
            void main(){
                wp=(model*vec4(a,1.0)).xyz;
                wn=normalize(n/max(scale,vec3(0.001)));
                gl_Position=m*vec4(a,1.0);
            }
        """.trimIndent()

        val fragmentSource = """
            precision mediump float;
            uniform vec4 c;
            uniform vec3 eye;
            uniform float glow;
            varying vec3 wn;
            varying vec3 wp;
            void main(){
                float d=max(dot(normalize(wn),normalize(vec3(-0.45,0.8,0.35))),0.0);
                vec3 col=c.rgb*(0.42+d*0.72);
                col=mix(col,c.rgb*1.5,clamp(glow,0.0,1.0));
                float dist=length(wp-eye);
                float fog=1.0-exp(-pow(dist*0.009,2.0));
                gl_FragColor=vec4(mix(col,vec3(0.18,0.23,0.30),clamp(fog,0.0,0.93)),c.a);
            }
        """.trimIndent()

        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            throw IllegalStateException("Program link error: ${GLES20.glGetProgramInfoLog(program)}")
        }

        aPos = GLES20.glGetAttribLocation(program, "a")
        aNormal = GLES20.glGetAttribLocation(program, "n")
        uMvp = GLES20.glGetUniformLocation(program, "m")
        uModel = GLES20.glGetUniformLocation(program, "model")
        uScale = GLES20.glGetUniformLocation(program, "scale")
        uColor = GLES20.glGetUniformLocation(program, "c")
        uEye = GLES20.glGetUniformLocation(program, "eye")
        uGlow = GLES20.glGetUniformLocation(program, "glow")

        buildWorlds()
        lastNanos = System.nanoTime()
    }

    private fun buildWorlds() {
        worlds.forEach { it.clear() }
        enemies.forEach { it.clear() }
        npcs.forEach { it.clear() }

        worlds[0] += WorldObj052(0f, -1.4f, 0f, 68f, 1f, 68f, 0.20f, 0.27f, 0.20f)
        for (i in -5..5) {
            val z = i * 10f
            worlds[0] += WorldObj052(-56f, 2f, z, 1.5f, 3.5f, 4f, 0.18f, 0.22f, 0.19f)
            worlds[0] += WorldObj052(56f, 2f, z, 1.5f, 3.5f, 4f, 0.18f, 0.22f, 0.19f)
        }
        for (i in 0 until 18) {
            val angle = i * (Math.PI * 2.0 / 18.0)
            val radius = 24f + (i % 3) * 8f
            worlds[0] += WorldObj052(
                (cos(angle) * radius).toFloat(),
                1.6f,
                (sin(angle) * radius).toFloat(),
                1.6f,
                3.2f + (i % 4),
                1.6f,
                0.16f + (i % 3) * 0.03f,
                0.28f,
                0.18f
            )
        }

        worlds[1] += loadWorld("world_aether.json")
        worlds[2] += loadWorld("world_forest.json")
        if (worlds[1].isEmpty()) worlds[1] += WorldObj052(0f, -1.4f, 0f, 68f, 1f, 68f, 0.35f, 0.31f, 0.45f)
        if (worlds[2].isEmpty()) worlds[2] += WorldObj052(0f, -1.4f, 0f, 72f, 1f, 72f, 0.16f, 0.34f, 0.18f)

        for (w in 0..2) {
            val limit = if (w == 2) 54f else 48f
            for (i in 0 until 9) {
                val angle = i * 0.91f + w * 0.7f
                val radius = 16f + (i % 4) * 7f
                enemies[w] += Actor052(cos(angle) * radius, sin(angle) * radius, 100f, i * 0.15f, angle, angle)
            }
            for (i in 0 until 5) {
                val angle = i * 1.37f + 0.4f
                val radius = min(limit, 12f + i * 7f)
                npcs[w] += Actor052(cos(angle) * radius, sin(angle) * radius, 100f, 0f, angle, angle)
            }
        }
    }

    override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
        val safeHeight = max(1, height)
        GLES20.glViewport(0, 0, width, safeHeight)
        Matrix.perspectiveM(projection, 0, 58f, width.toFloat() / safeHeight.toFloat(), 0.08f, 260f)
    }

    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
        val now = System.nanoTime()
        val dt = if (lastNanos == 0L) 0.016f else ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastNanos = now
        if (!paused) update(dt)
        render()
    }

    private fun update(dt: Float) {
        time += dt
        portalCooldown = max(0f, portalCooldown - dt)
        comboFx = max(0f, comboFx - dt)
        dashFx = max(0f, dashFx - dt)
        mana = min(100f, mana + 4.5f * dt)

        yaw -= lookX * 1.55f * dt
        pitch = (pitch - lookY * 0.8f * dt).coerceIn(-0.15f, 0.55f)

        val forwardX = -sin(yaw)
        val forwardZ = -cos(yaw)
        val rightX = cos(yaw)
        val rightZ = -sin(yaw)
        val speed = if (flying) 10.5f else 6.8f
        val dx = (rightX * moveX + forwardX * -moveZ) * speed * dt
        val dz = (rightZ * moveX + forwardZ * -moveZ) * speed * dt
        px += dx
        pz += dz

        val bound = if (world == 2) 78f else 70f
        px = px.coerceIn(-bound, bound)
        pz = pz.coerceIn(-bound, bound)

        if (flying) {
            flySpeed = min(13f, flySpeed + 8f * dt)
            py += (-lookY * 9.5f + 0.25f) * dt
            py = py.coerceIn(1.0f, 28f)
        } else {
            flySpeed = 0f
            py = max(0f, py - 13f * dt)
        }

        updateActors(dt)
        updateRuins()
        updatePortals()

        hudAt -= dt
        if (hudAt <= 0f) {
            hudAt = 0.15f
            hud(
                HudState052(
                    life.roundToInt().coerceIn(0, 100),
                    mana.roundToInt().coerceIn(0, 100),
                    worldNames[world],
                    enemies[world].count { it.hp > 0f },
                    combo,
                    flying,
                    Integer.bitCount(ruinMask[world])
                )
            )
        }
    }

    private fun updateActors(dt: Float) {
        enemies[world].forEachIndexed { index, actor ->
            if (actor.hp <= 0f) return@forEachIndexed
            actor.cooldown = max(0f, actor.cooldown - dt)
            val dx = px - actor.x
            val dz = pz - actor.z
            val distance = max(0.001f, hypot(dx, dz))
            if (distance < 20f) {
                val chase = if (distance > 5.5f) 2.1f else -0.8f
                actor.x += dx / distance * chase * dt
                actor.z += dz / distance * chase * dt
                if (distance < 8.5f && actor.cooldown <= 0f) {
                    life = max(0f, life - (3.2f + (index % 3)))
                    actor.cooldown = 1.25f + (index % 4) * 0.15f
                }
            } else {
                actor.phase += dt * (0.5f + (index % 3) * 0.12f)
                actor.x += cos(actor.phase) * 0.55f * dt
                actor.z += sin(actor.phase * 0.83f) * 0.55f * dt
            }
        }

        npcs[world].forEachIndexed { index, actor ->
            actor.phase += dt * (0.28f + index * 0.025f)
            actor.x += cos(actor.phase) * 0.32f * dt
            actor.z += sin(actor.phase * 0.91f) * 0.32f * dt
        }

        if (life <= 0f) {
            life = 100f
            mana = 100f
            px = 0f
            py = 0f
            pz = 0f
            flying = false
        }
    }

    private fun updateRuins() {
        ruinPositions[world].forEachIndexed { index, pos ->
            if (hypot(px - pos[0], pz - pos[1]) < 5.5f) {
                ruinMask[world] = ruinMask[world] or (1 shl index)
            }
        }
    }

    private fun updatePortals() {
        if (portalCooldown > 0f) return
        if (world == 0) {
            if (hypot(px + 30f, pz) < 4.2f) switchWorld(1)
            else if (hypot(px - 30f, pz) < 4.2f) switchWorld(2)
        } else if (hypot(px, pz + 30f) < 4.2f) {
            switchWorld(0)
        }
    }

    private fun switchWorld(target: Int) {
        world = target.coerceIn(0, 2)
        px = 0f
        py = 0f
        pz = if (world == 0) 12f else 0f
        flying = false
        portalCooldown = 2f
        combo = "Aucun"
        selected = -1
    }

    fun element(index: Int) {
        if (index !in 0..3 || mana < 8f) return
        if (selected < 0) {
            selected = index
            combo = arrayOf("Feu", "Eau", "Air", "Terre")[index]
            comboFx = 0.65f
            mana = max(0f, mana - 3f)
            return
        }

        val first = min(selected, index)
        val second = max(selected, index)
        combo = when (first * 10 + second) {
            1 -> "Vapeur"
            2 -> "Tempête de braises"
            3 -> "Lave"
            12 -> "Givre"
            13 -> "Boue entravante"
            23 -> "Tempête de sable"
            else -> arrayOf("Feu", "Eau", "Air", "Terre")[index]
        }
        selected = -1
        mana = max(0f, mana - 12f)
        comboFx = 1.1f

        enemies[world].forEach { enemy ->
            if (enemy.hp > 0f) {
                val d = hypot(enemy.x - px, enemy.z - pz)
                if (d < 16f) enemy.hp = max(0f, enemy.hp - if (d < 8f) 55f else 34f)
            }
        }
    }

    fun dash() {
        val dx = if (abs(moveX) + abs(moveZ) > 0.15f) moveX else 0f
        val dz = if (abs(moveX) + abs(moveZ) > 0.15f) -moveZ else 1f
        val length = max(0.001f, hypot(dx, dz))
        val localX = dx / length
        val localZ = dz / length
        val worldX = cos(yaw) * localX - sin(yaw) * localZ
        val worldZ = -sin(yaw) * localX - cos(yaw) * localZ
        val bound = if (world == 2) 76f else 68f

        dashFromX = px
        dashFromZ = pz
        var tx = px + worldX * 11f
        var tz = pz + worldZ * 11f
        if (tx !in -bound..bound || tz !in -bound..bound) {
            tx = (px - worldX * 7f).coerceIn(-bound, bound)
            tz = (pz - worldZ * 7f).coerceIn(-bound, bound)
        }
        px = tx
        pz = tz
        dashToX = tx
        dashToZ = tz
        dashFx = 0.42f
    }

    fun toggleFlight() {
        flying = !flying
        if (flying) {
            py = max(1.3f, py + 1.3f)
            flySpeed = 8f
            dashFx = 0.65f
            dashFromX = px
            dashFromZ = pz + 3f
            dashToX = px
            dashToZ = pz
        } else {
            flySpeed = 0f
        }
    }

    fun fastTravel() {
        val mask = ruinMask[world]
        if (mask == 0) {
            val nearest = ruinPositions[world].indices.minByOrNull { i ->
                val p = ruinPositions[world][i]
                hypot(px - p[0], pz - p[1])
            } ?: return
            val p = ruinPositions[world][nearest]
            if (hypot(px - p[0], pz - p[1]) < 9f) ruinMask[world] = mask or (1 shl nearest)
            return
        }

        repeat(3) {
            ruinCursor = (ruinCursor + 1) % 3
            if ((ruinMask[world] and (1 shl ruinCursor)) != 0) {
                val p = ruinPositions[world][ruinCursor]
                dashFromX = px
                dashFromZ = pz
                px = p[0] + 3f
                pz = p[1]
                dashToX = px
                dashToZ = pz
                dashFx = 0.7f
                return
            }
        }
    }

    fun zoom(delta: Float) {
        cameraZoom = (cameraZoom + delta).coerceIn(5.5f, 20f)
    }

    private fun render() {
        val cycle = (sin(time * 0.075f) + 1f) * 0.5f
        val night = cycle < 0.28f
        val sunset = cycle in 0.28f..0.42f
        val clear = when {
            night -> floatArrayOf(0.025f, 0.035f, 0.075f)
            sunset -> floatArrayOf(0.34f, 0.20f, 0.18f)
            else -> floatArrayOf(0.18f, 0.31f, 0.43f)
        }
        GLES20.glClearColor(clear[0], clear[1], clear[2], 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (program == 0) return

        val cp = cos(pitch)
        val eyeX = px + sin(yaw) * cameraZoom * cp
        val eyeY = py + 3.1f + sin(pitch) * cameraZoom
        val eyeZ = pz + cos(yaw) * cameraZoom * cp
        Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ, px, py + 1.1f, pz, 0f, 1f, 0f)
        Matrix.multiplyMM(vp, 0, projection, 0, view, 0)

        GLES20.glUseProgram(program)
        GLES20.glUniform3f(uEye, eyeX, eyeY, eyeZ)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glEnableVertexAttribArray(aNormal)
        cubeBuffer.position(0)
        normalBuffer.position(0)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, cubeBuffer)
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 0, normalBuffer)

        worlds[world].forEach { obj ->
            drawCube(obj.x, obj.y, obj.z, obj.sx, obj.sy, obj.sz, obj.r, obj.g, obj.b, 1f, 0f)
        }

        ruinPositions[world].forEachIndexed { index, p ->
            val active = (ruinMask[world] and (1 shl index)) != 0
            val pulse = 0.55f + sin(time * 3f + index) * 0.18f
            drawCube(p[0], 0.25f, p[1], 2.7f, 0.16f, 2.7f, if (active) 0.22f else 0.15f, 0.62f, 0.78f, 0.72f, pulse)
            drawCube(p[0], 1.4f, p[1], 0.25f, 1.2f, 0.25f, 0.30f, 0.78f, 0.92f, 0.65f, pulse)
        }

        if (world == 0) {
            drawPortal(-30f, 0f, 0.50f, 0.48f, 0.95f)
            drawPortal(30f, 0f, 0.20f, 0.80f, 0.46f)
        } else {
            drawPortal(0f, -30f, 0.72f, 0.50f, 0.92f)
        }

        enemies[world].forEachIndexed { index, enemy ->
            if (enemy.hp > 0f) {
                val bob = sin(time * 2f + index) * 0.12f
                drawCube(enemy.x, 1.0f + bob, enemy.z, 0.62f, 1.0f, 0.62f, 0.66f, 0.12f, 0.16f, 1f, 0.08f)
                if (enemy.cooldown > 0.7f) {
                    drawCube(enemy.x, 2.15f + bob, enemy.z, 0.20f, 0.20f, 0.20f, 0.86f, 0.22f, 0.16f, 0.75f, 0.8f)
                }
            }
        }

        npcs[world].forEachIndexed { index, npc ->
            val bob = sin(time * 1.7f + index) * 0.08f
            drawCube(npc.x, 1.0f + bob, npc.z, 0.58f, 1.0f, 0.58f, 0.18f, 0.52f, 0.72f, 1f, 0.04f)
        }

        val bodyGlow = if (flying) 0.42f else 0.03f
        drawCube(px, py + 1.0f, pz, 0.48f, 1.0f, 0.36f, 0.74f, 0.78f, 0.84f, 1f, bodyGlow)
        drawCube(px, py + 2.20f, pz, 0.40f, 0.40f, 0.40f, 0.82f, 0.72f, 0.60f, 1f, bodyGlow)

        if (comboFx > 0f) {
            val s = 1.4f + (1.1f - comboFx) * 1.8f
            val color = comboColor(combo)
            drawCube(px, py + 1.0f, pz - 1.4f, s, 0.12f, s, color[0], color[1], color[2], 0.55f, 0.95f)
        }

        if (dashFx > 0f) {
            val alpha = (dashFx / 0.7f).coerceIn(0f, 0.65f)
            for (i in 1..5) {
                val t = i / 6f
                val x = dashFromX + (dashToX - dashFromX) * t
                val z = dashFromZ + (dashToZ - dashFromZ) * t
                drawCube(x, py + 0.9f, z, 0.45f + t * 0.3f, 0.65f, 0.45f + t * 0.3f, 0.025f, 0.025f, 0.035f, alpha * (1f - t * 0.5f), 0.25f)
            }
        }

        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aNormal)
    }

    private fun drawPortal(x: Float, z: Float, r: Float, g: Float, b: Float) {
        val pulse = 0.70f + sin(time * 3.4f) * 0.20f
        drawCube(x - 2.0f, 2.1f, z, 0.35f, 2.1f, 0.35f, 0.18f, 0.16f, 0.22f, 1f, 0.1f)
        drawCube(x + 2.0f, 2.1f, z, 0.35f, 2.1f, 0.35f, 0.18f, 0.16f, 0.22f, 1f, 0.1f)
        drawCube(x, 4.2f, z, 2.35f, 0.35f, 0.35f, 0.18f, 0.16f, 0.22f, 1f, 0.1f)
        drawCube(x, 2.15f, z, 1.55f, 1.55f, 0.12f, r, g, b, 0.45f, pulse)
    }

    private fun comboColor(name: String): FloatArray = when (name) {
        "Vapeur" -> floatArrayOf(0.78f, 0.86f, 0.90f)
        "Tempête de braises" -> floatArrayOf(1.0f, 0.28f, 0.06f)
        "Lave" -> floatArrayOf(0.95f, 0.18f, 0.02f)
        "Givre" -> floatArrayOf(0.34f, 0.76f, 1.0f)
        "Boue entravante" -> floatArrayOf(0.35f, 0.22f, 0.10f)
        "Tempête de sable" -> floatArrayOf(0.78f, 0.62f, 0.28f)
        "Feu" -> floatArrayOf(1.0f, 0.25f, 0.05f)
        "Eau" -> floatArrayOf(0.10f, 0.48f, 1.0f)
        "Air" -> floatArrayOf(0.70f, 0.90f, 1.0f)
        "Terre" -> floatArrayOf(0.36f, 0.24f, 0.12f)
        else -> floatArrayOf(0.65f, 0.55f, 0.95f)
    }

    private fun drawCube(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        alpha: Float, glow: Float
    ) {
        if (program == 0) return
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        Matrix.scaleM(model, 0, sx, sy, sz)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES20.glUniform3f(uScale, max(0.001f, sx), max(0.001f, sy), max(0.001f, sz))
        GLES20.glUniform4f(uColor, r, g, b, alpha)
        GLES20.glUniform1f(uGlow, glow)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, cube.size / 3)
    }
}
