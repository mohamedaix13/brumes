package com.mumu.brumes

import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
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
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.*

private data class V07Hud(
    val life:Int,
    val mana:Int,
    val world:String,
    val flying:Boolean,
    val enemies:Int,
    val spell:String,
    val score:Int,
    val crystals:Int,
    val purified:Boolean,
    val combo:Int,
    val night:Boolean,
    val px:Float,
    val pz:Float
)


class GameActivityV07 : Activity() {
    private lateinit var root:FrameLayout
    private lateinit var game:BrumesV07View
    private var menu:View?=null
    private val gold=Color.rgb(228,196,126)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun panel(alpha:Int=185)=android.graphics.drawable.GradientDrawable().apply{
        setColor(Color.argb(alpha,7,11,16));cornerRadius=dp(13).toFloat();setStroke(dp(1),Color.argb(130,228,196,126))
    }
    private fun text(t:String,size:Float=11f)=TextView(this).apply{
        this.text=t;textSize=size;setTextColor(Color.rgb(245,242,234));setPadding(dp(9),dp(5),dp(9),dp(5))
    }
    private fun button(t:String,w:Int=70,onClick:()->Unit)=Button(this).apply{
        text=t;textSize=9.5f;isAllCaps=false;setTextColor(gold);backgroundTintList=null;background=panel(178)
        setPadding(dp(3),0,dp(3),0);minWidth=0;minHeight=0;setOnClickListener{onClick()}
        layoutParams=LinearLayout.LayoutParams(dp(w),dp(42))
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility=(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        root=FrameLayout(this).apply{isMotionEventSplittingEnabled=true}
        game=BrumesV07View(this);root.addView(game,FrameLayout.LayoutParams(-1,-1))

        val hud=text("BRUMES 0.8",10.5f).apply{background=panel(178)}
        root.addView(hud,FrameLayout.LayoutParams(dp(268),dp(78),Gravity.TOP or Gravity.START).apply{setMargins(dp(9),dp(7),0,0)})
        root.addView(button("\u2630",50){openMenu()},FrameLayout.LayoutParams(dp(54),dp(42),Gravity.TOP or Gravity.END).apply{setMargins(0,dp(7),dp(9),0)})

        val mini=V07Minimap(this)
        root.addView(mini,FrameLayout.LayoutParams(dp(96),dp(96),Gravity.TOP or Gravity.END).apply{setMargins(0,dp(56),dp(9),0)})

        val left=V07Stick(this){x,y->game.move(x,y)}
        root.addView(left,FrameLayout.LayoutParams(dp(122),dp(122),Gravity.BOTTOM or Gravity.START).apply{setMargins(dp(10),0,0,dp(9))})
        val right=V07Stick(this){x,y->game.look(x,y)}
        root.addView(right,FrameLayout.LayoutParams(dp(108),dp(108),Gravity.BOTTOM or Gravity.END).apply{setMargins(0,0,dp(10),dp(9))})

        val spells=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        spells.addView(button("\ud83d\udd25 Feu",66){game.element(0)})
        spells.addView(button("\ud83d\udca7 Eau",66){game.element(1)})
        spells.addView(button("\ud83c\udf2a Air",66){game.element(2)})
        spells.addView(button("\ud83e\udea8 Terre",66){game.element(3)})
        root.addView(spells,FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,dp(45),Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply{setMargins(0,0,0,dp(8))})

        val actions=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER}
        actions.addView(button("\u2726 Esquive",78){game.dash()})
        actions.addView(button("\u2601 Vol",78){game.flight()},LinearLayout.LayoutParams(dp(78),dp(42)).apply{topMargin=dp(4)})
        actions.addView(button("\u25c9 Ruine",78){game.fastTravel()},LinearLayout.LayoutParams(dp(78),dp(42)).apply{topMargin=dp(4)})
        root.addView(actions,FrameLayout.LayoutParams(dp(84),FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER_VERTICAL or Gravity.END).apply{setMargins(0,0,dp(11),0)})

        val elnames=arrayOf("Feu","Eau","Air","Terre")
        game.onHud={s->hud.text="BRUMES 0.9 \u00b7 ${s.world}${if(s.purified)" \u2727 purifi\u00e9" else ""}${if(s.night)" \u00b7 NUIT" else ""}\nVie ${s.life}   Mana ${s.mana}   ${if(s.flying)"VOL" else "SOL"}   Score ${s.score}\nCristaux ${s.crystals}/3 \u00b7 ${s.enemies} ennemis \u00b7 ${s.spell}${if(s.combo>=0)" \u00b7 \u2727 ${elnames[s.combo]}" else ""}";mini.player(s.px,s.pz,s.night)}
        setContentView(root)
    }
    override fun onPause(){game.onPause();super.onPause()}
    override fun onResume(){super.onResume();game.onResume()}
    @Deprecated("Deprecated in Java") override fun onBackPressed(){if(menu==null)openMenu() else closeMenu()}
    private fun closeMenu(){menu?.let{root.removeView(it)};menu=null;game.setPaused(false)}
    private fun openMenu(){
        if(menu!=null)return
        game.setPaused(true)
        val shade=FrameLayout(this).apply{setBackgroundColor(0xC3000000.toInt());isClickable=true}
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(18),dp(14),dp(18),dp(14));background=panel(238)}
        body.addView(text("B R U M E S",24f).apply{gravity=Gravity.CENTER;setTextColor(gold)})
        body.addView(text("0.9 \u00b7 ciels vivants \u00b7 eau anim\u00e9e \u00b7 ombres \u00b7 qu\u00eate des cristaux \u00b7 sauvegarde",11f).apply{gravity=Gravity.CENTER})
        body.addView(button("REPRENDRE",205){closeMenu()},LinearLayout.LayoutParams(dp(205),dp(46)).apply{topMargin=dp(7)})
        body.addView(text("Joystick gauche : d\u00e9placement. Droit : cam\u00e9ra et hauteur en vol. Deux \u00e9l\u00e9ments lancent une combinaison (givre, lave, vapeur\u2026). Active les 3 cristaux d'un monde pour le purifier. Les portails changent de monde.",10.5f).apply{gravity=Gravity.CENTER})
        shade.addView(body,FrameLayout.LayoutParams(dp(440),FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER))
        root.addView(shade,FrameLayout.LayoutParams(-1,-1));menu=shade
    }
}

