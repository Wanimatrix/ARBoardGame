package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;

public class PathFinderOrig1 {
	private static final String TAG = Pathfinder.class.getSimpleName();
	
	private static class Node implements Comparable<Node>{
		private WorldCoordinate wc;
		private float f;
		private float g;
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
		
		public float getFScore() {
			return f;
		}
		
		public float getGScore() {
			return g;
		}
		
		public WorldCoordinate getCoordinate() {
			return wc;
		}
		
		public List<Node> getNeighbours(World w) {
			List<Node> result = new ArrayList<Node>();
			WorldNode currentWNode = w.getNode(wc);
			if(wc.getLeft() != null && currentWNode.getLeft() != null) result.add(new Node(wc.getLeft()));
			if(wc.getRight() != null && currentWNode.getRight() != null) result.add(new Node(wc.getRight()));
			if(wc.getTop() != null && currentWNode.getTop() != null) result.add(new Node(wc.getTop()));
			if(wc.getBottom() != null && currentWNode.getBottom() != null) result.add(new Node(wc.getBottom()));
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
			return o.getClass() == this.getClass() && ((Node)o).getCoordinate().equals(wc);
		}
		
		@Override
		public int compareTo(Node other) {
			return Float.compare(this.getFScore(), other.getFScore());
		}
	}
	
	public static List<WorldCoordinate> findPath(final WorldCoordinate start, final WorldCoordinate target, World w) {
		long startTime = System.nanoTime();
		
		Map<WorldCoordinate,Node> closed = new HashMap<WorldCoordinate,Node>();
		PriorityQueue<Node> open = new PriorityQueue<Node>();
		Map<WorldCoordinate, Node> openNodes = new HashMap<WorldCoordinate, PathFinderOrig1.Node>();
		Node current = null;
		Node startNode = new Node(start);
		startNode.f = start.distance(target);
		startNode.g = 0;
		open.add(startNode);
		openNodes.put(start, startNode);
		
		while(!open.isEmpty()) {
			current = open.remove();
			if(current != null) {
				if(current.getCoordinate().equals(target))
					break;
				
				closed.put(current.getCoordinate(), current);
				for (Node nb : current.getNeighbours(w)) {
					if(closed.containsKey(nb.getCoordinate()))
						continue;
					float g = current.getGScore() + current.getCoordinate().distance(nb.getCoordinate());
					if(!open.contains(nb)) {
						nb.setPreviousNode(current);
						nb.setGScore(g);
						nb.setFScore(g+Math.abs(nb.getCoordinate().x-target.x)+Math.abs(nb.getCoordinate().y-target.y));
						open.add(nb);
						openNodes.put(nb.getCoordinate(), nb);
					} else if(g < openNodes.get(nb.getCoordinate()).getGScore()) {
						openNodes.get(nb.getCoordinate()).setPreviousNode(current);
						openNodes.get(nb.getCoordinate()).setGScore(g);
						openNodes.get(nb.getCoordinate()).setFScore(g+nb.getCoordinate().distance(target));
					}
				}
			}
		}
		
		if(!current.getCoordinate().equals(target)) return null;
		
		List<WorldCoordinate> path = new ArrayList<WorldCoordinate>();
		while(current != null) {
			path.add(current.getCoordinate());
			current = current.getPreviousNode();
		}
		Collections.reverse(path);
		
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Lemming path found in "+(System.nanoTime()-startTime)/1000000L+"ms");
		
		return path;
	}
}