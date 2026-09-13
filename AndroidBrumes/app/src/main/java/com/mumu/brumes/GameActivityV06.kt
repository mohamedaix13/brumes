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
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.*

private data class V06Hud(val life:Int,val mana:Int,val world:String,val flying:Boolean,val enemies:Int,val spell:String)

class GameActivityV06 : Activity() {
    private lateinit var root:FrameLayout
    private lateinit var game:BrumesV06View
    private var menu:View?=null
    private val gold=Color.rgb(226,190,117)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun panel(alpha:Int=205)=android.graphics.drawable.GradientDrawable().apply{
        setColor(Color.argb(alpha,7,12,18));cornerRadius=dp(14).toFloat();setStroke(dp(1),Color.argb(150,226,190,117))
    }
    private fun text(t:String,size:Float=12f)=TextView(this).apply{this.text=t;textSize=size;setTextColor(Color.rgb(244,240,232));setPadding(dp(10),dp(6),dp(10),dp(6))}
    private fun button(t:String,wide:Int=76,onClick:()->Unit)=Button(this).apply{
        text=t;textSize=10f;isAllCaps=false;setTextColor(gold);backgroundTintList=null;background=panel(188);setPadding(dp(4),0,dp(4),0);setOnClickListener{onClick()};minWidth=0;minHeight=0
        layoutParams=LinearLayout.LayoutParams(dp(wide),dp(44))
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility=(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        root=FrameLayout(this).apply{isMotionEventSplittingEnabled=true}
        game=BrumesV06View(this);root.addView(game,FrameLayout.LayoutParams(-1,-1))

        val hud=text("BRUMES 0.6",11f).apply{background=panel(190)}
        root.addView(hud,FrameLayout.LayoutParams(dp(255),dp(70),Gravity.TOP or Gravity.START).apply{setMargins(dp(10),dp(8),0,0)})
        root.addView(button("☰",54){openMenu()},FrameLayout.LayoutParams(dp(58),dp(44),Gravity.TOP or Gravity.END).apply{setMargins(0,dp(8),dp(10),0)})

        val left=V06Stick(this){x,y->game.move(x,y)}
        root.addView(left,FrameLayout.LayoutParams(dp(130),dp(130),Gravity.BOTTOM or Gravity.START).apply{setMargins(dp(12),0,0,dp(12))})
        val right=V06Stick(this){x,y->game.look(x,y)}
        root.addView(right,FrameLayout.LayoutParams(dp(118),dp(118),Gravity.BOTTOM or Gravity.END).apply{setMargins(0,0,dp(12),dp(12))})

        val spells=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        spells.addView(button("🔥 Feu",68){game.element(0)})
        spells.addView(button("💧 Eau",68){game.element(1)})
        spells.addView(button("🌪 Air",68){game.element(2)})
        spells.addView(button("🪨 Terre",68){game.element(3)})
        root.addView(spells,FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,dp(48),Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply{setMargins(0,0,0,dp(10))})

        val actions=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER}
        actions.addView(button("✦ Esquive",82){game.dash()})
        actions.addView(button("☁ Vol",82){game.flight()},LinearLayout.LayoutParams(dp(82),dp(44)).apply{topMargin=dp(5)})
        actions.addView(button("◉ Ruine",82){game.fastTravel()},LinearLayout.LayoutParams(dp(82),dp(44)).apply{topMargin=dp(5)})
        root.addView(actions,FrameLayout.LayoutParams(dp(88),FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER_VERTICAL or Gravity.END).apply{setMargins(0,0,dp(14),0)})

        game.onHud={s->hud.text="BRUMES 0.6 · ${s.world}\nVie ${s.life}   Mana ${s.mana}   ${if(s.flying)"VOL" else "SOL"}\n${s.enemies} ennemis · ${s.spell}"}
        setContentView(root)
    }
    override fun onPause(){game.onPause();super.onPause()}
    override fun onResume(){super.onResume();game.onResume()}
    @Deprecated("Deprecated in Java") override fun onBackPressed(){if(menu==null)openMenu() else closeMenu()}
    private fun closeMenu(){menu?.let{root.removeView(it)};menu=null;game.setPaused(false)}
    private fun openMenu(){
        if(menu!=null)return
        game.setPaused(true)
        val shade=FrameLayout(this).apply{setBackgroundColor(0xC9000000.toInt());isClickable=true}
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(18),dp(16),dp(18),dp(16));background=panel(240)}
        body.addView(text("B R U M E S",25f).apply{gravity=Gravity.CENTER;setTextColor(gold)})
        body.addView(text("Version 0.6 · reconstruction complète",12f).apply{gravity=Gravity.CENTER})
        body.addView(button("REPRENDRE",210){closeMenu()},LinearLayout.LayoutParams(dp(210),dp(48)).apply{topMargin=dp(8)})
        body.addView(text("Deux joysticks · vol libre · esquive instantanée · magie combinée · portails · ruines de voyage rapide",11f).apply{gravity=Gravity.CENTER})
        shade.addView(body,FrameLayout.LayoutParams(dp(430),FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER));root.addView(shade,FrameLayout.LayoutParams(-1,-1));menu=shade
    }
}

