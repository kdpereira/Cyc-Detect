import java.util.List;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

public class LKFlow {

	//LKParameters
	Size winSize;
	TermCriteria criteria;
	MatOfByte status;
	int maxLevel;
	MatOfFloat err;
	int flags;
	double minEigThreshold;

	//Feature Detector parameters
	MatOfPoint currCorners = new MatOfPoint();
	MatOfPoint prevCorners = new MatOfPoint();
	int numberOfCorners;
	double qualityLevel;
	double minDistance;

	//Frames
	//Mat currFrame;
	Mat tempFrame;
	private Mat image = new Mat();
	private boolean initialised = false;
	private MatOfPoint2f prevFeatures = new MatOfPoint2f();
	private Mat prevImage = new Mat();
	private Mat currImage = new Mat();
	private MatOfPoint2f currFeatures = new MatOfPoint2f();
	private List<Point> prevFeatureList;
	private List<Point> currFeatureList;

	public LKFlow(){

		//LK
		winSize = new Size(15.0, 15.0);
		criteria = new TermCriteria(TermCriteria.COUNT+TermCriteria.EPS,20,0.3);
		status = new MatOfByte();
		maxLevel = 4;
		err = new MatOfFloat();
		flags = 0;
		minEigThreshold = 0.1;

		//Detector
		numberOfCorners = 400;
		qualityLevel = 0.1;
		minDistance = 0.1;
	}

	public LKFlow(Size winSize, TermCriteria criteria, int maxLevel, int flags,
			double minEigThreshold) {
		super();
		this.winSize = winSize;
		this.criteria = criteria;
		this.maxLevel = maxLevel;
		this.flags = flags;
		this.minEigThreshold = minEigThreshold;
	}

	private MatOfPoint getCorners(Mat image, MatOfPoint corners){

		Imgproc.goodFeaturesToTrack(image, corners, numberOfCorners, qualityLevel, minDistance);

		return corners;

	}

	public Mat getFlow(Mat prevFrame, Mat currentFrame, Mat dest){

		if(prevFrame.channels() > 1){
			Imgproc.cvtColor(prevFrame,prevImage,Imgproc.COLOR_RGB2GRAY);
			
		}else{
			prevImage = prevFrame.clone();
		}
		
		if(currentFrame.channels() > 1){
			Imgproc.cvtColor(currentFrame,currImage,Imgproc.COLOR_RGB2GRAY);
			
		}else{
			currImage = currentFrame.clone();
		}


			prevCorners = getCorners(prevImage, prevCorners);
			//Need to change MatOfPoints to MatOfPoints2f for optic flow
			prevFeatures.fromArray(prevCorners.toArray());


		Video.calcOpticalFlowPyrLK(prevImage, currImage, prevFeatures,currFeatures, 
				status, err, winSize, maxLevel, criteria, flags, minEigThreshold);
		prevFeatureList = prevFeatures.toList();
		currFeatureList = currFeatures.toList();
		
		
		
		Mat flow = drawFlow(dest);
		
		
		
		/*
		if(prevFeatureList.size() > currFeatureList.size()){
			
			prevCorners = getCorners(currImage, prevCorners);
			prevFeatures.fromArray(prevCorners.toArray());
			prevFeatureList = prevFeatures.toList();
			
		}
		*/
		
	//	currFeatures.copyTo(prevFeatures);
		 
		 return flow;
	}

	private Mat drawFlow(Mat currentFrame) {

		Mat temp = currentFrame.clone();
		Mat optFlow = new Mat();
		Imgproc.cvtColor(temp,optFlow,Imgproc.COLOR_GRAY2RGB);
		
		List<Byte>found = status.toList();

		//Points for drawing optical flow lines
		Point p = new Point();
		Point q = new Point();
		
		Scalar lineColour = new Scalar(0, 0, 255);
		
		//Draw red lines to represent the flow		
		for(int i = 0; i < currFeatureList.size(); i++){

			if(found.get(i) == 1){
				p.x = prevFeatureList.get(i).x;
				p.y = prevFeatureList.get(i).y; 
				q.x = currFeatureList.get(i).x;
				q.y = currFeatureList.get(i).y;

				//Use the coordinates to draw the line 
				double angle; angle = Math.atan2((double) p.y - q.y, (double) p.x - q.x );
				double mag; mag = Math.sqrt( Math.pow((p.y - q.y),2) + Math.pow((p.x - q.x),2));

				//q.x = (int) (p.x -  mag * Math.cos(angle));
				//q.y = (int) (p.y -  mag * Math.sin(angle));
				Core.line(optFlow, p, q, lineColour);
				
				/*
				//Draw arrow head on line
				p.x = (int) (q.x + 9 * Math.cos(angle + Math.PI / 4));
				p.y = (int) (q.y + 9 * Math.sin(angle + Math.PI / 4));
				Core.line( optFlow, p, q, lineColour);
				p.x = (int) (q.x + 9 * Math.cos(angle - Math.PI / 4));
				p.y = (int) (q.y + 9 * Math.sin(angle - Math.PI / 4));
				Core.line( optFlow, p, q, lineColour);
				*/
				//Display features on the video as green
				//if(i < currFeatureList.size())
				
				if(mag <0){
					Core.circle(optFlow, prevFeatureList.get(i), 1, new Scalar(255, 0, 0), Core.FILLED);
				}else{
					Core.circle(optFlow, prevFeatureList.get(i), 1, new Scalar(0, 255, 0), Core.FILLED);
				}
			}

		}
		
		return optFlow;
	}

}
