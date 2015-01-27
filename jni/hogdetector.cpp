#include <hogdetector.hpp>
#include <iostream>

// /*Function to calculate the integral histogram*/
// std::vector<cv::Mat> calculateIntegralHOG(const cv::Mat& _in, int& _nbins)
// {
//         /*Convert the input image to grayscale*/
//
//         cv::Mat img_gray; // = cv::Mat(in.size(),CV_8UC1);// cvCreateImage(cvGetSize(in), IPL_DEPTH_8U,1);
//
//         cv::cvtColor(_in,img_gray,CV_BGR2GRAY);
//         cv::equalizeHist(img_gray, img_gray);
//
//         /* Calculate the derivates of the grayscale image in the x and y
//            directions using a sobel operator and obtain 2 gradient images
//            for the x and y directions*/
//
//         cv::Mat xsobel, ysobel;
//         cv::Sobel(img_gray,xsobel,CV_32FC1,1,0);
//         cv::Sobel(img_gray,ysobel,CV_32FC1,0,1);
//
//         img_gray.release();
//
//         /* Create an array of 9 images (9 because I assume bin size 20 degrees
//            and unsigned gradient ( 180/20 = 9), one for each bin which will have
//            zeroes for all pixels, except for the pixels in the original image
//            for which the gradient values correspond to the particular bin.
//            These will be referred to as bin images. These bin images will be then
//            used to calculate the integral histogram, which will quicken
//            the calculation of HOG descriptors */
//
//         std::vector<cv::Mat> bins(_nbins);
//         for (int i = 0; i < _nbins; i++)
//         {
//                 bins[i] = cv::Mat::zeros(_in.size(), CV_32FC1);
//         }
//
//         /* Create an array of 9 images ( note the dimensions of the image,
//            the cvIntegral() function requires the size to be that), to store
//            the integral images calculated from the above bin images.
//            These 9 integral images together constitute the integral histogram */
//
//         std::vector<cv::Mat> integrals(_nbins);
//         //IplImage** integrals = (IplImage**) malloc(9 * sizeof(IplImage*));
//         for (int i = 0; i < _nbins; i++)
//         {
//                 integrals[i] = cv::Mat(cv::Size(_in.size().width + 1, _in.size().height + 1), CV_64FC1);
//         }
//
//         /* Calculate the bin images. The magnitude and orientation of the gradient
//            at each pixel is calculated using the xsobel and ysobel images.
//            {Magnitude = sqrt(sq(xsobel) + sq(ysobel) ), gradient = itan (ysobel/xsobel) }.
//            Then according to the orientation of the gradient, the value of the
//            corresponding pixel in the corresponding image is set */
//
//         int x, y;
//         float temp_gradient, temp_magnitude;
//         for (y = 0; y <_in.size().height; y++)
//         {
//                 /* ptr1 and ptr2 point to beginning of the current row in the xsobel and ysobel images
//                    respectively.
//                    ptrs[i] point to the beginning of the current rows in the bin images */
//
//                 float* xsobelRowPtr = (float*) (xsobel.row(y).data);
//                 float* ysobelRowPtr = (float*) (ysobel.row(y).data);
//                 float** binsRowPtrs = new float *[_nbins];
//                 for (int i = 0; i < _nbins; i++)
//                 {
//                         binsRowPtrs[i] = (float*) (bins[i].row(y).data);
//                 }
//
//                 /*For every pixel in a row gradient orientation and magnitude
//                    are calculated and corresponding values set for the bin images. */
//                 for (x = 0; x <_in.size().width; x++)
//                 {
//                         /* if the xsobel derivative is zero for a pixel, a small value is
//                            added to it, to avoid division by zero. atan returns values in radians,
//                            which on being converted to degrees, correspond to values between -90 and 90 degrees.
//                            90 is added to each orientation, to shift the orientation values range from {-90-90} to {0-180}.
//                            This is just a matter of convention. {-90-90} values can also be used for the calculation. */
//                         if (xsobelRowPtr[x] == 0)
//                         {
//                                 temp_gradient = ((atan(ysobelRowPtr[x] / (xsobelRowPtr[x] + 0.00001))) * (180/ PI)) + 90;
//                         }
//                         else
//                         {
//                                 temp_gradient = ((atan(ysobelRowPtr[x] / xsobelRowPtr[x])) * (180 / PI)) + 90;
//                         }
//                         temp_magnitude = sqrt((xsobelRowPtr[x] * xsobelRowPtr[x]) + (ysobelRowPtr[x] * ysobelRowPtr[x]));
//
//                         /*The bin image is selected according to the gradient values.
//                            The corresponding pixel value is made equal to the gradient
//                            magnitude at that pixel in the corresponding bin image */
//                         float binStep = 180/_nbins;
//
//                         for (int i=1; i<=_nbins; i++)
//                         {
//                                 if (temp_gradient <= binStep*i)
//                                 {
//                                         binsRowPtrs[i-1][x] = temp_magnitude;
//                                         break;
//                                 }
//                         }
//                 }
//         }
//
//         //cvReleaseImage(&xsobel);
//         //cvReleaseImage(&ysobel);
//
//         xsobel.release();
//         ysobel.release();
//
//         /*Integral images for each of the bin images are calculated*/
//
//         for (int i = 0; i <_nbins; i++)
//         {
//                 cv::integral(bins[i], integrals[i]);
//         }
//
//         for (int i = 0; i <_nbins; i++)
//         {
//                 bins[i].release();
//         }
//
//         /*The function returns an array of 9 images which consitute the integral histogram*/
//         return (integrals);
// }
//
// /*The following demonstrates how the integral histogram calculated using
//    the above function can be used to calculate the histogram of oriented
//    gradients for any rectangular region in the image:*/
//
// /* The following function takes as input the rectangular cell for which the
//    histogram of oriented gradients has to be calculated, a matrix hog_cell
//    of dimensions 1xnbins to store the bin values for the histogram, the integral histogram,
//    and the normalization scheme to be used. No normalization is done if normalization = -1 */
//
// void calculateHOG_rect(std::vector<cv::Mat> _integrals, cv::Rect _roi,
//                        int _nbins, cv::Mat& _hogCell, int _normalization)
// {
//         if (_roi.width == 0 || _roi.height == 0)
//         {
//                 _roi.x = 0; _roi.y = 0;
//                 _roi.width = _integrals[0].size().width-1;
//                 _roi.height = _integrals[0].size().height-1;
//         }
//         /* Calculate the bin values for each of the bin of the histogram one by one */
//         for (int i = 0; i < _nbins; i++)
//         {
//                 IplImage intImgIpl = _integrals[i];
//
//                 float a = ((double*)(intImgIpl.imageData + (_roi.y)
//                                      * (intImgIpl.widthStep)))[_roi.x];
//                 float b = ((double*) (intImgIpl.imageData + (_roi.y + _roi.height)
//                                       * (intImgIpl.widthStep)))[_roi.x + _roi.width];
//                 float c = ((double*) (intImgIpl.imageData + (_roi.y)
//                                       * (intImgIpl.widthStep)))[_roi.x + _roi.width];
//                 float d = ((double*) (intImgIpl.imageData + (_roi.y + _roi.height)
//                                       * (intImgIpl.widthStep)))[_roi.x];
//
//                 ((float*) _hogCell.data)[i] = (a + b) - (c + d);
//         }
//         /*Normalize the matrix*/
//         if (_normalization != -1)
//         {
//                 cv::normalize(_hogCell, _hogCell, 0, 1, _normalization);
//         }
// }


