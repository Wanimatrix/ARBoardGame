package be.wouterfranken.arboardgame.rendering.tracking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfFloat4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.gameworld.LegoBrick;
import be.wouterfranken.arboardgame.gameworld.LegoBrickContainer;
import be.wouterfranken.arboardgame.gameworld.WorldLines;
import be.wouterfranken.arboardgame.rendering.meshes.CuboidMesh;
import be.wouterfranken.arboardgame.rendering.meshes.RenderOptions;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPoseTracker.Mapper2D3D;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPoseTracker.ZMapper2D3D;
import be.wouterfranken.arboardgame.utilities.AndroidUtils;
import be.wouterfranken.arboardgame.utilities.Color;
import be.wouterfranken.arboardgame.utilities.Color.ColorName;
import be.wouterfranken.arboardgame.utilities.DebugUtilities;
import be.wouterfranken.arboardgame.utilities.MathUtilities;

public class LegoBrickTracker extends Tracker{
	private static final String TAG = LegoBrickTracker.class.getSimpleName();
	
	private Mat contour = new Mat();
	private Mat threshold = new Mat();
	private List<Color> contourColors = new ArrayList<Color>();
	private Mat contourExtern = new Mat();
	private Mat thresholdExtern = new Mat();
	private Mat brickPositionData = new Mat();
	private Mat originalContours = new Mat();
	private List<LegoBrickContainer> brickCandidates = new ArrayList<LegoBrickContainer>();
//	private List<LegoBrickContainer> brickContainers = new ArrayList<LegoBrickContainer>();
	private Object lock = new Object();
	private Object lockExtern = new Object();
//	private CountNonZero cnt;
	
	private Context ctx;
	