private class V07Minimap(activity:Activity):View(activity){
    private val bg=Paint(Paint.ANTI_ALIAS_FLAG);private val fg=Paint(Paint.ANTI_ALIAS_FLAG);private val f2=Paint(Paint.ANTI_ALIAS_FLAG);private val pl=Paint(Paint.ANTI_ALIAS_FLAG)
    private var px=0f;private var pz=8f;private var night=false
    fun player(x:Float,z:Float,n:Boolean){px=x;pz=z;night=n;invalidate()}
    private fun w2s(v:Float,size:Int)=size/2f+v/130f*size
    override fun onDraw(c:Canvas){
        val s=min(width,height).toFloat();bg.color=Color.argb(150,7,11,16);c.drawRoundRect(0f,0f,s,s,dp6(),dp6(),bg);bg.style=Paint.Style.STROKE;bg.color=Color.argb(120,228,196,126);bg.strokeWidth=2f;c.drawRoundRect(1f,1f,s-1f,s-1f,dp6(),dp6(),bg);bg.style=Paint.Style.FILL
        fg.color=Color.argb(180,40,90,150);c.drawCircle(w2s(24f,s),w2s(0f,s),3f,fg);c.drawCircle(w2s(-24f,s),w2s(0f,s),3f,fg);c.drawCircle(w2s(0f,s),w2s(18f,s),3f,fg)
        f2.color=Color.argb(220,80,200,255);c.drawCircle(w2s(24f,s),w2s(0f,s),1.6f,f2);c.drawCircle(w2s(-24f,s),w2s(0f,s),1.6f,f2);c.drawCircle(w2s(0f,s),w2s(18f,s),1.6f,f2)
        pl.color=if(night)Color.argb(255,120,180,255) else Color.argb(255,255,220,120);c.drawCircle(w2s(px,s),w2s(pz,s),3.4f,pl)
    }
    private fun dp6()=6f
}

private class V07Stick(activity:Activity,val out:(Float,Float)->Unit):View(activity){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG);private var dx=0f;private var dy=0f
    override fun onDraw(c:Canvas){
        val r=min(width,height)*.39f;p.style=Paint.Style.FILL;p.color=0x2410151B;c.drawCircle(width/2f,height/2f,r,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=2f;p.color=0x88E4C47E.toInt();c.drawCircle(width/2f,height/2f,r,p)
        p.style=Paint.Style.FILL;p.color=0xA8E4C47E.toInt();c.drawCircle(width/2f+dx*r,height/2f+dy*r,r*.21f,p)
    }
    override fun onTouchEvent(e:MotionEvent):Boolean{
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE->{
                dx=(e.x-width/2)/(width*.39f);dy=(e.y-height/2)/(height*.39f);val l=max(1f,hypot(dx,dy));dx/=l;dy/=l
            }
            else->{dx=0f;dy=0f}
        }
        out(dx,dy);invalidate();return true
    }
}

private data class V07Obj(val x:Float,val y:Float,val z:Float,val sx:Float,val sy:Float,val sz:Float,val r:Float,val g:Float,val b:Float)
private data class V07Mob(var x:Float,var z:Float,val friendly:Boolean,var phase:Float,var hp:Float=100f,var maxHp:Float=100f,var cooldown:Float=0f,var freeze:Float=0f,var slow:Float=0f,var burn:Float=0f,var ranged:Boolean=false,var boss:Boolean=false)
private data class V07Spark(var x:Float,var y:Float,var z:Float,var vx:Float,var vy:Float,var vz:Float,var life:Float,val r:Float,val g:Float,val b:Float,val size:Float)
private data class V07Pickup(var x:Float,var z:Float,var life:Float,val kind:Int)

private class BrumesV07View(activity:Activity):GLSurfaceView(activity){
    internal var onHud:((V07Hud)->Unit)?=null
    private val r=V07Renderer(activity,activity.assets){h->post{onHud?.invoke(h)}}
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
        if(e.pointerCount>=2){
            val d=hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1));if(pinch>0)r.zoom((pinch-d)/230f);pinch=d
        } else if(e.actionMasked==MotionEvent.ACTION_UP||e.actionMasked==MotionEvent.ACTION_CANCEL)pinch=0f
        return true
    }
}

private class V07Renderer(private val ctx:Context,private val assets:AssetManager,private val hud:(V07Hud)->Unit):GLSurfaceView.Renderer{
    @Volatile var mx=0f;@Volatile var my=0f;@Volatile var lx=0f;@Volatile var ly=0f;@Volatile var paused=false
    private var program=0
    private var aPos=0;private var aNor=0;private var aUv=0
    private var uMvp=0;private var uModel=0;private var uColor=0;private var uEye=0;private var uGlow=0;private var uFog=0;private var uFogColor=0;private var uUseTex=0;private var uSky=0;private var uTex=0;private var uTime=0;private var uWind=0;private var uNight=0;private var uMinY=0
    private val proj=FloatArray(16);private val view=FloatArray(16);private val vp=FloatArray(16);private val model=FloatArray(16);private val mvp=FloatArray(16)
    private lateinit var cube:FloatBuffer;private var cubeCount=0
    private lateinit var sphere:FloatBuffer;private var sphereCount=0
    private lateinit var quad:FloatBuffer;private var quadCount=0
    private val terrain=Array<FloatBuffer?>(3){null};private val terrainCount=IntArray(3)
    private val maps=Array(3){mutableListOf<V07Obj>()};private val mapYOffset=FloatArray(3)
    private val mobs=Array(3){mutableListOf<V07Mob>()};private val sparks=mutableListOf<V07Spark>();private val pickups=mutableListOf<V07Pickup>()
    private val names=arrayOf("Brumes","Cit\u00e9 d'\u00c9ther","\u00cele For\u00eat")
    private val grassTufts=Array(3){mutableListOf<FloatArray>()}

    private var texGrass=0;private var texDirt=0;private var texStone=0;private var texLog=0;private var texLeaves=0;private var texMoss=0;private var texCobble=0;private var texObsidian=0;private var texBrick=0;private var texDeep=0;private var texPlank=0;private var texBlue=0
    private var world=0;private var px=0f;private var py=1.3f;private var pz=8f;private var yaw=0f;private var pitch=.20f;private var camZoom=10.5f
    private var flying=false;private var flyBoost=0f;private var life=100f;private var mana=100f;private var last=0L;private var t=0f;private var hudT=0f
    private var selected=-1;private var spell="Aucun";private var dashFx=0f;private var portalCooldown=0f;private var spawnGrace=6f
    private var score=0;private var kills=0
    private var dayT=0.32f;private var nightMul=0f;private var lastSave=0f
    private val ruins=arrayOf(
        arrayOf(floatArrayOf(-30f,-18f),floatArrayOf(27f,23f),floatArrayOf(8f,-34f)),
        arrayOf(floatArrayOf(-28f,18f),floatArrayOf(22f,-24f),floatArrayOf(34f,8f)),
        arrayOf(floatArrayOf(-28f,-10f),floatArrayOf(12f,28f),floatArrayOf(34f,-22f))
    )
    private val crystals=arrayOf(BooleanArray(3),BooleanArray(3),BooleanArray(3))
    private val purified=BooleanArray(3)
    private var ruinIndex=0

    private fun save(){
        try{
            val p=ctx.getSharedPreferences("brumes_v08",Context.MODE_PRIVATE).edit()
            p.putInt("score",score).putInt("kills",kills).putInt("world",world)
            p.putFloat("px",px).putFloat("pz",pz).putFloat("yaw",yaw).putFloat("life",life).putFloat("mana",mana).putFloat("dayT",dayT)
            for(w in 0..2){p.putBoolean("pur$w",purified[w]);for(i in 0..2)p.putBoolean("cr$w$i",crystals[w][i])}
            p.apply()
        }catch(_:Throwable){}
    }
    private fun load(){
        try{
            val p=ctx.getSharedPreferences("brumes_v08",Context.MODE_PRIVATE)
            if(!p.contains("score"))return
            score=p.getInt("score",0);kills=p.getInt("kills",0);world=p.getInt("world",0)
            px=p.getFloat("px",0f);pz=p.getFloat("pz",8f);yaw=p.getFloat("yaw",0f);life=p.getFloat("life",100f);mana=p.getFloat("mana",100f);dayT=p.getFloat("dayT",0.32f)
            for(w in 0..2){purified[w]=p.getBoolean("pur$w",false);for(i in 0..2)crystals[w][i]=p.getBoolean("cr$w$i",false)}
        }catch(_:Throwable){}
    }

