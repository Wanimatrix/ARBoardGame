//package be.wouterfranken.arboardgame.rendering.meshes;
//
//import java.nio.Buffer;
//
//import org.opencv.core.Point3;
//
//import be.wouterfranken.arboardgame.rendering.meshes.MeshObject.BUFFER_TYPE;
//
//public class BrickMesh extends MeshObject {
//
//	private Buffer mVertBuff;
//    private int verticesNumber = 0;
//    public BrickMesh(Point3[] corners, RenderOptions ro)
//    {
//    	super(ro);
//        setVerts(corners);
//        setDebugMesh(true);
//        
//    }
//    
//    
//    private void setVerts(Point3[] corners)
//    {
//    	Point3 corner0 = corners[0];
//    	Point3 corner1 = corners[0];
//    	float[] vertices = new float[]{(float) corner0.x,(float) corner0.y,(float) corner0.z,
//    								   (float) corner0.x,(float) corner0.y,(float) corner0.z,};
//    	
//    	//DebugUtilities.logGLMatrix("Vertices", vertices, 4, 3);
//    	
//        mVertBuff = fillBuffer(vertices);
//        verticesNumber = vertices.length / 3;
//    }
//    
//    
//    @Override
//    public int getNumObjectVertex()
//    {
//        return verticesNumber;
//    }
//    
//    @Override
//	public int getNumObjectIndex() {
//		return verticesNumber;
//	}
//    
//    
//    @Override
//    public Buffer getBuffer(BUFFER_TYPE bufferType)
//    {
//        Buffer result = null;
//        switch (bufferType)
//        {
//            case BUFFER_TYPE_VERTEX:
//                result = mVertBuff;
//                break;
//            case BUFFER_TYPE_TEXTURE_COORD:
//                break;
//            case BUFFER_TYPE_NORMALS:
//                break;
//            case BUFFER_TYPE_INDICES:
//            default:
//                break;
//        
//        }
//        
//        return result;
//    }
//
//}