	public LegoBrickTracker(Context ctx) {
//		cnt = new CountNonZero(ctx);
		this.ctx = ctx;
		if(AppConfig.LEGO_TRACKING_CAD) { // Load all LegoBrick CAD renderings
			try {
				String[] fileNames = ctx.getAssets().list("model_imgs");
				Log.d(TAG, "File amount: "+fileNames.length);
				for (String fn : fileNames) {
					AndroidUtils.copyFileFromAssets(ctx, "model_imgs",fn);
				}
				AndroidUtils.copyFileFromAssets(ctx, "","hogTestImg.png");
				generateHOGDescriptors(ctx.getDir("execdir",Context.MODE_PRIVATE).getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class ContourInformation {
		public int[] upVIdx;
		public int upSize;
		public int anchorIdx;
		public float[] z0Pt;
		public float[] z1Pt;
		public int origContIdx;
		public Point[] points;
		
		
		public ContourInformation(Point[] points, int[] upVIdx, int upSize, int anchorIdx, float[] z0Pt,
				float[] z1Pt, int origContIdx) {
			super();
			this.points = points;
			this.upSize = upSize;
			this.upVIdx = upVIdx;
			this.anchorIdx = anchorIdx;
			this.z0Pt = z0Pt;
			this.z1Pt = z1Pt;
			this.origContIdx = origContIdx;
		}
	}
	
	
	public void findLegoBrick(Mat yuvFrameImage, Mat modelView, float orientation, FrameTrackingCallback trackingCallback,
			WorldLines currentWorld) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG,"Legobrick tracking ...");
		
		Log.d(TAG, "Orientation (in degrees): "+orientation);
		
		long start = System.nanoTime();
		
		if(AppConfig.LEGO_TRACKING_CAD) {
			
			findLegoBrick3(yuvFrameImage.getNativeObjAddr(), brickPositionData.getNativeObjAddr());
		} else if(AppConfig.LEGO_TRACKING_LINES) {
			Log.d(TAG,"LINES...");
			
			brickCandidates.clear();
			
			// Setup debug image.
			Mat check = new Mat();
			check = yuvFrameImage.clone();
			
			
			// Calculate global UpVector
			long startTrackLines = System.nanoTime();
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
	        Log.d("PERFORMANCE_ANALYSIS", "Calculate GlobalUpVec: "+(System.nanoTime()-startTrackLines)/1000000L+"ms");
	        
	        startTrackLines = System.nanoTime();
	        // Find legobrick contours
			findLegoBrickLines(yuvFrameImage.getNativeObjAddr(), upAngle, brickPositionData.getNativeObjAddr(), originalContours.getNativeObjAddr());
			Log.d("PERFORMANCE_ANALYSIS", "BrickDetection time (C++ part): "+(System.nanoTime()-startTrackLines)/1000000L+"ms");
			
			long startJava = System.nanoTime();
			
			// Convert contours to appropriate data types
			startTrackLines = System.nanoTime();
			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
			List<float[]> angles = new ArrayList<float[]>();
			List<Integer> idxes = new ArrayList<Integer>();
			for(int i = 0; i< brickPositionData.rows(); i++) {
				Point[] pts = new Point[4];
				float[] tmpAngles = new float[3];
//				Log.d(TAG, "Contour idx: "+i+", OrigIdx: "+brickPositionData.get(i, 0)[0]);
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
			Log.d("PERFORMANCE_ANALYSIS", "Convert C++ contours: "+(System.nanoTime()-startTrackLines)/1000000L+"ms");
			
			
			// GENERATE ORIGINAL CONTOUR LIST
//			List<MatOfPoint> originalContourList = new ArrayList<MatOfPoint>();
//			Log.d("ORIGCONT", "Total Size: ("+originalContours.rows()+","+originalContours.cols()+")");
//			for(int i = 0; i< originalContours.rows(); i++) {
//				Point[] pts = new Point[(int) originalContours.get(i, 0)[0]];
//				Log.d("ORIGCONT", "Contour Size: "+originalContours.get(i, 0)[0]);
//				for(int j = 0; j < pts.length; j++) {
//					pts[j] = new Point(originalContours.get(i, j*2+1)[0],originalContours.get(i, j*2+2)[0]);
//					Log.d("ORIGCONT", "Point: ("+pts[j].x+","+pts[j].y+")");
//				}
//				originalContourList.add(new MatOfPoint(pts));
//				Log.d("ORIGCONT", "List Size: "+originalContourList.size());
//			}
			
			
			// OLD SEARCH FOR HEIGHT!
//			for (int i = 0; i < originalContourList.size(); i++) {
//				Point[] contour = originalContourList.get(i).toArray();
//				if(contour.length == 0) continue;
//				
//				// SEARCH FOR A BOTTOM PIXEL
//			    Vector vecDown = new Vector();
//			    float downAngle = upAngle; // Pixel y axis is in the inverted direction
//			    float downAngleRad = (float) (downAngle * (Math.PI/180.0f));
//			    vecDown.x = Math.cos(downAngleRad);
//			    vecDown.y = Math.sin(downAngleRad);
//
//			    vecDown = vecDown.normalize();
//
//			    Point a = contour[0];
//			    Point b = new Point(a.x+1,(-vecDown.x/vecDown.y)+a.y);
//
//
//			    List<Integer> cornerHeight = new ArrayList<Integer>(contour.length);
//			    for (int j = 0; j < contour.length; j++) {
//					cornerHeight.add(-1);
//				}
//			    
//			    int bottomIdx = 0;
//			    for(int pidx = 1; pidx< contour.length; pidx++) {
//			    	Core.line(check, a, new Vector(a,b).resize(500).add(b), new Scalar(255,0,0));
//			    	Core.line(check, a, vecDown.resize(500).add(a), new Scalar(0,0,255));
//					Point p = contour[pidx];
//					Core.putText(check, MathUtilities.leftOrRightFromLine(a,b,p)+" == "+MathUtilities.leftOrRightFromLine(a,b,vecDown.resize(500).add(a)), 
//							p, Core.FONT_HERSHEY_PLAIN, 1, new Scalar(0,0,0));
//					if(MathUtilities.leftOrRightFromLine(a,b,p) == MathUtilities.leftOrRightFromLine(a,b,vecDown.resize(500).add(a))) {
//						a = p;
//						b = new Point(p.x+1,(-vecDown.x/vecDown.y)+p.y);
//						bottomIdx = pidx;
//					}
//			    }
//			    
//			    int height = 0;
//			    int firstUnknownIdx = -1;
//			    int secondUnknownIdx = -1;
//			    List<Integer> heightDiffIdxes = new ArrayList<Integer>();
//			    cornerHeight.set(bottomIdx, 0);
//			    int pidx = Utilities.getCircularIndex(bottomIdx+1, contour.length);
//			    while(pidx != bottomIdx) {
//			    	int factor = 0;
//			    	if(firstUnknownIdx != 0)
//			    		factor = firstUnknownIdx/Math.abs(firstUnknownIdx);
//			    	
//			    	Point prev = contour[Utilities.getCircularIndex(pidx+1*factor,contour.length)];
//			    	Point p = contour[Utilities.getCircularIndex(pidx, contour.length)];
//			    	Point next = contour[Utilities.getCircularIndex(pidx-1*factor, contour.length)];
//	
//			    	float angleDeg = new Vector(p, next).normalize().getAngleWithHorVector();
//	
//			    	if(MathUtilities.angleToDirectionalAngle(angleDeg) > (MathUtilities.angleToDirectionalAngle(upAngle)-25  % 180) 
//			    		  && MathUtilities.angleToDirectionalAngle(angleDeg) < (MathUtilities.angleToDirectionalAngle(upAngle)+25 % 180)) {
//			    		boolean up = true;
//			    		// Determine the z0 and z1 pt of the up-edge
//						float[] z0Pt = camPose.get3DPointFrom2D((float)p.x, (float)p.y, new CameraPoseTracker.ZMapper2D3D(height));
//						float[] z1Pt = camPose.get3DPointFrom2D((float)next.x, (float)next.y, 
//								new CameraPoseTracker.XYMapper2D3D(z0Pt[0],z0Pt[1]));
//						if(z1Pt[2] <= 0) {
//							z0Pt = camPose.get3DPointFrom2D((float)next.x, (float)next.y, new CameraPoseTracker.ZMapper2D3D(height));
//							z1Pt = camPose.get3DPointFrom2D((float)p.x, (float)p.y, 
//									new CameraPoseTracker.XYMapper2D3D(z0Pt[0],z0Pt[1]));
//							up = false;
//						}
//			    	  
//						int upSize = Math.round(z1Pt[2]/0.95f);
//						if(up){ // Going UP
//							cornerHeight.set(pidx, height);
//							height += upSize;
//						}
//						else { // Going DOWN
//							cornerHeight.set(pidx, height);
//							height -= upSize;
//						}
//			    	} else if(height != 0 && MathUtilities.leftOrRightFromLine(prev,p,next) == 1*factor){ // Left turn, but not on bottom line!
//			    		
//			    		if(firstUnknownIdx == -1) {
//			    			firstUnknownIdx = pidx;
//			    			pidx = Utilities.getCircularIndex(bottomIdx-1, contour.length);
//			    			height = 0;
//			    			continue;
//			    		} else {
//			    			secondUnknownIdx = pidx;
//			    			break;
//			    		}
//			    	}
//			    	if(firstUnknownIdx == -1)
//			    		pidx = Utilities.getCircularIndex(pidx+1, contour.length);
//			    	else
//			    		pidx = Utilities.getCircularIndex(pidx-1, contour.length);
//			    }
//			    
//			    Core.circle(check, contour[bottomIdx], 3, new Scalar(255,0,0));
//			    if(firstUnknownIdx != -1 && secondUnknownIdx != -1) {
//				    Core.circle(check, contour[firstUnknownIdx], 3, new Scalar(0,255,0));
//				    Core.circle(check, contour[secondUnknownIdx], 3, new Scalar(0,255,0));
//			    }
////			    if()
//			}
			
			// Filter contours and convert to 3D LegoBricks
			startTrackLines = System.nanoTime();
			Iterator<MatOfPoint> it = contours.iterator();
			List<Integer> contourOrigIdx = new ArrayList<Integer>(); // Holds the original indexes of accepted contours
			List<ContourInformation> acceptedContours = new ArrayList<ContourInformation>();  // Holds the accepted contours
			int i = -1;
			int newIdx = 0;
			while(it.hasNext()) {
				i++;
				MatOfPoint contour = it.next();
				Point[] pts = contour.toArray();
				
//				// For the same contour, another contour was already accepted.
//				if(acceptedContours.contains((int)brickPositionData.get(i,0)[0])) {
//					it.remove();
//			    	continue;
//				}
				
				
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
					it.remove();
					idxes.remove(newIdx);
			    	continue;
				}
				
//				Log.d(TAG, "Contour coverage ratio: "+Math.abs(Imgproc.contourArea(contour)/brickPositionData.get(i,2)[0]-1));
////				Log.d(TAG, "Contour coverage ratio: "+Imgproc.contourArea(contour)/brickPositionData.get(i,2)[0]-1);
//				
				ContourInformation contourInfo = new ContourInformation(pts, upVIdx, upSize,anchorPtIdx,z0Pt,z1Pt,(int)brickPositionData.get(i,0)[0]);
				
				newIdx++;
//				
//				boolean removed = false;
//				for (int j = 0; j < acceptedContours.size(); j++) {
//					ContourInformation ci = acceptedContours.get(j);
//					if(ci.origContIdx != (int)brickPositionData.get(i,0)[0]) continue;
//					if(Math.abs(Imgproc.contourArea(contour)/brickPositionData.get(i,2)[0]-1) < ci.areaRatio) {
//						contourOrigIdx.add(i);
//						acceptedContours.set(j, contourInfo);
//					} else {
//						it.remove();
//						removed = true;
//						break;
//					}
//				}
//				if(removed) continue;
				
				
				if(!acceptedContours.contains(contourInfo)) {
					contourOrigIdx.add(i);
					acceptedContours.add(contourInfo);
				}
				
//				Log.d(TAG, "BrickSize "+(int)brickPositionData.get(i,0)[0]+": "+allSizes[0]+","+allSizes[1]+","+allSizes[2]);
				Log.d(TAG, "UpVector Idxes: "+upVIdx[0]+","+upVIdx[1]);
			}
			
			Log.d("PERFORMANCE_ANALYSIS", "Filter contours: "+(System.nanoTime()-startTrackLines)/1000000L+"ms");
			
			List<LegoBrickContainer> acceptedBrickCandidates = new ArrayList<LegoBrickContainer>();
			
			long startCILoop = System.nanoTime();
			
			for (ContourInformation ci : acceptedContours) {
				
				// Calculate sizes and vectors of all sides.
				startTrackLines = System.nanoTime();
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
				Log.d("PERFORMANCE_ANALYSIS", "Find sizes and side vectors: "+(System.nanoTime()-startTrackLines)/1000000L+"ms");
				
				startTrackLines = System.nanoTime();
				
				int amountOfBricks = allSizes[1]*allSizes[2]*allSizes[0];
				if(amountOfBricks == 0) continue;
				Log.d(TAG, "Amount Of Bricks: "+amountOfBricks+": ("+allSizes[0]+","+allSizes[1]+","+allSizes[2]+")");
				
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
					
//					cuboids[k][0][0] = anchorPt3D;
//					cuboids[k][0][3] = MathUtilities.vectorToPoint(sideVectors[1], cuboids[k][0][0]);
//					cuboids[k][0][2] = MathUtilities.vectorToPoint(sideVectors[2], cuboids[k][0][3]);
//					cuboids[k][0][1] = MathUtilities.vectorToPoint(sideVectors[2], cuboids[k][0][0]);
//					cuboids[k][0][4] = MathUtilities.vectorToPoint(sideVectors[0], cuboids[k][0][0]);
//					cuboids[k][0][7] = MathUtilities.vectorToPoint(sideVectors[1], cuboids[k][0][4]);
//					cuboids[k][0][6] = MathUtilities.vectorToPoint(sideVectors[2], cuboids[k][0][7]);
//					cuboids[k][0][5] = MathUtilities.vectorToPoint(sideVectors[2], cuboids[k][0][4]);
				
				
				
				
				
	//				acceptedBricks.put(ci.origContIdx, new LegoBrick(cuboids[0], ColorName.BLUE, null));
					
					if(tmpBricks.size() <= 0)
						tmpBricks.add(new LegoBrickContainer(new LegoBrick(cuboids[k][0], halfSideVectors, orientation, 0)));
					else 
						tmpBricks.get(0).add((new LegoBrick(cuboids[k][0], halfSideVectors, orientation,0 )));
					
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
//								for(int rowIdx = 0; rowIdx < cuboids[k][cuboidsIdx].length; rowIdx++) {
//									cuboids[k][cuboidsIdx][rowIdx] = new float[3];
								newBlock.get(0, 0, cuboids[k][cuboidsIdx]);
	//								cuboids[cuboidsIdx][rowIdx] = ((MatOfFloat4)newBlock.row(rowIdx)).toArray();
//								}
								
								if(tmpBricks.size() <= cuboidsIdx)
									tmpBricks.add(new LegoBrickContainer(new LegoBrick(cuboids[k][cuboidsIdx], halfSideVectors, orientation, 0)));
								else 
									tmpBricks.get(cuboidsIdx).add((new LegoBrick(cuboids[k][cuboidsIdx], halfSideVectors, orientation, 0)));
	//							acceptedBricks.put(ci.origContIdx, new LegoBrick(cuboids[cuboidsIdx], ColorName.BLUE, null));
								cuboidsIdx++;
							}
						}
					}
					
					acceptedBrickCandidates.addAll(tmpBricks);
				}
				
				Log.d("PERFORMANCE_ANALYSIS", "Split brick in 2x2 bricks time: "+(System.nanoTime()-startTrackLines)/1000000L+"ms");
				
				startTrackLines = System.nanoTime();
				
				// Check input containers for overlap
//				for (LegoBrickContainer legoBrickContainer : brickCandidatesIn) {
//					Iterator<LegoBrick> brickIterator = legoBrickContainer.iterator();
//					while(brickIterator.hasNext()) {
//						LegoBrick brick = brickIterator.next();
//						float[] overlapResult = brick.getOverlap(modelView);
//						if(overlapResult[0] < 0.75f) {
//							brick.voteRemoval();
//							if(brick.getRemovalVotes() >= 4) {
//								brickIterator.remove();
//								Log.d(TAG, "Brick Removed");
//							}
//						}
//					}
//				}
				
//				Log.d(TAG, "Input container overlapCheck time: "+(System.nanoTime() - startOverlapCheck)/1000000.0+"ms");
				
				
				
				
				// Overlap test!
//				coord3D = Mat.ones(4, 8, CvType.CV_32FC1);
//				coord3D.put(0, 0, cuboid[0][0]);
//				coord3D.put(1, 0, cuboid[0][1]);
//				coord3D.put(2, 0, cuboid[0][2]);
//				coord3D.put(0, 1, cuboid[1][0]);
//				coord3D.put(1, 1, cuboid[1][1]);
//				coord3D.put(2, 1, cuboid[1][2]);
//				coord3D.put(0, 2, cuboid[2][0]);
//				coord3D.put(1, 2, cuboid[2][1]);
//				coord3D.put(2, 2, cuboid[2][2]);
//				coord3D.put(0, 3, cuboid[3][0]);
//				coord3D.put(1, 3, cuboid[3][1]);
//				coord3D.put(2, 3, cuboid[3][2]);
//				coord3D.put(0, 4, cuboid[4][0]);
//				coord3D.put(1, 4, cuboid[4][1]);
//				coord3D.put(2, 4, cuboid[4][2]);
//				coord3D.put(0, 5, cuboid[5][0]);
//				coord3D.put(1, 5, cuboid[5][1]);
//				coord3D.put(2, 5, cuboid[5][2]);
//				coord3D.put(0, 6, cuboid[6][0]);
//				coord3D.put(1, 6, cuboid[6][1]);
//				coord3D.put(2, 6, cuboid[6][2]);
//				coord3D.put(0, 7, cuboid[7][0]);
//				coord3D.put(1, 7, cuboid[7][1]);
//				coord3D.put(2, 7, cuboid[7][2]);
//				coord2D = camPose.get2DPointFrom3D(coord3D, camPose.getMvMat());
//				
//				Point[] reprojected2DPts = new Point[8];
//				reprojected2DPts[0] = new Point(coord2D.get(0,0)[0]/coord2D.get(2,0)[0],coord2D.get(1,0)[0]/coord2D.get(2,0)[0]);
//				reprojected2DPts[1] = new Point(coord2D.get(0,1)[0]/coord2D.get(2,1)[0],coord2D.get(1,1)[0]/coord2D.get(2,1)[0]);
//				reprojected2DPts[2] = new Point(coord2D.get(0,2)[0]/coord2D.get(2,2)[0],coord2D.get(1,2)[0]/coord2D.get(2,2)[0]);
//				reprojected2DPts[3] = new Point(coord2D.get(0,3)[0]/coord2D.get(2,3)[0],coord2D.get(1,3)[0]/coord2D.get(2,3)[0]);
//				reprojected2DPts[4] = new Point(coord2D.get(0,4)[0]/coord2D.get(2,4)[0],coord2D.get(1,4)[0]/coord2D.get(2,4)[0]);
//				reprojected2DPts[5] = new Point(coord2D.get(0,5)[0]/coord2D.get(2,5)[0],coord2D.get(1,5)[0]/coord2D.get(2,5)[0]);
//				reprojected2DPts[6] = new Point(coord2D.get(0,6)[0]/coord2D.get(2,6)[0],coord2D.get(1,6)[0]/coord2D.get(2,6)[0]);
//				reprojected2DPts[7] = new Point(coord2D.get(0,7)[0]/coord2D.get(2,7)[0],coord2D.get(1,7)[0]/coord2D.get(2,7)[0]);
//				
//				float[] overlapResult = checkOverlap(new MatOfPoint(reprojected2DPts).getNativeObjAddr(), ci.origContIdx);
//				
//				if(overlapResult[0] > 0.90f && overlapResult[1] > 0.90f) { //0.93-0.98
//					Log.d(TAG, "Contour with orig idx: "+overlapResult[0]+", "+overlapResult[1]);
//					if(acceptedBricks.get(ci.origContIdx) != null){
//						float[] previousOverlapResult = acceptedBricks.get(ci.origContIdx).getOverlap();
//						if((previousOverlapResult[0]+previousOverlapResult[1])/2 < (overlapResult[0]+overlapResult[1])/2) {
//							acceptedBricks.put(ci.origContIdx, new LegoBrick(cuboid, ColorName.BLUE, overlapResult));
//						}
//					} else {
//						acceptedBricks.put(ci.origContIdx, new LegoBrick(cuboid, ColorName.BLUE, overlapResult));
//					}
//				} else {
//					Log.d(TAG, "Fail overlap test: "+overlapResult[0]+", "+ overlapResult[1]);
//				}
			}
			
			
			long start2 = System.nanoTime();
			// Check new containers for overlap and add them to the brickCandidates
			List<LegoBrick> tmpBricksArray = new ArrayList<LegoBrick>();
			for (LegoBrickContainer legoBrickContainer : acceptedBrickCandidates) {
				tmpBricksArray.addAll(legoBrickContainer);
			}
			
