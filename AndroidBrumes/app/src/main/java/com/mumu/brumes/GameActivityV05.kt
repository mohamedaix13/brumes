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

private data class HudState(
    val life:Int=100, val mana:Int=100, val world:Int=0, val worldName:String="Brumes",
    val enemies:Int=0, val combo:String="Aucun", val flying:Boolean=false, val ruins:Int=0
)

class GameActivityV05 : Activity() {
    private lateinit var game: BrumesV05View
    private lateinit var root: FrameLayout
    private var menu: View? = null
    private val gold = Color.rgb(218,181,108)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun panel(alpha:Int=235)=android.graphics.drawable.GradientDrawable().apply {
        setColor(Color.argb(alpha,14,18,26));cornerRadius=dp(12).toFloat();setStroke(dp(1),Color.rgb(104,90,67))
    }
    private fun label(t:String,size:Float=14f)=TextView(this).apply {
        text=t;textSize=size;setTextColor(Color.rgb(240,235,224));setPadding(dp(10),dp(6),dp(10),dp(6))
    }
    private fun button(t:String,onClick:()->Unit)=Button(this).apply {
        text=t;textSize=11f;isAllCaps=false;setTextColor(gold);backgroundTintList=null;background=panel(215);setOnClickListener{onClick()}
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        root=FrameLayout(this);root.isMotionEventSplittingEnabled=true
        game=BrumesV05View(this);root.addView(game)
        val hud=label("BRUMES 0.5",12f).apply{background=panel()}
        root.addView(hud,FrameLayout.LayoutParams(dp(285),dp(82),Gravity.TOP or Gravity.START).apply{setMargins(dp(10),dp(8),0,0)})
        root.addView(button("☰ MENU"){openMenu()},FrameLayout.LayoutParams(dp(96),dp(44),Gravity.TOP or Gravity.END).apply{setMargins(0,dp(8),dp(10),0)})

        val left=StickView(this){x,y->game.move(x,y)}
        root.addView(left,FrameLayout.LayoutParams(dp(144),dp(144),Gravity.BOTTOM or Gravity.START).apply{setMargins(dp(12),0,0,dp(10))})
        val right=StickView(this){x,y->game.lookStick(x,y)}
        root.addView(right,FrameLayout.LayoutParams(dp(132),dp(132),Gravity.BOTTOM or Gravity.END).apply{setMargins(0,0,dp(12),dp(92))})

        val spells=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        listOf("🔥 Feu","💧 Eau","🌪 Air","🪨 Terre").forEachIndexed{i,n->spells.addView(button(n){game.element(i)},LinearLayout.LayoutParams(dp(82),dp(52)).apply{setMargins(dp(3),0,dp(3),0)})}
        root.addView(spells,FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,dp(58),Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply{setMargins(0,0,0,dp(8))})

        val actions=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.END}
        actions.addView(button("✦ ESQUIVE"){game.dash()},LinearLayout.LayoutParams(dp(110),dp(48)))
        actions.addView(button("☁ TRANSPLANAGE"){game.toggleFlight()},LinearLayout.LayoutParams(dp(110),dp(48)).apply{setMargins(0,dp(4),0,0)})
        actions.addView(button("◉ RUINE"){game.fastTravel()},LinearLayout.LayoutParams(dp(110),dp(44)).apply{setMargins(0,dp(4),0,0)})
        root.addView(actions,FrameLayout.LayoutParams(dp(116),FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER_VERTICAL or Gravity.END).apply{setMargins(0,0,dp(10),0)})

