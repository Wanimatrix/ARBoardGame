package be.wouterfranken.arboardgame.old;
//package be.wouterfranken.arboardgame;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Timer;
//import java.util.TimerTask;
//
//import android.app.Activity;
//import android.os.Handler;
//import android.widget.TextView;
//
//public class DebugController {
//	
//	private static Map<String,String> debugMap = new HashMap<String, String>();
//	private static Timer timer = new Timer();
//	private static Handler handler;
//	private static String newDebugText = "";
//	private static TextView dbgTxtView;
//	
//	public static void registerLogValue(String name) throws IllegalAccessException {
//		if(debugMap.size() >= 10) {
//			throw new IllegalAccessException("Cannot register more than 10 debug values.");
//		}
//		debugMap.put(name, "");
//	}
//	
//	public static void updateLogValue(String name, String newValue) {
//		if(!debugMap.containsKey(name)) {
//			throw new IllegalArgumentException("Cannot register more than 10 debug values.");
//		}
//		debugMap.put(name, newValue);
//	}
//	
//	public static void startDebugger(Activity activity) {
//		if(AppConfig.DEBUG) {
//			dbgTxtView = (TextView) activity.findViewById(R.id.debugText);
//			dbgTxtView.bringToFront();
//			dbgTxtView.setText("DEBUGTEXT");
//			handler = new Handler() {
//				public void handleMessage(android.os.Message msg) {
//					dbgTxtView.setText(newDebugText);
//				};
//			};
//			
//			timer.schedule(new TimerTask() {
//				
//				@Override
//				public void run() {
//					newDebugText = "";
//					for(String k:debugMap.keySet()) {
//						newDebugText += k + " : " + debugMap.get(k) + "\n";
//					}
//					handler.obtainMessage(1).sendToTarget();
//					
//				}
//			}, 0, 500);
//		}
//	}
//}
