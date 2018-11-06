package com.zhaolongqing.zlqpc.camera2api;

import android.hardware.camera2.CameraDevice;
import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;

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
        Logger.t(TAG).d("onDisconnected");
    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {
        Logger.t(TAG).d("onError");
    }
}
