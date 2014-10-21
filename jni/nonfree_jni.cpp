//
//
//
//
//JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_utilities_JNILib_storeTrackerKeypoints(JNIEnv * env, jobject obj, jlong imgMatPtr)
//{
//
//	Mat& image = *(Mat*) imgMatPtr;
//
////	string pKp = env->GetStringUTFChars(keypointPath , NULL );
////	string pDsc = env->GetStringUTFChars(descPath , NULL );
//
//	pd.buildPatternFromImage(image, p);
//	pd.train(p);
//
////	vector<KeyPoint> keypoints;
////	vector<Point2f> points;
////	Mat descriptors;
////
////	detector.detect(image, keypoints);
////	extractor.compute( image, keypoints, descriptors );
////
////    /* Some other processing, please check the download package for details. */
////    FileStorage fs;
////	fs.open(pKp, FileStorage::WRITE);
////	write(fs, "keypoints", keypoints);
////	fs.release();
////
////	// Store description to "descriptors.des".
////
////	fs.open(pDsc, FileStorage::WRITE);
////	fs << "descriptors" << descriptors;
////	fs.release();
//
//	// DEBUG: Draw keypoints
////	const char * imgOutFile = "/sdcard/nonfree/kptPic.jpg";
////	Mat outputImg;
////	Scalar keypointColor = Scalar(255, 0, 0);
////	drawKeypoints(image, keypoints, outputImg, keypointColor, DrawMatchesFlags::DRAW_RICH_KEYPOINTS);
////	imwrite(imgOutFile, outputImg);
//}
//
//bool stop = false;
//
//JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_utilities_JNILib_getCameraPose(
//		JNIEnv * env,
//		jclass javaClass,
//		jstring path_object,
//		jlong sceneImgPtr,
//		jstring objKeypointsPath,
//		jstring objDescriptorsPath,
//		jlong cameraPosePtr)
//{
//	Mat &camPose = * (Mat *) cameraPosePtr;
//	camPose.eye(3,3,CV_32SC1);
//
//	const char *objectPath = env->GetStringUTFChars(path_object, NULL);
//	const char *objKeypointsPth = env->GetStringUTFChars(objKeypointsPath, NULL);
//	const char *objDescriptorsPth = env->GetStringUTFChars(objDescriptorsPath, NULL);
//
//	if (stop) return;
//	stop = true;
//
//	Mat img_object = imread( objectPath, CV_LOAD_IMAGE_GRAYSCALE );
////	Mat img_scene = imread( scenePath, CV_LOAD_IMAGE_GRAYSCALE );
//	Mat img_scene = * (Mat *) sceneImgPtr;
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"KPT PATH: %s",objKeypointsPth);
//
//	vector<KeyPoint> keypoints_object;
//	FileStorage fs;
//	fs.open(objKeypointsPth, FileStorage::READ);
//	FileNode fn = fs["keypoints"];
//	read(fn, keypoints_object);
//	fs.release();
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"DESC PATH: %s",objDescriptorsPth);
//
//	Mat descriptors_object;
//	fs.open(objDescriptorsPth, FileStorage::READ);
//	fs["descriptors"] >> descriptors_object;
//	fs.release();
//
//	if( !img_object.data || !img_scene.data )
//	{
//		__android_log_print(ANDROID_LOG_ERROR,APPNAME," --(!) Error reading images ");
//		return ;
//	}
//
//	//-- Step 1: Detect the keypoints
//	std::vector<KeyPoint> keypoints_scene;
//
////	detector.detect( img_object, keypoints_object );
//	detector.detect( img_scene, keypoints_scene );
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Step 1: detection DONE");
//
//	//-- Step 2: Calculate descriptors (feature vectors)
//
//
//	Mat descriptors_scene;
//
////	extractor.compute( img_object, keypoints_object, descriptors_object );
//	extractor.compute( img_scene, keypoints_scene, descriptors_scene );
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Step 2: extraction DONE");
//
//	//-- Step 3: Matching descriptor vectors
//	std::vector< DMatch > matches;
//
//	if(descriptors_object.cols == descriptors_scene.cols) {
//		matcher.match( descriptors_object, descriptors_scene, matches );
//	} else {
//		stop = false;
//		return;
//	}
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Step 3: Matching DONE");
//
//	double max_dist = 0; double min_dist = 100;
//
//	//-- Quick calculation of max and min distances between keypoints
//	for( int i = 0; i < descriptors_object.rows; i++ )
//	{
//		double dist = matches[i].distance;
//		if( dist < min_dist ) min_dist = dist;
//		if( dist > max_dist ) max_dist = dist;
//	}
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"-- Max dist : %f \n", max_dist );
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"-- Min dist : %f \n", min_dist );
//
//	//-- Draw only "good" matches (i.e. whose distance is less than 3*min_dist )
//	std::vector< DMatch > good_matches;
//
//	for( int i = 0; i < descriptors_object.rows; i++ )
//	{
//		if( matches[i].distance < 200 ){
//			good_matches.push_back( matches[i]);
//		}
//	}
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Good matches found!");
//
//	//-- Localize the object
//	std::vector<Point2f> objPoints;
//	std::vector<Point2f> scene;
//
//	for( int i = 0; i < good_matches.size(); i++ )
//	{
//		//-- Get the keypoints from the good matches
//		objPoints.push_back( keypoints_object[ good_matches[i].queryIdx ].pt );
//		scene.push_back( keypoints_scene[ good_matches[i].trainIdx ].pt );
//	}
//
//	Mat status;
//	Mat H = findHomography( objPoints, scene, CV_RANSAC , 2.5, status);
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Found homography!");
//
//	vector<DMatch> inliers;
//	for(size_t i = 0; i < good_matches.size(); i++)
//	{
//	    if(status.at<char>(i) != 0)
//	    {
//	        inliers.push_back(matches[i]);
//	    }
//	}
//
//	Mat img_matches;
//	drawMatches( img_object, keypoints_object, img_scene, keypoints_scene,
//			inliers, img_matches, Scalar::all(-1), Scalar::all(-1),
//			   vector<char>(), DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS );
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Matches drawn!");
//
//	//-- Get the corners from the image_1 ( the object to be "detected" )
//	std::vector<Point2f> obj_corners(4);
//	obj_corners[0] = cvPoint(0,0); obj_corners[1] = cvPoint( img_object.cols, 0 );
//	obj_corners[2] = cvPoint( img_object.cols, img_object.rows ); obj_corners[3] = cvPoint( 0, img_object.rows );
//	std::vector<Point2f> scene_corners(4);
//
//	perspectiveTransform( obj_corners, scene_corners, H);
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Perspective transformation done!");
//
//	//-- Draw lines between the corners (the mapped object in the scene - image_2 )
//	line( img_matches, scene_corners[0] + Point2f( img_object.cols, 0), scene_corners[1] + Point2f( img_object.cols, 0), Scalar(0, 255, 0), 4 );
//	line( img_matches, scene_corners[1] + Point2f( img_object.cols, 0), scene_corners[2] + Point2f( img_object.cols, 0), Scalar( 0, 255, 0), 4 );
//	line( img_matches, scene_corners[2] + Point2f( img_object.cols, 0), scene_corners[3] + Point2f( img_object.cols, 0), Scalar( 0, 255, 0), 4 );
//	line( img_matches, scene_corners[3] + Point2f( img_object.cols, 0), scene_corners[0] + Point2f( img_object.cols, 0), Scalar( 0, 255, 0), 4 );
//
//	//-- Show detected matches
//	imwrite( "/sdcard/nonfree/matched.jpg", img_matches );
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Image written!");
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Scene corners: (%f,%f),(%f,%f),(%f,%f),(%f,%f)",
//			scene_corners[0].x, scene_corners[0].y,
//			scene_corners[1].x, scene_corners[1].y,
//			scene_corners[2].x, scene_corners[2].y,
//			scene_corners[3].x, scene_corners[3].y);
//
//	Utilities::logMat(camPose, "CameraPose");
//
//	return;
//}
