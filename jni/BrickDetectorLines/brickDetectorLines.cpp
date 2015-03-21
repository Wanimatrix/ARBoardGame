#include <iostream>
#include <cstdio>
#include <string>
#include <cstdlib>
#include "../logger.hpp"
#include "brickDetectorLines.hpp"
#include "../utilities.hpp"

extern "C" {
  #include "getRealTime.h"
}

using namespace std;
using namespace cv;

/**
* TODO in 3D
* ----------
*  1) Voting system
*  2) Calculate height of brick using comparison between Z-axis in 3D (comes out of plane) 
*       and calculation of possible Z-values for point on upper plane of brick.
*  3) Use amount of overlap to deny smaller blocks (also in 2D)
*  
*/

/**
* TODO HERE
* Refine threshold edge -> Straight line.
* Corner detection to fix wrong contours. (Or use as a replacement for contour detection?)
*/

vector<Mat > originalContourThresholds;
Mat currentFrameThreshold;
int currentFrameNonZero;

/// Function Headers
#define NORMALIZE(point) Point2f((point).x/sqrt(pow((point).x,2)+pow((point).y,2)),(point).y/sqrt(pow((point).x,2)+pow((point).y,2)))
#define NORM(point) sqrt(pow((point).x,2)+pow((point).y,2))
#define DIR_ANGLE(angle) (((angle) < 0) ? angle+180 : angle)
#define SIGN(arg) ((arg) > 0) - ((arg) < 0)
#define CIRC_IDX(idx, max) (((idx) + (max)) % (max))
#define RIGHT_OR_LEFT_FROM_LINE(lineA, lineB, pt) SIGN(((lineB).x-(lineA).x)*((pt).y-(lineA).y) - ((lineB).y-(lineA).y)*((pt).x-(lineA).x))
#define PI 3.141592f

#define LOG_TAG "BRICK_LINES_DETECTOR"

