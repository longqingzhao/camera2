package com.blackuio.center.camera2api;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
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
import android.os.Looper;
import android.os.Message;
import android.util.Size;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
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

import static com.blackuio.center.camera2api.MySessionStateCallback.CONFIG;

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

    //状态
    private boolean isSessionOk = false;
    private boolean isRecord;
    private CaptureRequest.Builder mRecorderBuilder;

    public static Camera2ManagerApi getCamera2ManagerApi() {
        return camera2ManagerApi;
    }

    @Override
    public void initCamera(Activity activity, AutoFitTextureView autoFitTextureView, int witchCamera) {
        this.activityWeakReference = new WeakReference<>(activity);
        this.textureViewWeakReference = new WeakReference<>(autoFitTextureView);
        this.witchCamera = witchCamera;
        autoFitTextureView.setSurfaceTextureListener(new MyTextureListener(this));
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

    public boolean isSessionOk() {
        return isSessionOk;
    }

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
            try (FileOutputStream output = new FileOutputStream(mFile)) {
                output.write(bytes);
            } catch (Exception e) {
                Log.e(TAG, "ImageSaver", e);
                camera2ManagerApi.pictureFileListener.error();
            } finally {
                mImage.close();
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
            if (cameraManager != null) {
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
                    imageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
                    cameraManager.openCamera(cameraId, new MyStateCallback(this), null);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "openCamera", e);
            if (isSnapCapture) {
                pictureFileListener.error();
            }
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
            List<Surface> surfaces = new ArrayList<>();
            mPreviewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            SurfaceTexture surfaceTexture = textureViewWeakReference.get().getSurfaceTexture();
            if (surfaceTexture != null) {
                surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                textureSurface = new Surface(surfaceTexture);
                surfaces.add(textureSurface);
                surfaces.add(imageReader.getSurface());
                mPreviewBuilder.addTarget(textureSurface);
                mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                cameraDevice.createCaptureSession(surfaces, new MySessionStateCallback(this, mPreviewBuilder, mBackgroundHandler), mBackgroundHandler);
            } else {
                if (pictureFileListener != null)
                    pictureFileListener.error();
            }
        } catch (Exception e) {
            Log.e(TAG, "controlCamera", e);
            if (isSnapCapture) {
                pictureFileListener.error();
            }
        }

    }

    private void setUpMediaRecorder() {
        final Activity activity = activityWeakReference.get();
        if (null == activity) {
            return;
        }
        if (mediaRecorder != null) {
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
            } catch (Exception e) {
                Log.e(TAG, "setUpMediaRecorder", e);
                if (isSnapCapture) {
                    pictureFileListener.error();
                }
            }
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
            closePreviewSession();
            setUpMediaRecorder();
            mRecorderBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mRecorderBuilder.addTarget(mediaRecorder.getSurface());
            SurfaceTexture surfaceTexture = textureViewWeakReference.get().getSurfaceTexture();
            if (surfaceTexture != null && previewSize.getHeight() < previewSize.getWidth()) {
                surfaceTexture.setDefaultBufferSize(previewSize.getHeight(), previewSize.getWidth());
                textureSurface = new Surface(surfaceTexture);
            }
            mRecorderBuilder.addTarget(textureSurface);
            mRecorderBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            cameraDevice.createCaptureSession(Arrays.asList(mediaRecorder.getSurface(), textureSurface), new MySessionStateCallback(this, mRecorderBuilder, mBackgroundHandler), mBackgroundHandler);
            isRecord = true;
        } catch (Exception e) {
            Log.e(TAG, "startRecord", e);
            if (isSnapCapture) {
                pictureFileListener.error();
            }
        }
    }

    @Override
    public synchronized File stopRecord() {
        Log.g(TAG, "textureView--surface:" + textureViewWeakReference.get().toString());
        if (mPreviewSession != null && mPreviewBuilder != null && mBackgroundHandler != null) {
            isRecord = false;
            final String path = absolutePath;
            try {
//                mPreviewSession.stopRepeating();
                controlCamera();
//                mPreviewSession.abortCaptures();
            } catch (Exception e) {
                Log.e(TAG, "stopRecord", e);
            }
            isSnapRecord = false;
            return new File(path);
        }
        isSnapRecord = false;
        return null;
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

    @Override
    public void cameraAngle(int angle) {

    }

    private synchronized void closePreviewSession() {
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
            public synchronized void handleMessage(Message msg) {
                if (msg.what == CONFIG) {
                    if (isSnapCapture && !isSnapRecord) {
                        if (imageReader.getSurface() != null)
                            lock();
                        else
                            pictureFileListener.error();
                    } else if (isSnapRecord && !isSnapCapture) {
                        startRecord();
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
                Log.e(TAG, "stopBackgroundThread", e);
                if (isSnapCapture) {
                    pictureFileListener.error();
                }
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
        isSessionOk = true;
        if (isRecord) {
            try {
                mRecorderBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                mRecorderBuilder.setTag("record");
                mRecorderBuilder.addTarget(textureSurface);
                mRecorderBuilder.addTarget(mediaRecorder.getSurface());
                mRecorderBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                mPreviewSession.setRepeatingBurst(Collections.singletonList(mRecorderBuilder.build()), new MyCaptureSessionRecorderCallback(mediaRecorder, this), mBackgroundHandler);
            } catch (Exception e) {
                Log.e(TAG, "CameraAccessException", e);
            }
        }
    }

    @Override
    public void lock() {
        try {
            if (mCaptureBuilder == null) {
                mCaptureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                mCaptureBuilder.setTag("capture");
                mCaptureBuilder.addTarget(imageReader.getSurface());
            }
//            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);
            CaptureRequest captureRequest = mCaptureBuilder.build();
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
            mPreviewSession.stopRepeating();
            mPreviewSession.capture(captureRequest, new MyCaptureSessionCallback(this, MyCaptureSessionCallback.TYPE.CAPTURE), mBackgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "lock", e);
            pictureFileListener.error();
        }
    }

    @Override
    public void unLock() {
        try {
//            mPreviewSession.abortCaptures();
//            mPreviewSession.stopRepeating();
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (pictureFile != null)
                        pictureFileListener.success(pictureFile);
                    else
                        pictureFileListener.error();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "unLock", e);
            pictureFileListener.error();
        }
        isSnapCapture = false;
    }

}
