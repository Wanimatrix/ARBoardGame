#include "opencv2/opencv.hpp"
#include <iostream>
#include <cstdio>
#include "brickDetectorLines.hpp"


/** @function main */
int main( int argc, char** argv )
{

  printf("Testing custom detection using camera\n");
  VideoCapture cap(0); // open the default camera
  if(!cap.isOpened()) { // check if we succeeded
      printf("Error opening camera!\n");
      return EXIT_FAILURE;
  }

  cap.set(CV_CAP_PROP_FRAME_WIDTH, 640);
  cap.set(CV_CAP_PROP_FRAME_HEIGHT, 480);

  Mat frame;
  while ((cvWaitKey(10) & 255) != 27) {
      cap >> frame; // get a new frame from camera
      BrickDetectorLines::TrackBricks(frame);
      waitKey(0);
  }
  cap.release();

  // waitKey(0);
  return 0;
}