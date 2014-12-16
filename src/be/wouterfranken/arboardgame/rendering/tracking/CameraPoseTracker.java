package be.wouterfranken.arboardgame.rendering.tracking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint3;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Process;
import android.util.Log;
import be.wouterfranken.arboardgame.R;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.gameworld.LegoBrick;
import be.wouterfranken.arboardgame.gameworld.World;
import be.wouterfranken.arboardgame.gameworld.WorldConfig;
import be.wouterfranken.arboardgame.gameworld.WorldCoordinate;
import be.wouterfranken.arboardgame.gameworld.WorldNode;
import be.wouterfranken.arboardgame.utilities.AndroidUtils;
import be.wouterfranken.arboardgame.utilities.DebugUtilities;
import be.wouterfranken.arboardgame.utilities.MathUtilities;



public class CameraPoseTracker extends Tracker{
	
	private final static String TAG = CameraPoseTracker.class.getSimpleName();
	private final static String KEYPOINTS_NAME = "keypoints";
	private final static String DESCRIPTORS_NAME = "descriptors";
	
	private Context context;
	private MatOfDouble mv = new MatOfDouble();
	private MatOfDouble proj = new MatOfDouble();
	private MatOfDouble mvExtern = new MatOfDouble();
	private MatOfDouble projExtern = new MatOfDouble();
	private Object lock = new Object();
	private Object lockExtern = new Object();
	private MatOfPoint points = new MatOfPoint();
	private Mat intrinsics;
	private String cameraIntDistPath;
	
	public CameraPoseTracker(Context context) {
		buildGrid();
		this.context = context;
		
		try {
			cameraIntDistPath = AndroidUtils.getPathToRaw(context, R.raw.camcalib, "cameraCalib.yml");
			Mat distortion = new Mat();
		    Mat camMatrix = new Mat();
			loadCameraCalibration(cameraIntDistPath,camMatrix.getNativeObjAddr(), distortion.getNativeObjAddr());
			
			float[] focal = new float[]{(float) camMatrix.get(0, 0)[0]*(AppConfig.PREVIEW_RESOLUTION[0]/4128.0f),(float) camMatrix.get(1, 1)[0]*(AppConfig.PREVIEW_RESOLUTION[1]/3096.0f)};
		    float[] principalPoint = new float[]{(float) camMatrix.get(0, 2)[0]*(AppConfig.PREVIEW_RESOLUTION[0]/4128.0f),(float) camMatrix.get(1, 2)[0]*(AppConfig.PREVIEW_RESOLUTION[1]/3096.0f)};
		    intrinsics = Mat.eye(3,3, CvType.CV_32FC1);
		    intrinsics.put(0, 0, focal[0]);
		    intrinsics.put(1, 1, focal[1]);
		    intrinsics.put(0, 2, principalPoint[0]);
		    intrinsics.put(1, 2, principalPoint[1]);
		} catch (IOException e) {
			Log.e(TAG, "Camera calibration not correctly loaded with error "+e.getMessage());
		}
	}
	
