package com.miseservice.camerastream.data.streaming

interface StreamingRuntime {
    fun start(port: Int)
    fun stop()
    fun switchCamera()
    fun onAppBackgroundChanged(isInBackground: Boolean)
}

