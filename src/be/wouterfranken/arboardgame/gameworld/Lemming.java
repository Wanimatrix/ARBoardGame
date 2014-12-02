package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.rendering.meshes.CubeMesh;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject;
import be.wouterfranken.arboardgame.rendering.meshes.RenderOptions;
import be.wouterfranken.arboardgame.utilities.Color;
import be.wouterfranken.arboardgame.utilities.MathUtilities;

public class Lemming {
	private static final String TAG = Lemming.class.getSimpleName();
	
	private MeshObject mesh;
	private List<WorldCoordinate> path;
	
	private float locationX;
	private float locationY;
	private float size;
	private float height;
	private float speed; // cm/s
	
	public Lemming(float size, float height, float locationX, float locationY, float speed, List<WorldCoordinate> path, Color color) {
		mesh = new CubeMesh(size, locationX, locationY, height, new RenderOptions(true, color));
		this.locationX = locationX;
		this.locationY = locationY;
		this.size = size;
		this.height = height;
		this.speed = speed;
		this.path = path;
	}
	
	public float getLocationX() {
		return locationX;
	}
	
	public float getLocationY() {
		return locationY;
	}
	
	public MeshObject getMesh() {
		return mesh;
	}
	
	public void updateLocation(WorldCoordinate end, World w) {
		float fps = AppConfig.FPS_RANGE[1]/1000.0f; // max frame/sec
		float todoDistance = speed/fps;
		
		float newLocationX = locationX;
		float newLocationY = locationY;
		float distance;
		
		WorldCoordinate to = path.get(1);
		// When we will pass a node, the path will be updated first.
		if(MathUtilities.distance(newLocationX,newLocationY,to.x,to.y) < todoDistance) {
			if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Finding path from "+path.get(0).toString()+", to "+end.toString());
			List<WorldCoordinate> tmpPath = Pathfinder.findPath(to, end, w);
			if(tmpPath != null) {
				List<WorldCoordinate> newPath = new ArrayList<WorldCoordinate>(tmpPath.size()+1);
				newPath.add(0, path.get(0));
				newPath.addAll(tmpPath);
				path = newPath;
			}
		}
		
		if(AppConfig.DEBUG_LOGGING){
			Log.d(TAG, "THE Lemming's PATH: ");
			for (int i = 0; i < path.size(); i++) {
				Log.d(TAG, "PATH NODE: "+path.get(i).toString());
			}
		}
		
		if(path.get(1).equals(path.get(0))) {
			throw new IllegalStateException("Path is going from/to the same node!");
		}
		
		while(todoDistance > 0) {
			if(path.size() == 1) {
				newLocationX = path.get(0).x;
				newLocationY = path.get(0).y;
				break;
			}
			to = path.get(1);
			
			float distToEnd = MathUtilities.distance(newLocationX,newLocationY,to.x,to.y);
			if(distToEnd <= todoDistance || Math.abs(distToEnd-todoDistance) < 0.0001f) {
				distance = distToEnd;
				todoDistance -= distToEnd;
				path.remove(0);
			} else {
				distance = todoDistance;
				todoDistance = 0;
			}
			float slope = (to.y-newLocationY)/(to.x-newLocationX);
			if(Float.isInfinite(slope)) {
				newLocationY += Math.signum(to.y-newLocationY)*distance;
			} else if(Math.abs(slope) == 0){
				newLocationX += Math.signum(to.x-newLocationX)*distance;
			} 
			else {
				throw new IllegalStateException("A Lemming cannot go diagonal! Slope was "+slope);
			}
		}
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Lemming location updated: from ("+locationX+","+locationY+"), to ("+newLocationX+","+newLocationY+")");
		locationX = newLocationX;
		locationY = newLocationY;
		mesh = new CubeMesh(size, locationX, locationY, height, mesh.getRenderOptions());
	}
}
