package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import android.util.Log;
import android.util.Pair;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPoseTracker;

public class PathFinderOrig {
private static final String TAG = PathFinderOrig.class.getSimpleName();
	private static long[] time = new long[]{0,0,0};

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
		
//		public List<PathNode> getAllNeighbours(int hops) {
//			List<PathNode> result = new ArrayList<PathNode>();
//			GridNode current = theNode.getLeft();
//			for(int i = 1; current != null; i++) {
//				if(i >= hops){
//					result.add(new PathNode(current));
//					break;
//				}
//				if(current.getLeft() != null)
//					current = current.getLeft();
//			}
//			current = theNode.getRight();
//			for(int i = 1; current != null; i++) {
//				if(i >= hops){
//					result.add(new PathNode(current));
//					break;
//				}
//				if(current.getRight() != null)
//					current = current.getRight();
//			}
//			current = theNode.getTop();
//			for(int i = 1; current != null; i++) {
//				if(i >= hops){
//					result.add(new PathNode(current));
//					break;
//				}
//				if(current.getTop() != null)
//					current = current.getTop();
//			}
//			current = theNode.getBottom();
//			for(int i = 1; current != null; i++) {
//				if(i >= hops) {
//					result.add(new PathNode(current));
//					break;
//				}
//				if(current.getBottom() != null)
//					current = current.getBottom();
//			}
//			if(hops == 1) {
//			
//				Log.d(TAG, "HOPS: "+hops);
//				Log.d(TAG, "THIS: "+theNode.getCoordinate());
//				for (PathNode pathNode : result) {
//					Log.d(TAG, "NEIGHBOUR: "+pathNode.accessGridNode().getCoordinate());
//				}
//			}
//			return result;
//		}
		
		public List<PathNode> getAccessibleNeighbours(Mat brickThreshold, CameraPoseTracker cameraPose, Mat mv, int hops) {
			List<PathNode> result = new ArrayList<PathNode>();
//			Log.d(TAG, "Node "+theNode.getCoordinate()+" has "+theNode.getNeighbours().size()+" neighbours"
//					+ ".");
//			int l = 0;
			List<GridNode> nbs = theNode.getNeighbours();
			long start = System.nanoTime();
			Mat coord3D = Mat.ones(4, nbs.size(), CvType.CV_32FC1);
			for (int i = 0;i<nbs.size();i++) {
				WorldCoordinate wcoord = nbs.get(i).getCoordinate();
//				Log.d(TAG, "COORD: "+wcoord);
				coord3D.put(0, i, wcoord.x);
				coord3D.put(1, i, wcoord.y);
				coord3D.put(2, i, 0);
			}
			time[0] += System.nanoTime()-start;
			start = System.nanoTime();
			Mat coord2D = cameraPose.get2DPointFrom3D(coord3D, mv);
			time[1] += System.nanoTime()-start;
			
			start = System.nanoTime();
			for (int i = 0;i<nbs.size();i++) {
//				coord2D.put(0, i, );
//				coord2D.put(1, i, ;
//				int row = ;
//				int col = ;
				double threshValue = brickThreshold.get(
						(int)(coord2D.get(1,i)[0]*Math.pow(coord2D.get(2,i)[0],-1)),
						(int)(coord2D.get(0,i)[0]*Math.pow(coord2D.get(2,i)[0],-1)))[0];
//				Log.d(TAG, "treshValue: "+threshValue);
				if(brickThreshold.empty() || threshValue == 0) {
					result.add(new PathNode(nbs.get(i)));
				}
			}
			time[2] += System.nanoTime()-start;
//				l++;
//				
//				long start = System.nanoTime();
//				Mat coord3D = Mat.ones(4, 1, CvType.CV_32FC1);
				
				
				
//				Log.d(TAG, "3D to 2D in "+(System.nanoTime()-start)/1000000L+"ms");
//				coord2D.put(0, 0, coord2D.get(0,0)[0]/coord2D.get(2,0)[0]);
//				coord2D.put(1, 0, coord2D.get(1,0)[0]/coord2D.get(2,0)[0]);
				
				
//				Log.d(TAG, "BrickThreshold == null? "+(brickThreshold == null));
//				Log.d(TAG, "ImageCoordinate: ("+(int)coord2D.get(0,0)[0]+","+(int)coord2D.get(1,0)[0]+")");
//				Log.d(TAG, "BrickThreshold borders: ("+brickThreshold.cols()+","+brickThreshold.rows()+")");
//				long start = System.nanoTime();
//				Log.d(TAG, "Threshold valueArray == null? "+(brickThreshold.get((int)coord2D.get(1,0)[0], (int)coord2D.get(0,0)[0]) == null ));
//				Log.d(TAG, "Got Threshold value in "+(System.nanoTime()-start)/1000000L+"ms");
//				result.add(new PathNode(nb));
//				start = System.nanoTime();
				
//				timeForGettingThreshVal += System.nanoTime()-start;
				
//					if(nb.getNeighbours().size() == 0) {
//						Log.e(TAG, "A normal grid node has no neighbours (Neighbour "+l+")!");
//					}
					
//					if(!brickThreshold.empty())
//						Log.d(TAG, "Threshold value was: "+brickThreshold.get((int)coord2D.get(1,0)[0], (int)coord2D.get(0,0)[0])[0]);
//				}
//			}
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
		time = new long[]{0,0,0};
		
		Map<WorldCoordinate,PathNode> closed = new HashMap<WorldCoordinate,PathNode>();
		PriorityQueue<PathNode> open = new PriorityQueue<PathNode>();
//		Map<WorldCoordinate, PathNode> openNodes = new HashMap<WorldCoordinate, PathNode>();
//		Map<GridNode, Pair<float[],GridNode>> fgPrevMap = new HashMap<GridNode, Pair<float[],GridNode>>();
		Mat brickThreshold = w.getBrickThreshold();
		CameraPoseTracker cameraPose = w.getCameraPoseTracker();
		Mat mv = cameraPose.getMvMat();
		if(mv == null || mv.empty()) return null;
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
				
//				PathNode hopCheckCurrent = current;
//				int hopCnt = 0;
//				while(hopCheckCurrent != null) {
//					hopCheckCurrent = hopCheckCurrent.getPreviousNode();
//					hopCnt++;
//				}
//				if(hopCnt >= maxHops ) break;
				
				closed.put(current.accessGridNode().getCoordinate(), current);
//				long startNbs = System.nanoTime();
				
				List<PathNode> nbs;
				if(current.getGScore() >= 4* WorldConfig.NODE_DISTANCE)
					nbs = current.getAccessibleNeighbours(brickThreshold, cameraPose, mv,3);
				else
					nbs = current.getAccessibleNeighbours(brickThreshold, cameraPose, mv,1);
				for (PathNode nb : nbs) {
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
						nb.setHScore(Math.abs(nb.accessGridNode().getCoordinate().x-target.x)+Math.abs(nb.accessGridNode().getCoordinate().y-target.y));
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
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Time needed for FOR1: "+time[0]/1000000.0f+"ms");
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Time needed for 3dTo2D: "+time[1]/1000000.0f+"ms");
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Time needed for FOR2: "+time[2]/1000000.0f+"ms");
		
		return path;
	}
}
