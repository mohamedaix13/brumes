package com.mumu.brumes

import android.app.Activity
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
import android.view.KeyEvent
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
    val kills:Int,
    val level:Int,
    val bossHp:Float=-1f,
    val bossName:String=""
)

class GameActivityV07 : Activity() {
    private lateinit var root:FrameLayout
    private lateinit var game:BrumesV07View
    private var menu:View?=null
    private var audio:V07Audio?=null
    private val gold=Color.rgb(228,196,126)
    private val uiScale by lazy{(minOf(resources.displayMetrics.widthPixels,resources.displayMetrics.heightPixels).toFloat()/resources.displayMetrics.density/420f).coerceIn(1f,1.5f)}
    private fun dp(v:Int)=(v*resources.displayMetrics.density*uiScale).toInt()
    private fun bar(v:Int):String{val n=((v+9)/10).coerceIn(0,10);val b=StringBuilder();for(i in 0 until 10)b.append(if(i<n)'#' else '-');return b.toString()}
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
        try{Thread.setDefaultUncaughtExceptionHandler{t,e->
            try{
                try{java.io.File(filesDir,"crash.log").writeText("Appareil : "+android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+"\nAndroid : "+android.os.Build.VERSION.RELEASE+" (API "+android.os.Build.VERSION.SDK_INT+")\nEcran : "+resources.displayMetrics.widthPixels+"x"+resources.displayMetrics.heightPixels+"\nThread : "+t.name+"\nErreur : "+e+"\n\n"+e.stackTraceToString())}catch(_:Throwable){}
                if(android.os.Looper.getMainLooper().getThread()!=Thread.currentThread()){
                    try{
                        runOnUiThread{
                            try{
                                android.app.AlertDialog.Builder(this@GameActivityV07)
                                    .setTitle("Brumes s'est arrêté")
                                    .setMessage((e.toString()+"\n\n"+e.stackTraceToString()).take(2000))
                                    .setCancelable(false)
                                    .setPositiveButton("Fermer"){_,_->try{finishAffinity()}catch(_:Throwable){};Runtime.getRuntime().exit(0)}
                                    .show()
                            }catch(_:Throwable){}
                        }
                        Thread.sleep(60000)
                    }catch(_:Throwable){}
                }
            }catch(_:Throwable){}
            Runtime.getRuntime().exit(0)
        }}catch(_:Throwable){}
        try{
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility=(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        root=FrameLayout(this).apply{isMotionEventSplittingEnabled=true}
        game=BrumesV07View(this);root.addView(game,FrameLayout.LayoutParams(-1,-1))
        audio=try{V07Audio(this).also{a->game.setSfx{n->try{if(n.length>3&&n.startsWith("amb"))a.setWorld(n[3]-'0') else if(n.length>5&&n.startsWith("spell"))a.play("spell",1f+(n.drop(5).toIntOrNull()?:0)*.03f) else a.play(n);if(n=="hit"||n=="slam")try{getSystemService(android.os.Vibrator::class.java)?.vibrate(android.os.VibrationEffect.createOneShot(45,android.os.VibrationEffect.DEFAULT_AMPLITUDE))}catch(_:Throwable){}}catch(_:Throwable){}};a.setWorld(0)}}catch(_:Throwable){null}
        try{getSharedPreferences("brumes",0).getString("state",null)?.let{game.restore(it)}}catch(_:Throwable){}

        val hud=text("BRUMES 0.11.6",10.5f).apply{background=panel(178)}
        root.addView(hud,FrameLayout.LayoutParams(dp(235),dp(76),Gravity.TOP or Gravity.START).apply{setMargins(dp(9),dp(7),0,0)})
        root.addView(button("☰",50){openMenu()},FrameLayout.LayoutParams(dp(54),dp(42),Gravity.TOP or Gravity.END).apply{setMargins(0,dp(7),dp(9),0)})

        val left=V07Stick(this){x,y->game.move(x,y)}
        root.addView(left,FrameLayout.LayoutParams(dp(122),dp(122),Gravity.BOTTOM or Gravity.START).apply{setMargins(dp(10),0,0,dp(9))})
        val right=V07Stick(this){x,y->game.look(x,y)}
        root.addView(right,FrameLayout.LayoutParams(dp(108),dp(108),Gravity.BOTTOM or Gravity.END).apply{setMargins(0,0,dp(10),dp(9))})

        val spells=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        spells.addView(button("🔥 Feu",66){game.element(0)})
        spells.addView(button("💧 Eau",66){game.element(1)})
        spells.addView(button("🌪 Air",66){game.element(2)})
        spells.addView(button("🪨 Terre",66){game.element(3)})
        spells.addView(button("⚡ Foudre",66){game.element(4)})
        spells.addView(button("🌫 Brume",66){game.element(5)})
        spells.addView(button("✨ Lumière",66){game.element(6)})
        root.addView(spells,FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,dp(45),Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply{setMargins(0,0,0,dp(8))})

        val actions=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER}
        actions.addView(button("✦ Esquive",78){game.dash()})
        actions.addView(button("☁ Vol",78){game.flight()},LinearLayout.LayoutParams(dp(78),dp(42)).apply{topMargin=dp(4)})
        actions.addView(button("◉ Ruine",78){game.fastTravel()},LinearLayout.LayoutParams(dp(78),dp(42)).apply{topMargin=dp(4)})
        root.addView(actions,FrameLayout.LayoutParams(dp(84),FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER_VERTICAL or Gravity.END).apply{setMargins(0,0,dp(11),0)})

        game.onHud={s->hud.text="BRUMES 0.11.6 · ${s.world} · ${if(s.flying)"VOL" else "SOL"}\nVie [${bar(s.life)}] ${s.life}\nMana [${bar(s.mana)}] ${s.mana} · Niv ${s.level} · ${s.kills} victoires · ${s.enemies} ennemis · ${s.spell}"+(if(s.bossHp>=0f)"\n"+s.bossName+" [${bar(s.bossHp.roundToInt().coerceIn(0,100))}]" else "")}
        setContentView(root)
        }catch(err:Throwable){
            try{
                val tv=TextView(this).apply{textSize=13f;setTextColor(0xFFF5F2EA.toInt());setPadding(60,50,60,50);text="Brumes n'a pas pu démarrer sur cet appareil.\n\nAppareil : "+android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+" ("+resources.displayMetrics.widthPixels+"x"+resources.displayMetrics.heightPixels+", Android "+android.os.Build.VERSION.RELEASE+")\n\n"+err+"\n\n"+err.stackTraceToString().take(1800)}
                setContentView(android.widget.ScrollView(this).apply{addView(tv);setBackgroundColor(0xFF070B10.toInt())})
            }catch(_:Throwable){try{finish()}catch(_:Throwable){}}
            return
        }
        val lastCrash=try{java.io.File(filesDir,"crash.log").takeIf{it.exists()}?.readText()}catch(_:Throwable){null}
        if(lastCrash!=null){
            try{java.io.File(filesDir,"crash.log").delete()}catch(_:Throwable){}
            try{android.app.AlertDialog.Builder(this).setTitle("Brumes a planté au lancement précédent").setMessage(lastCrash.take(2500)).setPositiveButton("OK"){_,_->}.show()}catch(_:Throwable){}
        }
    }
    private val keys=mutableSetOf<Int>()
    private fun applyKeys(){
        val fwd=(if(keys.contains(KeyEvent.KEYCODE_W)||keys.contains(KeyEvent.KEYCODE_Z))1f else 0f)-(if(keys.contains(KeyEvent.KEYCODE_S))1f else 0f)
        val side=(if(keys.contains(KeyEvent.KEYCODE_D))1f else 0f)-(if(keys.contains(KeyEvent.KEYCODE_A)||keys.contains(KeyEvent.KEYCODE_Q))1f else 0f)
        game.move(side,-fwd)
        val lookX=(if(keys.contains(KeyEvent.KEYCODE_DPAD_RIGHT))1f else 0f)-(if(keys.contains(KeyEvent.KEYCODE_DPAD_LEFT))1f else 0f)
        val lookY=(if(keys.contains(KeyEvent.KEYCODE_DPAD_UP))1f else 0f)-(if(keys.contains(KeyEvent.KEYCODE_DPAD_DOWN))1f else 0f)
        game.look(lookX,lookY)
    }
    override fun onKeyDown(keyCode:Int,event:KeyEvent?):Boolean{
        if(event!=null&&event.repeatCount==0){
            when(keyCode){
                KeyEvent.KEYCODE_SPACE->{game.flight();return true}
                KeyEvent.KEYCODE_SHIFT_LEFT,KeyEvent.KEYCODE_SHIFT_RIGHT->{game.dash();return true}
                KeyEvent.KEYCODE_F->{game.fastTravel();return true}
                KeyEvent.KEYCODE_ESCAPE->{if(menu==null)openMenu() else closeMenu();return true}
            }
            if(keyCode>=KeyEvent.KEYCODE_1&&keyCode<=KeyEvent.KEYCODE_7){game.element(keyCode-KeyEvent.KEYCODE_1);return true}
        }
        when(keyCode){
            KeyEvent.KEYCODE_W,KeyEvent.KEYCODE_Z,KeyEvent.KEYCODE_S,KeyEvent.KEYCODE_D,KeyEvent.KEYCODE_A,KeyEvent.KEYCODE_Q,KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT->{keys.add(keyCode);applyKeys();return true}
        }
        return super.onKeyDown(keyCode,event)
    }
    override fun onKeyUp(keyCode:Int,event:KeyEvent?):Boolean{
        if(keys.remove(keyCode)){applyKeys();return true}
        return super.onKeyUp(keyCode,event)
    }
    override fun onPause(){try{getSharedPreferences("brumes",0).edit().putString("state",game.getSnap()).apply()}catch(_:Throwable){};try{audio?.pause()}catch(_:Throwable){};game.onPause();super.onPause()}
    override fun onResume(){super.onResume();game.onResume();try{audio?.resume()}catch(_:Throwable){}}
    @Deprecated("Deprecated in Java") override fun onBackPressed(){if(menu==null)openMenu() else closeMenu()}
    private fun closeMenu(){menu?.let{root.removeView(it)};menu=null;game.setPaused(false)}
    private fun openMenu(){
        if(menu!=null)return
        game.setPaused(true)
        val shade=FrameLayout(this).apply{setBackgroundColor(0xC3000000.toInt());isClickable=true}
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(18),dp(14),dp(18),dp(14));background=panel(238)}
        body.addView(text("B R U M E S",24f).apply{gravity=Gravity.CENTER;setTextColor(gold)})
        body.addView(text("0.11.6 · AMBIANCE SONORE : chants d'oiseaux le jour, hibou la nuit, grillons au crépuscule · halo soleil/lune, étang nocturne · poussière de pas · flash rouge à l'écran quand tu prends des dégâts · 3e BOSS : la DRYADE ÉTERNELLE dans l'île forêt (cercle runique à l'est : salves d'épines, coups de racines, appelle des esprits de la forêt ; Fleur d'Éternité = +10% vitesse permanente) · projectiles visibles · âmes lumineuses des vaincus qui s'élèvent · vie sauvage : lapins dans les Brumes, cerfs dans l'île forêt · étoiles filantes la nuit · les ennemis flashent quand tu les touches · progression (vie max/mana max/niveau/victoires) affichée au menu · 2e BOSS : la SENTINELLE D'ÉTHER dans la Cité (obélisque cyan au sud, se téléporte autour de toi, salves d'éclats, Éclat d'Éther à sa mort = +10 mana max permanent jusqu'à 150) · Gardien ENRAGÉ (aura rouge, coups à 14) · Cœur du Gardien (+10 vie max) · ailes lumineuses en vol · ORAGE (éclairs + tonnerre) · feuilles automnales · pluie périodique · coffres au trésor : suis le chemin au sud jusqu'à l'autel violet pour l'invoquer (600 PV, masse 12 dégâts, projectiles violets, il revient plus fort à chaque défaite : +25% PV) · victoire = +5 victoires, vie +50, mana à fond · barre GARDIEN au HUD · difficulté progressive (niveau +1 tous les 5 kills : ennemis plus rapides et plus vifs, plafonné à +40%, reset doux à la réapparition) · panneaux indicateurs vers les portails (orbe orange = Cité d'Éther, orbe vert = Île Forêt) · feux de camp près des ruines · pont de pierre · monde 0 plus lisible · soleil doré le jour · oiseaux · étang animé · HUD barres vie/mana · brutes mondes 1 & 2 · kills (+8 mana) · sauvegarde auto · lune & étoiles · pick-up · sons · adapté à l'écran réel ("+resources.displayMetrics.widthPixels+"x"+resources.displayMetrics.heightPixels+") · sans 4K ni HDR · téléphone & tablette",11f).apply{gravity=Gravity.CENTER})
        body.addView(text("Appareil : "+android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+" · Android "+android.os.Build.VERSION.RELEASE,10f).apply{gravity=Gravity.CENTER;setTextColor(Color.rgb(178,178,170))})
        val snd=try{audio?.status()}catch(_:Throwable){null}
        body.addView(text("Sons : "+(snd?:"indisponibles"),10f).apply{gravity=Gravity.CENTER;setTextColor(Color.rgb(178,178,170))})
        body.addView(text("Progression : "+game.stats(),10f).apply{gravity=Gravity.CENTER;setTextColor(Color.rgb(178,178,170))})
        body.addView(button("REPRENDRE",205){closeMenu()},LinearLayout.LayoutParams(dp(205),dp(46)).apply{topMargin=dp(7)})
        body.addView(text("Joysticks : déplacement et caméra. Clavier : ZQSD avancer, flèches caméra, 1-7 éléments, Espace vol, Maj esquive, F ruine. Souris : maintenir clic gauche pour avancer vers le curseur, clic droit caméra, molette zoom. Deux éléments = combinaison, les portails changent de monde.",10.5f).apply{gravity=Gravity.CENTER})
        shade.addView(body,FrameLayout.LayoutParams(dp(440),FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER))
        root.addView(shade,FrameLayout.LayoutParams(-1,-1));menu=shade
    }
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
private data class V07Mob(var x:Float,var z:Float,val friendly:Boolean,var phase:Float,var hp:Float=100f,var cooldown:Float=0f,val brute:Boolean=false,var hitFx:Float=0f)
private data class V07Spark(var x:Float,var y:Float,var z:Float,var vx:Float,var vy:Float,var vz:Float,var life:Float,val r:Float,val g:Float,val b:Float,val size:Float)
private data class V07Pick(var x:Float,var z:Float,val w:Int,val kind:Int,var cd:Float)
private data class V07Shot(var x:Float,var y:Float,var z:Float,val vx:Float,val vy:Float,val vz:Float,var life:Float,val r:Float=.85f,val g:Float=.35f,val b:Float=1f)

