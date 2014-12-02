package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.utilities.MathUtilities;

public class World {
	private static final String TAG = World.class.getSimpleName();
	
	private List<LegoBrick> bricks = new ArrayList<LegoBrick>();
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
	
	public void addBrick(LegoBrick brick) {
		float[][] brickCorners = brick.getCuboid();
		float[] xBounds = brick.getXBounds();
		float[] yBounds = brick.getYBounds();
		WorldCoordinate xBnd = WorldCoordinate.getClosestWorldCoordinate(xBounds, true, false);
		WorldCoordinate yBnd = WorldCoordinate.getClosestWorldCoordinate(yBounds, true, false);
		
		// If the brick is anywhere out of borders: don't add this brick to the world.
		if(xBnd.x < WorldConfig.BORDER.getXStart() || xBnd.y > WorldConfig.BORDER.getXEnd()
				|| yBnd.x < WorldConfig.BORDER.getYStart() || yBnd.y > WorldConfig.BORDER.getYEnd()) return;
		
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
		bricks.add(brick);
	}
	
	public boolean removeBrick() {
		WorldCoordinate c;
		if(bricks.size() == 0) return false;
		LegoBrick brick = bricks.remove(0);
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
	
	public WorldNode getNode(WorldCoordinate co) {
		return theWorld.get(co);
	}
	
	public int getSize() {
		return theWorld.size();
	}
}
