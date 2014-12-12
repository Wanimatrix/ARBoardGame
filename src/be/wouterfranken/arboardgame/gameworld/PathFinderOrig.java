package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import android.util.Log;
import android.util.Pair;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.gameworld.Pathfinder.Node;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPoseTracker;

public class PathFinderOrig {
private static final String TAG = PathFinderOrig.class.getSimpleName();
	
//	public static class Node implements Comparable<Node>{
//		private WorldCoordinate wc;
//		private float f;
//		private float g;
//		private float h;
//		private Node previousNode;
//		
//		public Node(WorldCoordinate wc) {
//			this.wc = wc;
//			f = 0;
//			g = 0;
//			previousNode = null;
//		}
//		
//		public void setFScore(float f) {
//			this.f = f;
//		}
//		
//		public void setGScore(float g) {
//			this.g = g;
//		}
//		
//		public void setHScore(float h) {
//			this.h = h;
//		}
//		
//		public float getFScore() {
//			return f;
//		}
//		
//		public float getGScore() {
//			return g;
//		}
//		
//		public float getHScore() {
//			return h;
//		}
//		
//		public WorldCoordinate getCoordinate() {
//			return wc;
//		}
//		
//		public List<Node> getNeighbours(World w) {
//			List<Node> result = new ArrayList<Node>();
//			WorldNode currentWNode = w.getNode(wc);
//			if(wc.getLeft() != null && currentWNode.getLeft() != null) result.add(new Node(wc.getLeft()));
//			if(wc.getRight() != null && currentWNode.getRight() != null) result.add(new Node(wc.getRight()));
//			if(wc.getTop() != null && currentWNode.getTop() != null) result.add(new Node(wc.getTop()));
//			if(wc.getBottom() != null && currentWNode.getBottom() != null) result.add(new Node(wc.getBottom()));
//			return result;
//		}
//		
//		public void setPreviousNode(Node n) {
//			previousNode = n;
//		}
//		
//		public Node getPreviousNode() {
//			return previousNode;
//		}
//		
//		@Override
//		public int hashCode() {
//			return wc.hashCode();
//		}
//		
//		@Override
//		public boolean equals(Object o) {
//			return o.getClass() == this.getClass() && ((Node)o).getCoordinate().equals(wc);
//		}
//		
//		@Override
//		public int compareTo(Node other) {
//			return Float.compare(this.getFScore(), other.getFScore());
//		}
//	}

	private static class PathNode implements Comparable<PathNode> {
		private float gScore = 0;
		private float hScore = 0;
		private PathNode previousNode;
		private GridNode theNode;
		
		public PathNode(GridNode theNode) {
			this.theNode = theNode;
		}
		
		public float getFScore() {
			return gScore + hScore;
		}
		
		public float getGScore() {
			return gScore;
		}
		
		public void setGScore(float g) {
			this.gScore = g;
		}
		
		public void setHScore(float h) {
			this.hScore = h;
		}

		public void setPreviousNode(PathNode n) {
			previousNode = n;
		}
		
		public PathNode getPreviousNode() {
			return previousNode;
		}
		
		public GridNode accessGridNode() {
			return theNode;
		}
		
		public List<PathNode> getNeighbours(Mat brickThreshold, CameraPoseTracker cameraPose) {
			List<PathNode> result = new ArrayList<PathNode>();
//			Log.d(TAG, "Node "+theNode.getCoordinate()+" has "+theNode.getNeighbours().size()+" neighbours"
//					+ ".");
//			int l = 0;
			for (GridNode nb : theNode.getNeighbours()) {
//				l++;
				WorldCoordinate wcoord = nb.getCoordinate();
				Mat coord3D = Mat.ones(4, 1, CvType.CV_32FC1);
				coord3D.put(0, 0, wcoord.x);
				coord3D.put(1, 0, wcoord.y);
				coord3D.put(2, 0, 0);
				Mat coord2D = cameraPose.get2DPointFrom3D(coord3D);
				coord2D.put(0, 0, coord2D.get(0,0)[0]/coord2D.get(2,0)[0]);
				coord2D.put(1, 0, coord2D.get(1,0)[0]/coord2D.get(2,0)[0]);
//				Log.d(TAG, "BrickThreshold == null? "+(brickThreshold == null));
//				Log.d(TAG, "ImageCoordinate: ("+(int)coord2D.get(0,0)[0]+","+(int)coord2D.get(1,0)[0]+")");
//				Log.d(TAG, "BrickThreshold borders: ("+brickThreshold.cols()+","+brickThreshold.rows()+")");
//				long start = System.nanoTime();
//				Log.d(TAG, "Threshold valueArray == null? "+(brickThreshold.get((int)coord2D.get(1,0)[0], (int)coord2D.get(0,0)[0]) == null ));
//				Log.d(TAG, "Got Threshold value in "+(System.nanoTime()-start)/1000000L+"ms");
//				result.add(new PathNode(nb));
				if(brickThreshold.empty() || brickThreshold.get((int)coord2D.get(1,0)[0], (int)coord2D.get(0,0)[0])[0] == 0) {
//					if(nb.getNeighbours().size() == 0) {
//						Log.e(TAG, "A normal grid node has no neighbours (Neighbour "+l+")!");
//					}
					result.add(new PathNode(nb));
//					if(!brickThreshold.empty())
//						Log.d(TAG, "Threshold value was: "+brickThreshold.get((int)coord2D.get(1,0)[0], (int)coord2D.get(0,0)[0])[0]);
				}
			}
//			Log.d(TAG, "Node "+theNode.getCoordinate()+" has "+theNode.getNeighbours().size()+" accessible neighbours.");
			return result;
		}
		
