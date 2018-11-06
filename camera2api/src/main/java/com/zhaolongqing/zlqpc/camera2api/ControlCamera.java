package com.zhaolongqing.zlqpc.camera2api;

import android.app.Activity;

import java.io.File;

public interface ControlCamera {
    void initCamera(Activity activity, AutoFitTextureView autoFitTextureView, int cameraId);

    void openCamera();

    void controlCamera();

    void takePicture(PictureFileListener pictureFileListener);

    void startRecord();

    File stopRecord();

    void snapPicture(Activity activity, AutoFitTextureView autoFitTextureView,PictureFileListener pictureFileListener);

    void snapVideo(Activity activity, AutoFitTextureView autoFitTextureView);

    void closeCamera();

}
