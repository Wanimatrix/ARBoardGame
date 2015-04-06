package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import android.util.ArrayMap;
import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.rendering.meshes.CuboidMesh;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject;
import be.wouterfranken.arboardgame.rendering.meshes.RenderOptions;
import be.wouterfranken.arboardgame.rendering.tracking.BrickTrackerConfig;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPoseTracker;
import be.wouterfranken.arboardgame.utilities.Color;
import be.wouterfranken.arboardgame.utilities.MathUtilities;

public class LegoBrick {
	
	private static final String TAG = LegoBrick.class.getSimpleName();
	
//	private float[][] corners3D;
	private float[] centerPoint;
	private float[][] halfSideVectors;
	private float[] xBounds;
	private float[] yBounds;
	private float[] size;
	private List<Float> orientations;
	private Color color;
	private long mergeCount;
	private int noMergeCount;
	private long votes;
	private long removalVotes;
	private long visibleFrames;
	private boolean active;
	private List<WorldCoordinate> coord = new ArrayList<WorldCoordinate>();
	
	public LegoBrick(float[][] cuboid, Color.ColorName color, float orientation) {
		
	}
	
	public LegoBrick(float[] centerPoint, float[][] halfSideVectors, float orientation) {
		this.centerPoint = centerPoint;
		this.halfSideVectors = halfSideVectors;
		
		calculateCuboid();
		
		this.color = new Color(1,0,0,1);
		this.orientations = new ArrayList<Float>();
		this.orientations.add(orientation);
		Log.d(TAG, "Orientations amount: "+orientations.size());
		
		mergeCount = 1;
		noMergeCount = 0;
		votes = 1;
		
		float minX = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				for (int k = 0; k < 2; k++) {
					float a = (float) Math.pow(-1,i);
					float b = (float) Math.pow(-1,j);
					float c = (float) Math.pow(-1,k);
					float[] cornerPt = MathUtilities.vectorsToPoint(centerPoint, 
							MathUtilities.multiply(halfSideVectors[0],a),
							MathUtilities.multiply(halfSideVectors[1],b),
							MathUtilities.multiply(halfSideVectors[2],c));
					
					if(cornerPt[0] > maxX) maxX = cornerPt[0];
					if(cornerPt[0] < minX) minX = cornerPt[0];
					if(cornerPt[1] > maxY) maxY = cornerPt[1];
					if(cornerPt[1] < minY) minY = cornerPt[1];
				}
			}
		}
		
		xBounds = new float[]{minX,maxX};
		yBounds = new float[]{minY,maxY};
		
		float norm0 = MathUtilities.norm(MathUtilities.multiply(halfSideVectors[0], 2));
		float norm1 = MathUtilities.norm(MathUtilities.multiply(halfSideVectors[1], 2));
		
		norm0 = (float) (Math.round(norm0/0.8f)*0.8);
		norm1 = (float) (Math.round(norm1/0.8f)*0.8);
		
		size = norm0 > norm1 ? new float[]{norm0,norm1} : new float[]{norm1,norm0};
		setSize(size);
	}
	
