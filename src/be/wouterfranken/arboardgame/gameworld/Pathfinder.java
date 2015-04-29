package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.os.Process;
import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPoseTracker;

// TODO: Generate new path after passing node EACH TIME (even if passing multiple nodes in 1 frame). (FIX THIS IN Lemming class)
// TODO: Fix that we sense the new node after each time, even if path was null before.
// TODO: Fix that neighbours are set to inaccessible, while they actually aren't.

public class Pathfinder {
	private static final String TAG = Pathfinder.class.getSimpleName();
	
	private List<PathNode> closed = new ArrayList<PathNode>();
	private PriorityQueue<PathNode> open = new PriorityQueue<PathNode>();
	private Map<WorldCoordinate, PathNode> nodes = new HashMap<WorldCoordinate, PathNode>();
	private ReusableTree tree = new ReusableTree();
	private LemmingPath path = null;
	
	private Mat currentMv;
	private Mat currentThreshold;
	
	public Pathfinder() {
		tree.initSearchCount();
		tree.setHmax(0, -1);
		
//		for(float x = WorldConfig.BORDER.getXStart();x<=WorldConfig.BORDER.getXEnd();x+=WorldConfig.NODE_DISTANCE) {
//			for(float y = WorldConfig.BORDER.getYStart();y<=WorldConfig.BORDER.getYEnd();y+=WorldConfig.NODE_DISTANCE) {
//				Node n = new Node(new WorldCoordinate(x, y));
//				tree.setGenerated(n, 0);
//				tree.setId(n, 0);
//				tree.setParent(n, null);
//				WorldCoordinate left = n.getCoordinate().getLeft();
//				WorldCoordinate bottom = n.getCoordinate().getBottom();
//				if(left != null && nodes.get(left) != null) {
//					nodes.get(left).right = n;
//					n.left = nodes.get(left);
//				} 
//				if(bottom != null && nodes.get(bottom) != null) {
//					nodes.get(bottom).top = n;
//					n.bottom = nodes.get(bottom);
//				}
//				nodes.put(n.getCoordinate(), n);
//			}
//		}
	}
	
	private PathNode getNode(GridNode g) {
		if(nodes.containsKey(g.getCoordinate()) && nodes.get(g.getCoordinate()) != null) return nodes.get(g.getCoordinate());
		else {
			PathNode result = new PathNode(g);
			nodes.put(g.getCoordinate(), result);
			return result;
		}
	}
	
	private boolean computePath(final WorldCoordinate start, final WorldCoordinate goal, World2 w) {
		PathNode current;
		while(!open.isEmpty()) {
			current = open.remove();
			
			if(current.accessGridNode().getCoordinate().equals(goal) || current.getHScore() <= tree.getHmax(tree.getId(current))) {
				// Current is in reusable tree!
				
				if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Current node "+current.accessGridNode().getCoordinate()+" is in the reusable tree!");
				
				for (PathNode current2 : closed)
					current2.setHScore(current.getFScore()-current2.getGScore());
				
				if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Adding new found paths to the reusable tree!");
				tree.addPath(current, getNode(w.getGridNode(start)), goal);
				return true;
			}
			
			closed.add(current);
			
			if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Fetching and processing neighbours of "+ current.accessGridNode().getCoordinate()+"; HASH: "+current.hashCode()+" ...");
			for (PathNode nb : current.getAccessibleNeighbours()) {
				Log.d("PATHFINDER", "Nb hashcode: "+nb.hashCode());
				tree.initState(nb, goal);
				if(nb.getGScore() > current.getGScore()+current.accessGridNode().getCoordinate().distance(nb.accessGridNode().getCoordinate())) {
					nb.setGScore(current.getGScore()+current.accessGridNode().getCoordinate().distance(nb.accessGridNode().getCoordinate()));
					Log.d("PATHFINDER", "New G-score: "+nb.getGScore());
					nb.setPreviousNode(current);
					if(open.contains(nb))
						open.remove(nb);
//					nb.setFScore(nb.getGScore()+nb.getHScore());
					open.add(nb);
				}
			}
			
			Log.d("PATHFINDER", "CURRENT OPENLIST");
			for (PathNode pathNode : open) {
				Log.d("PATHFINDER", "Node: "+pathNode.accessGridNode().getCoordinate().toString()+"; G: "+pathNode.gScore+"; H: "+pathNode.hScore+"; F: "+pathNode.getFScore()+"; HASH: "+pathNode.hashCode());
			}
		}
		return false;
	}
	
