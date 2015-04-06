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
	public static LemmingPath path;
	public LemmingPath myPath;
//	private static Pathfinder pathGenerator = null;
	
	private float locationX;
	private float locationY;
	private float size;
	private float height;
	private float speed; // cm/s
	
	public Lemming(float size, float height, float locationX, float locationY, float speed, Color color) {
		mesh = new CubeMesh(size, locationX, locationY, height, new RenderOptions(true, color, true));
		this.locationX = locationX;
		this.locationY = locationY;
		this.size = size;
		this.height = height;
		this.speed = speed;
		this.myPath = new LemmingPath();
		this.myPath.addAll(Lemming.path);
//		this.pathGenerator = new Pathfinder();
	}
	
	public static boolean generatePath(WorldCoordinate start, WorldCoordinate goal, WorldLines w) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Start path generation...");
//		path = pathGenerator.findPath2(start, goal, w);
		path = PathFinderOrig.findPath(start, goal, w);
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Path size: "+path.size());
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Path generation done...");
		return Lemming.path != null;
	}
	
//	public Lemming(float size, float height, float locationX, float locationY, LemmingPath path, float speed, Color color) {
//		mesh = new CubeMesh(size, locationX, locationY, height, new RenderOptions(true, color, true));
//		this.locationX = locationX;
//		this.locationY = locationY;
//		this.size = size;
//		this.height = height;
//		this.speed = speed;
//		this.path = path;
//	}
	
	public float getLocationX() {
		return locationX;
	}
	
	public float getLocationY() {
		return locationY;
	}
	
	public MeshObject getMesh() {
		return mesh;
	}
	
	public synchronized void updateLocation(WorldCoordinate end, WorldLines w) {
		long startFrameUpdate = System.nanoTime();
		float fps = AppConfig.FPS_RANGE[1]/1000.0f; // max frame/sec
		float todoDistance = speed/fps;
//		synchronized (timestampLock) {
//			todoDistance = speed*((newTimestamp-locationUpdateTimestamp)/1000000000.0f);///fps;
//			locationUpdateTimestamp = newTimestamp;
//		}
		
		float newLocationX = locationX;
		float newLocationY = locationY;
		float distance;
		
		WorldCoordinate to = myPath.get(1);
		// When we will pass a node, the path will be updated first.
//		if(MathUtilities.distance(newLocationX,newLocationY,to.x,to.y) < todoDistance) {
//			this.myPath.
//			if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Finding path from "+path.get(0).toString()+", to "+end.toString());
//			LemmingPath tmpPath;
//			if(!AppConfig.TREE_ADAPTIVE_ASTAR)
//				tmpPath = PathFinderOrig.findPath(to, end, w);
//			else tmpPath = pathGenerator.findPath2(to, end, w);
//			if(tmpPath != null) {
//				LemmingPath newPath = new LemmingPath();
//				if(!AppConfig.TREE_ADAPTIVE_ASTAR || !path.get(0).equals(tmpPath.get(0))) {
//					newPath.add(0, path.get(0));
//				}
//				newPath.addAll(tmpPath);
//				this.myPath = newPath;
//			}
//		}
		
		if(AppConfig.DEBUG_LOGGING){
			Log.d(TAG, "THE Lemming's PATH: ");
			for (int i = 0; i < myPath.size(); i++) {
				Log.d(TAG, "PATH NODE: "+myPath.get(i).toString());
			}
		}
		
		if(myPath.get(1).equals(myPath.get(0))) {
			throw new IllegalStateException("Path is going from/to the same node!");
		}
		
		long startMoving = System.nanoTime();
		while(todoDistance > 0) {
			
			if(myPath.size() == 1) {
				newLocationX = myPath.get(0).x;
				newLocationY = myPath.get(0).y;
				break;
			}
			to = myPath.get(1);
			
			float distToEnd = MathUtilities.distance(newLocationX,newLocationY,to.x,to.y);
			if(distToEnd <= todoDistance || Math.abs(distToEnd-todoDistance) < 0.0001f) {
				distance = distToEnd;
				todoDistance -= distToEnd;
				myPath.remove(0);
//				LemmingPath tmpPath;
//				if(!AppConfig.TREE_ADAPTIVE_ASTAR) tmpPath = PathFinderOrig.findPath(to, end, w);
//				else tmpPath = pathGenerator.findPath2(to, end, w);
//				if(tmpPath != null) {
////					LemmingPath newPath = new LemmingPath();
////					if(!AppConfig.TREE_ADAPTIVE_ASTAR || !path.get(0).equals(tmpPath.get(0))) {
////						newPath.add(0, path.get(0));
////					}
////					newPath.addAll(tmpPath);
//					path = tmpPath;
//				}
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
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Moved lemming in "+(System.nanoTime()-startMoving)/1000000L+"ms");
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Lemming location updated: from ("+locationX+","+locationY+"), to ("+newLocationX+","+newLocationY+")");
		locationX = newLocationX;
		locationY = newLocationY;
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Amount of stars: "+w.getStars().size());
		for (Star s : w.getStars()) {
			if(MathUtilities.distance(locationX, locationY, s.getPosition().x, s.getPosition().y)
					< WorldConfig.STAR_PERIMETER) {
				w.removeStar(s);
				speed = WorldConfig.LEMMINGS_SPEED_NO_STARS;
			}
		}
		
		mesh = new CubeMesh(size, locationX, locationY, height, mesh.getRenderOptions());
	}
}
