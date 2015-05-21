package be.wouterfranken.arboardgame.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;
import android.util.Log;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPoseTracker;
import be.wouterfranken.arboardgame.utilities.AndroidUtils;
import be.wouterfranken.experiments.TimerManager;

public class ColorCalibration {
	
	private final String TAG = ColorCalibration.class.getSimpleName();
	
	private MatOfDouble calibrationResult;
	private int calibrationCount;
	private AfterLoadingColorCalibListener listener;
	private Random randX;
	private Random randY;
	
	public ColorCalibration(AfterLoadingColorCalibListener listener) {
		calibrationResult = new MatOfDouble();
		calibrationCount = 0;
		randX = new Random();
		randY = new Random();
		this.listener = listener;
	}
	
	public void colorCalibration(Mat frame, int amountOfRandomSeeds, float errorDelta, Mat mv) {
		
		Log.d(TAG, "CALIBRATION STARTS ...");
//		TimerManager.start("CALIBRATION", "calibrationTotal", "/sdcard/arbg");
		
		Point3[] seedPoints = new Point3[]{new Point3(7.3f+errorDelta,6.45f-errorDelta,0),new Point3(7.3f+errorDelta,-5.45f-errorDelta,0)};
		
		Point3[][] seedCub = new Point3[2][];
		for (int i = 0; i < seedCub.length; i++) {
			seedCub[i] = new Point3[8];
			seedCub[i][0] = seedPoints[i];
	        seedCub[i][1] = new Point3(seedCub[i][0].x+3.2f-errorDelta*2, seedCub[i][0].y, errorDelta);
	        seedCub[i][2] = new Point3(seedCub[i][0].x+3.2f-errorDelta*2, seedCub[i][0].y-1.6f+errorDelta*2, errorDelta);
	        seedCub[i][3] = new Point3(seedCub[i][0].x, seedCub[i][0].y-1.6f+errorDelta*2, errorDelta);
	        seedCub[i][4] = new Point3(seedCub[i][0].x, seedCub[i][0].y, 1.9f-errorDelta);
	        seedCub[i][5] = new Point3(seedCub[i][1].x, seedCub[i][1].y, 1.9f-errorDelta);
	        seedCub[i][6] = new Point3(seedCub[i][2].x, seedCub[i][2].y, 1.9f-errorDelta);
	        seedCub[i][7] = new Point3(seedCub[i][3].x, seedCub[i][3].y, 1.9f-errorDelta);
		}
		
		Mat coord3D = Mat.ones(4, seedCub.length*seedCub[0].length, CvType.CV_32FC1);
		for(int i = 0; i < coord3D.cols(); i++) {
			coord3D.put(0, i, seedCub[i/seedCub[0].length][i % seedCub[0].length].x);
			coord3D.put(1, i, seedCub[i/seedCub[0].length][i % seedCub[0].length].y);
			coord3D.put(2, i, seedCub[i/seedCub[0].length][i % seedCub[0].length].z);
		}
		
		Point[][] seedCub2D = new Point[2][];
		Mat coord2D = CameraPoseTracker.get2DPointFrom3D(coord3D, mv);
		MatOfPoint[] convexHulls = new MatOfPoint[2];
		
		Mat dbgCalibration = new Mat();
		frame.copyTo(dbgCalibration);
		
//		Mat backProjections[] = new Mat[2];
		
//		hsvChannels.remove(1);
//		hsvChannels.remove(2);
		
		for (int i = 0; i < seedCub2D.length; i++) {
			seedCub2D[i] = new Point[8];
			for (int j = 0; j < seedCub2D[i].length; j++) {
				int idx = i*seedCub2D[i].length + j;
				seedCub2D[i][j] = new Point(coord2D.get(0, idx)[0]/coord2D.get(2, idx)[0],coord2D.get(1, idx)[0]/coord2D.get(2, idx)[0]);
				Log.d(TAG, "SeedPt2D: "+seedCub2D[i][j]);
			}
			
			convexHulls[i] = new MatOfPoint();
			getConvexHull(new MatOfPoint(seedCub2D[i]).getNativeObjAddr(), convexHulls[i].getNativeObjAddr());
			
			
//			Mat mask = new Mat(frame.size(), CvType.CV_8UC1);
//			Imgproc.drawContours(mask, Arrays.asList(new MatOfPoint[]{convexHulls[i]}), -1, new Scalar(255,255,255),-1);
//			Mat hist = new Mat();
////			Highgui.imwrite("/sdcard/arbg/hue.png",hsvChannels.get(0));
////			Highgui.imwrite("/sdcard/arbg/mask.png",mask);
////			Mat test = Converters.vector_Mat_to_Mat(hsvChannels);
////			Log.d(TAG, "Total: "+test.total());
//			Imgproc.calcHist(hsvChannels, new MatOfInt(0,1), mask, hist, new MatOfInt(181,256), new MatOfFloat(0, 180, 0, 255));
//			Core.normalize(hist, hist, 0, 255, Core.NORM_MINMAX, -1, new Mat());
//			
//			backProjections[i] = new Mat();
//			Imgproc.calcBackProject(hsvChannels, new MatOfInt(0,1), hist, backProjections[i], new MatOfFloat(0, 180, 0, 255), 1);
//			Highgui.imwrite("/sdcard/arbg/backProj"+i+".png",backProjections[i]);
		}
		
		Imgproc.drawContours(dbgCalibration, Arrays.asList(convexHulls), -1, new Scalar(255,0,0));
		
		// TODO: generate random seeds within convex hull. Make a range from these values that can be used for thresholding.
		List<List<Point>> generatedPoints = new ArrayList<List<Point>>();
//		List<Pair<Integer,Mat>> masks = new ArrayList<Pair<Integer,Mat>>();
		Mat finalMask = new Mat();
		Mat hsv = new Mat();
		Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_BGR2HSV);
		List<Mat> hsvChannels = new ArrayList<Mat>();
		Core.split(hsv, hsvChannels);
//		Highgui.imwrite("/sdcard/arbg/satBef.png", hsvChannels.get(1));
		Imgproc.equalizeHist(hsvChannels.get(1), hsvChannels.get(1));
//		Highgui.imwrite("/sdcard/arbg/satAfter.png", hsvChannels.get(1));
		
		
		MatOfDouble currentCalibrationResult = new MatOfDouble(new Mat(1, convexHulls.length*3*2, CvType.CV_64FC1));
		
