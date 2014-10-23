package be.wouterfranken.arboardgame.rendering.tracking;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.Vector;

import org.apache.commons.collections.iterators.ArrayListIterator;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import es.ava.aruco.CameraParameters;
import es.ava.aruco.Marker;
import es.ava.aruco.MarkerDetector;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.util.Log;
import be.wouterfranken.arboardgame.R;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.utilities.AndroidUtils;
import be.wouterfranken.arboardgame.utilities.DebugUtilities;



public class CameraPose {
	
	private final static String TAG = CameraPose.class.getSimpleName();
	private final static String KEYPOINTS_NAME = "keypoints";
	private final static String DESCRIPTORS_NAME = "descriptors";
	
	private Context context;
	private MatOfDouble mv = new MatOfDouble();
	private MatOfDouble proj = new MatOfDouble();
//	private MatOfDouble camPose = new MatOfDouble();
	private MatOfPoint points = new MatOfPoint();
//	private String markerPath;
	private String cameraIntDistPath;
	
	public CameraPose(Context context) {
		this.context = context;
		
		
		try {
//			markerPath = AndroidUtils.getPathToRaw(context, R.raw.marker_br, "marker_br.jpg");
			cameraIntDistPath = AndroidUtils.getPathToRaw(context, R.raw.camera2, "cameraCalib.yml");
			storeMarkerKeypoints();
		} catch (IOException e) {
			Log.d(TAG, "Markers not correctly loaded with error "+e.getMessage());
		}
	}
	
	public void storeMarkerKeypoints() {
//		Mat img = Highgui.imread(markerPath);
//		buildAndTrainPattern(img.getNativeObjAddr(), cameraIntDistPath);
		loadCameraCalibration(cameraIntDistPath);
	}
	
	int i = 0;
	public void updateCameraPose(byte[] frameData, Camera camera) {
//		if(!AppConfig.TOUCH_EVENT){
//			camera.addCallbackBuffer(frameData);
//			return;
//		} else { // TOUCH EVENT
//			AppConfig.TOUCH_EVENT = false;
		long start = System.currentTimeMillis();
		Size size = camera.getParameters().getPreviewSize();
		Mat frameImg = new Mat();
		Mat mYuv = new Mat( size.height + size.height/2, size.width, CvType.CV_8UC1 );
		mYuv.put( 0, 0, frameData );
		Imgproc.cvtColor( mYuv, frameImg, Imgproc.COLOR_YUV2GRAY_NV21, 1);
		mYuv.release();
//			camera.addCallbackBuffer(frameData);
		FindCameraPose task = new FindCameraPose();
		task.start = start;
		task.setupFrameTrackingCallback(camera, frameData);
//			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, frameImg, cameraPose);
		task.execute(frameImg);
//			getCameraPose3(frameImg.getNativeObjAddr(),points.getNativeObjAddr(),cameraPose.getNativeObjAddr());
//			camera.addCallbackBuffer(frameData);
			
//			camera.addCallbackBuffer(frameData);
//		}
		
	}
	
	public Point[] getPoints() {
		return points.toArray();
	}
	
	public double[] getProj() {
		if(proj == null || proj.empty()) return null;
		double[] result = new double[16];
		for (int i = 0; i < result.length; i++) {
			result[i] = (double) proj.get(i/4, i%4)[0];
		}
		return result;
	}
	
	public double[] getMv() {
		if(mv == null || mv.empty()) return null;
		double[] result = new double[16];
		for (int i = 0; i < result.length; i++) {
			result[i] = (double) mv.get(i/4, i%4)[0];
		}
		return result;
	}
	
	private class FindCameraPose extends AsyncTask<Mat, Void, Void > {

		private Camera camera;
		private byte[] callbackBuffer;
		private long start;
		
		@Override
		protected Void doInBackground(Mat... params) {
			getCameraPose3( 
					params[0].getNativeObjAddr(),
					proj.getNativeObjAddr(),mv.getNativeObjAddr());
			
//			for (int i = 0; i < mvMat.size().width; i++) {
////				mv[i] = mvMat.row(i);
//				mv[i] = mvMat.get(i, 0)[0];
//				prj[i] = prjMat.get(i, 0)[0];
//			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
//			if(mv != null) {
//				Log.d(TAG, "MVP: "+mv[0]+","+mv[1]+","+mv[2]+","+mv[3]);
//				Log.d(TAG, "     "+mv[4]+","+mv[5]+","+mv[6]+","+mv[7]);
//				Log.d(TAG, "     "+mv[8]+","+mv[9]+","+mv[10]+","+mv[11]);
//				Log.d(TAG, "     "+mv[12]+","+mv[13]+","+mv[14]+","+mv[15]);
//			}
			camera.addCallbackBuffer(callbackBuffer);
			Log.d(TAG, "Done in "+(System.currentTimeMillis()-start)+"ms");
		}
		
		private void setupFrameTrackingCallback(Camera camera, byte[] callbackBuffer) {
			this.camera = camera;
			this.callbackBuffer = callbackBuffer;
		}
		
		private native boolean getCameraPose(long frameImagePtr, long pointsPtr, long cameraPosePtr);
		
	}
	
	private native void buildAndTrainPattern(long patternImgPtr, String cameraIntDistPath);
	private native boolean getCameraPose2(long frameImagePtr, long pointsPtr, long cameraPosePtr);
	private native boolean getCameraPose3(long frameImagePtr, long projMatPtr, long mvMatPtr);
	private native void loadCameraCalibration(String cameraIntDistPath);
	public native void getCameraCalibration(long camMatPtr);
}
