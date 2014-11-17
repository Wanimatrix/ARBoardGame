#define BLK_SIZE    16
//prefix D for decoding
#define DSHRD_LEN   (BLK_SIZE/2)
#define DSHRD_SIZE  (2*DSHRD_LEN*DSHRD_LEN)

uchar4 convertYVUtoRGBA(int y, int u, int v)
{
    uchar4 ret;
    y-=16;
    u-=128;
    v-=128;
    int b = y + (int)(1.772f*u);
    int g = y - (int)(0.344f*u + 0.714f*v);
    int r = y + (int)(1.402f*v);
    ret.x = r>255? 255 : r<0 ? 0 : r;
    ret.y = g>255? 255 : g<0 ? 0 : g;
    ret.z = b>255? 255 : b<0 ? 0 : b;
    ret.w = 255;
    return ret;
}

__kernel void nv21torgba( __global uchar4* out,
                         __global uchar*  in,
                         int    im_width,
                         int    im_height)
{
    __local uchar uvShrd[DSHRD_SIZE];
    int gx	= get_global_id(0);
    int gy	= get_global_id(1);
    int lx  = get_local_id(0);
    int ly  = get_local_id(1);
    int off = im_width*im_height;
    // every thread whose
    // both x,y indices are divisible
    // by 2 move the u,v corresponding
    // to the 2x2 block into shared mem
    int inIdx= gy*im_width+gx;
    int uvIdx= off + (gy/2)*im_width + (gx & ~1);
    int shlx = lx/2;
    int shly = ly/2;
    int shIdx= 2*(shlx+shly*DSHRD_LEN);
    if( gx%2==0 && gy%2==0 ) {
        uvShrd[shIdx+0] = in[uvIdx+0];
        uvShrd[shIdx+1] = in[uvIdx+1];
    }
    // do some work while others copy
    // uv to shared memory
    int y   = (0xFF & ((int)in[inIdx]));
    if( y < 16 ) y=16;
    barrier(CLK_LOCAL_MEM_FENCE);
    // return invalid threads
    if( gx >= im_width || gy >= im_height )
        return;
    // convert color space
    int v   = (0xFF & ((int)uvShrd[shIdx+0]));
    int u   = (0xFF & ((int)uvShrd[shIdx+1]));
    // write output to image
    out[inIdx]  = convertYVUtoRGBA(y,u,v);
}