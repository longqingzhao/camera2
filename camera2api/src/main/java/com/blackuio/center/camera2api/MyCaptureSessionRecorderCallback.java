package com.blackuio.center.camera2api;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.MediaRecorder;


public class MyCaptureSessionRecorderCallback extends CameraCaptureSession.CaptureCallback {


    private String TAG = "MyCaptureSessionRecorderCallback";
    private boolean isFirst = true;
    private MediaRecorder mediaRecorder;
    private ControlCamera controlCamera;

    MyCaptureSessionRecorderCallback(MediaRecorder mediaRecorder, ControlCamera controlCamera) {
        this.mediaRecorder = mediaRecorder;
        this.controlCamera = controlCamera;
    }

    @Override
    public void onCaptureStarted( CameraCaptureSession session,  CaptureRequest request, long timestamp, long frameNumber) {
        super.onCaptureStarted(session, request, timestamp, frameNumber);
        Log.g(TAG, "onCaptureStarted");
    }

    @Override
    public void onCaptureProgressed(CameraCaptureSession session,  CaptureRequest request,  CaptureResult partialResult) {
        super.onCaptureProgressed(session, request, partialResult);
        Log.g(TAG, "onCaptureProgressed");
    }

    @Override
    public void onCaptureFailed( CameraCaptureSession session,  CaptureRequest request,  CaptureFailure failure) {
        super.onCaptureFailed(session, request, failure);
        Log.g(TAG, "onCaptureFailed");
    }

    @Override
    public void onCaptureSequenceCompleted( CameraCaptureSession session, int sequenceId, long frameNumber) {
        super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        Log.g(TAG, "onCaptureSequenceCompleted");
        if (!isFirst) {
            mediaRecorder.stop();
            mediaRecorder.reset();
//            controlCamera.controlCamera();
            isFirst = true;
        }
    }

    @Override
    public void onCaptureSequenceAborted( CameraCaptureSession session, int sequenceId) {
        super.onCaptureSequenceAborted(session, sequenceId);
        Log.g(TAG, "onCaptureSequenceAborted");
        if (!isFirst) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            controlCamera.controlCamera();
            isFirst = true;
        }
    }


    @Override
    public void onCaptureCompleted( CameraCaptureSession session,  CaptureRequest request,  TotalCaptureResult result) {
        super.onCaptureCompleted(session, request, result);
        Log.g(TAG, "onCaptureCompleted");
        if (isFirst) {
            mediaRecorder.start();
            isFirst = false;
        }
    }

}