	private LemmingPath stepPath(WorldCoordinate start, WorldCoordinate goal, World2 w) {
		if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Stepping to next node...");
		PathNode startNode = getNode(w.getGridNode(start));
		//nodes.get(start);
		
		if(startNode.getHScore() <= tree.getHmax(tree.getId(startNode))) {
			if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "This node "+startNode.accessGridNode().getCoordinate()+" is a non-goal state in reusable tree.");
			buildPath(startNode, goal);
			
			startNode = tree.getParent(startNode);
			
			if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Sensing the blocking of bricks at the new node...");
			
			List<PathNode> nbs = startNode.senseAccessibleNeighbours(currentThreshold, w.getCameraPoseTracker(), currentMv);
//			if(nbs.size() == 0) Process.killProcess(Process.myPid());
			Log.d("PATHFINDER", "New node has "+nbs.size()+" accessible neighbours");
//			WorldCoordinate[] pureNeighboursCoords = new WorldCoordinate[]{
//					startNode.getCoordinate().getLeft(),
//					startNode.getCoordinate().getRight(),
//					startNode.getCoordinate().getTop(),
//					startNode.getCoordinate().getBottom()};
			List<PathNode> allNeighbourNodes = startNode.getAllNeighbours();
			Log.d("PATHFINDER", "New node really has "+allNeighbourNodes.size()+" neighbours");
			for (int i = 0; i < allNeighbourNodes.size(); i++) 
			{
//				WorldCoordinate nbCoord = pureNeighbourNodes.get(i).accessGridNode().getCoordinate();
				PathNode nb = allNeighbourNodes.get(i);
				if (!nb.isInaccessible() && !nbs.contains(nb)) {
//					if(i == 0) {
//						Log.d("PATHFINDER", "Left neighbour set to null");
//						nb.right = null;
//						startNode.left = null;
//					} else if(i == 1) {
//						Log.d("PATHFINDER", "Right neighbour set to null");
//						nb.left = null;
//						startNode.right = null;
//					} else if(i == 2) {
//						Log.d("PATHFINDER", "Bottom neighbour set to null");
//						nb.bottom = null;
//						startNode.top = null;
//					} else if(i == 3) {
//						Log.d("PATHFINDER", "Top neighbour set to null");
//						nb.top = null;
//						startNode.bottom = null;
//					}
					Log.d("PATHFINDER", "Neighbour "+nb.accessGridNode().getCoordinate()+" set to inaccessible");
					nb.setInaccessible(true);
					if(tree.getParent(startNode).equals(nb))
						tree.removePaths(startNode);
				} else if(nb.isInaccessible() && nbs.contains(nb)) {
//					if(i == 0) {
//						nodes.get(nbCoord).right = startNode;
//						startNode.left = nodes.get(nbCoord);
//					} else if(i == 1) {
//						nodes.get(nbCoord).left = startNode;
//						startNode.right = nodes.get(nbCoord);
//					} else if(i == 2) {
//						nodes.get(nbCoord).bottom = startNode;
//						startNode.top = nodes.get(nbCoord);
//					} else if(i == 3) {
//						nodes.get(nbCoord).top = startNode;
//						startNode.bottom = nodes.get(nbCoord);
//					}
					Log.d("PATHFINDER", "Neighbour "+nb.accessGridNode().getCoordinate()+" set to accessible");
					nb.setInaccessible(false);
					if(tree.getParent(startNode).equals(nb))
						tree.removePaths(startNode);
				}
			}
		} else {
			if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "This node "+startNode.accessGridNode().getCoordinate()+" is a goal state or is not in reusable tree.");
			if(start.equals(goal)) {
				if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "This node "+startNode.accessGridNode().getCoordinate()+" is a goal state.");
				path = new LemmingPath();
				path.add(goal);
			} else {
				if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "This node "+startNode.accessGridNode().getCoordinate()+" is not in the reusable tree.");
				tree.incSearchCount();
				if(!performNewSearch(start, goal, w)) return null;
				return stepPath(start,goal,w);
			}
		}
		return path;
	}
	
	private boolean performNewSearch(final WorldCoordinate start, final WorldCoordinate goal, World2 w) {
		if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Starting new search...");
		PathNode startNode = getNode(w.getGridNode(start));
		//nodes.get(start);
		Log.d("PATHFINDER", "Init start "+start+"...");
		tree.initState(startNode, goal);
		startNode.setGScore(0);
		open = new PriorityQueue<PathNode>();
		closed = new ArrayList<PathNode>();
		this.currentMv = w.getCameraPoseTracker().getMvMat();
		this.currentThreshold = w.getBrickThreshold();
//		startNode.setFScore(startNode.getGScore()+startNode.getHScore());
		open.add(startNode);
		if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER","Starting path computation...");
		if(!computePath(start, goal, w)) return false;
		if(path == null) buildPath(startNode, goal);
		return true;
	}
	
	public LemmingPath findPath2(final WorldCoordinate start, final WorldCoordinate goal, World2 w) {
		long startTime = System.nanoTime();
		Log.d("PATHFINDER", "Search started @"+start+" to "+goal);
		if(path != null && path.get(0).equals(start)) return path;
		if(path == null) performNewSearch(start, goal, w);
		else stepPath(start, goal, w);
		Log.d("PATHFINDER", "New path has size: "+path.size());
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Lemming path found in "+(System.nanoTime()-startTime)/1000000L+"ms");
		return path;
	}
	
	private void buildPath(PathNode startNode, WorldCoordinate goal) {
		if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Building new resulting path from "+startNode.accessGridNode().getCoordinate()+".");
		path = new LemmingPath();
		PathNode current = startNode;
		while(current.getHScore() <= tree.getHmax(tree.getId(current))) {
			if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Current path node "+current.accessGridNode().getCoordinate()+".");
			path.add(current.accessGridNode().getCoordinate());
			current = tree.getParent(current);
			if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Current path node's parent "+current.accessGridNode().getCoordinate()+".");
			if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Current path node's parent has id "+tree.getId(current)+" and this Id has Hmax "+tree.getHmax(tree.getId(current))+", hScore: "+current.getHScore());
		}
		path.add(current.accessGridNode().getCoordinate());
	}
	
