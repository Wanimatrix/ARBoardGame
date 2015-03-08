package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject;
import be.wouterfranken.arboardgame.utilities.MathUtilities;

public class World {
	private static final String TAG = World.class.getSimpleName();
	
	private List<LegoBrick> bricks = new ArrayList<LegoBrick>();
	private List<Star> stars = new ArrayList<Star>();
	private Object starLock = new Object();
	private Object brickLock = new Object();
	private Map<WorldCoordinate,WorldNode> theWorld = new HashMap<WorldCoordinate,WorldNode>();
	private boolean worldGenerated = false;
	
	public World() {
		// Generate the world, fully linked.
		for(float x = WorldConfig.BORDER.getXStart();x<=WorldConfig.BORDER.getXEnd();x+=WorldConfig.NODE_DISTANCE) {
			for(float y = WorldConfig.BORDER.getYStart();y<=WorldConfig.BORDER.getYEnd();y+=WorldConfig.NODE_DISTANCE) {
				WorldNode newNode = new WorldNode();
				WorldCoordinate current = new WorldCoordinate(x,y);
				if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "WORLD Node generation, current node: "+current);
				WorldCoordinate left = new WorldCoordinate(x-WorldConfig.NODE_DISTANCE,y);
				WorldCoordinate bottom = new WorldCoordinate(x,y-WorldConfig.NODE_DISTANCE);
				if(theWorld.get(left) != null) {
					theWorld.get(left).setRight(current);
					newNode.setLeft(left);
				} 
				if(theWorld.get(bottom) != null) {
					theWorld.get(bottom).setTop(current);
					newNode.setBottom(bottom);
				}
				theWorld.put(current, newNode);
			}
		}
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "World is generated!");
		worldGenerated = true;
	}
	
	public boolean isWorldGenerated() {
		return worldGenerated;
	}
	
	private void activateBrick(LegoBrick brick) {
		float[][] brickCorners = brick.getCuboid();
		float[] xBounds = brick.getXBounds();
		float[] yBounds = brick.getYBounds();
		WorldCoordinate xBnd = WorldCoordinate.getClosestWorldCoordinate(xBounds, true, false);
		WorldCoordinate yBnd = WorldCoordinate.getClosestWorldCoordinate(yBounds, true, false);
		
		// If the brick is anywhere out of borders: don't add this brick to the world.
		if(xBnd.x < WorldConfig.BORDER.getXStart() || xBnd.y > WorldConfig.BORDER.getXEnd()
				|| yBnd.x < WorldConfig.BORDER.getYStart() || yBnd.y > WorldConfig.BORDER.getYEnd()) return;
		
		brick.setActive(true);
		
		for(float x = xBnd.x;x<=xBnd.y;x+=WorldConfig.NODE_DISTANCE) {
			for(float y = yBnd.x;y<=yBnd.y;y+=WorldConfig.NODE_DISTANCE) {
				int result0 = MathUtilities.pointLeftOfEdge(new float[]{x,y,0}, brickCorners[0], brickCorners[1]);
				int result1 = MathUtilities.pointLeftOfEdge(new float[]{x,y,0}, brickCorners[1], brickCorners[2]);
				int result2 = MathUtilities.pointLeftOfEdge(new float[]{x,y,0}, brickCorners[2], brickCorners[3]);
				int result3 = MathUtilities.pointLeftOfEdge(new float[]{x,y,0}, brickCorners[3], brickCorners[0]);
				if(result0 == result1 && result1 == result2 && result2 == result3) {
					WorldNode node = theWorld.get(new WorldCoordinate(x, y));
					if(node.getLeft() != null) theWorld.get(node.getLeft()).setRight(null);
					if(node.getBottom() != null) theWorld.get(node.getBottom()).setTop(null);
					if(node.getRight() != null) theWorld.get(node.getRight()).setLeft(null);
					if(node.getTop() != null) theWorld.get(node.getTop()).setBottom(null);
					node.setLeft(null);
					node.setRight(null);
					node.setTop(null);
					node.setBottom(null);
					brick.addCoordinate(new WorldCoordinate(x, y));
					if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Added BrickCoordinate: "+(new WorldCoordinate(x, y)));
				}
			}
		}
	}
	
	public void addBricks(LegoBrick[] bricksToAdd) {
		synchronized (brickLock) {
			Iterator<LegoBrick> i = bricks.iterator();
			while(i.hasNext()) {
				LegoBrick b = i.next();
				int mergeIdx = b.mergeCheck(bricksToAdd);
				if(b.readyToBeActive()) activateBrick(b);
				if(b.readyToBeRemoved()) removeBrick(i);
				else if(b.readyToBeInactive()) deactivateBrick(b);
				if(mergeIdx != -1) bricksToAdd[mergeIdx] = null;
			}
			
			for (LegoBrick b : bricksToAdd) {
				if(b != null) bricks.add(b);
			}
		}
		Log.d(TAG, "BricksSize: "+bricks.size());
	}
	
	private boolean deactivateBrick(LegoBrick brick) {
		WorldCoordinate c;
		synchronized (brickLock) {
			if(!bricks.contains(brick)) return false;
		}
		brick.deactivate();
		while((c = brick.removeCoordinate()) != null) {
			WorldNode n = theWorld.get(c);
			WorldNode left = theWorld.get(c.getLeft());
			WorldNode right = theWorld.get(c.getRight());
			WorldNode top = theWorld.get(c.getTop());
			WorldNode bottom = theWorld.get(c.getBottom());
			if(left!= null){
				left.setRight(c);
				n.setLeft(c.getLeft());
			}
			if(right!= null){
				right.setLeft(c);
				n.setRight(c.getRight());
			}
			if(top!= null){
				top.setBottom(c);
				n.setTop(c.getTop());
			}
			if(bottom!= null){
				bottom.setTop(c);
				n.setBottom(c.getBottom());
			}
		}
		return true;
	}
	
	private boolean removeBrick(Iterator<LegoBrick> i) {
		synchronized (brickLock) {
			i.remove();
		}
		return true;
	}
	
	public int getBrickAmount() {
		synchronized (brickLock) {
			return bricks.size();
		}
	}
	
	public List<LegoBrick> getActiveBricks() {
		List<LegoBrick> result;
		synchronized (brickLock) {
			result = new ArrayList<LegoBrick>();
			for (int i = 0; i < bricks.size(); i++) {
				if(bricks.get(i).isActive()) result.add(bricks.get(i));
			}
		}
		return result;
	}
	
//	public int getActiveBrickAmount() {
//		
//		int count = 0;
//		for (int i = 0; i < bricks.size(); i++) {
//			if(bricks.get(i).readyToBeActive()) count++;
//		}
//		return count;
//	}
	
	public void addStars() {
		synchronized (starLock) {
			while(stars.size() < WorldConfig.STAR_AMOUNT_PER_LEMMING)
				this.stars.add(new Star());
		}
	}
	
	public void removeStar(Star s) {
		synchronized (starLock) {
			this.stars.remove(s);
		}
	}
	
	public List<Star> getStars() {
		List<Star> stars = new ArrayList<Star>();
		synchronized (starLock) {
			for (Star s : this.stars) {
				stars.add(s);
			}
		}
		return stars;
	}
	
	public List<MeshObject> getStarMeshes() {
		List<MeshObject> starMeshes = new ArrayList<MeshObject>();
		synchronized (starLock) {
			for (Star s : stars) {
				starMeshes.add(s.getMesh());
			}
		}
		return starMeshes;
	}
	
	public WorldNode getNode(WorldCoordinate co) {
		return theWorld.get(co);
	}
	
	public int getSize() {
		return theWorld.size();
	}
}
