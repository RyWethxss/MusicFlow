package dev.musicflow.app
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.media.session.MediaSessionManager
import android.media.MediaMetadata
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
class MainActivity : Activity() {
 private val handler = Handler(Looper.getMainLooper())
 private lateinit var title: TextView
 private lateinit var artist: TextView
 private lateinit var cover: ImageView
 private val tick = object : Runnable { override fun run() { refresh(); handler.postDelayed(this, 1800) } }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,60,32,30); gravity=Gravity.CENTER_HORIZONTAL; setBackgroundColor(Color.rgb(15,18,27)) }
  fun label(s:String, size:Float):TextView = TextView(this).apply { text=s; textSize=size; setTextColor(Color.WHITE); gravity=Gravity.CENTER; setPadding(0,12,0,12) }
  root.addView(label("♫ MusicFlow",32f).apply { setTypeface(null,Typeface.BOLD) })
  cover=ImageView(this).apply { setBackgroundColor(Color.rgb(37,43,60)); scaleType=ImageView.ScaleType.FIT_CENTER }
  root.addView(cover,LinearLayout.LayoutParams(-1,0,1f))
  title=label("Müzik bekleniyor",23f)
  artist=label("Spotify veya başka bir oynatıcı aç",16f)
  root.addView(title); root.addView(artist)
  root.addView(Button(this).apply { text="Medya erişimine izin ver"; setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } })
  root.addView(label("MusicFlow • Open Source",12f))
  setContentView(root)
 }
 override fun onResume() { super.onResume(); handler.removeCallbacks(tick); handler.post(tick) }
 override fun onPause() { handler.removeCallbacks(tick); super.onPause() }
 private fun refresh() { try {
  val mgr=getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
  val controllers=mgr.getActiveSessions(ComponentName(this,MediaListener::class.java))
  val active=controllers.firstOrNull { it.playbackState?.state==android.media.session.PlaybackState.STATE_PLAYING } ?: controllers.firstOrNull()
  val md=active?.metadata
  title.text=md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Müzik bekleniyor"
  artist.text=md?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Medya erişimini etkinleştir"
  cover.setImageBitmap(md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART))
 } catch (_:Exception) { title.text="Medya erişimi gerekli" } }
}