    private fun fb(a:FloatArray)=ByteBuffer.allocateDirect(a.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(a);position(0)}

    private fun cubeMesh():FloatArray{
        val o=ArrayList<Float>(288)
        fun q(nx:Float,ny:Float,nz:Float,a:FloatArray,b:FloatArray,c:FloatArray,d:FloatArray){
            val v=arrayOf(a,b,c,a,c,d);val uv=arrayOf(floatArrayOf(0f,1f),floatArrayOf(1f,1f),floatArrayOf(1f,0f),floatArrayOf(0f,1f),floatArrayOf(1f,0f),floatArrayOf(0f,0f))
            for(i in v.indices){val p=v[i];o+=p[0];o+=p[1];o+=p[2];o+=nx;o+=ny;o+=nz;o+=uv[i][0];o+=uv[i][1]}
        }
        q(0f,0f,1f,floatArrayOf(-1f,-1f,1f),floatArrayOf(1f,-1f,1f),floatArrayOf(1f,1f,1f),floatArrayOf(-1f,1f,1f))
        q(0f,0f,-1f,floatArrayOf(1f,-1f,-1f),floatArrayOf(-1f,-1f,-1f),floatArrayOf(-1f,1f,-1f),floatArrayOf(1f,1f,-1f))
        q(1f,0f,0f,floatArrayOf(1f,-1f,1f),floatArrayOf(1f,-1f,-1f),floatArrayOf(1f,1f,-1f),floatArrayOf(1f,1f,1f))
        q(-1f,0f,0f,floatArrayOf(-1f,-1f,-1f),floatArrayOf(-1f,-1f,1f),floatArrayOf(-1f,1f,1f),floatArrayOf(-1f,1f,-1f))
        q(0f,1f,0f,floatArrayOf(-1f,1f,1f),floatArrayOf(1f,1f,1f),floatArrayOf(1f,1f,-1f),floatArrayOf(-1f,1f,-1f))
        q(0f,-1f,0f,floatArrayOf(-1f,-1f,-1f),floatArrayOf(1f,-1f,-1f),floatArrayOf(1f,-1f,1f),floatArrayOf(-1f,-1f,1f))
        return o.toFloatArray()
    }

    private fun sphereMesh(seg:Int=18,rings:Int=12):FloatArray{
        val o=ArrayList<Float>()
        fun p(th:Float,ph:Float):FloatArray{val y=sin(ph);val rr=cos(ph);return floatArrayOf(rr*cos(th),y,rr*sin(th))}
        for(j in 0 until rings){
            val p0=-PI.toFloat()/2+PI.toFloat()*j/rings;val p1=-PI.toFloat()/2+PI.toFloat()*(j+1)/rings
            for(i in 0 until seg){
                val t0=2*PI.toFloat()*i/seg;val t1=2*PI.toFloat()*(i+1)/seg
                val a=p(t0,p0);val b=p(t1,p0);val c=p(t1,p1);val d=p(t0,p1)
                val vv=arrayOf(a,b,c,a,c,d)
                val uv=arrayOf(floatArrayOf(i.toFloat()/seg,1f-j.toFloat()/rings),floatArrayOf((i+1f)/seg,1f-j.toFloat()/rings),floatArrayOf((i+1f)/seg,1f-(j+1f)/rings),floatArrayOf(i.toFloat()/seg,1f-j.toFloat()/rings),floatArrayOf((i+1f)/seg,1f-(j+1f)/rings),floatArrayOf(i.toFloat()/seg,1f-(j+1f)/rings))
                for(k in vv.indices){val v=vv[k];o+=v[0];o+=v[1];o+=v[2];o+=v[0];o+=v[1];o+=v[2];o+=uv[k][0];o+=uv[k][1]}
            }
        }
        return o.toFloatArray()
    }

    private fun quadMesh():FloatArray{
        val o=ArrayList<Float>()
        val v=arrayOf(floatArrayOf(-1f,0f,-1f),floatArrayOf(1f,0f,-1f),floatArrayOf(1f,0f,1f),floatArrayOf(-1f,0f,-1f),floatArrayOf(1f,0f,1f),floatArrayOf(-1f,0f,1f))
        val uv=arrayOf(floatArrayOf(0f,0f),floatArrayOf(1f,0f),floatArrayOf(1f,1f),floatArrayOf(0f,0f),floatArrayOf(1f,1f),floatArrayOf(0f,1f))
        for(i in v.indices){val p=v[i];o+=p[0];o+=p[1];o+=p[2];o+=0f;o+=1f;o+=0f;o+=uv[i][0];o+=uv[i][1]}
        return o.toFloatArray()
    }

    private fun ground(x:Float,z:Float,w:Int=world):Float{
        return when(w){
            0->sin(x*.065f)*1.05f+cos(z*.058f)*.72f+sin((x+z)*.032f)*.42f
            1->sin(x*.045f+1.2f)*.34f+cos(z*.04f)*.28f
            else->sin(x*.052f+2f)*.55f+cos(z*.049f-.8f)*.42f
        }
    }

    private fun terrainMesh(w:Int):FloatArray{
        val o=ArrayList<Float>();val n=32;val step=4.2f
        fun v(x:Float,z:Float):FloatArray{
            val y=ground(x,z,w);val e=.35f;val dx=ground(x+e,z,w)-ground(x-e,z,w);val dz=ground(x,z+e,w)-ground(x,z-e,w)
            var nx=-dx;var ny=e*2;var nz=-dz;val l=max(.001f,sqrt(nx*nx+ny*ny+nz*nz));nx/=l;ny/=l;nz/=l
            return floatArrayOf(x,y,z,nx,ny,nz,x*.105f,z*.105f)
        }
        for(iz in 0 until n){for(ix in 0 until n){
            val x0=(ix-n/2)*step;val z0=(iz-n/2)*step;val x1=x0+step;val z1=z0+step
            val a=v(x0,z0);val b=v(x1,z0);val c=v(x1,z1);val d=v(x0,z1);for(q in arrayOf(a,b,c,a,c,d))for(f in q)o+=f
        }}
        return o.toFloatArray()
    }

    private fun loadMap(name:String,w:Int){
        try{
            val text=assets.open(name).bufferedReader().use{it.readText()};val arr=JSONObject(text).getJSONArray("objects")
            var minBase=Float.MAX_VALUE
            for(i in 0 until arr.length()){
                val a=arr.getJSONArray(i)
                val o=V07Obj(a.getDouble(0).toFloat(),a.getDouble(1).toFloat(),a.getDouble(2).toFloat(),a.getDouble(3).toFloat()/2f,a.getDouble(4).toFloat()/2f,a.getDouble(5).toFloat()/2f,a.getInt(6)/255f,a.getInt(7)/255f,a.getInt(8)/255f)
                maps[w]+=o;minBase=min(minBase,o.y-o.sy)
            }
            if(minBase<Float.MAX_VALUE)mapYOffset[w]=-minBase+.08f
        }catch(_:Throwable){}
    }

