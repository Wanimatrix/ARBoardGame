package be.wouterfranken.arboardgame.rendering.tracking;

import android.util.Log;

public class Tracker {
	private static final String TAG = Tracker.class.getSimpleName();
	
	public Tracker() {
		Log.d(TAG, "Register tracker with name: "+this.getClass().getSimpleName());
		FrameTrackingCallback.registerTracker(this.getClass());
	}
}