		for (int i = 0; i < convexHulls.length; i++) {
			Rect boundRect = Imgproc.boundingRect(new MatOfPoint(convexHulls[i].toArray()));
			generatedPoints.add(new ArrayList<Point>());
			Log.d(TAG, "BoundingRect: "+boundRect);
			if(boundRect.x < 0 || boundRect.y < 0 || boundRect.x > AppConfig.PREVIEW_RESOLUTION[0] || boundRect.y > AppConfig.PREVIEW_RESOLUTION[1]) return;
			MatOfByte[] values = new MatOfByte[3];
			values[0] = new MatOfByte(new Mat(1,amountOfRandomSeeds,CvType.CV_8UC1));
			values[1] = new MatOfByte(new Mat(1,amountOfRandomSeeds,CvType.CV_8UC1));
			values[2] = new MatOfByte(new Mat(1,amountOfRandomSeeds,CvType.CV_8UC1));
			for (int j = 0; j < amountOfRandomSeeds; j++) {
				Point current;
				do {
					current = new Point(randX.nextDouble()*boundRect.width+boundRect.x, randY.nextDouble()*boundRect.height+boundRect.y);
//					Log.d(TAG, "Current point: "+current+", testResult: "+Imgproc.pointPolygonTest(new MatOfPoint2f(convexHulls[i].toArray()), current, true));
				}
				while(//(current.x < 3 || current.y < 3 || current.y >= AppConfig.PREVIEW_RESOLUTION[1]-3 || current.x >= AppConfig.PREVIEW_RESOLUTION[0]-3) 
						/*||*/ Imgproc.pointPolygonTest(new MatOfPoint2f(convexHulls[i].toArray()), current, true) <= 5);
				
				Log.d(TAG, "Value: "+hsvChannels.get(1).get((int)current.y, (int)current.x)[0]);
				values[0].put(0, j, hsvChannels.get(0).get((int)current.y, (int)current.x)[0]);
				values[1].put(0, j, hsvChannels.get(1).get((int)current.y, (int)current.x)[0]);
				values[2].put(0, j, hsvChannels.get(2).get((int)current.y, (int)current.x)[0]);
				
//				Mat region = new Mat(hsvChannels.get(1),new Rect((int)current.x-3,(int)current.y-3,6,6));
//				MatOfDouble meanMat = new MatOfDouble();
//				MatOfDouble stddevMat = new MatOfDouble();
//				Core.meanStdDev(region, meanMat, stddevMat);
//				double mean = meanMat.get(0, 0)[0];
//				double stddev = stddevMat.get(0, 0)[0];
//				
//				Log.d(TAG, "Mean: "+mean+", Stddev: "+stddev);
//				Highgui.imwrite("/sdcard/arbg/sat.png", hsvChannels.get(1));
//				
//				Mat tmpMask = new Mat();
//			    Mat mask0 = new Mat();
//			    Mat mask1 = new Mat();
//			    Imgproc.threshold(hsvChannels.get(1), mask0, mean+3*stddev, 255, Imgproc.THRESH_BINARY_INV);
//			    Imgproc.threshold(hsvChannels.get(1), mask1, mean-3*stddev, 255, Imgproc.THRESH_BINARY);
//			    Core.bitwise_and(mask0, mask1, tmpMask);
//			    
//			    if(masks.size() <= i) {
//			    	masks.add(new Pair<Integer, Mat>(0, tmpMask));
//			    } else {
//			    	int amount = masks.get(i).first;
//			    	Mat currMask = masks.get(i).second;
//			    	Mat t1 = new Mat();
//			    	Mat t2 = new Mat();
//			    	
//			    	Log.d(TAG, "Term1 = currMask * "+(amount/(amount+1.0)));
//			    	Core.multiply(currMask, new Scalar(amount/(amount+1.0)), t1);
//			    	Core.multiply(tmpMask, new Scalar(1.0/(amount+1.0)), t2);
//			    	Core.add(t1, t2, currMask);
//			    	masks.set(i, new Pair<Integer, Mat>(masks.get(i).first, currMask));
//			    }
//			    
//			    masks.set(i, new Pair<Integer, Mat>(masks.get(i).first+1, masks.get(i).second));
				
				generatedPoints.get(i).add(current);
				Core.circle(dbgCalibration, current, 2, new Scalar(0,0,255));
			}
			
//			double[] mean = new double[3];
//			double[] stddev = new double[3];
			
			//DEBUG
			Mat mask = new Mat();
			
			for (int k = 0; k < values.length; k++) {
				MatOfDouble meanMat = new MatOfDouble();
				MatOfDouble stddevMat = new MatOfDouble();
				Core.meanStdDev(values[k], meanMat, stddevMat);
				double mean = meanMat.get(0, 0)[0];
				double stddev = stddevMat.get(0, 0)[0];
				
				Log.d(TAG, "Mean: "+mean+", Stddev: "+stddev);
				
				currentCalibrationResult.put(0, i*6+2*k, mean);
				currentCalibrationResult.put(0, i*6+2*k+1, stddev);
				
				Log.d(TAG, "STDDEV: "+stddev);
				
				//DEBUG
//				Mat tmpMask = new Mat();
//			    Mat mask0 = new Mat();
//			    Mat mask1 = new Mat();
//			    Imgproc.threshold(hsvChannels.get(k), mask0, mean+3*stddev, 255, Imgproc.THRESH_BINARY_INV);
//			    Imgproc.threshold(hsvChannels.get(k), mask1, mean-3*stddev, 255, Imgproc.THRESH_BINARY);
//			    Core.bitwise_and(mask0, mask1, tmpMask);
			    
			  //DEBUG
//			    if(mask.total() == 0) {
//			    	mask = tmpMask;
//			    } else {
//			    	Core.bitwise_and(tmpMask, mask, mask);
//			    }
			}
			
			
			//DEBUG
//			Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new org.opencv.core.Size(3,3));
//			Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, element);
//			Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, element);
			
//		    if(masks.size() <= i) {
			
			//DEBUG
//			if(finalMask.total() == 0) {
//				finalMask = mask;
//			} else {
//				Core.bitwise_or(finalMask, mask, finalMask);
//			}
			
//		    } else {
//		    	int amount = masks.get(i).first;
//		    	Mat currMask = masks.get(i).second;
//		    	Mat t1 = new Mat();
//		    	Mat t2 = new Mat();
//		    	
//		    	Log.d(TAG, "Term1 = currMask * "+(amount/(amount+1.0)));
//		    	Core.multiply(currMask, new Scalar(amount/(amount+1.0)), t1);
//		    	Core.multiply(tmpMask, new Scalar(1.0/(amount+1.0)), t2);
//		    	Core.add(t1, t2, currMask);
//		    	masks.set(i, new Pair<Integer, Mat>(masks.get(i).first, currMask));
//		    }
		    
//		    masks.set(i, new Pair<Integer, Mat>(masks.get(i).first+1, masks.get(i).second));
		}
		
