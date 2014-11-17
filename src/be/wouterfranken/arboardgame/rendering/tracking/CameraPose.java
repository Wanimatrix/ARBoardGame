package be.wouterfranken.arboardgame.rendering.tracking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.util.Log;
import be.wouterfranken.arboardgame.R;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.utilities.AndroidUtils;



public class CameraPose extends Tracker{
	
	private final static String TAG = CameraPose.class.getSimpleName();
	private final static String KEYPOINTS_NAME = "keypoints";
	private final static String DESCRIPTORS_NAME = "descriptors";
	
	private Context context;
	private MatOfDouble mv = new MatOfDouble();
	private MatOfDouble proj = new MatOfDouble();
	private MatOfPoint points = new MatOfPoint();
	private String cameraIntDistPath;
	
	public CameraPose(Context context) {
		this.context = context;
		
		try {
			cameraIntDistPath = AndroidUtils.getPathToRaw(context, R.raw.camcalib, "cameraCalib.yml");
			loadCameraCalibration(cameraIntDistPath);
		} catch (IOException e) {
			Log.e(TAG, "Camera calibration not correctly loaded with error "+e.getMessage());
		}
	}
	
	public void updateCameraPose(Mat colFrameImg, FrameTrackingCallback trackingCallback) {
		long start = System.currentTimeMillis();
		
		Mat grayImg = new Mat();
		Imgproc.cvtColor(colFrameImg, grayImg, Imgproc.COLOR_BGR2GRAY);
		
		FindCameraPose task = new FindCameraPose();
		task.start = start;
		
		task.setupFrameTrackingCallback(trackingCallback);
//		task.execute(grayImg);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, grayImg);
	}
	
	public Point[] getPoints() {
		return points.toArray();
	}
	
	public double[] getProj() {
		Mat tmp = new Mat();
		proj.copyTo(tmp);
		if(tmp == null || tmp.empty()) return null;
		double[] result = new double[16];
		for (int i = 0; i < result.length; i++) {
			result[i] = (double) tmp.get(i/4, i%4)[0];
		}
		return result;
	}
	
	public double[] getMv() {
		Mat tmp = new Mat();
		mv.copyTo(tmp);
		if(tmp == null || tmp.empty()) return null;
		double[] result = new double[16];
		for (int i = 0; i < result.length; i++) {
			result[i] = (double) tmp.get(i/4, i%4)[0];
		}
		return result;
	}
	
	private class FindCameraPose extends AsyncTask<Mat, Void, Void > {
		private FrameTrackingCallback trackingCallback;
		private long start;
		
		@Override
		protected Void doInBackground(Mat... params) {
			getCameraPose( 
					params[0].getNativeObjAddr(),
					proj.getNativeObjAddr(),mv.getNativeObjAddr());
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			this.trackingCallback.trackingDone(CameraPose.class);
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "CameraPose found in "+(System.currentTimeMillis()-start)+"ms");
		}
		
		private void setupFrameTrackingCallback(FrameTrackingCallback trackingCallback) {
			this.trackingCallback = trackingCallback;
		}
	}
	
	private native boolean getCameraPose(long frameImagePtr, long projMatPtr, long mvMatPtr);
	private native void loadCameraCalibration(String cameraIntDistPath);
	private native void getCalibrationData(long cameraMat, long distortion);
}
