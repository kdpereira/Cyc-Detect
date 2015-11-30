import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import org.opencv.core.*;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.*;
import org.opencv.video.Video;


public class VideoLoader {
	
	private VideoCapture capture;
	private String filepath;
	
	public VideoLoader(){
		
		//System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
		this.filepath = null;
		this.capture  = new VideoCapture();
	}
	
	public VideoLoader(String path) throws IOException{
		
		//System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
		this.filepath = path;
		this.capture = new VideoCapture();
		
		if(capture.open(filepath)){
			
			System.out.println("Successfully loaded file");
			
		}else{
			throw new IOException("Error: could not load file");
		}
		
	}
	
	public VideoCapture getCapture() throws IOException{
		
		//Give a warning if capture video file is not opened
		if(capture.isOpened() == false){
			if((filepath == null) || (capture.open(filepath) == false)){
				throw new IOException("Error: there is no video opened");
			}
		}
			return capture;
		
	}
	
	public void setCapture(VideoCapture capture) {
		this.capture = capture;
	}
	
	public String getFilepath() {
		return filepath;
	}
	public void setFilepath(String path) {
		
		if(capture.isOpened() == true){
			capture.release();
			
		}
		
		this.filepath = path;
		
	}
	
	
	

}
