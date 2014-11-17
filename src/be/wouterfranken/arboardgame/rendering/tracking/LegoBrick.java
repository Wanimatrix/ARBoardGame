package be.wouterfranken.arboardgame.rendering.tracking;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import android.os.AsyncTask;
import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.utilities.DebugUtilities;

public class LegoBrick extends Tracker{
	private static final String TAG = LegoBrick.class.getSimpleName();
	
	private Mat contour = new Mat();
	
	public void findLegoBrick(Mat yuvFrameImage, FrameTrackingCallback trackingCallback) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG,"Legobrick tracking ...");
		
		long start = System.nanoTime();
//		FindLegoBrick task = new FindLegoBrick();
//		task.start = System.nanoTime();
		
		findLegoBrick( 
				yuvFrameImage.getNativeObjAddr(),
				contour.getNativeObjAddr()
				);
		trackingCallback.trackingDone(LegoBrick.class);
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "LegoBrick found in "+(System.nanoTime()-start)/1000000L+"ms");
		
//		task.setupFrameTrackingCallback(trackingCallback);
//		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, yuvFrameImage);
	}
	
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
			this.trackingCallback.trackingDone(LegoBrick.class);
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "LegoBrick found in "+(System.nanoTime()-start)/1000000L+"ms");
		}
		
		private void setupFrameTrackingCallback(FrameTrackingCallback trackingCallback) {
			this.trackingCallback = trackingCallback;
		}
	}
	
	private native void findLegoBrick(long yuvFrameImage, long contour);
}
