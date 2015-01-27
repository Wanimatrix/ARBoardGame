package be.wouterfranken.arboardgame.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

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
}
