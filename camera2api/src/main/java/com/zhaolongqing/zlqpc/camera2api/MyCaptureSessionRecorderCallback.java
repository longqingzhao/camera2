package com.zhaolongqing.zlqpc.camera2api;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.view.Surface;

import com.orhanobut.logger.Logger;

import java.io.IOException;

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
    public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
        super.onCaptureStarted(session, request, timestamp, frameNumber);
        Logger.t(TAG).d("%s", "onCaptureStarted");
    }

    @Override
    public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
        super.onCaptureProgressed(session, request, partialResult);
        Logger.t(TAG).d("%s", "onCaptureProgressed");
    }

    @Override
    public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
        super.onCaptureFailed(session, request, failure);
        Logger.t(TAG).d("%s", "onCaptureFailed");
    }

    @Override
    public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
        super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        Logger.t(TAG).d("%s", "onCaptureSequenceCompleted");
        if (!isFirst) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            controlCamera.controlCamera();
            isFirst = true;
        }
    }

    @Override
    public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
        super.onCaptureSequenceAborted(session, sequenceId);
        Logger.t(TAG).d("%s", "onCaptureSequenceAborted");
        if (!isFirst) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            controlCamera.controlCamera();
            isFirst = true;
        }
    }

    @Override
    public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
        super.onCaptureBufferLost(session, request, target, frameNumber);
        Logger.t(TAG).d("%s", "onCaptureBufferLost");
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(session, request, result);
        Logger.t(TAG).d("%s", "onCaptureCompleted");
        if (isFirst) {
            mediaRecorder.start();
            isFirst = false;
        }
    }

}
