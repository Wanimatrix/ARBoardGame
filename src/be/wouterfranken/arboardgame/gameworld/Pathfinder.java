package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;

public class Pathfinder {
	private static final String TAG = Pathfinder.class.getSimpleName();
	
	private List<Node> closed = new ArrayList<Node>();
	private PriorityQueue<Node> open = new PriorityQueue<Node>();
	private Map<WorldCoordinate, Node> nodes = new HashMap<WorldCoordinate, Pathfinder.Node>();
	private ReusableTree tree = new ReusableTree();
	private LemmingPath path = null;
	
	public Pathfinder() {
		tree.initSearchCount();
		tree.setHmax(0, -1);
		
		for(float x = WorldConfig.BORDER.getXStart();x<=WorldConfig.BORDER.getXEnd();x+=WorldConfig.NODE_DISTANCE) {
			for(float y = WorldConfig.BORDER.getYStart();y<=WorldConfig.BORDER.getYEnd();y+=WorldConfig.NODE_DISTANCE) {
				Node n = new Node(new WorldCoordinate(x, y));
				tree.setGenerated(n, 0);
				tree.setId(n, 0);
				tree.setParent(n, null);
				WorldCoordinate left = n.getCoordinate().getLeft();
				WorldCoordinate bottom = n.getCoordinate().getBottom();
				if(left != null && nodes.get(left) != null) {
					nodes.get(left).right = n;
					n.left = nodes.get(left);
				} 
				if(bottom != null && nodes.get(bottom) != null) {
					nodes.get(bottom).top = n;
					n.bottom = nodes.get(bottom);
				}
				nodes.put(n.getCoordinate(), n);
			}
		}
	}
	
	private boolean computePath(final WorldCoordinate start, final WorldCoordinate goal, World w) {
		Node current;
		while(!open.isEmpty()) {
			current = open.remove();
			
			if(current.getCoordinate().equals(goal) || PathUtilities.h(current.getCoordinate(),goal) <= tree.getHmax(tree.getId(current))) {
				// Current is in reusable tree!
				
				if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Current node "+current.getCoordinate()+" is in the reusable tree!");
				
				for (Node current2 : closed)
					current2.setHScore(current.getGScore()+current.getHScore()-current2.getGScore());
				
				if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Adding new found paths to the reusable tree!");
				tree.addPath(current, start, goal);
				return true;
			}
			
			closed.add(current);
			
			if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Fetching and processing neighbours of "+ current.getCoordinate()+" ...");
			for (Node nb : current.getNeighbours(w,false)) {
				tree.initState(nb, goal);
				if(nb.getGScore() > current.getGScore()+current.getCoordinate().distance(nb.getCoordinate())) {
					nb.setGScore(current.getGScore()+current.getCoordinate().distance(nb.getCoordinate()));
					nb.setPreviousNode(current);
					if(open.contains(nb))
						open.remove(nb);
					nb.setFScore(nb.getGScore()+nb.getHScore());
					open.add(nb);
				}
			}
		}
		return false;
	}
	
