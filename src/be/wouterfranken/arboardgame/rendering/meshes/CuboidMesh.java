package be.wouterfranken.arboardgame.rendering.meshes;

import java.nio.Buffer;

public class CuboidMesh extends MeshObject {

	private Buffer mVertBuff;
    private int verticesNumber = 0;
    
    
    public CuboidMesh(float sizeX, float sizeY, float sizeZ, float locationX, float locationY, float height, RenderOptions ro)
    {
    	super(ro);
        setVerts(sizeX, sizeY, sizeZ, locationX, locationY, height);
        setDebugMesh(true);
    }
    
    public CuboidMesh(float[][] points, RenderOptions ro)
    {
    	super(ro);
        setVerts(points);
        setDebugMesh(true);
    }
    
    private void setVerts(float[][] points)
    {
    	float[] vertices = new float[]{
    		// TOP
            points[0][0],points[0][1],points[0][2], 
            points[1][0],points[1][1],points[1][2], 
            points[3][0],points[3][1],points[3][2],
            points[2][0],points[2][1],points[2][2], 
            // BOTTOM
            points[4][0],points[4][1],points[4][2], 
            points[5][0],points[5][1],points[5][2], 
            points[7][0],points[7][1],points[7][2], 
            points[6][0],points[6][1],points[6][2], 
            // SIDE1
            points[0][0],points[0][1],points[0][2], 
            points[1][0],points[1][1],points[1][2], 
            points[4][0],points[4][1],points[4][2], 
            points[5][0],points[5][1],points[5][2], 
            // SIDE2
            points[2][0],points[2][1],points[2][2], 
            points[3][0],points[3][1],points[3][2], 
            points[6][0],points[6][1],points[6][2], 
            points[7][0],points[7][1],points[7][2], 
            // SIDE3
            points[1][0],points[1][1],points[1][2], 
            points[2][0],points[2][1],points[2][2], 
            points[5][0],points[5][1],points[5][2], 
            points[6][0],points[6][1],points[6][2], 
            // SIDE4
            points[3][0],points[3][1],points[3][2], 
            points[0][0],points[0][1],points[0][2], 
            points[7][0],points[7][1],points[7][2], 
            points[4][0],points[4][1],points[4][2], 
        };
    	this.setMultiRenderConfiguration(new int[]{0,4,8,12,16,20});
    	
        mVertBuff = fillBuffer(vertices);
        verticesNumber = vertices.length / 3;
    }
    
    private void setVerts(float sizeX, float sizeY, float sizeZ, float locationX, float locationY, float height)
    {
    	float[] locationFloat = new float[]{locationX,locationY,(float) height};
    	float[] vertices = new float[]{
            locationFloat[0]-sizeX/2, locationFloat[1]-sizeY/2, locationFloat[2]+sizeZ/2, // 0. left-bottom-front
            locationFloat[0]+sizeX/2, locationFloat[1]-sizeY/2, locationFloat[2]+sizeZ/2, // 1. right-bottom-front
            locationFloat[0]-sizeX/2, locationFloat[1]+sizeY/2, locationFloat[2]+sizeZ/2, // 2. left-top-front
            locationFloat[0]+sizeX/2, locationFloat[1]+sizeY/2, locationFloat[2]+sizeZ/2, // 3. right-top-front
            // BACK
            locationFloat[0]+sizeX/2, locationFloat[1]-sizeY/2, locationFloat[2]-sizeZ/2, // 6. right-bottom-back
            locationFloat[0]-sizeX/2, locationFloat[1]-sizeY/2, locationFloat[2]-sizeZ/2, // 4. left-bottom-back
            locationFloat[0]+sizeX/2, locationFloat[1]-sizeY/2, locationFloat[2]-sizeZ/2, // 7. right-top-back
            locationFloat[0]-sizeX/2, locationFloat[1]+sizeY/2, locationFloat[2]-sizeZ/2, // 5. left-top-back
            // LEFT
            locationFloat[0]-sizeX/2, locationFloat[1]-sizeY/2, locationFloat[2]-sizeZ/2, // 4. left-bottom-back
            locationFloat[0]-sizeX/2, locationFloat[1]-sizeY/2, locationFloat[2]+sizeZ/2, // 0. left-bottom-front
            locationFloat[0]-sizeX/2, locationFloat[1]+sizeY/2, locationFloat[2]-sizeZ/2, // 5. left-top-back
            locationFloat[0]-sizeX/2, locationFloat[1]+sizeY/2, locationFloat[2]+sizeZ/2, // 2. left-top-front
            // RIGHT
            locationFloat[0]+sizeX/2, locationFloat[1]-sizeY/2, locationFloat[2]+sizeZ/2, // 1. right-bottom-front
            locationFloat[0]+sizeX/2, locationFloat[1]-sizeY/2, locationFloat[2]-sizeZ/2, // 6. right-bottom-back
            locationFloat[0]+sizeX/2, locationFloat[1]+sizeY/2, locationFloat[2]+sizeZ/2, // 3. right-top-front
            locationFloat[0]+sizeX/2, locationFloat[1]+sizeY/2, locationFloat[2]-sizeZ/2, // 7. right-top-back
            // TOP
            locationFloat[0]-sizeX/2, locationFloat[1]+sizeY/2, locationFloat[2]+sizeZ/2, // 2. left-top-front
            locationFloat[0]+sizeX/2, locationFloat[1]+sizeY/2, locationFloat[2]+sizeZ/2, // 3. right-top-front
            locationFloat[0]-sizeX/2, locationFloat[1]+sizeY/2, locationFloat[2]-sizeZ/2, // 5. left-top-back
            locationFloat[0]+sizeX/2, locationFloat[1]+sizeY/2, locationFloat[2]-sizeZ/2, // 7. right-top-back
            // BOTTOM
            locationFloat[0]-sizeX/2, locationFloat[1]-sizeY/2, locationFloat[2]-sizeZ/2, // 4. left-bottom-back
            locationFloat[0]+sizeX/2, locationFloat[1]-sizeY/2, locationFloat[2]-sizeZ/2, // 6. right-bottom-back
            locationFloat[0]-sizeX/2, locationFloat[1]-sizeY/2, locationFloat[2]+sizeZ/2, // 0. left-bottom-front
            locationFloat[0]+sizeX/2, locationFloat[1]-sizeY/2, locationFloat[2]+sizeZ/2 // 1. right-bottom-front
        };
    	this.setMultiRenderConfiguration(new int[]{0,4,8,12,16,20});
    	
    	//DebugUtilities.logGLMatrix("Vertices", vertices, 4, 3);
    	
        mVertBuff = fillBuffer(vertices);
        verticesNumber = vertices.length / 3;
    }
    
  
    
    
    @Override
    public int getNumObjectVertex()
    {
        return verticesNumber;
    }
    
    @Override
	public int getNumObjectIndex() {
		return verticesNumber;
	}
    
    
    @Override
    public Buffer getBuffer(BUFFER_TYPE bufferType)
    {
        Buffer result = null;
        switch (bufferType)
        {
            case BUFFER_TYPE_VERTEX:
                result = mVertBuff;
                break;
            case BUFFER_TYPE_TEXTURE_COORD:
                break;
            case BUFFER_TYPE_NORMALS:
                break;
            case BUFFER_TYPE_INDICES:
            default:
                break;
        
        }
        
        return result;
    }

}