private class V06Stick(activity:Activity,val out:(Float,Float)->Unit):View(activity){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG);private var dx=0f;private var dy=0f
    override fun onDraw(c:Canvas){
        val r=min(width,height)*.40f;p.color=0x2810151B;p.style=Paint.Style.FILL;c.drawCircle(width/2f,height/2f,r,p)
        p.color=0x99E2BE75.toInt();p.style=Paint.Style.STROKE;p.strokeWidth=2.2f;c.drawCircle(width/2f,height/2f,r,p)
        p.style=Paint.Style.FILL;p.color=0xB5E2BE75.toInt();c.drawCircle(width/2f+dx*r,height/2f+dy*r,r*.23f,p)
    }
    override fun onTouchEvent(e:MotionEvent):Boolean{
        when(e.actionMasked){MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE->{dx=(e.x-width/2)/(width*.40f);dy=(e.y-height/2)/(height*.40f);val l=max(1f,hypot(dx,dy));dx/=l;dy/=l};else->{dx=0f;dy=0f}}
        out(dx,dy);invalidate();return true
    }
}

private data class Mob(var x:Float,var z:Float,val friendly:Boolean,var phase:Float,var hp:Float=100f,var cooldown:Float=0f)
private data class Spark(var x:Float,var y:Float,var z:Float,var vx:Float,var vy:Float,var vz:Float,var life:Float,val r:Float,val g:Float,val b:Float,val size:Float)

private class BrumesV06View(activity:Activity):GLSurfaceView(activity){
    internal var onHud:((V06Hud)->Unit)?=null
    private val r=V06Renderer{h->post{onHud?.invoke(h)}}
    init{setEGLContextClientVersion(2);setRenderer(r);renderMode=RENDERMODE_CONTINUOUSLY}
    fun move(x:Float,y:Float){r.mx=x;r.my=y}
    fun look(x:Float,y:Float){r.lx=x;r.ly=y}
    fun element(i:Int){queueEvent{r.element(i)}}
    fun dash(){queueEvent{r.dash()}}
    fun flight(){queueEvent{r.toggleFlight()}}
    fun fastTravel(){queueEvent{r.fastTravel()}}
    fun setPaused(v:Boolean){queueEvent{r.paused=v}}
    private var pinch=0f
    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.pointerCount>=2){val d=hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1));if(pinch>0)r.zoom((pinch-d)/220f);pinch=d}else if(e.actionMasked==MotionEvent.ACTION_UP||e.actionMasked==MotionEvent.ACTION_CANCEL)pinch=0f
        return true
    }
}

