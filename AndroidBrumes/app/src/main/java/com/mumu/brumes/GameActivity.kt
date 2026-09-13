package com.mumu.brumes

import android.app.Activity
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.*

class GameActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        val game = BrumesView(this)
        root.addView(game)
        val hud = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 14f; setPadding(24, 18, 24, 18)
            setBackgroundColor(0x66000000); text = "BRUMES\nVie 100 · Mana 100"
        }
        root.addView(hud, FrameLayout.LayoutParams(330, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START))
        game.hud = hud
        val bar = LinearLayout(this).apply { gravity = Gravity.END; setPadding(8, 0, 20, 16) }
        listOf("FEU", "GIVRE", "ONDE").forEachIndexed { i, name ->
            bar.addView(Button(this).apply {
                text = name; textSize = 11f
                setOnClickListener { game.cast(i) }
            }, LinearLayout.LayoutParams(104, 64).apply { setMargins(8, 0, 0, 0) })
        }
        root.addView(bar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        setContentView(root)
    }
}

class BrumesView(activity: Activity) : GLSurfaceView(activity) {
    @Volatile var hud: TextView? = null
    private val renderer = BrumesRenderer { text -> hud?.post { hud?.text = text } }
    private var startX = 0f; private var startY = 0f; private var moving = false
    init { setEGLContextClientVersion(2); setRenderer(renderer); renderMode = RENDERMODE_CONTINUOUSLY }
    fun cast(spell: Int) { renderer.cast(spell) }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                moving = e.x < width * .48f
                if (moving) { startX = e.x; startY = e.y } else renderer.cast(0)
            }
            MotionEvent.ACTION_MOVE -> if (moving) {
                renderer.moveX = ((e.x - startX) / 115f).coerceIn(-1f, 1f)
                renderer.moveZ = ((e.y - startY) / 115f).coerceIn(-1f, 1f)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (moving) { renderer.moveX = 0f; renderer.moveZ = 0f; moving = false }
        }
        return true
    }
}

