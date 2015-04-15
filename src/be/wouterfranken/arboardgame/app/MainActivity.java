package be.wouterfranken.arboardgame.app;

import java.io.File;
import java.text.ParseException;

import org.opencv.android.OpenCVLoader;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView.BufferType;
import android.widget.Toast;
import be.wouterfranken.arboardgame.R;
import be.wouterfranken.arboardgame.rendering.tracking.BrickTrackerConfig;
import be.wouterfranken.arboardgame.utilities.BrickTrackerConfigFactory;
import be.wouterfranken.arboardgame.utilities.CustomExceptionHandler;
import be.wouterfranken.experiments.TimerManager;

import com.google.vrtoolkit.cardboard.CardboardActivity;

public class MainActivity extends CardboardActivity implements OnSharedPreferenceChangeListener {
	
	private static final String TAG = MainActivity.class.getSimpleName();
	
    static
	{
		if(OpenCVLoader.initDebug()) {
			try {
				System.loadLibrary("gnustl_shared");
				System.loadLibrary("aruco_opencv");
				System.loadLibrary("jni_interface");
			} catch (UnsatisfiedLinkError e) {
				System.err.println("Native code library failed to load.\n"+e);
			}
		} else {
			System.err.println("Native code library failed to load.\n");
		}
	}
	
	/**
	 * Setup app:
	 * 	- Set defined layout in content view;
	 * 	- Get the Surface holder from the Surface View;
	 *  - Add the Surface Callback to the Surface Holder;
	 *  - Register sensorManager
	 *  - Get default Accelerometer and Magnetometer
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		registerForContextMenu(findViewById(R.id.arView));
		
		if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
		    Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler("/sdcard/arbg/log.txt"));
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
	}
	
	/**
	 * On resume:
	 * 	- Get the camera;
	 * 	- Register event listeners on the sensorManager for 
	 * 		the Accelerometer and Magnetometer.
	 */
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	/**
	 * On pause:
	 * 	- Stop the camera when in preview and release the camera,
	 * 		so it can be used by other apps;
	 * 	- Unregister listener on the sensorManager.
	 */
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {	
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    
	    if(!((CameraView)findViewById(R.id.arView)).getGamePhaseManager().canSaveColorCalibration()) {
			menu.findItem(R.id.saveCalib).setVisible(false);
		} else {
			menu.findItem(R.id.saveCalib).setVisible(true);
		}
	    
	    if(!((CameraView)findViewById(R.id.arView)).getGamePhaseManager().canEditBrickDetectionParams()) {
	    	menu.setGroupVisible(R.id.brickDetectParams, true);
	    } else
	    	menu.setGroupVisible(R.id.brickDetectParams, false);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
//	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		AlertDialog.Builder builder = new Builder(MainActivity.this);
		final EditText input = new EditText(this);
		final Switch boolSwitch = new Switch(this);
		
	    switch (item.getItemId()) {
	        case R.id.saveCalib:
	        	builder.setTitle("Save color calibration");
	        	builder.setMessage("To save the current color calibration result, fill in a filename (no extension) and hit \"Save\".");
	        	
	        	builder.setView(input);
	        	builder.setPositiveButton("Save", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						((CameraView)findViewById(R.id.arView))
							.getGamePhaseManager()
							.getColorCalibration()
							.saveCalibrationData(new File(MainActivity.this.getDir("execdir",Context.MODE_PRIVATE)+"/calibration/",input.getText().toString()+".colcal"));
					}
				});
	        	builder.create().show();
	            
	            return true;
	        case R.id.loadCalib:
	        	builder.setTitle("Load color calibration");
//	        	builder.setMessage("To load a saved color calibration, select the calibration result to load.");
	        	final File calibrationFolder = new File(MainActivity.this.getDir("execdir",Context.MODE_PRIVATE)+"/calibration/");
	        	Log.d(TAG, "File amount: "+calibrationFolder.listFiles().length+", First: "+calibrationFolder.list()[0]);
	        	
	        	builder.setItems(calibrationFolder.list(), new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						((CameraView)findViewById(R.id.arView))
							.getGamePhaseManager()
							.getColorCalibration()
							.loadCalibrationData(calibrationFolder.listFiles()[which]);
					}
				});
	        	builder.create().show();
	            return true;
	        case R.id.editParams:
	        	builder.setTitle("Edit brick detection params");
	        	final String[] parameterList = BrickTrackerConfigFactory.getConfiguration().getDescriptions();
	        	Log.d(TAG, "The first parameter: "+parameterList[0]);
	        	builder.setItems(parameterList, 
	        			new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, final int which) {
						String value = BrickTrackerConfigFactory.getConfiguration().getValues()[which].toString();
						Builder b = new AlertDialog.Builder(MainActivity.this);
						b.setTitle("Edit "+parameterList[which]);
						input.setText(value, BufferType.EDITABLE);
						if(BrickTrackerConfigFactory.getConfiguration().getValues()[which].getClass() == Boolean.class) {
							boolSwitch.setChecked((Boolean)BrickTrackerConfigFactory.getConfiguration().getValues()[which]);
							b.setView(boolSwitch);
						} else {
							input.setText(value, BufferType.EDITABLE);
							b.setView(input);
						}
						
						b.setPositiveButton("Save", new OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int unused) {
								try {
									String result = "";
									if(BrickTrackerConfigFactory.getConfiguration().getValues()[which].getClass() == Boolean.class)
										result += boolSwitch.isChecked();
									else
										result += input.getText();
									
									BrickTrackerConfigFactory.getConfiguration()
										.set(BrickTrackerConfigFactory.getConfiguration().getShortNames()[which], result);
									Log.d(TAG, "New value for "+BrickTrackerConfigFactory.getConfiguration().getLongNames()[which]+": "
										+BrickTrackerConfigFactory.getConfiguration().getValues()[which]+", type: "
										+BrickTrackerConfigFactory.getConfiguration().getValues()[which].getClass());
								} catch (ParseException e) {
									e.printStackTrace();
								}
							}
						});
						
						b.create().show();
					}
				});
	        	
	        	builder.create().show();
	        	return true;
	        case R.id.resetParams:
	        	builder.setTitle("Reset brick detection params");
	        	builder.setMessage("Are you sure you want to reset all brick detection params?");
	        	builder.setPositiveButton("Yes", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						BrickTrackerConfigFactory.getConfiguration().reset();
					}
				});
	        	
	        	builder.setNegativeButton("No", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
	        	builder.create().show();
	        	builder = new AlertDialog.Builder(this);
	        	return true;
	        default:
	            return super.onContextItemSelected(item);
	    }
	}
}
