package dev.musicflow.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private enum class DisplayMode { STUDIO, AMBIENT, LOCK_VIEW }

    private val stage = Color.rgb(23, 37, 30)
    private val stageLight = Color.rgb(41, 56, 46)
    private val paper = Color.rgb(244, 241, 233)
    private val muted = Color.rgb(177, 190, 174)
    private val line = Color.rgb(78, 96, 79)
    private val accent = Color.rgb(196, 71, 35)
    private val sage = Color.rgb(183, 200, 161)

    private val handler = Handler(Looper.getMainLooper())
    private var mode = DisplayMode.STUDIO
    private var controller: MediaController? = null
    private var metadata: MediaMetadata? = null
    private var durationMs = 0L
    private var seeking = false
    private var lastTrackKey = ""
    private var lastVisibleTopInset = 0
    private var lastVisibleBottomInset = 0
    private lateinit var root: LinearLayout
    private lateinit var scrollContainer: ScrollView
    private lateinit var brandHeader: LinearLayout
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var sourceText: TextView
    private lateinit var artwork: ImageView
    private lateinit var artworkFallback: LinearLayout
    private lateinit var trackTitle: TextView
    private lateinit var trackArtist: TextView
    private lateinit var albumText: TextView
    private lateinit var playButton: TextView
    private lateinit var previousButton: TextView
    private lateinit var nextButton: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var elapsedText: TextView
    private lateinit var durationText: TextView
    private lateinit var permissionStatus: TextView
    private lateinit var permissionAction: TextView
    private lateinit var permissionCard: LinearLayout
    private lateinit var modeInfo: TextView
    private lateinit var modeSelector: LinearLayout
    private lateinit var ambientExit: TextView
    private lateinit var modeButtons: Map<DisplayMode, TextView>
    private lateinit var titleTypeface: Typeface
    private lateinit var monoTypeface: Typeface

    private val sessionCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            this@MainActivity.metadata = metadata
            renderTrack(metadata)
            updateProgress()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updatePlaybackControls()
            updateProgress()
        }

        override fun onSessionDestroyed() {
            controller?.unregisterCallback(this)
            controller = null
            refreshSessions()
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            updateClock()
            refreshSessions()
            updateProgress()
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        titleTypeface = loadTypeface("fonts/manrope_variable.ttf", Typeface.DEFAULT)
        monoTypeface = loadTypeface("fonts/ibm_plex_mono.ttf", Typeface.MONOSPACE)
        window.statusBarColor = stage
        window.navigationBarColor = stage
        window.decorView.systemUiVisibility = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        mode = DisplayMode.values().getOrElse(
            getPreferences(MODE_PRIVATE).getInt("display_mode", 0)
        ) { DisplayMode.STUDIO }
        buildScreen()
        applyMode()
        updateClock()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(ticker)
        refreshSessions()
        handler.post(ticker)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    override fun onDestroy() {
        controller?.unregisterCallback(sessionCallback)
        super.onDestroy()
    }

    private fun buildScreen() {
        scrollContainer = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = true
            setBackgroundColor(stage)
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(16), dp(24), dp(28))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(stage, Color.rgb(29, 44, 34), stage)
            )
        }
        scrollContainer.addView(root, FrameLayout.LayoutParams(-1, -2))
        scrollContainer.setOnApplyWindowInsetsListener { _, insets ->
            val bars = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.systemBars())
            } else {
                null
            }
            val topInset = bars?.top ?: insets.systemWindowInsetTop
            val bottomInset = bars?.bottom ?: insets.systemWindowInsetBottom
            if (topInset > 0) lastVisibleTopInset = topInset
            if (bottomInset > 0) lastVisibleBottomInset = bottomInset
            val topPadding = if (mode == DisplayMode.AMBIENT) 0 else lastVisibleTopInset
            val bottomPadding = if (mode == DisplayMode.AMBIENT) 0 else lastVisibleBottomInset
            scrollContainer.setPadding(0, topPadding, 0, bottomPadding)
            root.setPadding(dp(24), dp(16), dp(24), dp(28))
            insets
        }
        setContentView(scrollContainer)

        brandHeader = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        val mark = ImageView(this).apply {
            setImageResource(R.drawable.ic_wethworks_mark)
            imageTintList = android.content.res.ColorStateList.valueOf(sage)
            contentDescription = "WETHWORKS"
        }
        brandHeader.addView(mark, LinearLayout.LayoutParams(dp(36), dp(36)))
        val brandCopy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brandCopy.addView(text("WETHWORKS", 10f, sage, monoTypeface, bold = false))
        brandCopy.addView(text("MusicFlow", 20f, paper, titleTypeface, bold = true))
        val brandSpace = View(this)
        brandHeader.addView(brandSpace, LinearLayout.LayoutParams(0, 1, 1f))
        brandHeader.addView(brandCopy)
        root.addView(brandHeader, fullWidth())

        clockText = text("--:--", 46f, paper, monoTypeface, bold = false).apply {
            gravity = Gravity.CENTER
            letterSpacing = -0.035f
        }
        root.addView(clockText, topMargin(fullWidth(), 20))
        dateText = text("YOUR LISTENING SPACE", 10f, muted, monoTypeface).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
        }
        root.addView(dateText, topMargin(fullWidth(), 2))

        val coverFrame = FrameLayout(this).apply {
            background = rounded(stageLight, 22)
            clipToOutline = true
            elevation = dp(8).toFloat()
        }
        artwork = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(stageLight, 22)
            contentDescription = "Album artwork"
        }
        coverFrame.addView(artwork, FrameLayout.LayoutParams(-1, -1))
        artworkFallback = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(52, 75, 55), stageLight, Color.rgb(84, 60, 42))
            )
        }
        artworkFallback.addView(
            text("W", 96f, sage, titleTypeface, bold = true).apply { gravity = Gravity.CENTER },
            fullWidth()
        )
        artworkFallback.addView(
            text("A LITTLE MORE PRESENT", 9f, paper, monoTypeface).apply { gravity = Gravity.CENTER },
            fullWidth()
        )
        coverFrame.addView(artworkFallback, FrameLayout.LayoutParams(-1, -1))
        val sourceChip = text("READY WHEN YOU ARE", 9f, paper, monoTypeface)
        sourceChip.gravity = Gravity.CENTER
        sourceChip.setPadding(dp(12), dp(8), dp(12), dp(8))
        sourceChip.background = rounded(Color.argb(195, 23, 37, 30), 99)
        val chipParams = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply {
            setMargins(dp(14), dp(14), dp(14), dp(14))
        }
        coverFrame.addView(sourceChip, chipParams)
        sourceText = sourceChip
        val width = (resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt()
        val coverSize = (width - 48).coerceIn(240, 360)
        root.addView(coverFrame, topMargin(LinearLayout.LayoutParams(dp(coverSize), dp(coverSize)), 22))

        trackTitle = text("Your music, in the moment.", 23f, paper, titleTypeface, bold = true).apply {
            gravity = Gravity.CENTER
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        root.addView(trackTitle, topMargin(fullWidth(), 22))
        trackArtist = text("Start playing in any music app", 14f, muted, titleTypeface).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        root.addView(trackArtist, topMargin(fullWidth(), 5))
        albumText = text("MusicFlow follows your active media session", 11f, muted, monoTypeface).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        root.addView(albumText, topMargin(fullWidth(), 5))

        val progressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        elapsedText = text("0:00", 10f, muted, monoTypeface)
        durationText = text("--:--", 10f, muted, monoTypeface)
        seekBar = SeekBar(this).apply {
            max = 1000
            progressTintList = android.content.res.ColorStateList.valueOf(sage)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(line)
            thumbTintList = android.content.res.ColorStateList.valueOf(paper)
            contentDescription = "Track position"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) { seeking = true }
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) elapsedText.text = formatTime(durationMs * progress / 1_000L)
                }
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seeking = false
                    val target = durationMs * (this@MainActivity.seekBar.progress.toLong()) / 1_000L
                    runCatching { controller?.transportControls?.seekTo(target) }
                }
            })
        }
        progressRow.addView(elapsedText)
        progressRow.addView(seekBar, LinearLayout.LayoutParams(0, dp(38), 1f).apply {
            marginStart = dp(10); marginEnd = dp(10)
        })
        progressRow.addView(durationText)
        root.addView(progressRow, topMargin(fullWidth(), 8))

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        previousButton = control("|◀", 48, 20, "Previous track") {
            runCatching { controller?.transportControls?.skipToPrevious() }
        }
        playButton = control("▶", 68, 26, "Play") {
            runCatching {
                val transport = controller?.transportControls ?: return@control
                if (controller?.playbackState?.state == PlaybackState.STATE_PLAYING) transport.pause()
                else transport.play()
            }
        }
        nextButton = control("▶|", 48, 20, "Next track") {
            runCatching { controller?.transportControls?.skipToNext() }
        }
        controls.addView(previousButton, sideControlParams(12))
        controls.addView(playButton, sideControlParams(28))
        controls.addView(nextButton, sideControlParams(12))
        root.addView(controls, topMargin(fullWidth(), 12))

        root.addView(sectionLabel("YOUR DISPLAY"), topMargin(fullWidth(), 26))
        modeSelector = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val studio = modeTile("STUDIO", "Full player", DisplayMode.STUDIO)
        val ambient = modeTile("AMBIENT", "Dim display", DisplayMode.AMBIENT)
        val lock = modeTile("LOCK VIEW", "Keyguard-ready", DisplayMode.LOCK_VIEW)
        modeButtons = mapOf(
            DisplayMode.STUDIO to studio,
            DisplayMode.AMBIENT to ambient,
            DisplayMode.LOCK_VIEW to lock
        )
        listOf(studio, ambient, lock).forEachIndexed { index, button ->
            modeSelector.addView(button, LinearLayout.LayoutParams(0, dp(70), 1f).apply {
                if (index > 0) marginStart = dp(8)
            })
        }
        root.addView(modeSelector, topMargin(fullWidth(), 10))
        modeInfo = text("", 11f, muted, titleTypeface).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(2))
            maxLines = 3
        }
        root.addView(modeInfo, topMargin(fullWidth(), 6))
        ambientExit = text("EXIT AMBIENT VIEW", 10f, paper, monoTypeface).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(Color.argb(130, 255, 253, 247), 8, line)
            setOnClickListener { setMode(DisplayMode.STUDIO) }
        }
        root.addView(ambientExit, topMargin(fullWidth(), 12))

        permissionCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(Color.argb(95, 255, 253, 247), 12, line)
        }
        permissionStatus = text("CONNECT YOUR MUSIC", 11f, sage, monoTypeface, bold = true)
        permissionCard.addView(permissionStatus, fullWidth())
        permissionCard.addView(text(
            "Allow MusicFlow to see active media sessions so it can show track details and control playback.",
            13f, paper, titleTypeface
        ).apply { setPadding(0, dp(7), 0, dp(10)) }, fullWidth())
        permissionAction = text("OPEN NOTIFICATION ACCESS  →", 10f, paper, monoTypeface, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(accent, 6)
            isClickable = true
            isFocusable = true
            setOnClickListener { openNotificationAccess() }
        }
        permissionCard.addView(permissionAction, fullWidth())
        root.addView(permissionCard, topMargin(fullWidth(), 24))

        val privacy = text(
            "MusicFlow uses active media sessions for music details. Notification text is not read or stored.",
            11f, muted, titleTypeface
        ).apply { gravity = Gravity.CENTER; setPadding(dp(8), dp(12), dp(8), 0) }
        root.addView(privacy, topMargin(fullWidth(), 8))
        val footer = text("WETHWORKS  /  BUILT TO BE UNDERSTOOD", 9f, sage, monoTypeface).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.04f
        }
        root.addView(footer, topMargin(fullWidth(), 28))
        updateModeUi()
        updatePermissionUi()
    }

    private fun modeTile(label: String, detail: String, selectedMode: DisplayMode): TextView =
        text("$label\n$detail", 9f, paper, monoTypeface).apply {
            gravity = Gravity.CENTER
            maxLines = 2
            setPadding(dp(4), dp(8), dp(4), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { setMode(selectedMode) }
        }

    private fun setMode(newMode: DisplayMode) {
        mode = newMode
        getPreferences(MODE_PRIVATE).edit().putInt("display_mode", mode.ordinal).apply()
        applyMode()
        updateModeUi()
    }

    private fun applyMode() {
        val ambient = mode == DisplayMode.AMBIENT
        val lockView = mode == DisplayMode.LOCK_VIEW
        window.setFlags(
            if (ambient) WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON else 0,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(lockView)
        } else {
            window.setFlags(
                if (lockView) WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED else 0,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            )
        }
        window.attributes = window.attributes.apply {
            screenBrightness = if (ambient) 0.08f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        if (ambient) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LOW_PROFILE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else {
            window.decorView.systemUiVisibility = 0
        }
        window.statusBarColor = stage
        window.navigationBarColor = stage
        val topPadding = if (ambient) 0 else lastVisibleTopInset
        val bottomPadding = if (ambient) 0 else lastVisibleBottomInset
        scrollContainer.setPadding(0, topPadding, 0, bottomPadding)
        root.setPadding(dp(24), dp(16), dp(24), dp(28))
        window.decorView.requestApplyInsets()
    }

    private fun updateModeUi() {
        modeButtons.forEach { (key, button) ->
            val selected = key == mode
            button.background = rounded(
                if (selected) Color.rgb(67, 88, 67) else Color.argb(75, 255, 253, 247),
                8,
                if (selected) sage else line
            )
            button.setTextColor(if (selected) paper else muted)
        }
        modeInfo.text = when (mode) {
            DisplayMode.STUDIO -> "Your full music display. Playback stays with the app already playing music."
            DisplayMode.AMBIENT -> "Dim bedside view. Keeps this screen awake while MusicFlow is open."
            DisplayMode.LOCK_VIEW -> "Can appear over the keyguard while already open. Android still owns the lock screen and screen wake."
        }
        modeSelector.visibility = if (mode == DisplayMode.AMBIENT) View.GONE else View.VISIBLE
        permissionCard.visibility = if (mode == DisplayMode.AMBIENT) View.GONE else View.VISIBLE
        ambientExit.visibility = if (mode == DisplayMode.AMBIENT) View.VISIBLE else View.GONE
        brandHeader.visibility = if (mode == DisplayMode.AMBIENT) View.GONE else View.VISIBLE
        dateText.text = when (mode) {
            DisplayMode.AMBIENT -> "AMBIENT LISTENING"
            DisplayMode.LOCK_VIEW -> "LOCK VIEW READY"
            DisplayMode.STUDIO -> "YOUR LISTENING SPACE"
        }
    }

    private fun refreshSessions() {
        val hasAccess = hasNotificationAccess()
        updatePermissionUi(hasAccess)
        if (!hasAccess) {
            if (controller != null) {
                controller?.unregisterCallback(sessionCallback)
                controller = null
            }
            metadata = null
            renderTrack(null)
            updatePlaybackControls()
            return
        }
        try {
            val manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val active = manager.getActiveSessions(ComponentName(this, MediaListener::class.java))
            val chosen = active.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: active.firstOrNull { it.playbackState?.state == PlaybackState.STATE_BUFFERING }
                ?: active.firstOrNull()
            if (chosen?.sessionToken != controller?.sessionToken) {
                controller?.unregisterCallback(sessionCallback)
                controller = chosen
                controller?.registerCallback(sessionCallback, handler)
                metadata = controller?.metadata
                lastTrackKey = ""
                renderTrack(metadata)
                updatePlaybackControls()
            }
        } catch (_: SecurityException) {
            updatePermissionUi(false)
        } catch (_: RuntimeException) {
            updatePermissionUi(false)
        }
    }

    private fun renderTrack(media: MediaMetadata?) {
        metadata = media
        val title = media?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
        val artist = media?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?.takeIf { it.isNotBlank() }
            ?: media?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.takeIf { it.isNotBlank() }
        val album = media?.getString(MediaMetadata.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }
        val packageName = controller?.packageName
        val sourceName = packageName?.let(::applicationName)
        durationMs = media?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val key = listOf(title, artist, album, packageName, durationMs).joinToString("|")
        if (key == lastTrackKey) return
        lastTrackKey = key
        if (title == null && artist == null && sourceName == null) {
            trackTitle.text = "Your music, in the moment."
            trackArtist.text = "Start playing in any music app"
            albumText.text = "MusicFlow follows your active media session"
            sourceText.text = "READY WHEN YOU ARE"
        } else {
            trackTitle.text = title ?: "Unknown track"
            trackArtist.text = artist ?: "Unknown artist"
            albumText.text = album ?: "Playing from ${sourceName ?: "your music app"}"
            sourceText.text = (sourceName ?: "MUSIC APP").uppercase(Locale.getDefault())
        }
        val cover: Bitmap? = runCatching {
            media?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: media?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: media?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        }.getOrNull()
        if (cover != null) {
            artwork.setImageBitmap(cover)
            artwork.visibility = View.VISIBLE
            artworkFallback.visibility = View.GONE
        } else {
            artwork.setImageDrawable(null)
            artwork.visibility = View.GONE
            artworkFallback.visibility = View.VISIBLE
        }
        durationText.text = if (durationMs > 0) formatTime(durationMs) else "--:--"
        updateProgress()
    }

    private fun updatePermissionUi(hasAccess: Boolean = hasNotificationAccess()) {
        if (!::permissionCard.isInitialized) return
        permissionStatus.text = if (hasAccess) "MEDIA ACCESS IS READY" else "CONNECT YOUR MUSIC"
        permissionStatus.setTextColor(if (hasAccess) sage else paper)
        permissionAction.text = if (hasAccess) "MANAGE NOTIFICATION ACCESS  →" else "OPEN NOTIFICATION ACCESS  →"
        permissionAction.background = rounded(if (hasAccess) stageLight else accent, 6)
        if (hasAccess && controller == null) {
            trackArtist.text = "Open Spotify or another music app to begin"
        }
    }

    private fun hasNotificationAccess(): Boolean {
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val component = ComponentName(this, MediaListener::class.java).flattenToString()
        return listeners.split(':').any { it.equals(component, ignoreCase = true) }
    }

    private fun openNotificationAccess() {
        runCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            .onFailure {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                })
            }
    }

    private fun updatePlaybackControls() {
        val state = controller?.playbackState?.state
        val playing = state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
        playButton.text = if (playing) "Ⅱ" else "▶"
        playButton.contentDescription = if (playing) "Pause" else "Play"
        val enabled = controller != null
        listOf(previousButton, playButton, nextButton).forEach { button ->
            button.alpha = if (enabled) 1f else 0.45f
            button.isEnabled = enabled
        }
        seekBar.isEnabled = enabled && durationMs > 0
    }

    private fun updateProgress() {
        val playback = controller?.playbackState
        if (durationMs <= 0L) durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val basePosition = playback?.position ?: 0L
        val advanced = if (playback?.state == PlaybackState.STATE_PLAYING && playback.lastPositionUpdateTime > 0L) {
            ((android.os.SystemClock.elapsedRealtime() - playback.lastPositionUpdateTime)
                .coerceAtLeast(0L) * playback.playbackSpeed).toLong()
        } else 0L
        val position = (basePosition + advanced).coerceIn(0L, durationMs.coerceAtLeast(0L))
        if (!seeking && durationMs > 0L) seekBar.progress = (position * 1_000L / durationMs).toInt()
        if (!seeking) elapsedText.text = formatTime(position)
        durationText.text = if (durationMs > 0) formatTime(durationMs) else "--:--"
        seekBar.isEnabled = controller != null && durationMs > 0
    }

    private fun updateClock() {
        val now = Date()
        clockText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        if (mode != DisplayMode.AMBIENT) {
            dateText.text = SimpleDateFormat("EEEE  ·  d MMMM", Locale.getDefault())
                .format(now).uppercase(Locale.getDefault())
        }
    }

    private fun applicationName(packageName: String): String = runCatching {
        val app = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(app).toString()
    }.getOrDefault(packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() })

    private fun control(symbol: String, size: Int, textSize: Int, description: String, click: () -> Unit) =
        text(symbol, textSize.toFloat(), paper, titleTypeface, bold = true).apply {
            gravity = Gravity.CENTER
            contentDescription = description
            isClickable = true
            isFocusable = true
            background = rounded(if (size > 50) accent else Color.argb(110, 255, 253, 247), 99)
            setOnClickListener { if (isEnabled) click() }
        }

    private fun sideControlParams(gap: Int) = LinearLayout.LayoutParams(dp(68), dp(68)).apply {
        marginStart = dp(gap); marginEnd = dp(gap)
    }

    private fun sectionLabel(label: String) = text(label, 10f, sage, monoTypeface, bold = true).apply {
        letterSpacing = 0.1f
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        typeface: Typeface,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        this.typeface = if (bold) Typeface.create(typeface, Typeface.BOLD) else typeface
        includeFontPadding = true
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun rounded(color: Int, radiusDp: Int, strokeColor: Int? = null) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeColor != null) setStroke(dp(1), strokeColor)
        }

    private fun fullWidth() = LinearLayout.LayoutParams(-1, -2)

    private fun topMargin(params: LinearLayout.LayoutParams, marginDp: Int) = params.apply {
        topMargin = dp(marginDp)
    }

    private fun loadTypeface(path: String, fallback: Typeface) = runCatching {
        Typeface.createFromAsset(assets, path)
    }.getOrDefault(fallback)

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1_000L).coerceAtLeast(0)
        return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
