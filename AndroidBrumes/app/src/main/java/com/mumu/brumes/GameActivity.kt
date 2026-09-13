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

internal data class GameStats(val life: Int=100, val mana: Int=100, val kills: Int=0, val wave: Int=1,
    val remaining: Int=14, val ranks: List<Int> = listOf(1,1,1), val cooldowns: List<Float> = listOf(0f,0f,0f)) {
    val points: Int get() = (kills/3-ranks.sum()+3).coerceAtLeast(0)
    val level: Int get() = 1+kills/3
}

class GameActivity : Activity() {
    private lateinit var game: BrumesView
    private lateinit var root: FrameLayout
    private var menu: View? = null
    private val gold = Color.rgb(211,177,112)
    private fun dp(n:Int) = (n*resources.displayMetrics.density).toInt()
    private fun panel() = android.graphics.drawable.GradientDrawable().apply {
        setColor(Color.rgb(18,24,30)); cornerRadius=dp(12).toFloat(); setStroke(dp(1),Color.rgb(98,87,65))
    }
    private fun label(value:String,size:Float=16f) = TextView(this).apply {
        text=value; textSize=size; setTextColor(Color.rgb(236,231,219)); setPadding(dp(12),dp(8),dp(12),dp(8))
    }
    private fun button(value:String, action:()->Unit) = Button(this).apply {
        text=value; textSize=12f; isAllCaps=false; backgroundTintList=null; setTextColor(gold); background=panel()
        setOnClickListener { action() }
    }
    override fun onPause() { game.save(); game.onPause(); super.onPause() }
    override fun onResume() { super.onResume(); game.onResume() }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if(menu==null) openMenu() else closeMenu() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        root=FrameLayout(this); root.isMotionEventSplittingEnabled=true
        game=BrumesView(this); root.addView(game)
        val hud=label("BRUMES",13f).apply { background=panel() }
        root.addView(hud,FrameLayout.LayoutParams(dp(260),dp(82),Gravity.TOP or Gravity.START).apply { setMargins(dp(12),dp(10),0,0) })
        root.addView(button("☰  MENU") { openMenu() },FrameLayout.LayoutParams(dp(100),dp(48),Gravity.TOP or Gravity.END).apply { setMargins(0,dp(10),dp(12),0) })
        val names=listOf("Flamme","Givre","Onde")
        val spells=names.mapIndexed { i,name -> button(name) { game.cast(i) } }
        val bar=LinearLayout(this).apply { gravity=Gravity.END; setPadding(0,0,dp(12),dp(12)) }
        spells.forEach { bar.addView(it,LinearLayout.LayoutParams(dp(100),dp(64)).apply { setMargins(dp(8),0,0,0) }) }
        root.addView(bar,FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,dp(82),Gravity.BOTTOM or Gravity.END))
        val objective=label("",12f)
        root.addView(objective,FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,dp(42),Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        val pad=object:View(this) {
            val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            var dx=0f; var dy=0f
            override fun onDraw(canvas:android.graphics.Canvas) {
                val r=width*.43f; paint.color=0x55434D54; canvas.drawCircle(width/2f,height/2f,r,paint)
                paint.style=android.graphics.Paint.Style.STROKE; paint.strokeWidth=dp(2).toFloat();paint.color=gold
                canvas.drawCircle(width/2f,height/2f,r,paint);paint.style=android.graphics.Paint.Style.FILL
                paint.color=0xCCD3B170.toInt();canvas.drawCircle(width/2f+dx*r,height/2f+dy*r,r*.28f,paint)
            }
            override fun onTouchEvent(e:MotionEvent):Boolean {
                when(e.actionMasked) {
                    MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE -> {
                        dx=(e.x-width/2f)/(width*.43f);dy=(e.y-height/2f)/(height*.43f)
                        val length=max(1f,hypot(dx,dy));dx/=length;dy/=length
                    }
                    MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> { dx=0f;dy=0f }
                }
                game.move(dx,dy);invalidate();return true
            }
        }
        root.addView(pad,FrameLayout.LayoutParams(dp(138),dp(138),Gravity.BOTTOM or Gravity.START).apply { setMargins(dp(16),0,0,dp(12)) })
        game.onStats={ s ->
            hud.text="BRUMES  ·  Mage niveau ${s.level}\nVie ${s.life} / 100    Mana ${s.mana} / 100\n${s.points} point(s) de compétence"
            objective.text=if(s.remaining==0) "Vague terminée · Ouvre le menu" else "Vague ${s.wave} · ${s.remaining} ombres restantes"
            spells.forEachIndexed { i,b ->
                val cd=s.cooldowns[i]; val cost=listOf(14,25,38)[i]
                b.text=if(cd>.05f) "${names[i]}\n${ceil(cd).toInt()} s" else "${names[i]}  ${s.ranks[i]}\n$cost mana"
                b.isEnabled=cd<=.05f && s.mana>=cost; b.alpha=if(b.isEnabled)1f else .48f
            }
        }
        setContentView(root)
        openMenu()
    }
    private fun closeMenu() { menu?.let { root.removeView(it) };menu=null;game.pauseWorld(false) }
    private fun openMenu() {
        if(menu!=null)return
        game.pauseWorld(true)
        val shade=FrameLayout(this).apply { setBackgroundColor(0xDC080D13.toInt());isClickable=true }
        val scroll=android.widget.ScrollView(this)
        val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(16),dp(22),dp(16));background=panel() }
        val s=game.stats
        body.addView(label("B R U M E S",27f).apply { setTextColor(gold);gravity=Gravity.CENTER })
        body.addView(label("LES TROIS SANCTUAIRES",12f).apply { gravity=Gravity.CENTER })
        body.addView(label("Niveau ${s.level}  ·  ${s.kills} ombres vaincues  ·  ${s.points} point(s)\nUn point gagné toutes les 3 victoires. Rangs sauvegardés.",13f))
        body.addView(button("JOUER / REPRENDRE") { closeMenu() },LinearLayout.LayoutParams(-1,dp(48)))
        body.addView(button(if(game.detailed) "Graphismes : détaillés" else "Graphismes : équilibrés") {
            game.setQuality(!game.detailed);menu?.let { root.removeView(it) };menu=null;openMenu()
        },LinearLayout.LayoutParams(-1,dp(44)))
        body.addView(label("COMPÉTENCES",16f).apply { setTextColor(gold) })
        val descriptions=listOf("Flamme · dégâts autour du mage · portée 18 m · recharge 1 s", "Givre · dégâts et gel de 2 s · portée 18 m · recharge 4 s", "Onde · frappe puissante · portée 8 m · recharge 7 s")
        descriptions.forEachIndexed { i,desc ->
            val line=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
            line.addView(label("$desc\nRang ${s.ranks[i]}/5 · +10 dégâts par rang",12f),LinearLayout.LayoutParams(0,-2,1f))
            line.addView(button("+ RANG") {
                game.upgrade(i) { menu?.let { root.removeView(it) };menu=null;openMenu() }
            }.apply { isEnabled=s.points>0 && s.ranks[i]<5;alpha=if(isEnabled)1f else .4f },LinearLayout.LayoutParams(dp(90),dp(48)))
            body.addView(line)
        }
        body.addView(button("VAGUE SUIVANTE") { game.nextWave();closeMenu() }.apply { isEnabled=s.remaining==0;alpha=if(isEnabled)1f else .4f },LinearLayout.LayoutParams(-1,dp(48)))
        body.addView(label("Joystick à gauche pour marcher. Sorts à droite.\nLe menu met le monde en pause. Les compétences sont conservées sur ce téléphone.",12f))
        scroll.addView(body);shade.addView(scroll,FrameLayout.LayoutParams(dp(560),-1,Gravity.CENTER))
        root.addView(shade,FrameLayout.LayoutParams(-1,-1));menu=shade
    }
}

