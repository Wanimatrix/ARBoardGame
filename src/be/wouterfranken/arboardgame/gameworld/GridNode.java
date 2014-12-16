package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.List;

public class GridNode{
	private WorldCoordinate coord = null;
	private GridNode left = null;
	private GridNode right = null;
	private GridNode top = null;
	private GridNode bottom = null;
	private boolean accessible = true;
	
	public GridNode(WorldCoordinate coord) {
		this.coord = coord;
	}
	
	public WorldCoordinate getCoordinate() {
		return coord;
	}
	
	public List<GridNode> getNeighbours() {
		List<GridNode> neighbours = new ArrayList<GridNode>();
		if(left != null) neighbours.add(left);
		if(right != null) neighbours.add(right);
		if(top != null) neighbours.add(top);
		if(bottom != null) neighbours.add(bottom);
		return neighbours;
	}
	
	public GridNode getBottom() {
		return bottom;
	}
	
	public GridNode getLeft() {
		return left;
	}
	
	public GridNode getRight() {
		return right;
	}
	
	public GridNode getTop() {
		return top;
	}
	
	public void setBottom(GridNode bottom) {
		this.bottom = bottom;
	}
	
	public void setLeft(GridNode left) {
		this.left = left;
	}
	
	public void setRight(GridNode right) {
		this.right = right;
	}
	
	public void setTop(GridNode top) {
		this.top = top;
	}
	
	public boolean isAccessible() {
		return accessible;
	}
	
	public void setAccessible(boolean accessible) {
		this.accessible = accessible;
	}
	
	@Override
	public int hashCode() {
		return coord.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		return this == o || this.hashCode() == o.hashCode();
	}
}