        game.onHud={s->hud.text="BRUMES 0.5 · ${s.worldName}\nVie ${s.life}   Mana ${s.mana}   ${if(s.flying)"VOL" else "SOL"}\n${s.enemies} ennemis · ${s.ruins}/3 ruines · ${s.combo}"}
        setContentView(root)
        openMenu()
    }
    override fun onPause(){game.onPause();super.onPause()}
    override fun onResume(){super.onResume();game.onResume()}
    @Deprecated("Deprecated in Java") override fun onBackPressed(){if(menu==null)openMenu() else closeMenu()}
    private fun closeMenu(){menu?.let{root.removeView(it)};menu=null;game.pause(false)}
    private fun openMenu(){
        if(menu!=null)return
        game.pause(true)
        val shade=FrameLayout(this).apply{setBackgroundColor(0xDD070B10.toInt());isClickable=true}
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(16),dp(18),dp(16));background=panel()}
        body.addView(label("B R U M E S   0.5",26f).apply{setTextColor(gold);gravity=Gravity.CENTER})
        body.addView(label("Trois mondes · ennemis et PNJ · magie combinée · ruines de voyage · transplanage",13f).apply{gravity=Gravity.CENTER})
        body.addView(button("JOUER / REPRENDRE"){closeMenu()},LinearLayout.LayoutParams(-1,dp(48)))
        body.addView(label("Contrôles\nJoystick gauche : déplacement. Joystick droit : caméra; en transplanage il pilote aussi la hauteur.\nESQUIVE : téléportation courte avec traînée noire; si la limite est touchée, la trajectoire ricoche vers une zone sûre.\nTRANSPLANAGE : impulsion immédiate puis vol libre. Pince l'écran pour zoomer.\nMagie : choisis deux éléments. Exemple Eau + Air = Givre.",12f))
        body.addView(label("Combinaisons\nEau + Air = Givre · Feu + Terre = Lave · Feu + Air = Tempête de braises · Eau + Terre = Entrave de boue · Feu + Eau = Vapeur · Air + Terre = Tempête de sable",12f))
        body.addView(label("Portails\nLe monde principal mène vers la Cité d'Éther et l'Île Forêt. Les deux cartes importées sont reconstruites à partir des positions, tailles et couleurs de tes fichiers RBXL pour cette version test.",12f))
        shade.addView(body,FrameLayout.LayoutParams(dp(620),FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER));root.addView(shade,FrameLayout.LayoutParams(-1,-1));menu=shade
    }
}

private class StickView(activity:Activity,val output:(Float,Float)->Unit):View(activity){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG);private var dx=0f;private var dy=0f
    override fun onDraw(c:Canvas){val r=width*.42f;p.color=0x55434D54;c.drawCircle(width/2f,height/2f,r,p);p.style=Paint.Style.STROKE;p.strokeWidth=3f;p.color=0xCCDAB56C.toInt();c.drawCircle(width/2f,height/2f,r,p);p.style=Paint.Style.FILL;p.color=0xDDDAB56C.toInt();c.drawCircle(width/2f+dx*r,height/2f+dy*r,r*.28f,p)}
    override fun onTouchEvent(e:MotionEvent):Boolean{when(e.actionMasked){MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE->{dx=(e.x-width/2)/(width*.42f);dy=(e.y-height/2)/(height*.42f);val l=max(1f,hypot(dx,dy));dx/=l;dy/=l};else->{dx=0f;dy=0f}};output(dx,dy);invalidate();return true}
}

private data class WorldObj(val x:Float,val y:Float,val z:Float,val sx:Float,val sy:Float,val sz:Float,val r:Float,val g:Float,val b:Float)
private data class Actor(var x:Float,var z:Float,var hp:Float=100f,var cooldown:Float=0f,var wander:Float=0f,var state:Int=0)

private class BrumesV05View(activity:Activity):GLSurfaceView(activity){
    internal var onHud:((HudState)->Unit)?=null
    private val renderer=V05Renderer(activity.assets){s->post{onHud?.invoke(s)}}
    init{setEGLContextClientVersion(2);setRenderer(renderer);renderMode=RENDERMODE_CONTINUOUSLY}
    fun move(x:Float,y:Float){renderer.moveX=x;renderer.moveZ=y}
    fun lookStick(x:Float,y:Float){renderer.lookX=x;renderer.lookY=y}
    fun element(i:Int){queueEvent{renderer.element(i)}}
    fun dash(){queueEvent{renderer.dash()}}
    fun toggleFlight(){queueEvent{renderer.toggleFlight()}}
    fun fastTravel(){queueEvent{renderer.fastTravel()}}
    fun pause(v:Boolean){queueEvent{renderer.paused=v}}
    private var lastDist=0f
    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.pointerCount>=2){val d=hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1));if(lastDist>0)renderer.zoom((lastDist-d)/180f);lastDist=d}else if(e.actionMasked==MotionEvent.ACTION_UP||e.actionMasked==MotionEvent.ACTION_CANCEL)lastDist=0f
        return true
    }
}

