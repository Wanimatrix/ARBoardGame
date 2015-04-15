package be.wouterfranken.arboardgame.rendering.tracking;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfFloat4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import android.content.Context;
import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.gameworld.LegoBrick;
import be.wouterfranken.arboardgame.gameworld.LegoBrickContainer;
import be.wouterfranken.arboardgame.gameworld.WorldCoordinate;
import be.wouterfranken.arboardgame.gameworld.WorldLines;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPoseTracker.Mapper2D3D;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPoseTracker.ZMapper2D3D;
import be.wouterfranken.arboardgame.utilities.BrickTrackerConfigFactory;
import be.wouterfranken.arboardgame.utilities.Color;
import be.wouterfranken.arboardgame.utilities.Color.ColorName;
import be.wouterfranken.arboardgame.utilities.DebugUtilities;
import be.wouterfranken.arboardgame.utilities.MathUtilities;
import be.wouterfranken.experiments.TimerManager;

public class LegoBrickTracker2 extends Tracker {
private static final String TAG = LegoBrickTracker.class.getSimpleName();
	
	private Mat contour = new Mat();
	private Mat threshold = new Mat();
	private List<Color> contourColors = new ArrayList<Color>();
	private Mat contourExtern = new Mat();
	private Mat thresholdExtern = new Mat();
	private Mat brickPositionData = new Mat();
	private Mat originalContours = new Mat();
	private List<LegoBrickContainer> brickCandidates = new ArrayList<LegoBrickContainer>();
	private Map<WorldCoordinate, List<List<float[]>>> falseBricks = new HashMap<WorldCoordinate, List<List<float[]>>>();
//	private Map<Float, Map<Float, List<List<float[]>>>> falseBricks = new HashMap<Float, Map<Float,List<List<float[]>>>>();
	private Object lock = new Object();
	private Object lockExtern = new Object();
	
	private Context ctx;
	
	public LegoBrickTracker2(Context ctx) {
		this.ctx = ctx;
	}
	
	private class ContourInformation {
		public int[] upVIdx;
		public int upSize;
		public int anchorIdx;
		public float[] z0Pt;
		public Point[] points;
		
		
		public ContourInformation(Point[] points, int[] upVIdx, int upSize, int anchorIdx, float[] z0Pt,
				float[] z1Pt, int origContIdx) {
			super();
			this.points = points;
			this.upSize = upSize;
			this.upVIdx = upVIdx;
			this.anchorIdx = anchorIdx;
			this.z0Pt = z0Pt;
		}
	}
	
	int frameCount = 0;
	
	public void findLegoBrick(Mat yuvFrameImage, Mat modelView, float orientation, Mat colorCalibration, FrameTrackingCallback trackingCallback,
			WorldLines currentWorld) throws IOException {
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG,"Legobrick tracking ...");
		
		File f = new File("/sdcard/arbg/algo/removalReason"+frameCount+".txt");
		BufferedWriter writer = new BufferedWriter(new FileWriter(f));
		long start = System.nanoTime();
	
		Log.d(TAG,"LINES...");
		
		brickCandidates.clear();
		
		// Setup debug image.
		Mat check = new Mat();
		check = yuvFrameImage.clone();
		
		
		// Calculate global UpVector
//		long startTrackLines = System.nanoTime();
		TimerManager.start("BrickDetection", "UpVectorCalc", BrickTrackerConfigFactory.getConfiguration().toString());
		Mat coord3D = Mat.ones(4, 2, CvType.CV_32FC1);
		coord3D.put(0, 0, 0);
		coord3D.put(1, 0, 0);
		coord3D.put(2, 0, 1);
		coord3D.put(0, 1, 0);
		coord3D.put(1, 1, 0);
		coord3D.put(2, 1, 0);
		
		DebugUtilities.logMat("COORD3D", coord3D);
		
		Mat coord2D = CameraPoseTracker.get2DPointFrom3D(coord3D, modelView);
		
