package com.zhaolongqing.zlqpc.camera2api;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;

public interface CameraManager {

    void setCameraDevice(CameraDevice cameraDevice);
    void setSession(CameraCaptureSession cameraCaptureSession);

}