			for (LegoBrickContainer legoBrickContainer : currentWorld.getCandidateBricks()) {
				tmpBricksArray.addAll(legoBrickContainer);
			}
			
			tmpBricksArray.addAll(currentWorld.getBricks());
			
			Log.d("PERFORMANCE_ANALYSIS", "CHECKPOINT1 time: "+(System.nanoTime() - start2)/1000000.0+"ms");
			
			start2 = System.nanoTime();
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
			Log.d("PERFORMANCE_ANALYSIS", "CHECKPOINT2 time: "+(System.nanoTime() - start2)/1000000.0+"ms");
			
			long startOverlapCalc = System.nanoTime();
			coord2D = CameraPoseTracker.get2DPointFrom3D(coord3D, modelView);
			Log.d("PERFORMANCE_ANALYSIS", "OverlapCalc time: "+(System.nanoTime() - startOverlapCalc)/1000000.0+"ms");
			
			Log.d(TAG, "COORD2D size: "+coord2D.cols()+" VS "+tmpBricksArray.size());
			
			start2 = System.nanoTime();
//			List<float[]> overlap = new ArrayList<float[]>();
			Mat bricksMat = new Mat(tmpBricksArray.size(),8*2, CvType.CV_32FC1);
//			Point[] tmpPts0 = new Point[8];
			for (int l = 0; l < tmpBricksArray.size(); l++) {
				for (int m = 0; m < 8; m++) {
					bricksMat.put(l, m*2, coord2D.get(0,l*8+m)[0]/coord2D.get(2,l*8+m)[0]);
					bricksMat.put(l, m*2+1, coord2D.get(1,l*8+m)[0]/coord2D.get(2,l*8+m)[0]);
//					if(l == 0) {
//						tmpPts0[m] = new Point(coord2D.get(0,m)[0]/coord2D.get(2,m)[0],coord2D.get(1,m)[0]/coord2D.get(2,m)[0]);
//					}
				}
//				overlap.add(tmpBricksArray.get(l).getOverlap(tmpPts));
			}
			