		DebugUtilities.logMat("COORD2D", coord2D);
		float[] vec = new float[]{(float) (coord2D.get(0,0)[0]/coord2D.get(2,0)[0]-coord2D.get(0,1)[0]/coord2D.get(2,1)[0]),
				(float) (coord2D.get(1,0)[0]/coord2D.get(2,0)[0]-coord2D.get(1,1)[0]/coord2D.get(2,1)[0])};
		float norm = (float) Math.sqrt(vec[0]*vec[0]+vec[1]*vec[1]);
		vec = new float[]{vec[0]/norm, vec[1]/norm};
		DebugUtilities.logGLMatrix("UpVector", vec, 2, 1);
		float upAngle = (float) (Math.acos(vec[0])*(180.0f/Math.PI)); 
		Log.d(TAG, "UpAngle: "+upAngle);
        upAngle = (coord2D.get(1,0)[0] > coord2D.get(1,1)[0]) ? (upAngle*-1) : upAngle;
        Log.d(TAG, "UpAngle: "+upAngle);
        TimerManager.stop();
        
        TimerManager.start("BrickDetection", "C++Part", BrickTrackerConfigFactory.getConfiguration().toString());
        // Find legobrick contours
		findLegoBrickLines(yuvFrameImage.getNativeObjAddr(), upAngle, (Double)BrickTrackerConfigFactory.getConfiguration().getItem("APDP"), colorCalibration.getNativeObjAddr(), brickPositionData.getNativeObjAddr(), originalContours.getNativeObjAddr());
		TimerManager.stop();
		
		long startJava = System.nanoTime();
		TimerManager.start("BrickDetection", "JavaPart", BrickTrackerConfigFactory.getConfiguration().toString());
		
		// Convert contours to appropriate data types
		TimerManager.start("BrickDetection", "ConvertC++Contours", BrickTrackerConfigFactory.getConfiguration().toString());
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		List<float[]> angles = new ArrayList<float[]>();
		List<Integer> idxes = new ArrayList<Integer>();
		for(int i = 0; i< brickPositionData.rows(); i++) {
			Point[] pts = new Point[4];
			float[] tmpAngles = new float[3];
			for(int j = 0; j < brickPositionData.cols()-2;j+=3) {
				Log.d(TAG, "Pt idx: "+(j/3));
				pts[j/3] = new Point(brickPositionData.get(i, j+2)[0], brickPositionData.get(i, j+3)[0]);
				if(j/3 < 3)
					tmpAngles[j/3] = (float) brickPositionData.get(i, j+4)[0];
			}
			idxes.add((int)brickPositionData.get(i,0)[0]);
			MatOfPoint mp = new MatOfPoint(pts);
			contours.add(mp);
			angles.add(tmpAngles);
		}
		Log.d(TAG, "Contour size: "+contours.size());
		TimerManager.stop();
		
		writer.append(contours.size()+" possible collections of bricks found.");
		writer.newLine();
		
