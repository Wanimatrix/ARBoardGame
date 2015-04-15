#pragma version(1)
#pragma rs java_package_name(be.wouterfranken.arboardgame.rendering.tracking)

#define MSG_TAG "RSCountNonZero"

rs_script gScript;
rs_allocation gInput;
rs_allocation gOut;

int size = 0;
int count = 0;

int32_t *out; 

void root(const uchar *a) {
	if(count != 0) return;
	count++;

	int tmp = 0;

    for(int i = 0; i < size; i++){
        if(a[i] != 0) { 
        	rsDebug("PixelValue & index", a[i], i);
        	tmp++;
        }
    }
    
    rsAtomicAdd(&out[0], tmp);
}

void init(){
    rsDebug("Called init", rsUptimeMillis());
}