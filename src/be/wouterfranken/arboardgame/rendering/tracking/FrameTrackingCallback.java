package be.wouterfranken.arboardgame.rendering.tracking;

import java.util.ArrayList;

import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.gameworld.LemmingsGenerator;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.Log;

public class FrameTrackingCallback {
	
	private final static String TAG = FrameTrackingCallback.class.getSimpleName();
	
	private static ArrayList<Class<? extends Tracker>> registeredTrackers = new ArrayList<Class<? extends Tracker>>();
	
	public static void registerTracker(Class<? extends Tracker> c) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Register tracker with name: "+c.getSimpleName());
		
		if((c != CameraPoseTracker.class || AppConfig.CAMERA_POSE_ESTIMATION) 
				&& (c != LegoBrickTracker.class || AppConfig.LEGO_TRACKING)
				&& (c != LemmingsGenerator.class || AppConfig.LEMMING_RENDERING)) registeredTrackers.add(c);
	}
	
	public static void unRegisterAll() {
		registeredTrackers.clear();
	}
	
	private ArrayList<Class<? extends Tracker>> doneTrackers = new ArrayList<Class<? extends Tracker>>();
	private Camera camera;
	private byte[] frameData;
	private long timerStart;
	
	@SuppressWarnings("unchecked")
	public FrameTrackingCallback(byte[] frameData, Camera camera, long timerStart) {
		doneTrackers = (ArrayList<Class<? extends Tracker>>) registeredTrackers.clone();
		this.frameData = frameData;
		this.camera = camera;
		this.timerStart = timerStart;
	}
	
	public void trackingDone(Class<? extends Tracker> c) {
		if(doneTrackers.isEmpty())
			throw new IllegalStateException("All trackers are done already!");
		if(!doneTrackers.remove(c)) {
			throw new IllegalArgumentException("The tracker "+c.getSimpleName()+" was already removed or was not registered.");
		}
		
		if(doneTrackers.isEmpty()) {
			camera.addCallbackBuffer(frameData);
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Totaltime in "+(System.nanoTime()-timerStart)/1000000L+"ms");
		}
		else {
			for (int i = 0; i < doneTrackers.size(); i++) {
				Log.d(TAG, "Tracker not done yet: "+doneTrackers.get(i));
			}
		}
	}
}
