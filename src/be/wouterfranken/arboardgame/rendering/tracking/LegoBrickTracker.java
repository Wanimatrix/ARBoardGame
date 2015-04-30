package be.wouterfranken.arboardgame.rendering.tracking;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import android.os.AsyncTask;
import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.gameworld.LegoBrick;
import be.wouterfranken.arboardgame.gameworld.WorldConfig;
import be.wouterfranken.arboardgame.utilities.Color;
import be.wouterfranken.arboardgame.utilities.DebugUtilities;
import be.wouterfranken.arboardgame.utilities.MathUtilities;
import be.wouterfranken.experiments.TimerManager;

public class LegoBrickTracker extends Tracker{
	private static final String TAG = LegoBrickTracker.class.getSimpleName();
	
	private Mat contour = new Mat();
	private Mat threshold = new Mat();
	private List<Color> contourColors = new ArrayList<Color>();
	private Mat contourExtern = new Mat();
	private Mat thresholdExtern = new Mat();
	private Object lock = new Object();
	private Object lockExtern = new Object();
	
	public void findLegoBrick(Mat yuvFrameImage) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG,"Legobrick tracking ...");
		
		long start = System.nanoTime();
		TimerManager.start("BrickDetection", "BrickTracking", "/sdcard/arbg/oldTimeBrickTrack.txt");
		Mat threshold = new Mat();
		Mat contour = new Mat();
//		FindLegoBrick task = new FindLegoBrick();
//		task.start = System.nanoTime();
		
		findLegoBrick( 
				yuvFrameImage.getNativeObjAddr(),
				contour.getNativeObjAddr()
				);
//		findLegoBrick2( 
//				yuvFrameImage.getNativeObjAddr(),
//				threshold.getNativeObjAddr()
//				);
//		setThreshold(threshold);
		setContour(contour);
//		trackingCallback.trackingDone(LegoBrickTracker.class);
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "LegoBrick found in "+(System.nanoTime()-start)/1000000L+"ms");
		TimerManager.stop();
		
//		task.setupFrameTrackingCallback(trackingCallback);
//		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, yuvFrameImage);
	}
	
