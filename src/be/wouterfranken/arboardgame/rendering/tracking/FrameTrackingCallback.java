package be.wouterfranken.arboardgame.rendering.tracking;

import java.util.ArrayList;

import be.wouterfranken.arboardgame.app.AppConfig;
import android.hardware.Camera;

public class FrameTrackingCallback {
	private static ArrayList<Class<? extends Tracker>> registeredTrackers = new ArrayList<Class<? extends Tracker>>();
	
	public static void registerTracker(Class<? extends Tracker> c) {
		if((c != CameraPose.class || AppConfig.CAMERA_POSE_ESTIMATION) 
				&& (c != LegoBrick.class || AppConfig.LEGO_TRACKING)) registeredTrackers.add(c);
	}
	
	private ArrayList<Class<? extends Tracker>> doneTrackers = new ArrayList<Class<? extends Tracker>>();
	private Camera camera;
	private byte[] frameData;
	
	public FrameTrackingCallback(byte[] frameData, Camera camera) {
		doneTrackers = (ArrayList<Class<? extends Tracker>>) registeredTrackers.clone();
		this.frameData = frameData;
		this.camera = camera;
	}
	
	public void trackingDone(Class<? extends Tracker> c) {
		if(doneTrackers.isEmpty())
			throw new IllegalStateException("All trackers are done already!");
		if(!doneTrackers.remove(c)) {
			throw new IllegalArgumentException("The tracker "+c.getSimpleName()+" was already removed or was not registered.");
		}
		if(doneTrackers.isEmpty())
			camera.addCallbackBuffer(frameData);
	}
}