		// Filter contours and convert to 3D LegoBricks
		TimerManager.start("BrickDetection", "FilterContours", BrickTrackerConfigFactory.getConfiguration().toString());
		Iterator<MatOfPoint> it = contours.iterator();
		List<Integer> contourOrigIdx = new ArrayList<Integer>(); // Holds the original indexes of accepted contours
		List<ContourInformation> acceptedContours = new ArrayList<ContourInformation>();  // Holds the accepted contours
		int i = -1;
		int newIdx = 0;
		while(it.hasNext()) {
			i++;
			MatOfPoint contour = it.next();
			Point[] pts = contour.toArray();
			
			
			int anchorPtIdx; // Will hold the index of the anchor point in the contour (the bottom point of the up-edge)
			
			// Will hold the indexes that define the up-edge (in the same sequence as in the contour)
			int[] upVIdx = new int[]{(int) brickPositionData.get(i, 1)[0],0};
			upVIdx[1] = (upVIdx[0]+1 >= pts.length ? upVIdx[0]+1-pts.length : upVIdx[0]+1);
			
			// Determine the z0 and z1 pt of the up-edge
			float[] z0Pt = CameraPoseTracker.get3DPointFrom2D(modelView, (float)pts[upVIdx[0]].x, (float)pts[upVIdx[0]].y, new CameraPoseTracker.ZMapper2D3D(0));
			float[] z1Pt = CameraPoseTracker.get3DPointFrom2D(modelView, (float)pts[upVIdx[1]].x, (float)pts[upVIdx[1]].y, 
					new CameraPoseTracker.XYMapper2D3D(z0Pt[0],z0Pt[1]));
			if(z1Pt[2] > 0) {
				anchorPtIdx = upVIdx[0];
			} else {
				z0Pt = CameraPoseTracker.get3DPointFrom2D(modelView, (float)pts[upVIdx[1]].x, (float)pts[upVIdx[1]].y, new CameraPoseTracker.ZMapper2D3D(0));
				z1Pt = CameraPoseTracker.get3DPointFrom2D(modelView, (float)pts[upVIdx[0]].x, (float)pts[upVIdx[0]].y, 
						new CameraPoseTracker.XYMapper2D3D(z0Pt[0],z0Pt[1]));
				anchorPtIdx = upVIdx[1];
			}
			
			// Get the directional angle of the up edge
			float dirAngleLs = MathUtilities.angleToDirectionalAngle(angles.get(i)[upVIdx[0]]);
			
			// Get the directional angle of the global up vector, on the same place as the up-edge
			float[] pt3D2 = new float[]{z0Pt[0],z0Pt[1],z0Pt[2]+1};
			
			coord3D = Mat.ones(4, 1, CvType.CV_32FC1);
			coord3D.put(0, 0, pt3D2[0]);
			coord3D.put(1, 0, pt3D2[1]);
			coord3D.put(2, 0, pt3D2[2]);
			coord2D = CameraPoseTracker.get2DPointFrom3D(coord3D, modelView);
			
			float[] pt2D2 = new float[]{(float) (coord2D.get(0,0)[0]/coord2D.get(2,0)[0]),(float) (coord2D.get(1,0)[0]/coord2D.get(2,0)[0])};
			vec = new float[]{(float) (pt2D2[0]-pts[upVIdx[0]].x),(float) (pt2D2[1]-pts[upVIdx[0]].y)};
			norm = (float) Math.sqrt(vec[0]*vec[0]+vec[1]*vec[1]);
			vec = new float[]{vec[0]/norm,vec[1]/norm};
			
			float angleDeg = (float) (Math.acos(vec[0])*(180.0f/Math.PI)); 
			angleDeg = (pt2D2[1] > pts[upVIdx[0]].y) ? (angleDeg*-1) : angleDeg;
			float dirAngleUp = MathUtilities.angleToDirectionalAngle(angleDeg);
			
			// Reject brick contours that have an up-edge that does not closely match the global up-vector
			if(dirAngleLs > (dirAngleUp-10  % 180) && dirAngleLs < (dirAngleUp+10 % 180)) {
				Core.line(check, pts[upVIdx[0]], pts[upVIdx[1]], new Scalar(0,0,255));
		    } else {
		    	writer.append("Brick collection removed. Reason: no good 'up-edge' found.");
		    	writer.newLine();
		    	it.remove();
		    	idxes.remove(newIdx);
		    	continue;
		    }

			// Calculate the size of the up-edge. If too small, reject the contour
			int upSize;
			if(Math.abs(1.9-z1Pt[2]) < 0.4 || Math.abs(2.85-z1Pt[2]) < 0.4
					|| Math.abs(0.95-z1Pt[2]) < 0.4
					|| Math.abs(3.8-z1Pt[2]) < 0.4) {
				upSize = Math.round(z1Pt[2]/0.95f);
			} else {
				writer.append("Brick collection removed. Reason: 'up-edge' has no correct size.");
				writer.newLine();
				it.remove();
				idxes.remove(newIdx);
		    	continue;
			}
			
			ContourInformation contourInfo = new ContourInformation(pts, upVIdx, upSize,anchorPtIdx,z0Pt,z1Pt,(int)brickPositionData.get(i,0)[0]);
			
			newIdx++;
			
			
			if(!acceptedContours.contains(contourInfo)) {
				contourOrigIdx.add(i);
				acceptedContours.add(contourInfo);
			}
			
			Log.d(TAG, "UpVector Idxes: "+upVIdx[0]+","+upVIdx[1]);
		}
		
		TimerManager.stop();
		
