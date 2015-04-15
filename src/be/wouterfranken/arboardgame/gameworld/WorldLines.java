package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject;
import be.wouterfranken.arboardgame.utilities.DebugUtilities;
import be.wouterfranken.arboardgame.utilities.MathUtilities;

public class WorldLines {
	private static final String TAG = WorldLines.class.getSimpleName();
	
	private List<LegoBrick> bricks = new ArrayList<LegoBrick>();
	private List<LegoBrickContainer> brickCandidates = new ArrayList<LegoBrickContainer>();
	private List<Star> stars = new ArrayList<Star>();
	private Object starLock = new Object();
	private Object brickLock = new Object();
//	private Map<WorldCoordinate,WorldNode> theWorld = new HashMap<WorldCoordinate,WorldNode>();
	private boolean worldGenerated = false;
	
	private final Grid worldGrid;
	
	public WorldLines() {
		
//		for(float x = WorldConfig.BORDER.getXStart();x<=WorldConfig.BORDER.getXEnd();x+=WorldConfig.NODE_DISTANCE) {
//			for(float y = WorldConfig.BORDER.getYStart();y<=WorldConfig.BORDER.getYEnd();y+=WorldConfig.NODE_DISTANCE) {
//				WorldNode newNode = new WorldNode();
//				WorldCoordinate current = new WorldCoordinate(x,y);
//				if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "WORLD Node generation, current node: "+current);
//				WorldCoordinate left = new WorldCoordinate(x-WorldConfig.NODE_DISTANCE,y);
//				WorldCoordinate bottom = new WorldCoordinate(x,y-WorldConfig.NODE_DISTANCE);
//				if(theWorld.get(left) != null) {
//					theWorld.get(left).setRight(current);
//					newNode.setLeft(left);
//				} 
//				if(theWorld.get(bottom) != null) {
//					theWorld.get(bottom).setTop(current);
//					newNode.setBottom(bottom);
//				}
//				theWorld.put(current, newNode);
//			}
//		}
		
		// Generate the world, fully linked.
		worldGrid = new Grid(WorldConfig.BORDER,WorldConfig.NODE_DISTANCE);
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "World is generated!");
		worldGenerated = true;
	}
	
	public boolean isWorldGenerated() {
		return worldGenerated;
	}
	
	public void activateAllBricks() {
		
		Log.d("FINMERGE", "Final mergeCheck!");
		ArrayList<LegoBrick> oldBrickList = new ArrayList<LegoBrick>(bricks);
		ArrayList<LegoBrick> newBrickList = new ArrayList<LegoBrick>();
		while (oldBrickList.size() > 0) {
			Log.d("FINMERGE", "OldBrickList size: "+oldBrickList.size());
			if(oldBrickList.size() == 1) {
				newBrickList.add(oldBrickList.remove(0));
				break;
			}
			LegoBrick b = oldBrickList.remove(0); 
			
			List<Integer> merges = b.mergeCheckAll(oldBrickList.toArray(new LegoBrick[oldBrickList.size()]), -1);
			Integer[] mergesArray = merges.toArray(new Integer[merges.size()]);
			Arrays.sort(mergesArray, Collections.reverseOrder());
			Log.d("FINMERGE", "MergeAmount: "+merges.size());
			for (int i = 0; i < mergesArray.length; i++) {
				Integer idx = mergesArray[i];
				oldBrickList.remove(idx.intValue());
				Log.d("FINMERGE", "Index "+idx+" removed from list.");
			}
			
			Log.d("FINMERGE", "OldBrickList size: "+oldBrickList.size());
					
//					new ArrayList<LegoBrick>(
//					Arrays.asList(b.mergeCheck(oldBrickList.toArray(new LegoBrickContainer[oldBrickList.size()]))));
			newBrickList.add(b);
			Log.d("FINMERGE", "NewBrickList size: "+newBrickList.size());
		}
		
		bricks = new ArrayList<LegoBrick>(newBrickList);
		
		for (LegoBrick brick : bricks) {
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
						GridNode node = worldGrid.getGridNode(new WorldCoordinate(x, y));
						node.setAccessible(false);
//						if(node.getLeft() != null) worldGrid.getGridNode(node.getLeft().getCoordinate()).setRight(null);
//						if(node.getBottom() != null) worldGrid.getGridNode(node.getBottom().getCoordinate()).setTop(null);
//						if(node.getRight() != null) worldGrid.getGridNode(node.getRight().getCoordinate()).setLeft(null);
//						if(node.getTop() != null) worldGrid.getGridNode(node.getTop().getCoordinate()).setBottom(null);
//						node.setLeft(null);
//						node.setRight(null);
//						node.setTop(null);
//						node.setBottom(null);
						
//						brick.addCoordinate(new WorldCoordinate(x, y));
//						if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Added BrickCoordinate: "+(new WorldCoordinate(x, y)));
					}
				}
			}
		}
	}
	
	int frameCounter = 0;
	
	public void addBricks(LegoBrickContainer[] bricksToAdd, int frameCount) {
		Log.d(TAG, "Amount of bricks-Size: "+brickCandidates.size());
		
		long startAddingBricks = System.nanoTime();
		
		synchronized (brickLock) {
			Iterator<LegoBrickContainer> candidatesIt = brickCandidates.iterator();
			while (candidatesIt.hasNext()) {
				LegoBrickContainer candidate = candidatesIt.next();
				bricksToAdd = candidate.mergeCheck(bricksToAdd, frameCount);
				if(candidate.size() == 0)
					candidatesIt.remove();
				else if(candidate.readyToBecomeRealBrick()) {
					Log.d("REALBRICK", "OK!");
					
					Log.d(TAG, "This brick just became real: (removal = "+candidate.get(0).getRemovalVotes()+"), (mergeCnt = "+candidate.get(0).getMergeCount()+"), (size = ("+
						Math.round(MathUtilities.norm(candidate.get(0).getHalfSideVectors()[0])*2/0.95f)+","+
						Math.round(MathUtilities.norm(candidate.get(0).getHalfSideVectors()[0])*2/1.6f)+","+
						Math.round(MathUtilities.norm(candidate.get(0).getHalfSideVectors()[0])*2/1.6f)+","+"))");
					
					candidate.get(0).resetRemoval();
					candidate.get(0).setAcceptedFrame(frameCounter);
					bricks.add(candidate.get(0));
					candidatesIt.remove();
				}
//				if(b.readyToBeActive()) activateBrick(b);
//				if(b.readyToBeRemoved()) removeBrick(i);
//				else if(b.readyToBeInactive()) deactivateBrick(b);
//				if(mergeIdx != -1) bricksToAdd[mergeIdx] = null;
			}
			
			for (LegoBrick legoBrick : bricks) {
				bricksToAdd = legoBrick.mergeCheck(bricksToAdd, frameCount);
			}
			
			// TODO: Newly added bricks also need to be merged with each other!
			ArrayList<LegoBrickContainer> bricksToAddList = new ArrayList<LegoBrickContainer>(Arrays.asList(bricksToAdd));
			ArrayList<LegoBrickContainer> newBricksToAddList = new ArrayList<LegoBrickContainer>();
			Log.d(TAG, "Length tracker begin: "+bricksToAdd.length);
			while (bricksToAddList.size() > 0) {
				if(bricksToAddList.size() == 1) {
					newBricksToAddList.add(bricksToAddList.remove(0));
					break;
				}
				LegoBrickContainer bc = bricksToAddList.remove(0); 
				bricksToAddList = new ArrayList<LegoBrickContainer>(
						Arrays.asList(bc.mergeCheck(bricksToAddList.toArray(new LegoBrickContainer[bricksToAddList.size()]),frameCount)));
				newBricksToAddList.add(bc);
				Log.d(TAG, "Length tracker: "+bricksToAdd.length);
			}
			
			bricksToAdd = newBricksToAddList.toArray(new LegoBrickContainer[newBricksToAddList.size()]);
			
			for (LegoBrickContainer bc : bricksToAdd) {
				float[][] cub3D = bc.get(0).getCuboid();
				Log.d("DebugUtilities", "New Brick!");
				for (int i = 0; i < cub3D.length; i++)
					DebugUtilities.logGLMatrix("BRICK Pt", cub3D[i], 1, 3);
				
				if(bc.readyToBecomeRealBrick()) { 
					bc.get(0).resetRemoval();
					bc.get(0).setAcceptedFrame(frameCounter);
					bricks.add(bc.get(0));
					Log.d(TAG, "This brick just became real: (removal = "+bc.get(0).getRemovalVotes()+"), (mergeCnt = "+bc.get(0).getMergeCount()+")");
				}
				else brickCandidates.add(bc);
			}
		}
		Log.d(TAG, "BricksSize: "+bricks.size());
		frameCounter++;
		Log.d(TAG, "Add bricks-time: "+((System.nanoTime() - startAddingBricks)/1000000.0f)+"ms");
//		synchronized (brickLock) {
//			Iterator<LegoBrickContainer> i = brickCandidates.iterator();
//			while(i.hasNext()) {
//				LegoBrickContainer b = i.next();
//				int mergeIdx = b.mergeCheck(bricksToAdd);
//				if(b.readyToBeActive()) activateBrick(b);
////				if(b.readyToBeRemoved()) removeBrick(i);
////				else if(b.readyToBeInactive()) deactivateBrick(b);
//				if(mergeIdx != -1) bricksToAdd[mergeIdx] = null;
//			}
//			
//			for (LegoBrick b : bricksToAdd) {
//				if(b != null) bricks.add(b);
//			}
//		}
//		Log.d(TAG, "BricksSize: "+bricks.size());
	}
	
