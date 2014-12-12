package be.wouterfranken.arboardgame.rendering.meshes;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public abstract class MeshObject
{
	private boolean isDebugMesh = false;
	
	// RenderOptions
	private final RenderOptions ro;
	private int[] multiRenderConfig = new int[]{0};
	
	public MeshObject(RenderOptions ro) {
		this.ro = ro;
	}
	
	public int[] getMultiRenderConfiguration() {
		return multiRenderConfig;
	}
	
	protected void setMultiRenderConfiguration(int[] multiRenderConfig) {
		this.multiRenderConfig = multiRenderConfig;
	}
	
    public enum BUFFER_TYPE
    {
        BUFFER_TYPE_VERTEX, BUFFER_TYPE_TEXTURE_COORD, BUFFER_TYPE_NORMALS, BUFFER_TYPE_INDICES, BUFFER_TYPE_SHADOW_VERTEX
    }
    
    
    public Buffer getVertices()
    {
        return getBuffer(BUFFER_TYPE.BUFFER_TYPE_VERTEX);
    }
    
    
    public Buffer getTexCoords()
    {
        return getBuffer(BUFFER_TYPE.BUFFER_TYPE_TEXTURE_COORD);
    }
    
    
    public Buffer getNormals()
    {
        return getBuffer(BUFFER_TYPE.BUFFER_TYPE_NORMALS);
    }
    
    
    public Buffer getIndices()
    {
        return getBuffer(BUFFER_TYPE.BUFFER_TYPE_INDICES);
    }
    
    public Buffer getShadowVertices()
    {
        return getBuffer(BUFFER_TYPE.BUFFER_TYPE_SHADOW_VERTEX);
    }
    
    public RenderOptions getRenderOptions() {
    	return ro;
    }
    
    protected Buffer fillBuffer(double[] array)
    {
        // Convert to floats because OpenGL doesn't work on doubles, and manually
        // casting each input value would take too much time.
        // Each float takes 4 bytes
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : array)
            bb.putFloat((float) d);
        bb.rewind();
        
        return bb;
        
    }
    
    
    protected Buffer fillBuffer(float[] array)
    {
        // Each float takes 4 bytes
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array)
            bb.putFloat(d);
        bb.rewind();
        
        return bb;
        
    }
    
    
    protected Buffer fillBuffer(short[] array)
    {
        // Each short takes 2 bytes
        ByteBuffer bb = ByteBuffer.allocateDirect(2 * array.length);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (short s : array)
            bb.putShort(s);
        bb.rewind();
        
        return bb;
        
    }
    
    
    public abstract Buffer getBuffer(BUFFER_TYPE bufferType);
    
    
    public abstract int getNumObjectVertex();
    
    
    public abstract int getNumObjectIndex();
    
    public void setDebugMesh(boolean isDebugMesh) {
		this.isDebugMesh = isDebugMesh;
	}
    
    public boolean isDebugMesh() {
		return isDebugMesh;
	}
    
}
