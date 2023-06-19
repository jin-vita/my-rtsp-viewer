package org.techtown.rtspviewer.rtsp

import android.content.Context
import android.net.Uri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class RtspViewer {

    private var url: String = ""

    private lateinit var videoLayout: VLCVideoLayout

    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer

    var started = false

    var attached = false


    fun init(context: Context, videoLayout: VLCVideoLayout, listener: RtspListener?) {
        this.videoLayout = videoLayout

        libVlc = LibVLC(context)
        mediaPlayer = MediaPlayer(libVlc)

        mediaPlayer.setEventListener {
            when (it.type) {
                MediaPlayer.Event.PositionChanged -> {
                    //Log.d("Main", "MediaPlayer.Event PositionChanged...")

                    if (!started) {
                        //Log.d("Main", "MediaPlayer.Event Started...**")
                        started = true

                        listener?.onEvent(RtspEvent.STARTED)
                    }
                }
                MediaPlayer.Event.Opening -> {
                    //Log.d("Main", "MediaPlayer.Event Opening...")

                    listener?.onEvent(RtspEvent.OPENING)
                }
                MediaPlayer.Event.Buffering -> {
                    //Log.d("Main", "MediaPlayer.Event Buffering...")
                }
                MediaPlayer.Event.Playing -> {
                    //Log.d("Main", "MediaPlayer.Event Playing...")

                    listener?.onEvent(RtspEvent.PLAYING)
                }
                MediaPlayer.Event.Paused -> {
                    //Log.d("Main", "MediaPlayer.Event Paused...")

                    listener?.onEvent(RtspEvent.PAUSED)
                }
                MediaPlayer.Event.Stopped -> {
                    //Log.d("Main", "MediaPlayer.Event Stopped...**")

                    listener?.onEvent(RtspEvent.STOPPED)
                    started = false
                }
            }
        }

    }

    fun connect(url: String) {
        this.url = url

        if (!attached) {
            mediaPlayer.attachViews(videoLayout, null, false, false)
            attached = true
        }

        val media = Media(libVlc, Uri.parse(url))
        media.setHWDecoderEnabled(false, false)
        media.addOption(":network-caching=600")

        //media.setEventListener {
        //    println("media event : ${it.toString()}")
        //}

        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()

    }

    fun disconnect() {

        mediaPlayer.stop()
        mediaPlayer.detachViews()
        attached = false

    }

    fun stop() {

        mediaPlayer.stop()
        mediaPlayer.detachViews()
        attached = false

    }

    fun destroy() {

        mediaPlayer.release()
        libVlc.release()

    }
}