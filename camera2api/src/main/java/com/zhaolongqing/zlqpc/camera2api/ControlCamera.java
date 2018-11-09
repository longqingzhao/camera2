package com.zhaolongqing.zlqpc.camera2api;

import android.app.Activity;

import java.io.File;

public interface ControlCamera {

    //初始化
    void initCamera(Activity activity, AutoFitTextureView autoFitTextureView, int cameraId);

    //拍照
    void takePicture(PictureFileListener pictureFileListener);

    //开始摄像
    void startRecord();

    //快照
    void snapPicture(Activity activity, AutoFitTextureView autoFitTextureView,PictureFileListener pictureFileListener,int switchCamera);

    //停止摄像
    File stopRecord();

    //快视频
    void snapVideo(Activity activity, AutoFitTextureView autoFitTextureView,int switchCamera);

    //关闭照相机
    void closeCamera();


    void openCamera();

    void controlCamera();
}
