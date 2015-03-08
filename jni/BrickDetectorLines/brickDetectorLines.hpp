#ifndef DETECT_HPP
#define DETECT_HPP

#include <opencv2/opencv.hpp>

using namespace cv;

class BrickDetectorLines {
public:
    static void TrackBricks(Mat&, float, Mat&);
    static void CheckOverlap(Mat&, int, float&, float&);
};

#endif // DETECT_HPP