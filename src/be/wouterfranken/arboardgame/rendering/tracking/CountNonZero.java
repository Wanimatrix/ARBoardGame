package be.wouterfranken.arboardgame.rendering.tracking;

import org.opencv.core.Mat;

import android.content.Context;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;
import android.renderscript.Type.Builder;
import android.util.Log;
import be.wouterfranken.arboardgame.rendering.tracking.ScriptC_cnt_nonZero;

public class CountNonZero {
	
	private final static String TAG = CountNonZero.class.getSimpleName();
	
	private RenderScript rs;
	private ScriptC_cnt_nonZero script;
	private byte[] matInBytes;
	
	public CountNonZero(Context ctx) {
		rs = RenderScript.create(ctx, RenderScript.ContextType.DEBUG);
		script = new ScriptC_cnt_nonZero(rs);
	}
	
	public int getCountNonZero(Mat a)  throws Exception {
		
		int rows, cols, size;
		long start, end;
		
		start = System.nanoTime();
		
		rows = a.rows();
		cols = a.cols();
		size = rows * cols;
		
		Allocation mInputMatrixA = creatInputAllocation(rs, a, rows, cols);
//		Allocation result = Allocation.createSized(rs, Element.I32(rs), 1);
		Type.Builder intArrayBuilder = new Builder(rs, Element.I32(rs));
		intArrayBuilder.setX(1);
		Allocation out = Allocation.createTyped(rs, intArrayBuilder.create());
		out.copyFrom(new int[]{0});
		
		if (mInputMatrixA == null) throw new Exception("Error occurs when creating allocation");
		
//		int result = 0;
		
		script.set_size(((int)a.total()) * a.channels());
		script.set_count(0);
		
		script.set_gScript(script);
		script.set_gInput(mInputMatrixA);
//		script.set_gOut(result);
		script.bind_out(out);
		
		end = (System.nanoTime() - start) / 1000000;
		Log.d(TAG, "It took " + end + " ms to copy the matrices");
		
		long nonzeroTime = System.nanoTime(); 
		script.forEach_root(mInputMatrixA);
		
		
		
		

//		------------------
//		BOTTLENECK
//		------------------
		start = System.nanoTime();
		
		int[] result = new int[1];
		out.copyTo(result);
//		result = script.get_nonZero();
		
		end = (System.nanoTime() - start) / 1000000;
		Log.d(TAG, "It took " + end + " ms to copy the matrices back to Java");
		
		Log.d("NONZEROCOUNTER", "(RS) Index: 0, Result: "+result[0]+", time: "+(System.nanoTime() - nonzeroTime)/1000000.0+"ms");
//		------------------
		
//		float[][] output = new float[rows][cols];
//		for(int i = 0; i < rows; i++){
//			for(int j = 0; j < cols; j++){
//				try{
//					output[i][j] = buf[i * rows + j];
//				}catch(Exception e){
//					Log.d(TAG, "i=" + i + ",j=" + j);
//					return null;
//				}
//			}
//		}
		
		return result[0];
	}
	
	private Allocation creatInputAllocation(RenderScript renderscript, 
			Mat matrix, int rows, int cols) {
		
//		byte[] array = new byte[rows * cols];
		
		
		if(matInBytes == null)
			matInBytes = new byte[((int)matrix.total()) * matrix.channels()];
		Log.d(TAG, "Byte array SIZE: "+matInBytes.length);
		long start = System.nanoTime();
		matrix.get(0, 0, matInBytes);
		
		Log.d(TAG, "Time lost: "+(System.nanoTime() - start)/1000000.0+"ms");
		
//		for (int i = 0; i < rows; i++) {
//			for (int j = 0; j < cols; j++) {
//				try {
//					array[i * cols + j] = (byte) matrix.get(i, j)[0];
//					if(array[i * cols + j] != 0) Log.d(TAG, "Value: "+array[i * cols + j]+", orig: "+matrix.get(i, j)[0]);
//				} catch(Exception e) {
//					Log.d(TAG, "i=" + i + ",j=" + j);
//					return null;
//				}
//			}
//		}
		
		Allocation allocation = 
				Allocation.createSized(renderscript, Element.U8(renderscript), matInBytes.length, Allocation.USAGE_SCRIPT);
		allocation.copyFrom(matInBytes);
		
		return allocation;
	}
}