private class V06Renderer(private val hud:(V06Hud)->Unit):GLSurfaceView.Renderer{
    @Volatile var mx=0f;@Volatile var my=0f;@Volatile var lx=0f;@Volatile var ly=0f;@Volatile var paused=false
    private var program=0;private var aPos=0;private var aNor=0;private var uMvp=0;private var uModel=0;private var uColor=0;private var uEye=0;private var uGlow=0;private var uFog=0
    private val proj=FloatArray(16);private val view=FloatArray(16);private val vp=FloatArray(16);private val model=FloatArray(16);private val mvp=FloatArray(16)
    private lateinit var cube:FloatBuffer;private var cubeCount=0;private lateinit var sphere:FloatBuffer;private var sphereCount=0
    private val terrain=Array<FloatBuffer?>(3){null};private val terrainCount=IntArray(3)
    private val mobs=Array(3){mutableListOf<Mob>()};private val sparks=mutableListOf<Spark>()
    private val names=arrayOf("Brumes","Cité d'Éther","Île Forêt")
    private var world=0;private var px=0f;private var py=0f;private var pz=8f;private var yaw=0f;private var pitch=.20f;private var camZoom=11f
    private var flying=false;private var flyBoost=0f;private var life=100f;private var mana=100f;private var last=0L;private var t=0f;private var hudT=0f
    private var selected=-1;private var spell="Aucun";private var dashFx=0f
    private val ruins=arrayOf(arrayOf(floatArrayOf(-34f,-22f),floatArrayOf(28f,24f),floatArrayOf(8f,-38f)),arrayOf(floatArrayOf(-30f,20f),floatArrayOf(24f,-26f),floatArrayOf(36f,10f)),arrayOf(floatArrayOf(-32f,-12f),floatArrayOf(14f,30f),floatArrayOf(38f,-24f)))
    private var ruinIndex=0

