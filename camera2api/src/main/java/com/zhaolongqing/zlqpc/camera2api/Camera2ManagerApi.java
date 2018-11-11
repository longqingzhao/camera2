package com.zhaolongqing.zlqpc.camera2api;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Size;
import android.view.Surface;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.zhaolongqing.zlqpc.camera2api.MySessionStateCallback.CONFIG;

public class Camera2ManagerApi implements ControlCamera, CameraSet, CaptureCall {

    private static final String TAG = "Camera2ManagerApi";
    private static Camera2ManagerApi camera2ManagerApi = new Camera2ManagerApi();
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private WeakReference<Activity> activityWeakReference;
    private WeakReference<AutoFitTextureView> textureViewWeakReference;
    private Size previewSize;
    private Size videoSize;
    private MediaRecorder mediaRecorder;
    private CameraDevice cameraDevice;
    private CameraCaptureSession mPreviewSession;
    private CaptureRequest.Builder mPreviewBuilder;
    private CaptureRequest.Builder mCaptureBuilder;
    private String absolutePath;
    private ImageReader imageReader;
    private int witchCamera;
    private PictureFileListener pictureFileListener;
    private File pictureFile;
    private Surface textureSurface;
    private boolean isSnapCapture = false;
    private boolean isSnapRecord = false;

    public static Camera2ManagerApi getCamera2ManagerApi() {
        return camera2ManagerApi;
    }


    @Override
    public void initCamera(Activity activity, AutoFitTextureView autoFitTextureView, int witchCamera) {
        this.activityWeakReference = new WeakReference<>(activity);
        this.textureViewWeakReference = new WeakReference<>(autoFitTextureView);
        autoFitTextureView.setSurfaceTextureListener(new MyTextureListener(this));
        this.witchCamera = witchCamera;
    }


    @Override
    public void snapPicture(Activity activity, AutoFitTextureView autoFitTextureView, PictureFileListener pictureFileListener, int switchCamera) {
        this.activityWeakReference = new WeakReference<>(activity);
        this.textureViewWeakReference = new WeakReference<>(autoFitTextureView);
        this.pictureFileListener = pictureFileListener;
        this.isSnapCapture = true;
        this.witchCamera = switchCamera;
        openCamera();
    }

    @Override
    public void snapVideo(Activity activity, AutoFitTextureView autoFitTextureView, int switchCamera) {
        this.activityWeakReference = new WeakReference<>(activity);
        this.textureViewWeakReference = new WeakReference<>(autoFitTextureView);
        this.isSnapRecord = true;
        this.witchCamera = switchCamera;
        openCamera();
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            pictureFile = new File(getVideoFilePath(false));
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), pictureFile));
        }

    };

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    @SuppressLint("MissingPermission")
    @Override
    public void openCamera() {
        startBackgroundThread();
        CameraManager cameraManager = (CameraManager) activityWeakReference.get().getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[witchCamera];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                previewSize = getPreviewSize(map.getOutputSizes(MediaRecorder.class));
                videoSize = Collections.max(Arrays.asList(map.getOutputSizes(MediaRecorder.class)), new CompareSizesByArea());
                textureViewWeakReference.get().setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                mediaRecorder = new MediaRecorder();
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                imageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);
                cameraManager.openCamera(cameraId, new MyStateCallback(this), null);
            }
        } catch (CameraAccessException e) {
            Logger.t(TAG).e(new Throwable(), "openCamera:%s", e);
        }


    }

    private Size getPreviewSize(Size[] outputSizes) {
        final int width = textureViewWeakReference.get().getWidth();
        Size size = null;
        for (Size s : outputSizes) {
            if (s.getWidth() > width && size == null || (size != null && size.getWidth() < s.getWidth())) {
                size = s;
            }
        }
        return size;
    }

    @Override
    public void controlCamera() {
        closePreviewSession();
        try {
            setUpMediaRecorder();
            List<Surface> surfaces = new ArrayList<>();
            mPreviewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            SurfaceTexture surfaceTexture = textureViewWeakReference.get().getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            textureSurface = new Surface(surfaceTexture);
            surfaces.add(textureSurface);
            surfaces.add(imageReader.getSurface());
            mPreviewBuilder.addTarget(textureSurface);
            Surface recorderSurface = mediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);
            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            cameraDevice.createCaptureSession(surfaces,
                    new MySessionStateCallback(this,
                            mPreviewBuilder,
                            mBackgroundHandler),
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
        absolutePath = getVideoFilePath(true);
        mediaRecorder.setOutputFile(absolutePath);
        mediaRecorder.setVideoEncodingBitRate(Integer.MAX_VALUE);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
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
    public void takePicture(PictureFileListener pictureFileListener) {
        this.pictureFileListener = pictureFileListener;
        lock();
    }

    @Override
    public void startRecord() {
        try {
            CaptureRequest.Builder mRecorderBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mRecorderBuilder.setTag("record");
            mRecorderBuilder.addTarget(mediaRecorder.getSurface());
            mRecorderBuilder.addTarget(textureSurface);
            mRecorderBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//            mPreviewSession.abortCaptures();
//            mPreviewSession.setRepeatingRequest(mRecorderBuilder.build(), new MyCaptureSessionRecorderCallback(mediaRecorder), mBackgroundHandler);
            mPreviewSession.setRepeatingBurst(Arrays.asList(mRecorderBuilder.build(), mPreviewBuilder.build()),
                    new MyCaptureSessionCallback(mediaRecorder, this, MyCaptureSessionCallback.TYPE.VIDEO), mBackgroundHandler);
        } catch (CameraAccessException e) {
            Logger.t(TAG).e(new Throwable(), "lock:%s", e);
        }
    }

    @Override
    public File stopRecord() {
        final String path = absolutePath;
        try {
            mPreviewSession.stopRepeating();
            mPreviewSession.abortCaptures();
        } catch (CameraAccessException e) {
            Logger.t(TAG).e(new Throwable(), "stopRecord:%s", e);
        }
        return new File(path);
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
        mCaptureBuilder = null;
        mPreviewBuilder = null;
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
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == CONFIG) {
                    if (isSnapCapture) {
                        lock();
                        isSnapCapture = false;
                    } else if (isSnapRecord) {
                        startRecord();
                        isSnapRecord = false;
                    }
                }
            }
        };
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (Exception e) {
                Logger.t(TAG).e(new Throwable(), "stopBackgroundThread:%s", e);
            }
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

    @Override
    public void lock() {
        try {
            if (mCaptureBuilder != null) {
                mCaptureBuilder = null;
            }
            mCaptureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureBuilder.setTag("capture");
            mCaptureBuilder.addTarget(imageReader.getSurface());
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 180);
            CaptureRequest captureRequest = mCaptureBuilder.build();
            mPreviewSession.capture(captureRequest, new MyCaptureSessionCallback(this, MyCaptureSessionCallback.TYPE.CAPTURE), mBackgroundHandler);
        } catch (CameraAccessException e) {
            Logger.t(TAG).e(new Throwable(), "lock:%s", e);
        }
    }

    @Override
    public void unLock() {
        try {
            mPreviewSession.abortCaptures();
            mPreviewSession.stopRepeating();
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
            if (pictureFile != null)
                pictureFileListener.success(pictureFile);
            else
                pictureFileListener.error();
        } catch (CameraAccessException e) {
            Logger.t(TAG).e(new Throwable(), "unlock:%s", e);
        }
    }

}
