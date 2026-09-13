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
    val shrineMask:Int=0, val guidance:String="Explore les trois sanctuaires", val remaining: Int=14, val ranks: List<Int> = listOf(1,1,1), val cooldowns: List<Float> = listOf(0f,0f,0f)) {
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
        root.addView(objective,FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,dp(66),Gravity.TOP or Gravity.CENTER_HORIZONTAL))
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
            objective.text="${s.guidance}\n"+(if(s.remaining==0) "Vague terminée · Ouvre le menu" else "Vague ${s.wave} · ${s.remaining} ombres")
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
        body.addView(label("Joystick à gauche pour marcher. Glisse sur le décor pour tourner la caméra. Sorts à droite.\nLe menu met le monde en pause. Les compétences sont conservées sur ce téléphone.",12f))
        scroll.addView(body);shade.addView(scroll,FrameLayout.LayoutParams(dp(560),-1,Gravity.CENTER))
        root.addView(shade,FrameLayout.LayoutParams(-1,-1));menu=shade
    }
}

class BrumesView(activity: Activity) : GLSurfaceView(activity) {
    private val prefs=activity.getSharedPreferences("brumes_progress",0)
    @Volatile internal var stats=GameStats(shrineMask=prefs.getInt("shrines",0) and 7,kills=prefs.getInt("kills",0).coerceAtLeast(0),ranks=(0..2).map { prefs.getInt("rank$it",1).coerceIn(1,5) })
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
    fun save() { val s=stats;prefs.edit().putInt("shrines",s.shrineMask).putInt("kills",s.kills).apply { s.ranks.forEachIndexed { i,r -> putInt("rank$i",r) } }.apply() }
    override fun onPause() { move(0f,0f);super.onPause() }
    private var lookX=0f; private var lookY=0f
    override fun onTouchEvent(e:MotionEvent):Boolean {
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lookX=e.x;lookY=e.y }
            MotionEvent.ACTION_MOVE -> {
                val dx=(e.x-lookX)/resources.displayMetrics.density
                val dy=(e.y-lookY)/resources.displayMetrics.density
                lookX=e.x;lookY=e.y;queueEvent { renderer.look(dx,dy) }
            }
        }
        return true
    }
}