    private fun fb(a:FloatArray)=ByteBuffer.allocateDirect(a.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(a);position(0)}
    private fun cubeMesh():FloatArray{
        val o=ArrayList<Float>(216)
        fun q(nx:Float,ny:Float,nz:Float,a:FloatArray,b:FloatArray,c:FloatArray,d:FloatArray){
            val v=arrayOf(a,b,c,a,c,d);for(p in v){o+=p[0];o+=p[1];o+=p[2];o+=nx;o+=ny;o+=nz}
        }
        q(0f,0f,1f,floatArrayOf(-1f,-1f,1f),floatArrayOf(1f,-1f,1f),floatArrayOf(1f,1f,1f),floatArrayOf(-1f,1f,1f))
        q(0f,0f,-1f,floatArrayOf(1f,-1f,-1f),floatArrayOf(-1f,-1f,-1f),floatArrayOf(-1f,1f,-1f),floatArrayOf(1f,1f,-1f))
        q(1f,0f,0f,floatArrayOf(1f,-1f,1f),floatArrayOf(1f,-1f,-1f),floatArrayOf(1f,1f,-1f),floatArrayOf(1f,1f,1f))
        q(-1f,0f,0f,floatArrayOf(-1f,-1f,-1f),floatArrayOf(-1f,-1f,1f),floatArrayOf(-1f,1f,1f),floatArrayOf(-1f,1f,-1f))
        q(0f,1f,0f,floatArrayOf(-1f,1f,1f),floatArrayOf(1f,1f,1f),floatArrayOf(1f,1f,-1f),floatArrayOf(-1f,1f,-1f))
        q(0f,-1f,0f,floatArrayOf(-1f,-1f,-1f),floatArrayOf(1f,-1f,-1f),floatArrayOf(1f,-1f,1f),floatArrayOf(-1f,-1f,1f))
        return o.toFloatArray()
    }
    private fun sphereMesh(seg:Int=12,rings:Int=8):FloatArray{
        val o=ArrayList<Float>()
        fun p(th:Float,ph:Float):FloatArray{val y=sin(ph);val rr=cos(ph);return floatArrayOf(rr*cos(th),y,rr*sin(th))}
        for(j in 0 until rings){val p0=-PI.toFloat()/2+PI.toFloat()*j/rings;val p1=-PI.toFloat()/2+PI.toFloat()*(j+1)/rings
            for(i in 0 until seg){val t0=2*PI.toFloat()*i/seg;val t1=2*PI.toFloat()*(i+1)/seg;val a=p(t0,p0);val b=p(t1,p0);val c=p(t1,p1);val d=p(t0,p1);for(v in arrayOf(a,b,c,a,c,d)){o+=v[0];o+=v[1];o+=v[2];o+=v[0];o+=v[1];o+=v[2]}}
        };return o.toFloatArray()
    }
    private fun ground(x:Float,z:Float,w:Int=world):Float{
        val k=w*1.7f;return sin(x*.075f+k)*1.15f+cos(z*.065f-k)*.8f+sin((x+z)*.035f)*.55f
    }
    private fun terrainMesh(w:Int):FloatArray{
        val o=ArrayList<Float>();val n=34;val step=4f
        fun v(x:Float,z:Float):FloatArray{val y=ground(x,z,w);val eps=.4f;val dx=ground(x+eps,z,w)-ground(x-eps,z,w);val dz=ground(x,z+eps,w)-ground(x,z-eps,w);var nx=-dx;var ny=eps*2;var nz=-dz;val l=sqrt(nx*nx+ny*ny+nz*nz);nx/=l;ny/=l;nz/=l;return floatArrayOf(x,y,z,nx,ny,nz)}
        for(iz in 0 until n){for(ix in 0 until n){val x0=(ix-n/2)*step;val z0=(iz-n/2)*step;val x1=x0+step;val z1=z0+step;val a=v(x0,z0);val b=v(x1,z0);val c=v(x1,z1);val d=v(x0,z1);for(q in arrayOf(a,b,c,a,c,d))for(f in q)o+=f}}
        return o.toFloatArray()
    }
    override fun onSurfaceCreated(gl:javax.microedition.khronos.opengles.GL10?,cfg:javax.microedition.khronos.egl.EGLConfig?){
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glEnable(GLES20.GL_BLEND);GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);GLES20.glEnable(GLES20.GL_CULL_FACE)
        val vs="""attribute vec3 a;attribute vec3 n;uniform mat4 m;uniform mat4 model;varying vec3 N;varying vec3 W;void main(){vec4 w=model*vec4(a,1.0);W=w.xyz;N=normalize(mat3(model)*n);gl_Position=m*vec4(a,1.0);}"""
        val fs="""precision mediump float;uniform vec4 color;uniform vec3 eye;uniform float glow;uniform vec3 fogColor;varying vec3 N;varying vec3 W;void main(){vec3 L=normalize(vec3(-0.45,0.85,0.25));float d=max(dot(normalize(N),L),0.0);float rim=pow(1.0-max(dot(normalize(N),normalize(eye-W)),0.0),2.0);vec3 c=color.rgb*(0.38+d*0.72)+rim*0.10+color.rgb*glow*0.65;float fog=clamp((length(W-eye)-34.0)/80.0,0.0,0.82);gl_FragColor=vec4(mix(c,fogColor,fog),color.a);}"""
        fun sh(type:Int,s:String):Int{val x=GLES20.glCreateShader(type);GLES20.glShaderSource(x,s);GLES20.glCompileShader(x);return x}
        program=GLES20.glCreateProgram();GLES20.glAttachShader(program,sh(GLES20.GL_VERTEX_SHADER,vs));GLES20.glAttachShader(program,sh(GLES20.GL_FRAGMENT_SHADER,fs));GLES20.glLinkProgram(program);GLES20.glUseProgram(program)
        aPos=GLES20.glGetAttribLocation(program,"a");aNor=GLES20.glGetAttribLocation(program,"n");uMvp=GLES20.glGetUniformLocation(program,"m");uModel=GLES20.glGetUniformLocation(program,"model");uColor=GLES20.glGetUniformLocation(program,"color");uEye=GLES20.glGetUniformLocation(program,"eye");uGlow=GLES20.glGetUniformLocation(program,"glow");uFog=GLES20.glGetUniformLocation(program,"fogColor")
        val c=cubeMesh();cube=fb(c);cubeCount=c.size/6;val s=sphereMesh();sphere=fb(s);sphereCount=s.size/6
        for(w in 0..2){val tm=terrainMesh(w);terrain[w]=fb(tm);terrainCount[w]=tm.size/6;repeat(8){i->mobs[w]+=Mob(-26f+i*7.2f,(-18f+(i*11)%39),false,i*.73f)};repeat(5){i->mobs[w]+=Mob(-22f+i*10f,22f-(i*7)%30,true,i*.41f)}}
    }
    override fun onSurfaceChanged(gl:javax.microedition.khronos.opengles.GL10?,w:Int,h:Int){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(proj,0,58f,w.toFloat()/max(1,h),.1f,180f)}
    fun zoom(d:Float){camZoom=(camZoom+d).coerceIn(6.5f,17f)}
    fun toggleFlight(){flying=!flying;flyBoost=if(flying)10f else 0f;burst(px,py+1f,pz,.08f,.08f,.10f,18)}
    fun dash(){
        val fx=sin(yaw);val fz=-cos(yaw);val rx=cos(yaw);val rz=sin(yaw);var dx=rx*mx+fx*(-my);var dz=rz*mx+fz*(-my);if(abs(dx)+abs(dz)<.15f){dx=fx;dz=fz};val l=max(.01f,hypot(dx,dz));dx/=l;dz/=l
        px=(px+dx*8.5f).coerceIn(-61f,61f);pz=(pz+dz*8.5f).coerceIn(-61f,61f);dashFx=.42f;burst(px,py+1f,pz,.03f,.03f,.04f,28)
    }
    fun fastTravel(){ruinIndex=(ruinIndex+1)%3;px=ruins[world][ruinIndex][0];pz=ruins[world][ruinIndex][1];if(!flying)py=ground(px,pz);burst(px,py+1f,pz,.15f,.48f,.9f,30)}
    fun element(i:Int){
        if(selected<0){selected=i;spell=arrayOf("Feu prêt","Eau prête","Air prêt","Terre prête")[i];return}
        val a=min(selected,i);val b=max(selected,i);spell=when(a*10+b){1->"Vapeur";2->"Tempête de braises";3->"Lave";12->"Givre";13->"Boue entravante";23->"Tempête de sable";else->arrayOf("Feu","Eau","Air","Terre")[i]};cast(i);selected=-1
    }
    private fun cast(i:Int){if(mana<8)return;mana-=8;val fx=sin(yaw);val fz=-cos(yaw);val col=arrayOf(floatArrayOf(1f,.18f,.03f),floatArrayOf(.08f,.55f,1f),floatArrayOf(.75f,.85f,1f),floatArrayOf(.65f,.45f,.22f))[i];repeat(18){k->sparks+=Spark(px,py+1.4f,pz,fx*(6f+k*.08f)+(k%3-1)*.12f,(k%5-2)*.07f,fz*(6f+k*.08f)+(k%4-2)*.1f,1.4f,col[0],col[1],col[2],.18f+(k%3)*.05f)}}
    private fun burst(x:Float,y:Float,z:Float,r:Float,g:Float,b:Float,n:Int){repeat(n){k->val a=k*6.283f/n;val s=1.2f+(k%5)*.45f;sparks+=Spark(x,y,z,cos(a)*s,(k%4)*.35f,sin(a)*s,.7f,r,g,b,.12f+(k%3)*.04f)}}
    private fun update(dt:Float){
        t+=dt;mana=min(100f,mana+dt*4f);yaw+=lx*dt*1.8f;pitch=(pitch-ly*dt*.75f).coerceIn(-.12f,.55f)
        val fx=sin(yaw);val fz=-cos(yaw);val rx=cos(yaw);val rz=sin(yaw);val dx=rx*mx+fx*(-my);val dz=rz*mx+fz*(-my);val speed=if(flying)8.5f else 5.2f
        px=(px+dx*speed*dt).coerceIn(-62f,62f);pz=(pz+dz*speed*dt).coerceIn(-62f,62f)
        if(flying){py=(py+(-ly*6.2f+flyBoost)*dt).coerceIn(ground(px,pz)+1.2f,30f);flyBoost=max(0f,flyBoost-dt*24f);if(abs(dx)+abs(dz)>.2f&&((t*12).toInt()%2==0))sparks+=Spark(px-fx*.8f,py+.7f,pz-fz*.8f,-fx*.8f,.15f,-fz*.8f,.45f,.02f,.025f,.035f,.24f)}else py=ground(px,pz)
        if(dashFx>0)dashFx-=dt
        for(m in mobs[world]){m.phase+=dt*(if(m.friendly).6f else .9f);m.cooldown=max(0f,m.cooldown-dt);val d=hypot(m.x-px,m.z-pz)
            if(!m.friendly&&d<18f&&d>2.4f){m.x+=(px-m.x)/d*dt*1.35f;m.z+=(pz-m.z)/d*dt*1.35f}else if(d>3f){m.x+=sin(m.phase)*dt*.28f;m.z+=cos(m.phase*.83f)*dt*.28f}
            if(!m.friendly&&d<2.6f&&m.cooldown<=0){life=max(0f,life-7f);m.cooldown=1.15f;burst(px,py+1f,pz,.65f,.05f,.04f,10)}
        }
        val it=sparks.iterator();while(it.hasNext()){val s=it.next();s.x+=s.vx*dt;s.y+=s.vy*dt;s.z+=s.vz*dt;s.vy-=dt*.35f;s.life-=dt;for(m in mobs[world])if(!m.friendly&&m.hp>0&&hypot(s.x-m.x,s.z-m.z)<1.15f){m.hp-=22f;s.life=0f};if(s.life<=0)it.remove()}
        mobs[world].removeAll{it.hp<=0}
        val portals=if(world==0)arrayOf(floatArrayOf(-49f,5f,1f),floatArrayOf(49f,-4f,2f)) else arrayOf(floatArrayOf(0f,-51f,0f));for(p in portals)if(hypot(px-p[0],pz-p[1])<2.1f){world=p[2].toInt();px=0f;pz=if(world==0)42f else 0f;py=ground(px,pz);burst(px,py+1f,pz,.18f,.5f,1f,32);break}
        hudT+=dt;if(hudT>.16f){hudT=0f;hud(V06Hud(life.toInt(),mana.toInt(),names[world],flying,mobs[world].count{!it.friendly},spell))}
    }
    override fun onDrawFrame(gl:javax.microedition.khronos.opengles.GL10?){
        val now=System.nanoTime();val dt=if(last==0L)0f else min(.033f,(now-last)/1_000_000_000f);last=now;if(!paused)update(dt)
        val day=(sin(t*.035f)+1f)*.5f;val skyR=.045f+day*.13f;val skyG=.075f+day*.25f;val skyB=.13f+day*.38f;GLES20.glClearColor(skyR,skyG,skyB,1f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val fx=sin(yaw);val fz=-cos(yaw);val cp=cos(pitch);val eyeX=px-fx*camZoom*cp;val eyeY=py+3.1f+sin(pitch)*camZoom;val eyeZ=pz-fz*camZoom*cp
        Matrix.setLookAtM(view,0,eyeX,eyeY,eyeZ,px,py+1.5f,pz,0f,1f,0f);Matrix.multiplyMM(vp,0,proj,0,view,0);GLES20.glUseProgram(program);GLES20.glUniform3f(uEye,eyeX,eyeY,eyeZ);GLES20.glUniform3f(uFog,skyR,skyG,skyB)
        drawTerrain();drawWorld();drawPlayer();for(m in mobs[world])drawMob(m);for(s in sparks)drawSphere(s.x,s.y,s.z,s.size,s.size,s.size,s.r,s.g,s.b,1f,.85f)
    }
    private fun bind(buf:FloatBuffer,count:Int){buf.position(0);GLES20.glEnableVertexAttribArray(aPos);GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,24,buf);buf.position(3);GLES20.glEnableVertexAttribArray(aNor);GLES20.glVertexAttribPointer(aNor,3,GLES20.GL_FLOAT,false,24,buf)}
    private fun drawTerrain(){Matrix.setIdentityM(model,0);Matrix.multiplyMM(mvp,0,vp,0,model,0);GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(uModel,1,false,model,0);val c=when(world){0->floatArrayOf(.11f,.25f,.16f,1f);1->floatArrayOf(.17f,.18f,.23f,1f);else->floatArrayOf(.10f,.28f,.18f,1f)};GLES20.glUniform4fv(uColor,1,c,0);GLES20.glUniform1f(uGlow,0f);bind(terrain[world]!!,terrainCount[world]);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,terrainCount[world])}
    private fun drawCube(x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,r:Float,g:Float,b:Float,a:Float=1f,glow:Float=0f,rot:Float=0f){Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);if(rot!=0f)Matrix.rotateM(model,0,rot,0f,1f,0f);Matrix.scaleM(model,0,sx,sy,sz);Matrix.multiplyMM(mvp,0,vp,0,model,0);GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(uModel,1,false,model,0);GLES20.glUniform4f(uColor,r,g,b,a);GLES20.glUniform1f(uGlow,glow);bind(cube,cubeCount);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,cubeCount)}
    private fun drawSphere(x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,r:Float,g:Float,b:Float,a:Float=1f,glow:Float=0f){Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,sx,sy,sz);Matrix.multiplyMM(mvp,0,vp,0,model,0);GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(uModel,1,false,model,0);GLES20.glUniform4f(uColor,r,g,b,a);GLES20.glUniform1f(uGlow,glow);bind(sphere,sphereCount);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,sphereCount)}
    private fun drawWorld(){
        for(i in 0 until 24){val x=((i*37)%101-50).toFloat();val z=((i*61+17)%103-51).toFloat();if(hypot(x,z)<9)continue;val y=ground(x,z);if(world==2||i%3!=0){drawCube(x,y+1.4f,z,.28f,1.4f,.28f,.22f,.12f,.07f);drawSphere(x,y+3.2f,z,1.35f,1.65f,1.35f,if(world==1).22f else .08f,if(world==1).28f else .38f,if(world==1).32f else .14f)}else{drawCube(x,y+.8f,z,.9f,.8f,.9f,.24f,.25f,.28f,1f,0f,i*17f)}}
        for(i in 0 until 10){val x=-42f+i*9.2f;val z=34f+sin(i*1.7f)*5f;val y=ground(x,z);drawCube(x,y+1.8f,z,.65f,1.8f,.65f,.34f,.32f,.30f);if(i%2==0)drawCube(x+2.5f,y+3.7f,z,.25f,3.4f,.25f,.30f,.28f,.27f)}
        for(rp in ruins[world])drawRuine(rp[0],rp[1])
        if(world==0){drawPortal(-49f,5f,.15f,.55f,1f);drawPortal(49f,-4f,.75f,.22f,1f)} else drawPortal(0f,-51f,.2f,.65f,1f)
    }
    private fun drawRuine(x:Float,z:Float){val y=ground(x,z);for(k in 0 until 12){val a=k*2*PI.toFloat()/12;drawCube(x+cos(a)*3f,y+.18f,z+sin(a)*3f,.42f,.12f,.42f,.15f,.55f,.9f,.85f,.8f,k*30f)};for(k in 0..3){val a=k*PI.toFloat()/2;drawCube(x+cos(a)*3.8f,y+1.5f,z+sin(a)*3.8f,.35f,1.5f,.35f,.28f,.31f,.34f)}}
    private fun drawPortal(x:Float,z:Float,r:Float,g:Float,b:Float){val y=ground(x,z);for(k in 0 until 18){val a=k*2*PI.toFloat()/18;drawCube(x+cos(a)*2.2f,y+2.6f+sin(a)*2.6f,z,.18f,.38f,.34f,r,g,b,.88f,1f,k*20f)};drawCube(x,y+2.6f,z,.06f,2.15f,1.5f,r,g,b,.18f,1f)}
    private fun drawPlayer(){val bob=if(flying).1f else sin(t*8f)*.06f;drawCube(px,py+1.05f+bob,pz,.38f,.68f,.26f,.17f,.19f,.23f);drawSphere(px,py+2.02f+bob,pz,.34f,.38f,.34f,.72f,.58f,.46f);val swing=sin(t*8f)*.22f;drawCube(px-.48f,py+1.1f+bob,pz+swing,.12f,.55f,.12f,.15f,.17f,.20f);drawCube(px+.48f,py+1.1f+bob,pz-swing,.12f,.55f,.12f,.15f,.17f,.20f);drawCube(px-.20f,py+.28f+bob,pz-swing,.15f,.55f,.16f,.10f,.11f,.13f);drawCube(px+.20f,py+.28f+bob,pz+swing,.15f,.55f,.16f,.10f,.11f,.13f)}
    private fun drawMob(m:Mob){val y=ground(m.x,m.z);val base=if(m.friendly)floatArrayOf(.12f,.38f,.66f) else floatArrayOf(.55f,.07f,.06f);val bob=sin(m.phase*4f)*.05f;drawCube(m.x,y+1f+bob,m.z,.34f,.63f,.25f,base[0],base[1],base[2]);drawSphere(m.x,y+1.92f+bob,m.z,.31f,.34f,.31f,.68f,.53f,.42f);val s=sin(m.phase*5f)*.18f;drawCube(m.x-.43f,y+1.05f,m.z+s,.10f,.48f,.10f,base[0]*.8f,base[1]*.8f,base[2]*.8f);drawCube(m.x+.43f,y+1.05f,m.z-s,.10f,.48f,.10f,base[0]*.8f,base[1]*.8f,base[2]*.8f)}
}