		List<LegoBrickContainer> acceptedBrickCandidates = new ArrayList<LegoBrickContainer>();
		
//		long startCILoop = System.nanoTime();
		TimerManager.start("BrickDetection", "CILoop", BrickTrackerConfigFactory.getConfiguration().toString());
		
		for (ContourInformation ci : acceptedContours) {
			
			// Calculate sizes and vectors of all sides.
			TimerManager.start("BrickDetection", "Size&VectorCalcs", BrickTrackerConfigFactory.getConfiguration().toString());
			int[] allSizes = new int[3];
			float[][] sideVectors = new float[3][];
			int j = 1;
			for(int ptIdx = 0;ptIdx < ci.points.length-1;ptIdx++) {
				Mapper2D3D mapper;
				if(ptIdx == ci.upVIdx[0]) {
					allSizes[0] = ci.upSize;
					sideVectors[0] = MathUtilities.vector(ci.z0Pt, new float[]{ci.z0Pt[0], ci.z0Pt[1], 1});
					continue;
				}
				else if(   (ptIdx < ci.upVIdx[0] && ci.upVIdx[0] == ci.anchorIdx)
						|| (ptIdx > ci.upVIdx[0] && ci.upVIdx[0] != ci.anchorIdx)) {
					mapper = new ZMapper2D3D(0);
				} else {
					mapper = new ZMapper2D3D(ci.upSize*0.95f);
				}
				float[] p1 = CameraPoseTracker.get3DPointFrom2D(modelView, (float)ci.points[ptIdx].x, (float)ci.points[ptIdx].y, mapper);
				float[] p2 = CameraPoseTracker.get3DPointFrom2D(modelView, (float)ci.points[ptIdx+1].x, (float)ci.points[ptIdx+1].y, mapper);
				allSizes[j] = Math.round(MathUtilities.norm(MathUtilities.vector(p1, p2))/1.6f);
				Log.d(TAG, "Allsizes: "+MathUtilities.norm(MathUtilities.vector(p1, p2))+" = "+allSizes[j]);
				sideVectors[j] = MathUtilities.resize(MathUtilities.vector(p1, p2),1);
				sideVectors[j][2] = 0; // Force z = 0 (vector must be in XY-plane!)
				
				if(ptIdx < ci.anchorIdx) {
					sideVectors[j] = MathUtilities.multiply(sideVectors[j], -1);
				}
				j++;
			}
			
			// Make sure all vectors have angle of 90 degrees
			float[] tmpSideVec = MathUtilities.cross(sideVectors[0], sideVectors[1]);
			if(Math.signum(tmpSideVec[0]) != Math.signum(sideVectors[2][0]) || Math.signum(tmpSideVec[1]) != Math.signum(sideVectors[2][1]))
				sideVectors[2] = MathUtilities.multiply(tmpSideVec, -1);
			else
				sideVectors[2] = tmpSideVec;
			sideVectors[2][2] = 0;
			TimerManager.stop();
			
			TimerManager.start("BrickDetection", "SplitBrick", BrickTrackerConfigFactory.getConfiguration().toString());
			Log.d("Timer", "AllSizes: {"+allSizes[0]+","+allSizes[1]+","+allSizes[2]+"}");
			int amountOfBricks = allSizes[1]*allSizes[2]*allSizes[0];
			if(amountOfBricks == 0) {
				TimerManager.stop();
				writer.append("Brick collection removed. Reason: Amount of bricks in the collection equals 0.");
				writer.newLine();
				continue;
			}
			Log.d(TAG, "Amount Of Bricks: "+amountOfBricks+": ("+allSizes[0]+","+allSizes[1]+","+allSizes[2]+")");
			
			writer.append("Accepted brick collection has "+amountOfBricks+" bricks.");
			writer.newLine();
			
			float[][][] cuboids = new float[1][][];
			sideVectors[0] = MathUtilities.resize(sideVectors[0],0.95f);
			sideVectors[1] = MathUtilities.resize(sideVectors[1],1.6f);
			sideVectors[2] = MathUtilities.resize(sideVectors[2],1.6f);
			
			float[][] halfSideVectors = new float[3][];
			halfSideVectors[0] = MathUtilities.resize(sideVectors[0], 0.95f/2);
			halfSideVectors[1] = MathUtilities.resize(sideVectors[1], 1.6f/2);
			halfSideVectors[2] = MathUtilities.resize(sideVectors[2], 1.6f/2);
			
			List<LegoBrickContainer> tmpBricks = new ArrayList<LegoBrickContainer>();
			
			for (int k = 0; k < cuboids.length; k++) {
				cuboids[k] = new float[amountOfBricks][];
				cuboids[k][0] = new float[3];
				
				float[] anchorPt3D = CameraPoseTracker.get3DPointFrom2D(modelView, (float)ci.points[ci.anchorIdx].x, (float)ci.points[ci.anchorIdx].y, 
						new CameraPoseTracker.ZMapper2D3D(k*0.95f));
				
				cuboids[k][0] = MathUtilities.vectorsToPoint(anchorPt3D, halfSideVectors[0], halfSideVectors[1], halfSideVectors[2]);
			
//				acceptedBricks.put(ci.origContIdx, new LegoBrick(cuboids[0], ColorName.BLUE, null));
				
				if(tmpBricks.size() <= 0)
					tmpBricks.add(new LegoBrickContainer(new LegoBrick(cuboids[k][0], halfSideVectors, orientation, frameCount)));
				else 
					tmpBricks.get(0).add((new LegoBrick(cuboids[k][0], halfSideVectors, orientation, frameCount)));
				
				MatOfFloat4 anchorBlock = new MatOfFloat4(new Mat(1, 4, CvType.CV_32FC1));
				anchorBlock.put(0, 0, cuboids[k][0][0]);
				anchorBlock.put(0, 1, cuboids[k][0][1]);
				anchorBlock.put(0, 2, cuboids[k][0][2]);
				anchorBlock.put(0, 3, 1);
				
				Mat translation = Mat.eye(4, 4, CvType.CV_32FC1);
				int cuboidsIdx = 1;
				for(int a = 0; a < allSizes[0];a++) {
					for(int b = 0; b < allSizes[1];b++) {
						for(int c = 0; c < allSizes[2];c++) {
							if(a == 0 && b == 0 && c == 0) continue; // Anchorblock is already genereated
							Log.d(TAG, "CuboidIdx: "+cuboidsIdx);
							
							// Generate a new brick by translation of the anchorBrick
							MatOfFloat newBlock = new MatOfFloat();
							
							translation.put(0, 3, new float[]{sideVectors[1][0]*b+sideVectors[2][0]*c});
							translation.put(1, 3, new float[]{sideVectors[1][1]*b+sideVectors[2][1]*c});
							translation.put(2, 3, new float[]{sideVectors[0][2]*a});
							Log.d(TAG, "GEMM: "+anchorBlock.size()+", translation: "+translation.size());
							Core.gemm(anchorBlock, translation.t(), 1, new Mat(), 1, newBlock, 0);
							cuboids[k][cuboidsIdx] = new float[3];
							newBlock.get(0, 0, cuboids[k][cuboidsIdx]);
							
							if(tmpBricks.size() <= cuboidsIdx)
								tmpBricks.add(new LegoBrickContainer(new LegoBrick(cuboids[k][cuboidsIdx], halfSideVectors, orientation, frameCount)));
							else 
								tmpBricks.get(cuboidsIdx).add((new LegoBrick(cuboids[k][cuboidsIdx], halfSideVectors, orientation, frameCount)));
							cuboidsIdx++;
						}
					}
				}
				
				acceptedBrickCandidates.addAll(tmpBricks);
			}
			
			TimerManager.stop();
		}
		
