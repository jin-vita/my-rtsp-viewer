package org.techtown.rtspviewer.rtsp

interface RtspListener {

    fun onEvent(event: RtspEvent)

}