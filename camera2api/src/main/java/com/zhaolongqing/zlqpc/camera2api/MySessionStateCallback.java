package com.zhaolongqing.zlqpc.camera2api;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;

public class MySessionStateCallback extends CameraCaptureSession.StateCallback {

    private static final String TAG = "MySessionStateCallback";
    private CameraManager cameraManager;
    private final CaptureRequest.Builder builder;
    private final Handler handler;
    private boolean isRecord;
    private MediaRecorder mediaRecorder;

    public MySessionStateCallback(CameraManager cameraManager, CaptureRequest.Builder builder, Handler handler, boolean isRecord, MediaRecorder mediaRecorder) {
        this.cameraManager = cameraManager;
        this.builder = builder;
        this.handler = handler;
        this.isRecord = isRecord;
        this.mediaRecorder = mediaRecorder;
    }

    @Override
    public void onConfigured(@NonNull CameraCaptureSession session) {
        cameraManager.setSession(session);
        try {
            session.setRepeatingRequest(builder.build(), null, handler);
        } catch (CameraAccessException e) {
            Logger.t(TAG).e(new Throwable(), "onConfigured:%s", e);
        }
        if (isRecord)
            mediaRecorder.start();
    }

    @Override
    public void onConfigureFailed(@NonNull CameraCaptureSession session) {

    }
}
