package be.wouterfranken.arboardgame.rendering.tracking;

import be.wouterfranken.arboardgame.app.AppConfig;
import android.util.Log;

public class Tracker {
	private static final String TAG = Tracker.class.getSimpleName();
	
	public Tracker() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Register tracker with name: "+this.getClass().getSimpleName());
		FrameTrackingCallback.registerTracker(this.getClass());
	}
}
