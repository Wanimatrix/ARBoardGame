#include <math.h>
#include "features.hpp"

#include "logger.hpp"
// #include "mex.h"

#define LOG_TAG "FEATDETECT"

// small value, used to avoid division by zero
#define eps 0.1

//Hardcoded maximum number of levels in the pyramid
#define MAXLEVELS 200
//Hardcoded minimum dimension of smallest (coarsest) pyramid level
#define MINDIMENSION 5

// unit vectors used to compute gradient orientation
double uu[9] = {1.0000,
		0.9397,
		0.7660,
		0.500,
		0.1736,
		-0.1736,
		-0.5000,
		-0.7660,
		-0.9397};
double vv[9] = {0.0000,
		0.3420,
		0.6428,
		0.8660,
		0.9848,
		0.9848,
		0.8660,
		0.6428,
		0.3420};

static inline double min(double x, double y) { return (x <= y ? x : y); }
static inline double max(double x, double y) { return (x <= y ? y : x); }

static inline int min(int x, int y) { return (x <= y ? x : y); }
static inline int max(int x, int y) { return (x <= y ? y : x); }

void reportError(string errorMessage) {
	LOGD("This went wrong: %s\n",errorMessage.c_str());
	exit(1);
}

// main function:
// takes a double color image and a bin size
// returns HOG features
Mat getHogFeatures(Mat image, int sbin) {
  	int dims[3];
	dims[0] = image.rows;
	dims[1] = image.cols;
	dims[2] = image.channels();
	double *im = (double *)image.data;
	if(image.type() != CV_64FC3) reportError("INPUT DIM ERROR");
	if(!image.isContinuous()) reportError("Image is not Continuous!");
//   if (mxGetNumberOfDimensions(mximage) != 3 ||
//       dims[2] != 3 ||
//       mxGetClassID(mximage) != mxDOUBLE_CLASS)
//     mexErrMsgTxt("Invalid input");
//   int sbin = (int)mxGetScalar(mxsbin);

  	// memory for caching orientation histograms & their norms
  	int blocks[2];
  	blocks[0] = (int)floor(0.5+(double)dims[0]/(double)sbin);
  	blocks[1] = (int)floor(0.5+(double)dims[1]/(double)sbin);
	Mat histMat = Mat::zeros(blocks[0],blocks[1],CV_64FC18);
	Mat normMat = Mat::zeros(blocks[0],blocks[1],CV_64FC1);
  	double *hist = (double *)histMat.data;
  	double *norm = (double *)normMat.data;

  	// memory for HOG features
  	int out[3];
  	out[0] = max(blocks[0]-2, 0);
  	out[1] = max(blocks[1]-2, 0);
  	out[2] = 9;
	Mat matFeat = Mat::zeros(out[0],out[1],CV_64FC9);
 //  	mxArray *mxfeat = mxCreateNumericArray(3, out, mxDOUBLE_CLASS, mxREAL);
  	double *feat = (double *)matFeat.data;
	if(!matFeat.isContinuous()) reportError("Feat result is not Continuous!");

	int visible[2];
	visible[0] = blocks[0]*sbin;
  	visible[1] = blocks[1]*sbin;

	LOGD("Image channels: %d\n",image.channels());

  	for (int x = 1; x < visible[1]-1; x++) {
    	for (int y = 1; y < visible[0]-1; y++) {
      		// first color channel
      		double *s = im + min(x, dims[1]-2)*image.channels() + min(y, dims[0]-2)*image.step1();
      		double dy = *(s+dims[1]*3) - *(s-dims[1]*3);
      		double dx = *(s+3) - *(s-3);
      		double v = dx*dx + dy*dy;

			// cout << "First row: ";
			// int k = 0;
			// for(int i =0; i< image.cols;i++) {
			// 	cout << "@(0," << i*3 << "): (" << *(im+i*3) << ", ";
			// 	cout << *(im+i*3+1) << ", ";
			// 	cout << *(im+i*3+2) << "), ";
			// 	k+=3;
			// }
			// cout << endl;
			// cout << "Amount: " << k << endl;
			//
			// cout << "First row: ";
			// k = 0;
			// for(int i =0; i< image.cols;i++) {
			// 	cout << "@(0," << i << "): (" << image.at<Vec3d>(1,i)[0] << ", ";
			// 	cout << image.at<Vec3d>(1,i)[1] << ", ";
			// 	cout << image.at<Vec3d>(1,i)[2] << "), ";
			// 	k+=3;
			// }
			// cout << endl;
			// cout << "Amount: " << k << endl;

			if(*s != image.at<Vec3d>(min(y, dims[0]-2),min(x, dims[1]-2))[0]) {
				reportError("Wrong dimensions! 1");
			}
			if(*(s+3*dims[1]) != image.at<Vec3d>(min(y, dims[0]-2)+1,min(x, dims[1]-2))[0]) {
				reportError("Wrong dimensions! 2");
			}
			if(*(s+3) != image.at<Vec3d>(min(y, dims[0]-2),min(x, dims[1]-2)+1)[0]) {
				reportError("Wrong dimensions! 3");
			}

      		// second color channel
      		s += 1;//dims[0]*dims[1];
      		double dy2 = *(s+3*dims[1]) - *(s-3*dims[1]);
      		double dx2 = *(s+3) - *(s-3);
			double v2 = dx2*dx2 + dy2*dy2;

			if(*s != image.at<Vec3d>(min(y, dims[0]-2),min(x, dims[1]-2))[1]) {
				LOGD("%lf vs %lf\n",*s,image.at<Vec3d>(min(y, dims[0]-2),min(x, dims[1]-2))[1]);
				reportError("Wrong dimensions! 4");
			}
			if(*(s+3*dims[1]) != image.at<Vec3d>(min(y, dims[0]-2)+1,min(x, dims[1]-2))[1]) {
				reportError("Wrong dimensions! 5");
			}
			if(*(s+3) != image.at<Vec3d>(min(y, dims[0]-2),min(x, dims[1]-2)+1)[1]) {
				reportError("Wrong dimensions! 6");
			}

			// third color channel
			s += 1;//dims[0]*dims[1];
			double dy3 = *(s+3*dims[1]) - *(s-3*dims[1]);
			double dx3 = *(s+3) - *(s-3);
			double v3 = dx3*dx3 + dy3*dy3;

			if(*s != image.at<Vec3d>(min(y, dims[0]-2),min(x, dims[1]-2))[2]) {
				reportError("Wrong dimensions! 7");
			}
			if(*(s+3*dims[1]) != image.at<Vec3d>(min(y, dims[0]-2)+1,min(x, dims[1]-2))[2]) {
				reportError("Wrong dimensions! 8");
			}
			if(*(s+3) != image.at<Vec3d>(min(y, dims[0]-2),min(x, dims[1]-2)+1)[2]) {
				reportError("Wrong dimensions! 9");
			}

			// pick channel with strongest gradient
			if (v2 > v) {
				v = v2;
				dx = dx2;
				dy = dy2;
			}
			if (v3 > v) {
				v = v3;
				dx = dx3;
				dy = dy3;
			}

      		// snap to one of 18 orientations
			double best_dot = 0;
			int best_o = 0;
			for (int o = 0; o < 9; o++) {
				double dot = uu[o]*dx + vv[o]*dy;
				if (dot > best_dot) {
				  best_dot = dot;
				  best_o = o;
				} else if (-dot > best_dot) {
				  best_dot = -dot;
				  best_o = o+9;
				}
			}

			// add to 4 histograms around pixel using linear interpolation
			double xp = ((double)x+0.5)/(double)sbin - 0.5;
			double yp = ((double)y+0.5)/(double)sbin - 0.5;
			int ixp = (int)floor(xp);
			int iyp = (int)floor(yp);
			double vx0 = xp-ixp;
			double vy0 = yp-iyp;
			double vx1 = 1.0-vx0;
			double vy1 = 1.0-vy0;
			v = sqrt(v);

			if (ixp >= 0 && iyp >= 0) {
				*(hist + ixp*blocks[0] + iyp + best_o*blocks[0]*blocks[1]) += vx1*vy1*v;
			}

			if (ixp+1 < blocks[1] && iyp >= 0) {
				*(hist + (ixp+1)*blocks[0] + iyp + best_o*blocks[0]*blocks[1]) += vx0*vy1*v;
			}

			if (ixp >= 0 && iyp+1 < blocks[0]) {
				*(hist + ixp*blocks[0] + (iyp+1) + best_o*blocks[0]*blocks[1]) += vx1*vy0*v;
			}

			if (ixp+1 < blocks[1] && iyp+1 < blocks[0]) {
				*(hist + (ixp+1)*blocks[0] + (iyp+1) + best_o*blocks[0]*blocks[1]) += vx0*vy0*v;
			}
		}
	}

	// compute energy in each block by summing over orientations
	for (int o = 0; o < 9; o++) {
		double *src1 = hist + o*blocks[0]*blocks[1];
		double *src2 = hist + (o+9)*blocks[0]*blocks[1];
		double *dst = norm;
		double *end = norm + blocks[1]*blocks[0];
		while (dst < end) {
			*(dst++) += (*src1 + *src2) * (*src1 + *src2);
			src1++;
			src2++;
		}
	}

	// compute features
	for (int x = 0; x < out[1]; x++) {
		for (int y = 0; y < out[0]; y++) {
			double *dst = feat + x*matFeat.channels() + y*(matFeat.channels()*matFeat.cols);

			// x*3 is a compensation for the fact that at() cannot handle mats with 9 channels
			if(*dst != matFeat.at<Vec<double,9> >(y,x)[0] && *dst == *dst) {

				// cout << "First row: ";
				// int k = 0;
				// for(int i =0; i< matFeat.rows;i++) {
				// 	cout << "@(0," << i*9 << "): (" << *(feat+i*9*matFeat.cols) << ", ";
				// 	cout << *(feat+i*9*matFeat.cols+1) << ", ";
				// 	cout << *(feat+i*9*matFeat.cols+2) << ", ";
				// 	cout << *(feat+i*9*matFeat.cols+3) << ", ";
				// 	cout << *(feat+i*9*matFeat.cols+4) << ", ";
				// 	cout << *(feat+i*9*matFeat.cols+5) << ", ";
				// 	cout << *(feat+i*9*matFeat.cols+6) << ", ";
				// 	cout << *(feat+i*9*matFeat.cols+7) << ", ";
				// 	cout << *(feat+i*9*matFeat.cols+8) << "), ";
				// 	k+=9;
				// }
				// cout << endl;
				// cout << "Amount: " << k << endl;
				//
				// cout << "First row: ";
				// k = 0;
				// for(int i =0; i< matFeat.rows;i++) {
				// 	cout << "@(0," << i << "): (" << matFeat.at<Vec3d>(i,0)[0] << ", ";
				// 	cout << matFeat.at<Vec3d>(i,0)[1] << ", ";
				// 	cout << matFeat.at<Vec3d>(i,0)[2] << ", ";
				// 	cout << matFeat.at<Vec3d>(i,0)[3] << ", ";
				// 	cout << matFeat.at<Vec3d>(i,0)[4] << ", ";
				// 	cout << matFeat.at<Vec3d>(i,0)[5] << ", ";
				// 	cout << matFeat.at<Vec3d>(i,0)[6] << ", ";
				// 	cout << matFeat.at<Vec3d>(i,0)[7] << ", ";
				// 	cout << matFeat.at<Vec3d>(i,0)[8] << "), ";
				// 	k+=9;
				// }
				// cout << endl;
				// cout << "Amount: " << k << endl;
				// exit(1);

				LOGD("Channels: %d, Stepsize: %d\n", matFeat.channels(), matFeat.step1());
				LOGD("(X,Y) (%d,%d)\n",x,y);
				reportError("Wrong dimensions! 10");
			}

			double *src, *p, n1, n2, n3, n4;

			p = norm + (x+1)*blocks[0] + y+1;
			n1 = 1.0 / sqrt(*p + *(p+1) + *(p+blocks[0]) + *(p+blocks[0]+1) + eps);
			p = norm + (x+1)*blocks[0] + y;
			n2 = 1.0 / sqrt(*p + *(p+1) + *(p+blocks[0]) + *(p+blocks[0]+1) + eps);
			p = norm + x*blocks[0] + y+1;
			n3 = 1.0 / sqrt(*p + *(p+1) + *(p+blocks[0]) + *(p+blocks[0]+1) + eps);
			p = norm + x*blocks[0] + y;
			n4 = 1.0 / sqrt(*p + *(p+1) + *(p+blocks[0]) + *(p+blocks[0]+1) + eps);

			double t1 = 0;
			double t2 = 0;
			double t3 = 0;
			double t4 = 0;

			// contrast-sensitive features
			// src = hist + (x+1)*blocks[0] + (y+1);
			// for (int o = 0; o < 18; o++) {
			// 	double h1 = min(*src * n1, 0.2);
			// 	double h2 = min(*src * n2, 0.2);
			// 	double h3 = min(*src * n3, 0.2);
			// 	double h4 = min(*src * n4, 0.2);
			// 	//	*dst = 0.5 * (h1 + h2 + h3 + h4);
			// 	t1 += h1;
			// 	t2 += h2;
			// 	t3 += h3;
			// 	t4 += h4;
			// 	//	dst += out[0]*out[1];
			// 	src += blocks[0]*blocks[1];
			// }

			// contrast-insensitive features
			src = hist + (x+1)*blocks[0] + (y+1);
			for (int o = 0; o < 9; o++) {
				double sum = *src + *(src + 9*blocks[0]*blocks[1]);
				double h1 = min(sum * n1, 0.2);
				double h2 = min(sum * n2, 0.2);
				double h3 = min(sum * n3, 0.2);
				double h4 = min(sum * n4, 0.2);

				if(*dst != matFeat.at<Vec<double, 9> >(y,x)[o] && *dst == *dst) {
					reportError("Wrong dimensions! 11");
				}

				*dst = 0.5 * (h1 + h2 + h3 + h4);

				if(*dst != matFeat.at<Vec<double, 9> >(y,x)[o] && *dst == *dst) {
					reportError("Wrong dimensions! 12");
				}

				dst += 1;//out[0]*out[1];

				src += blocks[0]*blocks[1];
			}

			// texture features
			// *dst = 0.2357 * t1;
			//dst += out[0]*out[1];
			//*dst = 0.2357 * t2;
			//dst += out[0]*out[1];
			//*dst = 0.2357 * t3;
			//dst += out[0]*out[1];
			//*dst = 0.2357 * t4;
		}
	}

 //  	delete[] hist;
 //  	delete[] norm;

  	return matFeat;
}