		TimerManager.stop();
		
		
		
		if((Boolean)BrickTrackerConfigFactory.getConfiguration().getItem("FB")) {
			TimerManager.start("BrickDetection", "FalseBrick", BrickTrackerConfigFactory.getConfiguration().toString());
			Iterator<LegoBrickContainer> candIt = acceptedBrickCandidates.iterator();
			while (candIt.hasNext()) {
				LegoBrickContainer legoBrickContainer = candIt.next();
				Iterator<LegoBrick> brickIt = legoBrickContainer.iterator();
				while (brickIt.hasNext()) {
					LegoBrick lb = brickIt.next();
					WorldCoordinate xy = new WorldCoordinate(lb.getCenterPoint()[0], lb.getCenterPoint()[1]);
					int z = Math.round((lb.getCenterPoint()[2]-(0.95f/2))/0.95f);
					
					if(!falseBricks.containsKey(xy)) continue;
					else if(falseBricks.get(xy).get(z).size() > (Long)BrickTrackerConfigFactory.getConfiguration().getItem("FB_THR")) {
						brickIt.remove();
					}
				}
				if(legoBrickContainer.isEmpty()) candIt.remove();
			}
			TimerManager.stop();
		}
		
//		long start2 = System.nanoTime();
		TimerManager.start("BrickDetection", "CHECKPOINT1", BrickTrackerConfigFactory.getConfiguration().toString());
		// Check new containers for overlap and add them to the brickCandidates
		List<LegoBrick> tmpBricksArray = new ArrayList<LegoBrick>();
		for (LegoBrickContainer legoBrickContainer : acceptedBrickCandidates) {
			tmpBricksArray.addAll(legoBrickContainer);
		}
		
