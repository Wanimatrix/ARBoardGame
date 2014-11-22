package be.wouterfranken.arboardgame.rendering.tracking;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Size;

import android.os.AsyncTask;
import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.utilities.DebugUtilities;

public class LegoBrickTracker extends Tracker{
	private static final String TAG = LegoBrickTracker.class.getSimpleName();
	
	private Mat contour = new Mat();
	
	public void findLegoBrick(Mat yuvFrameImage, FrameTrackingCallback trackingCallback) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG,"Legobrick tracking ...");
		
//		long start = System.nanoTime();
		FindLegoBrick task = new FindLegoBrick();
		task.start = System.nanoTime();
		
//		findLegoBrick( 
//				yuvFrameImage.getNativeObjAddr(),
//				contour.getNativeObjAddr()
//				);
//		trackingCallback.trackingDone(LegoBrick.class);
//		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "LegoBrick found in "+(System.nanoTime()-start)/1000000L+"ms");
		
		task.setupFrameTrackingCallback(trackingCallback);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, yuvFrameImage);
	}
	
	// TODO: Let this method call getOcvContour
	public float[][] getGLContour() {
		Mat tmp = new Mat();
		contour.copyTo(tmp);
		if(tmp.empty()) return null;
		float[][] result = new float[tmp.rows()][];
		for (int j = 0; j < tmp.rows(); j++) {
			int cornerAmount = (int) tmp.get(j,0)[0];
			result[j] = new float[cornerAmount*3];
			for (int i = 0; i < cornerAmount*3; i+=3) {
				if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Point "+i/3+": ("+tmp.get(j,2*i/3+1)[0]+","+tmp.get(j,2*i/3+2)[0]+")");
				result[j][i] = (float) ((tmp.get(j,2*i/3+1)[0]/AppConfig.PREVIEW_RESOLUTION[0])*2-1);
				result[j][i+1] = (float) ((1-tmp.get(j,2*i/3+2)[0]/AppConfig.PREVIEW_RESOLUTION[1])*2-1);
				result[j][i+2] = 0;
			}
			DebugUtilities.logGLMatrix("GlContour", result[j], result[j].length /3, 3);
		}
//		DebugUtilities.logGLMatrix("GlContour", result, result.length /3, 3);
		return result;
	}
	
	public float[][] getOcvContour() {
		Mat tmp = new Mat();
		contour.copyTo(tmp);
		if(tmp.empty()) return null;
		float[][] result = new float[tmp.rows()][];
		for (int j = 0; j < tmp.rows(); j++) {
			int cornerAmount = (int) tmp.get(j,0)[0];
			result[j] = new float[cornerAmount*3];
			for (int i = 0; i < cornerAmount*3; i+=3) {
				if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Point "+i/3+": ("+tmp.get(j,2*i/3+1)[0]+","+tmp.get(j,2*i/3+2)[0]+")");
				result[j][i] = (float) (tmp.get(j,2*i/3+1)[0]);
				result[j][i+1] = (float) (tmp.get(j,2*i/3+2)[0]);
				result[j][i+2] = 0;
			}
			DebugUtilities.logGLMatrix("OcvContour", result[j], result[j].length /3, 3);
		}
//		DebugUtilities.logGLMatrix("GlContour", result, result.length /3, 3);
		return result;
	}
	
	public float[][] get3DCorners(float[] extrinsics) {
		float[] homography = new float[9];
		int i = 0;
		for (int row = 0; row < extrinsics.length/4; row++) {
			for (int col = 0; col < extrinsics.length/4; col++) {
				if(row == 2) break;
				else if(col == 3) continue;
				else {
					homography[i++] = extrinsics[row*4+col];
				}
			}
		}
		DebugUtilities.logGLMatrix("Extrinsics", extrinsics, 4, 4);
		DebugUtilities.logGLMatrix("Homography", homography, 3, 3);
		
		float[][] contours = getOcvContour();
		float[][] contours3D = new float[contours.length][];
		for (int j = 0; j < contours.length; j++) {
			contours3D[j] = new float[contours[j].length];
			for (int k = 0; k < contours[j].length; k+=3) {
				float[] p = {contours[0][k],contours[0][k+1],1};
				contours3D[j][k]   = homography[0]*p[0]+homography[3]*p[1]+homography[6]*p[2];
				contours3D[j][k+1] = homography[1]*p[0]+homography[4]*p[1]+homography[7]*p[2];
				contours3D[j][k+2] = homography[2]*p[0]+homography[5]*p[1]+homography[8]*p[2];
			}
			DebugUtilities.logGLMatrix("Contour3D ("+j+")", contours3D[j], contours3D[j].length/3, 3);
		}
		
		return null;
	}
	
	private class FindLegoBrick extends AsyncTask<Mat, Void, Void > {
		private FrameTrackingCallback trackingCallback;
		private long start;
		
		@Override
		protected Void doInBackground(Mat... params) {
			findLegoBrick( 
					params[0].getNativeObjAddr(),
					contour.getNativeObjAddr()
					);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			this.trackingCallback.trackingDone(LegoBrickTracker.class);
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "LegoBrick found in "+(System.nanoTime()-start)/1000000L+"ms");
		}
		
		private void setupFrameTrackingCallback(FrameTrackingCallback trackingCallback) {
			this.trackingCallback = trackingCallback;
		}
	}
	
	private native void findLegoBrick(long yuvFrameImage, long contour);
}
