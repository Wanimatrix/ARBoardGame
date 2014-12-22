package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import be.wouterfranken.arboardgame.gameworld.Pathfinder.PathNode;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

public class ReusableTree {
	private Map<PathNode, Pair<Integer,PathNode>> perState = new HashMap<PathNode, Pair<Integer,PathNode>>();
	private SparseArray<Pair<float[], Set<Integer>>> perPath = new SparseArray<Pair<float[],Set<Integer>>>();
	private Map<PathNode, Integer> generated = new HashMap<PathNode, Integer>();
	private int searchCount = -1;
	
	public Integer getId(PathNode n) {
		if(perState.get(n) == null) return 0;
		return perState.get(n).first;
	}
	
	public void setId(PathNode n, int newId) {
		Pair<Integer,PathNode> stateVars = perState.get(n);
		if(stateVars == null)
			perState.put(n,new Pair<Integer, PathNode>(newId,null));
		else
			perState.put(n,new Pair<Integer, PathNode>(newId,stateVars.second));
	}
	
	public PathNode getParent(PathNode n) {
		return perState.get(n).second;
	}
	
	public void setParent(PathNode n, PathNode newParent) {
		Pair<Integer,PathNode> stateVars = perState.get(n);
		if(stateVars == null)
			perState.put(n,new Pair<Integer, PathNode>(0,newParent));
		else 
			perState.put(n,new Pair<Integer, PathNode>(stateVars.first,newParent));
	}
	
	public int getGenerated(PathNode state) {
		Integer gen = generated.get(state);
		if(gen == null) {
			Log.d("PATHFINDER", "Generated: 0");
			return 0;
		}
		else {
			Log.d("PATHFINDER", "Generated: "+gen);
			return gen;
		}
	}
	
	public void setGenerated(PathNode n, int generated) {
		this.generated.put(n, generated);
	}
	
	public float getHmax(Integer pathId) {
		if(pathId == 0) return -1;
		return perPath.get(pathId).first[1];
	}
	
	public void setHmax(Integer pathId, float newHmax) {
		Pair<float[], Set<Integer>> pathVars = perPath.get(pathId);
		if(pathVars == null)
			perPath.put(pathId,new Pair<float[], Set<Integer>>(new float[]{-1,newHmax},new HashSet<Integer>()));
		else
			perPath.put(pathId,new Pair<float[], Set<Integer>>(new float[]{pathVars.first[0],newHmax},pathVars.second));
	}
	
	public float getHmin(Integer pathId) {
		return perPath.get(pathId).first[0];
	}
	
	public void setHmin(Integer pathId, float newHmin) {
		Pair<float[], Set<Integer>> pathVars = perPath.get(pathId);
		if(pathVars == null)
			perPath.put(pathId,new Pair<float[], Set<Integer>>(new float[]{newHmin,-1},new HashSet<Integer>()));
		else
			perPath.put(pathId,new Pair<float[], Set<Integer>>(new float[]{newHmin,pathVars.first[1]},pathVars.second));
	}
	
	public Set<Integer> getPaths(Integer pathId) {
		return perPath.get(pathId).second;
	}
	
	public void initSearchCount() {
		searchCount = 1;
	}
	
	public void incSearchCount() {
		searchCount++;
	}
	
	private int getSearchCount() {
		Log.d("PATHFINDER", "SearchCount: "+searchCount);
		return searchCount;
	}
	
//	private void addNode(PathNode n) {
//		this.perState.put(n, new Pair<Integer, PathNode>(0, null));
//		setGenerated(n, searchCount);
//	}
	
	public void initState(PathNode n, WorldCoordinate goal) {
		Log.d("PATHFINDER", "NodeCoord: "+n.accessGridNode().getCoordinate());
		if(this.getGenerated(n) == 0) {
			n.setGScore(Float.POSITIVE_INFINITY);
			n.setHScore(PathUtilities.h(n.accessGridNode().getCoordinate(),goal));
		} else if(this.getGenerated(n) != this.getSearchCount()) {
			n.setGScore(Float.POSITIVE_INFINITY);
		}
		Log.d("PATHFINDER", "SearchCount: "+searchCount);
		setGenerated(n, searchCount);
		Log.d("PATHFINDER", "New generated: "+this.getGenerated(n));
	}
	
	public void addPath(PathNode s, PathNode start, WorldCoordinate goal) {
		if(!s.accessGridNode().getCoordinate().equals(goal)) {
			getPaths(getId(s)).add(searchCount);
		}
		perPath.put(searchCount, new Pair<float[], Set<Integer>>(new float[]{s.getHScore(),start.getHScore()}, new HashSet<Integer>()));
		while(!s.equals(start)) {
			PathNode sTmp = s;
			s = s.getPreviousNode();
//			Log.d("PATHFINDER", "Current Node: "+s.getCoordinate());
			setId(s, searchCount);
			setParent(s, sTmp);
			Log.d("PATHFINDER", "Node "+s.accessGridNode().getCoordinate()+" has new parent "+sTmp.accessGridNode().getCoordinate());
		}
	}
	
	public void removePaths(PathNode s) {
		Log.d("PATHFINDER", "Removing paths ... ");
		int x = getId(s);
		if(getHmax(x) > getParent(s).getHScore())
			setHmax(x, getParent(s).getHScore());
		List<Integer> q = new ArrayList<Integer>();
		for (Integer x2 : getPaths(x)) {
			if(getHmax(x) < getHmin(x)) {
				q.add(x2);
				getPaths(x).remove(x2);
			}
		}
		
		while(!q.isEmpty()) {
			Integer first = q.get(0);
			if(getHmax(first) > getHmin(first)) {
				setHmax(first, getHmin(first));
				for (Integer first2 : getPaths(first)) {
					q.add(first2);
					getPaths(first).remove(first2);
				}
			}
		}
	}
}