void calculateHOGDescriptor(Mat& inputImg, vector<float>& descriptorValues, Size winSize, Size cellSize, vector<Point>& locations) {
    HOGDescriptor d(
    winSize, //winSize
    Size(16,16), //blocksize
    Size(8,8), //blockStride,
    cellSize, //cellSize,
    9, //nbins,
    0, //derivAper,
    -1, //winSigma,
    0, //histogramNormType,
    0.2, //L2HysThresh,
    0 //gammal correction,
    //nlevels=64
    );

    // void HOGDescriptor::compute(const Mat& img, vector<float>& descriptors,
    //                             Size winStride, Size padding,
    //                             const vector<Point>& locations) const
    // vector<float> descriptorsValues;
    // vector<Point> locations;
    d.compute( inputImg, descriptorValues, Size(0,0), Size(0,0), locations);
}

// HOGDescriptor visual_imagealizer
// adapted for arbitrary size of feature sets and training images
Mat get_hogdescriptor_visual_image(Mat& origImg,
                                   vector<float>& descriptorValues,
                                   Size winSize,
                                   Size cellSize,
                                   int scaleFactor,
                                   double viz_factor)
{
    Mat visual_image;
    resize(origImg, visual_image, Size(origImg.cols*scaleFactor, origImg.rows*scaleFactor));

    int gradientBinSize = 9;
    // dividing 180° into 9 bins, how large (in rad) is one bin?
    float radRangeForOneBin = 3.14/(float)gradientBinSize;

    // prepare data structure: 9 orientation / gradient strenghts for each cell
	int cells_in_x_dir = winSize.width / cellSize.width;
    int cells_in_y_dir = winSize.height / cellSize.height;
    int totalnrofcells = cells_in_x_dir * cells_in_y_dir;
    float*** gradientStrengths = new float**[cells_in_y_dir];
    int** cellUpdateCounter   = new int*[cells_in_y_dir];
    for (int y=0; y<cells_in_y_dir; y++)
    {
        gradientStrengths[y] = new float*[cells_in_x_dir];
        cellUpdateCounter[y] = new int[cells_in_x_dir];
        for (int x=0; x<cells_in_x_dir; x++)
        {
            gradientStrengths[y][x] = new float[gradientBinSize];
            cellUpdateCounter[y][x] = 0;

            for (int bin=0; bin<gradientBinSize; bin++)
                gradientStrengths[y][x][bin] = 0.0;
        }
    }

    // nr of blocks = nr of cells - 1
    // since there is a new block on each cell (overlapping blocks!) but the last one
    int blocks_in_x_dir = cells_in_x_dir - 1;
    int blocks_in_y_dir = cells_in_y_dir - 1;

    // compute gradient strengths per cell
    int descriptorDataIdx = 0;
    int cellx = 0;
    int celly = 0;

    for (int blockx=0; blockx<blocks_in_x_dir; blockx++)
    {
        for (int blocky=0; blocky<blocks_in_y_dir; blocky++)
        {
            // 4 cells per block ...
            for (int cellNr=0; cellNr<4; cellNr++)
            {
                // compute corresponding cell nr
                int cellx = blockx;
                int celly = blocky;
                if (cellNr==1) celly++;
                if (cellNr==2) cellx++;
                if (cellNr==3)
                {
                    cellx++;
                    celly++;
                }

                for (int bin=0; bin<gradientBinSize; bin++)
                {
                    float gradientStrength = descriptorValues[ descriptorDataIdx ];
                    descriptorDataIdx++;

                    gradientStrengths[celly][cellx][bin] += gradientStrength;

                } // for (all bins)


                // note: overlapping blocks lead to multiple updates of this sum!
                // we therefore keep track how often a cell was updated,
                // to compute average gradient strengths
                cellUpdateCounter[celly][cellx]++;

            } // for (all cells)


        } // for (all block x pos)
    } // for (all block y pos)


    // compute average gradient strengths
    for (int celly=0; celly<cells_in_y_dir; celly++)
    {
        for (int cellx=0; cellx<cells_in_x_dir; cellx++)
        {

            float NrUpdatesForThisCell = (float)cellUpdateCounter[celly][cellx];

            // compute average gradient strenghts for each gradient bin direction
            for (int bin=0; bin<gradientBinSize; bin++)
            {
                gradientStrengths[celly][cellx][bin] /= NrUpdatesForThisCell;
            }
        }
    }


    cout << "descriptorDataIdx = " << descriptorDataIdx << endl;

    // draw cells
    for (int celly=0; celly<cells_in_y_dir; celly++)
    {
        for (int cellx=0; cellx<cells_in_x_dir; cellx++)
        {
            int drawX = cellx * cellSize.width;
            int drawY = celly * cellSize.height;

            int mx = drawX + cellSize.width/2;
            int my = drawY + cellSize.height/2;

            rectangle(visual_image,
                      Point(drawX*scaleFactor,drawY*scaleFactor),
                      Point((drawX+cellSize.width)*scaleFactor,
                      (drawY+cellSize.height)*scaleFactor),
                      CV_RGB(100,100,100),
                      1);

            // draw in each cell all 9 gradient strengths
            for (int bin=0; bin<gradientBinSize; bin++)
            {
                float currentGradStrength = gradientStrengths[celly][cellx][bin];

                // no line to draw?
                if (currentGradStrength==0)
                    continue;

                float currRad = bin * radRangeForOneBin + radRangeForOneBin/2;

                float dirVecX = cos( currRad );
                float dirVecY = sin( currRad );
                float maxVecLen = cellSize.width/2;
                float scale = viz_factor; // just a visual_imagealization scale,
                                          // to see the lines better

                // compute line coordinates
                float x1 = mx - dirVecX * currentGradStrength * maxVecLen * scale;
                float y1 = my - dirVecY * currentGradStrength * maxVecLen * scale;
                float x2 = mx + dirVecX * currentGradStrength * maxVecLen * scale;
                float y2 = my + dirVecY * currentGradStrength * maxVecLen * scale;

                // draw gradient visual_imagealization
                line(visual_image,
                     Point(x1*scaleFactor,y1*scaleFactor),
                     Point(x2*scaleFactor,y2*scaleFactor),
                     CV_RGB(255,0,0),
                     1);

            } // for (all bins)

        } // for (cellx)
    } // for (celly)


    // don't forget to free memory allocated by helper data structures!
    for (int y=0; y<cells_in_y_dir; y++)
    {
      for (int x=0; x<cells_in_x_dir; x++)
      {
           delete[] gradientStrengths[y][x];
      }
      delete[] gradientStrengths[y];
      delete[] cellUpdateCounter[y];
    }
    delete[] gradientStrengths;
    delete[] cellUpdateCounter;

    return visual_image;

}
