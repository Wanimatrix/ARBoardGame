package be.wouterfranken.arboardgame.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

public class AndroidUtils {
	
	private final static String TAG = AndroidUtils.class.getSimpleName();
	
	public static String getPathToRaw(Context context, int rawId, String fileName) throws IOException {
		InputStream is = context.getResources().openRawResource(rawId);
		File f = new File(context.getFilesDir(), fileName);
		FileOutputStream os = new FileOutputStream(f);
		 
		byte[] buffer = new byte[4096];
		int bytesRead;
		
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
		
		is.close();
		os.close();
		return context.getFilesDir() + "/" + fileName;
	}
	
	public static void copyFileFromAssets(Context ctx, final String dir, final String f) {
		InputStream in;
		try {
			if(dir.equals("")) in = ctx.getAssets().open(f);
			else in = ctx.getAssets().open(dir+"/"+f);
			
			final File of = new File(ctx.getDir("execdir",Context.MODE_PRIVATE), f);
			
			final OutputStream out = new FileOutputStream(of);

			final byte b[] = new byte[65535];
			int sz = 0;
			while ((sz = in.read(b)) > 0) {
				out.write(b, 0, sz);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void logHeap(Context context) {
        Double allocated = new Double(Debug.getNativeHeapAllocatedSize())/new Double((1048576));
        Double available = new Double(Debug.getNativeHeapSize())/1048576.0;
        Double free = new Double(Debug.getNativeHeapFreeSize())/1048576.0;
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(2);
        
        String absPath = new File(context.getFilesDir(),"heap.hprof").getAbsolutePath();
        if(available.doubleValue() > 900) {
	        try {
	            Debug.dumpHprofData(absPath);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
        }

        Log.d(TAG, "debug. =================================");
        Log.d(TAG, "debug.heap native: allocated " + df.format(allocated) + "MB of " + df.format(available) + "MB (" + df.format(free) + "MB free)");
        Log.d(TAG, "debug.memory: allocated: " + df.format(new Double(Runtime.getRuntime().totalMemory()/1048576)) + "MB of " + df.format(new Double(Runtime.getRuntime().maxMemory()/1048576))+ "MB (" + df.format(new Double(Runtime.getRuntime().freeMemory()/1048576)) +"MB free)");
    }
	
	public static void saveColorCalibration(File file, MatOfDouble colCalib){
	    if(colCalib!=null && !colCalib.empty()){

	        JSONArray jsonArr = new JSONArray();            

	        double[] array = colCalib.toArray();
	        for(int i=0; i<array.length/6; i++){
	            double[] means = new double[]{array[i*6],array[i*6+2],array[i*6+4]};
	            double[] stddevs = new double[]{array[i*6+1],array[i*6+3],array[i*6+5]};

	            JSONArray color_array = new JSONArray();
	            
	            for (int j = 0; j < 3; j++) {
	            	JSONObject channel_obj = new JSONObject();
					
					channel_obj.put("mean",means[j]);
					channel_obj.put("stddev",stddevs[j]);
					color_array.add(channel_obj); 
				}
	            jsonArr.add(color_array);          
	        }
	        
			try {
				FileWriter fw = new FileWriter(file, false);
				JSONValue.writeJSONString(jsonArr, fw);
				fw.flush();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	}

	public static MatOfDouble loadColorCalibration(File file){
		if(!file.exists()) return null;
		
		JSONArray array = new JSONArray();
		try {
			array = (JSONArray)JSONValue.parse(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	    MatOfDouble result = new MatOfDouble(new Mat(1,array.size()*6, CvType.CV_64FC1));
	    for (int i = 0; i < array.size(); i++) {
	    	JSONArray color_arr = (JSONArray)array.get(i);
	    	for (int j = 0; j < color_arr.size(); j++) {
				JSONObject channel_obj = (JSONObject)color_arr.get(j);
				Log.d(TAG, "MEAN: "+(Double)channel_obj.get("mean"));
				Log.d(TAG, "STDDEV: "+(Double)channel_obj.get("stddev"));
				Log.d(TAG, "Idx: "+(i*color_arr.size()*2 + j*2)+", "+(i*color_arr.size()*2 + j*2 + 1));
				result.put(0, i*color_arr.size()*2 + j*2, (Double)channel_obj.get("mean"));
				result.put(0, i*color_arr.size()*2 + j*2 + 1, (Double)channel_obj.get("stddev"));
			}
		}
	    
	    DebugUtilities.logMat("LoadedColorCalib", result);

	    return result;
	}
}