//	public float[][] getGLContour() {
//		Mat tmp = new Mat();
//		synchronized (lockExtern) {
//			contourExtern.copyTo(tmp);
//		}
//		if(tmp.empty()) return null;
//		float[][] result = new float[tmp.rows()][];
//		for (int j = 0; j < tmp.rows(); j++) {
//			int cornerAmount = (int) tmp.get(j,1)[0];
//			result[j] = new float[cornerAmount*3];
//			for (int i = 0; i < cornerAmount*3; i+=3) {
//				if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Point "+i/3+": ("+tmp.get(j,2*i/3+2)[0]+","+tmp.get(j,2*i/3+3)[0]+")");
//				result[j][i] = (float) ((tmp.get(j,2*i/3+2)[0]/AppConfig.PREVIEW_RESOLUTION[0])*2-1);
//				result[j][i+1] = (float) ((tmp.get(j,2*i/3+3)[0]/AppConfig.PREVIEW_RESOLUTION[1])*2-1);
//				result[j][i+2] = 0;
//			}
//		}
//		return result;
//	}
	
	public float[][] getOcvContour() {
		Mat tmp = new Mat();
		synchronized (lockExtern) {
			contourExtern.copyTo(tmp);
		}
		if(tmp.empty()) return null;
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Contours found: "+tmp.rows());
		float[][] result = new float[tmp.rows()][];
		for (int j = 0; j < tmp.rows(); j++) {
			int cornerAmount = (int) tmp.get(j,1)[0];
			result[j] = new float[cornerAmount*3+1];
			result[j][0] = (int) tmp.get(j,0)[0];
			for (int i = 0; i < cornerAmount*3; i+=3) {
				if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Point "+i/3+": ("+tmp.get(j,2*i/3+2)[0]+","+tmp.get(j,2*i/3+3)[0]+")");
				result[j][i+1] = (float) (tmp.get(j,2*i/3+2)[0]);
				result[j][i+2] = (float) (tmp.get(j,2*i/3+3)[0]);
				result[j][i+3] = 0;
			}
		}
		return result;
	}
	
	public LegoBrick[] getLegoBricks(CameraPoseTracker camPose) {
	    float[][] ocvContours = getOcvContour();
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "NEW CORNERS!!!");
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "--------------");
	    LegoBrick[] bricks;
	    if(ocvContours != null) {
	    	float[][][] cuboid = new float[ocvContours.length][8][3];
	    	float[][] corners3D;
		    float[][] corners3D1;
		    float[] point3D1;
		    float[] point3D;
	    	bricks = new LegoBrick[ocvContours.length];
	    	Log.d(TAG, "Amount of contours: "+ocvContours.length);
	    	for (int k = 0; k < ocvContours.length; k++) {
		    	corners3D = new float[(ocvContours[k].length-1)/3][4];
		    	corners3D1 = new float[(ocvContours[k].length-1)/3][4];
		    	
		    	float minDistance = Float.POSITIVE_INFINITY;
		    	int minDistanceCorner=-1;
		    	for (int i = 0; i < (ocvContours[k].length-1)/3; i++) {
		    		point3D = camPose.get3DPointFrom2D(ocvContours[k][i*3+1], ocvContours[k][i*3+2],0);
		    		point3D1 = camPose.get3DPointFrom2D(ocvContours[k][i*3+1], ocvContours[k][i*3+2],1);
		    		if(point3D == null || point3D1 == null) break;
		    		corners3D[i][0] = point3D[0];
		    		corners3D[i][1] = point3D[1];
		    		corners3D[i][2] = point3D[2];
		    		corners3D[i][3] = 1;
		    		corners3D1[i][0] = point3D1[0];
		    		corners3D1[i][1] = point3D1[1];
		    		corners3D1[i][2] = point3D1[2];
		    		corners3D1[i][3] = 1;
		    		float[] cameraLocation = camPose.getCameraPosition();
		    		if(AppConfig.DEBUG_LOGGING) DebugUtilities.logGLMatrix("3D Corner ("+i+")", corners3D[i], 1, 4);
		    		if(AppConfig.DEBUG_LOGGING) DebugUtilities.logGLMatrix("3D1 Corner ("+i+")", corners3D1[i], 1, 4);
		    		if(cameraLocation != null) {
		    			float distance = MathUtilities.distance(corners3D[i][0], corners3D[i][1], cameraLocation[0], cameraLocation[1]);
		    			if(distance < minDistance) {
		    				minDistance = distance;
		    				minDistanceCorner = i;
		    			}
		    			if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Corner distance: "+distance);
		    		}
				}
		    	
		    	float minAngle = 360;
		    	float minDistanceAngle = 360;
		    	int[] cornersMinAngle = new int[3];
		    	for (int i = 0; i < corners3D.length; i++) {
		    		float angle = MathUtilities.angle(corners3D[i], corners3D[(i+1)%corners3D.length], corners3D[(i+2)%corners3D.length]);
		    		if(Math.abs(90-angle) < minDistanceAngle){
		    			minDistanceAngle = Math.abs(90-angle);
		    			minAngle = angle;
		    			cornersMinAngle = new int[]{i,(i+1)%corners3D.length,(i+2)%corners3D.length};
		    		}
		    		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "ANGLE Angle between corners ("+i+","+(i+1)%corners3D.length+","+(i+2)%corners3D.length+"): "+angle);
				}
		    	
		    	if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Minimum Distance to camera: "+minDistance);
		    	if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Minimum Angle: "+minAngle);
		    	if(minDistance != 0 && minAngle != 0) {
		    		float[][] corners;
		    		
		    		if(cornersMinAngle[0] == minDistanceCorner || cornersMinAngle[1] == minDistanceCorner || cornersMinAngle[2] == minDistanceCorner) {
		    			corners = corners3D;
		    		} else {
		    			corners = corners3D1;
		    		}
		    		
		    		if(corners.length < 3) continue;
		    		
		    		float[] vec1 = MathUtilities.vector(corners[cornersMinAngle[1]], corners[cornersMinAngle[0]]);
		    		float[] vec2 = MathUtilities.vector(corners[cornersMinAngle[1]], corners[cornersMinAngle[2]]);
		    		float[] vecZ = new float[]{0,0,1};
		    		
		    		float[] otherVec2a = MathUtilities.cross(vec1, vecZ);
		    		float[] otherVec2b = MathUtilities.cross(vecZ, vec1);
		    		float[] otherVec1a = MathUtilities.cross(vec2, vecZ);
		    		float[] otherVec1b = MathUtilities.cross(vecZ, vec2);
		    		float[] newVec1;
		    		float[] newVec2;
		    		if(MathUtilities.distance(otherVec2a[0], otherVec2a[1], vec2[0], vec2[1])
		    				< MathUtilities.distance(otherVec2b[0], otherVec2b[1], vec2[0], vec2[1])) {
		    			newVec2 = MathUtilities.mean(vec2, otherVec2a);
		    		} else newVec2 = MathUtilities.mean(vec2, otherVec2b);
		    		
		    		if(MathUtilities.distance(otherVec1a[0], otherVec1a[1], vec1[0], vec1[1])
		    				< MathUtilities.distance(otherVec1b[0], otherVec1b[1], vec1[0], vec1[1])) {
		    			newVec1 = MathUtilities.mean(vec1, otherVec1a);
		    		} else newVec1 = MathUtilities.mean(vec1, otherVec1b);
		    		
	    			newVec1 = MathUtilities.resize(newVec1, MathUtilities.norm(vec1));
	    			newVec2 = MathUtilities.resize(newVec2, MathUtilities.norm(vec2));
	    			
//	    			if(MathUtilities.norm(vec1) > MathUtilities.norm(vec2)) {
//	    				Log.d(TAG, "Original longest: "+MathUtilities.norm(vec1));
//		    			newVec1 = MathUtilities.resize(newVec1, 3.2f);
//		    			newVec2 = MathUtilities.resize(newVec2, 1.6f);
//		    		} else {
//		    			Log.d(TAG, "Original longest: "+MathUtilities.norm(vec2));
//		    			newVec1 = MathUtilities.resize(newVec1, 1.6f);
//		    			newVec2 = MathUtilities.resize(newVec2, 3.2f);
//		    		}
	    			
		    		cuboid[k][0] = MathUtilities.vectorToPoint(newVec1, corners[cornersMinAngle[1]]);
		    		cuboid[k][1] = corners[cornersMinAngle[1]];
		    		cuboid[k][2] = MathUtilities.vectorToPoint(newVec2, corners[cornersMinAngle[1]]);
		    		cuboid[k][3] = MathUtilities.vectorToPoint(newVec2, cuboid[k][0]);
		    		
//		    		float[][] tmp = new float[4][];
//		    		int nextIdx;
//		    		int prevIdx;
//		    		float[] vec;
//		    		for (int i = 0; i < 4; i++) {
//		    			nextIdx = (i+1)%4;
//		    			prevIdx = ((((i-1) % 4) + 4) % 4);
//		    			vec = MathUtilities.vector(cuboid[k][i],cuboid[k][i%2==0 ? prevIdx : nextIdx]);
//		    			vec = MathUtilities.multiply(vec, -1);
//		    			vec = MathUtilities.resize(vec, WorldConfig.BRICK_PERIMETER);
//		    			tmp[i] = MathUtilities.vectorToPoint(vec, cuboid[k][i]);
//					}
//		    		
//		    		for (int i = 0; i < 4; i++) {
//		    			cuboid[k][i] = tmp[i];
//		    		}
//		    		
//		    		for (int i = 0; i < 4; i++) {
//		    			nextIdx = (i+1)%4;
//		    			prevIdx = ((((i-1) % 4) + 4) % 4);
//		    			vec = MathUtilities.vector(cuboid[k][i],cuboid[k][i%2==0 ? nextIdx : prevIdx]);
//		    			vec = MathUtilities.multiply(vec, -1);
//		    			vec = MathUtilities.resize(vec, WorldConfig.BRICK_PERIMETER);
//		    			tmp[i] = MathUtilities.vectorToPoint(vec, cuboid[k][i]);
//		    			
//					}
//		    		
//		    		for (int i = 0; i < 4; i++) {
//		    			cuboid[k][i] = tmp[i];
//		    		}
		    		
		    		
		    		cuboid[k][4] = new float[]{cuboid[k][0][0],cuboid[k][0][1],1-cuboid[k][0][2]};
		    		cuboid[k][5] = new float[]{cuboid[k][1][0],cuboid[k][1][1],1-cuboid[k][1][2]};
		    		cuboid[k][6] = new float[]{cuboid[k][2][0],cuboid[k][2][1],1-cuboid[k][2][2]};
		    		cuboid[k][7] = new float[]{cuboid[k][3][0],cuboid[k][3][1],1-cuboid[k][3][2]};
		    		
		    		if(AppConfig.DEBUG_LOGGING) {
			    		for (int i = 0; i < cuboid[k].length; i++) {
							Log.d(TAG, "RESULTING CUBOID Point "+i+": ("+cuboid[k][i][0]+","+cuboid[k][i][1]+","+cuboid[k][i][2]+")");
						}
		    		}
		    		bricks[k] = new LegoBrick(cuboid[k],Color.ColorName.values()[(int) ocvContours[k][0]]);
		    	} else return new LegoBrick[0];
	    	}
	    	return bricks;
	    } else return new LegoBrick[0];
	}
	
	public void frameTick() {
		synchronized (lock) {
			synchronized (lockExtern) {
				contour.copyTo(contourExtern);
				threshold.copyTo(thresholdExtern);
			}
		}
	}
	
	public Mat getThreshold() {
		Mat result = new Mat();
		synchronized (lockExtern) {
			thresholdExtern.copyTo(result);
		}
		return result;
	}
	
	private void setContour(Mat contour) {
		synchronized (lock) {
			contour.copyTo(this.contour);
		}
	}
	
	private void setThreshold(Mat threshold) {
		synchronized (lock) {
			threshold.copyTo(this.threshold);
		}
	}
	
	private class FindLegoBrick extends AsyncTask<Mat, Void, Void > {
		private FrameTrackingCallback trackingCallback;
		private long start;
		private Mat contour;
		private Mat threshold;
		
		public FindLegoBrick() {
			synchronized (lock) {
//				contour = new Mat(LegoBrickTracker.this.contour.size(), LegoBrickTracker.this.contour.type());
				threshold = new Mat(LegoBrickTracker.this.threshold.size(), LegoBrickTracker.this.threshold.type());
			}
		}
		
		@Override
		protected Void doInBackground(Mat... params) {
//			findLegoBrick( 
//					params[0].getNativeObjAddr(),
//					contour.getNativeObjAddr()
//					);
			long start = System.nanoTime();
			findLegoBrick2( 
					params[0].getNativeObjAddr(),
					threshold.getNativeObjAddr()
					);
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "LegoBrick threshold found in "+(System.nanoTime()-start)/1000000L+"ms");
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
//			setContour(contour);
			setThreshold(threshold);
			this.trackingCallback.trackingDone(LegoBrickTracker.class);
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "LegoBrick found in "+(System.nanoTime()-start)/1000000L+"ms");
		}
		
		private void setupFrameTrackingCallback(FrameTrackingCallback trackingCallback) {
			this.trackingCallback = trackingCallback;
		}
	}
	
	private native void findLegoBrick(long yuvFrameImage, long contour);
	private native void findLegoBrick2(long yuvFrameImage, long contour);
}