bool isStrictPos(const double value) {
    return value>0;
}

void hogPyramid(Mat inputImg, HogParams hogParams, vector<Mat> &feat, vector<double> &scale) {

    LOGD("Starting to construct the HOG pyramid ...\n");
    //Make sure image is in double format
    inputImg.convertTo(inputImg, CV_64FC3);

    double sc = pow(2.0,(1.0/hogParams.levels_per_octave));

    // Start at detect_max_scale, and keep going down by the increment sc, until
    // we reach MAXLEVELS or detect_min_scale
    scale.clear();
    scale.resize(MAXLEVELS,0);
    feat.clear();
    LOGD("Variables initialized ...\n");
    for(int i = 1;i<=MAXLEVELS;i++) {
        // double scaler = hogParams.max_scale / pow(sc,(i-1));
		double scaler = hogParams.max_scale - hogParams.scale_step * (i-1);

        LOGD("Scaler: %f\n",scaler);

        if(scaler < hogParams.min_scale) {
            LOGD("Scaler is too small...");
            scale.resize(i-1,0);
            break;
        }

        scale[i-1] = scaler;
        Mat scaled;
        resize(inputImg,scaled,Size(inputImg.cols*scale[i-1],inputImg.rows*scale[i-1]));
        LOGD("(Old,New) dimensions: ([%d x %d], [%d x %d])\n",inputImg.size().width,
							inputImg.size().height,scaled.size().width,scaled.size().height);
        // scaled = imresize(I,scale(i));

        //if minimum dimensions is less than or equal to 5, exit
        if(min(scaled.cols,scaled.rows)<=MINDIMENSION){
            // scale = scale(scale>0); // Remove all non-positive elements from scale
            LOGD("Minimum dimensions is too low: %d ...\n",min(scaled.cols,scaled.rows));
            vector<double> out;
            copy_if(scale.begin(), scale.end(), out.begin(), isStrictPos);
            scale = out;
            break;
        }

        if(scaled.channels()==2) {
            LOGD("Scaled image has only 2 channels...\n");
            vector<Mat> channels;
            split(scaled,channels);
            channels.push_back(channels[0]);
            channels.push_back(channels[1]);
            channels.push_back(channels[0]);
            channels.push_back(channels[1]);
            merge(channels,scaled);
        }
            // scaled=repmat(scaled,[1 1 3]);
        LOGD("Starting to get the HOG features...\n");
        Mat hog = getHogFeatures(scaled,hogParams.sbin);
        feat.push_back(hog.clone());
        LOGD("Getting HOG features done ...\n");

        // if we get zero size feature, backtrack one, and dont produce any
        // more levels
        if ((feat[i-1].rows*feat[i-1].cols) == 0) {
            LOGD("We got zero size features ...\n");

            feat.pop_back();
            scale.pop_back();
            break;
        }

        // recover lost bin!!!
        Mat padded = Mat::zeros(feat[i-1].rows+2,feat[i-1].cols+2,feat[i-1].type());
        feat[i-1].copyTo(padded.rowRange(1,feat[i-1].rows+1).colRange(1,feat[i-1].cols+1));
        feat[i-1] = padded;

		// vector<Mat> splittedFeat;
        // split(feat[i-1],splittedFeat);
		// double maxEl, minEl;
        // cout << "Channels: " << hog.channels() << endl;
        // for(int ch = 0; ch < hog.channels(); ch++){
        //     minMaxLoc(splittedFeat[ch], &minEl, &maxEl);
        //     cout << "Max: "<< maxEl << endl;
        //     cout << "Type: " << splittedFeat[ch].type() << endl;
        // }
        // feat[i-1] = padarray(feat[i], [1 1 0], 0);

        // if the max dimensions is less than or equal to 5, dont produce
        // any more levels
        if (max(feat[i-1].rows,feat[i-1].cols)<=MINDIMENSION) {
            LOGD("Max dimensions was less than or equal to 5\n");

            // scale = scale(scale>0);
            vector<double> out;
            copy_if(scale.begin(), scale.end(), out.begin(), isStrictPos);
            scale = out;
            break;
        }
    }

    LOGD("HogPyramid calculation done.\n");
}

// // matlab entry point
// // F = features_pedro(image, bin)
// // image should be color with double values
// void mexFunction(int nlhs, mxArray *plhs[], int nrhs, const mxArray *prhs[]) {
//   if (nrhs != 2)
//     mexErrMsgTxt("Wrong number of inputs");
//   if (nlhs != 1)
//     mexErrMsgTxt("Wrong number of outputs");
//   plhs[0] = process(prhs[0], prhs[1]);
// }