    private fun loadAssetTexture(name:String,repeat:Boolean=true):Int{
        val bmp=try{assets.open(name).use{BitmapFactory.decodeStream(it)}}catch(_:Throwable){null} ?: return 0
        return uploadTexture(bmp,repeat)
    }
    private fun uploadTexture(bmp:Bitmap,repeat:Boolean):Int{
        val ids=IntArray(1);GLES20.glGenTextures(1,ids,0);val id=ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,id)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,if(repeat)GLES20.GL_REPEAT else GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,if(repeat)GLES20.GL_REPEAT else GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bmp,0);GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);bmp.recycle();return id
    }

    override fun onSurfaceCreated(gl:javax.microedition.khronos.opengles.GL10?,cfg:javax.microedition.khronos.egl.EGLConfig?){
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glEnable(GLES20.GL_BLEND);GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);GLES20.glEnable(GLES20.GL_CULL_FACE)
        val vs="""attribute vec3 a;attribute vec3 n;attribute vec2 uv;uniform mat4 m;uniform mat4 model;uniform float time;uniform float wind;uniform float minY;varying vec3 N;varying vec3 W;varying vec2 T;void main(){vec4 wp=model*vec4(a,1.0);if(wind>1.5){wp.y+=sin(time*2.0+wp.x*0.5)*0.12+cos(time*1.6+wp.z*0.4)*0.1;}else if(wind>0.5){float h=clamp((wp.y-minY)/3.0,0.0,1.0);float s=sin(time*1.8+wp.x*0.4+wp.z*0.3)*0.09*h;wp.x+=s;wp.z+=cos(time*1.3+wp.z*0.25)*0.05*h;}W=wp.xyz;N=normalize(n);T=uv;gl_Position=m*vec4(a,1.0);}"""
        val fs="""precision mediump float;uniform vec4 c;uniform vec3 eye;uniform float glow;uniform float fog;uniform vec3 fogColor;uniform float useTex;uniform float sky;uniform float night;uniform float time;uniform sampler2D tex;varying vec3 N;varying vec3 W;varying vec2 T;void main(){vec4 tx=texture2D(tex,T);if(sky>0.5){vec3 dir=normalize(W-eye);float h=clamp(dir.y*.5+.5,0.0,1.0);vec3 horizon=c.rgb*.72+vec3(.08,.09,.12);vec3 zenith=c.rgb*1.18;vec3 sc=mix(horizon,zenith,pow(h,.72));if(night>0.05&&dir.y>0.06){vec3 sdir=floor(dir*260.0);float n=fract(sin(dot(sdir,vec3(12.9898,78.233,37.719)))*43758.5453);float st=step(0.996,n);sc+=vec3(st)*(0.55+0.45*sin(time*2.5+n*13.0))*smoothstep(0.05,0.5,night);}gl_FragColor=vec4(sc,1.0);return;}vec3 base=c.rgb;if(useTex>0.5){base*=mix(vec3(1.0),tx.rgb*1.45,0.92);}float d=max(dot(normalize(N),normalize(vec3(-0.35,0.82,0.42))),0.0);vec3 col=base*(0.36+d*0.76)+base*glow*0.58;float dist=length(W-eye);float hf=1.0-exp(-pow(max(0.0,(2.5-W.y))*fog*1.4,1.7));float df=1.0-exp(-pow(dist*fog,2.0));float f=clamp(max(hf,df),0.0,0.85);gl_FragColor=vec4(mix(col,fogColor,f),c.a*(useTex>0.5?tx.a:1.0));}"""
        fun sh(type:Int,s:String)=GLES20.glCreateShader(type).also{GLES20.glShaderSource(it,s);GLES20.glCompileShader(it)}
        val sv=sh(GLES20.GL_VERTEX_SHADER,vs);val sf=sh(GLES20.GL_FRAGMENT_SHADER,fs);program=GLES20.glCreateProgram();GLES20.glAttachShader(program,sv);GLES20.glAttachShader(program,sf);GLES20.glLinkProgram(program);GLES20.glUseProgram(program)
        aPos=GLES20.glGetAttribLocation(program,"a");aNor=GLES20.glGetAttribLocation(program,"n");aUv=GLES20.glGetAttribLocation(program,"uv")
        uMvp=GLES20.glGetUniformLocation(program,"m");uModel=GLES20.glGetUniformLocation(program,"model");uColor=GLES20.glGetUniformLocation(program,"c");uEye=GLES20.glGetUniformLocation(program,"eye");uGlow=GLES20.glGetUniformLocation(program,"glow");uFog=GLES20.glGetUniformLocation(program,"fog");uFogColor=GLES20.glGetUniformLocation(program,"fogColor");uUseTex=GLES20.glGetUniformLocation(program,"useTex");uSky=GLES20.glGetUniformLocation(program,"sky");uTex=GLES20.glGetUniformLocation(program,"tex");uTime=GLES20.glGetUniformLocation(program,"time");uWind=GLES20.glGetUniformLocation(program,"wind");uNight=GLES20.glGetUniformLocation(program,"night");uMinY=GLES20.glGetUniformLocation(program,"minY")
        val cm=cubeMesh();cube=fb(cm);cubeCount=cm.size/8;val sm=sphereMesh();sphere=fb(sm);sphereCount=sm.size/8;val qm=quadMesh();quad=fb(qm);quadCount=qm.size/8
        for(w in 0..2){val tm=terrainMesh(w);terrain[w]=fb(tm);terrainCount[w]=tm.size/8}
        loadMap("world_aether.json",1);loadMap("world_forest.json",2)
        texGrass=loadAssetTexture("grass_top.png");texDirt=loadAssetTexture("dirt.png");texStone=loadAssetTexture("stone.png");texLog=loadAssetTexture("log_oak.png");texBlue=loadAssetTexture("wool_colored_blue.png")
        texLeaves=texGrass;texMoss=texGrass;texCobble=texStone;texObsidian=texStone;texBrick=texStone;texDeep=texStone;texPlank=texLog
        seedTufts();seedMobs();load();py=ground(px,pz)+1.25f
    }

    override fun onSurfaceChanged(gl:javax.microedition.khronos.opengles.GL10?,w:Int,h:Int){
        GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(proj,0,58f,w.toFloat()/max(1,h),.12f,190f)
    }

    private fun seedTufts(){
        for(w in 0..2){val n=if(w==0)28 else 18;repeat(n){val ang=it*2.399f;val rad=8f+(it%6)*8.5f;grassTufts[w]+=floatArrayOf(cos(ang)*rad,0f,sin(ang)*rad)}}
    }

    private fun seedMobs(){
        val pos=arrayOf(arrayOf(floatArrayOf(-16f,-16f),floatArrayOf(16f,-18f),floatArrayOf(21f,13f),floatArrayOf(-22f,18f),floatArrayOf(4f,25f)),arrayOf(floatArrayOf(-18f,14f),floatArrayOf(18f,-14f),floatArrayOf(24f,18f),floatArrayOf(-25f,-18f),floatArrayOf(6f,28f)),arrayOf(floatArrayOf(-18f,-12f),floatArrayOf(18f,14f),floatArrayOf(26f,-16f),floatArrayOf(-24f,19f),floatArrayOf(8f,25f)))
        for(w in 0..2){for(i in pos[w].indices){val p=pos[w][i];val ranged=(w==1&&i%2==0);val boss=(i==3);val hp=if(boss)260f else 100f;mobs[w]+=V07Mob(p[0],p[1],i==4||i==3,i*.9f,hp,hp,0f,0f,0f,0f,ranged,boss)}}
    }

    fun zoom(v:Float){camZoom=(camZoom+v).coerceIn(6.5f,16f)}
    fun toggleFlight(){flying=!flying;flyBoost=if(flying)7.5f else 0f;spell=if(flying)"Transplanage" else "Atterrissage";burst(px,py,pz,.12f,.12f,.16f,18)}
    fun dash(){
        val f=-my;val s=mx;var dx=sin(yaw)*f+cos(yaw)*s;var dz=-cos(yaw)*f+sin(yaw)*s
        if(hypot(dx,dz)<.18f){dx=sin(yaw);dz=-cos(yaw)};val l=max(.01f,hypot(dx,dz));dx/=l;dz/=l
        px=(px+dx*7.2f).coerceIn(-58f,58f);pz=(pz+dz*7.2f).coerceIn(-58f,58f);dashFx=.55f;burst(px,py,pz,.04f,.04f,.05f,24)
    }
    fun fastTravel(){val r=ruins[world][ruinIndex%3];px=r[0];pz=r[1];py=ground(px,pz)+1.3f;ruinIndex++;spell="Ruine ${ruinIndex%3+1}"}
    fun element(i:Int){
        val names=arrayOf("Feu","Eau","Air","Terre")
        if(selected<0){selected=i;spell="${names[i]} pr\u00eat";return}
        if(selected==i){spell=names[i];cast(i,i);selected=-1;return}
        val a=min(selected,i);val b=max(selected,i)
        spell=when(a*10+b){1->"Vapeur";2->"Temp\u00eate de braises";3->"Lave";12->"Givre";13->"Boue entravante";23->"Temp\u00eate de sable";else->"Arcane"}
        cast(selected,i);selected=-1
    }
    private fun cast(a:Int,b:Int){
        if(mana<8)return;mana=max(0f,mana-8f)
        val combo=setOf(a,b)
        val col=when{a==0&&b==0->floatArrayOf(1f,.22f,.05f);a==1&&b==1->floatArrayOf(.08f,.55f,1f);a==2&&b==2->floatArrayOf(.72f,.85f,1f);a==3&&b==3->floatArrayOf(.7f,.48f,.25f);combo==setOf(0,3)->floatArrayOf(1f,.24f,.03f);combo==setOf(1,2)->floatArrayOf(.55f,.9f,1f);else->floatArrayOf(.85f,.7f,.95f)}
        burst(px+sin(yaw)*2f,py+.7f,pz-cos(yaw)*2f,col[0],col[1],col[2],36)
        val radius=if(combo==setOf(0,2))11f else 8.5f
        val dmg=when{combo==setOf(0,3)->46f;combo==setOf(0,2)->30f;combo==setOf(1,2)->28f;a==b&&a==0->40f;a==b&&a==1->30f;else->34f}
        for(m in mobs[world])if(!m.friendly&&m.hp>0&&hypot(m.x-px,m.z-pz)<radius){
            m.hp-=dmg
            when{combo==setOf(1,2)->m.freeze=2.6f;combo==setOf(1,3)->m.slow=4f;combo==setOf(0,3)->m.burn=3.5f;combo==setOf(2,3)->m.slow=2.5f}
            burst(m.x,ground(m.x,m.z)+1f,m.z,col[0],col[1],col[2],8)
        }
        if(combo==setOf(0,1)){life=min(100f,life+18f);spell="Vapeur (+vie)"}
    }
    private fun burst(x:Float,y:Float,z:Float,r:Float,g:Float,b:Float,count:Int){
        repeat(count){val a=(it*2.399f+t);val sp=.8f+(it%5)*.18f;sparks+=V07Spark(x,y,z,cos(a)*sp,((it%7)-2)*.22f,sin(a)*sp,.65f+(it%4)*.08f,r,g,b,.07f+(it%3)*.025f)}
        while(sparks.size>140)sparks.removeAt(0)
    }

    private fun update(dt:Float){
        if(paused)return;t+=dt;dayT=(dayT+dt*.012f)%1f;portalCooldown=max(0f,portalCooldown-dt);spawnGrace=max(0f,spawnGrace-dt);mana=min(100f,mana+5.5f*dt)
        lastSave+=dt;if(lastSave>5f){lastSave=0f;save()}
        yaw+=lx*1.85f*dt;pitch=(pitch-ly*.8f*dt).coerceIn(-.18f,.62f)
        val f=-my;val s=mx;val dx=sin(yaw)*f+cos(yaw)*s;val dz=-cos(yaw)*f+sin(yaw)*s;val speed=if(flying)7.4f else 5.4f
        px=(px+dx*speed*dt).coerceIn(-60f,60f);pz=(pz+dz*speed*dt).coerceIn(-60f,60f)
        if(flying){flyBoost=max(0f,flyBoost-14f*dt);py+=( -ly*5.3f + flyBoost)*dt;py=py.coerceIn(1.1f,34f)}else py=ground(px,pz)+1.25f
        if(dashFx>0)dashFx=max(0f,dashFx-dt)
        val iter=sparks.iterator();while(iter.hasNext()){val s0=iter.next();s0.x+=s0.vx*dt;s0.y+=s0.vy*dt;s0.z+=s0.vz*dt;s0.vy+=.18f*dt;s0.life-=dt;if(s0.life<=0)iter.remove()}
        val pit=pickups.iterator();while(pit.hasNext()){val p=pit.next();p.life-=dt;if(hypot(px-p.x,pz-p.z)<1.6f){if(p.kind==0)life=min(100f,life+30f) else mana=min(100f,mana+35f);burst(p.x,ground(p.x,p.z)+.5f,p.z,if(p.kind==0)1f else .4f,if(p.kind==0).2f else .6f,if(p.kind==0).2f else 1f,10);pit.remove();spell=if(p.kind==0)"Soin r\u00e9cup\u00e9r\u00e9" else "Mana r\u00e9cup\u00e9r\u00e9"}else if(p.life<=0)pit.remove()}
        for(m in mobs[world]){
            if(m.hp<=0){m.x=(sin(m.phase*4.1f)*26f);m.z=(cos(m.phase*3.7f)*24f);m.hp=m.maxHp;m.freeze=0f;m.slow=0f;m.burn=0f;m.cooldown=0f}
            m.phase+=dt*(if(m.friendly).38f else .62f);m.cooldown=max(0f,m.cooldown-dt)
            if(m.freeze>0)m.freeze=max(0f,m.freeze-dt);if(m.slow>0)m.slow=max(0f,m.slow-dt)
            if(m.burn>0){m.burn=max(0f,m.burn-dt);m.hp-=14f*dt;if(m.hp<=0&&kills<999){kills++;score+=if(m.boss)200 else 40;dropLoot(m);burst(m.x,ground(m.x,m.z)+1f,m.z,1f,.3f,.05f,12)}}
            val d=hypot(px-m.x,pz-m.z)
            if(!m.friendly&&m.freeze<=0f&&!purified[world]){
                val spd=(if(m.boss)1.0f else 1.35f)*(if(m.slow>0).5f else 1f)
                if(m.ranged&&d<16f&&d>4f&&m.cooldown<=0){m.cooldown=2.2f;enemyBolt(m.x,ground(m.x,m.z)+1.2f,m.z,px,py+1f,pz)}
                else if(d<10f&&spawnGrace<=0){val ux=(px-m.x)/max(.1f,d);val uz=(pz-m.z)/max(.1f,d);if(d>3f){m.x+=ux*spd*dt;m.z+=uz*spd*dt}else if(d<2.15f&&m.cooldown<=0){life-=(if(m.boss)7f else 3.5f)*(1f+nightMul*.6f);m.cooldown=1.25f;burst(px,py+.5f,pz,.65f,.05f,.05f,7)}}
                else{m.x+=cos(m.phase)*.34f*dt;m.z+=sin(m.phase*.91f)*.34f*dt}
            }else{m.x+=cos(m.phase)*.34f*dt;m.z+=sin(m.phase*.91f)*.34f*dt}
        }
        if(life<=0){life=100f;mana=max(mana,60f);px=0f;pz=8f;py=ground(px,pz)+1.25f;spawnGrace=6f;spell="R\u00e9apparition"}
        checkCrystals();checkPortals()
        hudT+=dt;if(hudT>.15f){hudT=0f;hud(V07Hud(life.roundToInt().coerceIn(0,100),mana.roundToInt().coerceIn(0,100),names[world],flying,mobs[world].count{!it.friendly&&it.hp>0},spell,score,crystals[world].count{it},purified[world],if(selected<0)-1 else selected,night,px,pz))}
    }

    private fun dropLoot(m:V07Mob){val kind=if(kills%2==0)0 else 1;pickups+=V07Pickup(m.x,m.z,12f,kind)}

    private fun enemyBolt(x:Float,y:Float,z:Float,tx:Float,ty:Float,tz:Float){
        val dx=tx-x;val dy=ty-y;val dz=tz-z;val l=max(.1f,hypot(hypot(dx,dz),dy))
        sparks+=V07Spark(x,y,z,dx/l*7f,dy/l*7f,dz/l*7f,1.2f,.9f,.3f,.12f)
    }

    private fun checkCrystals(){
        if(purified[world])return
        for(i in 0..2){if(!crystals[world][i]){val r=ruins[world][i];if(hypot(px-r[0],pz-r[1])<2.6f){crystals[world][i]=true;life=min(100f,life+25f);mana=min(100f,mana+30f);score+=100;burst(r[0],ground(r[0],r[1])+1f,r[1],.3f,.75f,1f,24);spell="Cristal ${i+1}/3 activ\u00e9"}}}
        if(crystals[world].all{it}&&!purified[world]){purified[world]=true;spawnGrace=8f;for(m in mobs[world])if(!m.friendly){m.friendly=true;m.hp=m.maxHp};burst(px,py+1f,pz,.8f,.9f,1f,40);spell="Monde purifi\u00e9 \u2727"}
    }

    private fun checkPortals(){
        if(portalCooldown>0)return
        if(world==0){
            if(hypot(px-24f,pz)<2.7f){world=1;px=0f;pz=8f;py=ground(px,pz)+1.25f;portalCooldown=2.2f;spawnGrace=4f;spell="Cit\u00e9 d'\u00c9ther"}
            else if(hypot(px+24f,pz)<2.7f){world=2;px=0f;pz=8f;py=ground(px,pz)+1.25f;portalCooldown=2.2f;spawnGrace=4f;spell="\u00cele For\u00eat"}
        }else if(hypot(px,pz-18f)<2.8f){world=0;px=0f;pz=8f;py=ground(px,pz)+1.25f;portalCooldown=2.2f;spawnGrace=4f;spell="Brumes"}
    }

    override fun onDrawFrame(gl:javax.microedition.khronos.opengles.GL10?){
        val now=System.nanoTime();val dt=if(last==0L)0f else min(.033f,(now-last)/1_000_000_000f);last=now;update(dt)
        val dn=dayT*6.28318f;val nightAmt=(1f-cos(dn))*.5f;nightMul=nightAmt;val night=nightAmt>.55f
        fun lerp(a:Float,b:Float,f:Float)=a+(b-a)*f
        val dayCol=when(world){0->floatArrayOf(.16f,.24f,.32f);1->floatArrayOf(.18f,.25f,.36f);else->floatArrayOf(.13f,.24f,.22f)}
        val niteCol=when(world){0->floatArrayOf(.025f,.045f,.085f);1->floatArrayOf(.03f,.04f,.10f);else->floatArrayOf(.02f,.055f,.065f)}
        val clear=floatArrayOf(lerp(dayCol[0],niteCol[0],nightAmt),lerp(dayCol[1],niteCol[1],nightAmt),lerp(dayCol[2],niteCol[2],nightAmt))
        GLES20.glClearColor(clear[0],clear[1],clear[2],1f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT);GLES20.glUseProgram(program)
        val eyeY=py+2.4f+sin(pitch)*3.2f;val ex=px-sin(yaw)*cos(pitch)*camZoom;val ez=pz+cos(yaw)*cos(pitch)*camZoom
        Matrix.setLookAtM(view,0,ex,eyeY,ez,px,py+.75f,pz,0f,1f,0f);Matrix.multiplyMM(vp,0,proj,0,view,0)
        GLES20.glUniform3f(uEye,ex,eyeY,ez);GLES20.glUniform1f(uFog,if(world==1).011f else .0090f)
        val fcd=when(world){0->floatArrayOf(.22f,.28f,.31f);1->floatArrayOf(.20f,.24f,.34f);else->floatArrayOf(.18f,.28f,.25f)};val fcn=floatArrayOf(.05f,.06f,.12f);GLES20.glUniform3f(uFogColor,lerp(fcd[0],fcn[0],nightAmt),lerp(fcd[1],fcn[1],nightAmt),lerp(fcd[2],fcn[2],nightAmt));GLES20.glUniform1i(uTex,0)
        GLES20.glUniform1f(uTime,t);GLES20.glUniform1f(uNight,nightAmt);GLES20.glUniform1f(uWind,0f)

        drawSky(ex,eyeY,ez,night,nightAmt)
        drawWater()
        drawTerrain()
        if(world==0)drawMainScenery() else drawImportedMap(world)
        drawGrass();drawRuins();drawPortals();drawMobs();drawPlayer();drawPickups();drawSparks()
    }

    private fun bindMesh(buf:FloatBuffer){
        buf.position(0);GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,32,buf);GLES20.glEnableVertexAttribArray(aPos)
        buf.position(3);GLES20.glVertexAttribPointer(aNor,3,GLES20.GL_FLOAT,false,32,buf);GLES20.glEnableVertexAttribArray(aNor)
        buf.position(6);GLES20.glVertexAttribPointer(aUv,2,GLES20.GL_FLOAT,false,32,buf);GLES20.glEnableVertexAttribArray(aUv)
    }
    private fun drawMesh(buf:FloatBuffer,count:Int,x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,r:Float,g:Float,b:Float,a:Float=1f,tex:Int=0,glow:Float=0f,sky:Float=0f,wind:Float=0f,minY:Float=0f){
        Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,sx,sy,sz);Matrix.multiplyMM(mvp,0,vp,0,model,0)
        GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(uModel,1,false,model,0);GLES20.glUniform4f(uColor,r,g,b,a);GLES20.glUniform1f(uGlow,glow);GLES20.glUniform1f(uUseTex,if(tex!=0)1f else 0f);GLES20.glUniform1f(uSky,sky);GLES20.glUniform1f(uWind,wind);GLES20.glUniform1f(uMinY,minY)
        if(tex!=0){GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,tex)}
        bindMesh(buf);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count)
    }
    private fun box(x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,r:Float,g:Float,b:Float,tex:Int=0,glow:Float=0f,a:Float=1f,wind:Float=0f,minY:Float=0f)=drawMesh(cube,cubeCount,x,y,z,sx,sy,sz,r,g,b,a,tex,glow,0f,wind,minY)
    private fun sph(x:Float,y:Float,z:Float,s:Float,r:Float,g:Float,b:Float,tex:Int=0,glow:Float=0f,a:Float=1f,wind:Float=0f,minY:Float=0f)=drawMesh(sphere,sphereCount,x,y,z,s,s,s,r,g,b,a,tex,glow,0f,wind,minY)
    private fun flat(x:Float,y:Float,z:Float,sx:Float,sz:Float,r:Float,g:Float,b:Float,a:Float,wind:Float=0f)=drawMesh(quad,quadCount,x,y,z,sx,1f,sz,r,g,b,a,0,0f,0f,wind,0f)

    private fun shadow(x:Float,z:Float,sx:Float,sz:Float){val gy=ground(x,z);GLES20.glDisable(GLES20.GL_CULL_FACE);flat(x,gy+.03f,z,sx,sz,0f,0f,0f,.32f);GLES20.glEnable(GLES20.GL_CULL_FACE)}

    private fun drawSky(ex:Float,ey:Float,ez:Float,night:Boolean,na:Float){
        GLES20.glDepthMask(false);GLES20.glDisable(GLES20.GL_CULL_FACE)
        fun lerp(a:Float,b:Float,f:Float)=a+(b-a)*f
        val dc=floatArrayOf(.32f,.57f,.88f);val nc=floatArrayOf(.055f,.09f,.19f);val c=floatArrayOf(lerp(dc[0],nc[0],na),lerp(dc[1],nc[1],na),lerp(dc[2],nc[2],na))
        drawMesh(sphere,sphereCount,ex,ey,ez,92f,92f,92f,c[0],c[1],c[2],1f,0,0f,1f,0f,0f)
        val ang=dayT*6.28318f;val arc=sin(ang)
        val sx=ex+cos(ang)*46f;val sy=ey+max(.12f,arc)*44f;val sz=ez-40f
        val sunArc=if(arc>=0f)1f-na else 0f
        if(sunArc>0.01f){sph(sx,sy,sz,3.0f*sunArc+.6f,1f,.82f,.45f,0,sunArc);sph(sx,sy,sz,5.5f,.9f,.85f,.5f,0,sunArc*.5f)}
        if(na>.2f){val mx=ex-cos(ang)*46f;val my=ey+max(.12f,-arc)*44f;val mz=ez-40f;sph(mx,my,mz,2.2f,.72f,.78f,1f,0,na*.8f);sph(mx,my,mz,3.4f,.5f,.6f,.8f,0,na*.35f)}
        GLES20.glEnable(GLES20.GL_CULL_FACE);GLES20.glDepthMask(true);GLES20.glUniform1f(uSky,0f)
    }
    private fun drawWater(){
        val lvl=when(world){0->-0.55f;1->-0.4f;else->-0.3f}
        if(px>34f||pz>34f)return
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        flat(0f,lvl,0f,26f,26f,.18f,.42f,.68f,.62f,2f)
        flat(0f,lvl+.02f,0f,26f,26f,.3f,.55f,.8f,.25f,2f)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
    }
    private fun drawTerrain(){
        val tint=when(world){0->floatArrayOf(.72f,.88f,.70f);1->floatArrayOf(.68f,.78f,.86f);else->floatArrayOf(.57f,.79f,.58f)}
        val b=terrain[world]?:return;drawMesh(b,terrainCount[world],0f,0f,0f,1f,1f,1f,tint[0],tint[1],tint[2],1f,texGrass,0f,0f,0f,0f)
    }
    private fun drawMainScenery(){
        val trees=arrayOf(floatArrayOf(-34f,-27f),floatArrayOf(-25f,14f),floatArrayOf(-14f,-30f),floatArrayOf(15f,-27f),floatArrayOf(31f,-18f),floatArrayOf(34f,15f),floatArrayOf(20f,30f),floatArrayOf(-25f,30f),floatArrayOf(5f,35f),floatArrayOf(-38f,2f),floatArrayOf(39f,2f),floatArrayOf(11f,18f))
        for((i,p) in trees.withIndex()){
            val gy=ground(p[0],p[1]);val h=2.7f+(i%3)*.32f
            shadow(p[0],p[1],1.6f,1.6f)
            box(p[0],gy+h*.55f,p[1],.34f,h*.55f,.34f,.82f,.72f,.58f,texLog)
            sph(p[0],gy+h+1.05f,p[1],1.45f,.46f,.76f,.42f,texLeaves,0f,1f,1f,gy)
            sph(p[0]-.85f,gy+h+.55f,p[1]+.15f,.92f,.38f,.68f,.35f,texLeaves,0f,1f,1f,gy)
            sph(p[0]+.75f,gy+h+.68f,p[1]-.18f,.9f,.42f,.72f,.38f,texLeaves,0f,1f,1f,gy)
        }
        for(i in -7..7){val z=i*3.8f;val gy=ground(0f,z);box(0f,gy+.05f,z,1.25f,.10f,1.45f,.82f,.82f,.80f,texCobble)}
        val rocks=arrayOf(floatArrayOf(-16f,8f),floatArrayOf(17f,7f),floatArrayOf(-13f,-15f),floatArrayOf(14f,-14f),floatArrayOf(-31f,-6f),floatArrayOf(29f,6f))
        for((i,p) in rocks.withIndex()){val gy=ground(p[0],p[1]);box(p[0],gy+.45f,p[1],.65f+(i%2)*.3f,.45f,.8f,.72f,.74f,.72f,texMoss)}
    }
    private fun drawImportedMap(w:Int){
        for(o in maps[w]){
            if(hypot(o.x-px,o.z-pz)>72f)continue
            val y=o.y+mapYOffset[w];val tex=when{(o.b>o.r*1.15f&&o.b>o.g*.95f)->texBlue;(o.g>o.r*1.20f&&o.g>o.b*1.05f)->texLeaves;(o.r>.42f&&o.g>.22f&&o.g<o.r*.82f)->if(o.sy>.8f)texLog else texPlank;(o.r<.28f&&o.g<.28f&&o.b<.34f)->texObsidian;(o.r>.45f&&o.g>.35f&&o.b>.30f)->texBrick;else->if(w==2)texMoss else texDeep}
            val rr=(.55f+o.r*.55f).coerceIn(.42f,1.05f);val gg=(.55f+o.g*.55f).coerceIn(.42f,1.05f);val bb=(.55f+o.b*.55f).coerceIn(.42f,1.05f)
            box(o.x,y,o.z,max(.12f,o.sx),max(.12f,o.sy),max(.12f,o.sz),rr,gg,bb,tex)
        }
        if(w==2){
            val extra=arrayOf(floatArrayOf(-20f,-20f),floatArrayOf(-30f,10f),floatArrayOf(22f,-24f),floatArrayOf(30f,16f))
            for(p in extra){val gy=ground(p[0],p[1],w);box(p[0],gy+1.3f,p[1],.28f,1.3f,.28f,.78f,.67f,.52f,texLog);sph(p[0],gy+3.2f,p[1],1.3f,.38f,.68f,.34f,texLeaves,0f,1f,1f,gy)}
        }
    }
    private fun drawGrass(){
        for(g in grassTufts[world]){val gx=g[0];val gz=g[2];if(hypot(gx-px,gz-pz)>40f)continue;val gy=ground(gx,gz);for(k in 0..2){val a=k*1.047f;box(gx+cos(a)*.12f,gy+.22f,gz+sin(a)*.12f,.05f,.44f,.05f,.36f,.72f,.30f,texGrass,0f,1f,1f,gy-.05f)}}
    }
    private fun drawRuins(){
        for((ri,r) in ruins[world].withIndex()){
            val gy=ground(r[0],r[1]);val on=if(crystals[world][ri])1f else 0f;val pulse=.35f+.25f*sin(t*2f+ri)+on*.5f
            for(k in 0 until 8){val a=k*PI.toFloat()/4f;box(r[0]+cos(a)*2.05f,gy+.18f,r[1]+sin(a)*2.05f,.42f,.16f,.72f,.78f,.82f,.84f,texMoss,pulse)}
            box(r[0],gy+.06f,r[1],1.15f,.07f,1.15f,.30f,.72f,1f,texBlue,.72f+on*.4f)
            sph(r[0],gy+1.3f,r[1],.32f,.35f,.85f,1f,0,.6f+pulse)
            if(crystals[world][ri]){for(k in 0 until 5){val a=t*1.5f+k*1.256f;sph(r[0]+cos(a)*1.1f,gy+1.3f+sin(a*.7f)*.7f,r[1]+sin(a)*1.1f,.09f,.5f,.85f,1f,0,.7f)}}
        }
    }
    private fun drawPortals(){
        fun portal(x:Float,z:Float,r:Float,g:Float,b:Float){
            val gy=ground(x,z);box(x-1.55f,gy+1.65f,z,.34f,1.65f,.45f,.72f,.74f,.78f,texBrick);box(x+1.55f,gy+1.65f,z,.34f,1.65f,.45f,.72f,.74f,.78f,texBrick);box(x,gy+3.15f,z,1.9f,.30f,.45f,.72f,.74f,.78f,texBrick)
            box(x,gy+1.65f,z,.06f,1.35f,1.25f,r,g,b,texObsidian,.95f,.82f)
            for(k in 0 until 6){val a=t*1.5f+k*PI.toFloat()/3f;sph(x+cos(a)*1.25f,gy+1.65f+sin(a*.7f)*.9f,z+.18f,.08f,r,g,b,0,1f)}
        }
        if(world==0){portal(24f,0f,.25f,.66f,1f);portal(-24f,0f,.72f,.35f,1f)}else portal(0f,18f,.35f,.75f,1f)
    }
    private fun drawHumanoid(x:Float,z:Float,friendly:Boolean,player:Boolean=false){
        val gy=if(player)py else ground(x,z)+1.05f;val body=if(player)floatArrayOf(.22f,.28f,.34f) else if(friendly)floatArrayOf(.18f,.42f,.34f) else when(world){1->floatArrayOf(.5f,.18f,.55f);2->floatArrayOf(.2f,.45f,.25f);else->floatArrayOf(.48f,.08f,.08f)}
        shadow(x,z,if(player).9f else .7f,if(player).7f else .55f)
        box(x,gy,z,.36f,.78f,.25f,body[0],body[1],body[2],if(player)texStone else 0)
        sph(x,gy+1.08f,z,.34f,.84f,.70f,.58f,0)
        box(x-.48f,gy+.05f,z,.12f,.62f,.12f,body[0]*.9f,body[1]*.9f,body[2]*.9f,0)
        box(x+.48f,gy+.05f,z,.12f,.62f,.12f,body[0]*.9f,body[1]*.9f,body[2]*.9f,0)
        box(x-.20f,gy-.92f,z,.13f,.42f,.14f,.10f,.11f,.13f,0);box(x+.20f,gy-.92f,z,.13f,.42f,.14f,.10f,.11f,.13f,0)
    }
    private fun drawMobs(){for(m in mobs[world])if(m.hp>0){drawHumanoid(m.x,m.z,m.friendly);if(m.boss)sph(m.x,ground(m.x,m.z)+2.6f,m.z,.5f,.9f,.3f,.1f,0,.6f);if(m.freeze>0)sph(m.x,ground(m.x,m.z)+1f,m.z,.7f,.55f,.8f,1f,0,.5f);if(!m.friendly){val gy=ground(m.x,m.z);val frac=(m.hp/m.maxHp).coerceIn(0f,1f);val by=if(m.boss)gy+3.4f else gy+2.1f;val bw=if(m.boss)2.2f else 1.1f;GLES20.glDisable(GLES20.GL_CULL_FACE);flat(m.x,by,m.z,bw,.16f,.1f,.1f,.55f);flat(m.x-bw*(1f-frac)*.5f,by,m.z,bw*frac,.14f,.2f,.85f,.75f);GLES20.glEnable(GLES20.GL_CULL_FACE)}}}
    private fun drawPlayer(){drawHumanoid(px,pz,true,true);if(flying||dashFx>0){for(i in 1..6){val d=i*.34f;sph(px-sin(yaw)*d,py+.55f,pz+cos(yaw)*d,.16f+i*.025f,.04f,.04f,.055f,0,.45f,.55f)}}}
    private fun drawPickups(){for(p in pickups){val gy=ground(p.x,p.z);val bob=sin(t*3f+p.x)*.15f;val col=if(p.kind==0)floatArrayOf(1f,.2f,.2f) else floatArrayOf(.3f,.6f,1f);sph(p.x,gy+.4f+bob,p.z,.18f,col[0],col[1],col[2],0,.7f+(sin(t*4f)*.5f+.5f)*.3f)}}
    private fun drawSparks(){for(s in sparks)sph(s.x,s.y,s.z,s.size,s.r,s.g,s.b,0,.9f,(s.life/.8f).coerceIn(0f,1f))}

}