//	private boolean deactivateBrick(LegoBrick brick) {
//		WorldCoordinate c;
//		synchronized (brickLock) {
//			if(!bricks.contains(brick)) return false;
//		}
//		brick.deactivate();
//		while((c = brick.removeCoordinate()) != null) {
//			WorldNode n = theWorld.get(c);
//			WorldNode left = theWorld.get(c.getLeft());
//			WorldNode right = theWorld.get(c.getRight());
//			WorldNode top = theWorld.get(c.getTop());
//			WorldNode bottom = theWorld.get(c.getBottom());
//			if(left!= null){
//				left.setRight(c);
//				n.setLeft(c.getLeft());
//			}
//			if(right!= null){
//				right.setLeft(c);
//				n.setRight(c.getRight());
//			}
//			if(top!= null){
//				top.setBottom(c);
//				n.setTop(c.getTop());
//			}
//			if(bottom!= null){
//				bottom.setTop(c);
//				n.setBottom(c.getBottom());
//			}
//		}
//		return true;
//	}
//	
//	private boolean removeBrick(Iterator<LegoBrick> i) {
//		synchronized (brickLock) {
//			i.remove();
//		}
//		return true;
//	}
	
	public int getBrickAmount() {
		synchronized (brickLock) {
			return bricks.size();
		}
	}
	
	public List<LegoBrick> getBricks() {
		return bricks;
	}
	
	public List<LegoBrickContainer> getCandidateBricks() {
		return this.brickCandidates;
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
	
	public void removeBrick(LegoBrick b) {
		bricks.remove(b);
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
	
	public GridNode getGridNode(WorldCoordinate coordinate) {
		return worldGrid.getGridNode(coordinate);
	}
	
	@Override
	public String toString() {
		String result = "";
		for (WorldCoordinate co : worldGrid.getCoordinates()) {
			if(getGridNode(co).isAccessible())
				result += co+", ";
		}
		return result;
	}
	
	public void clean() {
		brickCandidates.clear();
		ListIterator<LegoBrick> brickIt = bricks.listIterator();
		Log.d("CLEAN", "Cleaning starts ...");
		while(brickIt.hasNext()) {
			LegoBrick b1 = brickIt.next();
			Log.d("CLEAN", "First brick");
			ListIterator<LegoBrick> brickIt2 = bricks.listIterator(brickIt.nextIndex());
			while(brickIt2.hasNext()) {
				LegoBrick b2 = brickIt2.next();
				Log.d("CLEAN", "Second brick & overlap check!");
				double maxOverlapDistance = b1.overlaps3D(b2);
//				if(maxOverlapDistance)
				Log.d("CLEAN", "Result!");
				if(maxOverlapDistance > 0) Log.d("CLEAN", "Overlap3DDistance: "+maxOverlapDistance);
			}
		}
	}
	
//	public WorldNode getNode(WorldCoordinate co) {
//		return theWorld.get(co);
//	}
	
//	public int getSize() {
//		return theWorld.size();
//	}
}
