package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.List;

import be.wouterfranken.arboardgame.rendering.meshes.CuboidMesh;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject;
import be.wouterfranken.arboardgame.rendering.meshes.RenderOptions;

public class LegoBrick {
	
	private final float[][] corners;
	private final float[] xBounds;
	private final float[] yBounds;
	private List<WorldCoordinate> coord = new ArrayList<WorldCoordinate>();
	
	public LegoBrick(float[][] corners) {
		this.corners = corners;
		
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
	}
	
	public MeshObject getMesh(RenderOptions ro) {
		return new CuboidMesh(corners, ro);
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
	
	public void addCoordinate(WorldCoordinate node) {
		coord.add(node);
	}
	
	public WorldCoordinate removeCoordinate() {
		if(coord.size() == 0) return null;
		return coord.remove(0);
	}
}