class BrumesView(activity: Activity) : GLSurfaceView(activity) {
    private val prefs=activity.getSharedPreferences("brumes_progress",0)
    @Volatile internal var stats=GameStats(kills=prefs.getInt("kills",0).coerceAtLeast(0),ranks=(0..2).map { prefs.getInt("rank$it",1).coerceIn(1,5) })
        private set
    internal var onStats:((GameStats)->Unit)?=null
    var detailed=prefs.getBoolean("detailed",false)
        private set
    private fun resizeSurface() { if(width>0 && height>0) { val scale=if(detailed)1f else .75f;holder.setFixedSize(max(1,(width*scale).toInt()),max(1,(height*scale).toInt())) } }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) { super.onSizeChanged(w,h,oldw,oldh);resizeSurface() }
    fun setQuality(value:Boolean) { detailed=value;prefs.edit().putBoolean("detailed",value).apply();resizeSurface() }
    private val renderer=BrumesRenderer(stats) { value -> stats=value;post { onStats?.invoke(value) } }
    init { setEGLContextClientVersion(2);setRenderer(renderer);renderMode=RENDERMODE_CONTINUOUSLY }
    fun cast(spell:Int) { queueEvent { renderer.cast(spell) } }
    fun move(x:Float,z:Float) { renderer.moveX=x;renderer.moveZ=z }
    fun pauseWorld(value:Boolean) { move(0f,0f);queueEvent { renderer.paused=value } }
    fun nextWave() { queueEvent { renderer.nextWave() } }
    fun upgrade(index:Int, done:()->Unit) { queueEvent { renderer.upgrade(index);post { save();done() } } }
    fun save() { val s=stats;prefs.edit().putInt("kills",s.kills).apply { s.ranks.forEachIndexed { i,r -> putInt("rank$i",r) } }.apply() }
    override fun onPause() { move(0f,0f);super.onPause() }
    override fun onTouchEvent(e:MotionEvent):Boolean { return true }
}