			float[] overlap = getOverlap(bricksMat.getNativeObjAddr());
			
			
			// TRYING TO USE RENDERSCRIPT, GIVES WRONG VALUES
//			if(tmpBricksArray.size() > 0) {
//				Mat convex = new Mat();
//				Mat currFrameThresh = new Mat();
//				Mat pts = new MatOfPoint(tmpPts0);
//				getConvexHull(pts.getNativeObjAddr(), convex.getNativeObjAddr(), currFrameThresh.getNativeObjAddr());
//				
////				Highgui.imwrite("/sdcard/arbg/convex.png", convex);
////				Mat result = new Mat();
////				Core.bitwise_and(convex, currFrameThresh, result);
////				Highgui.imwrite("/sdcard/arbg/result.png", result);
//				int nonZero;
//				try {
//					long nonzeroTime = System.nanoTime();
//					nonZero = cnt.getCountNonZero(convex);
//					
//				} catch (Exception e) {
//					Log.e("NONZEROCOUNTER", e.getMessage());
//				}
//			}
			
			Log.d("PERFORMANCE_ANALYSIS", "CHECKPOINT3 time: "+(System.nanoTime() - start2)/1000000.0+"ms");
			
			start2 = System.nanoTime();
			int counter = 0;
			for (int k = 0; k < overlap.length; k++) {
				if(k >= acceptedBrickCandidates.size()+currentWorld.getCandidateBricks().size()) {
					LegoBrick b = tmpBricksArray.get(k);
					Log.d("REALBRICKREMOVAL", "Realbrick, overlap check: "+overlap[counter]);
//					if(overlap[counter] < 0.5f) {
//						b.voteRemoval();
//						if(b.getRemovalVotes() >= 3) {
//							Log.d("REALBRICKREMOVAL", "Real brick was removed!");
//							currentWorld.removeBrick(b);
//						}
//					}
					counter++;
				} else {
					boolean isNewDetected = (k < acceptedBrickCandidates.size());
					LegoBrickContainer lc = isNewDetected ? acceptedBrickCandidates.get(k) : currentWorld.getCandidateBricks().get(k-acceptedBrickCandidates.size());
					Iterator<LegoBrick> lcIterator = lc.iterator();
					while(lcIterator.hasNext()) {
						LegoBrick lb = lcIterator.next();
	//					float[] overlapResult = overlap.get(counter);
	//					long startOverlapCalc = System.nanoTime();
	//					float[] overlapResult = legoBrickContainer.get(l).getOverlap(camPose);
	//					Log.d(TAG, "OverlapCalc time: "+(System.nanoTime() - startOverlapCalc)/1000000.0+"ms");
						
						if(overlap[counter] < 0.75f) {
	//						bricks.add(legoBrickContainer.get(l));
							if(isNewDetected)
								lcIterator.remove();
							else {
								lb.voteRemoval();
								if(lb.getRemovalVotes() >= 3)
									lcIterator.remove();
							}
						}
						counter++;
					}
					if(isNewDetected && !lc.isEmpty()) this.brickCandidates.add(lc);
				}
//				int[] mergeResult = legoBrickContainer.mergeCheck(bricks.toArray(new LegoBrick[bricks.size()]));
//				for (int l = 0; l < mergeResult.length; l++) {
//					if(mergeResult[l] != -1) {
//						bricks.get(mergeResult[l]).voteUp(5);
//					} else {
//						
//					}
//				}
			}
			
			Log.d("PERFORMANCE_ANALYSIS", "CHECKPOINT4 time: "+(System.nanoTime() - start2)/1000000.0+"ms");
			