		for (LegoBrickContainer legoBrickContainer : currentWorld.getCandidateBricks()) {
			tmpBricksArray.addAll(legoBrickContainer);
		}
		
//		tmpBricksArray.addAll(currentWorld.getBricks());
		
//		Log.d("PERFORMANCE_ANALYSIS", "CHECKPOINT1 time: "+(System.nanoTime() - start2)/1000000.0+"ms");
		TimerManager.stop();
		
//		start2 = System.nanoTime();
		TimerManager.start("BrickDetection", "CHECKPOINT2", BrickTrackerConfigFactory.getConfiguration().toString());
		coord3D = Mat.ones(4, tmpBricksArray.size()*8, CvType.CV_32FC1);
		for (int l = 0; l < tmpBricksArray.size(); l++) {
			LegoBrick b = tmpBricksArray.get(l);
			float[][] cuboid = b.getCuboid();
			for (int m = 0; m < 8; m++) {
				coord3D.put(0, l*8+m, cuboid[m][0]);
				coord3D.put(1, l*8+m, cuboid[m][1]);
				coord3D.put(2, l*8+m, cuboid[m][2]);
			}
		}
//		Log.d("PERFORMANCE_ANALYSIS", "CHECKPOINT2 time: "+(System.nanoTime() - start2)/1000000.0+"ms");
		TimerManager.stop();
		
//		long startOverlapCalc = System.nanoTime();
		coord2D = CameraPoseTracker.get2DPointFrom3D(coord3D, modelView);
//		Log.d("PERFORMANCE_ANALYSIS", "OverlapCalc time: "+(System.nanoTime() - startOverlapCalc)/1000000.0+"ms");
		
		Log.d(TAG, "COORD2D size: "+coord2D.cols()+" VS "+tmpBricksArray.size());
		
		TimerManager.start("BrickDetection", "CHECKPOINT3", BrickTrackerConfigFactory.getConfiguration().toString());
		Mat bricksMat = new Mat(tmpBricksArray.size(),8*2, CvType.CV_32FC1);
		for (int l = 0; l < tmpBricksArray.size(); l++) {
			for (int m = 0; m < 8; m++) {
				bricksMat.put(l, m*2, coord2D.get(0,l*8+m)[0]/coord2D.get(2,l*8+m)[0]);
				bricksMat.put(l, m*2+1, coord2D.get(1,l*8+m)[0]/coord2D.get(2,l*8+m)[0]);
			}
		}
		
