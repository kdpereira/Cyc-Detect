import java.util.List;

import org.opencv.core.*;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

public class FarnFlow {

	//Farneback Args
	Mat flow = new Mat();
	double pyr_scale;
	int levels;
	int winsize2;
	int iterations;
	int poly_n;
	double poly_sigma;
	double max;
	double mean; 
	double stdDev;
	double stdDevThresh = 0.07;
	int step;
	private Mat prevImage = new Mat();
	private Mat currImage= new Mat();
	private int flags;
	
	public FarnFlow(){
		
		pyr_scale = 0.5;
		levels = 5;
		winsize2 = 15;
		iterations = 3;
		poly_n = 5;
		poly_sigma = 1.2;
		max = 0;
		mean = 0;
		step = 1;
		flags = 0;
	}
	
	/*
	Video.calcOpticalFlowFarneback(prevImage, currImage, flow,
			pyr_scale, levels, winsize2, iterations,
			poly_n, poly_sigma, flags);
			*/
	
	
	/*
	for(int y=0; y <currentFrame.rows();y +=step ){
		for(int x=0; x<currentFrame.cols(); x+=step){
			
			v.set(flow.get(y, x));
			p.x = x;
			p.y = y;
			q.x = (int) (x + v.x);
			q.y = (int) (y + v.y);
			
			double mag; mag = Math.sqrt( Math.pow((v.x),2) + Math.pow((v.y),2));
			double angle; angle = Math.atan2( (double) v.y, (double) v.x );
			
			double h = 0; //= angle*180/(Math.PI/2);
			//double v = 
			if(max < mag)
				max = mag;
			
			double[] rgb = new double[3];
			HSVtoRGB(rgb, h ,mag/5);
			
			Core.line(currentFrame, p, q, new Scalar(255*rgb[2],255*rgb[1],255*rgb[0]));
		}
	}
	
*/
	
	public Mat getFlow(Mat prevFrame, Mat currentFrame, Mat vel){
		
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
		Video.calcOpticalFlowFarneback(prevImage, currImage, flow,
				pyr_scale, levels, winsize2, iterations,
				poly_n, poly_sigma, flags);

		
		
		Mat vflow = drawFlow(currentFrame);
		flow.copyTo(vel);
		
		 return vflow;
	}
	
	private Mat drawFlow(Mat currentFrame) {

		Mat temp = currentFrame.clone();
		Mat optFlow = new Mat();
		Imgproc.cvtColor(temp,optFlow,Imgproc.COLOR_GRAY2RGB);
		
		
		//Points for drawing optical flow lines
		Point p = new Point();
		Point q = new Point();
		Point v = new Point();
		max = 0;
		mean=0;
		int numPix;
		
		//Calculate std dev and mean velocity of field
		for(int y=0; y <optFlow.rows();y+= step ){
			for(int x=0; x<optFlow.cols(); x+= step){
				v.set(flow.get(y, x));
				
				double mag; mag = Math.sqrt( Math.pow((v.x),2) + Math.pow((v.y),2));
				
				mean += mag;
				stdDev += mag*mag;
				if(max < mag){
					max = mag;
				}
				double angle; angle = Math.atan2( (double) v.y, (double) v.x );
			
				
			}
		}
		numPix = (optFlow.rows()*optFlow.cols()/step);
		mean = mean/numPix;
		stdDev = Math.sqrt(stdDev/numPix - mean*mean);
		

	//	System.out.println("mean= " + mean + " Std = " + stdDev);
		
		//Draw velocity field on frame
		for(int y=0; y <optFlow.rows();y+= step ){
			for(int x=0; x<optFlow.cols(); x+= step){
				
				v.set(flow.get(y, x));
				p.x = x;
				p.y = y;
				q.x = (int) (x + v.x);
				q.y = (int) (y + v.y);
				
				double mag; mag = Math.sqrt( Math.pow((v.x),2) + Math.pow((v.y),2));
				double angle; angle = Math.atan2( (double) v.y, (double) v.x );
				double val = Math.sqrt(mag);
				//angle = mag*360/max;
				
				
				
				double h = angle*180/(Math.PI/2);
				//double v = 
				/*
				if(max < mag){
					max = mag;
					System.out.println("New max is:" + max);
				}*/
				
				double[] rgb = new double[3];
				
				//Threshold velocity
				if((mag > mean)){
					HSVtoRGB(rgb, 1 ,mag/(mean+stdDev));
				}else{
					HSVtoRGB(rgb, 1 ,mag/(mean+1.5*stdDev));
				}
				
				
				//Draw red veloctiy field line on frame
				Core.line(optFlow, p, q, new Scalar(255*rgb[2],255*rgb[1],255*rgb[0]));
			}
			
		}
		
		return optFlow;
	}
	
	//Used to colour velocities of different angles using hue
	private void HSVtoRGB( double rgb[], double h, double v )
	{
		int i;
		double f, p, q, t;
		if(v > 1.0){
			v = 1.0;
		}

		h /= 60;			// sector 0 to 5
		i = (int) Math.floor( h );
		f = h - i;			// factorial part of h
		p = 0;
		q = v * ( 1 - f );
		t = v * f;

		switch( i ) {
			case 0:
				rgb[0] =  v;
				rgb[1] =  t;
				rgb[2] =  p;
				break;
			case 1:
				rgb[0] = q;
				rgb[1] = v;
				rgb[2] = p;
				break;
			case 2:
				rgb[0] = p;
				rgb[1] = v;
				rgb[2] = t;
				break;
			case 3:
				rgb[0] = p;
				rgb[1] = q;
				rgb[2] = v;
				break;
			case 4:
				rgb[0] = t;
				rgb[1] = p;
				rgb[2] = v;
				break;
			default:		// case 5:
				rgb[0] = v;
				rgb[1] = p;
				rgb[2] = q;
				break;
		}

	}
			
}
