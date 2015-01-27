#ifndef HOGDETECTOR_HPP_INCLUDED
#define HOGDETECTOR_HPP_INCLUDED

//#include <opencv/cv.h>
#include <opencv2/opencv.hpp>
#include <vector>

#define ELEM(type,start,step,size,xpos,ypos,ichannel) \
        *((type*)(start+step*(ypos)+(xpos)*size+ichannel))

#define PI 3.142

using namespace cv;
using namespace std;

//IplImage** calculateIntegralHOG(IplImage* in);
//void calculateHOG_rect(CvRect cell, CvMat* hog_cell,
//                        IplImage** integrals, int normalization);

// std::vector<cv::Mat> calculateIntegralHOG(const cv::Mat& _in, int& _nbins);
// void calculateHOG_rect(std::vector<cv::Mat> _integrals, cv::Rect _cell,
//                        int _nbins, cv::Mat& _hogCell, int _normalization=cv::NORM_MINMAX);

Mat get_hogdescriptor_visual_image(Mat& origImg,
                                   vector<float>& descriptorValues,
                                   Size winSize,
                                   Size cellSize,
                                   int scaleFactor,
                                   double viz_factor);

void calculateHOGDescriptor(Mat& inputImg, vector<float>& descriptorValues, Size winSize, Size cellSize, vector<Point>& locations);

#endif // HOGDETECTOR_HPP_INCLUDED
