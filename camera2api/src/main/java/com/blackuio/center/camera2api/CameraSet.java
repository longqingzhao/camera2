package com.blackuio.center.camera2api;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;

public interface CameraSet {

    void setCameraDevice(CameraDevice cameraDevice);
    void setSession(CameraCaptureSession cameraCaptureSession);

}