		@Override
		public int compareTo(PathNode another) {
			return Float.compare(this.getFScore(), another.getFScore());
		}
		
		@Override
		public int hashCode() {
			return theNode.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			return this == o || this.hashCode() == o.hashCode();
		}
	}
	
	public static LemmingPath findPath(final WorldCoordinate start, final WorldCoordinate target, World2 w) {
		long startTime = System.nanoTime();
		
		Map<WorldCoordinate,PathNode> closed = new HashMap<WorldCoordinate,PathNode>();
		PriorityQueue<PathNode> open = new PriorityQueue<PathNode>();
//		Map<WorldCoordinate, PathNode> openNodes = new HashMap<WorldCoordinate, PathNode>();
//		Map<GridNode, Pair<float[],GridNode>> fgPrevMap = new HashMap<GridNode, Pair<float[],GridNode>>();
		Mat brickThreshold = w.getBrickThreshold();
		CameraPoseTracker cameraPose = w.getCameraPoseTracker();
//		Highgui.imwrite("/sdcard/arbg/threshold.png",brickThreshold);
		
		PathNode current = null;
		PathNode startNode = new PathNode(w.getGridNode(start));
//		fgPrevMap.put(startNode, new Pair<float[], GridNode>(new float[]{start.distance(target), 0f},null));
		startNode.setGScore(0);
		startNode.setHScore(start.distance(target));
//		startNode.f = start.distance(target);
//		startNode.g = 0;
		open.add(startNode);
//		openNodes.put(start, startNode);
//		int i = 0;
		while(!open.isEmpty()) {
//			Log.d(TAG, "LoopNb: "+i);
//			i++;
			current = open.remove();
			if(current != null) {
//				Log.d(TAG, "CurrentPathNode: "+current.accessGridNode().getCoordinate());
				if(current.accessGridNode().getCoordinate().equals(target))
					break;
				
				closed.put(current.accessGridNode().getCoordinate(), current);
				for (PathNode nb : current.getNeighbours(brickThreshold, cameraPose)) {
					if(closed.containsKey(nb.accessGridNode().getCoordinate()))
						continue;
					float g = current.getGScore() + current.accessGridNode().getCoordinate().distance(nb.accessGridNode().getCoordinate());
					if(!open.contains(nb)) {
//						Log.d(TAG, "Unreachable Code Test: True!");
//						fgPrevMap.put(startNode, new Pair<float[], GridNode>(new float[]{g+Math.abs(nb.getCoordinate().x-target.x)+Math.abs(nb.getCoordinate().y-target.y), g},current));
						nb.setPreviousNode(current);
						nb.setGScore(g);
						nb.setHScore(Math.abs(nb.accessGridNode().getCoordinate().x-target.x)+Math.abs(nb.accessGridNode().getCoordinate().y-target.y));
						open.add(nb);
//						openNodes.put(nb.accessGridNode().getCoordinate(), nb);
					} else if(g < nb.getGScore()) {
//						Log.d(TAG, "Unreachable Code Test: False!");
//						fgPrevMap.put(nb, new Pair<float[], GridNode>(new float[]{g+nb.getCoordinate().distance(target), g},current));
						nb.setPreviousNode(current);
						nb.setGScore(g);
						nb.setHScore(nb.accessGridNode().getCoordinate().distance(target));
//						openNodes.get(nb.accessGridNode().getCoordinate()).setPreviousNode(current);
//						openNodes.get(nb.accessGridNode().getCoordinate()).setGScore(g);
//						openNodes.get(nb.accessGridNode().getCoordinate()).setHScore(nb.accessGridNode().getCoordinate().distance(target));
					}
				}
			}
		}
		
		if(!current.accessGridNode().getCoordinate().equals(target)) return null;
		
		LemmingPath path = new LemmingPath();
		while(current != null) {
			path.add(current.accessGridNode().getCoordinate());
			current = current.getPreviousNode();
		}
		Collections.reverse(path);
		
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Lemming path found in "+(System.nanoTime()-startTime)/1000000L+"ms");
		
		return path;
	}
}