private class BrumesRenderer(initial:GameStats, private val setHud: (GameStats) -> Unit) : GLSurfaceView.Renderer {
    @Volatile var moveX = 0f; @Volatile var moveZ = 0f
    private var program = 0; private var aPos = 0; private var uMvp = 0; private var uColor = 0
    private var aNormal = 0; private var uModel = 0; private var uScale = 0; private var uEye = 0
    var paused=true
    private var wave=1
    private var shrineMask=initial.shrineMask;private var checkpoint=-1
    private val hitColor=floatArrayOf(1f,.72f,.32f,1f)
    private val healthColor=floatArrayOf(.32f,.9f,.38f,1f)
    private val ranks=initial.ranks.toIntArray()
    private val cooldowns=FloatArray(3)
    private var uGlow=0;private var uKind=0;private var uTime=0;private var uShadows=0
    private val shadowPoints=FloatArray(24)
    private var yaw=0f;private var cameraHeight=6f
    fun look(dx:Float,dy:Float) { if(!paused) { yaw=(yaw-dx*.006f)%(2f*PI.toFloat());cameraHeight=(cameraHeight+dy*.035f).coerceIn(3.4f,13f) } }
    private fun heightAt(x:Float,z:Float):Float = sin(x*.065f)*cos(z*.05f)*2.3f+sin(z*.12f+x*.035f)*.75f+cos(x*.16f-z*.09f)*.4f-.4f
    private fun buffer(values:FloatArray)=java.nio.ByteBuffer.allocateDirect(values.size*4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply { put(values);position(0) }
    private var lastFrame = 0L; private var nextHud = 0f
    private val projection = FloatArray(16); private val view = FloatArray(16); private val vp = FloatArray(16)
    private val model = FloatArray(16); private val mvp = FloatArray(16)
    private var playerX = 0f; private var playerZ = 0f; private var mana = 100f; private var life = 100f
    private var effect = 0f; private var effectType = 0; private var elapsed = 0f; private var kills = initial.kills
    private val enemies = Array(14) { i -> floatArrayOf(sin(i * 2.4f) * (17 + i % 4 * 8), cos(i * 2.4f) * (17 + i % 4 * 8), 100f, 0f, 0f, 0f, 0f) }
    private val cube = floatArrayOf(
        -1f,-1f, 1f, 1f,-1f, 1f, 1f,1f, 1f, -1f,-1f, 1f, 1f,1f, 1f, -1f,1f, 1f,
        1f,-1f,-1f, -1f,-1f,-1f, -1f,1f,-1f, 1f,-1f,-1f, -1f,1f,-1f, 1f,1f,-1f,
        -1f,-1f,-1f, -1f,-1f,1f, -1f,1f,1f, -1f,-1f,-1f, -1f,1f,1f, -1f,1f,-1f,
        1f,-1f,1f, 1f,-1f,-1f, 1f,1f,-1f, 1f,-1f,1f, 1f,1f,-1f, 1f,1f,1f,
        -1f,1f,1f, 1f,1f,1f, 1f,1f,-1f, -1f,1f,1f, 1f,1f,-1f, -1f,1f,-1f,
        -1f,-1f,-1f, 1f,-1f,-1f, 1f,-1f,1f, -1f,-1f,-1f, 1f,-1f,1f, -1f,-1f,1f
    )
    private val ground = run {
        val values=FloatArray(80*80*18);var i=0
        fun v(x:Float,z:Float) { values[i++]=x;values[i++]=heightAt(x,z);values[i++]=z }
        for(z in 0 until 80) for(x in 0 until 80) {
            val px=-100f+x*2.5f;val pz=-100f+z*2.5f
            v(px,pz);v(px,pz+2.5f);v(px+2.5f,pz+2.5f)
            v(px,pz);v(px+2.5f,pz+2.5f);v(px+2.5f,pz)
        };values
    }
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
    private val foliage = run {
        val values=ArrayList<Float>()
        for(i in 0 until 12) {
            val a=i*PI.toFloat()/6;val b=(i+1)*PI.toFloat()/6
            val ra=1f+sin(i*2.7f)*.14f;val rb=1f+sin((i+1)%12*2.7f)*.14f
            values.addAll(listOf(cos(a)*ra,.12f+sin(i*2f)*.12f,sin(a)*ra, 0f,2f,0f, cos(b)*rb,.12f+sin((i+1)%12*2f)*.12f,sin(b)*rb))
        };values.toFloatArray()
    }
    private val grass = run {
        val values=ArrayList<Float>()
        for(i in 0 until 1200) {
            val x=sin(i*127.13f)*75f;val z=cos(i*71.71f)*75f
            if(abs(x-sin(z*.085f)*5f)<2.8f)continue
            val h=heightAt(x,z);val tall=.25f+(i%5)*.11f;val w=.16f
            values.addAll(listOf(x-w,h,z,x+w,h,z,x+.07f,h+tall,z, x,h,z-w,x,h,z+w,x+.07f,h+tall,z))
        };values.toFloatArray()
    }
    private val grb=buffer(grass)
    private val grn=normals(grass)
    private val orb = run {
        val vertices=ArrayList<Float>()
        fun point(lat:Float,lon:Float) { vertices.add(cos(lat)*cos(lon));vertices.add(sin(lat));vertices.add(cos(lat)*sin(lon)) }
        for(y in 0 until 8) for(x in 0 until 12) {
            val a=-PI.toFloat()/2+y*PI.toFloat()/8;val b=a+PI.toFloat()/8
            val c=x*PI.toFloat()/6;val d=c+PI.toFloat()/6
            point(a,c);point(b,c);point(b,d);point(a,c);point(b,d);point(a,d)
        }
        vertices.toFloatArray()
    }
    private val ob=java.nio.ByteBuffer.allocateDirect(orb.size*4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply { put(orb);position(0) }
    private val on=java.nio.ByteBuffer.allocateDirect(orb.size*4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply { put(orb);position(0) }
    private val cloth=floatArrayOf(.15f,.12f,.29f,1f)
    private val trim=floatArrayOf(.64f,.47f,.19f,1f)
    private val skin=floatArrayOf(.72f,.52f,.36f,1f)
    private val shadow=floatArrayOf(.16f,.09f,.18f,1f)
    private val magic=floatArrayOf(.24f,.72f,1f,1f)
    private val fb = java.nio.ByteBuffer.allocateDirect(foliage.size*4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply { put(foliage); position(0) }
    private val cn = normals(cube); private val fn = normals(foliage)
    private val gn = run {
        val values=FloatArray(ground.size)
        for(i in ground.indices step 3) {
            val x=ground[i];val z=ground[i+2]
            val nx=(heightAt(x-.1f,z)-heightAt(x+.1f,z))/.2f
            val nz=(heightAt(x,z-.1f)-heightAt(x,z+.1f))/.2f
            val length=sqrt(nx*nx+1f+nz*nz)
            values[i]=nx/length;values[i+1]=1f/length;values[i+2]=nz/length
        };buffer(values)
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
            uniform mediump float kind; uniform float clock;
            varying mediump vec3 world; varying mediump vec3 normal;
            void main() {
                vec3 position=a;
                if(kind>2.5 && kind<3.5) position.x+=sin(clock*1.4+a.z*.6+a.x)*.035;
                world=(model*vec4(position,1.0)).xyz;
                normal=normalize(n/scale);
                gl_Position=m*vec4(position,1.0);
            }
        """.trimIndent()
        val fs = """
            precision mediump float;
            uniform vec4 c; uniform vec3 eye; uniform float glow;
            uniform mediump float kind; uniform vec3 shadows[8];
            varying mediump vec3 world; varying mediump vec3 normal;
            float hash(vec2 p) { return fract(sin(dot(mod(p,64.0),vec2(12.9898,78.233)))*437.58); }
            float noise(vec2 p) {
                vec2 cell=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
                return mix(mix(hash(cell),hash(cell+vec2(1,0)),f.x),mix(hash(cell+vec2(0,1)),hash(cell+vec2(1,1)),f.x),f.y);
            }
            float material(vec2 p) { return noise(p)*.57+noise(p*2.03)*.28+noise(p*4.07)*.15; }
            void main() {
                vec3 base=c.rgb;

                if(kind>.5 && kind<1.5) {
                    float grain=material(world.xz*3.0);
                    float patch=material(world.xz*.28);
                    base=mix(vec3(.14,.20,.08),vec3(.31,.33,.17),patch)*(.72+grain*.48);
                    float path=1.0-smoothstep(1.1,2.6,abs(world.x-sin(world.z*.085)*5.0));
                    vec3 dirt=mix(vec3(.19,.15,.10),vec3(.42,.35,.24),grain);
                    base=mix(base,dirt,path*.92);
                    float steep=1.0-smoothstep(.87,.99,normalize(normal).y);
                    base=mix(base,vec3(.27,.26,.23)*(.75+grain*.4),steep*.5);
                    float shade=0.0;
                    for(int i=0;i<8;i++) {
                        vec2 delta=(world.xz-shadows[i].xy-vec2(.3,-.2))/vec2(1.0,.65);
                        float distanceSquared=dot(delta,delta);
                        shade=max(shade,(1.0-smoothstep(.05,1.9,distanceSquared))*shadows[i].z);
                    }
                    base*=1.0-shade*.48;
                } else { base *= .78+material((world.xz+world.y*.43)*2.0)*.44; }
                float diffuse=max(dot(normalize(normal),normalize(vec3(-.55,.8,.35))),0.0);
                vec3 lit=base*(vec3(.40,.48,.56)+vec3(.94,.79,.57)*diffuse*.85);
                lit=mix(lit,c.rgb*1.35,glow);
                float distanceToEye=length(world-eye);
                float fog=1.0-exp(-pow(distanceToEye*.012,2.0));
                gl_FragColor=vec4(mix(lit,vec3(.31,.39,.43),clamp(fog,0.0,.97)),c.a);
            }
        """.trimIndent()
        fun shader(type:Int, source:String) = GLES20.glCreateShader(type).also { GLES20.glShaderSource(it,source); GLES20.glCompileShader(it) }
        program = GLES20.glCreateProgram().also { GLES20.glAttachShader(it,shader(GLES20.GL_VERTEX_SHADER,vs)); GLES20.glAttachShader(it,shader(GLES20.GL_FRAGMENT_SHADER,fs)); GLES20.glLinkProgram(it) }
        uKind=GLES20.glGetUniformLocation(program,"kind");uTime=GLES20.glGetUniformLocation(program,"clock");uShadows=GLES20.glGetUniformLocation(program,"shadows[0]")
        uGlow=GLES20.glGetUniformLocation(program,"glow");
        aNormal=GLES20.glGetAttribLocation(program,"n"); uModel=GLES20.glGetUniformLocation(program,"model"); uScale=GLES20.glGetUniformLocation(program,"scale"); uEye=GLES20.glGetUniformLocation(program,"eye")
        aPos=GLES20.glGetAttribLocation(program,"a"); uMvp=GLES20.glGetUniformLocation(program,"m"); uColor=GLES20.glGetUniformLocation(program,"c")
    }
    override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, w:Int, h:Int) { GLES20.glViewport(0,0,w,h); Matrix.perspectiveM(projection,0,62f,w.toFloat()/h,0.1f,180f) }
    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
        val now=System.nanoTime(); val dt=if(lastFrame==0L) 0f else ((now-lastFrame)/1_000_000_000f).coerceIn(0f,.05f); lastFrame=now; if(paused) { renderScene();return }; elapsed += dt;
        for(i in cooldowns.indices) cooldowns[i]=max(0f,cooldowns[i]-dt); mana=min(100f,mana+dt*8); effect=max(0f,effect-dt)
        val oldX=playerX;val oldZ=playerZ
        val mx=moveX*cos(yaw)+moveZ*sin(yaw);val mz=-moveX*sin(yaw)+moveZ*cos(yaw)
        playerX=(playerX+mx*dt*8).coerceIn(-82f,82f);playerZ=(playerZ+mz*dt*8).coerceIn(-82f,82f)
        trees.forEach { t -> if(hypot(playerX-t[0],playerZ-t[1])<.55f+t[2]*.22f) { playerX=oldX;playerZ=oldZ } }
        enemies.forEach { e -> e[4]=max(0f,e[4]-dt);e[5]=max(0f,e[5]-dt);e[6]=max(0f,e[6]-dt);if(e[2]>0) { val dx=playerX-e[0]; val dz=playerZ-e[1]; val d=max(.1f,sqrt(dx*dx+dz*dz)); e[3]=max(0f,e[3]-dt); if(d<26 && e[3]<=0f) { e[0]+=dx/d*dt*1.35f; e[1]+=dz/d*dt*1.35f }; if(d<1.7f && e[3]<=0f && e[6]<=0f) { life=max(0f,life-7f);e[6]=1.1f } } }
        if(life<=0) { playerX=if(checkpoint<0)0f else sin(checkpoint*2.09f)*53+4f;playerZ=if(checkpoint<0)0f else cos(checkpoint*2.09f)*53;life=100f;mana=100f;resetEnemies() }
        for(i in 0 until 3) {
            val x=sin(i*2.09f)*53;val z=cos(i*2.09f)*53
            if(hypot(playerX-x,playerZ-z)<3f) {
                checkpoint=i
                if(shrineMask and (1 shl i)==0) { shrineMask=shrineMask or (1 shl i);life=100f;mana=100f }
                if(enemies.none { it[2]>0 && hypot(it[0]-playerX,it[1]-playerZ)<8f }) { life=min(100f,life+dt*20);mana=min(100f,mana+dt*25) }
            }
        }
        if(elapsed>=nextHud) { nextHud=elapsed+.25f;publish() }
        renderScene()
    }
    private fun renderScene() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT); GLES20.glUseProgram(program)
        val eyeX=playerX+sin(yaw)*11f;val eyeZ=playerZ+cos(yaw)*11f
        val eyeY=max(heightAt(playerX,playerZ)+cameraHeight,heightAt(eyeX,eyeZ)+2.8f)
        Matrix.setLookAtM(view,0,eyeX,eyeY,eyeZ,playerX,heightAt(playerX,playerZ)+1.1f,playerZ,0f,1f,0f);Matrix.multiplyMM(vp,0,projection,0,view,0)
        GLES20.glUniform3f(uEye,eyeX,eyeY,eyeZ);GLES20.glUniform1f(uTime,elapsed)
        shadowPoints.fill(0f);shadowPoints[0]=playerX;shadowPoints[1]=playerZ;shadowPoints[2]=1f
        var shadowIndex=1
        enemies.forEach { e -> if(e[2]>0 && shadowIndex<8 && hypot(e[0]-playerX,e[1]-playerZ)<24f) {
            shadowPoints[shadowIndex*3]=e[0];shadowPoints[shadowIndex*3+1]=e[1];shadowPoints[shadowIndex*3+2]=.75f;shadowIndex++
        } }
        GLES20.glUniform3fv(uShadows,8,shadowPoints,0)
        draw(ground,gb,0f,0f,0f,1f,1f,1f,earth)
        draw(grass,grb,0f,0f,0f,1f,1f,1f,leaves)
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
            draw(orb,ob,x,2f,z,12f+(i%3)*4f,8f+(i%5)*2.2f,14f,stone)
        }
        for(i in 0 until 3) {
            val a=i*2.09f;val x=sin(a)*53;val z=cos(a)*53
            draw(cube,cb,x,.15f,z,3.1f,.15f,3.1f,stone)
            for(side in -1..1 step 2) {
                draw(cube,cb,x+side*2.1f,2.1f,z,.38f,2.1f,.44f,stone)
                draw(cube,cb,x+side*2.1f,.35f,z,.64f,.35f,.67f,stone)
            }
            draw(cube,cb,x,4.2f,z,2.6f,.36f,.52f,stone)
            draw(orb,ob,x,1.8f+sin(elapsed+i)*.15f,z,.42f,.65f,.42f,if(shrineMask and (1 shl i)!=0)healthColor else magic,1f)
            if(shrineMask and (1 shl i)==0) draw(cube,cb,x,6f,z,.045f,5f,.045f,magic,1f)
        }
        for(i in 0 until 24) {
            val x=sin(i*2.39f)*((i%6)*9+12);val z=cos(i*1.67f)*((i%5)*12+16)
            draw(orb,ob,x,.25f,z,.55f+(i%3)*.25f,.6f,.5f+(i%4)*.2f,stone)
        }
        val stride=sin(elapsed*9f)*min(1f,hypot(moveX,moveZ))*.25f
        // Layered robe, articulated limbs and an emissive staff replace the block avatar.
        draw(orb,ob,playerX,.85f,playerZ,.58f,.88f,.44f,cloth)
        draw(orb,ob,playerX,1.55f,playerZ,.40f,.54f,.30f,cloth)
        draw(orb,ob,playerX,2.16f,playerZ,.31f,.35f,.31f,skin)
        draw(orb,ob,playerX,2.31f,playerZ+.10f,.35f,.28f,.30f,cloth)
        draw(orb,ob,playerX-.23f,.23f,playerZ+stride,.19f,.24f,.32f,bark)
        draw(orb,ob,playerX+.23f,.23f,playerZ-stride,.19f,.24f,.32f,bark)
        draw(orb,ob,playerX-.49f,1.35f+stride*.3f,playerZ,.18f,.44f,.20f,cloth)
        draw(orb,ob,playerX+.49f,1.43f,playerZ,.18f,.40f,.20f,cloth)
        draw(orb,ob,playerX,1.05f,playerZ,.48f,.065f,.34f,trim)
        draw(cube,cb,playerX+.72f,1.25f,playerZ,.055f,1.22f,.055f,bark)
        draw(orb,ob,playerX+.72f,2.57f,playerZ,.15f,.24f,.15f,magic,1f)
        enemies.forEachIndexed { i,e -> if(e[2]>0) {
            val hp=e[2]/(100f+(wave-1)*15f)
            if(hypot(e[0]-playerX,e[1]-playerZ)<22f) for(segment in 0 until 8) {
                val offset=(segment-3.5f)*.12f
                draw(orb,ob,e[0]+cos(yaw)*offset,2.65f,e[1]-sin(yaw)*offset,.045f,.045f,.045f,if(segment/8f<hp)healthColor else shadow,1f)
            }
            val frozen=e[3]>0f
            val bob=if(frozen)0f else sin(elapsed*3f+i)*.12f
            val material=if(e[4]>0f)hitColor else if(frozen)magic else shadow
            draw(orb,ob,e[0],.95f+bob,e[1],.48f,.82f,.38f,material)
            draw(orb,ob,e[0],1.85f+bob,e[1],.30f,.36f,.29f,material)
            draw(orb,ob,e[0]-.51f,1.04f+bob,e[1],.16f,.53f,.18f,material)
            draw(orb,ob,e[0]+.51f,1.04f+bob,e[1],.16f,.53f,.18f,material)
            draw(orb,ob,e[0]-.11f,1.91f+bob,e[1]+.27f,.055f,.04f,.03f,magic,1f)
            draw(orb,ob,e[0]+.11f,1.91f+bob,e[1]+.27f,.055f,.04f,.03f,magic,1f)
        } }
        enemies.forEach { e -> if(e[5]>0f) {
            val progress=1f-e[5]/.7f
            for(i in 0 until 8) { val angle=i*PI.toFloat()/4
                draw(orb,ob,e[0]+cos(angle)*progress,1f+progress*1.8f,e[1]+sin(angle)*progress,.09f,.09f,.09f,magic,1f)
            }
        } }
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
    private fun publish() {
        val count=Integer.bitCount(shrineMask)
        var nearest=1000f
        for(i in 0 until 3) if(shrineMask and (1 shl i)==0) nearest=min(nearest,hypot(playerX-sin(i*2.09f)*53,playerZ-cos(i*2.09f)*53))
        val guide=if(count==3) "3/3 sanctuaires éveillés" else "$count/3 sanctuaires · prochain à ${nearest.toInt()} m"
        setHud(GameStats(life=life.toInt(),mana=mana.toInt(),kills=kills,wave=wave,shrineMask=shrineMask,guidance=guide,remaining=enemies.count { it[2]>0 },ranks=ranks.toList(),cooldowns=cooldowns.toList()))
    }
    private fun resetEnemies() {
        enemies.forEachIndexed { i,e -> e[0]=sin(i*2.4f)*(17+i%4*8);e[1]=cos(i*2.4f)*(17+i%4*8);e[2]=100f+(wave-1)*15f;e[3]=0f;e[4]=0f;e[5]=0f;e[6]=0f }
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
            e[4]=.2f
            if(type==2) {
                val distance=max(.1f,hypot(e[0]-playerX,e[1]-playerZ))
                e[0]=(e[0]+(e[0]-playerX)/distance*1.8f).coerceIn(-82f,82f)
                e[1]=(e[1]+(e[1]-playerZ)/distance*1.8f).coerceIn(-82f,82f)
            }
            if(e[2]<=0) { kills++;e[5]=.7f }
        } }
        publish()
    }
    private fun draw(vertices:FloatArray, buffer:java.nio.FloatBuffer,x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,color:FloatArray,glow:Float=0f) { Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y+(if(vertices===ground || vertices===grass)0f else heightAt(x,z)),z);Matrix.scaleM(model,0,sx,sy,sz);Matrix.multiplyMM(mvp,0,vp,0,model,0);GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(uModel,1,false,model,0);GLES20.glUniform3f(uScale,sx,sy,sz);val nb=if(vertices===ground)gn else if(vertices===grass)grn else if(vertices===foliage)fn else if(vertices===orb)on else cn;nb.position(0);GLES20.glVertexAttribPointer(aNormal,3,GLES20.GL_FLOAT,false,0,nb);GLES20.glEnableVertexAttribArray(aNormal);GLES20.glUniform1f(uKind,if(vertices===ground)1f else if(vertices===grass || vertices===foliage)3f else 0f);GLES20.glUniform1f(uGlow,glow);GLES20.glUniform4fv(uColor,1,color,0);buffer.position(0);GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,0,buffer);GLES20.glEnableVertexAttribArray(aPos);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertices.size/3) }
}