	private LemmingPath stepPath(WorldCoordinate start, WorldCoordinate goal, World w) {
		if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Stepping to next node...");
		Node startNode = nodes.get(start);
		
		if(PathUtilities.h(startNode.getCoordinate(), goal) <= tree.getHmax(tree.getId(startNode))) {
			if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "This node "+startNode.getCoordinate()+" is a non-goal state in reusable tree.");
			buildPath(startNode, goal);
			
			startNode = tree.getParent(startNode);
			
			if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Sensing the blocking of bricks at the new node...");
			
			List<WorldCoordinate> nbs = startNode.getNeighbouringCoords(w, true);
			Log.d("PATHFINDER", "New node has "+nbs.size()+" neighbours");
			WorldCoordinate[] pureNeighboursCoords = new WorldCoordinate[]{
					startNode.getCoordinate().getLeft(),
					startNode.getCoordinate().getRight(),
					startNode.getCoordinate().getTop(),
					startNode.getCoordinate().getBottom()};
			Node[] pureNeighbourNodes = new Node[]{
					startNode.left,
					startNode.right,
					startNode.top,
					startNode.bottom};
			for (int i = 0; i < pureNeighboursCoords.length; i++) 
			{
				WorldCoordinate nbCoord = pureNeighboursCoords[i];
				Node nb = pureNeighbourNodes[i];
				if (nb != null && !nbs.contains(nbCoord)) {
					if(i == 0) {
						Log.d("PATHFINDER", "Left neighbour set to null");
						nb.right = null;
						startNode.left = null;
					} else if(i == 1) {
						Log.d("PATHFINDER", "Right neighbour set to null");
						nb.left = null;
						startNode.right = null;
					} else if(i == 2) {
						Log.d("PATHFINDER", "Bottom neighbour set to null");
						nb.bottom = null;
						startNode.top = null;
					} else if(i == 3) {
						Log.d("PATHFINDER", "Top neighbour set to null");
						nb.top = null;
						startNode.bottom = null;
					}
					if(tree.getParent(startNode).equals(nb))
						tree.removePaths(startNode, start, goal);
				} else if(nb == null && nbs.contains(nbCoord)) {
					if(i == 0) {
						nodes.get(nbCoord).right = startNode;
						startNode.left = nodes.get(nbCoord);
					} else if(i == 1) {
						nodes.get(nbCoord).left = startNode;
						startNode.right = nodes.get(nbCoord);
					} else if(i == 2) {
						nodes.get(nbCoord).bottom = startNode;
						startNode.top = nodes.get(nbCoord);
					} else if(i == 3) {
						nodes.get(nbCoord).top = startNode;
						startNode.bottom = nodes.get(nbCoord);
					}
					if(tree.getParent(startNode).equals(nodes.get(nbCoord)))
						tree.removePaths(startNode, start, goal);
				}
			}
		} else {
			if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "This node "+startNode.getCoordinate()+" is a goal state or is not in reusable tree.");
			if(start.equals(goal)) {
				if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "This node "+startNode.getCoordinate()+" is a goal state.");
				path = new LemmingPath();
				path.add(goal);
			} else {
				if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "This node "+startNode.getCoordinate()+" is a not in the reusable tree.");
				tree.incSearchCount();
				if(!performNewSearch(start, goal, w)) return null;
				return stepPath(start,goal,w);
			}
		}
		return path;
	}
	
	private boolean performNewSearch(final WorldCoordinate start, final WorldCoordinate goal, World w) {
		if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Starting new search...");
		Node startNode = nodes.get(start);
		Log.d("PATHFINDER", "Init start "+start+"...");
		tree.initState(startNode, goal);
		startNode.setGScore(0);
		open = new PriorityQueue<Node>();
		closed = new ArrayList<Node>();
		startNode.setFScore(startNode.getGScore()+startNode.getHScore());
		open.add(startNode);
		if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER","Starting path computation...");
		if(!computePath(start, goal, w)) return false;
		if(path == null) buildPath(startNode, goal);
		return true;
	}
	
	public LemmingPath findPath2(final WorldCoordinate start, final WorldCoordinate goal, World w) {
		long startTime = System.nanoTime();
		if(path != null && path.get(0).equals(start)) return path;
		if(path == null) performNewSearch(start, goal, w);
		else stepPath(start, goal, w);
		Log.d("PATHFINDER", "New path has size: "+path.size());
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Lemming path found in "+(System.nanoTime()-startTime)/1000000L+"ms");
		return path;
	}
	
	private void buildPath(Node startNode, WorldCoordinate goal) {
		if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Building new resulting path from "+startNode.getCoordinate()+".");
		path = new LemmingPath();
		Node current = startNode;
		while(PathUtilities.h(current.getCoordinate(), goal) <= tree.getHmax(tree.getId(current))) {
			if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Current path node "+current.getCoordinate()+".");
			path.add(current.getCoordinate());
			current = tree.getParent(current);
		}
		path.add(current.getCoordinate());
	}
	
	public class Node implements Comparable<Node>{
		private WorldCoordinate wc;
		private Node left;
		private Node top;
		private Node right;
		private Node bottom;
		private float f;
		private float g;
		private float h;
		private Node previousNode;
		
		public Node(WorldCoordinate wc) {
			this.wc = wc;
			f = 0;
			g = 0;
			previousNode = null;
		}
		
		public void setFScore(float f) {
			this.f = f;
		}
		
		public void setGScore(float g) {
			this.g = g;
		}
		
		public void setHScore(float h) {
			this.h = h;
		}
		
		public float getFScore() {
			return f;
		}
		
		public float getGScore() {
			return g;
		}
		
		public float getHScore() {
			return h;
		}
		
		public WorldCoordinate getCoordinate() {
			return wc;
		}
		
		public List<Node> getNeighbours(World w, boolean checkBricks) {
			List<Node> result = new ArrayList<Node>();
			
			if(left != null && (!checkBricks || w.getNode(wc).getLeft() != null)) result.add(left);
			if(right != null && (!checkBricks || w.getNode(wc).getRight() != null)) result.add(right);
			if(top != null && (!checkBricks || w.getNode(wc).getTop() != null)) result.add(top);
			if(bottom != null && (!checkBricks || w.getNode(wc).getBottom() != null)) result.add(bottom);
			
			return result;
		}
		
		public List<WorldCoordinate> getNeighbouringCoords(World w, boolean checkBricks) {
			List<WorldCoordinate> result = new ArrayList<WorldCoordinate>();
			
			if(left != null && (!checkBricks || w.getNode(wc).getLeft() != null)) result.add(left.getCoordinate());
			if(right != null && (!checkBricks || w.getNode(wc).getRight() != null)) result.add(right.getCoordinate());
			if(top != null && (!checkBricks || w.getNode(wc).getTop() != null)) result.add(top.getCoordinate());
			if(bottom != null && (!checkBricks || w.getNode(wc).getBottom() != null)) result.add(bottom.getCoordinate());
			
			return result;
		}
		
		public void setPreviousNode(Node n) {
			previousNode = n;
		}
		
		public Node getPreviousNode() {
			return previousNode;
		}
		
		@Override
		public int hashCode() {
			return wc.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			return o != null && o.getClass() == this.getClass() && ((Node)o).getCoordinate().equals(wc);
		}
		
		@Override
		public int compareTo(Node other) {
			return Float.compare(this.getFScore(), other.getFScore());
		}
	}
}