private class BrumesRenderer(private val setHud: (String) -> Unit) : GLSurfaceView.Renderer {
    @Volatile var moveX = 0f; @Volatile var moveZ = 0f
    private var program = 0; private var aPos = 0; private var uMvp = 0; private var uColor = 0
    private val projection = FloatArray(16); private val view = FloatArray(16); private val vp = FloatArray(16)
    private val model = FloatArray(16); private val mvp = FloatArray(16)
    private var playerX = 0f; private var playerZ = 0f; private var mana = 100f; private var life = 100f
    private var effect = 0f; private var effectType = 0; private var elapsed = 0f; private var kills = 0
    private val enemies = Array(14) { i -> floatArrayOf(sin(i * 2.4f) * (17 + i % 4 * 8), cos(i * 2.4f) * (17 + i % 4 * 8), 100f) }
    private val cube = floatArrayOf(
        -1f,-1f, 1f, 1f,-1f, 1f, 1f,1f, 1f, -1f,-1f, 1f, 1f,1f, 1f, -1f,1f, 1f,
        1f,-1f,-1f, -1f,-1f,-1f, -1f,1f,-1f, 1f,-1f,-1f, -1f,1f,-1f, 1f,1f,-1f,
        -1f,-1f,-1f, -1f,-1f,1f, -1f,1f,1f, -1f,-1f,-1f, -1f,1f,1f, -1f,1f,-1f,
        1f,-1f,1f, 1f,-1f,-1f, 1f,1f,-1f, 1f,-1f,1f, 1f,1f,-1f, 1f,1f,1f,
        -1f,1f,1f, 1f,1f,1f, 1f,1f,-1f, -1f,1f,1f, 1f,1f,-1f, -1f,1f,-1f,
        -1f,-1f,-1f, 1f,-1f,-1f, 1f,-1f,1f, -1f,-1f,-1f, 1f,-1f,1f, -1f,-1f,1f
    )
    private val ground = floatArrayOf(-100f,0f,-100f, 100f,0f,-100f, 100f,0f,100f, -100f,0f,-100f, 100f,0f,100f, -100f,0f,100f)
    private val cb = java.nio.ByteBuffer.allocateDirect(cube.size*4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply { put(cube); position(0) }
    private val gb = java.nio.ByteBuffer.allocateDirect(ground.size*4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply { put(ground); position(0) }
    override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        GLES20.glClearColor(.025f,.06f,.105f,1f); GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        val vs = "attribute vec3 a; uniform mat4 m; void main(){ gl_Position=m*vec4(a,1.0); }"
        val fs = "precision mediump float; uniform vec4 c; void main(){ gl_FragColor=c; }"
        fun shader(type:Int, source:String) = GLES20.glCreateShader(type).also { GLES20.glShaderSource(it,source); GLES20.glCompileShader(it) }
        program = GLES20.glCreateProgram().also { GLES20.glAttachShader(it,shader(GLES20.GL_VERTEX_SHADER,vs)); GLES20.glAttachShader(it,shader(GLES20.GL_FRAGMENT_SHADER,fs)); GLES20.glLinkProgram(it) }
        aPos=GLES20.glGetAttribLocation(program,"a"); uMvp=GLES20.glGetUniformLocation(program,"m"); uColor=GLES20.glGetUniformLocation(program,"c")
    }
    override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, w:Int, h:Int) { GLES20.glViewport(0,0,w,h); Matrix.perspectiveM(projection,0,62f,w.toFloat()/h,0.1f,180f) }
    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
        val dt=.016f; elapsed += dt; mana=min(100f,mana+dt*8); effect=max(0f,effect-dt)
        playerX=(playerX+moveX*dt*8).coerceIn(-82f,82f); playerZ=(playerZ+moveZ*dt*8).coerceIn(-82f,82f)
        enemies.forEach { e -> if(e[2]>0) { val dx=playerX-e[0]; val dz=playerZ-e[1]; val d=max(.1f,sqrt(dx*dx+dz*dz)); if(d<20 && effectType!=1 || effect<=0) { e[0]+=dx/d*dt*1.35f; e[1]+=dz/d*dt*1.35f }; if(d<1.7f) life=max(0f,life-dt*5) } }
        if(life<=0) { playerX=0f;playerZ=0f;life=100f }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT); GLES20.glUseProgram(program)
        Matrix.setLookAtM(view,0,playerX,11f,playerZ+12f,playerX,0f,playerZ,0f,1f,0f); Matrix.multiplyMM(vp,0,projection,0,view,0)
        draw(ground,gb,0f,0f,0f,1f,1f,1f,floatArrayOf(.10f,.22f,.18f,1f))
        for(i in 0 until 35) { val x=sin(i*2.23f)*((i%7)*10+16); val z=cos(i*1.71f)*((i%6)*12+14); draw(cube,cb,x,2f,z,1f,2f,1f,floatArrayOf(.08f,.28f,.20f,1f)) }
        for(i in 0 until 3) { val a=i*2.09f; draw(cube,cb,sin(a)*53,2.4f,cos(a)*53,2.7f,2.4f,2.7f,if(effect>0) floatArrayOf(1f,.8f,.2f,1f) else floatArrayOf(.1f,.8f,1f,1f)) }
        draw(cube,cb,playerX,1f,playerZ,.55f,1f,.55f,floatArrayOf(.27f,.18f,.9f,1f)); draw(cube,cb,playerX,2.15f,playerZ,.25f,.25f,.25f,floatArrayOf(.96f,.72f,.48f,1f))
        enemies.forEach { e -> if(e[2]>0) draw(cube,cb,e[0],.85f,e[1],.55f,.85f,.55f,if(effectType==1 && effect>0) floatArrayOf(.1f,.8f,1f,1f) else floatArrayOf(.65f,.08f,.38f,1f)) }
        if(effect>0) { val s=1f+(1f-effect)*7f; draw(cube,cb,playerX,.15f,playerZ,s,.06f,s,when(effectType){0->floatArrayOf(1f,.25f,.04f,1f);1->floatArrayOf(.1f,.9f,1f,1f);else->floatArrayOf(.7f,.2f,1f,1f)}) }
        if((elapsed*2).toInt()%2==0) setHud("BRUMES · Mage des trois sanctuaires\nVie ${life.toInt()} · Mana ${mana.toInt()} · Ombres vaincues $kills\nJoyst. : moitié gauche · Sorts : boutons en bas")
    }
    fun cast(type:Int) { val cost=when(type){0->14f;1->25f;else->38f}; if(mana<cost)return; mana-=cost;effect=1f;effectType=type; val range=if(type==2)8f else 18f; enemies.forEach { e -> if(e[2]>0 && hypot(e[0]-playerX,e[1]-playerZ)<range) { e[2]-=when(type){0->45f;1->30f;else->70f};if(e[2]<=0)kills++ } } }
    private fun draw(vertices:FloatArray, buffer:java.nio.FloatBuffer,x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,color:FloatArray) { Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,sx,sy,sz);Matrix.multiplyMM(mvp,0,vp,0,model,0);GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniform4fv(uColor,1,color,0);buffer.position(0);GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,0,buffer);GLES20.glEnableVertexAttribArray(aPos);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertices.size/3) }
}