	public void updateCameraPose(Mat colFrameImg, FrameTrackingCallback trackingCallback) {
		long start = System.nanoTime();
		
		Mat grayImg = new Mat();
		Imgproc.cvtColor(colFrameImg, grayImg, Imgproc.COLOR_BGR2GRAY);
		
		if(!AppConfig.PARALLEL_POSE_ESTIMATION) {
			Mat proj = new Mat(this.proj.size(), this.proj.type());
			Mat mv = new Mat(this.mv.size(), this.mv.type());
			getCameraPose( 
						grayImg.getNativeObjAddr(),
						proj.getNativeObjAddr(),mv.getNativeObjAddr());
			
			setMv(mv);
			setProj(proj);
			trackingCallback.trackingDone(CameraPoseTracker.class);
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "CameraPose found in "+(System.nanoTime()-start)/1000000L+"ms");
		} else {
			FindCameraPose task = new FindCameraPose();
			task.start = start;
			
			task.setupFrameTrackingCallback(trackingCallback);
//			task.execute(grayImg);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, grayImg);
		}
	}
	
	public boolean cameraPoseFound() {
		synchronized (lockExtern) {
			return !mvExtern.empty();
		}
	}
	
	public Point[] getPoints() {
		return points.toArray();
	}
	
	public float[] getProj() {
		Mat tmp = new Mat();
		synchronized (lockExtern) {
			projExtern.copyTo(tmp);
		}
		if(tmp == null || tmp.empty()) return null;
		float[] result = new float[16];
		for (int i = 0; i < result.length; i++) {
			result[i] = (float) tmp.get(i/4, i%4)[0];
		}
		return result;
	}
	
	public float[] getMv() {
		Mat tmp = new Mat();
		synchronized (lockExtern) {
			mvExtern.copyTo(tmp);
		}
		if(tmp == null || tmp.empty()) return null;
		float[] result = new float[16];
		for (int i = 0; i < result.length; i++) {
			result[i] = (float) tmp.get(i/4, i%4)[0];
		}
		return result;
	}
	
	public float[] get3DPointFrom2D(float x, float y, float finalZ) {
	    Mat glMv = new Mat();
	    Mat tmp2 = new Mat();
	    synchronized (lockExtern) {
	    	mvExtern.copyTo(tmp2);
	    }
	    if(tmp2 == null || tmp2.empty()) return null;
	    Core.transpose(tmp2, glMv);
	    glMv.convertTo(glMv, CvType.CV_32FC1);
	    Mat vector2D = Mat.ones(4,1,CvType.CV_32FC1);
	    Mat vector3D = Mat.zeros(4,1,CvType.CV_32FC1);
	    
	    // Set Extrinsics
	    Mat extrinsics = Mat.zeros(3,4, CvType.CV_32FC1);
	    glMv.row(0).copyTo(extrinsics.row(0));
	    glMv.row(1).copyTo(extrinsics.row(1));
	    // Necessary, because glMv is already transformed for OpenGL.
	    Core.multiply(glMv.row(2), new Scalar(-1), extrinsics.row(2));
	    
	    Mat tmp = Mat.zeros(3,4,CvType.CV_32FC1);
	    Core.gemm(intrinsics,extrinsics,1,new Mat(),0,tmp,0);
	    Mat square = Mat.eye(4, 4,CvType.CV_32FC1);
	    tmp.row(0).copyTo(square.row(0));
	    tmp.row(1).copyTo(square.row(1));
	    tmp.row(2).copyTo(square.row(2));
	    
	    
	    Mat squareInverted = Mat.eye(4, 4,CvType.CV_32FC1);
	    Core.invert(square, squareInverted);
	    
	    vector2D.put(0, 0, x);
	    vector2D.put(1, 0, y);
	    // Knowing that z has to be finalZ, we can calculate the correct value for w!
	    double a = (squareInverted.get(2,0)[0]*x+squareInverted.get(2,1)[0]*y+squareInverted.get(2,2)[0]);
	    double b = (squareInverted.get(3,0)[0]*x+squareInverted.get(3,1)[0]*y+squareInverted.get(3,2)[0]);
	    double newW = (finalZ*b-a)/(squareInverted.get(2,3)[0]-finalZ*squareInverted.get(3,3)[0]);
	    vector2D.put(3, 0, newW);
	    
	    Core.gemm(squareInverted, vector2D, 1, new Mat(), 1, vector3D, 0);
	    Core.multiply(vector3D, new Scalar(1.0/vector3D.get(3, 0)[0]), vector3D);
	    
	    return new float[]{(float) vector3D.get(0,0)[0],(float) vector3D.get(1,0)[0],(float) vector3D.get(2,0)[0]};
	}
	
	public Mat get2DPointFrom3D(Mat points3D) {
	    Mat glMv = new Mat();
	    Mat tmp2 = new Mat();
	    synchronized (lockExtern) {
	    	mvExtern.copyTo(tmp2);
	    }
	    if(tmp2 == null || tmp2.empty()) return null;
	    Core.transpose(tmp2, glMv);
	    glMv.convertTo(glMv, CvType.CV_32FC1);
	    Mat points2D = Mat.ones(3,points3D.cols(),CvType.CV_32FC1);
//	    Mat vector3D = Mat.ones(4,1,CvType.CV_32FC1);
//	    vector3D.put(0,0,x);
//	    vector3D.put(1,0,y);
//	    vector3D.put(2,0,z);
	    
	    // Set Extrinsics
	    Mat extrinsics = Mat.zeros(3,4, CvType.CV_32FC1);
	    glMv.row(0).copyTo(extrinsics.row(0));
	    glMv.row(1).copyTo(extrinsics.row(1));
	    // Necessary, because glMv is already transformed for OpenGL.
	    Core.multiply(glMv.row(2), new Scalar(-1), extrinsics.row(2));
	    
	    Mat tmp = Mat.zeros(3,4,CvType.CV_32FC1);
	    Core.gemm(intrinsics,extrinsics,1,new Mat(),0,tmp,0);
//	    Log.d(TAG, "Points3D Cols: "+points3D.cols()+", Rows: "+points3D.rows());
	    Core.gemm(tmp,points3D,1,new Mat(),0,points2D,0);
//	    Log.d(TAG, "Points2D Cols: "+points2D.cols()+", Rows: "+points2D.rows());
	    
	    return points2D;
	}
	
	private Mat grid = new Mat();
	private void buildGrid(){
		int i = 0;
		int matSize = (int)((WorldConfig.BORDER.getXEnd()-WorldConfig.BORDER.getXStart()+WorldConfig.NODE_DISTANCE)/WorldConfig.NODE_DISTANCE
								*(WorldConfig.BORDER.getYEnd()-WorldConfig.BORDER.getYStart()+WorldConfig.NODE_DISTANCE)/WorldConfig.NODE_DISTANCE);
		grid = new Mat(4,matSize,CvType.CV_32FC1);
		for(float x = WorldConfig.BORDER.getXStart();x<=WorldConfig.BORDER.getXEnd();x+=WorldConfig.NODE_DISTANCE) {
			for(float y = WorldConfig.BORDER.getYStart();y<=WorldConfig.BORDER.getYEnd();y+=WorldConfig.NODE_DISTANCE) {
//				Log.d(TAG, "Points3D Grid point added...");
				grid.put(0, i, x);
				grid.put(1, i, y);
				grid.put(2, i, 0);
				grid.put(3, i++, 1);
			}
		}
		Log.d(TAG, "Matsize: "+matSize+",i:"+i);
//		tmp.copyTo(grid);
	}
	
	
	public List<float[]> calculateImageGrid(Mat image, World w) {
		List<float[]> result = new ArrayList<float[]>();
		Mat newImage = new Mat();
		
		image.copyTo(newImage);
		synchronized (lockExtern) {
			if(mvExtern == null || mvExtern.empty() || grid.empty()) return null;
			List<LegoBrick> bricks = w.getActiveBricks();
			Mat gridPts2D = get2DPointFrom3D(grid);
			boolean inBrick = false;
			for (int i = 0; i < gridPts2D.cols(); i++) {
				gridPts2D.put(0, i, gridPts2D.get(0,i)[0]/gridPts2D.get(2,i)[0]);
				gridPts2D.put(1, i, gridPts2D.get(1,i)[0]/gridPts2D.get(2,i)[0]);
				
				for (LegoBrick b : bricks) {
					if(b.hasCoordinate(new WorldCoordinate((float)grid.get(0, i)[0], (float)grid.get(1, i)[0]))) {
						inBrick = true;
						break;
					}
				}
				if(!inBrick && gridPts2D.get(0,i)[0] >= 0 && gridPts2D.get(0,i)[0] <= AppConfig.PREVIEW_RESOLUTION[0] 
						&& gridPts2D.get(1,i)[0] >= 0 && gridPts2D.get(1,i)[0] <= AppConfig.PREVIEW_RESOLUTION[1]) {
					result.add(new float[]{(float) gridPts2D.get(0,i)[0],(float) gridPts2D.get(1,i)[0]});
					Core.circle(newImage, new Point(gridPts2D.get(0,i)[0], gridPts2D.get(1,i)[0]), 3, new Scalar(1, 0, 0));
				}
				inBrick = false;
			}
		}
		Highgui.imwrite("/sdcard/arbg/gridImg.png", newImage);
		Highgui.imwrite("/sdcard/arbg/origImg.png", image);
		return result;//(float[][])result.toArray();
	}
	
	public float[] getCameraPosition() {
		Mat glMv = new Mat();
	    Mat tmp2 = new Mat();
	    synchronized (lockExtern) {
	    	mvExtern.copyTo(tmp2);
		}
	    if(tmp2 == null || tmp2.empty()) return null;
	    Core.transpose(tmp2, glMv);
	    glMv.convertTo(glMv, CvType.CV_32FC1);
	    
	    Mat extrinsics = Mat.zeros(3,4, CvType.CV_32FC1);
	    glMv.row(0).copyTo(extrinsics.row(0));
	    glMv.row(1).copyTo(extrinsics.row(1));
	    Core.multiply(glMv.row(2), new Scalar(-1), extrinsics.row(2));
	    
	    Mat rotation = extrinsics.rowRange(0, 3).colRange(0, 3).t();
	    Mat translation = extrinsics.rowRange(0, 3).col(3);
	    
	    Mat camPosition = Mat.zeros(3,1,CvType.CV_32FC1);
	    Core.multiply(rotation, new Scalar(-1), rotation);
	    Core.gemm(rotation,translation,1,new Mat(),0,camPosition,0);
	    
	    return new float[]{(float) camPosition.get(0, 0)[0],(float) camPosition.get(1, 0)[0],(float) camPosition.get(2, 0)[0]};
	}
	
	public void frameTick() {
		synchronized (lock) {
			synchronized (lockExtern) {
				mv.copyTo(mvExtern);
				proj.copyTo(projExtern);
			}
		}
	}
	
	private void setMv(Mat newMv) {
		synchronized (lock) {
			newMv.copyTo(mv);
		}
	}
	
	private void setProj(Mat newProj) {
		synchronized (lock) {
			newProj.copyTo(proj);
		}
	}
	
	private class FindCameraPose extends AsyncTask<Mat, Void, Void > {
		private FrameTrackingCallback trackingCallback;
		private long start;
		private Mat proj;
		private Mat mv;
		
		public FindCameraPose() {
			synchronized (lock) {
				proj = new Mat(CameraPoseTracker.this.proj.size(), CameraPoseTracker.this.proj.type());
				mv = new Mat(CameraPoseTracker.this.mv.size(), CameraPoseTracker.this.mv.type());
			}
		}
		
		@Override
		protected Void doInBackground(Mat... params) {
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "CameraPose preprocessing done in "+(System.nanoTime()-start)+"ms");
			long startCalculation = System.nanoTime();
			getCameraPose( 
					params[0].getNativeObjAddr(),
					this.proj.getNativeObjAddr(),this.mv.getNativeObjAddr());
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "CameraPose calculations done in "+(System.nanoTime()-startCalculation)+"ms");
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			long startSettingMatrices = System.nanoTime();
			setMv(this.mv);
			setProj(this.proj);
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "CameraPose matrices set in "+(System.nanoTime()-startSettingMatrices)+"ms");
			this.trackingCallback.trackingDone(CameraPoseTracker.class);
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "CameraPose found in "+(System.nanoTime()-start)+"ms");
		}
		
		private void setupFrameTrackingCallback(FrameTrackingCallback trackingCallback) {
			this.trackingCallback = trackingCallback;
		}
	}
	
	private native boolean getCameraPose(long frameImagePtr, long projMatPtr, long mvMatPtr);
	private native void loadCameraCalibration(String cameraIntDistPath,long cameraMat, long distortion);
	public native void getCalibrationData(long cameraMat, long distortion);
}