private class BrumesV07View(activity:Activity):GLSurfaceView(activity){
    internal var onHud:((V07Hud)->Unit)?=null
    private val r=V07Renderer(activity.assets){h->post{onHud?.invoke(h)}}
    init{setEGLContextClientVersion(2);setRenderer(r);renderMode=RENDERMODE_CONTINUOUSLY}
    fun move(x:Float,y:Float){r.mx=x;r.my=y}
    fun look(x:Float,y:Float){r.lx=x;r.ly=y}
    fun element(i:Int){queueEvent{r.element(i)}}
    fun dash(){queueEvent{r.dash()}}
    fun flight(){queueEvent{r.toggleFlight()}}
    fun fastTravel(){queueEvent{r.fastTravel()}}
    fun setPaused(v:Boolean){queueEvent{r.paused=v}}
    fun setSfx(f:(String)->Unit){r.sfx=f}
    fun getSnap():String=r.snap
    fun stats():String=r.statLine()
    fun restore(sv:String){queueEvent{r.restoreState(sv)}}
    private var pinch=0f
    override fun onGenericMotionEvent(e:MotionEvent):Boolean{
        if(e.actionMasked==MotionEvent.ACTION_SCROLL){r.zoom(-e.getAxisValue(MotionEvent.AXIS_VSCROLL)*.5f);return true}
        return super.onGenericMotionEvent(e)
    }
    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.pointerCount>0&&e.getToolType(0)==MotionEvent.TOOL_TYPE_MOUSE){
            val prim=e.buttonState and MotionEvent.BUTTON_PRIMARY!=0
            val sec=e.buttonState and MotionEvent.BUTTON_SECONDARY!=0
            val dx=e.x-width/2f;val dy=e.y-height/2f;val l=max(1f,hypot(dx,dy))
            if(prim){val m=(l/(min(width,height)*.45f)).coerceAtMost(1f);r.mx=dx/l*m;r.my=dy/l*m}else{r.mx=0f;r.my=0f}
            if(sec){r.lx=dx/l*1.3f;r.ly=dy/l*1.3f}else{r.lx=0f;r.ly=0f}
            return true
        }
        if(e.pointerCount>=2){
            val d=hypot(e.getX(0)-e.getX(1),e.getY(0)-e.getY(1));if(pinch>0)r.zoom((pinch-d)/230f);pinch=d
        } else if(e.actionMasked==MotionEvent.ACTION_UP||e.actionMasked==MotionEvent.ACTION_CANCEL)pinch=0f
        return true
    
}
}

private class V07Renderer(private val assets:AssetManager,private val hud:(V07Hud)->Unit):GLSurfaceView.Renderer{
    @Volatile var mx=0f;@Volatile var my=0f;@Volatile var lx=0f;@Volatile var ly=0f;@Volatile var paused=false
    private var program=0
    private var aPos=0;private var aNor=0;private var aUv=0
    private var uMvp=0;private var uModel=0;private var uColor=0;private var uEye=0;private var uGlow=0;private var uFog=0;private var uFogColor=0;private var uUseTex=0;private var uSky=0;private var uTex=0
    private val proj=FloatArray(16);private val view=FloatArray(16);private val vp=FloatArray(16);private val model=FloatArray(16);private val mvp=FloatArray(16)
    private lateinit var cube:FloatBuffer;private var cubeCount=0
    private lateinit var sphere:FloatBuffer;private var sphereCount=0
    private val terrain=Array<FloatBuffer?>(3){null};private val terrainCount=IntArray(3)
    private val maps=Array(3){mutableListOf<V07Obj>()};private val mapYOffset=FloatArray(3)
    private val mobs=Array(3){mutableListOf<V07Mob>()};private val sparks=mutableListOf<V07Spark>()
    private val names=arrayOf("Brumes","Cité d'Éther","Île Forêt")

