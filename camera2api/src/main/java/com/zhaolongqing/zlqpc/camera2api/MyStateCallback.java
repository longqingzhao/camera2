package com.zhaolongqing.zlqpc.camera2api;

import android.hardware.camera2.CameraDevice;
import android.support.annotation.NonNull;

public class MyStateCallback extends CameraDevice.StateCallback {

    private Camera2SetApi controlCamera;

    public MyStateCallback(Camera2SetApi controlCamera) {
        this.controlCamera = controlCamera;
    }

    @Override
    public void onOpened(@NonNull CameraDevice camera) {
        controlCamera.setCameraDevice(camera);
        controlCamera.controlCamera(false);
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {

    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {

    }
}
