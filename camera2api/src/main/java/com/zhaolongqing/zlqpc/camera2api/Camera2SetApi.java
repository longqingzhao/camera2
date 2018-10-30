package com.zhaolongqing.zlqpc.camera2api;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Camera2SetApi implements ControlCamera, CameraSet {

    private static final String TAG = "Camera2SetApi";
    private static Camera2SetApi camera2ManagerApi = new Camera2SetApi();
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private WeakReference<Activity> activityWeakReference;
    private WeakReference<AutoFitTextureView> textureViewWeakReference;
    private Size size;
    private MediaRecorder mediaRecorder;
    private CameraDevice cameraDevice;
    private CameraCaptureSession mPreviewSession;
    private CaptureRequest.Builder mPreviewBuilder;
    private String absolutePath;

    public static Camera2SetApi getCamera2ManagerApi() {
        return camera2ManagerApi;
    }


    @Override
    public void initCamera(Activity activity, AutoFitTextureView autoFitTextureView) {
        this.activityWeakReference = new WeakReference<>(activity);
        this.textureViewWeakReference = new WeakReference<>(autoFitTextureView);
        autoFitTextureView.setSurfaceTextureListener(new MyTextureListener(this));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void openCamera() {
        startBackgroundThread();
        CameraManager cameraManager = (CameraManager) activityWeakReference.get().getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                size = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                textureViewWeakReference.get().setAspectRatio(size.getWidth(), size.getHeight());
                mediaRecorder = new MediaRecorder();
                cameraManager.openCamera(cameraId, new MyStateCallback(this), null);
            }
        } catch (CameraAccessException e) {
            Logger.t(TAG).e(new Throwable(), "openCamera:%s", e);
        }
    }

    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        return choices[choices.length - 1];
    }

    @Override
    public void controlCamera(boolean isRecord) {
        closePreviewSession();
        try {
            if (isRecord) {
                setUpMediaRecorder();
            }
            List<Surface> surfaces = new ArrayList<>();
            mPreviewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            SurfaceTexture surfaceTexture = textureViewWeakReference.get().getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
            Surface surface = new Surface(surfaceTexture);
            surfaces.add(surface);
            mPreviewBuilder.addTarget(surface);
            if (isRecord) {
                Surface recorderSurface = mediaRecorder.getSurface();
                surfaces.add(recorderSurface);
                mPreviewBuilder.addTarget(recorderSurface);
            }
            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            cameraDevice.createCaptureSession(surfaces,
                    new MySessionStateCallback(this,
                            mPreviewBuilder,
                            mBackgroundHandler,
                            isRecord, mediaRecorder),
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            Logger.t(TAG).e(new Throwable(), "controlCamera:%s", e);
        }

    }

    private void setUpMediaRecorder() {
        final Activity activity = activityWeakReference.get();
        if (null == activity) {
            return;
        }
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if (absolutePath == null || absolutePath.isEmpty()) {
            absolutePath = getVideoFilePath(true);
        }
        mediaRecorder.setOutputFile(absolutePath);
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(size.getWidth(), size.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Logger.t(TAG).e(new Throwable(), "setUpMediaRecorder:%s", e);
        }
    }

    private String getVideoFilePath(boolean flag) {
        final File dir = activityWeakReference.get().getExternalFilesDir(null);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(new Date());
        return (dir == null ? "" : (dir.getAbsolutePath() + "/")) + timeStamp + (flag ? ".mp4" : ".jpg");
    }

    @Override
    public File takePicture() {
        lock();
        File file = new File(getVideoFilePath(false));
        try (FileOutputStream outputPhoto = new FileOutputStream(file)) {
            textureViewWeakReference.get().getBitmap().compress(Bitmap.CompressFormat.PNG, 100, outputPhoto);
        } catch (IOException e) {
            Logger.t(TAG).e(new Throwable(), "takePicture:%s", e);
        } finally {
            unlock();
        }
        return file;
    }

    public void startRecord() {
        controlCamera(true);
    }

    @Override
    public File stopRecord() {
        mediaRecorder.stop();
        mediaRecorder.reset();
        controlCamera(false);
        return new File(absolutePath);
    }

    @Override
    public void closeCamera() {
        closePreviewSession();
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != mediaRecorder) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        stopBackgroundThread();
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (Exception e) {
            Logger.t(TAG).e(new Throwable(), "stopBackgroundThread:%s", e);
        }
    }


    @Override
    public void setCameraDevice(CameraDevice cameraDevice) {
        this.cameraDevice = cameraDevice;
    }

    @Override
    public void setSession(CameraCaptureSession cameraCaptureSession) {
        this.mPreviewSession = cameraCaptureSession;
    }

    private void lock() {
        try {
            mPreviewSession.capture(mPreviewBuilder.build(),
                    null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Logger.t(TAG).e(new Throwable(), "lock:%s", e);
        }
    }

    private void unlock() {
        try {
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(),
                    null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Logger.t(TAG).e(new Throwable(), "unlock:%s", e);
        }
    }

}
