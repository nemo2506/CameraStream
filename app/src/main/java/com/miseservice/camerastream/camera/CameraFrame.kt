package com.miseservice.camerastream.camera

class CameraFrame(
    val nv21: ByteArray,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int
)