private class V05Renderer(private val assets:android.content.res.AssetManager,private val hud:(HudState)->Unit):GLSurfaceView.Renderer{
    @Volatile var moveX=0f;@Volatile var moveZ=0f;@Volatile var lookX=0f;@Volatile var lookY=0f;@Volatile var paused=true
    private var program=0;private var aPos=0;private var aNormal=0;private var uMvp=0;private var uModel=0;private var uScale=0;private var uColor=0;private var uEye=0;private var uGlow=0
    private val proj=FloatArray(16);private val view=FloatArray(16);private val vp=FloatArray(16);private val model=FloatArray(16);private val mvp=FloatArray(16)
    private val cube=floatArrayOf(-1f,-1f,1f,1f,-1f,1f,1f,1f,1f,-1f,-1f,1f,1f,1f,1f,-1f,1f,1f,1f,-1f,-1f,-1f,-1f,-1f,-1f,1f,-1f,1f,-1f,-1f,-1f,1f,-1f,1f,1f,-1f,-1f,-1f,-1f,-1f,-1f,1f,-1f,1f,1f,-1f,-1f,-1f,-1f,1f,1f,-1f,-1f,1f,-1f,-1f,1f,1f,1f,1f,-1f,1f,-1f,1f,1f,1f,1f,-1f,1f,1f,1f,-1f,-1f,1f,1f,1f,1f,1f,1f,1f,-1f,-1f,1f,1f,1f,1f,-1f,1f,-1f,-1f,-1f,-1f,1f,-1f,-1f,1f,-1f,1f,-1f,-1f,-1f,1f,-1f,1f,-1f,-1f,1f)
    private val cb=buffer(cube);private val cn=normals(cube)
    private val worlds=Array(3){mutableListOf<WorldObj>()};private val enemies=Array(3){mutableListOf<Actor>()};private val npcs=Array(3){mutableListOf<Actor>()}
    private var world=0;private var px=0f;private var pz=0f;private var py=0f;private var yaw=0f;private var pitch=.2f;private var zoom=11f;private var flying=false;private var flySpeed=0f
    private var life=100f;private var mana=100f;private var last=0L;private var time=0f;private var hudAt=0f;private var selected=-1;private var combo="Aucun";private var comboFx=0f
    private var dashFx=0f;private var dashFromX=0f;private var dashFromZ=0f;private var dashToX=0f;private var dashToZ=0f
    private val ruinMask=IntArray(3);private var ruinCursor=0
    private val worldNames=arrayOf("Brumes","Cité d'Éther","Île Forêt")
    private val ruinPos=arrayOf(arrayOf(floatArrayOf(-45f,15f),floatArrayOf(0f,48f),floatArrayOf(48f,-18f)),arrayOf(floatArrayOf(-28f,-15f),floatArrayOf(8f,20f),floatArrayOf(35f,-25f)),arrayOf(floatArrayOf(-38f,10f),floatArrayOf(5f,32f),floatArrayOf(42f,-18f)))
    private fun buffer(a:FloatArray)=ByteBuffer.allocateDirect(a.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(a);position(0)}
    private fun normals(v:FloatArray):FloatBuffer{val n=FloatArray(v.size);for(i in v.indices step 9){val ax=v[i+3]-v[i];val ay=v[i+4]-v[i+1];val az=v[i+5]-v[i+2];val bx=v[i+6]-v[i];val by=v[i+7]-v[i+1];val bz=v[i+8]-v[i+2];val nx=ay*bz-az*by;val ny=az*bx-ax*bz;val nz=ax*by-ay*bx;val l=max(.001f,sqrt(nx*nx+ny*ny+nz*nz));for(j in 0..2){n[i+j*3]=nx/l;n[i+j*3+1]=ny/l;n[i+j*3+2]=nz/l}};return buffer(n)}
    private fun loadWorld(name:String):MutableList<WorldObj>{val out=mutableListOf<WorldObj>();val text=assets.open(name).bufferedReader().use{it.readText()};val arr=JSONObject(text).getJSONArray("objects");for(i in 0 until arr.length()){val a=arr.getJSONArray(i);out+=WorldObj(a.getDouble(0).toFloat(),a.getDouble(1).toFloat(),a.getDouble(2).toFloat(),a.getDouble(3).toFloat()/2,a.getDouble(4).toFloat()/2,a.getDouble(5).toFloat()/2,a.getInt(6)/255f,a.getInt(7)/255f,a.getInt(8)/255f)};return out}
    override fun onSurfaceCreated(gl:javax.microedition.khronos.opengles.GL10?,cfg:javax.microedition.khronos.egl.EGLConfig?){
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glEnable(GLES20.GL_BLEND);GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA)
        val vs="""attribute vec3 a;attribute vec3 n;uniform mat4 m;uniform mat4 model;uniform vec3 scale;varying vec3 wn;varying vec3 wp;void main(){wp=(model*vec4(a,1.0)).xyz;wn=normalize(n/scale);gl_Position=m*vec4(a,1.0);}"""
        val fs="""precision mediump float;uniform vec4 c;uniform vec3 eye;uniform float glow;varying vec3 wn;varying vec3 wp;void main(){float d=max(dot(normalize(wn),normalize(vec3(-.45,.8,.35))),0.0);vec3 col=c.rgb*(.42+d*.72);col=mix(col,c.rgb*1.5,glow);float fog=1.0-exp(-pow(length(wp-eye)*.009,2.0));gl_FragColor=vec4(mix(col,vec3(.18,.23,.30),clamp(fog,0.0,.93)),c.a);}"""
        fun sh(type:Int,s:String)=GLES20.glCreateShader(type).also{GLES20.glShaderSource(it,s);GLES20.glCompileShader(it)}
        program=GLES20.glCreateProgram().also{GLES20.glAttachShader(it,sh(GLES20.GL_VERTEX_SHADER,vs));GLES20.glAttachShader(it,sh(GLES20.GL_FRAGMENT_SHADER,fs));GLES20.glLinkProgram(it)}
        aPos=GLES20.glGetAttribLocation(program,"a");aNormal=GLES20.glGetAttribLocation(program,"n");uMvp=GLES20.glGetUniformLocation(program,"m");uModel=GLES20.glGetUniformLocation(program,"model");uScale=GLES20.glGetUniformLocation(program,"scale");uColor=GLES20.glGetUniformLocation(program,"c");uEye=GLES20.glGetUniformLocation(program,"eye");uGlow=GLES20.glGetUniformLocation(program,"glow")
        worlds[1]=loadWorld("world_aether.json");worlds[2]=loadWorld("world_forest.json")
        for(w in 0..2){repeat(if(w==0)18 else 24){i->val a=i*2.399f;val r=15+(i%6)*6f;enemies[w]+=Actor(sin(a)*r,cos(a)*r,100f,wander=i*.4f,state=i%3)};repeat(7){i->val a=i*.91f;npcs[w]+=Actor(sin(a)*22,cos(a)*22,100f,wander=i*.7f)}}
    }
    override fun onSurfaceChanged(gl:javax.microedition.khronos.opengles.GL10?,w:Int,h:Int){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(proj,0,62f,w.toFloat()/max(1,h),.1f,260f)}
    fun zoom(v:Float){zoom=(zoom+v).coerceIn(5f,20f)}
    private fun groundY(x:Float,z:Float)=when(world){0->sin(x*.06f)*cos(z*.055f)*1.9f+sin((x+z)*.11f)*.45f;1->-1.2f;else->sin(x*.045f)*cos(z*.05f)*1.1f}
    private fun bound():Float=if(world==2)78f else 70f
    private fun safe(nx:Float,nz:Float)=abs(nx)<bound()&&abs(nz)<bound()
    fun dash(){if(paused)return;val dirX=if(abs(moveX)+abs(moveZ)>.1f)moveX*cos(yaw)+moveZ*sin(yaw) else -sin(yaw);val dirZ=if(abs(moveX)+abs(moveZ)>.1f)-moveX*sin(yaw)+moveZ*cos(yaw) else -cos(yaw);var tx=px+dirX*9f;var tz=pz+dirZ*9f;if(!safe(tx,tz)){tx=(px-dirZ*7f).coerceIn(-bound()+2,bound()-2);tz=(pz+dirX*7f).coerceIn(-bound()+2,bound()-2)};dashFromX=px;dashFromZ=pz;dashToX=tx;dashToZ=tz;px=tx;pz=tz;dashFx=1f}
    fun toggleFlight(){if(paused)return;flying=!flying;if(flying){flySpeed=18f;py=max(py,groundY(px,pz)+2.2f);dashFx=1f}else py=groundY(px,pz)}
    fun fastTravel(){val active=(0..2).filter{ruinMask[world] and (1 shl it)!=0};if(active.isEmpty())return;ruinCursor=(ruinCursor+1)%active.size;val p=ruinPos[world][active[ruinCursor]];dashFromX=px;dashFromZ=pz;px=p[0];pz=p[1];dashToX=px;dashToZ=pz;dashFx=1f}
    fun element(i:Int){if(paused)return;if(selected<0){selected=i;combo=arrayOf("Feu","Eau","Air","Terre")[i]}else{castCombo(selected,i);selected=-1}}
    private fun castCombo(a:Int,b:Int){val lo=min(a,b);val hi=max(a,b);combo=when(lo*10+hi){1->"Vapeur";2->"Tempête de braises";3->"Lave";12->"Givre";13->"Boue entravante";23->"Tempête de sable";else->arrayOf("Feu","Eau","Air","Terre")[a]};val range=if(combo=="Givre")20f else 15f;enemies[world].forEach{e->val d=hypot(e.x-px,e.z-pz);if(d<range&&e.hp>0){val dmg=when(combo){"Lave"->55f;"Givre"->38f;"Tempête de braises"->48f;else->34f};e.hp-=dmg;if(combo=="Givre")e.cooldown=2.2f;if(combo=="Boue entravante")e.cooldown=1.4f}};comboFx=1f;mana=max(0f,mana-12f)}
    private fun switchWorld(w:Int){world=w;px=0f;pz=0f;py=groundY(0f,0f);flying=false;dashFx=1f}
    override fun onDrawFrame(gl:javax.microedition.khronos.opengles.GL10?){
        val now=System.nanoTime();val dt=if(last==0L)0f else ((now-last)/1e9f).coerceIn(0f,.05f);last=now;if(!paused)update(dt);render()
    }
    private fun update(dt:Float){time+=dt;mana=min(100f,mana+dt*7f);dashFx=max(0f,dashFx-dt*2.2f);comboFx=max(0f,comboFx-dt*1.7f);yaw=(yaw-lookX*dt*2.4f)%(PI.toFloat()*2);pitch=(pitch-lookY*dt*.9f).coerceIn(-.25f,.75f)
        val wx=moveX*cos(yaw)+moveZ*sin(yaw);val wz=-moveX*sin(yaw)+moveZ*cos(yaw)
        if(flying){flySpeed=max(10f,flySpeed-dt*6f);px=(px+wx*dt*flySpeed).coerceIn(-bound(),bound());pz=(pz+wz*dt*flySpeed).coerceIn(-bound(),bound());py=(py-lookY*dt*8f).coerceIn(groundY(px,pz)+1.5f,28f)}else{px=(px+wx*dt*8f).coerceIn(-bound(),bound());pz=(pz+wz*dt*8f).coerceIn(-bound(),bound());py=groundY(px,pz)}
        enemies[world].forEachIndexed{i,e->if(e.hp>0){e.cooldown=max(0f,e.cooldown-dt);val dx=px-e.x;val dz=pz-e.z;val d=max(.1f,hypot(dx,dz));if(d<26f&&e.cooldown<=0){val speed=if(e.state==2)2.2f else 1.35f;e.x+=dx/d*dt*speed;e.z+=dz/d*dt*speed}else{e.wander+=dt*(.4f+i*.003f);e.x+=sin(e.wander+i)*dt*.35f;e.z+=cos(e.wander*.8f+i)*dt*.35f};if(d<1.8f&&e.cooldown<=0){life=max(0f,life-if(e.state==2)11f else 6f);e.cooldown=1.2f}}}
        npcs[world].forEachIndexed{i,n->n.wander+=dt*.35f;n.x=(n.x+sin(n.wander+i)*dt*.45f).coerceIn(-bound()+3,bound()-3);n.z=(n.z+cos(n.wander*.7f+i)*dt*.45f).coerceIn(-bound()+3,bound()-3)}
        if(life<=0){life=100f;mana=100f;px=0f;pz=0f;py=groundY(0f,0f);flying=false}
        ruinPos[world].forEachIndexed{i,p->if(hypot(px-p[0],pz-p[1])<3.5f)ruinMask[world]=ruinMask[world] or (1 shl i)}
        if(world==0){if(hypot(px+32f,pz+34f)<3.4f)switchWorld(1);else if(hypot(px-32f,pz+34f)<3.4f)switchWorld(2)}else if(hypot(px,pz+42f)<3.4f)switchWorld(0)
        if(time>hudAt){hudAt=time+.25f;hud(HudState(life.toInt(),mana.toInt(),world,worldNames[world],enemies[world].count{it.hp>0},combo,flying,Integer.bitCount(ruinMask[world])))}
    }
    private fun render(){val phase=(time%120f)/120f;val sky=when{phase<.48f->floatArrayOf(.34f,.48f,.65f);phase<.62f->floatArrayOf(.63f,.34f,.27f);phase<.9f->floatArrayOf(.035f,.055f,.11f);else->floatArrayOf(.18f,.24f,.36f)};GLES20.glClearColor(sky[0],sky[1],sky[2],1f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT);GLES20.glUseProgram(program)
        val eyeX=px+sin(yaw)*zoom;val eyeZ=pz+cos(yaw)*zoom;val targetY=py+1.1f;val eyeY=targetY+4.4f+pitch*6f;Matrix.setLookAtM(view,0,eyeX,eyeY,eyeZ,px,targetY,pz,0f,1f,0f);Matrix.multiplyMM(vp,0,proj,0,view,0);GLES20.glUniform3f(uEye,eyeX,eyeY,eyeZ)
        if(world==0)renderMain() else renderImported();renderRuins();renderPortals();renderActors();renderPlayer();renderFx()
    }
    private fun renderMain(){for(x in -7..7)for(z in -7..7){val px0=x*10f;val pz0=z*10f;draw(px0,groundY(px0,pz0)-1f,pz0,5f,1f,5f,floatArrayOf(.18f+.02f*((x+z)and 1),.28f,.15f,1f))};for(i in 0 until 70){val a=i*2.399f;val r=18+(i%12)*4f;val x=sin(a)*r;val z=cos(a)*r;draw(x,groundY(x,z)+2f,z,.32f,2f,.32f,floatArrayOf(.24f,.14f,.07f,1f));draw(x,groundY(x,z)+5f,z,1.7f,2.4f,1.7f,floatArrayOf(.08f,.24f,.12f,1f))}}
    private fun renderImported(){for(x in -8..8)for(z in -8..8){val xx=x*10f;val zz=z*10f;draw(xx,groundY(xx,zz)-1f,zz,5f,1f,5f,if(world==1)floatArrayOf(.20f,.22f,.26f,1f) else floatArrayOf(.12f,.25f,.13f,1f))};worlds[world].forEachIndexed{i,o->if(i%1==0&&abs(o.x-px)<80&&abs(o.z-pz)<80){val y=o.y+(if(world==1)-.5f else 0f);draw(o.x,y,o.z,o.sx.coerceAtMost(8f),o.sy.coerceAtMost(8f),o.sz.coerceAtMost(8f),floatArrayOf(o.r,o.g,o.b,1f))}}}
    private fun renderRuins(){ruinPos[world].forEachIndexed{i,p->val active=ruinMask[world] and (1 shl i)!=0;val c=if(active)floatArrayOf(.20f,.95f,.72f,1f)else floatArrayOf(.45f,.34f,.75f,1f);for(k in 0 until 18){val a=k*PI.toFloat()/9+time*.35f;draw(p[0]+cos(a)*2.2f,groundY(p[0],p[1])+.08f,p[1]+sin(a)*2.2f,.12f,.06f,.35f,c,1f)};for(k in 0 until 6){val a=k*PI.toFloat()/3-time*.5f;draw(p[0]+cos(a)*1.35f,groundY(p[0],p[1])+1.0f+sin(time+k)*.3f,p[1]+sin(a)*1.35f,.16f,.16f,.16f,c,1f)}}}
    private fun portal(x:Float,z:Float,c:FloatArray){for(i in 0 until 24){val a=i*PI.toFloat()/12;draw(x+cos(a)*2.2f,groundY(x,z)+2.5f+sin(a)*2.5f,z,.16f,.16f,.16f,c,1f)}}
    private fun renderPortals(){if(world==0){portal(-32f,-34f,floatArrayOf(.5f,.3f,1f,1f));portal(32f,-34f,floatArrayOf(.1f,.75f,.9f,1f))}else portal(0f,-42f,floatArrayOf(.8f,.65f,.25f,1f))}
    private fun renderActors(){npcs[world].forEach{n->draw(n.x,groundY(n.x,n.z)+1f,n.z,.42f,1f,.42f,floatArrayOf(.32f,.58f,.75f,1f));draw(n.x,groundY(n.x,n.z)+2.2f,n.z,.30f,.30f,.30f,floatArrayOf(.72f,.55f,.42f,1f))};enemies[world].forEach{e->if(e.hp>0){val c=when(e.state){0->floatArrayOf(.22f,.06f,.25f,1f);1->floatArrayOf(.34f,.08f,.06f,1f);else->floatArrayOf(.06f,.13f,.25f,1f)};draw(e.x,groundY(e.x,e.z)+1f,e.z,.5f,1f,.5f,c);draw(e.x,groundY(e.x,e.z)+2.2f,e.z,.34f,.34f,.34f,c);if(e.state==2)draw(e.x,groundY(e.x,e.z)+3f,e.z,.12f,.12f,.12f,floatArrayOf(.2f,.7f,1f,1f),1f)}}}
    private fun renderPlayer(){val alpha=if(dashFx>.45f).35f else 1f;val robe=if(flying)floatArrayOf(.09f,.08f,.14f,alpha)else floatArrayOf(.14f,.10f,.23f,alpha);draw(px,py+1f,pz,.56f,1f,.42f,robe);draw(px,py+2.15f,pz,.31f,.34f,.31f,floatArrayOf(.72f,.52f,.38f,alpha));draw(px+.65f,py+1.5f,pz,.07f,1.25f,.07f,floatArrayOf(.22f,.12f,.05f,alpha));draw(px+.65f,py+2.85f,pz,.16f,.16f,.16f,floatArrayOf(.25f,.75f,1f,alpha),1f)}
    private fun renderFx(){if(dashFx>0){for(i in 0 until 16){val t=i/15f;val x=dashFromX+(dashToX-dashFromX)*t;val z=dashFromZ+(dashToZ-dashFromZ)*t;val s=.16f+(1-t)*.25f;draw(x,groundY(x,z)+1f+sin(time*8+i)*.5f,z,s,s,s,floatArrayOf(.015f,.02f,.04f,.55f),.5f);if(i%3==0)draw(x,groundY(x,z)+1.2f,z,s*.45f,s*.45f,s*.45f,floatArrayOf(.18f,.55f,1f,.8f),1f)}};if(flying){for(i in 0 until 12){val t=i/12f;val x=px+sin(yaw)*t*5f;val z=pz+cos(yaw)*t*5f;draw(x,py+.8f+sin(time*6+i)*.35f,z,.18f+t*.15f,.18f+t*.15f,.18f+t*.15f,floatArrayOf(.01f,.015f,.035f,.5f),.4f)}};if(comboFx>0){for(i in 0 until 28){val a=i*PI.toFloat()/14;val r=(1-comboFx)*12+1.2f;draw(px+cos(a)*r,groundY(px,pz)+.45f+sin(a*3+time*5)*.4f,pz+sin(a)*r,.13f,.13f,.13f,floatArrayOf(.25f,.72f,1f,.8f),1f)}}}
    private fun draw(x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,c:FloatArray,glow:Float=0f){Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,sx,sy,sz);Matrix.multiplyMM(mvp,0,vp,0,model,0);GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(uModel,1,false,model,0);GLES20.glUniform3f(uScale,sx,sy,sz);GLES20.glUniform4fv(uColor,1,c,0);GLES20.glUniform1f(uGlow,glow);cb.position(0);cn.position(0);GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,0,cb);GLES20.glEnableVertexAttribArray(aPos);GLES20.glVertexAttribPointer(aNormal,3,GLES20.GL_FLOAT,false,0,cn);GLES20.glEnableVertexAttribArray(aNormal);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,cube.size/3)}
}