private class BrumesRenderer(initial:GameStats, private val setHud: (GameStats) -> Unit) : GLSurfaceView.Renderer {
    @Volatile var moveX = 0f; @Volatile var moveZ = 0f
    private var program = 0; private var aPos = 0; private var uMvp = 0; private var uColor = 0
    private var aNormal = 0; private var uModel = 0; private var uScale = 0; private var uEye = 0
    var paused=true
    private var wave=1
    private val ranks=initial.ranks.toIntArray()
    private val cooldowns=FloatArray(3)
    private var uGlow=0
    private var lastFrame = 0L; private var nextHud = 0f
    private val projection = FloatArray(16); private val view = FloatArray(16); private val vp = FloatArray(16)
    private val model = FloatArray(16); private val mvp = FloatArray(16)
    private var playerX = 0f; private var playerZ = 0f; private var mana = 100f; private var life = 100f
    private var effect = 0f; private var effectType = 0; private var elapsed = 0f; private var kills = initial.kills
    private val enemies = Array(14) { i -> floatArrayOf(sin(i * 2.4f) * (17 + i % 4 * 8), cos(i * 2.4f) * (17 + i % 4 * 8), 100f, 0f) }
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
    // Flat normals are built once; inverse scale keeps lighting correct on stretched meshes.
    private fun normals(vertices: FloatArray): java.nio.FloatBuffer {
        val values = FloatArray(vertices.size)
        for (i in vertices.indices step 9) {
            val ax=vertices[i+3]-vertices[i]; val ay=vertices[i+4]-vertices[i+1]; val az=vertices[i+5]-vertices[i+2]
            val bx=vertices[i+6]-vertices[i]; val by=vertices[i+7]-vertices[i+1]; val bz=vertices[i+8]-vertices[i+2]
            val nx=ay*bz-az*by; val ny=az*bx-ax*bz; val nz=ax*by-ay*bx
            val length=max(.0001f,sqrt(nx*nx+ny*ny+nz*nz))
            for (j in 0..2) { values[i+j*3]=nx/length; values[i+j*3+1]=ny/length; values[i+j*3+2]=nz/length }
        }
        return java.nio.ByteBuffer.allocateDirect(values.size*4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply { put(values); position(0) }
    }
    private val foliage = floatArrayOf(
        -1f,0f,1f, 1f,0f,1f, 0f,2f,0f,
        1f,0f,1f, 1f,0f,-1f, 0f,2f,0f,
        1f,0f,-1f, -1f,0f,-1f, 0f,2f,0f,
        -1f,0f,-1f, -1f,0f,1f, 0f,2f,0f
    )
    private val fb = java.nio.ByteBuffer.allocateDirect(foliage.size*4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply { put(foliage); position(0) }
    private val cn = normals(cube); private val fn = normals(foliage)
    private val gn = java.nio.ByteBuffer.allocateDirect(ground.size*4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply {
        repeat(ground.size/3) { put(0f); put(1f); put(0f) }; position(0)
    }
    private val earth = floatArrayOf(.23f,.29f,.16f,1f)
    private val bark = floatArrayOf(.23f,.15f,.09f,1f)
    private val leaves = floatArrayOf(.09f,.23f,.14f,1f)
    private val stone = floatArrayOf(.29f,.32f,.31f,1f)
    private val trees = Array(65) { i ->
        val angle=i*2.39996f; val radius=18f+(i%11)*5.6f
        floatArrayOf(sin(angle)*radius,cos(angle)*radius, .8f+(i%5)*.17f)
    }
    override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        lastFrame=0L
        GLES20.glClearColor(.31f,.39f,.43f,1f); GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        val vs = """
            attribute vec3 a; attribute vec3 n;
            uniform mat4 m; uniform mat4 model; uniform vec3 scale;
            varying mediump vec3 world; varying mediump vec3 normal;
            void main() {
                world=(model*vec4(a,1.0)).xyz;
                normal=normalize(n/scale);
                gl_Position=m*vec4(a,1.0);
            }
        """.trimIndent()
        val fs = """
            precision mediump float;
            uniform vec4 c; uniform vec3 eye; uniform float glow;
            varying mediump vec3 world; varying mediump vec3 normal;
            float hash(vec2 p) { return fract(sin(dot(mod(p,64.0),vec2(12.9898,78.233)))*437.58); }
            float noise(vec2 p) {
                vec2 cell=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
                return mix(mix(hash(cell),hash(cell+vec2(1,0)),f.x),mix(hash(cell+vec2(0,1)),hash(cell+vec2(1,1)),f.x),f.y);
            }
            float material(vec2 p) { return noise(p)*.57+noise(p*2.03)*.28+noise(p*4.07)*.15; }
            void main() {
                vec3 base=c.rgb;

                if(world.y < 0.025) {
                    float grain=material(world.xz*3.0);
                    float patch=material(world.xz*.28);
                    base=mix(vec3(.14,.20,.08),vec3(.31,.33,.17),patch)*(.72+grain*.48);
                    float path=1.0-smoothstep(1.1,2.6,abs(world.x-sin(world.z*.085)*5.0));
                    vec3 dirt=mix(vec3(.19,.15,.10),vec3(.42,.35,.24),grain);
                    base=mix(base,dirt,path*.92);
                } else { base *= .78+material((world.xz+world.y*.43)*2.0)*.44; }
                float diffuse=max(dot(normalize(normal),normalize(vec3(-.55,.8,.35))),0.0);
                vec3 lit=base*(vec3(.40,.48,.56)+vec3(.94,.79,.57)*diffuse*.85);
                lit=mix(lit,c.rgb*1.35,glow);
                float distanceToEye=length(world-eye);
                float fog=1.0-exp(-distanceToEye*distanceToEye*.00019);
                gl_FragColor=vec4(mix(lit,vec3(.31,.39,.43),clamp(fog,0.0,.97)),c.a);
            }
        """.trimIndent()
        fun shader(type:Int, source:String) = GLES20.glCreateShader(type).also { GLES20.glShaderSource(it,source); GLES20.glCompileShader(it) }
        program = GLES20.glCreateProgram().also { GLES20.glAttachShader(it,shader(GLES20.GL_VERTEX_SHADER,vs)); GLES20.glAttachShader(it,shader(GLES20.GL_FRAGMENT_SHADER,fs)); GLES20.glLinkProgram(it) }
        uGlow=GLES20.glGetUniformLocation(program,"glow");
        aNormal=GLES20.glGetAttribLocation(program,"n"); uModel=GLES20.glGetUniformLocation(program,"model"); uScale=GLES20.glGetUniformLocation(program,"scale"); uEye=GLES20.glGetUniformLocation(program,"eye")
        aPos=GLES20.glGetAttribLocation(program,"a"); uMvp=GLES20.glGetUniformLocation(program,"m"); uColor=GLES20.glGetUniformLocation(program,"c")
    }
    override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, w:Int, h:Int) { GLES20.glViewport(0,0,w,h); Matrix.perspectiveM(projection,0,62f,w.toFloat()/h,0.1f,180f) }
    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
        val now=System.nanoTime(); val dt=if(lastFrame==0L) 0f else ((now-lastFrame)/1_000_000_000f).coerceIn(0f,.05f); lastFrame=now; if(paused) { renderScene();return }; elapsed += dt;
        for(i in cooldowns.indices) cooldowns[i]=max(0f,cooldowns[i]-dt); mana=min(100f,mana+dt*8); effect=max(0f,effect-dt)
        playerX=(playerX+moveX*dt*8).coerceIn(-82f,82f); playerZ=(playerZ+moveZ*dt*8).coerceIn(-82f,82f)
        enemies.forEach { e -> if(e[2]>0) { val dx=playerX-e[0]; val dz=playerZ-e[1]; val d=max(.1f,sqrt(dx*dx+dz*dz)); e[3]=max(0f,e[3]-dt); if(d<26 && e[3]<=0f) { e[0]+=dx/d*dt*1.35f; e[1]+=dz/d*dt*1.35f }; if(d<1.7f) life=max(0f,life-dt*5) } }
        if(life<=0) { playerX=0f;playerZ=0f;life=100f;mana=100f;resetEnemies() }
        if(elapsed>=nextHud) { nextHud=elapsed+.25f;publish() }
        renderScene()
    }
    private fun renderScene() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT); GLES20.glUseProgram(program)
        Matrix.setLookAtM(view,0,playerX,11f,playerZ+12f,playerX,0f,playerZ,0f,1f,0f); Matrix.multiplyMM(vp,0,projection,0,view,0)
        GLES20.glUniform3f(uEye,playerX,11f,playerZ+12f)
        draw(ground,gb,0f,0f,0f,1f,1f,1f,earth)
        trees.forEach { t ->
            val x=t[0]; val z=t[1]; val size=t[2]
            if (abs(x-playerX)<65f && abs(z-playerZ)<65f) {
                draw(cube,cb,x,1.7f*size,z,.22f*size,1.7f*size,.22f*size,bark)
                draw(foliage,fb,x,1.5f*size,z,1.7f*size,1.9f*size,1.7f*size,leaves)
                draw(foliage,fb,x,3.1f*size,z,1.25f*size,1.5f*size,1.25f*size,leaves)
            }
        }
        // Distant ridges give the small exploration area a visible horizon.
        for (i in 0 until 18) {
            val angle=i*.3491f; val x=sin(angle)*92f; val z=cos(angle)*92f
            draw(foliage,fb,x,-.2f,z,12f+(i%3)*4f,8f+(i%5)*2.2f,14f,stone)
        }
        for(i in 0 until 3) { val a=i*2.09f; draw(cube,cb,sin(a)*53,2.4f,cos(a)*53,2.7f,2.4f,2.7f,if(effect>0) floatArrayOf(1f,.8f,.2f,1f) else floatArrayOf(.1f,.8f,1f,1f)) }
        draw(cube,cb,playerX,1f,playerZ,.55f,1f,.55f,floatArrayOf(.27f,.18f,.9f,1f)); draw(cube,cb,playerX,2.15f,playerZ,.25f,.25f,.25f,floatArrayOf(.96f,.72f,.48f,1f))
        enemies.forEach { e -> if(e[2]>0) draw(cube,cb,e[0],.85f,e[1],.55f,.85f,.55f,if(e[3]>0f) floatArrayOf(.1f,.8f,1f,1f) else floatArrayOf(.65f,.08f,.38f,1f)) }
        if(effect>0) {
            val radius=.8f+(1f-effect)*7f
            val color=when(effectType){0->floatArrayOf(1f,.35f,.05f,1f);1->floatArrayOf(.2f,.85f,1f,1f);else->floatArrayOf(.75f,.3f,1f,1f)}
            for(i in 0 until 40) {
                val a=i*.15708f
                val size=.08f+effect*.12f
                draw(cube,cb,playerX+cos(a)*radius,.3f+sin(a*3f+elapsed*6f)*.15f,playerZ+sin(a)*radius,size,size,size,color,1f)
            }
            for(i in 0 until 12) {
                val a=i*2.4f+elapsed
                draw(cube,cb,playerX+cos(a)*radius*.65f,.5f+(1f-effect)*(1+i%4),playerZ+sin(a)*radius*.65f,.08f,.16f,.08f,color,1f)
            }
        }

    }
    private fun publish() { setHud(GameStats(life.toInt(),mana.toInt(),kills,wave,enemies.count { it[2]>0 },ranks.toList(),cooldowns.toList())) }
    private fun resetEnemies() {
        enemies.forEachIndexed { i,e -> e[0]=sin(i*2.4f)*(17+i%4*8);e[1]=cos(i*2.4f)*(17+i%4*8);e[2]=100f+(wave-1)*15f;e[3]=0f }
    }
    fun nextWave() { if(enemies.any { it[2]>0 })return;wave++;life=100f;mana=100f;resetEnemies();publish() }
    fun upgrade(index:Int) {
        if(index !in 0..2 || ranks[index]>=5 || kills/3-(ranks.sum()-3)<=0)return
        ranks[index]++;publish()
    }
    fun cast(type:Int) {
        if(paused || type !in 0..2 || cooldowns[type]>0f)return
        val cost=when(type){0->14f;1->25f;else->38f}
        if(mana<cost)return
        mana-=cost;effect=1f;effectType=type;cooldowns[type]=when(type){0->1f;1->4f;else->7f}
        val range=if(type==2)8f else 18f
        enemies.forEach { e -> if(e[2]>0 && hypot(e[0]-playerX,e[1]-playerZ)<range) {
            e[2]-=(when(type){0->45f;1->30f;else->70f})+(ranks[type]-1)*10f
            if(type==1)e[3]=2f
            if(e[2]<=0)kills++
        } }
        publish()
    }
    private fun draw(vertices:FloatArray, buffer:java.nio.FloatBuffer,x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,color:FloatArray,glow:Float=0f) { Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,sx,sy,sz);Matrix.multiplyMM(mvp,0,vp,0,model,0);GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(uModel,1,false,model,0);GLES20.glUniform3f(uScale,sx,sy,sz);val nb=if(vertices===ground)gn else if(vertices===foliage)fn else cn;nb.position(0);GLES20.glVertexAttribPointer(aNormal,3,GLES20.GL_FLOAT,false,0,nb);GLES20.glEnableVertexAttribArray(aNormal);GLES20.glUniform1f(uGlow,glow);GLES20.glUniform4fv(uColor,1,color,0);buffer.position(0);GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,0,buffer);GLES20.glEnableVertexAttribArray(aPos);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertices.size/3) }
}