void BrickDetectorLines::TrackBricks(Mat& frame, float upAngle, Mat& result, Mat& origContoursMat)
{
  Mat hsv;
  originalContourThresholds.clear();
  currentFrameThreshold = Mat::zeros(frame.size(), CV_8UC1);


  double start = getRealTime();
  cvtColor(frame, hsv, CV_BGR2HSV);

  // medianBlur(hsv, hsv, 5); // PERFORMANCE LOSS

  // Threshold, to keep only colors in the frame.
  Mat thresh;
  // int lowerbounds[] = {0,80,80}; // Light @ NIGHT
  // int lowerbounds[] = {0,140,80}; // Better for Light @ HOME DAYTIME
  int lowerbounds[] = {0,160,80}; // Better for Light @ KOT DAYTIME
  vector<int> lbs(begin(lowerbounds),end(lowerbounds));
  int upperbounds[] = {255,255,255};
  vector<int> ubs(begin(upperbounds),end(upperbounds));
  inRange(hsv, lbs, ubs, thresh);
  // medianBlur(thresh, thresh, 5); // PERFORMANCE LOSS

  int morph_size = 1;
  Mat element = getStructuringElement( MORPH_RECT, Size( 2*morph_size + 1, 2*morph_size+1 ), Point( morph_size, morph_size ) );
  morphologyEx( thresh, thresh, MORPH_CLOSE, element, Point(-1,-1), 1);
  morphologyEx( thresh, thresh, MORPH_OPEN, element, Point(-1,-1), 1);

  // int morph_elem = 0;
  // int morph_size = 0;
  // Mat element = getStructuringElement( morph_elem, Size( 2*morph_size + 1, 2*morph_size+1 ), Point( morph_size, morph_size ) );
  // /// Apply the specified morphology operation
  // morphologyEx( thresh, thresh, 0, element );

  // int morph_size = 8;
  // Mat element = getStructuringElement( MORPH_CROSS, Size( 2*morph_size + 1, 2*morph_size+1 ), Point( morph_size, morph_size ) );
  // morphologyEx( thresh, thresh, MORPH_CLOSE, element, Point(-1,-1), 1);

  Mat bricksCutFromImg;
  Mat mask;
  Mat inv_mask;
  vector<Mat> channels;
  channels.push_back(thresh);
  channels.push_back(thresh);
  channels.push_back(thresh);
  merge(channels, mask);
  bitwise_and(mask, frame, bricksCutFromImg);
  bitwise_not(mask, inv_mask);
  // bricksCutFromImg = bricksCutFromImg + inv_mask;
  bricksCutFromImg = mask;

  // imshow("before morphing", bricksCutFromImg);

  // Mat mask;
  // lbs[1] = 0;
  // ubs[0] = 0;
  // ubs[1] = 0;
  // ubs[2] = 0;
  // inRange(bricksCutFromImg, lbs, ubs, thresh);
  // channels.clear();
  // channels.push_back(thresh);
  // channels.push_back(thresh);
  // channels.push_back(thresh);
  // merge(channels, mask);

  // bricksCutFromImg = bricksCutFromImg + mask;

  __android_log_print(ANDROID_LOG_DEBUG,"BrickDetectTime","CutBricksFromImg time: %f\n",((float)(getRealTime() - start))*1000.0);

  start = getRealTime();

  // Detect contours
  Mat gray;
  Mat canny_out;
  vector<vector<Point> > contours;
  vector<Vec4i> hierarchy;
  RNG rng(12345);

  // GaussianBlur( bricksCutFromImg, bricksCutFromImg, Size(5,5), 0, 0, BORDER_DEFAULT );
  Mat tmp;
  double startGaussian = getRealTime(); // PERFORMANCE LOSS
  // cv::GaussianBlur(bricksCutFromImg, tmp, cv::Size(0, 0), 3);
  // cv::addWeighted(bricksCutFromImg, 1.5, tmp, -0.5, 0, bricksCutFromImg);
  // cv::GaussianBlur(bricksCutFromImg, tmp, cv::Size(0, 0), 3);
  // cv::addWeighted(bricksCutFromImg, 1.5, tmp, -0.5, 0, bricksCutFromImg);
  __android_log_print(ANDROID_LOG_DEBUG,"BrickDetectTime","Gaussian time: %f\n",((float)(getRealTime() - startGaussian))*1000.0);

  cvtColor(bricksCutFromImg, gray, CV_BGR2GRAY);

  double startEqual = getRealTime();
  // equalizeHist( gray, gray ); // PERFORMANCE LOSS
  __android_log_print(ANDROID_LOG_DEBUG,"BrickDetectTime","Equal time: %f\n",((float)(getRealTime() - startEqual))*1000.0);

  Mat drawing;

  double startCanny = getRealTime();
  // Detect edges using canny
  Canny( gray, canny_out, 180, 200, 3 );
  __android_log_print(ANDROID_LOG_DEBUG,"BrickDetectTime","Canny time: %f\n",((float)(getRealTime() - startCanny))*1000.0);

  Mat out;

  __android_log_print(ANDROID_LOG_DEBUG,"BrickDetectTime","Edge detection time: %f\n",((float)(getRealTime() - start))*1000.0);

  // Mat harrisResponse;
  // Mat harrisResponse_norm;
  // vector<Point2f> corners;
  /// Detecting corners
  // Mat mask1c;
  // split(mask, channels);
  // mask1c = channels[0];

  // imshow("Mask",mask1c);

  // cornerHarris( mask1c, harrisResponse, 10, 3, 0.04, BORDER_DEFAULT );
  

  // goodFeaturesToTrack(mask1c, corners, 25, 0.15, 10, Mat(), 7);
  // channels.clear();

  /// Normalizing
  // normalize( harrisResponse, harrisResponse_norm, 0, 255, NORM_MINMAX, CV_32FC1, Mat() );

  // imshow("Harris",harrisResponse_norm);

  start = getRealTime();

  cvtColor(canny_out, out, CV_GRAY2BGR);

  findContours( canny_out, contours, hierarchy, CV_RETR_CCOMP, CV_CHAIN_APPROX_SIMPLE, Point(0, 0) );

  double maxArea = 0;

  vector<vector<Point> > origContours;

  for( int c = 0; c< contours.size(); c++ ) {
    // double epsilon = 0.002*arcLength(contours[c],true);
    vector<Point> tmpContour;
    float origArea, newArea;
    origArea = contourArea(contours[c]);
    origContours.push_back(contours[c]);
    newArea = origArea;
    vector<Point> previous;
    // for(float i = 1; newArea/origArea >= 0.98; i+=0.01) {
      // previous = tmpContour;
      //0.01*arcLength(contours[c], true)
      approxPolyDP(Mat(contours[c]), tmpContour, 0.003*arcLength(contours[c], true), true);
    //   if(tmpContour.size() >= 2){
    //     newArea = contourArea(tmpContour);
    //   }
    //   else {
    //     tmpContour = previous;
    //     break;
    //   }
    //   LOGD("i: %f, area ratio: %f",i,newArea/origArea);
    // }
    if(tmpContour.size() > 0) {
      contours[c] = tmpContour;
    }

    double area = contourArea(contours[c]);

    if(area > maxArea)
      maxArea = area;

      
  }

  __android_log_print(ANDROID_LOG_DEBUG,"BrickDetectTime","Contour2Poly time: %f\n",((float)(getRealTime() - start))*1000.0);

  LOGD("Contour max area: %f",maxArea);


  start = getRealTime();
  int maxSize = 0;

  vector<vector<Point> > goodContours;
  vector<vector<Point> > originalContours;
  vector<vector<float> > contourAngles;
  vector<vector<float> > sideLengths;
  for( int c = 0; c< contours.size(); c++ ) {
    if(contourArea(contours[c]) > maxArea/100 && contours[c].size() > 3 && hierarchy[c][2] < 0) {
      vector<Point> contour = contours[c];

      vector<Point> betterContour;

      vector<float> curAngle(0);
      
      for(int pidx = 0; pidx< contours[c].size(); pidx++) {
        Point2f prev = contours[c][CIRC_IDX(pidx-1,contours[c].size())];
        Point2f p = contours[c][CIRC_IDX(pidx, contours[c].size())];
        Point2f next = contours[c][CIRC_IDX(pidx+1, contours[c].size())];

        Point2f vec = NORMALIZE(next-p);
        float angleDeg = acos(vec.x)*(180.0f/PI); 
        angleDeg = (next.y > p.y) ? (angleDeg*-1) : angleDeg;
        curAngle.push_back(angleDeg);

        LOGD("Direction angle: %f",angleDeg);
      }

      contourAngles.push_back(vector<float>(0));
      sideLengths.push_back(vector<float>(0));
      for(int pidx = 0; pidx< contours[c].size(); pidx++) {
        if(DIR_ANGLE(curAngle[pidx]) < (DIR_ANGLE(curAngle[CIRC_IDX(pidx-1,curAngle.size())])-35 % 180)
          || DIR_ANGLE(curAngle[pidx]) > (DIR_ANGLE(curAngle[CIRC_IDX(pidx-1,curAngle.size())])+35 % 180)) {
          betterContour.push_back(contour[pidx]);
          contourAngles.back().push_back(curAngle[pidx]);
          
        }
      }

      // for(int pidx = 0; pidx< betterContour.size(); pidx++) {
      //   sideLengths.back().push_back(NORM(betterContour[CIRC_IDX(pidx+1,betterContour.size())]-betterContour[pidx]));
      //   cout << "SideLength (px): " << sideLengths.back().back() << endl;
      // }


    //   contour = betterContour;
      if(betterContour.size() > maxSize) {
        maxSize = betterContour.size();
      }
      goodContours.push_back(betterContour);
      originalContours.push_back(origContours[c]);
      // Scalar color = Scalar( 0,0,255 );
      // drawContours( out, goodContours, goodContours.size()-1, color, 2, 3, hierarchy[c], 0, Point() );
    }
  }

  __android_log_print(ANDROID_LOG_DEBUG,"BrickDetectTime","Angle calculation time: %f\n",((float)(getRealTime() - start))*1000.0);


  LOGD("Amount of contours: %d",goodContours.size());
  LOGD("Amount of originalContours: %d",goodContours.size());
  LOGD("UpVector: %f",upAngle);

  start = getRealTime();

  LOGD("Non-filtered contoursSize: %d, OriginalAreas size: %d\n",goodContours.size(),originalContours.size());

  vector<Point2f> tmpVec;

  origContoursMat = Mat(goodContours.size(),maxSize*2+1,CV_32FC1);
  // vector<vector<Point> > brickContour;
  result = Mat(0,10,CV_32FC1); // 9: 1 col for the contour idx and rest is for the points of the contour (4 points)
  for( int c = 0; c< goodContours.size(); c++ ) {
    LOGD("NEXT CONTOUR");
    LOGD("-------------------");

    Mat contourThreshold = Mat::zeros(frame.size(), CV_8UC1);
    drawContours( contourThreshold, originalContours, c, Scalar(255,255,255), CV_FILLED);
    originalContourThresholds.push_back(contourThreshold);
    bitwise_or(currentFrameThreshold, contourThreshold, currentFrameThreshold);



    //   Point2f p = goodContours[c][CIRC_IDX(pidx, goodContours[c].size())];

      
      // RIGHT_OR_LEFT_FROM_LINE(a,b,p) == RIGHT_OR_LEFT_FROM_LINE(a,b,a+vecDown);

    //   Point2f testP = p+10*vecDown;
    //   Vec3b pixel;

    //   LOGD("ImageTYPE: %d",bricksCutFromImg.type());
    //   LOGD("TestPIXEL: (%f,%f)",testP.x,testP.y);

    //   // TODO: SOMETHING IS CLEARLY WRONG HERE!
    //   if(testP.x > 640) testP.x = 640;
    //   if(testP.x < 0) testP.x = 0;
    //   if(testP.y > 480) testP.y = 480;
    //   if(testP.y < 0) testP.y = 0;

    //   int amountWhite = 0;
    //   Mat ROI = bricksCutFromImg(Rect(((int) testP.y)-1,((int) testP.x)-1,3,3));
    //   for( int y = 0; y < ROI.rows; y++ ) {
    //     for( int x = 0; x < ROI.cols; x++ ) {
    //       pixel = bricksCutFromImg.at<Vec3b>(y, x);
    //       if(pixel[0] == 255 && pixel[1] == 255 && pixel[2] == 255)
    //         amountWhite++;
    //     }
    //   }
      
    //   LOGD("PIXEL: (%d,%d,%d)",pixel[0],pixel[1],pixel[2]);
    //   if(amountWhite > 6) {
    //     LOGD("Bottom Pixel!");
    //     // This is a bottom pixel!
    //     circle( bricksCutFromImg, p, 5,  Scalar(0,255,0), 3, 8, 0 );
    //     circle( bricksCutFromImg, testP, 5,  Scalar(255,255,0), 3, 8, 0 );
    //   }
    // }

    for(int startpidx = 0; startpidx< goodContours[c].size(); startpidx++) {
      vector<float> angles;
      bool isBrick = true;
      int up = -1;
      int dir = -2;


      for(int pidx = 0; pidx< goodContours[c].size(); pidx++) {
        LOGD("IDX TEST: %d",CIRC_IDX(-1,goodContours[c].size()));

        Point2f prev = goodContours[c][CIRC_IDX(startpidx+pidx-1,goodContours[c].size())];
        Point2f p = goodContours[c][CIRC_IDX(startpidx+pidx, goodContours[c].size())];
        Point2f next = goodContours[c][CIRC_IDX(startpidx+pidx+1, goodContours[c].size())];

        float angleDeg = contourAngles[c][CIRC_IDX(startpidx+pidx, goodContours[c].size())];
        LOGD("Direction angle: %f",angleDeg);

        if(DIR_ANGLE(angleDeg) > (DIR_ANGLE(upAngle)-25  % 180) && DIR_ANGLE(angleDeg) < (DIR_ANGLE(upAngle)+25 % 180)) {
          up = pidx;
        }

        if(angles.size() == 0) {
          angles.push_back(angleDeg);
          continue;
        }

        if(dir == -2) {
          dir = RIGHT_OR_LEFT_FROM_LINE(prev, p, next);
        } else if(RIGHT_OR_LEFT_FROM_LINE(prev, p, next) != dir) {
          LOGD("Incorrect direction!");
          isBrick = false;
          break;
        }
        
        for(int i = 0; i< angles.size(); i++) {
          if(DIR_ANGLE(angleDeg) > (DIR_ANGLE(angles[i])-35  % 180) && DIR_ANGLE(angleDeg) < (DIR_ANGLE(angles[i])+35 % 180)) {
            LOGD("This is not a brick! %f vs %f",DIR_ANGLE(angleDeg),DIR_ANGLE(angles[i]));
            isBrick = false;
            break;
          } 
        }

        if(!isBrick) break;
        else angles.push_back(angleDeg);


        LOGD("Angles size: %d",angles.size());
        if(angles.size() == 3 && up != -1) { // This contour is a brick
          Mat tmp(1,13,CV_32FC1);
          // Point2i tmp;
          // brickContour.push_back(vector<Point>(0));
          // brickContour.back().push_back();
          // brickContour.back().push_back(goodContours[c][CIRC_IDX(startpidx+1, goodContours[c].size())]);
          // brickContour.back().push_back(goodContours[c][CIRC_IDX(startpidx+2, goodContours[c].size())]);
          // brickContour.back().push_back(goodContours[c][CIRC_IDX(startpidx+3, goodContours[c].size())]);

          // originalContours[c];

          tmp.at<float>(0,0) = c;
          tmp.at<float>(0,1) = up;
          for(int pid = 0; pid < 4; pid++) {
            Point2f currentPt = goodContours[c][CIRC_IDX(startpidx+pid, goodContours[c].size())];
            tmp.at<float>(0,pid*3+2) = currentPt.x;
            tmp.at<float>(0,pid*3+3) = currentPt.y;
            if(pid < 3)
              tmp.at<float>(0,pid*3+4) = contourAngles[c][CIRC_IDX(startpidx+pid, contourAngles[c].size())];
          }
          result.push_back(tmp);
          Utilities::logMat(result,"BrickOutput");

          // tmp = goodContours[c][CIRC_IDX(startpidx+3, goodContours[c].size())]
          //       +goodContours[c][CIRC_IDX(startpidx+0, goodContours[c].size())]
          //       -goodContours[c][CIRC_IDX(startpidx+1, goodContours[c].size())];
          // brickContour.back().push_back(tmp);
          // tmp = tmp+(goodContours[c][CIRC_IDX(startpidx+1, goodContours[c].size())]
          //          -goodContours[c][CIRC_IDX(startpidx+2, goodContours[c].size())]);
          // brickContour.back().push_back(tmp);
          break;
        } else if(angles.size() == 3 && up == -1) {
          break;
        }
      }
      // if(isBrick) break;
    }

    origContoursMat.at<float>(c, 0) = goodContours[c].size();
    for(int pt = 0; pt < goodContours[c].size(); pt++) {
      origContoursMat.at<float>(c, pt*2+1) = goodContours[c][pt].x;
      origContoursMat.at<float>(c, pt*2+2) = goodContours[c][pt].y;
    }
  }


  currentFrameNonZero = countNonZero(currentFrameThreshold);
  __android_log_print(ANDROID_LOG_DEBUG,"BrickDetectTime","Contour filter+conversion time: %f\n",((float)(getRealTime() - start))*1000.0);


  // for( int c = 0; c< goodContours.size(); c++ ) {
  //   Scalar color = Scalar( 0, 255,0);
  //   drawContours( out, goodContours, c, color, 2, 3, hierarchy[c], 0, Point() );
  // }

  // /// Drawing a circle around corners
  // for( int j = 0; j < corners.size() ; j++ )
  //    { 
  //           circle( bricksCutFromImg, corners[j], 5,  Scalar(0), 2, 8, 0 );
  //    }

  


  // LOGIMG("frame",  frame);
  // LOGIMG("thresh",  thresh);
  // LOGIMG( "bricksCut",  bricksCutFromImg);
  // LOGIMG( "external",  out);
  // LOGIMG( "frameThreshold",  currentFrameThreshold);
  return;
}

