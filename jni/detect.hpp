#ifndef DETECT_H
#define DETECT_H

#include <string>
#include <vector>
#include <opencv2/opencv.hpp>

struct ResultingMatch {
    float rho;
    int phi;
    int theta;
    cv::Point location;
    cv::Size templateSize;
};

class HogDetector {
    public:
        static void loadRenderedImages(std::string dir);
        static std::vector<ResultingMatch> findBestMatches(void);
};

#endif //DETECT_H
