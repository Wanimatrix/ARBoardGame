package be.wouterfranken.arboardgame.utilities;

import org.opencv.core.Mat;

import android.util.Log;

public class DebugUtilities {
	private final static String TAG = DebugUtilities.class.getSimpleName();
	
	public static void logGLMatrix(String matrixName, double[] matrix, int rows, int cols) {
		if(cols*rows != matrix.length) throw new IllegalArgumentException("Matrix with "+matrix.length+" elements has not enough elements to match "+rows+" rows and "+cols+" cols.");
		for (int row = 0; row < rows; row++) {
			String rowStr = "";
			if(row == 0)
				rowStr += matrixName+": ";
			for (int col = 0; col < cols; col++) {
				rowStr += matrix[row*cols+col];
				if(col != cols-1)
					rowStr += ",";
			}
			Log.d(TAG, rowStr);
		}
	}
	
	public static void logGLMatrix(String matrixName, float[] matrix, int rows, int cols) {
		if(cols*rows != matrix.length) throw new IllegalArgumentException("Matrix with "+matrix.length+" elements has not enough elements to match "+rows+" rows and "+cols+" cols.");
		for (int row = 0; row < rows; row++) {
			String rowStr = "";
			if(row == 0)
				rowStr += matrixName+": ";
			for (int col = 0; col < cols; col++) {
				rowStr += matrix[row*cols+col];
				if(col != cols-1)
					rowStr += ",";
			}
			Log.d(TAG, rowStr);
		}
	}
	
	public static void logMat(String matrixName, Mat matrix) {
		for (int row = 0; row < matrix.rows(); row++) {
			String rowStr = "";
			if(row == 0)
				rowStr += matrixName+": ";
			for (int col = 0; col < matrix.cols(); col++) {
				rowStr += matrix.get(row, col)[0];
				if(col != matrix.cols()-1)
					rowStr += ",";
			}
			Log.d(TAG, rowStr);
		}
	}
}
