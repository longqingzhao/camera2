package com.blackuio.center.camera2api;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;

public class MyCaptureSessionCallback extends CameraCaptureSession.CaptureCallback {


    private final MediaRecorder mediaRecorder;
    private final ControlCamera controlCamera;
    private CaptureCall captureCall;
    private boolean isFirst = true;
    private TYPE type;
    private String TAG = "MyCaptureSessionCallback";

    public enum TYPE {
        CAPTURE, VIDEO
    }

    MyCaptureSessionCallback(CaptureCall captureCall, TYPE type) {
        this.captureCall = captureCall;
        this.type = type;
        mediaRecorder = null;
        controlCamera = null;
    }

    MyCaptureSessionCallback(MediaRecorder mediaRecorder, ControlCamera controlCamera, TYPE type) {
        this.mediaRecorder = mediaRecorder;
        this.controlCamera = controlCamera;
        this.type = type;
    }

    @Override
    public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
        super.onCaptureStarted(session, request, timestamp, frameNumber);
        Log.g(TAG, "onCaptureStarted");
    }

    @Override
    public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
        super.onCaptureProgressed(session, request, partialResult);
        Log.g(TAG, "onCaptureProgressed");
    }

    @Override
    public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
        super.onCaptureFailed(session, request, failure);
        Log.g(TAG, "onCaptureFailed");
    }

    @Override
    public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
        super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        Log.g(TAG, "onCaptureSequenceCompleted");
        if (!isFirst && TYPE.VIDEO == type && mediaRecorder != null && controlCamera != null) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            controlCamera.controlCamera();
            isFirst = true;
        } else if (type == TYPE.CAPTURE) {
            //解除lock
            captureCall.unLock();
        }
    }

    @Override
    public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
        super.onCaptureSequenceAborted(session, sequenceId);
        Log.g(TAG, "onCaptureSequenceAborted");
    }


    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(session, request, result);
        if (isFirst && type == TYPE.VIDEO && mediaRecorder != null) {
            mediaRecorder.start();
            isFirst = false;
        }
    }

}