//	private Map<Integer, Integer> isCloseTo(LegoBrick other) {
//		Map<Integer, Integer> cornerMap = new ArrayMap<Integer, Integer>();
//		for (int i = 0; i < corners3D.length; i++) {
//			for (int j = 0; j < other.getCuboid().length; j++) {
//				if(cornerMap.containsValue(j)) continue;
//				if(Math.abs(corners3D[i][0]-other.getCuboid()[j][0]) <= AppConfig.LEGO_CORNERS_CLOSENESS_BOUND
//						&& Math.abs(corners3D[i][1]-other.getCuboid()[j][1]) <= AppConfig.LEGO_CORNERS_CLOSENESS_BOUND
//						&& Math.abs(corners3D[i][2]-other.getCuboid()[j][2]) <= 0.0001f) {
//					cornerMap.put(i, j);
//					break;
//				}
//			}
//			if(cornerMap.size() != i+1) return null;
//		}
//		return cornerMap;
//	}
	
	private Map<Integer, Integer> isCloseTo(LegoBrick other) {
		Map<Integer, Integer> poseMap = new ArrayMap<Integer, Integer>();
		poseMap.put(0, 0);
		
		if(MathUtilities.norm(MathUtilities.vector(centerPoint, other.centerPoint)) < AppConfig.LEGO_CORNERS_CLOSENESS_BOUND) {
			float angleDeg1t = (float) (Math.acos((float)halfSideVectors[1][0])*(180.0f/Math.PI)); 
			angleDeg1t = (halfSideVectors[1][1] > 0) ? (angleDeg1t*-1) : angleDeg1t;
			angleDeg1t = (angleDeg1t < 0) ? angleDeg1t+180 : angleDeg1t;
			float angleDeg1o = (float) (Math.acos((float)other.halfSideVectors[1][0])*(180.0f/Math.PI)); 
			angleDeg1o = (other.halfSideVectors[1][1] > 0) ? (angleDeg1o*-1) : angleDeg1o;
			angleDeg1o = (angleDeg1o < 0) ? angleDeg1o+180 : angleDeg1o;
			float angleDeg2o = (float) (Math.acos((float)other.halfSideVectors[2][0])*(180.0f/Math.PI));
			angleDeg2o = (other.halfSideVectors[2][1] > 0) ? (angleDeg2o*-1) : angleDeg2o;
			angleDeg2o = (angleDeg2o < 0) ? angleDeg2o+180 : angleDeg2o;
			
			if(angleDeg1o > angleDeg1t-25 && angleDeg1o < angleDeg1t+25) {
				poseMap.put(1, 1);
				poseMap.put(2, 2);
			} else if(angleDeg2o > angleDeg1t-25 && angleDeg2o < angleDeg1t+25) {
				poseMap.put(1, 2);
				poseMap.put(2, 1);
			} else {
				Log.d("MERGEACCEPT", "Not merged, reason angles: "+(angleDeg1t+25)+" > "+angleDeg1o+" AND "+angleDeg2o+" > "+(angleDeg1t-25));
				
				
//				Core.line(closeTest, new Point(50, 50), new Point(50+halfSideVectors[1][0]*100,50+halfSideVectors[1][1]*100), new Scalar(255,0,0));
//				Core.putText(closeTest, angleDeg1t+"d", new Point(50+halfSideVectors[1][0]*8,50+halfSideVectors[1][1]*8), Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(255,0,0));
//				Core.line(closeTest, new Point(50, 50), new Point(50+halfSideVectors[2][0]*100,50+halfSideVectors[2][1]*100), new Scalar(255,0,0));
//				Core.line(closeTest, new Point(50, 50), new Point(50+other.halfSideVectors[1][0]*100,50+other.halfSideVectors[1][1]*100), new Scalar(0,0,255));
//				Core.putText(closeTest, angleDeg1o+"d", new Point(50+other.halfSideVectors[1][0]*8,50+other.halfSideVectors[1][1]*8), 
//						Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(0,0,255));
//				Core.line(closeTest, new Point(50, 50), new Point(50+other.halfSideVectors[2][0]*100,50+other.halfSideVectors[2][1]*100), new Scalar(0,0,255));
//				Core.putText(closeTest, angleDeg2o+"d", new Point(50+other.halfSideVectors[2][0]*8,50+other.halfSideVectors[2][1]*8), 
//						Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(0,0,255));
//				
//				Highgui.imwrite("/sdcard/arbg/closeTest.png", closeTest);
				return null;
			}
		} else {
			Log.d("MERGEACCEPT", "Not merged, reason centerPt distance: "+MathUtilities.norm(MathUtilities.vector(centerPoint, other.centerPoint))+" > 0.8");
			return null;
		}
		Log.d("MERGEACCEPT", "Merge accepted");
		
		return poseMap;
	}
	
	public LegoBrickContainer[] mergeCheck(LegoBrickContainer[] others) {
		List<LegoBrickContainer> result = new ArrayList<LegoBrickContainer>(Arrays.asList(others));
		Iterator<LegoBrickContainer> it = result.iterator();
		while(it.hasNext()) {
			LegoBrickContainer lc = it.next();
			List<Integer> mergeIdxes = this.mergeCheckAll(lc.toArray(new LegoBrick[lc.size()]));
			int i = 0;
			Log.d("INDEX_TEST", "New indexes");
			for (Integer idx : mergeIdxes) {
				Log.d("INDEX_TEST", "Remove index: "+idx);
				lc.remove(idx-i++);
				if(lc.size() == 0) it.remove();
			}
		}
		
		return result.toArray(new LegoBrickContainer[result.size()]);
	}
	
	public List<Integer> mergeCheckAll(LegoBrick[] others) {
		List<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < others.length; ) {
			int mergeRes = mergeCheck(i, others);
			if(mergeRes == -1) return result;
			else  {
				result.add(mergeRes);
				if(mergeRes == others.length-1)
					return result;
				else
					i = mergeRes+1;
			}
		}
		return result;
	}
	
	public int mergeCheck(LegoBrick[] others) {
		return mergeCheck(0, others);
	}
	
	public int mergeCheck(int startIdx, LegoBrick[] others) {
		for (int o = startIdx; o < others.length; o++) {
			LegoBrick other = others[o];
//			if(other == null) continue;
			Map<Integer,Integer> poseMap = isCloseTo(other);
			if(poseMap == null) continue;
			
			centerPoint[0] = (centerPoint[0]*mergeCount + other.centerPoint[0])/(mergeCount+1);
			centerPoint[1] = (centerPoint[1]*mergeCount + other.centerPoint[1])/(mergeCount+1);
			centerPoint[2] = (centerPoint[2]*mergeCount + other.centerPoint[2])/(mergeCount+1);
			
			Log.d(TAG, "OLD Half-side vector sizes: "+MathUtilities.norm(halfSideVectors[0])+","+MathUtilities.norm(halfSideVectors[1])+","+MathUtilities.norm(halfSideVectors[2]));
			
			for (int i = 0; i < halfSideVectors.length-1; i++) {
				float norm = MathUtilities.norm(halfSideVectors[i]);
				halfSideVectors[i][0] = (halfSideVectors[i][0]*mergeCount + other.halfSideVectors[poseMap.get(i)][0])/(mergeCount+1);
				halfSideVectors[i][1] = (halfSideVectors[i][1]*mergeCount + other.halfSideVectors[poseMap.get(i)][1])/(mergeCount+1);
				halfSideVectors[i][2] = (halfSideVectors[i][2]*mergeCount + other.halfSideVectors[poseMap.get(i)][2])/(mergeCount+1);
				
				// Make sure the size of the vector does not change.
				halfSideVectors[i] = MathUtilities.resize(halfSideVectors[i], norm);
			}
			
			// Make sure the third vector is perpendicular to the other two
			float norm = MathUtilities.norm(halfSideVectors[1]);
			float[] tmpSideVec = MathUtilities.cross(halfSideVectors[0], halfSideVectors[1]);
			if(Math.signum(tmpSideVec[0]) != Math.signum(halfSideVectors[2][0]) || Math.signum(tmpSideVec[1]) != Math.signum(halfSideVectors[2][1]))
				halfSideVectors[2] = MathUtilities.multiply(tmpSideVec, -1);
			else
				halfSideVectors[2] = tmpSideVec;
			halfSideVectors[2][2] = 0;
			halfSideVectors[2] = MathUtilities.resize(halfSideVectors[2], norm);
			
			
			Log.d(TAG, "NEW Half-side vector sizes: "+MathUtilities.norm(halfSideVectors[0])+","+MathUtilities.norm(halfSideVectors[1])+","+MathUtilities.norm(halfSideVectors[2]));
			
//			voteForSize(MathUtilities.norm(MathUtilities.vector(corners[0], corners[3])),
//					MathUtilities.norm(MathUtilities.vector(corners[0], corners[1])));
			
//			mergeOrientations(other.getOrientations());
			
			calculateCuboid();
			
			noMergeCount = 0;
			mergeCount++;
			
			updateColor();
			
			Log.d(TAG, "COLOR: "+this.color.r+", "+this.color.g+", "+this.color.b+", "+this.color.a+"; MergeCount: "+mergeCount);
			
			
			return o;	
		}
		noMergeCount++;
		return -1;
	}
	