void BrickDetectorLines::getCurrentFrameThresholdAndOverlap(Mat& frameThreshold, int& frameOverlap) {
  frameThreshold = currentFrameThreshold;
  frameOverlap = currentFrameNonZero;
}

void BrickDetectorLines::CheckOverlap(Mat& inputPoints, int origContourIdx, float& inputOverlapResult, float& origOverlapResult) {
  Mat origThreshold = originalContourThresholds[origContourIdx];

  vector<vector<Point >> inputContours(1);
  convexHull(inputPoints,inputContours[0],true);
  Mat inputPtsThreshold(origThreshold.size(), origThreshold.type());
  drawContours(inputPtsThreshold, inputContours, 0, Scalar(255,255,255), -1);

  int inputNonZero = countNonZero(inputPtsThreshold);
  int origNonZero = countNonZero(origThreshold);
  Mat result;
  bitwise_and(inputPtsThreshold, origThreshold, result);

  // Mat debug;
  // bitwise_or(inputPtsThreshold/2, origThreshold/2, debug);
  // bitwise_or(debug,result,debug);

  // LOGIMG( "overlapDebug",  debug);

  int resultNonZero = countNonZero(result);
  inputOverlapResult = resultNonZero/(float)inputNonZero;
  origOverlapResult = resultNonZero/(float)origNonZero;
  LOGD("Contour with orig idx %d: inputRatio(%f) origRatio(%f)", origContourIdx, inputOverlapResult, origOverlapResult);
}

