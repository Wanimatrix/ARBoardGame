#ifndef FEATURES_H
#define FEATURES_H

#include <opencv2/opencv.hpp>

#define CV_64FC9 CV_MAKETYPE(CV_64F,9)
#define CV_32FC9 CV_MAKETYPE(CV_32F,9)
#define CV_64FC18 CV_MAKETYPE(CV_64F,18)
#define CV_32FC18 CV_MAKETYPE(CV_32F,18)

using namespace std;
using namespace cv;

struct HogParams {
    int sbin;
    double min_scale;
    double max_scale;
    double scale_step;
    int levels_per_octave;
};

Mat getHogFeatures(Mat image, int nBins);
void hogPyramid(Mat inputImg, HogParams hogParams, vector<Mat> &feat, vector<double> &scale);

#endif //FEATURES_H
