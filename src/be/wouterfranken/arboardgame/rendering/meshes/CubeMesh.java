package be.wouterfranken.arboardgame.rendering.meshes;

import java.nio.Buffer;

import org.opencv.core.Point3;

import be.wouterfranken.arboardgame.gameworld.WorldCoordinate;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject.BUFFER_TYPE;

public class CubeMesh extends MeshObject {

	private Buffer mVertBuff;
    private int verticesNumber = 0;
    
    
    public CubeMesh(float size, float locationX, float locationY, float height, RenderOptions ro)
    {
    	super(ro);
        setVerts(size, locationX, locationY, height);
        setDebugMesh(true);
        
    }
    
    private void setVerts(float size, float locationX, float locationY, float height)
    {
    	float[] locationFloat = new float[]{locationX,locationY,(float) height};
    	float[] vertices = new float[]{
    		locationFloat[0]-size/2, locationFloat[1]-size/2, locationFloat[2]+size/2, // 0. left-bottom-front
    		locationFloat[0]+size/2, locationFloat[1]-size/2, locationFloat[2]+size/2, // 1. right-bottom-front
    		locationFloat[0]-size/2, locationFloat[1]+size/2, locationFloat[2]+size/2, // 2. left-top-front
    		locationFloat[0]+size/2, locationFloat[1]+size/2, locationFloat[2]+size/2, // 3. right-top-front
			// BACK
    		locationFloat[0]+size/2, locationFloat[1]-size/2, locationFloat[2]-size/2, // 6. right-bottom-back
    		locationFloat[0]-size/2, locationFloat[1]-size/2, locationFloat[2]-size/2, // 4. left-bottom-back
    		locationFloat[0]+size/2, locationFloat[1]-size/2, locationFloat[2]-size/2, // 7. right-top-back
    		locationFloat[0]-size/2, locationFloat[1]+size/2, locationFloat[2]-size/2, // 5. left-top-back
			// LEFT
    		locationFloat[0]-size/2, locationFloat[1]-size/2, locationFloat[2]-size/2, // 4. left-bottom-back
    		locationFloat[0]-size/2, locationFloat[1]-size/2, locationFloat[2]+size/2, // 0. left-bottom-front
    		locationFloat[0]-size/2, locationFloat[1]+size/2, locationFloat[2]-size/2, // 5. left-top-back
    		locationFloat[0]-size/2, locationFloat[1]+size/2, locationFloat[2]+size/2, // 2. left-top-front
			// RIGHT
    		locationFloat[0]+size/2, locationFloat[1]-size/2, locationFloat[2]+size/2, // 1. right-bottom-front
    		locationFloat[0]+size/2, locationFloat[1]-size/2, locationFloat[2]-size/2, // 6. right-bottom-back
    		locationFloat[0]+size/2, locationFloat[1]+size/2, locationFloat[2]+size/2, // 3. right-top-front
    		locationFloat[0]+size/2, locationFloat[1]+size/2, locationFloat[2]-size/2, // 7. right-top-back
			// TOP
    		locationFloat[0]-size/2, locationFloat[1]+size/2, locationFloat[2]+size/2, // 2. left-top-front
    		locationFloat[0]+size/2, locationFloat[1]+size/2, locationFloat[2]+size/2, // 3. right-top-front
    		locationFloat[0]-size/2, locationFloat[1]+size/2, locationFloat[2]-size/2, // 5. left-top-back
    		locationFloat[0]+size/2, locationFloat[1]+size/2, locationFloat[2]-size/2, // 7. right-top-back
			// BOTTOM
    		locationFloat[0]-size/2, locationFloat[1]-size/2, locationFloat[2]-size/2, // 4. left-bottom-back
    		locationFloat[0]+size/2, locationFloat[1]-size/2, locationFloat[2]-size/2, // 6. right-bottom-back
			locationFloat[0]-size/2, locationFloat[1]-size/2, locationFloat[2]+size/2, // 0. left-bottom-front
			locationFloat[0]+size/2, locationFloat[1]-size/2, locationFloat[2]+size/2 // 1. right-bottom-front
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