			Log.d(TAG, "OverlapChecker loop1 times: "+acceptedBrickCandidates.size());
//			Log.d(TAG, "OverlapChecker time loop1: "+(System.nanoTime() - startOverlapCheck)/1000000.0+"ms");
			Log.d("PERFORMANCE_ANALYSIS", "Overlap Checker time: "+(System.nanoTime()-startTrackLines)/1000000L+"ms");
			
//			Log.d(TAG, "AcceptedBricks size: "+acceptedBricks.size());
			
			
			Log.d(TAG, "CILoop amount: "+acceptedContours.size());
			Log.d("PERFORMANCE_ANALYSIS", "CILoop time: "+(System.nanoTime() - startCILoop)/1000000.0+"ms");
			
//			long startOverlapCheck = System.nanoTime();
//			Iterator<LegoBrick> iter = bricks.iterator();
//			while (iter.hasNext()) {
//				LegoBrick legoBrick = iter.next();
//				if(legoBrick.isVisible(camPose)) {
//					legoBrick.addVisibleFrame();
////				
//					float[] overlapResult = legoBrick.getOverlap(camPose);
//					if(overlapResult[0] < 0.9f) {
//						legoBrick.punish(1);
//	//						iter.remove();
//	//						continue;
//					}
//				}
//			}
//			Log.d(TAG, "OverlapChecker loop2 times: "+bricks.size());
//			Log.d(TAG, "OverlapChecker time loop2: "+(System.nanoTime() - startOverlapCheck)/1000000.0+"ms");
			
//			Log.d(TAG, "Accepted bricksSize: "+acceptedBricks.size());
			
//			int key = 0;
//			for(int ab = 0; ab < acceptedBricks.size(); ab++) {
//			   key = acceptedBricks.keyAt(ab);
//			   bricks.add(acceptedBricks.get(key));
//			}
			
//			Imgproc.drawContours(check, contours, -1, new Scalar(0, 255, 0));
//			Highgui.imwrite("/sdcard/arbg/check.png", check);
			
//			trackingCallback.trackingDone(LegoBrickTracker.class);
			
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "LegoBrick found in "+(System.nanoTime()-start)/1000000L+"ms");
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "LegoBrick (Java-part) found in "+(System.nanoTime()-startJava)/1000000L+"ms");
			
			
		} else if(AppConfig.PARALLEL_LEGO_TRACKING) {
			FindLegoBrick task = new FindLegoBrick();
			task.start = start;
			task.setupFrameTrackingCallback(trackingCallback);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, yuvFrameImage);
		} else {
			Mat threshold = new Mat();
			findLegoBrick2( 
					yuvFrameImage.getNativeObjAddr(),
					threshold.getNativeObjAddr()
					);
			setThreshold(threshold);
			trackingCallback.trackingDone(LegoBrickTracker.class);
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "LegoBrick found in "+(System.nanoTime()-start)/1000000L+"ms");
		}
		
		// This makes an image with a rectangle that shows one of the best results.
//		Mat testImg = Highgui.imread(ctx.getDir("execdir",Context.MODE_PRIVATE).getAbsolutePath()+"/hogTestImg.jpg");
//		Core.rectangle(testImg, new Point(brickPositionData.get(7, 3)[0], brickPositionData.get(7, 4)[0]), 
//				new Point((brickPositionData.get(7, 3)[0]+brickPositionData.get(7, 5)[0]), (brickPositionData.get(7, 4)[0])+brickPositionData.get(7, 6)[0]), 
//				new Scalar(0,0,255));
//		Highgui.imwrite("/sdcard/arbg/hogMatchingResult1.png", testImg);

//		findLegoBrick( 
//				yuvFrameImage.getNativeObjAddr(),
//				contour.getNativeObjAddr()
//				);
	}
	
	public List<LegoBrickContainer> getNewBrickCandidates() {
		return brickCandidates;
	}
	
