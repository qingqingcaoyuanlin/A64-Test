package org.camera.camera;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import org.camera.encode.VideoEncoderFromBuffer2;

import java.io.IOException;
import java.util.List;

@SuppressLint("NewApi")
public class CameraWrapper {
	private static final String TAG = "CameraWrapper";
	private Camera mCamera;
	private Camera.Parameters mCameraParamters;
	private static CameraWrapper mCameraWrapper;
	private boolean mIsPreviewing = false;
	private float mPreviewRate = -1.0f;
	public static final int IMAGE_HEIGHT = 480;
	public static final int IMAGE_WIDTH = 640;
	private CameraPreviewCallback mCameraPreviewCallback;
	private byte[] mImageCallbackBuffer = new byte[CameraWrapper.IMAGE_WIDTH
	                     				* CameraWrapper.IMAGE_HEIGHT * 3 / 2];

	public interface CamOpenOverCallback {
		public void cameraHasOpened();
	}

	private CameraWrapper() {
	}

	public static synchronized CameraWrapper getInstance() {
		if (mCameraWrapper == null) {
			mCameraWrapper = new CameraWrapper();
		}
		return mCameraWrapper;
	}

	public void doOpenCamera(CamOpenOverCallback callback) {
		Log.i(TAG, "Camera open....");
        int numCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                break;
            }
        }
        if (mCamera == null) {
            Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }
		Log.i(TAG, "Camera open over....");
		callback.cameraHasOpened();
	}

	public void doStartPreview(SurfaceHolder holder, float previewRate) {
		Log.i(TAG, "doStartPreview...");
		if (mIsPreviewing) {
			this.mCamera.stopPreview();
			return;
		}

		try {
			this.mCamera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		initCamera();
	}
	
	public void doStartPreview(SurfaceTexture surface, float previewRate) {
		Log.i(TAG, "doStartPreview()");
		if (mIsPreviewing) {
			this.mCamera.stopPreview();
			return;
		}

		try {
			this.mCamera.setPreviewTexture(surface);
		} catch (IOException e) {
			e.printStackTrace();
		}
		initCamera();
	}

	public void doStopCamera() {
		Log.i(TAG, "doStopCamera");
		if (this.mCamera != null) {
			mCameraPreviewCallback.close();
			this.mCamera.setPreviewCallback(null);
			this.mCamera.stopPreview();
			this.mIsPreviewing = false;
			this.mPreviewRate = -1f;
			this.mCamera.release();
			this.mCamera = null;
		}
	}
	
	private void initCamera() {
		if (this.mCamera != null) {
			this.mCameraParamters = this.mCamera.getParameters();
			this.mCameraParamters.setPreviewFormat(ImageFormat.NV21);
			this.mCameraParamters.setFlashMode("off");

//			this.mCameraParamters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
//			this.mCameraParamters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
			this.mCameraParamters.setPreviewSize(IMAGE_WIDTH, IMAGE_HEIGHT);
			//花屏
//			this.mCameraParamters.setPreviewSize(1280, 720);
//			this.mCamera.setDisplayOrientation(90);
			mCameraPreviewCallback = new CameraPreviewCallback();
			mCamera.addCallbackBuffer(mImageCallbackBuffer);

			mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
			List<String> focusModes = this.mCameraParamters.getSupportedFocusModes();
			if (focusModes.contains("continuous-video")) {
				this.mCameraParamters
						.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			}
			this.mCamera.setParameters(this.mCameraParamters);
			this.mCamera.startPreview();
			
			this.mIsPreviewing = true;
		}
	}
	
	class CameraPreviewCallback implements Camera.PreviewCallback {
		private static final String TAG = "CameraPreviewCallback";
		private VideoEncoderFromBuffer2 videoEncoder = null;

		private CameraPreviewCallback() {
			videoEncoder = new VideoEncoderFromBuffer2(CameraWrapper.IMAGE_WIDTH,CameraWrapper.IMAGE_HEIGHT);
		}

		void close() {
			videoEncoder.close();
		}

		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
//            Log.d(TAG, "onPreviewFrame() called with:");
//			long startTime = System.currentTimeMillis();
			videoEncoder.encodeFrame(data);
//			long endTime = System.currentTimeMillis();
//			Log.i(TAG, Integer.toString((int)(endTime-startTime)) + "ms");
        //    data = null;
			camera.addCallbackBuffer(data);
//            camera.setOneShotPreviewCallback(this);
		}
	}
}
