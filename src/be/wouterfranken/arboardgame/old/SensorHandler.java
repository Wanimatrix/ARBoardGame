package be.wouterfranken.arboardgame.old;
//package be.wouterfranken.arboardgame.sensordata;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import android.content.Context;
//import android.hardware.Sensor;
//import android.hardware.SensorEvent;
//import android.hardware.SensorEventListener;
//import android.hardware.SensorManager;
//import android.util.Log;
//import be.wouterfranken.arboardgame.AppConfig;
//import be.wouterfranken.arboardgame.DebugController;
//
///**	
// *  This class handles following sensordata:
// * 	  - orientation values: azimuth, pitch and roll.
// * 		|- Azimuth: the angle that changes when tilting head left or right;
// *  	|- Pitch: the angle that changes when looking up or down;
// *  	|- Roll: the angle that changes when looking left or right.
// *    - acceleration in the three axes: x, y and z.
// *    - magnetic field in the three axes: x, y and z. 
// * 
// * @author wouterfranken
// */
//public class SensorHandler implements SensorEventListener{
//	
//	public final static String TAG = "SensorData";
//	
//	private SensorDataType[] data = {new Acceleration(), new Geomagnetic(), new Orientation()};
//	private final Map<Class<? extends SensorDataType>, Integer> dataIdxs;
//	{
//		dataIdxs = new HashMap<Class<? extends SensorDataType>, Integer>();
//		dataIdxs.put(Acceleration.class, 0);
//		dataIdxs.put(Geomagnetic.class, 1);
//		dataIdxs.put(Orientation.class, 2);
//	}
//	
//	private SensorManager sensorManager;
//	private Sensor accelerometer;
//	private Sensor magnetometer;
//	
//	public SensorHandler(Context context) {
//		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
//		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//		magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//		try {
//			if(AppConfig.SENSOR_DEBUG) {
//				DebugController.registerLogValue(data[0].getClass().getSimpleName());
//				DebugController.registerLogValue(data[1].getClass().getSimpleName());
//				DebugController.registerLogValue(data[2].getClass().getSimpleName());
//			}
//		} catch (IllegalAccessException e) {
//			Log.e(TAG,Log.getStackTraceString(e));
//		}
//	}
//	
//	public void setData(Class<? extends SensorDataType> type, float[] data) {
//		if (data.length != 3) 
//			Log.e(SensorHandler.TAG,"Data array for SensorData must consist of three elements of type float.");
//		if (type == Orientation.class)
//			Log.e(SensorHandler.TAG,"Cannot set data for ORIENTATION sensor.");
//		this.data[dataIdxs.get(type)].setData(data);
//		
//		if(this.data[dataIdxs.get(Acceleration.class)].isSet() && this.data[dataIdxs.get(Geomagnetic.class)].isSet()) {
//			float[] R = new float[9];
//			float[] I = new float[9];
//			boolean success = SensorManager.getRotationMatrix(R, I, 
//					this.data[dataIdxs.get(Acceleration.class)].getData(), this.data[dataIdxs.get(Geomagnetic.class)].getData());
//			if(success) {
//				float[] orientationData = new float[]{0,0,0};
//				SensorManager.getOrientation(R, orientationData);
//				this.data[dataIdxs.get(Orientation.class)].setData(orientationData);
//			}
//		}
//	}
//	
//	public void resume() {
//		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
//		sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
//	}
//	
//	public void pause() {
//		sensorManager.unregisterListener(this);
//	}
//	
//	/**
//	 * When a sensor has changed his value, we:
//	 * 	- store the values of the sensor;
//	 *  - possibly update debug text.
//	 *  
//	 * Note that this is only done when the sensor value has changed of 
//	 * the accelerometer or the magnetic field.
//	 */
//	@Override
//	public void onSensorChanged(SensorEvent event) {
//		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//			this.setData(Acceleration.class, event.values);
//			if(AppConfig.SENSOR_DEBUG) {
//				DebugController.updateLogValue(Acceleration.class.getSimpleName(), data[dataIdxs.get(Acceleration.class)].toString());
//				DebugController.updateLogValue(Orientation.class.getSimpleName(), data[dataIdxs.get(Orientation.class)].toString());
//			}
//		}
//		else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//			this.setData(Geomagnetic.class, event.values);
//			if(AppConfig.SENSOR_DEBUG) {
//				DebugController.updateLogValue(Geomagnetic.class.getSimpleName(), data[dataIdxs.get(Geomagnetic.class)].toString());
//				DebugController.updateLogValue(Orientation.class.getSimpleName(), data[dataIdxs.get(Orientation.class)].toString());
//			}
//		}
//		else return;
//		
//		if(AppConfig.SENSOR_DEBUG) {
//			Log.d(SensorHandler.TAG, this.toString());
//		}
//	}
//	
//	@Override
//	public void onAccuracyChanged(Sensor sensor, int accuracy) {
//		// Unused
//	}
//	
//	@Override
//	public String toString() {
//		String result = "";
//		for(SensorDataType type : data) {
//			if(!type.debugEnabled()) continue;
//			result += type.toString();
//		}
//		return result;
//	}
//}