    private var texGrass=0;private var texDirt=0;private var texStone=0;private var texLog=0;private var texLeaves=0;private var texMoss=0;private var texCobble=0;private var texObsidian=0;private var texBlue=0;private var texSkyDay=0;private var texSkyNight=0
    private var world=0;private var px=0f;private var py=1.3f;private var pz=8f;private var yaw=0f;private var pitch=.20f;private var camZoom=10.5f;private var stepT=0f;internal var sfx:(String)->Unit={};private var nightF=0f;private val picks=java.util.ArrayList<V07Pick>();internal var snap=""
    private var flying=false;private var flyBoost=0f;private var life=100f;private var mana=100f;private var last=0L;private var t=0f;private var hudT=0f
    private var selected=-1;private var spell="Aucun";private var dashFx=0f;private var portalCooldown=0f;private var spawnGrace=6f;private var kills=0;private var level=0;private var bossOn=false;private var bossHp=0f;private var bossMax=600f;private var bossX=0f;private var bossZ=-26f;private var bossPh=0f;private var bossAtk=0f;private var bossShotT=0f;private var bossTier=0;private var bossCd=0f;private val shots=mutableListOf<V07Shot>();private var rainOn=false;private var rainT=0f;private var dropC=0;private var boltT=0f;private var boltCd=0f;private var boltX=0f;private var boltZ=0f;private val chests=java.util.ArrayList<V07Pick>();private var maxLife=100f;private var boss2On=false;private var boss2Hp=0f;private var boss2Max=550f;private var boss2X=0f;private var boss2Z=0f;private var boss2Ph=0f;private var boss2Atk=0f;private var boss2Blink=0f;private var boss2ShotT=0f;private var boss2Tier=0;private var boss2Cd=0f;private var maxMana=100f;private var meteorT=8f;private var arcT=0f;private var boss3On=false;private var boss3Hp=0f;private var boss3Max=640f;private var boss3X=0f;private var boss3Z=0f;private var boss3Ph=0f;private var boss3Atk=0f;private var boss3ShotT=0f;private var boss3Tier=0;private var boss3Cd=0f;private var boss3Call=0f;private var speedMult=1f;private var hitFx=0f;private var won1=false;private var won2=false;private var won3=false;private var masterCd=0f;private var cricketT=6f;private var petalT=0f;private var petN=0;private var hordeOn=false;private var hordeCd=0f;private val hordeMobs=mutableListOf<V07Mob>();private var foxX=1.2f;private var foxZ=8f;private var foxMove=false;private var foxYipT=9f;private var birdT=5f;private var owlT=12f
    private fun diffMult()=min(1.4f,1f+level*.08f)
    private val ruins=arrayOf(
        arrayOf(floatArrayOf(-30f,-18f),floatArrayOf(27f,23f),floatArrayOf(8f,-34f)),
        arrayOf(floatArrayOf(-28f,18f),floatArrayOf(22f,-24f),floatArrayOf(34f,8f)),
        arrayOf(floatArrayOf(-28f,-10f),floatArrayOf(12f,28f),floatArrayOf(34f,-22f))
    )
    private var ruinIndex=0
    private var campT=0f

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
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,if(repeat)GLES20.GL_REPEAT else GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,if(repeat)GLES20.GL_REPEAT else GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bmp,0);GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,0x84FE,8f);bmp.recycle();return id
    }
    private fun h2(x:Int,y:Int,s:Int):Float{var h=x*374761393+y*668265263+s*1274126177;h=(h xor (h shr 13))*1274126177;return ((h xor (h shr 16)) and 0xFFFF)/65535f}
    private fun vnoise(x:Float,y:Float,s:Int):Float{val xi=Math.floor(x.toDouble()).toInt();val yi=Math.floor(y.toDouble()).toInt();val fx=x-xi;val fy=y-yi;val u=fx*fx*(3-2*fx);val v=fy*fy*(3-2*fy);val a=h2(xi,yi,s);val b=h2(xi+1,yi,s);val c=h2(xi,yi+1,s);val d=h2(xi+1,yi+1,s);return a+(b-a)*u+(c-a)*v+(a-b-c+d)*u*v}
    private fun procTexture(lo:FloatArray,hi:FloatArray,seed:Int,size:Int=512):Int{
        val px=IntArray(size*size)
        for(yy in 0 until size){val fy=yy.toFloat()/size;for(xx in 0 until size){val fx=xx.toFloat()/size
            var f=0f;var amp=1f;var tot=0f;var sc=6f
            repeat(4){o->f+=vnoise(fx*sc+o*17.3f,fy*sc+o*9.7f,seed+o*31)*amp;tot+=amp;amp*=.52f;sc*=2.1f}
            f/=tot
            val r=((lo[0]+(hi[0]-lo[0])*f)*255f).toInt().coerceIn(0,255);val g=((lo[1]+(hi[1]-lo[1])*f)*255f).toInt().coerceIn(0,255);val b=((lo[2]+(hi[2]-lo[2])*f)*255f).toInt().coerceIn(0,255)
            px[yy*size+xx]=0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
        }}
        return uploadTexture(Bitmap.createBitmap(px,size,size,Bitmap.Config.ARGB_8888),true)
    }
    private fun texOr(name:String,lo:FloatArray,hi:FloatArray,seed:Int):Int{
        val t=try{loadAssetTexture(name,true)}catch(_:Throwable){0}
        return if(t!=0)t else procTexture(lo,hi,seed)
    }

    override fun onSurfaceCreated(gl:javax.microedition.khronos.opengles.GL10?,cfg:javax.microedition.khronos.egl.EGLConfig?){
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glEnable(GLES20.GL_BLEND);GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);GLES20.glEnable(GLES20.GL_CULL_FACE)
        val vs="""attribute vec3 a;attribute vec3 n;attribute vec2 uv;uniform mat4 m;uniform mat4 model;varying vec3 N;varying vec3 W;varying vec2 T;void main(){vec4 wp=model*vec4(a,1.0);W=wp.xyz;N=normalize(n);T=uv;gl_Position=m*vec4(a,1.0);}"""
        val fs="""precision mediump float;uniform vec4 c;uniform vec3 eye;uniform float glow;uniform float fog;uniform vec3 fogColor;uniform float useTex;uniform float sky;uniform sampler2D tex;varying vec3 N;varying vec3 W;varying vec2 T;void main(){vec4 tx=texture2D(tex,T);if(sky>0.5){gl_FragColor=vec4(mix(c.rgb,tx.rgb,useTex),c.a);return;}vec3 base=c.rgb;if(useTex>0.5){base*=mix(vec3(1.0),tx.rgb*1.28,0.55);}float d=max(dot(normalize(N),normalize(vec3(-0.35,0.82,0.42))),0.0);vec3 col=base*(0.36+d*0.76)+base*glow*0.58;float f=1.0-exp(-pow(length(W-eye)*fog,2.0));gl_FragColor=vec4(mix(col,fogColor,clamp(f,0.0,0.84)),c.a*(useTex>0.5?tx.a:1.0));}"""
        fun sh(type:Int,s:String)=GLES20.glCreateShader(type).also{GLES20.glShaderSource(it,s);GLES20.glCompileShader(it)}
        val sv=sh(GLES20.GL_VERTEX_SHADER,vs);val sf=sh(GLES20.GL_FRAGMENT_SHADER,fs);program=GLES20.glCreateProgram();GLES20.glAttachShader(program,sv);GLES20.glAttachShader(program,sf);GLES20.glLinkProgram(program);GLES20.glUseProgram(program)
        aPos=GLES20.glGetAttribLocation(program,"a");aNor=GLES20.glGetAttribLocation(program,"n");aUv=GLES20.glGetAttribLocation(program,"uv")
        uMvp=GLES20.glGetUniformLocation(program,"m");uModel=GLES20.glGetUniformLocation(program,"model");uColor=GLES20.glGetUniformLocation(program,"c");uEye=GLES20.glGetUniformLocation(program,"eye");uGlow=GLES20.glGetUniformLocation(program,"glow");uFog=GLES20.glGetUniformLocation(program,"fog");uFogColor=GLES20.glGetUniformLocation(program,"fogColor");uUseTex=GLES20.glGetUniformLocation(program,"useTex");uSky=GLES20.glGetUniformLocation(program,"sky");uTex=GLES20.glGetUniformLocation(program,"tex")
        val cm=cubeMesh();cube=fb(cm);cubeCount=cm.size/8;val sm=sphereMesh();sphere=fb(sm);sphereCount=sm.size/8
        for(w in 0..2){val tm=terrainMesh(w);terrain[w]=fb(tm);terrainCount[w]=tm.size/8}
        loadMap("world_aether.json",1);loadMap("world_forest.json",2)
        texGrass=texOr("grass_top.png",floatArrayOf(.24f,.52f,.18f),floatArrayOf(.46f,.74f,.31f),11);texDirt=texOr("dirt.png",floatArrayOf(.33f,.22f,.13f),floatArrayOf(.55f,.40f,.23f),23);texStone=texOr("stone.png",floatArrayOf(.40f,.41f,.44f),floatArrayOf(.63f,.64f,.68f),37);texLog=texOr("log_oak.png",floatArrayOf(.36f,.25f,.15f),floatArrayOf(.58f,.42f,.26f),53);texLeaves=texOr("cq_leaves.png",floatArrayOf(.13f,.38f,.12f),floatArrayOf(.31f,.60f,.24f),71);texMoss=texOr("cq_moss.png",floatArrayOf(.18f,.38f,.16f),floatArrayOf(.40f,.58f,.28f),89);texCobble=texOr("cq_cobble.png",floatArrayOf(.34f,.35f,.37f),floatArrayOf(.60f,.62f,.64f),101);texObsidian=texOr("cq_obsidian.png",floatArrayOf(.07f,.06f,.11f),floatArrayOf(.20f,.18f,.30f),113);texBlue=texOr("wool_colored_blue.png",floatArrayOf(.10f,.22f,.60f),floatArrayOf(.32f,.48f,.92f),127)
        texSkyDay=loadAssetTexture("sky_day.jpg",false);texSkyNight=loadAssetTexture("sky_night.jpg",false)
        if(picks.isEmpty())for(w in 0..2)for(i in 0 until 6){picks+=V07Pick(hsh(i,31f+w*7f)*100f-50f,hsh(i,32f+w*7f)*90f-45f,w,0,0f);picks+=V07Pick(hsh(i,33f+w*7f)*100f-50f,hsh(i,34f+w*7f)*90f-45f,w,1,0f)}
        if(chests.isEmpty())for(w in 0..2)for(i in 0 until 3)chests+=V07Pick(hsh(i*3+w,71f)*90f-45f,hsh(i*3+w,72f)*80f-40f,w,2,0f)
        seedMobs()
    }

    override fun onSurfaceChanged(gl:javax.microedition.khronos.opengles.GL10?,w:Int,h:Int){
        GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(proj,0,58f,w.toFloat()/max(1,h),.12f,190f)
    }

    private fun seedMobs(){
        val pos=arrayOf(arrayOf(floatArrayOf(-16f,-16f),floatArrayOf(16f,-18f),floatArrayOf(21f,13f),floatArrayOf(-22f,18f),floatArrayOf(4f,25f)),arrayOf(floatArrayOf(-18f,14f),floatArrayOf(18f,-14f),floatArrayOf(24f,18f),floatArrayOf(-25f,-18f),floatArrayOf(6f,28f)),arrayOf(floatArrayOf(-18f,-12f),floatArrayOf(18f,14f),floatArrayOf(26f,-16f),floatArrayOf(-24f,19f),floatArrayOf(8f,25f)))
        for(w in 0..2){for(i in pos[w].indices){val p=pos[w][i];mobs[w]+=V07Mob(p[0],p[1],i==4,i*.9f)}}
        val brutes=arrayOf(arrayOf(-9f,-26f,31f,-10f),arrayOf(-13f,-7f,15f,25f))
        for(w in 0..1){for(i in 0 until 2){mobs[w]+=V07Mob(brutes[w][i*2],brutes[w][i*2+1],false,2.6f+i*1.3f,170f,0f,true)}}
    }

    fun zoom(v:Float){camZoom=(camZoom+v).coerceIn(6.5f,16f)}
    fun statLine()=(if(won1&&won2&&won3)"★ MAÎTRE DES BRUMES ★ · " else "")+"boss : Gardien"+(if(won1)"✓" else "✗")+" · Sentinelle"+(if(won2)"✓" else "✗")+" · Dryade"+(if(won3)"✓" else "✗")+" · vie max "+maxLife.toInt()+"/150 · mana max "+maxMana.toInt()+"/150 · vitesse +"+((speedMult-1f)*100f).toInt()+"% · niveau "+level+" · "+kills+" victoires"
    fun toggleFlight(){flying=!flying;flyBoost=if(flying)7.5f else 0f;spell=if(flying)"Transplanage" else "Atterrissage";burst(px,py,pz,.12f,.12f,.16f,18);sfx("flight")}
    fun dash(){
        val f=-my;val s=mx;var dx=sin(yaw)*f+cos(yaw)*s;var dz=-cos(yaw)*f+sin(yaw)*s
        if(hypot(dx,dz)<.18f){dx=sin(yaw);dz=-cos(yaw)};val l=max(.01f,hypot(dx,dz));dx/=l;dz/=l
        px=(px+dx*7.2f).coerceIn(-58f,58f);pz=(pz+dz*7.2f).coerceIn(-58f,58f);dashFx=.55f;burst(px,py,pz,.04f,.04f,.05f,24);sfx("dash")
    }
    fun fastTravel(){val r=ruins[world][ruinIndex%3];px=r[0];pz=r[1];py=ground(px,pz)+1.3f;ruinIndex++;spell="Ruine ${ruinIndex%3+1}";sfx("portal")}
    fun element(i:Int){
        val names=arrayOf("Feu","Eau","Air","Terre","Foudre","Brume","Lumière")
        if(selected<0){selected=i;spell="${names[i]} prêt";return}
        if(selected==i){spell=names[i];cast(i,i);selected=-1;return}
        val a=min(selected,i);val b=max(selected,i)
        spell=when(a*10+b){1->"Vapeur";2->"Tempête de braises";3->"Lave";12->"Givre";13->"Boue entravante";23->"Tempête de sable";4->"Orage de flammes";5->"Fumée aveuglante";6->"Aube ardente";14->"Tempête électrique";15->"Brume glacée";16->"Eau lumineuse";24->"Cyclone chargé";25->"Nuée cendreuse";26->"Rayon zénithal";34->"Séisme fulgurant";35->"Poussière voilée";36->"Éclat tellurique";45->"Brume électrisée";46->"Éclair sacré";56->"Voile radieux";else->"Arcane"}
        cast(selected,i);selected=-1
    }
    private fun cast(a:Int,b:Int){
        if(mana<8)return;mana=max(0f,mana-8f);sfx("spell"+min(3,level))
        val col=when{a==0&&b==0->floatArrayOf(1f,.22f,.05f);a==1&&b==1->floatArrayOf(.08f,.55f,1f);a==2&&b==2->floatArrayOf(.72f,.85f,1f);a==3&&b==3->floatArrayOf(.7f,.48f,.25f);setOf(a,b)==setOf(0,3)->floatArrayOf(1f,.24f,.03f);setOf(a,b)==setOf(1,2)->floatArrayOf(.55f,.9f,1f);a==4&&b==4->floatArrayOf(1f,.92f,.2f);a==5&&b==5->floatArrayOf(.6f,.63f,.68f);a==6&&b==6->floatArrayOf(1f,.96f,.8f);else->floatArrayOf(.85f,.7f,.95f)}
        burst(px+sin(yaw)*2f,py+.7f,pz-cos(yaw)*2f,col[0],col[1],col[2],36)
        val foudre=(a==4||b==4);val brume=(a==5||b==5);val lumiere=(a==6||b==6)
        for(m in mobs[world])if(!m.friendly){val d=hypot(m.x-px,m.z-pz)
            val touch=if(foudre)d<20f&&d>.1f&&((m.x-px)*sin(yaw)-(m.z-pz)*cos(yaw))/d>.45f else d<8.5f
            if(touch){m.hitFx=.3f;m.hp-=when{foudre->46f;brume->20f;lumiere->12f;else->34f};if(brume)m.cooldown=2.5f;if(m.hp<=0){mana=min(maxMana,mana+8f);kills++;level=kills/5;sfx("kill");sparks+=V07Spark(m.x,ground(m.x,m.z)+1.2f,m.z,0f,1.5f,0f,1.5f,.78f,.88f,1f,.085f)}}}
        if(boss3On&&world==2){val d=hypot(boss3X-px,boss3Z-pz);val touch=if(foudre)d<24f else d<11f;if(touch){boss3Hp-=when{foudre->50f;brume->24f;lumiere->16f;else->36f};burst(boss3X,ground(boss3X,boss3Z,2)+2f,boss3Z,col[0],col[1],col[2],12)}}
        if(boss2On&&world==1){val d=hypot(boss2X-px,boss2Z-pz);val touch=if(foudre)d<24f else d<11f;if(touch){boss2Hp-=when{foudre->48f;brume->24f;lumiere->16f;else->35f};burst(boss2X,ground(boss2X,boss2Z,1)+2f,boss2Z,col[0],col[1],col[2],12)}}
        if(bossOn&&world==0){val d=hypot(bossX-px,bossZ-pz);val touch=if(foudre)d<24f else d<10.5f;if(touch){bossHp-=when{foudre->52f;brume->26f;lumiere->14f;else->38f};burst(bossX,ground(bossX,bossZ)+2.2f,bossZ,col[0],col[1],col[2],12)}}
        if(lumiere)life=min(maxLife,life+18f)
    }
    private fun burst(x:Float,y:Float,z:Float,r:Float,g:Float,b:Float,count:Int){
        repeat(count){val a=(it*2.399f+t);val sp=.8f+(it%5)*.18f;sparks+=V07Spark(x,y,z,cos(a)*sp,((it%7)-2)*.22f,sin(a)*sp,.65f+(it%4)*.08f,r,g,b,.07f+(it%3)*.025f)}
        while(sparks.size>120)sparks.removeAt(0)
    }

    private fun update(dt:Float){
        if(paused)return;t+=dt;portalCooldown=max(0f,portalCooldown-dt);spawnGrace=max(0f,spawnGrace-dt);mana=min(maxMana,mana+5.5f*dt)
        yaw+=lx*1.85f*dt;pitch=(pitch-ly*.8f*dt).coerceIn(-.18f,.62f)
        val f=-my;val s=mx;val dx=sin(yaw)*f+cos(yaw)*s;val dz=-cos(yaw)*f+sin(yaw)*s;val speed=(if(flying)7.4f else 5.4f)*speedMult
        px=(px+dx*speed*dt).coerceIn(-60f,60f);pz=(pz+dz*speed*dt).coerceIn(-60f,60f)
        if(flying){flyBoost=max(0f,flyBoost-14f*dt);py+=( -ly*5.3f + flyBoost)*dt;py=py.coerceIn(1.1f,34f)}else py=ground(px,pz)+1.25f
        if(dashFx>0)dashFx=max(0f,dashFx-dt);hitFx=max(0f,hitFx-dt);if(!flying&&(f!=0f||s!=0f)){stepT+=dt;if(stepT>.42f){stepT=0f;sfx("step");val dgy=ground(px,pz)+.06f;val dsn=(t*13f).toInt();sparks+=V07Spark(px-sin(yaw)*.3f,dgy,pz+cos(yaw)*.3f,(hsh(dsn,15f)-.5f)*.35f,.55f,(hsh(dsn,16f)-.5f)*.35f,.55f,.55f,.50f,.42f,.055f)}}else stepT=0f
        val iter=sparks.iterator();while(iter.hasNext()){val s0=iter.next();s0.x+=s0.vx*dt;s0.y+=s0.vy*dt;s0.z+=s0.vz*dt;s0.vy+=.18f*dt;s0.life-=dt;if(s0.life<=0)iter.remove()}
        campT+=dt
        if(world==0&&campT>1.15f){campT=0f;for(r in ruins[0]){val fx=r[0]+3.4f;val fz=r[1]+2.8f;val fy=ground(fx,fz)+.55f;repeat(3){k->val a=t*2f+k*2.4f;sparks+=V07Spark(fx+sin(a)*.10f,fy,fz+cos(a)*.10f,cos(a)*.28f,.8f+hsh(k,42f)*.9f,sin(a)*.28f,.7f+hsh(k,44f)*.4f,1f,.55f+hsh(k,45f)*.25f,.12f,.055f)};sparks+=V07Spark(fx,fy,fz,(hsh(kills,46f)-.5f)*.15f,.75f,(hsh(kills,47f)-.5f)*.15f,2.4f,.42f,.42f,.47f,.13f);while(sparks.size>140)sparks.removeAt(0)}}
        for(m in mobs[world]){
            if(m.hp<=0){m.x=(sin(m.phase*4.1f)*26f);m.z=(cos(m.phase*3.7f)*24f);m.hp=if(m.brute)170f else 100f}
            m.phase+=dt*(if(m.friendly).38f else if(m.brute).3f else .62f);m.cooldown=max(0f,m.cooldown-dt);m.hitFx=max(0f,m.hitFx-dt)
            val d=hypot(px-m.x,pz-m.z)
            if(!m.friendly&&d<10f&&spawnGrace<=0){val sp=(if(m.brute).72f else 1.35f)*diffMult();val ux=(px-m.x)/max(.1f,d);val uz=(pz-m.z)/max(.1f,d);m.x+=ux*sp*dt;m.z+=uz*sp*dt;if(d<(if(m.brute)2.6f else 2.15f)&&m.cooldown<=0){life-=(if(m.brute)6f else 3.5f);hitFx=.35f;m.cooldown=(if(m.brute)1.6f else 1.25f)/diffMult();burst(px,py+.5f,pz,.65f,.05f,.05f,7);sfx("hit")}}
            else{m.x+=cos(m.phase)*.34f*dt;m.z+=sin(m.phase*.91f)*.34f*dt}
        }
        bossCd=max(0f,bossCd-dt)
        if(world==0&&!bossOn&&bossCd<=0f&&hypot(px,pz+18f)<2.9f){bossOn=true;bossMax=600f*(1f+bossTier*.25f);bossHp=bossMax;bossX=0f;bossZ=-26f;bossPh=0f;bossAtk=2f;bossShotT=2.5f;spell="LE GARDIEN S'ÉVEILLE";sfx("portal");sfx("slam");burst(0f,ground(0f,-26f)+1.6f,-26f,.45f,.15f,1f,42)}
        if(bossOn&&world==0){
            bossPh+=dt;bossAtk=max(0f,bossAtk-dt);bossShotT=max(0f,bossShotT-dt)
            val bd=hypot(px-bossX,pz-bossZ)
            if(bd>3.1f){val sp=(if(bossHp<bossMax*.3f)1.35f else 1.05f)*(1f+bossTier*.06f);bossX+=(px-bossX)/max(.1f,bd)*sp*dt;bossZ+=(pz-bossZ)/max(.1f,bd)*sp*dt}
            if(bd<3.4f&&bossAtk<=0f&&spawnGrace<=0f){life-=if(bossHp<bossMax*.3f)14f else 12f;hitFx=.35f;bossAtk=if(bossHp<bossMax*.3f)1.8f else 2.3f;sfx("slam");burst(px,py+.4f,pz,.5f,.08f,.5f,16)}
            if(bossShotT<=0f&&bd>3.6f&&bd<30f){bossShotT=if(bossHp<bossMax*.3f)2.2f else 3.1f;val sy2=ground(bossX,bossZ)+2.6f;val tx=px-bossX;val ty=py+.8f-sy2;val tz=pz-bossZ;val l2=max(.1f,sqrt(tx*tx+ty*ty+tz*tz));shots+=V07Shot(bossX,sy2,bossZ,tx/l2*10.5f,ty/l2*10.5f,tz/l2*10.5f,4f,.85f,.30f,1f);sfx("spell3")}
            if(bossHp<=0f){bossOn=false;won1=true;bossTier++;bossCd=90f;kills+=5;level=kills/5;mana=maxMana;life=min(maxLife,life+50f);chests+=V07Pick(bossX,bossZ,0,3,0f);spell="Gardien vaincu !";sfx("kill");sfx("portal");sparks+=V07Spark(bossX,ground(bossX,bossZ)+1.5f,bossZ,0f,1.8f,0f,1.6f,.95f,.45f,.6f,.12f);repeat(3){k->burst(bossX+k*1.1f,ground(bossX,bossZ)+1.2f+k*.7f,bossZ,.5f,.3f,1f,24)}}
        }
        boss2Cd=max(0f,boss2Cd-dt)
        if(world==1&&!boss2On&&boss2Cd<=0f&&hypot(px,pz+14f)<2.6f){boss2On=true;boss2Max=550f*(1f+boss2Tier*.25f);boss2Hp=boss2Max;boss2X=0f;boss2Z=-22f;boss2Ph=0f;boss2Atk=2f;boss2Blink=3.5f;boss2ShotT=2f;spell="LA SENTINELLE S'ÉVEILLE";sfx("portal");burst(0f,ground(0f,-22f,1)+1.6f,-22f,.3f,.8f,1f,42)}
        if(boss2On&&world==1){
            boss2Ph+=dt;boss2Atk=max(0f,boss2Atk-dt);boss2Blink=max(0f,boss2Blink-dt);boss2ShotT=max(0f,boss2ShotT-dt)
            val en2=boss2Hp<boss2Max*.35f
            val d2=hypot(px-boss2X,pz-boss2Z)
            if(d2<2.6f&&boss2Atk<=0f&&spawnGrace<=0f){life-=if(en2)13f else 10f;hitFx=.35f;boss2Atk=2f;sfx("hit");burst(px,py+.5f,pz,.3f,.7f,1f,14)}
            if(boss2Blink<=0f&&d2>3f){boss2Blink=if(en2)2.5f else 4f;val ba=hsh(dropC,81f)*6.2832f;val nx=px+cos(ba)*6.5f;val nz=pz+sin(ba)*6.5f;burst(boss2X,ground(boss2X,boss2Z,1)+1.5f,boss2Z,.3f,.8f,1f,16);boss2X=nx;boss2Z=nz;dropC++;burst(nx,ground(nx,nz,1)+1.5f,nz,.3f,.8f,1f,16);sfx("portal")}
            if(boss2ShotT<=0f&&d2>2.6f&&d2<32f){boss2ShotT=if(en2)2.2f else 2.8f;val sy3=ground(boss2X,boss2Z,1)+2.2f;val nsh=if(en2)5 else 3;for(k in 0 until nsh){val sp2=9.5f;val ang=atan2(pz-boss2Z,px-boss2X)+((k-(nsh-1)/2f)*.22f);shots+=V07Shot(boss2X,sy3,boss2Z,cos(ang)*sp2,(py+.8f-sy3)*.55f,sin(ang)*sp2,3.5f,.35f,.85f,1f)};sfx("spell1")}
            if(boss2Hp<=0f){boss2On=false;won2=true;boss2Tier++;boss2Cd=90f;kills+=5;level=kills/5;mana=maxMana;life=min(maxLife,life+50f);chests+=V07Pick(boss2X,boss2Z,1,4,0f);spell="Sentinelle vaincue !";sfx("kill");sfx("portal");sparks+=V07Spark(boss2X,ground(boss2X,boss2Z,1)+1.5f,boss2Z,0f,1.8f,0f,1.6f,.45f,.85f,1f,.12f);repeat(3){k->burst(boss2X+k*1.1f,ground(boss2X,boss2Z,1)+1.2f+k*.7f,boss2Z,.3f,.8f,1f,24)}}
        }
        boss3Cd=max(0f,boss3Cd-dt)
        if(world==2&&!boss3On&&boss3Cd<=0f&&hypot(px-20f,pz-10f)<2.6f){boss3On=true;boss3Max=640f*(1f+boss3Tier*.25f);boss3Hp=boss3Max;boss3X=20f;boss3Z=-6f;boss3Ph=0f;boss3Atk=2f;boss3ShotT=2f;boss3Call=0f;spell="LA DRYADE S'ÉVEILLE";sfx("portal");burst(20f,ground(20f,-6f,2)+1.6f,-6f,.3f,1f,.5f,42)}
        if(boss3On&&world==2){
            boss3Ph+=dt;boss3Atk=max(0f,boss3Atk-dt);boss3ShotT=max(0f,boss3ShotT-dt)
            val en3=boss3Hp<boss3Max*.3f
            val d3=hypot(px-boss3X,pz-boss3Z)
            if(d3>3.0f){val sp=(if(en3)1.0f else .8f)*(1f+boss3Tier*.05f);boss3X+=(px-boss3X)/max(.1f,d3)*sp*dt;boss3Z+=(pz-boss3Z)/max(.1f,d3)*sp*dt}
            if(d3<3.2f&&boss3Atk<=0f&&spawnGrace<=0f){life-=if(en3)14f else 11f;hitFx=.35f;boss3Atk=if(en3)2f else 2.4f;sfx("slam");burst(px,py+.4f,pz,.3f,1f,.4f,16);burst(boss3X,ground(boss3X,boss3Z,2)+.4f,boss3Z,.25f,.7f,.2f,14)}
            if(boss3ShotT<=0f&&d3>3.2f&&d3<30f){boss3ShotT=if(en3)2.1f else 3f;val sy4=ground(boss3X,boss3Z,2)+2.4f;val nsh3=if(en3)5 else 3;for(k in 0 until nsh3){val ang3=atan2(pz-boss3Z,px-boss3X)+((k-(nsh3-1)/2f)*.24f);shots+=V07Shot(boss3X,sy4,boss3Z,cos(ang3)*9f,(py+.8f-sy4)*.5f,sin(ang3)*9f,3.5f,.35f,1f,.45f)};sfx("spell2")}
            if(boss3Hp<boss3Max*.6f&&boss3Call<=0f){boss3Call=1f;for(k in 0 until 2){mobs[2]+=V07Mob(boss3X+(k*2-1)*1.5f,boss3Z+2f,false,2.6f+k,60f,0f)};sfx("portal");burst(boss3X,ground(boss3X,boss3Z,2)+1f,boss3Z,.4f,1f,.5f,20)}
            if(boss3Hp<=0f){boss3On=false;won3=true;boss3Tier++;boss3Cd=90f;kills+=5;level=kills/5;mana=maxMana;life=min(maxLife,life+50f);chests+=V07Pick(boss3X,boss3Z,2,5,0f);spell=if(won1&&won2)"MAÎTRE DES BRUMES !" else "Dryade vaincue !";sfx("kill");sfx("portal");sparks+=V07Spark(boss3X,ground(boss3X,boss3Z,2)+1.5f,boss3Z,0f,1.8f,0f,1.6f,.5f,1f,.5f,.12f);repeat(3){k->burst(boss3X+k*1.1f,ground(boss3X,boss3Z,2)+1.2f+k*.7f,boss3Z,.4f,1f,.5f,24)}}
        }
        if(world==2)shots.clear()
        val si=shots.iterator();while(si.hasNext()){val s0=si.next();s0.x+=s0.vx*dt;s0.y+=s0.vy*dt;s0.z+=s0.vz*dt;s0.life-=dt;val gy2=ground(s0.x,s0.z);if(s0.life<=0f||s0.y<gy2+.15f){burst(s0.x,s0.y,s0.z,.5f,.2f,1f,10);si.remove()}else if(hypot(px-s0.x,pz-s0.z)<1.3f&&s0.y>py-1f&&s0.y<py+2.4f){life-=6f;hitFx=.3f;burst(s0.x,s0.y,s0.z,.6f,.1f,.5f,12);sfx("hit");si.remove()}}
        if(life<=0){life=maxLife;mana=max(mana,60f);px=0f;pz=8f;py=ground(px,pz)+1.25f;spawnGrace=6f;kills/=2;level=kills/5;spell="Réapparition"}
        if(won1&&won2&&won3&&world==0&&masterCd<=0f&&hypot(px,pz-8f)<2.3f){masterCd=20f;life=maxLife;mana=maxMana;spell="Énergie du Maître restaurée !";sfx("portal");burst(px,py+1f,pz,1f,.85f,.35f,26)}
        cricketT-=dt;if(cricketT<=0f){cricketT=3f+hsh((t*7f).toInt(),7f)*4f;if(nightF>.5f&&world==0)sfx("cricket")};birdT-=dt;if(birdT<=0f){birdT=6f+hsh((t*5f).toInt(),17f)*8f;if(nightF<.35f&&world==0)sfx("birds")};owlT-=dt;if(owlT<=0f){owlT=14f+hsh((t*3f).toInt(),18f)*12f;if(nightF>.6f)sfx("owl")};if(world==2){petalT-=dt;if(petalT<=0f){petalT=.45f;petN++;sparks+=V07Spark(px+(hsh(petN,8f)-.5f)*30f,py+5.5f+(hsh(petN,9f)-.5f)*4f,pz+(hsh(petN,10f)-.5f)*30f,(hsh(petN,11f)-.5f)*.4f,-.45f,(hsh(petN,12f)-.5f)*.4f,5f,.98f,.72f,.80f,.09f)}}
        if(hordeCd>0f)hordeCd-=dt;if(!hordeOn&&hordeCd<=0f&&world==0&&!bossOn&&hypot(px,pz+18f)<2.4f){hordeOn=true;spell="CERCLE DES ÂMES !";sfx("slam");for(k in 0 until 8){val hm=V07Mob(cos(k*.785f)*6f,-18f+sin(k*.785f)*6f,false,k*.8f,55f+level*5f);mobs[0]+=hm;hordeMobs+=hm};burst(px,py+1f,pz,.7f,.3f,1f,24)};if(hordeOn&&hordeMobs.isNotEmpty()&&hordeMobs.all{it.hp<=0}){hordeOn=false;hordeCd=150f;hordeMobs.clear();life=min(maxLife,life+50f);mana=maxMana;kills+=3;level=kills/5;spell="Cercle des âmes vaincu !";sfx("chime");burst(px,py+1f,pz,1f,.9f,.4f,30)}
        val fdx=px-foxX;val fdz=pz-foxZ;val fd=hypot(fdx,fdz);foxMove=fd>2.1f;if(fd>38f){foxX=px-1.2f;foxZ=pz}else if(fd>2.1f){foxX+=fdx/fd*min(fd-1.6f,5.5f)*dt;foxZ+=fdz/fd*min(fd-1.6f,5.5f)*dt};foxYipT-=dt;if(foxYipT<=0f){foxYipT=8f+hsh((t*9f).toInt(),14f)*9f;if(fd<14f)sfx("yip")}
        for(p in picks){if(p.cd>0f){p.cd-=dt}else if(p.w==world&&hypot(px-p.x,pz-p.z)<1.15f){if(p.kind==0)life=min(maxLife,life+25f) else mana=min(100f,mana+40f);p.cd=60f;sfx("chime");burst(p.x,ground(p.x,p.z)+1f,p.z,if(p.kind==0).95f else .35f,if(p.kind==0).4f else .6f,1f,14)}}
        rainT+=dt;rainOn=world==0&&rainT%75f>55f;arcT=max(0f,arcT-dt);if(world==0&&rainT%75f<2f&&arcT<=0f)arcT=18f
        meteorT-=dt;if(meteorT<=0f){meteorT=9f+hsh(dropC,87f)*14f;val ma=hsh(dropC,88f)*6.2832f;dropC++;val mx0=px+cos(ma)*60f;val mz0=pz+sin(ma)*60f;repeat(8){k->sparks+=V07Spark(mx0+cos(ma)*1.4f*k,py+30f-1.4f*k,mz0+sin(ma)*1.4f*k,-cos(ma)*16f,-7f,-sin(ma)*16f,.8f,1f,.97f,.85f,.08f)};while(sparks.size>150)sparks.removeAt(0)}
        if(rainOn){boltCd-=dt;boltT=max(0f,boltT-dt);if(boltCd<=0f){boltCd=3.5f+hsh(dropC,95f)*7f;boltT=.32f;boltX=px+(hsh(dropC,96f)-.5f)*70f;boltZ=pz+(hsh(dropC,97f)-.5f)*70f;dropC++;sfx("thunder");burst(boltX,ground(boltX,boltZ)+1f,boltZ,.7f,.75f,1f,18);if(hypot(px-boltX,pz-boltZ)<3.5f){life-=8f;hitFx=.4f;burst(px,py+.6f,pz,.9f,.9f,1f,12)}}}else{boltT=0f}
        if(rainOn)repeat(2){val rx=px+(hsh(dropC,91f)-.5f)*46f;val rz=pz+(hsh(dropC,92f)-.5f)*46f;dropC++;sparks+=V07Spark(rx,py+9f+hsh(dropC,93f)*5f,rz,0f,-12f,0f,.85f,.55f,.65f,.9f,.045f);while(sparks.size>150)sparks.removeAt(0)}
        for(p in chests){if(p.cd>0f){p.cd-=dt}else if(p.w==world&&hypot(px-p.x,pz-p.z)<1.45f){if(p.kind==3){maxLife=min(150f,maxLife+10f);life=min(life+30f,maxLife);p.cd=999999f;spell="Cœur du Gardien !"}else if(p.kind==4){maxMana=min(150f,maxMana+10f);mana=min(mana+30f,maxMana);p.cd=999999f;spell="Éclat d'Éther !"}else if(p.kind==5){speedMult=min(1.3f,speedMult+.1f);p.cd=999999f;spell="Fleur d'Éternité !"}else{life=min(maxLife,life+40f);mana=min(maxMana,mana+40f);p.cd=120f;spell="Trésor !"};sfx("chime");burst(p.x,ground(p.x,p.z)+.8f,p.z,.95f,.8f,.25f,20)}}
        checkPortals()
        hudT+=dt;if(hudT>.15f){hudT=0f;hud(V07Hud(life.roundToInt().coerceIn(0,150),mana.roundToInt().coerceIn(0,150),names[world],flying,mobs[world].count{!it.friendly&&it.hp>0},spell,kills,level,if(bossOn)bossHp/bossMax*100f else if(boss2On)boss2Hp/boss2Max*100f else if(boss3On)boss3Hp/boss3Max*100f else -1f,if(bossOn)"GARDIEN" else if(boss2On)"SENTINELLE" else if(boss3On)"DRYADE" else ""))}
        snap=""+world+"|"+px+"|"+py+"|"+pz+"|"+life+"|"+mana+"|"+flying+"|"+bossTier+"|"+maxLife+"|"+maxMana+"|"+speedMult+"|"+won1+"|"+won2+"|"+won3
    }

    private fun checkPortals(){
        if(portalCooldown>0)return
        if(world==0){
            if(hypot(px-24f,pz)<2.7f){world=1;px=0f;pz=8f;py=ground(px,pz)+1.25f;portalCooldown=2.2f;spawnGrace=4f;spell="Cité d'Éther";sfx("portal");sfx("amb"+world)}
            else if(hypot(px+24f,pz)<2.7f){world=2;px=0f;pz=8f;py=ground(px,pz)+1.25f;portalCooldown=2.2f;spawnGrace=4f;spell="Île Forêt";sfx("portal");sfx("amb"+world)}
        }else if(hypot(px,pz-18f)<2.8f){world=0;px=0f;pz=8f;py=ground(px,pz)+1.25f;portalCooldown=2.2f;spawnGrace=4f;spell="Brumes";sfx("portal");sfx("amb"+world)}
    }

    override fun onDrawFrame(gl:javax.microedition.khronos.opengles.GL10?){
        val now=System.nanoTime();val dt=if(last==0L)0f else min(.033f,(now-last)/1_000_000_000f);last=now;update(dt)
        val phase=(sin(t*.022f)+1f)*.5f;val night=phase<.22f;nightF=((.45f-phase)/.45f).coerceIn(0f,1f)
        val clear=when(world){0->if(night)floatArrayOf(.025f,.045f,.085f) else floatArrayOf(.16f,.24f,.32f);1->if(night)floatArrayOf(.03f,.04f,.10f) else floatArrayOf(.18f,.25f,.36f);else->if(night)floatArrayOf(.02f,.055f,.065f) else floatArrayOf(.13f,.24f,.22f)}
        val boltF=min(1f,boltT*3.2f);GLES20.glClearColor(clear[0]+.50f*boltF,clear[1]+.55f*boltF,clear[2]+.62f*boltF,1f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT);GLES20.glUseProgram(program)
        val eyeY=py+2.4f+sin(pitch)*3.2f;val ex=px-sin(yaw)*cos(pitch)*camZoom;val ez=pz+cos(yaw)*cos(pitch)*camZoom
        Matrix.setLookAtM(view,0,ex,eyeY,ez,px,py+.75f,pz,0f,1f,0f);Matrix.multiplyMM(vp,0,proj,0,view,0)
        GLES20.glUniform3f(uEye,ex,eyeY,ez);GLES20.glUniform1f(uFog,if(world==1).0072f else if(rainOn).0092f else .0060f)
        val fcd=when(world){0->floatArrayOf(.64f,.72f,.79f);1->floatArrayOf(.60f,.66f,.84f);else->floatArrayOf(.62f,.77f,.64f)};val fcn=when(world){0->floatArrayOf(.15f,.19f,.27f);1->floatArrayOf(.17f,.20f,.32f);else->floatArrayOf(.13f,.19f,.23f)};GLES20.glUniform3f(uFogColor,fcd[0]+(fcn[0]-fcd[0])*nightF,fcd[1]+(fcn[1]-fcd[1])*nightF,fcd[2]+(fcn[2]-fcd[2])*nightF);GLES20.glUniform1i(uTex,0)

        drawSky(ex,eyeY,ez,texSkyDay);if(nightF>.02f)drawSky(ex,eyeY,ez,texSkyNight,nightF);if(nightF>.03f&&nightF<.97f){GLES20.glDepthMask(false);GLES20.glDisable(GLES20.GL_CULL_FACE);drawMesh(sphere,sphereCount,ex,eyeY,ez,91f,91f,91f,1f,.58f,.30f,4f*nightF*(1f-nightF)*.30f,0,0f,0f);GLES20.glEnable(GLES20.GL_CULL_FACE);GLES20.glDepthMask(true)}
        if(boltT>0f){val fl=boltT/.32f;val bgy=ground(boltX,boltZ);box(boltX,bgy+26f,boltZ,.14f,26f,.14f,1f,1f,.92f,0,fl*1.4f);sph(boltX,bgy+.8f,boltZ,1.4f+fl*.8f,.8f,.85f,1f,0,fl)}
        if(arcT>0f&&world==0){val az=2.4f;val arcF=(arcT.coerceAtMost(3f)/3f);for(k in 0 until 7){val cv=when(k){0->floatArrayOf(1f,.30f,.25f);1->floatArrayOf(1f,.55f,.15f);2->floatArrayOf(1f,.85f,.2f);3->floatArrayOf(.30f,.85f,.30f);4->floatArrayOf(.30f,.55f,1f);5->floatArrayOf(.45f,.35f,.8f);else->floatArrayOf(.70f,.30f,.85f)};for(j in 0 until 9){val th=.22f+j*1.06f/8f;val rr=66f-k*1.2f;sph(px+sin(az)*cos(th)*rr,py+5f+sin(th)*rr*.8f,pz+cos(az)*cos(th)*rr,.55f,cv[0],cv[1],cv[2],0,.32f,.26f*arcF)}}}
        if(nightF>.02f){
            for(i in 0 until 46){val sa=hsh(i,21f)*6.2832f;val el=.18f+hsh(i,22f)*.55f;val d=86f;sph(ex+cos(sa)*cos(el)*d,eyeY+sin(el)*d*.75f+6f,ez+sin(sa)*cos(el)*d,.22f+hsh(i,23f)*.2f,1f,.98f,.92f,0,1f,nightF*(.45f+hsh(i,24f)*.55f))};for(k in 0 until 7){val cx=ex+cos(.65f+k*.145f)*72f;val cz=ez+sin(.65f+k*.145f)*72f;val cy=eyeY+34f+k*1.5f;sph(cx,cy,cz,.28f,1f,.95f,.80f,0,1f,nightF*(.65f+.30f*sin(t*1.4f+k*2f)))};sph(ex+cos(1.85f)*80f,eyeY+52f,ez+sin(1.85f)*80f,.42f,1f,.98f,.88f,0,1f,nightF)
            sph(ex+sin(t*.016f)*64f,eyeY+46f,ez+cos(t*.016f)*64f,6.4f,.92f,.90f,.80f,0,.22f,nightF*.12f);sph(ex+sin(t*.016f)*64f,eyeY+46f,ez+cos(t*.016f)*64f,3.6f,.96f,.94f,.84f,0,.7f,nightF)
        }
        if(nightF<.98f){
            val dayF=1f-nightF
            val sunX=ex-sin(t*.013f+.9f)*58f;val sunY=eyeY+38f;val sunZ=ez-cos(t*.013f+.9f)*58f
            sph(sunX,sunY,sunZ,13.5f,1f,.80f,.30f,0,.16f,dayF*.14f)
            sph(sunX,sunY,sunZ,7.6f,1f,.84f,.36f,0,.30f,dayF*.5f)
            sph(sunX,sunY,sunZ,3.4f,1f,.93f,.55f,0,.95f,dayF)
        }
        drawTerrain()
        if(world==0)drawMainScenery() else drawImportedMap(world)
        drawRuins();drawPortals();drawPickups();drawMobs();drawBoss();drawBoss2();drawBoss3();drawShots();drawPlayer();drawFox();drawGuide();drawFireflies();drawMasterBeacon();drawSoulRune();drawSparks()
        if(hitFx>0f){GLES20.glDepthMask(false);drawMesh(sphere,sphereCount,ex,eyeY,ez,93f,93f,93f,.78f,.05f,.05f,(hitFx/.35f)*.30f,0,0f,1f);GLES20.glDepthMask(true)}
    }

    private fun drawGuide(){val txr=when(world){0->if(!won1&&bossCd<=0f&&!bossOn)floatArrayOf(0f,-18f) else null;1->if(!won2&&boss2Cd<=0f&&!boss2On)floatArrayOf(0f,-14f) else null;2->if(!won3&&boss3Cd<=0f&&!boss3On)floatArrayOf(20f,10f) else null;else->null}?:return;val gdx=txr[0]-px;val gdz=txr[1]-pz;val gd=hypot(gdx,gdz);if(gd<3f)return;val gx=px+gdx/gd*2.2f;val gz=pz+gdz/gd*2.2f;val gy=py+1.9f+sin(t*2.2f)*.12f;sph(gx,gy,gz,.07f+sin(t*3f)*.01f,1f,.9f,.55f,0,1f,.85f);sph(gx+gdx/gd*.25f,gy,gz+gdz/gd*.25f,.05f,1f,.95f,.7f,0,1f,.6f)}
    private fun drawFox(){val fgy=ground(foxX,foxZ);val bob=if(foxMove)abs(sin(t*7f))*.16f else sin(t*1.8f)*.02f;drawMesh(sphere,sphereCount,foxX,fgy+.02f,foxZ,.30f,.05f,.30f,.05f,.05f,.08f,.22f,0,0f,0f);if(foxMove){sph(foxX,fgy+.34f+bob,foxZ,.17f,.85f,.48f,.16f,0,.10f,.98f)}else{sph(foxX,fgy+.24f+bob,foxZ,.185f,.85f,.48f,.16f,0,.10f,.98f)};val hx=foxX+(px-foxX)*.09f;val hz=foxZ+(pz-foxZ)*.09f;val hy=if(foxMove)fgy+.52f+bob else fgy+.40f+bob;sph(hx,hy,hz,.115f,.88f,.55f,.25f,0,.10f,.98f);sph(hx-.06f,hy+.11f,hz,.035f,.7f,.35f,.12f,0,.15f,.98f);sph(hx+.06f,hy+.11f,hz,.035f,.7f,.35f,.12f,0,.15f,.98f);val tx=foxX-(px-foxX)*.05f;val tz=foxZ-(pz-foxZ)*.05f;if(foxMove){sph(tx+sin(t*6f)*.03f,fgy+.36f+bob,tz,.09f,.85f,.48f,.16f,0,.15f,.98f)}else{sph(tx,fgy+.22f+bob,tz+.14f,.10f,.85f,.48f,.16f,0,.15f,.98f)};sph(foxX,fgy+.26f+bob,foxZ,.13f,.95f,.92f,.85f,0,0f,.95f)}
    private fun drawFireflies(){val dayF=1f-nightF;if(nightF>.06f){for(i in 0 until 10){val fx=px+(hsh(i,61f)-.5f)*26f;val fz=pz+(hsh(i,62f)-.5f)*26f;val fy=ground(fx,fz)+1.1f+hsh(i,63f)*1.6f+sin(t*.7f+i*2.1f)*.22f;val bl=.35f+.65f*(.5f+.5f*sin(t*2.4f+i*1.9f));sph(fx+sin(t*.5f+i)*.5f,fy,fz+cos(t*.4f+i)*.5f,.055f,.98f,.9f,.35f,0,1f,nightF*bl)}};if(dayF>.4f){for(i in 0 until 6){val bfx=px+(hsh(i,64f)-.5f)*22f;val bfz=pz+(hsh(i,65f)-.5f)*22f;val bfy=ground(bfx,bfz)+1.1f+hsh(i,66f)*1.3f+sin(t*1.1f+i*1.7f)*.3f;val wx=.10f*sin(t*15f+i);val wr=if(i%2==0)1f else .98f;val wg=if(i%2==0)1f else .72f;sph(bfx-wx,bfy,bfz,.05f,wr,wg,.86f,0,.25f,dayF*.95f);sph(bfx+wx,bfy,bfz,.05f,wr,wg,.86f,0,.25f,dayF*.95f)}}}
    private fun drawMasterBeacon(){if(!won1||!won2||!won3||world!=0)return;val bgy=ground(0f,8f);val gl=.55f+.35f*sin(t*2f);drawMesh(sphere,sphereCount,0f,bgy+7f,8f,.85f,7f,.85f,1f,.86f,.4f,.30f,0,gl*.55f,0f);if(masterCd>0f)masterCd-=.016f;for(k in 0 until 6){val ma=t*1.2f+k*1.047f;sph(sin(ma)*1.3f,bgy+2.4f+sin(t*2f+k)*.35f,8f+cos(ma)*1.3f,.10f,1f,.9f,.5f,0,.95f)}}
    private fun drawSoulRune(){if(world!=0||hordeOn||hordeCd>0f||bossOn)return;val rgy=ground(0f,-18f);sph(0f,rgy+.09f,-18f,1.4f,.62f,.28f,.98f,0,.45f+.30f*sin(t*2.5f),.55f)}
    private fun bindMesh(buf:FloatBuffer){
        buf.position(0);GLES20.glVertexAttribPointer(aPos,3,GLES20.GL_FLOAT,false,32,buf);GLES20.glEnableVertexAttribArray(aPos)
        buf.position(3);GLES20.glVertexAttribPointer(aNor,3,GLES20.GL_FLOAT,false,32,buf);GLES20.glEnableVertexAttribArray(aNor)
        buf.position(6);GLES20.glVertexAttribPointer(aUv,2,GLES20.GL_FLOAT,false,32,buf);GLES20.glEnableVertexAttribArray(aUv)
    }
    private fun drawMesh(buf:FloatBuffer,count:Int,x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,r:Float,g:Float,b:Float,a:Float=1f,tex:Int=0,glow:Float=0f,sky:Float=0f){
        Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,sx,sy,sz);Matrix.multiplyMM(mvp,0,vp,0,model,0)
        GLES20.glUniformMatrix4fv(uMvp,1,false,mvp,0);GLES20.glUniformMatrix4fv(uModel,1,false,model,0);GLES20.glUniform4f(uColor,r,g,b,a);GLES20.glUniform1f(uGlow,glow);GLES20.glUniform1f(uUseTex,if(tex!=0)1f else 0f);GLES20.glUniform1f(uSky,sky)
        if(tex!=0){GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,tex)}
        bindMesh(buf);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count)
    }
    private fun box(x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,r:Float,g:Float,b:Float,tex:Int=0,glow:Float=0f,a:Float=1f)=drawMesh(cube,cubeCount,x,y,z,sx,sy,sz,r,g,b,a,tex,glow,0f)
    private fun sph(x:Float,y:Float,z:Float,s:Float,r:Float,g:Float,b:Float,tex:Int=0,glow:Float=0f,a:Float=1f)=drawMesh(sphere,sphereCount,x,y,z,s,s,s,r,g,b,a,tex,glow,0f)
    private fun hsh(i:Int,s:Float):Float{val v=sin(i*127.1f+s*311.7f)*43758.5453f;return v-floor(v)}

    private fun drawSky(ex:Float,ey:Float,ez:Float,tex:Int,a:Float=1f){
        GLES20.glDepthMask(false);GLES20.glDisable(GLES20.GL_CULL_FACE)
        drawMesh(sphere,sphereCount,ex,ey,ez,92f,92f,92f,.05f+.37f*(1f-nightF),.07f+.53f*(1f-nightF),.13f+.65f*(1f-nightF),a,tex,0f,1f)
        GLES20.glEnable(GLES20.GL_CULL_FACE);GLES20.glDepthMask(true);GLES20.glUniform1f(uSky,0f)
    }
    private fun drawTerrain(){
        val tint=when(world){0->floatArrayOf(.72f,.88f,.70f);1->floatArrayOf(.68f,.78f,.86f);else->floatArrayOf(.57f,.79f,.58f)}
        val b=terrain[world]?:return;drawMesh(b,terrainCount[world],0f,0f,0f,1f,1f,1f,tint[0],tint[1],tint[2],1f,texGrass,0f,0f)
    }
    private fun drawSignposts(){
        val orange=floatArrayOf(1f,.55f,.18f);val vert=floatArrayOf(.30f,.85f,.35f)
        fun post(x:Float,z:Float,c:FloatArray){
            val gy=ground(x,z)
            box(x,gy+.85f,z,.14f,.85f,.14f,.62f,.50f,.36f,texLog)
            box(x,gy+1.62f,z,.52f,.34f,.16f,.74f,.76f,.80f,texCobble)
            sph(x,gy+1.62f,z,.20f,c[0],c[1],c[2],0,.85f+.25f*sin(t*2.4f+x))
            sph(x,gy+1.62f,z,.09f,1f,1f,.92f,0,1f)
        }
        post(6f,2.2f,orange);post(12f,1.4f,orange);post(18f,.6f,orange)
        post(-6f,2.2f,vert);post(-12f,1.4f,vert);post(-18f,.6f,vert)
    }
    private fun drawCampfires(){
        for((ri,r) in ruins[0].withIndex()){
            val fx=r[0]+3.4f;val fz=r[1]+2.8f;val gy=ground(fx,fz)
            for(k in 0 until 5){val a=k*PI.toFloat()*2f/5f;sph(fx+cos(a)*.55f,gy+.12f,fz+sin(a)*.55f,.19f,.42f,.40f,.38f,texStone)}
            for(k in 0 until 3){sph(fx+(k-1)*.14f,gy+.16f,fz+(k%2)*.12f,.09f,.30f,.20f,.12f,texLog)}
            if(nightF>.05f)for(k in 0 until 2){val ma=t*2.6f+k*3.1f;val mr=.25f+.18f*sin(t*1.3f+k*2f);sph(fx+cos(ma)*mr,gy+.75f+sin(t*3.5f+k*2.1f)*.28f,fz+sin(ma)*mr,.045f,.95f,.85f,.55f,0,.9f,nightF)}
            val fl=.9f+.35f*sin(t*7f+ri*2.1f)
            sph(fx,gy+.45f,fz,.30f,1f,.52f,.10f,0,fl)
            sph(fx,gy+.62f,fz,.17f,1f,.80f,.30f,0,fl)
            sph(fx,gy+.38f,fz,.75f,1f,.45f,.08f,0,.30f,.18f)
        }
    }
    private fun drawBridge(){
        val z=12f;val deckY=1.15f
        for(i in 0 until 8){val bx=-3f+i*1.8f;val gy=ground(bx,z);box(bx,gy+deckY,z,.95f,.22f,2.2f,.74f,.75f,.73f,texCobble);if(i%2==0){box(bx,gy+.45f,z,.34f,.5f,.34f,.66f,.68f,.70f,texCobble)}}
        for(i in 0 until 9){val rx=-3.8f+i*1.8f;val gy=ground(rx,z);box(rx,gy+deckY+.52f,z-1.02f,.16f,.30f,.16f,.70f,.72f,.74f,texCobble);box(rx,gy+deckY+.52f,z+1.02f,.16f,.30f,.16f,.70f,.72f,.74f,texCobble)}
    }
    private fun drawAltar(){
        val ax=0f;val az=-18f;val gy=ground(ax,az)
        val active=!bossOn&&bossCd<=0f
        val glow=if(active).65f+.35f*sin(t*2.2f) else .10f
        box(ax,gy+.06f,az,2.6f,.10f,2.6f,.16f,.13f,.22f,texObsidian,glow)
        for(k in 0 until 6){val a=k*PI.toFloat()/3f;val pxp=ax+cos(a)*2.1f;val pzp=az+sin(a)*2.1f;box(pxp,gy+.75f,pzp,.24f,.75f,.24f,.20f,.18f,.30f,texObsidian,glow*.5f);sph(pxp,gy+1.56f,pzp,.11f,.62f,.30f,1f,0,glow)}
        sph(ax,gy+.55f,az,.42f,.62f,.25f,1f,0,glow)
        for(k in 0 until 3){val ra=t*1.3f+k*2.09f;sph(ax+cos(ra)*.7f,gy+.85f+sin(t*2f+k)*.25f,az+sin(ra)*.7f,.06f,.75f,.40f,1f,0,glow*.8f)}
    }
    private fun drawBoss(){
        if(!bossOn||world!=0)return;drawMesh(sphere,sphereCount,bossX,ground(bossX,bossZ)+.02f,bossZ,1.1f,.08f,1.1f,.05f,.05f,.08f,.35f,0,0f,0f)
        val gy=ground(bossX,bossZ)+1.05f
        val y=gy+sin(bossPh*2f)*.10f
        sph(bossX,gy+.30f,bossZ,2.7f,if(bossHp<bossMax*.3f).72f else .42f,.08f,if(bossHp<bossMax*.3f).14f else .48f,0,if(bossHp<bossMax*.3f).55f+.18f*sin(t*6f) else .30f,.20f)
        box(bossX,y+1.5f,bossZ,1.25f,1.9f,.85f,.16f,.03f,.10f,texObsidian)
        sph(bossX,y+3.1f,bossZ,.62f,.24f,.12f,.30f,texObsidian)
        box(bossX-.34f,y+3.72f,bossZ,.11f,.55f,.11f,.75f,.68f,.60f)
        box(bossX+.34f,y+3.72f,bossZ,.11f,.55f,.11f,.75f,.68f,.60f)
        sph(bossX-.20f,y+3.15f,bossZ+.36f,.09f,1f,.20f,.12f,0,.9f+.2f*sin(t*5f))
        sph(bossX+.20f,y+3.15f,bossZ+.36f,.09f,1f,.20f,.12f,0,.9f+.2f*sin(t*5f))
        box(bossX-1.28f,y+.9f,bossZ,.30f,1.35f,.30f,.14f,.03f,.09f)
        box(bossX+1.28f,y+.9f,bossZ,.30f,1.35f,.30f,.14f,.03f,.09f)
        val swing=if(bossAtk>1.9f).5f else 0f
        box(bossX+1.28f,y+2.3f+swing*.6f,bossZ,.26f,1.1f,.26f,.30f,.26f,.24f,texStone,swing*.8f)
        box(bossX-.45f,y-.55f,bossZ,.34f,.95f,.38f,.10f,.02f,.08f)
        box(bossX+.45f,y-.55f,bossZ,.34f,.95f,.38f,.10f,.02f,.08f)
        sph(bossX,gy+2.2f,bossZ,1.5f,.55f,.12f,.55f,0,.22f,.15f)
    }
    private fun drawBoss2(){
        if(!boss2On||world!=1)return;drawMesh(sphere,sphereCount,boss2X,ground(boss2X,boss2Z,1)+.02f,boss2Z,1.1f,.08f,1.1f,.05f,.05f,.08f,.35f,0,0f,0f)
        val gy=ground(boss2X,boss2Z,1)
        val y=gy+1.5f+sin(boss2Ph*1.6f)*.16f
        val en=boss2Hp<boss2Max*.35f
        sph(boss2X,gy+.35f,boss2Z,2.2f,if(en).85f else .25f,.30f,if(en).20f else .85f,0,.35f,.22f)
        box(boss2X,y+.6f,boss2Z,.85f,1.45f,.55f,.05f,.10f,.14f,texObsidian)
        sph(boss2X,y+1.75f,boss2Z,.42f,.10f,.16f,.20f,texObsidian)
        sph(boss2X-.13f,y+1.80f,boss2Z+.30f,.07f,if(en)1f else .45f,if(en).45f else .95f,1f,0,1f)
        sph(boss2X+.13f,y+1.80f,boss2Z+.30f,.07f,if(en)1f else .45f,if(en).45f else .95f,1f,0,1f)
        for(k in 0 until 3){val oa=t*1.6f+k*2.094f;sph(boss2X+cos(oa)*1.15f,y+1.1f+sin(t*2f+k)*.25f,boss2Z+sin(oa)*1.15f,.14f,.45f,.85f,1f,0,.9f)}
        box(boss2X,y+.55f,boss2Z,1.15f,.18f,.60f,.03f,.06f,.09f,0,.5f,.7f)
    }
    private fun drawMainScenery(){
        val trees=arrayOf(floatArrayOf(-34f,-27f),floatArrayOf(-25f,14f),floatArrayOf(-14f,-30f),floatArrayOf(15f,-27f),floatArrayOf(31f,-18f),floatArrayOf(34f,15f),floatArrayOf(20f,30f),floatArrayOf(-25f,30f),floatArrayOf(5f,35f),floatArrayOf(-38f,2f),floatArrayOf(39f,2f),floatArrayOf(11f,18f))
        for((i,p) in trees.withIndex()){
            val gy=ground(p[0],p[1]);val h=2.7f+(i%3)*.32f;val swx=sin(t*1.1f+i*1.7f)*.16f;val swz=cos(t*.9f+i*1.1f)*.13f
            box(p[0],gy+h*.55f,p[1],.34f,h*.55f,.34f,.82f,.72f,.58f,texLog)
            sph(p[0]+swx,gy+h+1.05f+sin(t*1.4f+i)*.05f,p[1]+swz,1.45f,.46f,.76f,.42f,texLeaves)
            sph(p[0]-.85f+swx*.7f,gy+h+.55f,p[1]+.15f+swz*.7f,.92f,.38f,.68f,.35f,texLeaves)
            sph(p[0]+.75f+swx*.8f,gy+h+.68f,p[1]-.18f+swz*.8f,.9f,.42f,.72f,.38f,texLeaves)
        }
        for(i in -7..7){val z=i*3.8f;val gy=ground(0f,z);box(0f,gy+.05f,z,1.25f,.10f,1.45f,.82f,.82f,.80f,texCobble)}
        val rocks=arrayOf(floatArrayOf(-16f,8f),floatArrayOf(17f,7f),floatArrayOf(-13f,-15f),floatArrayOf(14f,-14f),floatArrayOf(-31f,-6f),floatArrayOf(29f,6f))
        for((i,p) in rocks.withIndex()){val gy=ground(p[0],p[1]);sph(p[0],gy+.34f,p[1],.62f+(i%2)*.28f,.60f,.63f,.60f,texMoss);sph(p[0]+.95f,gy+.16f,p[1]+.4f,.34f,.55f,.58f,.55f,texMoss)}
        val fcol=arrayOf(floatArrayOf(.95f,.28f,.22f),floatArrayOf(.98f,.85f,.30f),floatArrayOf(.96f,.96f,.90f),floatArrayOf(.72f,.42f,.92f))
        for(i in 0 until 46){val x=hsh(i,1f)*116f-58f;val z=hsh(i,2f)*104f-52f;if(abs(x)<2.4f)continue;val gy=ground(x,z);val c=fcol[i%4];val fs=sin(t*1.8f+i*2.2f)*.05f;box(x,gy+.14f,z,.045f,.15f,.045f,.30f,.55f,.24f);sph(x+fs,gy+.31f,z+fs*.6f,.115f,c[0],c[1],c[2]);sph(x+fs,gy+.31f,z+fs*.6f,.05f,.98f,.88f,.35f,0,.9f)}
        for(i in 0 until 34){val x=hsh(i,3f)*110f-55f;val z=hsh(i,4f)*98f-49f;val gy=ground(x,z);val gs=sin(t*1.5f+i*1.9f)*.06f;sph(x+gs,gy+.13f,z,.17f,.26f,.52f,.20f,texGrass);sph(x+.22f+gs,gy+.09f,z+.12f,.12f,.30f,.56f,.24f,texGrass)}
        for(i in 0 until 9){val x=hsh(i,5f)*100f-50f;val z=hsh(i,6f)*90f-45f;if(abs(x)<2.4f)continue;val gy=ground(x,z);sph(x,gy+.32f,z,.52f,.34f,.62f,.30f,texLeaves);sph(x+.5f,gy+.22f,z+.3f,.36f,.30f,.56f,.27f,texLeaves)}
        if(nightF>.02f)for(i in 0 until 16){val bx=hsh(i,7f)*80f-40f;val bz=hsh(i,8f)*70f-35f;val by=ground(bx,bz)+1.1f+sin(t*1.3f+i*2.1f)*.5f;sph(bx+sin(t*.7f+i)*1.4f,by,bz+cos(t*.6f+i*1.7f)*1.4f,.06f,1f,.93f,.55f,0,1f,nightF)}
        val birdA=1f-nightF*.85f
        if(birdA>.05f)for(i in 0 until 5){val ba=t*.3f+i*1.2566f;val bxc=sin(ba)*27f+3f;val bzc=cos(ba)*23f-3f;val byc=9.6f+sin(t*1.8f+i*2f)*.6f;sph(bxc,byc,bzc,.16f,.92f,.55f,.22f,0,0f,birdA);sph(bxc+.13f,byc+.02f,bzc,.10f,.72f,.68f,.62f,0,0f,birdA)}
        drawSignposts();drawCampfires();drawBridge();drawAltar()
        for(i in 0 until 3){val ra=t*.35f+i*2.09f;val rxx=sin(ra)*24f+i*9f-9f;val rzz=cos(ra*.7f)*20f-4f;val rgy=ground(rxx,rzz);val hop=abs(sin(t*3.5f+i*1.3f))*.35f;box(rxx,rgy+.16f+hop,rzz,.22f,.18f,.30f,.82f,.78f,.70f);sph(rxx,rgy+.32f+hop,rzz+.16f,.13f,.80f,.76f,.68f);sph(rxx,rgy+.46f+hop,rzz+.30f,.08f,1f,.95f,.85f)}
    }
    private fun drawImportedMap(w:Int){
        for(o in maps[w]){
            val y=o.y+mapYOffset[w];val tex=when{(o.b>o.r*1.15f&&o.b>o.g*.95f)->texBlue;(o.g>o.r*1.20f&&o.g>o.b*1.05f)->texLeaves;(o.r>.42f&&o.g>.22f&&o.g<o.r*.82f)->texLog;(o.r<.28f&&o.g<.28f&&o.b<.34f)->texObsidian;else->if(w==2)texMoss else texStone}
            val rr=(.55f+o.r*.55f).coerceIn(.42f,1.05f);val gg=(.55f+o.g*.55f).coerceIn(.42f,1.05f);val bb=(.55f+o.b*.55f).coerceIn(.42f,1.05f)
            box(o.x,y,o.z,max(.12f,o.sx),max(.12f,o.sy),max(.12f,o.sz),rr,gg,bb,tex)
        }
        if(w==1){
            for(i in 0 until 12){val x=hsh(i,11f)*90f-45f;val z=hsh(i,12f)*80f-40f;val y=ground(x,z,w)+2.2f+sin(t*1.1f+i*1.9f)*.9f;sph(x,y,z,.28f+(i%3)*.09f,.42f,.56f,.95f,texObsidian,.55f)}
            for(i in 0 until 8){val x=hsh(i,13f)*70f-35f;val z=hsh(i,14f)*60f-30f;val y=ground(x,z,w)+1.6f+sin(t*.8f+i*2.7f)*.7f;sph(x,y,z,.07f,.55f,.75f,1f,0,1f)}
            for(i in 0 until 6){val x=hsh(i,17f)*100f-50f;val z=hsh(i,18f)*90f-45f;val gy=ground(x,z,w);val hh=1.1f+hsh(i,19f)*1.6f;box(x,gy+hh*.5f,z,.30f,hh*.5f,.30f,.70f,.74f,.82f,texStone);sph(x,gy+hh+.25f,z,.22f,.30f,.75f,1f,0,.6f+.4f*sin(t*1.7f+i))}
            for(i in 0 until 7){val a=t*.45f+i*.897f;val x=sin(a)*32f+5f;val z=cos(a*1.2f)*26f-4f;val y=ground(x,z,w)+3.4f+sin(t*.9f+i*1.6f)*1.1f;sph(x,y,z,.09f+hsh(i,20f)*.06f,.85f,.62f,1f,0,.8f)}
            for(k in 0 until 4){val pxx=hsh(k,31f)*44f-22f;val pzz=hsh(k,32f)*40f-20f;val near=hypot(px-pxx,pz-pzz)<5.5f;val pgy=ground(pxx,pzz,1);if(near){val pfy=pgy+2.6f+sin(t*.8f+k)*.6f;val flap=.10f+.14f*abs(sin(t*22f+k*1.3f));sph(pxx,pfy,pzz,.09f,.55f,.55f,.62f,0,.15f,.98f);sph(pxx-.12f,pfy,pzz,flap,.72f,.74f,.78f,0,.2f,.95f);sph(pxx+.12f,pfy,pzz,flap,.72f,.74f,.78f,0,.2f,.95f)}else{val peck=abs(sin(t*3f+k*2f))*.05f;sph(pxx,pgy+.16f,pzz,.09f,.55f,.55f,.62f,0,.15f,.98f);sph(pxx+.05f,pgy+.26f+peck,pzz-.04f,.055f,.5f,.5f,.58f,0,.2f,.98f);sph(pxx-.05f,pgy+.27f+peck,pzz-.03f,.03f,1f,.75f,.4f,0,.4f,.98f);sph(pxx+.05f,pgy+.27f+peck,pzz-.03f,.03f,1f,.75f,.4f,0,.4f,.98f)}}
            val obx=0f;val obz=-14f;val oby=ground(obx,obz,1);val obOn=!boss2On&&boss2Cd<=0f;val obGlow=if(obOn).65f+.35f*sin(t*2f) else .10f
            box(obx,oby+1.2f,obz,.5f,1.2f,.5f,.16f,.16f,.22f,texObsidian,obGlow*.6f)
            box(obx,oby+2.45f,obz,.14f,1.3f,.14f,.20f,.20f,.30f,texObsidian,obGlow)
            sph(obx,oby+3.3f,obz,.18f,.45f,.85f,1f,0,obGlow)
        }
        if(w==2){
            val extra=arrayOf(floatArrayOf(-20f,-20f),floatArrayOf(-30f,10f),floatArrayOf(22f,-24f),floatArrayOf(30f,16f),floatArrayOf(-36f,-14f),floatArrayOf(8f,-30f))
            val pgy=ground(-7f,-9f,2)
            box(-7f,pgy+.05f,-9f,6.3f,.05f,5.1f,.10f,.25f,.17f,texMoss)
            val wob=.74f+.07f*sin(t*1.5f)
            box(-7f,pgy+.17f,-9f,5.6f,.06f+.02f*sin(t*1.5f),4.4f+.14f*sin(t*1.1f),.20f*(1f-nightF*.62f),.52f*(1f-nightF*.45f),.82f*(1f-nightF*.25f),0,0f,wob)
            for(k in 0 until 3){val ra=t*.9f+k*2.09f;sph(-7f+cos(ra)*3.3f,pgy+.24f+.04f*sin(t*2.1f+k),-9f+sin(ra)*2.5f,.09f,.5f,.78f,1f,0,.75f)}
            for(k in 0 until 3){val fa=t*1.1f+k*2.09f;val fxx=-7f+cos(fa)*4.6f;val fzz=-9f+sin(fa)*3.8f;val fgy2=ground(fxx,fzz,2);val hop2=abs(sin(t*4.2f+k*1.7f))*.24f;sph(fxx,fgy2+.17f+hop2,fzz,.16f,.30f,.62f,.25f,0,.15f,.98f);sph(fxx,fgy2+.25f+hop2,fzz-.11f,.10f,.35f,.68f,.30f,0,.15f,.98f);sph(fxx-.06f,fgy2+.31f+hop2,fzz-.17f,.034f,.95f,.9f,.2f,0,.5f,.98f);sph(fxx+.06f,fgy2+.31f+hop2,fzz-.17f,.034f,.95f,.9f,.2f,0,.5f,.98f)}
            for(k in 0 until 8){val mx2=hsh(k,33f)*90f-45f;val mz2=hsh(k,34f)*80f-40f;val mgy=ground(mx2,mz2,w);val mp=.5f+.5f*sin(t*1.6f+k*1.9f);sph(mx2,mgy+.14f,mz2,.07f,.82f,.76f,.68f,0,0f,.98f);sph(mx2,mgy+.27f,mz2,.15f,.75f,.28f,.34f,0,.12f+.55f*nightF*mp,.98f)}
            for(p in extra){val gy=ground(p[0],p[1],w);box(p[0],gy+1.3f,p[1],.28f,1.3f,.28f,.78f,.67f,.52f,texLog);sph(p[0],gy+3.2f,p[1],1.3f,.38f,.68f,.34f,texLeaves)}
            for(i in 0 until 14){val x=hsh(i,15f)*90f-45f;val z=hsh(i,16f)*80f-40f;val gy=ground(x,z,w);box(x,gy+.17f,z,.08f,.18f,.08f,.88f,.83f,.72f);sph(x,gy+.38f,z,.21f,.82f,.16f,.13f)}
            for(i in 14 until 22){val x=hsh(i,23f)*96f-48f;val z=hsh(i,24f)*86f-43f;val gy=ground(x,z,w);val cc=arrayOf(floatArrayOf(.88f,.30f,.24f),floatArrayOf(.90f,.72f,.22f),floatArrayOf(.80f,.44f,.80f))[i%3];box(x,gy+.12f,z,.06f,.13f,.06f,.92f,.88f,.78f);sph(x,gy+.28f,z,.16f+hsh(i,25f)*.09f,cc[0],cc[1],cc[2],0,.35f)}
            for(i in 0 until 6){val x=sin(t*.5f+i*1.1f)*24f+i*3.5f-9f;val z=cos(t*.4f+i*.9f)*22f;val y=ground(x,z,w)+1.2f+sin(t*2.2f+i)*.4f;sph(x,y,z,.05f,.98f,.80f,.25f,0,1f);sph(x+.09f,y,z,.04f,.98f,.80f,.25f,0,1f)}
            for(i in 0 until 12){val lx=sin(t*.23f+i*2.1f)*22f+i*5f-30f;val lz=cos(t*.31f+i*.9f)*20f-5f;val gy3=ground(lx,lz,2);val frac=(t*.42f+i*.131f)%1f;box(lx,gy3+4.2f-frac*4.2f,lz,.17f,.03f,.11f,.58f,.40f,.16f,texLeaves)}
            for(i in 0 until 2){val da=t*.13f+i*3.1f;val dxx=sin(da)*26f+i*14f-7f;val dzz=cos(da*.8f)*22f-3f;val dgy=ground(dxx,dzz,2);val bob2=sin(t*2.2f+i)*.05f;box(dxx,dgy+.75f+bob2,dzz,.85f,.55f,.40f,.62f,.44f,.28f,texLog);box(dxx,dgy+1.25f+bob2,dzz-.55f,.16f,.48f,.16f,.60f,.42f,.26f,texLog);sph(dxx,dgy+1.55f+bob2,dzz-.62f,.17f,.64f,.48f,.30f,texLog);box(dxx-.18f,dgy+1.75f+bob2,dzz-.62f,.04f,.22f,.04f,.55f,.40f,.25f);box(dxx+.18f,dgy+1.75f+bob2,dzz-.62f,.04f,.22f,.04f,.55f,.40f,.25f);for(k2 in 0 until 4){box(dxx-.28f+((k2%2)*.56f),dgy+.3f+bob2,dzz+.15f-(k2/2)*.9f,.09f,.42f,.09f,.55f,.38f,.24f,texLog)}}
            val rcx=20f;val rcz=10f;val rcy=ground(rcx,rcz,2);val rcOn=!boss3On&&boss3Cd<=0f;val rcGlow=if(rcOn).6f+.35f*sin(t*2f) else .10f
            box(rcx,rcy+.06f,rcz,2.4f,.10f,2.4f,.13f,.22f,.13f,texMoss,rcGlow)
            for(k in 0 until 6){val rca=k*PI.toFloat()/3f;val rsx=rcx+cos(rca)*2f;val rsz=rcz+sin(rca)*2f;val rgy2=ground(rsx,rsz,2);sph(rsx,rgy2+.35f,rsz,.20f,.30f,.30f,.28f,texStone);sph(rsx,rgy2+.75f,rsz,.08f,.45f,1f,.5f,0,rcGlow)}
        }
    }
    internal fun restoreState(sv:String){try{val p=sv.split("|");if(p.size<7)return;world=p[0].toInt().coerceIn(0,2);px=p[1].toFloat().coerceIn(-60f,60f);py=p[2].toFloat().coerceIn(1.1f,34f);pz=p[3].toFloat().coerceIn(-60f,60f);life=p[4].toFloat().coerceIn(1f,100f);mana=p[5].toFloat().coerceIn(0f,150f);flying=p[6].toBoolean();if(p.size>=8)bossTier=(p[7].toIntOrNull()?:0).coerceIn(0,20);if(p.size>=9)maxLife=p[8].toFloatOrNull()?.coerceIn(100f,150f)?:100f;if(p.size>=10)maxMana=p[9].toFloatOrNull()?.coerceIn(100f,150f)?:100f;if(p.size>=11)speedMult=p[10].toFloatOrNull()?.coerceIn(1f,1.3f)?:1f;if(p.size>=12)won1=p[11]=="true";if(p.size>=13)won2=p[12]=="true";if(p.size>=14)won3=p[13]=="true";spawnGrace=5f;portalCooldown=2f;spell="Partie restaur\u00e9e";sfx("amb"+world)}catch(_:Throwable){}}
    private fun drawPickups(){for((i,p) in picks.withIndex()){if(p.w!=world||p.cd>0f)continue;val gy=ground(p.x,p.z);val y=gy+.55f+sin(t*2f+i)*.16f;if(p.kind==0)sph(p.x,y,p.z,.24f,.96f,.32f,.40f,0,.85f) else sph(p.x,y,p.z,.24f,.30f,.55f,1f,0,.85f);sph(p.x,y+.36f,p.z,.08f,1f,1f,.85f,0,.9f)}
        for((i,p) in chests.withIndex()){if(p.w!=world||p.cd>0f)continue;val gy=ground(p.x,p.z);if(p.kind==3){val hy=gy+.75f+sin(t*2f+i)*.18f;sph(p.x-.13f,hy+.08f,p.z,.17f,.92f,.12f,.22f,0,.85f);sph(p.x+.13f,hy+.08f,p.z,.17f,.92f,.12f,.22f,0,.85f);box(p.x,hy-.14f,p.z,.20f,.26f,.18f,.92f,.12f,.22f,0,.85f);sph(p.x,hy+.25f,p.z,.06f,1f,.9f,.6f,0,1f)}else if(p.kind==4){val hy=gy+.75f+sin(t*2f+i)*.18f;box(p.x,hy+.12f,p.z,.16f,.34f,.16f,.35f,.85f,1f,0,.85f);sph(p.x,hy+.42f,p.z,.09f,.6f,.95f,1f,0,1f);sph(p.x,hy-.14f,p.z,.09f,.6f,.95f,1f,0,1f)}else if(p.kind==5){val hy=gy+.75f+sin(t*2f+i)*.18f;box(p.x,gy+.25f,p.z,.06f,.45f,.06f,.35f,.70f,.30f);for(k in 0 until 5){val pa=k*1.2566f;sph(p.x+cos(pa)*.15f,hy+.05f,p.z+sin(pa)*.15f,.11f,1f,.85f,.3f,0,.7f)};sph(p.x,hy+.12f,p.z,.08f,1f,.55f,.25f,0,.9f)}else{box(p.x,gy+.30f,p.z,.55f,.28f,.40f,.62f,.44f,.15f,texLog,.25f+.15f*sin(t*2f+i));box(p.x,gy+.56f,p.z,.60f,.12f,.45f,.78f,.60f,.20f,texLog,.5f);sph(p.x,gy+.70f,p.z,.07f,.98f,.85f,.35f,0,.8f+.2f*sin(t*3f+i))}}}
    private fun drawRuins(){
        for((ri,r) in ruins[world].withIndex()){
            val gy=ground(r[0],r[1]);val pulse=.35f+.25f*sin(t*2f+ri)
            for(k in 0 until 8){val a=k*PI.toFloat()/4f;box(r[0]+cos(a)*2.05f,gy+.18f,r[1]+sin(a)*2.05f,.42f,.16f,.72f,.78f,.82f,.84f,texMoss,pulse)}
            box(r[0],gy+.06f,r[1],1.15f,.07f,1.15f,.30f,.72f,1f,texBlue,.72f)
        }
    }
    private fun drawPortals(){
        fun portal(x:Float,z:Float,r:Float,g:Float,b:Float){
            val gy=ground(x,z);box(x-1.55f,gy+1.65f,z,.34f,1.65f,.45f,.72f,.74f,.78f,texCobble);box(x+1.55f,gy+1.65f,z,.34f,1.65f,.45f,.72f,.74f,.78f,texCobble);box(x,gy+3.15f,z,1.9f,.30f,.45f,.72f,.74f,.78f,texCobble)
            box(x,gy+1.65f,z,.06f,1.35f,1.25f,r,g,b,texObsidian,.95f,.82f)
            for(k in 0 until 6){val a=t*1.5f+k*PI.toFloat()/3f;sph(x+cos(a)*1.25f,gy+1.65f+sin(a*.7f)*.9f,z+.18f,.08f,r,g,b,0,1f)}
        }
        if(world==0){portal(24f,0f,1f,.55f,.18f);portal(-24f,0f,.30f,.85f,.35f)}else portal(0f,18f,.35f,.75f,1f)
    }
    private fun drawHumanoid(x:Float,z:Float,friendly:Boolean,player:Boolean=false,brute:Boolean=false){
        val gy=if(player)py else ground(x,z)+1.05f;val body=if(player)floatArrayOf(.22f,.28f,.34f) else if(brute)floatArrayOf(.30f,.04f,.13f) else if(friendly)floatArrayOf(.18f,.42f,.34f) else floatArrayOf(.48f,.08f,.08f)
        val k=if(brute)1.45f else 1f
        box(x,gy+k*.32f,z,.36f*k,.78f*k,.25f*k,body[0],body[1],body[2],if(player)texStone else 0)
        sph(x,gy+1.08f*k,z,.34f*k,if(brute).60f else .84f,if(brute).50f else .70f,if(brute).46f else .58f,0)
        box(x-.48f*k,gy+.05f,z,.12f,.62f*k,.12f,body[0]*.9f,body[1]*.9f,body[2]*.9f,0)
        box(x+.48f*k,gy+.05f,z,.12f,.62f*k,.12f,body[0]*.9f,body[1]*.9f,body[2]*.9f,0)
        box(x-.20f*k,gy-.92f,z,.13f,.42f*k,.14f,.10f,.11f,.13f,0);box(x+.20f*k,gy-.92f,z,.13f,.42f*k,.14f,.10f,.11f,.13f,0)
    }
    private fun drawMobs(){for(m in mobs[world])if(m.hp>0){val msz=if(m.brute).85f else .55f;drawMesh(sphere,sphereCount,m.x,ground(m.x,m.z)+.02f,m.z,msz,.06f,msz,.05f,.05f,.08f,.30f,0,0f,0f);drawHumanoid(m.x,m.z,m.friendly,false,m.brute);if(m.hitFx>0f)sph(m.x,ground(m.x,m.z)+1.1f,m.z,if(m.brute)1.5f else 1.1f,1f,.85f,.5f,0,.55f,.4f*m.hitFx/.3f)}}
    private fun drawShots(){for(s in shots){sph(s.x,s.y,s.z,.17f,s.r,s.g,s.b,0,1f);sph(s.x,ground(s.x,s.z)+.06f,s.z,.30f,.08f,.08f,.10f,0,.30f,.55f)}}
    private fun drawBoss3(){
        if(!boss3On||world!=2)return;drawMesh(sphere,sphereCount,boss3X,ground(boss3X,boss3Z,2)+.02f,boss3Z,1.2f,.08f,1.2f,.04f,.06f,.04f,.35f,0,0f,0f)
        val gy=ground(boss3X,boss3Z,2)+1.05f
        val sway=sin(boss3Ph*1.1f)*.06f
        val en=boss3Hp<boss3Max*.3f
        sph(boss3X,gy+.25f,boss3Z,2.3f,if(en).85f else .20f,if(en).25f else .70f,.15f,0,.32f,.22f)
        box(boss3X,gy+1.5f+sway,boss3Z,.75f,1.6f,.45f,.36f,.27f,.16f,texLog)
        sph(boss3X,gy+3.1f+sway,boss3Z,.55f,.30f,.48f,.20f,texLeaves)
        sph(boss3X-.5f,gy+3.5f+sway,boss3Z+.2f,.45f,.25f,.44f,.18f,texLeaves)
        sph(boss3X+.5f,gy+3.5f+sway,boss3Z-.2f,.45f,.25f,.44f,.18f,texLeaves)
        sph(boss3X-.16f,gy+3.05f+sway,boss3Z+.30f,.07f,if(en)1f else .5f,if(en).4f else 1f,if(en).15f else .45f,0,1f)
        sph(boss3X+.16f,gy+3.05f+sway,boss3Z+.30f,.07f,if(en)1f else .5f,if(en).4f else 1f,if(en).15f else .45f,0,1f)
        for(k in 0 until 4){val ba3=t*1.2f+k*1.5708f;box(boss3X+cos(ba3)*1.2f,gy+.35f,boss3Z+sin(ba3)*1.2f,.14f,.55f,.14f,.30f,.22f,.12f,texLog,.45f,.8f)}
    }
    private fun drawPlayer(){drawMesh(sphere,sphereCount,px,ground(px,pz)+.02f,pz,.55f,.06f,.55f,.05f,.05f,.08f,.32f,0,0f,0f);if(won1&&won2&&won3){sph(px,py-.25f,pz,1.35f,1f,.85f,.35f,0,.30f,.22f);for(k in 0 until 4){val ga=t*2.2f+k*1.5708f;sph(px+cos(ga)*.9f,py+.35f+sin(t*3f+k)*.2f,pz+sin(ga)*.9f,.05f,1f,.92f,.55f,0,.9f)}};if(flying){val bx=px-sin(yaw)*.45f;val bz=pz+cos(yaw)*.45f;val px2=cos(yaw)*.30f;val pz2=sin(yaw)*.30f;box(bx-px2,py+.85f,bz-pz2,.09f,.42f,.50f,.55f,.75f,1f,0,.4f,.6f);box(bx+px2,py+.85f,bz+pz2,.09f,.42f,.50f,.55f,.75f,1f,0,.4f,.6f)};drawHumanoid(px,pz,true,true);if(flying||dashFx>0){for(i in 1..6){val d=i*.34f;sph(px-sin(yaw)*d,py+.55f,pz+cos(yaw)*d,.16f+i*.025f,.04f,.04f,.055f,0,.45f,.55f)}}}
    private fun drawSparks(){for(s in sparks)sph(s.x,s.y,s.z,s.size,s.r,s.g,s.b,0,.9f,(s.life/.8f).coerceIn(0f,1f))}

}

