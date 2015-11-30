import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.opencv.core.*;
import org.opencv.highgui.*;
import org.opencv.imgproc.*;
import org.opencv.objdetect.HOGDescriptor;
import org.opencv.video.Video;


public class VideoAnalysis {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.loadLibrary( Core.NATIVE_LIBRARY_NAME );


		try {
			//Load the video file or images
			//VideoLoader loader = new VideoLoader("C:/Documents and Settings/pereirak/My Documents/Downloads/%03d.png");
			VideoLoader loader = new VideoLoader("data2/%010d.png");

			//Initialise some stuff

			VideoCapture cap = loader.getCapture();
			//Imshow LKwindow = new Imshow("LKFlow");
			Imshow Fwindow = new Imshow("FarnFlow");
			Imshow actual = new Imshow("actual");
			Imshow sub = new Imshow("submat");
			Imshow frame = new Imshow("Frame");

			Mat colourPrevFrame = new Mat();

			Mat prevFrame = new Mat();
			Mat tempPrev = new Mat();

			Mat currentFrame = new Mat();
			Mat tempCurrent = new Mat();

			Mat flow = new Mat();

			Mat diff1 = new Mat();
			Mat diff2 = new Mat();
			Mat temp1 = new Mat();

			Size ksize = new Size(3.0, 3.0);
			double sigmaX = 5;

			FarnFlow denseflow = new FarnFlow();
			int frame_num = 0;

			if(cap.read(tempPrev)== false){
				System.err.println("Error: Failed to read first frame");
				return;
			}

			//actual.showImage(tempPrev);
			tempPrev = normLum(tempPrev);
			//sub.showImage(tempPrev);

			if(tempPrev.channels() > 1){
				Imgproc.cvtColor(tempPrev,temp1,Imgproc.COLOR_BGR2GRAY);

			}else{
				temp1 = tempPrev.clone();
			}

			Imgproc.GaussianBlur(temp1, prevFrame, ksize, sigmaX);


			cap.read(tempCurrent);
			tempCurrent = normLum(tempCurrent);

			if(tempCurrent.channels() > 1){
				Imgproc.cvtColor(tempCurrent,temp1,Imgproc.COLOR_BGR2GRAY);
			}else{
				temp1 = tempCurrent.clone();
			}
			Imgproc.GaussianBlur(temp1, currentFrame, ksize, sigmaX);

			Core.subtract(currentFrame, prevFrame, diff1);
			currentFrame.copyTo(prevFrame);


			Mat threshImage = new Mat();
			//double thresh = 10;
			double flowThresh = 11;

			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
			Mat hierarchy = new Mat();
			Mat grayflow = new Mat();
			Mat velfield = new Mat();

			Mat subflow = new Mat();

			int[] boxHist;
			int[] objDetected;
			int maxPix;
			boolean done = false;

			Mat threshImage1 = new Mat();
			Mat threshImage2 = new Mat();
			int threshIm = 5;

			Imgproc.threshold(diff1, threshImage1, threshIm, 255, Imgproc.THRESH_BINARY);
			Imgproc.GaussianBlur(threshImage1, threshImage1, ksize, 2*sigmaX);

			Histogram velHist = new Histogram(100,100, 0.0,360.0, 0.0,50.0);


			int frameNumber = 1;


			HOGDescriptor hog = new HOGDescriptor(new Size(48, 96),new Size(16, 16), new Size(8, 8), new Size(8, 8),9);
			hog.setSVMDetector(HOGDescriptor.getDaimlerPeopleDetector());
			boolean skipFrame = false;

			//For each frame
			while(cap.read(tempCurrent)){
				
				if(skipFrame == false){

					int objectNumber = 1;


					tempCurrent = normLum(tempCurrent);
					
					if(tempCurrent.channels() > 1){
						Imgproc.cvtColor(tempCurrent,temp1,Imgproc.COLOR_BGR2GRAY);
					}else{
						temp1 = tempCurrent.clone();
					}

					//Difference the frames
					Imgproc.GaussianBlur(temp1, currentFrame, ksize, sigmaX);
					Core.subtract(currentFrame, prevFrame, diff2);

					if(!(prevFrame.channels() > 1)){
						Imgproc.cvtColor(prevFrame,colourPrevFrame,Imgproc.COLOR_GRAY2BGR);;
					}else{
						//prevFrame.copyTo(colourPrevFrame);
					}


					Imgproc.threshold(diff2, threshImage2, threshIm, 255, Imgproc.THRESH_BINARY);
					Imgproc.GaussianBlur(threshImage2, threshImage2, ksize, 2*sigmaX);

					//Get the velocity field of the differenced frames
					denseflow.getFlow(diff1, diff2, velfield).copyTo(flow);

					Imgproc.cvtColor(flow,grayflow,Imgproc.COLOR_BGR2GRAY);
					Imgproc.GaussianBlur(grayflow, grayflow, ksize, sigmaX*2);

					//Threshold the flow field
					Imgproc.threshold(grayflow, threshImage, flowThresh, 255, Imgproc.THRESH_BINARY);

					threshImage.copyTo(grayflow);

					//Get the contours of the blobs
					Imgproc.findContours(threshImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);


					//If there are any blobs in the frame
					if(contours.size()>0){

						Rect box = new Rect();

						for(MatOfPoint points : contours){
							
							//Place a bounding box around blob
							box = Imgproc.boundingRect(points);

							Point[]po = points.toArray();

							int minArea = 1000;

							//Ignore insignificant blobs
							if(box.size().area() > minArea){ 


								subflow = grayflow.submat(box);

								boxHist = new int[subflow.cols()];
								objDetected = new int[subflow.cols()];
								maxPix = 0;

								done = false;

								//Create a histogram of pixel heights to ignore the shadow
								for(int x=0; x<subflow.cols(); x++){
									for(int y=0; y <subflow.rows();y ++ ){

										double[] pixel = subflow.get(y, x);
										done = false;

										for(double p : pixel){

											if(!done && p > 0){
												boxHist[x]++;
												done = true;
											}
										}

									}

									if(boxHist[x] >maxPix){
										maxPix = boxHist[x];
									}
								}


								for(int n =0; n < boxHist.length; n++){
									if((double) boxHist[n] > 0.7*maxPix){
										objDetected[n] = 1;
									}
									//System.out.print(objDetected[n]);
									//System.out.print(boxHist[n]+ ",");
								}
								


								List<DetectedObj> objects = new ArrayList<DetectedObj>();

								int startObj = -1;

								int endObj = -1;
								int state = 0;
								double closeDistance = 0.1*objDetected.length;

								//Find and merge objects
								for(int n =0; n< objDetected.length; n++){

									if(n == objDetected.length-1){
										switch(state){
										case 0:
										case 1:
											objects.add(new DetectedObj(startObj, n));
											//	System.out.println("Added object between " + startObj+ " and " + n);
											break;
										case 2:
											if((n - endObj) < closeDistance){
												objects.add(new DetectedObj(startObj, n));
												//System.out.println("Added object between " + startObj+ " and " + n);
											}else{
												objects.add(new DetectedObj(startObj, endObj));
												//System.out.println("Added object between " + startObj+ " and " + endObj);
											}

										default:
											break;
										}
									}

									switch(state){
									case 0: if(objDetected[n] == 1){
										startObj =n;
										state =1;
									}
									break;
									case 1: if(objDetected[n] == 0){
										endObj = n;
										state =2;
									}
									break;
									case 2: if(objDetected[n] == 1){
										if((n - endObj) < closeDistance){
											state =1;
										}else{

											//AddObject
											objects.add(new DetectedObj(startObj, endObj));
											//System.out.println("Added object between " + startObj+ " and " + endObj);
											startObj = n;
											state = 1;
										}
									}
									break;
									default:
										break;


									}

								}

								//For each detected object
								for(DetectedObj object : objects){

									Point tl0 = box.tl();

									Point tl = new Point(object.getStart(),0);
									Point br = new Point(object.getEnd(),box.height-1);

									Point ntl = new Point(tl0.x+tl.x,  tl0.y+tl.y);
									Point nbr = new Point(tl0.x+br.x,  tl0.y+br.y);

									Point v = new Point();
									Rect newBox = new Rect(ntl,nbr);

									if((newBox.width < 10) || (newBox.height < 10)){
										continue;
									}

									Mat subVelField = velfield.submat(newBox);
									Mat subthreshBox = grayflow.submat(newBox);

							

									int[] histHist = new int[velHist.getValues().length];
									double meanVel = 0;
									double meanVelTop = 0;
									double meanVelBot = 0;
									double stdVelTop = 0;
									double stdVelBot = 0;
									double stdDevVel = 0;
									double sinTop = 0;
									double sinBot = 0;
									double cosTop = 0;
									double cosBot = 0;
									
									double sin = 0;
								    double cos = 0;
									int size = 0;
									int sizeTop = 0;
									int sizeBot = 0;
									
									for(int x=0; x<subthreshBox.cols(); x++){
										for(int y=0; y <subthreshBox.rows();y++){

											double[] pixel = subthreshBox.get(y, x);
											done = false;

											for(double p : pixel){

												if(!done && p > 0){
													v.set(subVelField.get(y, x));
													double mag; mag = Math.sqrt( Math.pow((v.x),2) + Math.pow((v.y),2));
													double angle; angle = Math.atan2( (double) v.y, (double) v.x );
													velHist.add(Math.toDegrees(angle),mag);
																					
													meanVel +=mag;
													stdDevVel += mag*mag;
													sin += Math.sin(angle);
											        cos += Math.cos(angle);
													
											        if(y >subthreshBox.rows()/2){
											        	meanVelBot +=mag;
														stdVelBot += mag*mag;
														sinBot += Math.sin(angle);
												        cosBot += Math.cos(angle);
												        sizeBot++;
											        }else{
											        	meanVelTop +=mag;
														stdVelTop += mag*mag;
														sinTop += Math.sin(angle);
												        cosTop += Math.cos(angle);
												        sizeTop++;
											        }
											        
													size++;
													done = true;
												}
											}
										}
									}
										
									//Stats for whole box
									meanVel /= size;
									sin /= size;
									cos /= size;
									double stdDevAng = Math.sqrt(-Math.log(sin*sin+cos*cos));
									stdDevVel = Math.sqrt(stdDevVel/size - meanVel*meanVel);
									
									//Stats for upper half
									meanVelTop /= sizeTop;
									sinTop /= sizeTop;
									cosTop /= sizeTop;
									double meanAngTop = Math.sqrt(sinTop*sinTop+cosTop*cosTop);
									double stdDevAngTop = Math.sqrt(-Math.log(sinTop*sinTop+cosTop*cosTop));
									stdVelTop = Math.sqrt(stdVelTop/sizeTop - meanVelTop*meanVelTop);
									
									//Stats for lower half
									meanVelBot /= sizeBot;
									sinBot /= sizeBot;
									cosBot /= sizeBot;
									double meanAngBot = Math.sqrt(sinBot*sinBot+cosBot*cosBot);
									double stdDevAngBot = Math.sqrt(-Math.log(sinBot*sinBot+cosBot*cosBot));
									stdVelBot = Math.sqrt(stdVelBot/sizeBot - meanVelBot*meanVelBot);
									
									double CVT = stdVelTop/meanVelTop;
									double VDT = meanVelTop*meanVelTop/sizeTop;
									double AT = stdDevAngTop*180/Math.PI;
									
									double CVB = stdVelBot/meanVelBot;
									double VDB = meanVelBot*meanVelBot/sizeBot;
									double AB = stdDevAngBot*180/Math.PI;
									double sizeRatio = (double)sizeTop/(double)sizeBot;
									double sizePerc = (double)sizeTop/(double)size;
									
									
									//Try Ang velocity
									///actual.showImage(prevFrame.submat(newBox));
						
									
									/*
									System.out.println("Top: CV= "+ CVT + " VD= " +VDT+ " Ang= " + AT);
									System.out.println("Bot: CV= "+ CVB + " VD= " +VDB+ " Ang= " + AB );
									System.out.println("diff: CV= "+ Math.abs(CVB-CVT) + " VD= " +Math.abs(VDB-VDT)+ " Ang= " +Math.abs(AB-AT));
									*/

									double[][] hist = velHist.getValues();
									int totalBins = 0;

									for (int m=0; m<hist.length; m++) {
										for (int n=0; n<hist[m].length; n++){
											if(hist[m][n] > 0){
												histHist[m] +=1;
											}

										}
									}
									double histMean = 0;
									double histstdDev = 0;

									for(int n=0; n <histHist.length; n++){
										if(histHist[n] > 0){
											histMean += histHist[n];
											histstdDev +=histHist[n]*histHist[n];
											totalBins += 1;
										}
									}

									histMean /= totalBins;
									histstdDev = Math.sqrt(histstdDev/totalBins - histMean*histMean);






									//
									if(meanVel > denseflow.mean){

										MatOfRect found = new MatOfRect();
										MatOfDouble weight = new MatOfDouble();
										Mat origwindow = new Mat();
										prevFrame.submat(newBox).copyTo(origwindow);
										Mat window = new Mat();
										int minWidth = 48;
										int minHeight = 96;
										int width = 96;
										int height = 192;


										Imgproc.resize(origwindow, window, new Size(width, height));
							
										
											if(stdDevAng < 1){
												Core.rectangle(colourPrevFrame,ntl,nbr,new Scalar(0,255,0));
											}else{
												Core.rectangle(colourPrevFrame,ntl,nbr,new Scalar(255,0,0));
											}
										 

									
									}

									//velHist.histclear();

								}


							}
						}



					}


					//Display the frame
					frame.showImage(prevFrame);

					sub.showImage(diff1);
					Fwindow.showImage(flow);


					diff2.copyTo(diff1);
					threshImage2.copyTo(threshImage1);
					//temp2.copyTo(temp1);
					currentFrame.copyTo(prevFrame);
					contours.clear();
					frameNumber++;


					try {

						Thread.sleep(10); 
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					skipFrame = false;
				}
				else{
					skipFrame = false;
				}

				
			}
			

			System.out.println("Analysis complete!");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		//VideoCapture cap = new VideoCapture();
	}

	static Mat norm_0_255( Mat src) {
		// Create and return normalized image:
		Mat dst = new Mat();
		switch(src.channels()) {
		case 1:
			Core.normalize(src, dst, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
			break;
		case 3:
			Core.normalize(src, dst, 0, 255,  Core.NORM_MINMAX, CvType.CV_8UC3);
			break;
		default:
			src.copyTo(dst);
			break;
		}
		return dst;
	}

	static Mat normLum (Mat src){

		Mat dst = new Mat();
		Mat temp = new Mat();
		List<Mat> planes = new ArrayList<Mat>();

		switch(src.channels()) {
		case 1:
			Core.normalize(src, dst, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
			break;
		case 3:
			Imgproc.cvtColor(src,temp,Imgproc.COLOR_BGR2YUV);
			Core.split(temp, planes);
			Core.normalize(planes.get(0), dst, 0, 255,  Core.NORM_MINMAX, CvType.CV_8UC1);

			planes.set(0, dst);
			//Core.merge(planes, dst);
			Core.merge(planes, temp);

			Imgproc.cvtColor(temp,dst,Imgproc.COLOR_YUV2BGR);

			break;
		default:
			src.copyTo(dst);
			break;
		}

		return dst;

	}

}

