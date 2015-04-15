#ifndef DETECT_HPP
#define DETECT_HPP

#include <opencv2/opencv.hpp>

using namespace cv;

class BrickDetectorLines {
public:
    static void TrackBricks(Mat&, float, double, Mat&, Mat&, Mat&);
    static void CheckOverlap(Mat&, int, float&, float&); 
    static void CheckCurrFrameOverlap(Mat&, float&, int);
    static void getCurrentFrameThreshold(Mat&);
};

#endif // DETECT_HPP