private class V07Audio(ctx:android.content.Context){
    private val pool=android.media.SoundPool.Builder().setMaxStreams(6).setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_GAME).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val ids=java.util.HashMap<String,Int>()
    private val loaded=java.util.concurrent.atomic.AtomicInteger(0)
    private val dir=java.io.File(ctx.cacheDir,"sfx")
    private var amb:android.media.MediaPlayer?=null
    private var ambW=-1
    private var lp=0.0
    private fun rnd(x:Double,s:Double):Double{val v=Math.sin(x*127.1+s*311.7)*43758.5453;return v-Math.floor(v)}
    private fun putWav(f:java.io.File,sr:Int,data:ShortArray){
        val b=java.nio.ByteBuffer.allocate(44+data.size*2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray());b.putInt(36+data.size*2);b.put("WAVE".toByteArray())
        b.put("fmt ".toByteArray());b.putInt(16);b.putShort(1);b.putShort(1);b.putInt(sr);b.putInt(sr*2);b.putShort(2);b.putShort(16)
        b.put("data".toByteArray());b.putInt(data.size*2)
        for(v in data)b.putShort(v)
        java.io.FileOutputStream(f).use{it.write(b.array())}
    }
    private fun gen(name:String,sr:Int,dur:Double,cross:Double,g:(Double)->Double){
        val n=(sr*dur).toInt();val d=ShortArray(n)
        for(i in 0 until n){val t=i.toDouble()/sr;val v=Math.max(-.98,Math.min(.98,g(t)));d[i]=(v*11000).toInt().toShort()}
        if(cross>0){val m=(sr*cross).toInt();if(m>0&&m<n){for(i in 0 until m){val w=i.toDouble()/m;val j=n-m+i;d[j]=((d[j].toDouble()*(1.0-w))+(d[i].toDouble()*w)).toInt().toShort()}}}
        putWav(java.io.File(dir,"$name.wav"),sr,d)
    }
    init{
        try{
            pool.setOnLoadCompleteListener{_,_,st->if(st==0)loaded.incrementAndGet()}
            dir.mkdirs()
            gen("spell",22050,.7,0.0){t->val e=t/.7;val f=480.0*Math.pow(70.0/480.0,e);(Math.sin(6.283185307179586*f*t)*.6+(rnd(t*997.0,1.0)*2.0-1.0)*.28*Math.exp(-9.0*e))*Math.exp(-3.0*e)}
            lp=0.0;gen("dash",22050,.38,0.0){t->val e=t/.38;val n=rnd(t*719.0,2.0)*2.0-1.0;lp+=.12*(n-lp);lp*2.6*Math.sin(3.141592653589793*e)}
            gen("portal",22050,1.3,0.0){t->val e=t/1.3;val env=Math.exp(-1.7*e);(Math.sin(6.283185307179586*523.25*t)*.32+Math.sin(6.283185307179586*659.25*t)*.26+Math.sin(6.283185307179586*783.99*t)*.2+Math.sin(6.283185307179586*1046.5*t)*.09)*env}
            lp=0.0;gen("hit",22050,.3,0.0){t->val e=t/.3;val env=Math.exp(-8.0*e);(Math.sin(6.283185307179586*82.0*t)*.85+(rnd(t*991.0,3.0)*2.0-1.0)*.3*Math.exp(-45.0*e))*env}
            lp=0.0;gen("step",22050,.12,0.0){t->val e=t/.12;val n=rnd(t*1301.0,4.0)*2.0-1.0;lp+=.2*(n-lp);lp*1.6*Math.exp(-13.0*e)}
            lp=0.0;gen("flight",22050,.7,0.0){t->val e=t/.7;val n=rnd(t*677.0,5.0)*2.0-1.0;lp+=.08*(n-lp);(Math.sin(6.283185307179586*(190.0+520.0*e)*t)*.2+lp*1.2)*Math.sin(3.141592653589793*e)}
            gen("chime",22050,.55,0.0){t->val e=t/.55;(Math.sin(6.283185307179586*880.0*t)*.3+Math.sin(6.283185307179586*1318.5*t)*.22+Math.sin(6.283185307179586*1760.0*t)*.12)*Math.exp(-4.5*e)}
            lp=0.0;gen("kill",22050,.42,0.0){t->val e=t/.42;val env=Math.exp(-5.0*e);(Math.sin(6.283185307179586*131.0*t)*.5+(Math.sin(6.283185307179586*659.25*t)*.2+(rnd(t*773.0,10.0)*2.0-1.0)*.2*Math.exp(-22.0*e))*Math.exp(-14.0*e))*env}
            lp=0.0;gen("slam",22050,.5,0.0){t->val e=t/.5;val env=Math.exp(-5.0*e);(Math.sin(6.283185307179586*54.0*t)*.9+(rnd(t*733.0,11.0)*2.0-1.0)*.25*Math.exp(-28.0*e))*env}
            lp=0.0;gen("thunder",22050,1.5,0.0){t->val e=t/1.5;val n=rnd(t*311.0,12.0)*2.0-1.0;lp+=.03*(n-lp);lp*1.9*Math.exp(-2.2*e)}
            gen("cricket",22050,.55,0.0){t->val tr=(t*22.0)%1.0;val env=Math.exp(-16.0*tr)*Math.exp(-3.5*t);Math.sin(6.283185307179586*4300.0*t)*.16*env}
            gen("yip",22050,.26,0.0){t->val e=t/.26;Math.sin(6.283185307179586*(1500.0-650.0*e)*t)*.22*Math.exp(-5.0*e)*(if((t*32.0)%1.0<.5)1.0 else 0.0)}
            gen("birds",22050,1.2,0.0){t->val e=t/1.2;val tr=(t*7.0)%1.0;val fr=if(tr<.5)2600.0 else 3100.0;Math.sin(6.283185307179586*fr*t)*.14*Math.exp(-8.0*tr)*Math.exp(-1.5*e)}
            gen("owl",22050,1.0,0.0){t->val e=t/1.0;val tr2=(t*2.0)%1.0;val h=Math.exp(-9.0*tr2)*(if(tr2<.55)1.0 else 0.0);Math.sin(6.283185307179586*(315.0+25.0*Math.sin(t*11.0))*t)*.17*h*Math.exp(-.8*e)}
            lp=0.0;gen("amb0",22050,7.0,.8){t->val n=rnd(t*509.0,6.0)*2.0-1.0;lp+=.045*(n-lp);lp*1.8*(.55+.45*Math.sin(6.283185307179586*t*2.0/7.0))}
            gen("amb1",22050,8.0,0.0){t->val e=t/8.0;val tr=.6+.4*Math.sin(6.283185307179586*e*3.0);(Math.sin(6.283185307179586*110.0*t)*.2+Math.sin(6.283185307179586*165.0*t)*.14+Math.sin(6.283185307179586*220.0*t)*.11+Math.sin(6.283185307179586*330.0*t)*.06)*tr}
            lp=0.0;gen("amb2",22050,9.0,1.0){t->val n=rnd(t*467.0,7.0)*2.0-1.0;lp+=.05*(n-lp);var v=lp*.55;val bt=t*3.0;val bi=Math.floor(bt).toInt();val ph=bt-bi;if(rnd(bi.toDouble(),8.0)>.7&&ph<.09){val f=1700.0+rnd(bi.toDouble(),9.0)*1800.0;v+=Math.sin(6.283185307179586*f*ph)*Math.exp(-38.0*ph)*.24};v}
            for(k in arrayOf("spell","dash","portal","hit","step","flight","chime","kill","slam","thunder","cricket","yip","birds","owl"))try{ids[k]=pool.load(java.io.File(dir,k+".wav").path,1)}catch(_:Throwable){}
        }catch(_:Throwable){}
    }
    fun play(name:String,rate:Float=1f){try{val id=ids[name]?:return;pool.play(id,1f,1f,1,0,rate)}catch(_:Throwable){}}
    fun setWorld(w:Int){
        if(ambW==w)return;ambW=w
        try{amb?.release()}catch(_:Throwable){};amb=null
        try{val p=android.media.MediaPlayer();p.setDataSource(java.io.File(dir,"amb"+w+".wav").path);p.isLooping=true;p.setVolume(.32f,.32f);p.prepare();p.start();amb=p}catch(_:Throwable){amb=null}
    }
    fun pause(){try{amb?.pause()}catch(_:Throwable){}}
    fun resume(){try{amb?.start()}catch(_:Throwable){}}
    fun status()=loaded.get().toString()+"/"+ids.size+" sons"
}