		float[] overlap = getOverlap(bricksMat.getNativeObjAddr());
		
//		Log.d("PERFORMANCE_ANALYSIS", "CHECKPOINT3 time: "+(System.nanoTime() - start2)/1000000.0+"ms");
		TimerManager.stop();
		
		
//		start2 = System.nanoTime();
		TimerManager.start("BrickDetection", "CHECKPOINT4", BrickTrackerConfigFactory.getConfiguration().toString());
		int counter = 0;
		for (int k = 0; k < overlap.length; k++) {
//			if(k >= acceptedBrickCandidates.size()+currentWorld.getCandidateBricks().size()) {
//				LegoBrick b = tmpBricksArray.get(k);
//				Log.d("REALBRICKREMOVAL", "Realbrick, overlap check: "+overlap[counter]);
//				if(overlap[counter] < 0.2f) {
//					b.voteRemoval();
//					if(b.getRemovalVotes() >= 3) {
//						Log.d("REALBRICKREMOVAL", "Real brick was removed!");
//						currentWorld.removeBrick(b);
//					}
//				}
//				counter++;
//			} else {
				boolean isNewDetected = (k < acceptedBrickCandidates.size());
				LegoBrickContainer lc = isNewDetected ? acceptedBrickCandidates.get(k) : currentWorld.getCandidateBricks().get(k-acceptedBrickCandidates.size());
				Iterator<LegoBrick> lcIterator = lc.iterator();
				while(lcIterator.hasNext()) {
					LegoBrick lb = lcIterator.next();
					
					if(overlap[counter] < (Double)BrickTrackerConfigFactory.getConfiguration().getItem("REM_THR")) {
						if(isNewDetected) {
							Log.d(TAG, "New detection removed");
							lcIterator.remove();
							writer.append("Brick removed. Reason: does not conform to frame threshold.");
							writer.newLine();
						} else {
							lb.voteRemoval();
							if(lb.getRemovalVotes() >= BrickTrackerConfigFactory.getConfiguration().getNecessRemovalVotes(lb.getPercentCompleted())) {
								lcIterator.remove();
								Log.d(TAG, "Brick removed in frame "+frameCount+" with "+lb.getPercentCompleted()+"% completeness and "+overlap[counter]+" amount overlap.");
								
								if((Boolean)BrickTrackerConfigFactory.getConfiguration().getItem("FB")) {
									WorldCoordinate xy = new WorldCoordinate(lb.getCenterPoint()[0], lb.getCenterPoint()[1]);
									int z = Math.round((lb.getCenterPoint()[2]-(0.95f/2))/0.95f);
									
									if(falseBricks.get(xy) == null) {
										List<List<float[]>> zList = new ArrayList<List<float[]>>(4);
										for (int zListIdx = 0; zListIdx< 4; zListIdx++) {
											zList.add(new ArrayList<float[]>());
										}
										zList.get(z).add(lb.getCenterPoint());
										
										falseBricks.put(xy, zList);
									} else {
										falseBricks.get(xy).get(z).add(lb.getCenterPoint());
									}
								}
								
							}
						}
					} else if(overlap[counter] != 999) {
						lb.mergeOrientations(new LinkedList<Float>(Arrays.asList(orientation)));
					}
					counter++;
				}
				if(isNewDetected && !lc.isEmpty()) this.brickCandidates.add(lc);
//			}
		}
		
//		Log.d("PERFORMANCE_ANALYSIS", "CHECKPOINT4 time: "+(System.nanoTime() - start2)/1000000.0+"ms");
		TimerManager.stop();
		
		Log.d(TAG, "OverlapChecker loop1 times: "+acceptedBrickCandidates.size());
//		Log.d("PERFORMANCE_ANALYSIS", "Overlap Checker time: "+(System.nanoTime()-startTrackLines)/1000000L+"ms");
//		TimerManager.stop();
		
		
		Log.d(TAG, "CILoop amount: "+acceptedContours.size());
//		Log.d("PERFORMANCE_ANALYSIS", "CILoop time: "+(System.nanoTime() - startCILoop)/1000000.0+"ms");
//		TimerManager.stop();
		
//		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "LegoBrick found in "+(System.nanoTime()-start)/1000000L+"ms");
//		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "LegoBrick (Java-part) found in "+(System.nanoTime()-startJava)/1000000L+"ms");
		
		TimerManager.stop();
		
		writer.close();
		
		frameCount++;
	}
	
	public List<LegoBrickContainer> getNewBrickCandidates() {
		return brickCandidates;
	}
	
	public native float[] getOverlap(long bricksPointer);
	private native void findLegoBrickLines(long bgrPointer, float upAngle, double apdp, long colorCalibrationPtr, long resultMatPtr, long origContMatPtr);
	private native float[] checkOverlap(long inputPoints, int idx);
	private native void getConvexHull(long bricksPointer, long convexThreshPtr, long currFrameThreshPtr);
}
