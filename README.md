# camera2
这是一个使用了camera2的工具，功能是：照相，录像
使用前需要权限请求。

dependencies {
	implementation 'com.github.zhaolongqing:camera2:1.0'
	}
	
	
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}


//初始化
void initCamera(Activity activity, AutoFitTextureView autoFitTextureView, int cameraId);

//拍照
void takePicture(PictureFileListener pictureFileListener);

//开始摄像
void startRecord();

//快照
void snapPicture(Activity activity, AutoFitTextureView autoFitTextureView,PictureFileListener pictureFileListener);

//停止摄像
File stopRecord();

//快视频 
void snapVideo(Activity activity, AutoFitTextureView autoFitTextureView);

//关闭照相机
void closeCamera();
