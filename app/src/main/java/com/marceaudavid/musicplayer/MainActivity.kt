package com.marceaudavid.musicplayer

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaMetadata
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class MainActivity : AppCompatActivity() {

    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var controls: ConstraintLayout
    private lateinit var art: ImageView
    private lateinit var title: TextView
    private lateinit var artist: TextView
    private lateinit var play: ImageButton
    private lateinit var stop: ImageButton
    private lateinit var next: ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        controls = findViewById(R.id.controls)
        art = findViewById(R.id.art)
        title = findViewById(R.id.title)
        artist = findViewById(R.id.artist)
        play = findViewById(R.id.play)
        stop = findViewById(R.id.stop)
        next = findViewById(R.id.next)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        } else {
            mediaBrowser = MediaBrowserCompat(
                this,
                ComponentName(this, MusicService::class.java),
                connectionCallbacks,
                null
            )
        }
    }

    public override fun onStart() {
        super.onStart()
        mediaBrowser.connect()
    }

    public override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    public override fun onStop() {
        super.onStop()
        MediaControllerCompat.getMediaController(this)?.unregisterCallback(controllerCallbacks)
        mediaBrowser.disconnect()
    }

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {

            // Get the token for the MediaSession
            mediaBrowser.sessionToken.also { token ->

                // Create a MediaControllerCompat
                val mediaController = MediaControllerCompat(
                    this@MainActivity, // Context
                    token
                )

                // Save the controller
                MediaControllerCompat.setMediaController(this@MainActivity, mediaController)
            }

            mediaBrowser.subscribe(
                mediaBrowser.root,
                object : MediaBrowserCompat.SubscriptionCallback() {
                    override fun onChildrenLoaded(
                        parentId: String,
                        children: MutableList<MediaBrowserCompat.MediaItem>
                    ) {
                        super.onChildrenLoaded(parentId, children)

                        viewManager =
                            LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
                        viewAdapter = SongAdapter(children, this@MainActivity)

                        recyclerView = findViewById<RecyclerView>(R.id.songs).apply {
                            layoutManager = viewManager
                            adapter = viewAdapter
                        }

                        recyclerView.addItemDecoration(
                            DividerItemDecoration(
                                recyclerView.context,
                                DividerItemDecoration.VERTICAL
                            )
                        )
                    }
                })

            val mediaController = MediaControllerCompat.getMediaController(this@MainActivity)
            // Grab the view for the play/pause button
            play.setOnClickListener {
                if (mediaController.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                    mediaController.transportControls.pause()
                    play.background = getDrawable(R.drawable.ic_play)
                } else {
                    mediaController.transportControls.play()
                    play.background = getDrawable(R.drawable.ic_pause)
                }
            }
            stop.setOnClickListener {
                mediaController.transportControls.stop()
            }
            next.setOnClickListener {
                mediaController.transportControls.skipToNext()
            }
            // Register a Callback to stay in sync
            mediaController.registerCallback(controllerCallbacks)
        }
    }

    private val controllerCallbacks = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Glide.with(this@MainActivity)
                .load(metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)).centerCrop()
                .into(art)
            title.text = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            artist.text = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            controls.visibility = View.VISIBLE
        }
    }

    fun onSongClick(holder: SongViewHolder, song: MediaBrowserCompat.MediaItem) {
        holder.song.setOnClickListener {
            mediaController.transportControls.playFromMediaId(
                song.description.mediaId,
                song.description.extras
            )
        }
    }

    fun onFavoriteClick(holder: SongViewHolder, song: MediaBrowserCompat.MediaItem) {
        holder.favorite.setOnClickListener {
            if (holder.favorite.background.constantState === getDrawable(R.drawable.ic_favorite_outline).constantState) {
                holder.favorite.background = getDrawable(R.drawable.ic_favorite)
            } else {
                holder.favorite.background = getDrawable(R.drawable.ic_favorite_outline)
            }
        }
    }
}