//	public int checkBrickOverlap(LegoBrick[] others) {
//		for (int o = 0; o < others.length; o++) {
//			LegoBrick other = others[o];
//			if(other == null) continue;
//			Map<Integer,Integer> cornerMap = isCloseTo(other);
//			if(cornerMap == null) continue;
//			
//			float[][] otherCorners = other.getCuboid();
//			
//			for (int i = 0; i < corners3D.length; i++) {
//				corners3D[i][0] = (corners3D[i][0]*mergeCount + otherCorners[cornerMap.get(i)][0])/(mergeCount+1);
//				corners3D[i][1] = (corners3D[i][1]*mergeCount + otherCorners[cornerMap.get(i)][1])/(mergeCount+1);
//				corners3D[i][2] = (corners3D[i][2]*mergeCount + otherCorners[cornerMap.get(i)][2])/(mergeCount+1);
//			}
//			
////			voteForSize(MathUtilities.norm(MathUtilities.vector(corners[0], corners[3])),
////					MathUtilities.norm(MathUtilities.vector(corners[0], corners[1])));
//			
//			noMergeCount = 0;
//			mergeCount++;
//			return o;	
//		}
//		noMergeCount++;
//		return -1;
//	}
	
	public Point[] get2DPoints(Mat modelView) {
		long start = System.nanoTime();
		
		float[][] corners3D = getCuboid();
		
		Mat coord3D = Mat.ones(4, 8, CvType.CV_32FC1);
		for (int i = 0; i < 8; i++) {
			coord3D.put(0, i, corners3D[i][0]);
			coord3D.put(1, i, corners3D[i][1]);
			coord3D.put(2, i, corners3D[i][2]);
		}
		Mat coord2D = CameraPoseTracker.get2DPointFrom3D(coord3D, modelView);
		Point[] corners2D = new Point[8];
		for (int i = 0; i < corners2D.length; i++) {
			corners2D[i] = new Point(coord2D.get(0,i)[0]/coord2D.get(2,i)[0],coord2D.get(1,i)[0]/coord2D.get(2,i)[0]);
		}
		Log.d(TAG, "Get2DPoints time: "+(System.nanoTime() - start)/1000000.0+"ms");
		
		return corners2D;
	}
	
	public boolean isVisible(Mat modelView) {
		Point[] points2D = get2DPoints(modelView);
		
		for (Point point : points2D) {
			if((point.x < 0 || point.x > AppConfig.PREVIEW_RESOLUTION[0])
					&& (point.y < 0 || point.y > AppConfig.PREVIEW_RESOLUTION[1]))
				return false;
		}
		return true;
	}
	
	public boolean readyToBeActive() {
		return !active && mergeCount >= AppConfig.REQUIRED_LEGO_MERGES;
	}
	
	public boolean readyToBeInactive() {
		return active && noMergeCount > AppConfig.MAX_LEGO_NO_MERGES;
	}
	
	public boolean readyToBeRemoved() {
		return noMergeCount > AppConfig.MAX_LEGO_NO_MERGES_BEFORE_REMOVAL;
	}
	
	public void deactivate() {
		setActive(false);
		mergeCount = 1;
	}
	
	public boolean isActive(){
		return active;
	}
	
	private void voteForSize(float norm0, float norm1) {
		float[] newSize = new float[2];
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Incoming vote for bricksize (without perimeter): "+"("+norm0+","+norm1+")");
		
		newSize[0] = (float) (Math.round(norm0/0.8f)*0.8);
		newSize[1] = (float) (Math.round(norm1/0.8f)*0.8);
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Vote was for bricksize (without perimeter): "+"("+newSize[0]+","+newSize[1]+")");
		
		if(newSize[0] > newSize[1]) {
			newSize[0] = (mergeCount*size[0]+newSize[0])/(mergeCount+1);
			newSize[1] = (mergeCount*size[1]+newSize[1])/(mergeCount+1);
		} else {
			float tmp = newSize[0];
			newSize[0] = (mergeCount*size[0]+newSize[1])/(mergeCount+1);
			newSize[1] = (mergeCount*size[1]+tmp)/(mergeCount+1);
		}
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "New bricksize (without perimeter): "+"("+newSize[0]+","+newSize[1]+")");
		
		setSize(newSize);
	}
	
	private void setSize(float[] newSize) {
//		float[][] cuboid = corners;
//		
//		float[][] tmp = new float[4][];
//		int nextIdx;
//		int prevIdx;
//		float[] vec;
//		float norm, otherNorm;
//		for (int i = 0; i < 4; i++) {
//			nextIdx = (i+1)%4;
//			prevIdx = ((((i-1) % 4) + 4) % 4);
//			vec = MathUtilities.vector(cuboid[i],cuboid[i%2==0 ? prevIdx : nextIdx]);
//			norm = MathUtilities.norm(vec);
//			otherNorm = MathUtilities.norm(MathUtilities.vector(cuboid[i],cuboid[i%2!=0 ? prevIdx : nextIdx]));
//			vec = MathUtilities.multiply(vec, -1);
//			vec = MathUtilities.resize(vec, (((norm > otherNorm ? newSize[0] : newSize[1])+WorldConfig.BRICK_PERIMETER*2)-norm)/2.0f);
//			tmp[i] = MathUtilities.vectorToPoint(vec, cuboid[i]);
//		}
//		
//		for (int i = 0; i < 4; i++) {
//			cuboid[i] = tmp[i];
//		}
//		
//		for (int i = 0; i < 4; i++) {
//			nextIdx = (i+1)%4;
//			prevIdx = ((((i-1) % 4) + 4) % 4);
//			vec = MathUtilities.vector(cuboid[i],cuboid[i%2==0 ? nextIdx : prevIdx]);
//			norm = MathUtilities.norm(vec);
//			otherNorm = MathUtilities.norm(MathUtilities.vector(cuboid[i],cuboid[i%2!=0 ? nextIdx : prevIdx]));
//			vec = MathUtilities.multiply(vec, -1);
//			vec = MathUtilities.resize(vec, (((norm > otherNorm ? newSize[0] : newSize[1])+WorldConfig.BRICK_PERIMETER*2)-norm)/2.0f);
//			tmp[i] = MathUtilities.vectorToPoint(vec, cuboid[i]);
//			
//		}
//		
//		for (int i = 0; i < 4; i++) {
//			cuboid[i] = tmp[i];
//		}
		
//		cuboid[4] = new float[]{cuboid[0][0],cuboid[0][1],1-cuboid[0][2]};
//		cuboid[5] = new float[]{cuboid[1][0],cuboid[1][1],1-cuboid[1][2]};
//		cuboid[6] = new float[]{cuboid[2][0],cuboid[2][1],1-cuboid[2][2]};
//		cuboid[7] = new float[]{cuboid[3][0],cuboid[3][1],1-cuboid[3][2]};
	}
	
	public MeshObject getMesh(RenderOptions ro) {
		return new CuboidMesh(getCuboid(), new RenderOptions(ro.useMVP, color, ro.lightPosition, ro.fragmentShader, ro.vertexShader));
	}
	
	float[][] cuboid;
	
	public void calculateCuboid() {
		float[][] cuboid = new float[8][];
		Log.d(TAG, "Getting cuboid!");
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				for (int k = 0; k < 2; k++) {
					float a = (float) Math.pow(-1,i);
					float b = (float) Math.pow(-1,j);
					float c = (float) Math.pow(-1,j == 0 ? k : 1-k);
					Log.d(TAG, "Getting cuboid: "+a+","+b+","+c);
					float[] cornerPt = MathUtilities.vectorsToPoint(centerPoint, 
							MathUtilities.multiply(halfSideVectors[0],a),
							MathUtilities.multiply(halfSideVectors[1],b),
							MathUtilities.multiply(halfSideVectors[2],c));
					
					cuboid[i*4+j*2+k] = cornerPt;
				}
			}
		}
		this.cuboid = cuboid;
	}
	
	public float[][] getCuboid() {
		return cuboid;
	}
	
	public float[] getXBounds() {
		return xBounds;
	}
	
	public float[] getYBounds() {
		return yBounds;
	}
	
	public Color getColor() {
		return color;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	public void addCoordinate(WorldCoordinate node) {
		coord.add(node);
	}
	
	public WorldCoordinate removeCoordinate() {
		if(coord.size() == 0) return null;
		return coord.remove(0);
	}
	
	public void setActive(boolean active) {
		this.active = active;
	}

	public float[] getOverlap(Point[] points2D) {
		return checkCurrentOverlap(new MatOfPoint(points2D).getNativeObjAddr());
	}
	
	public float[] getOverlap(Mat modelView) {
		return checkCurrentOverlap(new MatOfPoint(get2DPoints(modelView)).getNativeObjAddr());
//		return overlap;
	}
	
	private void vote(int amount) {
		this.votes += amount;
	}
	
	public void voteUp(int amount) {
		vote(amount);
	}
	
	public void punish(int amount) {
		vote(-amount);
	}
	
	public long getVotes() {
		return votes;
	}
	
	public void voteRemoval() {
		this.removalVotes++;
	}
	
	public void resetRemoval() {
		this.removalVotes = 0;
	}
	
	public long getRemovalVotes() {
		return removalVotes;
	}
	
	public void addVisibleFrame() {
		visibleFrames++;
	}
	
	public long getVisibleFrames() {
		return visibleFrames;
	}
	
	public long getMergeCount() {
		return mergeCount;
	}
	
	public void mergeOrientations(List<Float> orientations) {
		for (Float o1 : this.orientations) {
			Iterator<Float> orientationsIt = orientations.iterator();
			while(orientationsIt.hasNext()) {
				Float o2 = orientationsIt.next();
				
				if((int)(o1/10) == (int)(o2/10))
					orientationsIt.remove();
			}
		}
		
		this.orientations.addAll(orientations);
		
		updateColor();
	}
	
	public List<Float> getOrientations() {
		return orientations;
	}
	
	public float[] getCenterPoint() {
		return centerPoint;
	}
	
	public float[][] getHalfSideVectors() {
		return halfSideVectors;
	}
	
	public float getPercentCompleted() {
		float max = BrickTrackerConfig.NECESS_MERGE_COUNTS + BrickTrackerConfig.NECESS_ORIENTATIONS - 2;
		float mergeCompleted = Math.min((mergeCount - 1),(BrickTrackerConfig.NECESS_MERGE_COUNTS - 1));
		float orientCompleted = Math.min((orientations.size() - 1),(BrickTrackerConfig.NECESS_ORIENTATIONS - 1));
		Log.d(TAG, "BRICKCOMPLETED merges: "+mergeCompleted+", orientations: "+orientCompleted);
		return (mergeCompleted+orientCompleted)/max;
	}
	
	public void updateColor() {
		float completed = getPercentCompleted();
		if(completed == 1)
			this.color = new Color(1,1,0, 1);
		else 
			this.color = new Color(1-completed, completed,0, 1);
	}
	
	private native float[] checkCurrentOverlap(long inputPoints);
}
