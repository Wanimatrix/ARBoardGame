package be.wouterfranken.arboardgame.rendering.meshes;

import java.nio.Buffer;

public class FullSquadMesh extends MeshObject {

	private Buffer mVertBuff;
    private Buffer mTexCoordBuff;
    
    private int verticesNumber = 0;
    
    
    public FullSquadMesh()
    {
    	super(null); // This class is only used to render the camera correctly, 'RenderOptions' for this are HARD-CODED in the Render classes.
        setVerts();
        setTexCoords();
    }
    
    
    private void setVerts()
    {
    	float[] vertices = new float[]{
    			-1f, 1f,
	            1f, 1f,
	            -1f, -1f,
	            1f, -1f};
        mVertBuff = fillBuffer(vertices);
        verticesNumber = vertices.length / 2;
    }
    
    private void setTexCoords()
    {
    	float[] texCoords = new float[]{0,1,
					 					1,1,
					 					0,0,
					 					1,0};
        mTexCoordBuff = fillBuffer(texCoords);
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
            	result = mTexCoordBuff;
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