//	public float[][] getGLContour() {
//		Mat tmp = new Mat();
//		synchronized (lockExtern) {
//			contourExtern.copyTo(tmp);
//		}
//		if(tmp.empty()) return null;
//		float[][] result = new float[tmp.rows()][];
//		for (int j = 0; j < tmp.rows(); j++) {
//			int cornerAmount = (int) tmp.get(j,1)[0];
//			result[j] = new float[cornerAmount*3];
//			for (int i = 0; i < cornerAmount*3; i+=3) {
//				if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Point "+i/3+": ("+tmp.get(j,2*i/3+2)[0]+","+tmp.get(j,2*i/3+3)[0]+")");
//				result[j][i] = (float) ((tmp.get(j,2*i/3+2)[0]/AppConfig.PREVIEW_RESOLUTION[0])*2-1);
//				result[j][i+1] = (float) ((tmp.get(j,2*i/3+3)[0]/AppConfig.PREVIEW_RESOLUTION[1])*2-1);
//				result[j][i+2] = 0;
//			}
//		}
//		return result;
//	}
	
	public native float[] getOverlap(long bricksPointer);
	
	public float[][] getOcvContour() {
		Mat tmp = new Mat();
		synchronized (lockExtern) {
			contourExtern.copyTo(tmp);
		}
		if(tmp.empty()) return null;
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Contours found: "+tmp.rows());
		float[][] result = new float[tmp.rows()][];
		for (int j = 0; j < tmp.rows(); j++) {
			int cornerAmount = (int) tmp.get(j,1)[0];
			result[j] = new float[cornerAmount*3+1];
			result[j][0] = (int) tmp.get(j,0)[0];
			for (int i = 0; i < cornerAmount*3; i+=3) {
				if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Point "+i/3+": ("+tmp.get(j,2*i/3+2)[0]+","+tmp.get(j,2*i/3+3)[0]+")");
				result[j][i+1] = (float) (tmp.get(j,2*i/3+2)[0]);
				result[j][i+2] = (float) (tmp.get(j,2*i/3+3)[0]);
				result[j][i+3] = 0;
			}
		}
		return result;
	}
	
	private float[] calculateSetOfEq(float[] p0, float[] v0, float[] p1, float[] v1, float[] v2) {
//		float tmp = v2[2]*v0[1]-v2[1]*v0[2];
//		float zv0Quad = (float)Math.pow(v0[2], 2);
//		float denom = -v1[0]+v1[2]*v0[0]*tmp-v1[1]*zv0Quad*v2[0]+v1[2]*v0[1]*v0[2]*v2[0]+v1[1]*v0[2]*v2[2]*v0[0]-v1[2]*v0[1]*v2[2]*v0[0];
//		float nom = p1[0]*v0[2]*tmp-p0[0]*v0[2]*tmp+p1[1]*zv0Quad*v2[0]-p0[1]*zv0Quad*v2[0]-p1[2]*v0[1]*v0[2]*v2[0]+p0[2]*v0[1]*v0[2]
//						-p1[2]*v0[1]*tmp+p0[2]*v0[1]*tmp-v0[0]*v2[2]*(p1[0]*v0[2]-p0[1]*v0[2]-p1[2]*v0[1]+p0[2]*v0[1]);
//		float s = nom/denom;
//		float q = (p1[1]*v0[2]-p0[1]*v0[2]+s*v1[1]*v0[2]-p1[2]*v0[1]+p0[2]*v0[1]-s*v1[2]*v0[1])/tmp;
//		float t = (p1[2]-p0[2]+s*v1[2]+q*v2[2])/v0[2];
		
		float fac1 = (v2[1]*v0[2] - v0[1]*v2[2]);
		float fac2 = (v2[1]*v1[2] - v1[1]*v2[2]);
		float fac3 = (v1[1]*v0[2] - v0[1]*v1[2]);
		float fac4 = (p0[2] - p1[2]);
		float fac5 = (v2[1]*fac4 - p0[1]*v2[2] + p1[1]*v2[2]);
		float fac6 = (v0[1]*fac4 - p0[1]*v0[2] + p1[1]*v0[2]);
		float fac7 = (v1[1]*fac4 - p0[1]*v1[2] + p1[1]*v1[2]);

		float nom = -(fac1*p0[0] - fac1*p1[0] - fac5*v0[0] + fac6*v2[0]);

		float denom = (fac2*v0[0] - fac1*v1[0] + fac3*v2[0]);

		float s = nom/denom;

		nom = (fac3*p0[0] - fac3*p1[0] - fac7*v0[0] + fac6*v1[0]);

		float q = nom/denom;

		nom = -(fac2*p0[0] - fac2*p1[0] - fac5*v1[0] + fac7*v2[0]);

		float t = nom/denom;
		
		return new float[]{s,q,t};
	}
	
	public CuboidMesh getTrackedLegoBricks(Mat inputImg, CameraPoseTracker camPose) {
		//TODO: FOR EACH SCALE:
		//			Rotate this brick according to the correct matched viewpoint.
		//		END FOR
		//		This gives us the resulting brick. :)
		
		/**
		 * Left,Bottom: (144,411)
		 * Right,Top: (454,222)
		 * Template size: (600,600)
		 * */
		
		float[][] result = new float[8][];
		
		int scale = 0;
		Mat scaleResult = brickPositionData.row(scale);
		
		Log.d(TAG, "CameraPose Found: "+camPose.cameraPoseFound());
		
		if(!camPose.cameraPoseFound()) return null;
		
		Log.d(TAG, "Placing Brick in window...");
		
		Mat inputMv = camPose.getMvMat();
		float distanceToBrick = (float)scaleResult.get(0,0)[0];
		float distanceToFirstBrickPlane = distanceToBrick - 0.8f;
		float distanceToSecondBrickPlane = distanceToBrick + 0.8f;
		
		Log.d("HOGDETECT", "Distance to first plane: "+ distanceToFirstBrickPlane);
		
		int phi = (int)scaleResult.get(0,1)[0];
		int theta = (int)scaleResult.get(0,2)[0];
		Point location = new Point(scaleResult.get(0,3)[0],scaleResult.get(0,4)[0]);
		Size templSize = new Size(scaleResult.get(0,5)[0],scaleResult.get(0,6)[0]);
		
		Log.d("HOGDETECT", "Template size: [ "+templSize.width+" x "+templSize.height+" ]");
		
		Point lb = new Point(location.x+144.0*(templSize.width/600.0),location.y+411.0*(templSize.height/600.0));
		float[] lbp0 = camPose.get3DPointFrom2D((float)lb.x, (float)lb.y, new CameraPoseTracker.ZMapper2D3D(0));
		float[] lbp1 = camPose.get3DPointFrom2D((float)lb.x, (float)lb.y, new CameraPoseTracker.ZMapper2D3D(1));
		float[] lbv = MathUtilities.vector(lbp1, lbp0);
		
		Point lt = new Point(location.x+144.0*(templSize.width/600.0),location.y+222.0*(templSize.height/600.0));
		float[] ltp0 = camPose.get3DPointFrom2D((float)lt.x, (float)lt.y, new CameraPoseTracker.ZMapper2D3D(0));
		float[] ltp1 = camPose.get3DPointFrom2D((float)lt.x, (float)lt.y, new CameraPoseTracker.ZMapper2D3D(1));
		float[] ltv = MathUtilities.vector(ltp1, ltp0);
		
		Point rt = new Point(location.x+454.0*(templSize.width/600.0),location.y+222.0*(templSize.height/600.0));
		float[] rtp0 = camPose.get3DPointFrom2D((float)rt.x, (float)rt.y, new CameraPoseTracker.ZMapper2D3D(0));
		float[] rtp1 = camPose.get3DPointFrom2D((float)rt.x, (float)rt.y, new CameraPoseTracker.ZMapper2D3D(1));
		float[] rtv = MathUtilities.vector(rtp1, rtp0);
		
		Point rb = new Point(location.x+454.0*(templSize.width/600.0),location.y+411.0*(templSize.height/600.0));
		float[] rbp0 = camPose.get3DPointFrom2D((float)rb.x, (float)rb.y, new CameraPoseTracker.ZMapper2D3D(0));
		float[] rbp1 = camPose.get3DPointFrom2D((float)rb.x, (float)rb.y, new CameraPoseTracker.ZMapper2D3D(1));
		float[] rbv = MathUtilities.vector(rbp1, rbp0);
		
		float[] pp0 = camPose.get3DPointOnImagePlane((float)location.x, (float)location.y);
		float[] pp1 = camPose.get3DPointOnImagePlane((float)(location.x+templSize.width), (float)location.y);
		float[] pp2 = camPose.get3DPointOnImagePlane((float)location.x, (float)(location.y+templSize.height));
		
		float[] pv1 = MathUtilities.vector(pp0, pp1);
		float[] pv2 = MathUtilities.vector(pp0, pp2);
		
		float[] normalVec = MathUtilities.resize(MathUtilities.cross(pv1, pv2),1.0f);
		
		float[] p0p1 = MathUtilities.vectorToPoint(MathUtilities.resize(normalVec, distanceToFirstBrickPlane), pp0);
		float[] p0p2 = MathUtilities.vectorToPoint(MathUtilities.resize(normalVec, distanceToSecondBrickPlane), pp0);
		
		float[] sqt = calculateSetOfEq(lbp0, lbv, p0p1, pv1, pv2);
		
		result[0] = MathUtilities.vectorToPoint(MathUtilities.multiply(lbv, sqt[2]), lbp0);
		sqt = calculateSetOfEq(ltp0, ltv, p0p1, pv1, pv2);
		result[3] = MathUtilities.vectorToPoint(MathUtilities.multiply(ltv, sqt[2]), ltp0);
		sqt = calculateSetOfEq(rtp0, rtv, p0p1, pv1, pv2);
		result[2] = MathUtilities.vectorToPoint(MathUtilities.multiply(rtv, sqt[2]), rtp0);
		sqt = calculateSetOfEq(rbp0, rbv, p0p1, pv1, pv2);
		result[1] = MathUtilities.vectorToPoint(MathUtilities.multiply(rbv, sqt[2]), rbp0);
		
		sqt = calculateSetOfEq(lbp0, lbv, p0p2, pv1, pv2);
		result[4] = MathUtilities.vectorToPoint(MathUtilities.multiply(lbv, sqt[2]), lbp0);
		sqt = calculateSetOfEq(ltp0, ltv, p0p2, pv1, pv2);
		result[7] = MathUtilities.vectorToPoint(MathUtilities.multiply(ltv, sqt[2]), ltp0);
		sqt = calculateSetOfEq(rtp0, rtv, p0p2, pv1, pv2);
		result[6] = MathUtilities.vectorToPoint(MathUtilities.multiply(rtv, sqt[2]), rtp0);
		sqt = calculateSetOfEq(rbp0, rbv, p0p2, pv1, pv2);
		result[5] = MathUtilities.vectorToPoint(MathUtilities.multiply(rbv, sqt[2]), rbp0);
		
		float[] vec = MathUtilities.vector(result[0], result[6]);
		vec = MathUtilities.resize(vec, MathUtilities.norm(vec)/2.0f);
//		float[] translation = MathUtilities.vectorToPoint(vec, result[0]);
		float[] translation = new float[]{(result[0][0]+result[6][0])/2.0f,(result[0][1]+result[6][1])/2.0f,(result[0][2]+result[6][2])/2.0f};
		float[] rotationAxisTheta = MathUtilities.vector(result[0], result[3]);
		rotationAxisTheta = MathUtilities.resize(rotationAxisTheta, 1);
		float[] rotationAxisPhi = MathUtilities.vector(result[1], result[0]);
		rotationAxisPhi = MathUtilities.resize(rotationAxisPhi, 1);
		
		Log.d("HOGDETECT", "Translation: ("+translation[0]+", "+translation[1]+","+translation[2]+")");
		
		Log.d("HOGDETECT", "CUBOID: ");
		for (int i = 0; i < result.length; i++) {
			Log.d("HOGDETECT", "Coord: ("+result[i][0]+", "+result[i][1]+","+result[i][2]+")");
		}
		
		float[] transform = new float[16];
		Matrix.setIdentityM(transform, 0);
		Matrix.translateM(transform, 0, translation[0], translation[1], translation[2]);
		Matrix.rotateM(transform, 0, phi, rotationAxisPhi[0],rotationAxisPhi[1],rotationAxisPhi[2]);
		Matrix.rotateM(transform, 0, theta, rotationAxisTheta[0],rotationAxisTheta[1],rotationAxisTheta[2]);
		Matrix.translateM(transform, 0, -translation[0], -translation[1], -translation[2]);
		DebugUtilities.logGLMatrix("Transformation: ", transform, 4, 4);
		
		for (int i = 0; i < result.length; i++) {
			float[] tmpRes = new float[4];
			Matrix.multiplyMV(tmpRes, 0, transform, 0, new float[]{result[i][0],result[i][1],result[i][2],1}, 0);
			result[i] = new float[]{tmpRes[0],tmpRes[1],tmpRes[2]};
		}
		
		Log.d("HOGDETECT", "CUBOID RESULT: ");
		for (int i = 0; i < result.length; i++) {
			Log.d("HOGDETECT", "Coord: ("+result[i][0]+", "+result[i][1]+","+result[i][2]+")");
		}
		
//		Matrix.rotateM(transform, 0, theta, rotationAxisTheta[0],rotationAxisTheta[1],rotationAxisTheta[2]);
//		Matrix.rotateM(transform, 0, phi, rotationAxisPhi[0],rotationAxisPhi[1],rotationAxisPhi[2]);
//		Matrix.translateM(transform, 0, translation[0], translation[1], translation[2]);
		
		CuboidMesh mesh = new CuboidMesh(result, new RenderOptions(true, new Color(1, 0, 0, 1), true, transform));
		
//		LegoBrick brick = new LegoBrick(result, ColorName.RED);
		
		// THIS IS DEBUG HELP
		Mat saveImg = inputImg.clone();
		
		Log.d("HOGDETECT", "Template location: ("+location.x+","+location.y+"),("+(location.x+templSize.width)+","+(location.y+templSize.height)+")");
		
		Core.rectangle(saveImg,location,
                            new Point(location.x+templSize.width,location.y+templSize.height),
                            new Scalar(0,0,255));
		
		Mat points3D = Mat.ones(4, 4, CvType.CV_32FC1);
		for(int i = 0; i < 4;i++) {
			points3D.put(0, i, result[i][0]);
			points3D.put(1, i, result[i][1]);
			points3D.put(2, i, result[i][2]);
		}
		
		DebugUtilities.logMat("Resulting Points", points3D);
		
		Mat points = camPose.get2DPointFrom3D(points3D, inputMv);
		
		List<Point> points2D = new ArrayList<Point>();
		for(int i = 0; i < 4;i++) {
			points2D.add(new Point(points.get(0, i)[0]/points.get(2, i)[0], points.get(1, i)[0]/points.get(2, i)[0]));
			Log.d("HOGDETECT", "Pixel Pt.: "+points2D.get(points2D.size()-1).x+","+points2D.get(points2D.size()-1).y+")");
		}
		
		List<MatOfPoint> polyList = new ArrayList<MatOfPoint>();
		MatOfPoint pm = new MatOfPoint();
		pm.fromList(points2D);
		polyList.add(pm);
		
		Core.fillPoly(saveImg, polyList, new Scalar(255,0,0));
		Highgui.imwrite("/sdcard/arbg/resultWithBrick.png", saveImg);
		
		return mesh;
	}
	
	public LegoBrick[] getLegoBricks(CameraPoseTracker camPose) {
	    float[][] ocvContours = getOcvContour();
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "NEW CORNERS!!!");
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "--------------");
	    LegoBrick[] bricks;
	    if(ocvContours != null) {
	    	float[][][] cuboid = new float[ocvContours.length][8][3];
	    	float[][] corners3D;
		    float[][] corners3D1;
		    float[] point3D1;
		    float[] point3D;
	    	bricks = new LegoBrick[ocvContours.length];
	    	for (int k = 0; k < ocvContours.length; k++) {
		    	corners3D = new float[(ocvContours[k].length-1)/3][4];
		    	corners3D1 = new float[(ocvContours[k].length-1)/3][4];
		    	
		    	float minDistance = Float.POSITIVE_INFINITY;
		    	int minDistanceCorner=-1;
		    	for (int i = 0; i < (ocvContours[k].length-1)/3; i++) {
		    		point3D = camPose.get3DPointFrom2D(ocvContours[k][i*3+1], ocvContours[k][i*3+2],new CameraPoseTracker.ZMapper2D3D(0));
		    		point3D1 = camPose.get3DPointFrom2D(ocvContours[k][i*3+1], ocvContours[k][i*3+2],new CameraPoseTracker.ZMapper2D3D(1));
		    		if(point3D == null || point3D1 == null) break;
		    		corners3D[i][0] = point3D[0];
		    		corners3D[i][1] = point3D[1];
		    		corners3D[i][2] = point3D[2];
		    		corners3D[i][3] = 1;
		    		corners3D1[i][0] = point3D1[0];
		    		corners3D1[i][1] = point3D1[1];
		    		corners3D1[i][2] = point3D1[2];
		    		corners3D1[i][3] = 1;
		    		float[] cameraLocation = camPose.getCameraPosition();
		    		if(AppConfig.DEBUG_LOGGING) DebugUtilities.logGLMatrix("3D Corner ("+i+")", corners3D[i], 1, 4);
		    		if(AppConfig.DEBUG_LOGGING) DebugUtilities.logGLMatrix("3D1 Corner ("+i+")", corners3D1[i], 1, 4);
		    		if(cameraLocation != null) {
		    			float distance = MathUtilities.distance(corners3D[i][0], corners3D[i][1], cameraLocation[0], cameraLocation[1]);
		    			if(distance < minDistance) {
		    				minDistance = distance;
		    				minDistanceCorner = i;
		    			}
		    			if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Corner distance: "+distance);
		    		}
				}
		    	
		    	float minAngle = 360;
		    	float minDistanceAngle = 360;
		    	int[] cornersMinAngle = new int[3];
		    	for (int i = 0; i < corners3D.length; i++) {
		    		float angle = MathUtilities.angle(corners3D[i], corners3D[(i+1)%corners3D.length], corners3D[(i+2)%corners3D.length]);
		    		if(Math.abs(90-angle) < minDistanceAngle){
		    			minDistanceAngle = Math.abs(90-angle);
		    			minAngle = angle;
		    			cornersMinAngle = new int[]{i,(i+1)%corners3D.length,(i+2)%corners3D.length};
		    		}
		    		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "ANGLE Angle between corners ("+i+","+(i+1)%corners3D.length+","+(i+2)%corners3D.length+"): "+angle);
				}
		    	
		    	if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Minimum Distance to camera: "+minDistance);
		    	if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Minimum Angle: "+minAngle);
		    	if(minDistance != 0 && minAngle != 0) {
		    		float[][] corners;
		    		
		    		if(cornersMinAngle[0] == minDistanceCorner || cornersMinAngle[1] == minDistanceCorner || cornersMinAngle[2] == minDistanceCorner) {
		    			corners = corners3D;
		    		} else {
		    			corners = corners3D1;
		    		}
		    		
		    		if(corners.length < 3) continue;
		    		
		    		float[] vec1 = MathUtilities.vector(corners[cornersMinAngle[1]], corners[cornersMinAngle[0]]);
		    		float[] vec2 = MathUtilities.vector(corners[cornersMinAngle[1]], corners[cornersMinAngle[2]]);
		    		float[] vecZ = new float[]{0,0,1};
		    		
		    		float[] otherVec2a = MathUtilities.cross(vec1, vecZ);
		    		float[] otherVec2b = MathUtilities.cross(vecZ, vec1);
		    		float[] otherVec1a = MathUtilities.cross(vec2, vecZ);
		    		float[] otherVec1b = MathUtilities.cross(vecZ, vec2);
		    		float[] newVec1;
		    		float[] newVec2;
		    		if(MathUtilities.distance(otherVec2a[0], otherVec2a[1], vec2[0], vec2[1])
		    				< MathUtilities.distance(otherVec2b[0], otherVec2b[1], vec2[0], vec2[1])) {
		    			newVec2 = MathUtilities.mean(vec2, otherVec2a);
		    		} else newVec2 = MathUtilities.mean(vec2, otherVec2b);
		    		
		    		if(MathUtilities.distance(otherVec1a[0], otherVec1a[1], vec1[0], vec1[1])
		    				< MathUtilities.distance(otherVec1b[0], otherVec1b[1], vec1[0], vec1[1])) {
		    			newVec1 = MathUtilities.mean(vec1, otherVec1a);
		    		} else newVec1 = MathUtilities.mean(vec1, otherVec1b);
		    		
	    			newVec1 = MathUtilities.resize(newVec1, MathUtilities.norm(vec1));
	    			newVec2 = MathUtilities.resize(newVec2, MathUtilities.norm(vec2));
	    			
//	    			if(MathUtilities.norm(vec1) > MathUtilities.norm(vec2)) {
//	    				Log.d(TAG, "Original longest: "+MathUtilities.norm(vec1));
//		    			newVec1 = MathUtilities.resize(newVec1, 3.2f);
//		    			newVec2 = MathUtilities.resize(newVec2, 1.6f);
//		    		} else {
//		    			Log.d(TAG, "Original longest: "+MathUtilities.norm(vec2));
//		    			newVec1 = MathUtilities.resize(newVec1, 1.6f);
//		    			newVec2 = MathUtilities.resize(newVec2, 3.2f);
//		    		}
	    			
		    		cuboid[k][0] = MathUtilities.vectorToPoint(newVec1, corners[cornersMinAngle[1]]);
		    		cuboid[k][1] = corners[cornersMinAngle[1]];
		    		cuboid[k][2] = MathUtilities.vectorToPoint(newVec2, corners[cornersMinAngle[1]]);
		    		cuboid[k][3] = MathUtilities.vectorToPoint(newVec2, cuboid[k][0]);
		    		
//		    		float[][] tmp = new float[4][];
//		    		int nextIdx;
//		    		int prevIdx;
//		    		float[] vec;
//		    		for (int i = 0; i < 4; i++) {
//		    			nextIdx = (i+1)%4;
//		    			prevIdx = ((((i-1) % 4) + 4) % 4);
//		    			vec = MathUtilities.vector(cuboid[k][i],cuboid[k][i%2==0 ? prevIdx : nextIdx]);
//		    			vec = MathUtilities.multiply(vec, -1);
//		    			vec = MathUtilities.resize(vec, WorldConfig.BRICK_PERIMETER);
//		    			tmp[i] = MathUtilities.vectorToPoint(vec, cuboid[k][i]);
//					}
//		    		
//		    		for (int i = 0; i < 4; i++) {
//		    			cuboid[k][i] = tmp[i];
//		    		}
//		    		
//		    		for (int i = 0; i < 4; i++) {
//		    			nextIdx = (i+1)%4;
//		    			prevIdx = ((((i-1) % 4) + 4) % 4);
//		    			vec = MathUtilities.vector(cuboid[k][i],cuboid[k][i%2==0 ? nextIdx : prevIdx]);
//		    			vec = MathUtilities.multiply(vec, -1);
//		    			vec = MathUtilities.resize(vec, WorldConfig.BRICK_PERIMETER);
//		    			tmp[i] = MathUtilities.vectorToPoint(vec, cuboid[k][i]);
//		    			
//					}
//		    		
//		    		for (int i = 0; i < 4; i++) {
//		    			cuboid[k][i] = tmp[i];
//		    		}
		    		
		    		
		    		cuboid[k][4] = new float[]{cuboid[k][0][0],cuboid[k][0][1],1-cuboid[k][0][2]};
		    		cuboid[k][5] = new float[]{cuboid[k][1][0],cuboid[k][1][1],1-cuboid[k][1][2]};
		    		cuboid[k][6] = new float[]{cuboid[k][2][0],cuboid[k][2][1],1-cuboid[k][2][2]};
		    		cuboid[k][7] = new float[]{cuboid[k][3][0],cuboid[k][3][1],1-cuboid[k][3][2]};
		    		
		    		if(AppConfig.DEBUG_LOGGING) {
			    		for (int i = 0; i < cuboid[k].length; i++) {
							Log.d(TAG, "RESULTING CUBOID Point "+i+": ("+cuboid[k][i][0]+","+cuboid[k][i][1]+","+cuboid[k][i][2]+")");
						}
		    		}
		    		bricks[k] = new LegoBrick(cuboid[k],Color.ColorName.values()[(int) ocvContours[k][0]], camPose.getOrientationDeg());
		    	} else return new LegoBrick[0];
	    	}
	    	return bricks;
	    } else return new LegoBrick[0];
	}
	
	public void frameTick() {
		synchronized (lock) {
			synchronized (lockExtern) {
//				contour.copyTo(contourExtern);
				threshold.copyTo(thresholdExtern);
			}
		}
	}
	
	public Mat getThreshold() {
		Mat result = new Mat();
		synchronized (lockExtern) {
			thresholdExtern.copyTo(result);
		}
		return result;
	}
	
	private void setContour(Mat contour) {
		synchronized (lock) {
			contour.copyTo(this.contour);
		}
	}
	
	private void setThreshold(Mat threshold) {
		synchronized (lock) {
			threshold.copyTo(this.threshold);
		}
	}
	
	private class FindLegoBrick extends AsyncTask<Mat, Void, Void > {
		private FrameTrackingCallback trackingCallback;
		private long start;
		private Mat threshold;
		
		public FindLegoBrick() {
			synchronized (lock) {
				threshold = new Mat(LegoBrickTracker.this.threshold.size(), LegoBrickTracker.this.threshold.type());
			}
		}
		
		@Override
		protected Void doInBackground(Mat... params) {
			long start = System.nanoTime();
			findLegoBrick2( 
					params[0].getNativeObjAddr(),
					threshold.getNativeObjAddr()
					);
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "LegoBrick threshold found in "+(System.nanoTime()-start)/1000000L+"ms");
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			setThreshold(threshold);
			this.trackingCallback.trackingDone(LegoBrickTracker.class);
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "LegoBrick found in "+(System.nanoTime()-start)/1000000L+"ms");
		}
		
		private void setupFrameTrackingCallback(FrameTrackingCallback trackingCallback) {
			this.trackingCallback = trackingCallback;
		}
	}
	
	private native void findLegoBrick(long yuvFrameImage, long contour);
	private native void findLegoBrick2(long yuvFrameImage, long contour);
	private native void findLegoBrick3(long bgrPointer, long resultMatPtr);
//	private native void findLegoBrick3(long camFrameImage, String renderImgsPath, long brickPosPtr);
	private native void generateHOGDescriptors(String renderImgsPath);
	private native void findLegoBrickLines(long bgrPointer, float upAngle, long resultMatPtr, long origContMatPtr);
	private native float[] checkOverlap(long inputPoints, int idx);
	private native void getConvexHull(long bricksPointer, long convexThreshPtr, long currFrameThreshPtr);
}
