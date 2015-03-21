#ifndef DETECT_HPP
#define DETECT_HPP

#include <opencv2/opencv.hpp>

using namespace cv;

class BrickDetectorLines {
public:
    static void TrackBricks(Mat&, float, Mat&, Mat&);
    static void CheckOverlap(Mat&, int, float&, float&); 
    static void CheckCurrFrameOverlap(Mat&, float&);
    static void getCurrentFrameThresholdAndOverlap(Mat&, int&);
};

#endif // DETECT_HPP