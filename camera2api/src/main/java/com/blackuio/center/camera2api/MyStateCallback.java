package com.blackuio.center.camera2api;

import android.hardware.camera2.CameraDevice;
import android.support.annotation.NonNull;

public class MyStateCallback extends CameraDevice.StateCallback {

    private static final String TAG = "MyStateCallback";
    private Camera2ManagerApi controlCamera;

    MyStateCallback(Camera2ManagerApi controlCamera) {
        this.controlCamera = controlCamera;
    }

    @Override
    public void onOpened(@NonNull CameraDevice camera) {
        controlCamera.setCameraDevice(camera);
        controlCamera.controlCamera();
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {
        Log.g(TAG,"onDisconnected");
    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {
        Log.g(TAG,"onError");
    }
}