//	public class Node implements Comparable<Node>{
//		private WorldCoordinate wc;
//		private Node left;
//		private Node top;
//		private Node right;
//		private Node bottom;
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
//		public List<Node> getNeighbours(World w, boolean checkBricks) {
//			List<Node> result = new ArrayList<Node>();
//			
//			if(left != null && (!checkBricks || w.getNode(wc).getLeft() != null)) result.add(left);
//			if(right != null && (!checkBricks || w.getNode(wc).getRight() != null)) result.add(right);
//			if(top != null && (!checkBricks || w.getNode(wc).getTop() != null)) result.add(top);
//			if(bottom != null && (!checkBricks || w.getNode(wc).getBottom() != null)) result.add(bottom);
//			
//			return result;
//		}
//		
//		public List<WorldCoordinate> getNeighbouringCoords(World w, boolean checkBricks) {
//			List<WorldCoordinate> result = new ArrayList<WorldCoordinate>();
//			
//			if(left != null && (!checkBricks || w.getNode(wc).getLeft() != null)) result.add(left.getCoordinate());
//			if(right != null && (!checkBricks || w.getNode(wc).getRight() != null)) result.add(right.getCoordinate());
//			if(top != null && (!checkBricks || w.getNode(wc).getTop() != null)) result.add(top.getCoordinate());
//			if(bottom != null && (!checkBricks || w.getNode(wc).getBottom() != null)) result.add(bottom.getCoordinate());
//			
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
//			return o != null && o.getClass() == this.getClass() && ((Node)o).getCoordinate().equals(wc);
//		}
//		
//		@Override
//		public int compareTo(Node other) {
//			return Float.compare(this.getFScore(), other.getFScore());
//		}
//	}
	
	public class PathNode implements Comparable<PathNode> {
		private float gScore = 0;
		private float hScore = 0;
		private PathNode previousNode;
		private GridNode theNode;
		
		private boolean inaccessible = false;
		
		public PathNode(GridNode theNode) {
			this.theNode = theNode;
		}
		
		public float getFScore() {
			return gScore + hScore;
		}
		
		public float getGScore() {
			return gScore;
		}
		
		public float getHScore() {
			return hScore;
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
		
		public void setInaccessible(boolean inaccessible) {
			this.inaccessible = inaccessible;
		}
		
		public boolean isInaccessible() {
			return inaccessible;
		}
		
		public List<PathNode> getAllNeighbours() {
			List<PathNode> result = new ArrayList<PathNode>();
			if(theNode.getLeft() != null) result.add(getNode(theNode.getLeft()));
			if(theNode.getRight() != null) result.add(getNode(theNode.getRight()));
			if(theNode.getTop() != null) result.add(getNode(theNode.getTop()));
			if(theNode.getBottom() != null) result.add(getNode(theNode.getBottom()));
			return result;
		}
		
		public List<PathNode> getAccessibleNeighbours() {
			List<PathNode> result = new ArrayList<PathNode>();
			if(theNode.getLeft() != null && !getNode(theNode.getLeft()).isInaccessible()) result.add(getNode(theNode.getLeft()));
			if(theNode.getRight() != null && !getNode(theNode.getRight()).isInaccessible()) result.add(getNode(theNode.getRight()));
			if(theNode.getTop() != null && !getNode(theNode.getTop()).isInaccessible()) result.add(getNode(theNode.getTop()));
			if(theNode.getBottom() != null && !getNode(theNode.getBottom()).isInaccessible()) result.add(getNode(theNode.getBottom()));
			return result;
		}
		
		public List<PathNode> senseAccessibleNeighbours(Mat brickThreshold, CameraPoseTracker cameraPose, Mat mv) {
			List<PathNode> result = new ArrayList<PathNode>();
			
			List<GridNode> nbs = theNode.getNeighbours();
//			long start = System.nanoTime();
			Mat coord3D = Mat.ones(4, nbs.size(), CvType.CV_32FC1);
			for (int i = 0;i<nbs.size();i++) {
				WorldCoordinate wcoord = nbs.get(i).getCoordinate();
//				Log.d(TAG, "COORD: "+wcoord);
				coord3D.put(0, i, wcoord.x);
				coord3D.put(1, i, wcoord.y);
				coord3D.put(2, i, 0);
			}
//			time[0] += System.nanoTime()-start;
//			start = System.nanoTime();
			Mat coord2D = cameraPose.get2DPointFrom3D(coord3D, mv);
//			time[1] += System.nanoTime()-start;
			
//			start = System.nanoTime();
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
					result.add(getNode(nbs.get(i)));
				}
			}
//			time[2] += System.nanoTime()-start;
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
}
