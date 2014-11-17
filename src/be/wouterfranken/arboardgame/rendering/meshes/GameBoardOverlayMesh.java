/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States 
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package be.wouterfranken.arboardgame.rendering.meshes;

import java.nio.Buffer;

import be.wouterfranken.arboardgame.utilities.DebugUtilities;

public class GameBoardOverlayMesh extends MeshObject
{
    
    private Buffer mVertBuff;
    
    private int verticesNumber = 0;
    
    
    public GameBoardOverlayMesh(float[] targetSize)
    {
        setVerts(targetSize);
        setDebugMesh(true);
    }
    
    
    private void setVerts(float[] targetSize)
    {
    	float[] vertices = new float[]{-targetSize[0]/2,targetSize[1]/2,0,
					 targetSize[0]/2,targetSize[1]/2,0,
					 -targetSize[0]/2,-targetSize[1]/2,0,
					targetSize[0]/2,-targetSize[1]/2,0};
    	
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