		if(calibrationResult.total() == 0) {
			calibrationResult = currentCalibrationResult;
		} else {
			Mat t1 = new Mat();
			Mat t2 = new Mat();
//			for (int i = 0; i < calibrationResult.cols(); i++) {
//				if(i % 2 == 0) { // Mean
//					calibrationResult.put(0, i, calibrationResult.get(0, i)[0]*(calibrationCount/(calibrationCount+1.0)) 
//							+ currentCalibrationResult.get(0, i)[0]*(1.0/(calibrationCount+1.0))); 
//				} else {
//					calibrationResult.put(0, i, Math.max(calibrationResult.get(0, i)[0], currentCalibrationResult.get(0, i)[0]));
//				}
//			}
			Core.multiply(calibrationResult, new Scalar(calibrationCount/(calibrationCount+1.0)), t1);
			Core.multiply(currentCalibrationResult, new Scalar(1.0/(calibrationCount+1.0)), t2);
			Core.add(t1, t2, calibrationResult);
		}
		calibrationCount++;
		
//		Mat fullMask = new Mat(finalMask.size(), CvType.CV_8UC3);
//		Core.mixChannels(Arrays.asList(new Mat[]{finalMask}), Arrays.asList(new Mat[]{fullMask}), new MatOfInt(0,0,0,1,0,2));
//		Mat result = new Mat();
//		Core.bitwise_and(fullMask, frame, result);
//		Highgui.imwrite("/sdcard/arbg/mask.png",fullMask);
//		Highgui.imwrite("/sdcard/arbg/segmented.png",result);
//		Highgui.imwrite("/sdcard/arbg/calib.png", dbgCalibration);
//		TimerManager.stop();
//		TimerManager.save("calibrationTotal");
		
		Log.d(TAG, "CALIBRATION ENDS ...");
	}
	
	public void saveCalibrationData(File file) {
		AndroidUtils.saveColorCalibration(file, calibrationResult);
	}
	
	public void loadCalibrationData(File file) {
		MatOfDouble colCalib = AndroidUtils.loadColorCalibration(file);
		if(colCalib != null) {
			calibrationResult = colCalib;
			listener.afterLoadingColorCalib();
		}
	}
	
	public MatOfDouble getCalibrationResult() {
		return calibrationResult;
	}
	
	private native void getConvexHull(long framePtr, long convexHullPtr);
}
