#ifndef DETECT_H
#define DETECT_H

#include <string>
#include <vector>
#include <opencv2/opencv.hpp>

struct ResultingMatch {
    float rho;
    int phi;
    int theta;
    double score;
    cv::Point location;
    cv::Size templateSize;
};

class HogDetector {
    public:
        static void loadRenderedImages(std::string dir);
        static std::vector<ResultingMatch> findBestMatches(cv::Mat input);
};

#endif //DETECT_H
