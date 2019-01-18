package com.blackuio.center.camera2api;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.support.annotation.NonNull;

public class MySessionStateCallback extends CameraCaptureSession.StateCallback {

    private static final String TAG = "MySessionStateCallback";
    private CameraSet cameraSet;
    private final CaptureRequest.Builder builder;
    private final Handler handler;
    static final int CONFIG = 1234;

    MySessionStateCallback(CameraSet cameraSet, CaptureRequest.Builder builder, Handler handler) {
        this.cameraSet = cameraSet;
        this.builder = builder;
        this.handler = handler;
    }

    @Override
    public void onConfigured(@NonNull CameraCaptureSession session) {
        cameraSet.setSession(session);
        try {
            session.setRepeatingRequest(builder.build(), null, handler);
            handler.sendEmptyMessage(CONFIG);
        } catch (CameraAccessException e) {
            Log.e(TAG, "onConfigured", e);
        }
    }

    @Override
    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        Log.g(TAG, "onConfigureFailed-->session:" + session);

    }
}
