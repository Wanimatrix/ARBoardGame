package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.util.ArrayMap;
import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.rendering.meshes.CuboidMesh;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject;
import be.wouterfranken.arboardgame.rendering.meshes.RenderOptions;
import be.wouterfranken.arboardgame.utilities.Color;
import be.wouterfranken.arboardgame.utilities.MathUtilities;

public class LegoBrick {
	
	private static final String TAG = LegoBrick.class.getSimpleName();
	
	private float[][] corners;
	private float[] xBounds;
	private float[] yBounds;
	private float[] size;
	private Color.ColorName color;
	private long mergeCount;
	private int noMergeCount;
	private boolean active;
	private List<WorldCoordinate> coord = new ArrayList<WorldCoordinate>();
	
	private float[] overlap;
	
	public LegoBrick(float[][] corners, Color.ColorName color, float[] overlap) {
		this.corners = corners;
		this.color = color;
		this.overlap = overlap;
		
		mergeCount = 1;
		noMergeCount = 0;
		
		float minX = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < corners.length; i++) {
			if(corners[i][0] > maxX) maxX = corners[i][0];
			if(corners[i][0] < minX) minX = corners[i][0];
			if(corners[i][1] > maxY) maxY = corners[i][1];
			if(corners[i][1] < minY) minY = corners[i][1];
		}
		
		xBounds = new float[]{minX,maxX};
		yBounds = new float[]{minY,maxY};
		
		float norm0 = MathUtilities.norm(MathUtilities.vector(corners[0], corners[3]));
		float norm1 = MathUtilities.norm(MathUtilities.vector(corners[0], corners[1]));
		
		norm0 = (float) (Math.round(norm0/0.8f)*0.8);
		norm1 = (float) (Math.round(norm1/0.8f)*0.8);
		
		size = norm0 > norm1 ? new float[]{norm0,norm1} : new float[]{norm1,norm0};
		setSize(size);
	}
	
	private Map<Integer, Integer> isCloseTo(LegoBrick other) {
		Map<Integer, Integer> cornerMap = new ArrayMap<Integer, Integer>();
		for (int i = 0; i < corners.length; i++) {
			for (int j = 0; j < other.getCuboid().length; j++) {
				if(cornerMap.containsValue(j)) continue;
				if(Math.abs(corners[i][0]-other.getCuboid()[j][0]) <= AppConfig.LEGO_CORNERS_CLOSENESS_BOUND
						&& Math.abs(corners[i][1]-other.getCuboid()[j][1]) <= AppConfig.LEGO_CORNERS_CLOSENESS_BOUND
						&& Math.abs(corners[i][2]-other.getCuboid()[j][2]) <= 0.0001f) {
					cornerMap.put(i, j);
					break;
				}
			}
			if(cornerMap.size() != i+1) return null;
		}
		return cornerMap;
	}
	
	public int mergeCheck(LegoBrick[] others) {
		for (int o = 0; o < others.length; o++) {
			LegoBrick other = others[o];
			if(other == null) continue;
			Map<Integer,Integer> cornerMap = isCloseTo(other);
			if(cornerMap == null) continue;
			
			float[][] otherCorners = other.getCuboid();
			
			for (int i = 0; i < corners.length; i++) {
				corners[i][0] = (corners[i][0]*mergeCount + otherCorners[cornerMap.get(i)][0])/(mergeCount+1);
				corners[i][1] = (corners[i][1]*mergeCount + otherCorners[cornerMap.get(i)][1])/(mergeCount+1);
				corners[i][2] = (corners[i][2]*mergeCount + otherCorners[cornerMap.get(i)][2])/(mergeCount+1);
			}
			
//			voteForSize(MathUtilities.norm(MathUtilities.vector(corners[0], corners[3])),
//					MathUtilities.norm(MathUtilities.vector(corners[0], corners[1])));
			
			noMergeCount = 0;
			mergeCount++;
			return o;	
		}
		noMergeCount++;
		return -1;
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
		return new CuboidMesh(corners, new RenderOptions(ro.useMVP, Color.COLOR_MAP.get(color), ro.lightPosition, ro.fragmentShader, ro.vertexShader));
	}
	
	public float[][] getCuboid() {
		return corners;
	}
	
	public float[] getXBounds() {
		return xBounds;
	}
	
	public float[] getYBounds() {
		return yBounds;
	}
	
	public Color.ColorName getColor() {
		return color;
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

	public float[] getOverlap() {
		return overlap;
	}
}