void BrickDetectorLines::CheckCurrFrameOverlap(Mat& inputPoints, float& inputOverlapResult) {
  vector<vector<Point >> inputContours(1);

  double start = getRealTime();
  convexHull(inputPoints,inputContours[0],true);
  Rect bound = boundingRect(Mat(inputContours[0]));
  if(bound.x < 0 && abs(bound.x) < bound.width) bound.x = 0;
  else if(bound.y < 0 && abs(bound.y) < bound.height) bound.y = 0;
  else if(bound.x+bound.width > currentFrameThreshold.size().width) bound.x = currentFrameThreshold.size().width;
  else if(bound.y+bound.height > currentFrameThreshold.size().height) bound.y = currentFrameThreshold.size().height;
  LOGD("ROI size: (%d,%d);(%d,%d)",bound.width,bound.height,bound.x,bound.y);
  __android_log_print(ANDROID_LOG_DEBUG,"PERFORMANCE_ANALYSIS","Convex hull calculated in %f ms...",((float)(getRealTime() - start))*1000.0);

  Mat inputPtsThreshold = Mat::zeros(currentFrameThreshold.size(), currentFrameThreshold.type());

  start = getRealTime();
  drawContours(inputPtsThreshold, inputContours, 0, Scalar(255,255,255), -1);
  __android_log_print(ANDROID_LOG_DEBUG,"PERFORMANCE_ANALYSIS","Contours drawn in %f ms...",((float)(getRealTime() - start))*1000.0);

  Mat inputBounded = Mat(inputPtsThreshold, bound);
  Mat frameBounded = Mat(currentFrameThreshold, bound);

  start = getRealTime();
  double start2 = getRealTime();
  int inputNonZero = countNonZero(inputBounded);
  __android_log_print(ANDROID_LOG_DEBUG,"PERFORMANCE_ANALYSIS","CountNonZero input in %f ms...",((float)(getRealTime() - start2))*1000.0);
  start2 = getRealTime();
  // int origNonZero = currentFrameNonZero;
  __android_log_print(ANDROID_LOG_DEBUG,"PERFORMANCE_ANALYSIS","CountNonZero frame in %f ms...",((float)(getRealTime() - start2))*1000.0);
  Mat result(inputBounded.size(), CV_8UC1);
  start2 = getRealTime();
  bitwise_and(inputBounded, frameBounded, result, inputBounded);
  __android_log_print(ANDROID_LOG_DEBUG,"PERFORMANCE_ANALYSIS","And thresholds in %f ms...",((float)(getRealTime() - start2))*1000.0);

  // LOGIMG( "inputT",  inputPtsThreshold);
  // LOGIMG( "currentT",  currentFrameThreshold);

  // Mat debug;
  // bitwise_or(inputPtsThreshold/2, currentFrameThreshold/2, debug);
  // bitwise_or(debug,result,debug);

  start2 = getRealTime();
  int resultNonZero = countNonZero(frameBounded);
  __android_log_print(ANDROID_LOG_DEBUG,"PERFORMANCE_ANALYSIS","CountNonZero result in %f ms...",((float)(getRealTime() - start2))*1000.0);
  start2 = getRealTime();
  inputOverlapResult = resultNonZero/(float)inputNonZero;
  // origOverlapResult = resultNonZero/(float)origNonZero;
  __android_log_print(ANDROID_LOG_DEBUG,"PERFORMANCE_ANALYSIS","Calculation of overlap in %f ms...",((float)(getRealTime() - start2))*1000.0);

  // ostringstream ostr;
  // ostr << inputOverlapResult;
  // putText(debug, ostr.str(), Point(50,50), 0.5, 1, Scalar(255,255,255));
  // LOGIMG( "overlapDebug",  debug);


  LOGD("inputRatio(%f)", inputOverlapResult);
}