// This Plugin is able to cut and rotate a scan with several RCF Films 
// into images containing single films and stacking them as they were during the experiment
// It takes a 16 Bit Unsigned .tif and saves 16 Bit Unsigned .tif's 
// (works with other images as well, except RGB)
// The user spezifies the size of the matrix of films
// then, he specifies three sample ROI's from which the cutting lines are calculated
// He may change and/or delete ROI's afterwards
// 
// In a second step, the user has to define the line that is marked by the clip 
// and the dark mark left by the hole in the film on each image
// orientation relatively to the laserbeam is calculated from that
// and the films are shifted, so that they can be stacked
// as they were during the experiment



import ij.*;
import ij.gui.*; // for user interface, e.g. GenericDialog
import ij.io.*; // for FileOpener, OpenDialog 
import ij.plugin.*; // for incorporation of plugins
import ij.plugin.frame.RoiManager; // somehow, this one is necessary
import ij.process.*; // ImageProcessor
import java.awt.*; // programme crashes otherwise
import java.io.*; // Random Access File
import javax.swing.*; // Java commands like try


public class Cut_Film_LineRoi implements PlugIn{
	static File dir; // directory that images are saved to
		
	public String[] openFiles() {
		
		// Initialize
		String[] titles;
		String sdir;
		int returnVal;
		Opener opener = new Opener();
		JFileChooser fc = null;
		String path;
		File[] files;
		
		
		// Check if the ImageJ version is able to handle multiple Files etc.
		try {fc = new JFileChooser();}
		catch (Throwable e) { // error if plugin doesn't work with installed java version
			IJ.error("This plugin requires Java 2 or Swing.");
			titles=new String[1];
			titles[0]="empty";
			return titles; // number of open files is zero because files couldn't be opened
		} // catch
		
		fc.setMultiSelectionEnabled(true); // more than one file can be opened
		if (dir==null) { // directory must be set, dir mustn't point to null
			sdir = OpenDialog.getDefaultDirectory();
			if (sdir!=null) dir = new File(sdir); // directory is set
		} // if
		else;
		
		if (dir!=null) fc.setCurrentDirectory(dir); // directory was set before or is set now
		else;
		
		returnVal = fc.showOpenDialog(IJ.getInstance()); // Dialogue to open multiple images
		if (returnVal!=JFileChooser.APPROVE_OPTION) {
			titles=new String[1];
			titles[0]="empty";
			return titles; // JFileChooser is set to APPROVE_OPTION if user selected files successfully, otherwise (user cancelled or error occured) zero files were opened
		} // if
		else;
		
		files = fc.getSelectedFiles(); // Selected Files are saved as an array
		
		// getSelectedFiles does not work on some JVMs
		if (files.length==0) {
			files = new File[1]; 
			files[0] = fc.getSelectedFile(); // so only one file is opened
		} // if
		else;

		// Files that were selected are opened and their names are written into the String titles
		path = fc.getCurrentDirectory().getPath()+Prefs.getFileSeparator();
		titles=new String[files.length];
		for (int i=0; i<files.length; i++) { // Opens and counts selected files
			ImagePlus img = opener.openImage(path, files[i].getName());
			titles[i]=img.getTitle();
			if (img!=null) img.show();
		} // for 
		return titles;
	} //int openFiles()
	
	
	
	// Cuts the scan with the films into single films
	public void cutting(int nx, int ny, String directory, String[] titles, String titletiff){
		
		//Initialize
		ImagePlus implus=WindowManager.getCurrentImage();
		ImagePlus newimplus;
		RoiManager rectman=new RoiManager();
		Roi[] Rois;
		Rectangle[][] r = new Rectangle[nx][ny]; // Array of Rectangles to save ROIs
		Toolbar tool = new Toolbar();
		WaitForUserDialog samplerois,showall;
		int x1,y1,width,height,xdist,ydist=0;
		String bdepth=new String();
		int nfilm;
		
		
		// User has to define 3 sample ROIs from which suggestions for cutting is calculated
		tool.setTool(0); // Rectangle is set in the toolbar
		samplerois=new WaitForUserDialog("Select ROIs", "Select ROI in first film in top-row, then press OK.");
		samplerois.show();
		r[0][0]=implus.getRoi().getBounds();
		samplerois.close();
		samplerois=new WaitForUserDialog("Select ROIs", "Select ROI in second film in top-row, then press OK.");
		samplerois.show();
		r[1][0]=implus.getRoi().getBounds(); 
		samplerois.close();
		samplerois=new WaitForUserDialog("Select ROIs", "Select ROI in first film in second row, then press OK.");
		samplerois.show();
		r[0][1]=implus.getRoi().getBounds();
		samplerois.close();
		
		// Position Size and Distance of the Rois are caluclated as average of the user defined sample ROIs
		x1=(r[0][0].x+r[0][1].x)/2;
		y1=(r[0][0].y+r[1][0].y)/2;
		width=(int)(r[0][0].width+r[1][0].width+r[0][1].width)/3;
		height=(int)(r[0][0].height+r[1][0].height+r[0][1].height)/3;
		xdist=r[1][0].x-r[0][0].x;
		ydist=r[0][1].y-r[0][0].y;
		
		// The ROIs are added to ROI manager (for optional changes later) and written into the array of rectangles
		for (int j=0; j<ny; j++){ 			
			for (int i=0; i<nx; i++){	
				implus.setRoi(x1+i*xdist, y1+j*ydist, width, height); // ROI is set
				r[i][j] = implus.getRoi().getBounds(); // Added to array
				rectman.add(implus, implus.getRoi(), r[i][j].x); // Added to ROI manager
			}
		}
		
		showall=new WaitForUserDialog("Selection", "Please press the Show All Button in the ROI Manager. You may change or delete ROIs. Then, press OK.");
		showall.show(); // manager.runCommand("Select All"); // doesn't seem to work yet, maybe not implemented?
		
		Rois=rectman.getRoisAsArray(); // ROIs from the manager are returned as array
			
		nfilm=rectman.getCount();
		for (int n=1; n<=nfilm;n++){
			int helpy=n%nx; // needed to calculate position in 2-dimensional array
			if (helpy==0) helpy=nx;
			int helpx=(n-helpy)/nx+1;
			r[(helpy-1)][(helpx-1)]=Rois[(n-1)].getBounds(); // array with old ROIs is over written with corrected ROIs
		} // for

		nfilm=rectman.getCount();
		for (int n=1; n<=nfilm;n++){
			IJ.selectWindow(titles[0]);
			int helpy=n%nx;
			if (helpy==0) helpy=nx;
			int helpx=(n-helpy)/nx+1;
			implus.setRoi(r[(helpy-1)][(helpx-1)]);
			IJ.run("Copy"); // every selected ROI is copied
			
			if (implus.getBitDepth()==24) bdepth="RGB"; // Bit Depth of the image is checked, that new images get the same BitDepth as the original one
			else bdepth=implus.getBitDepth()+"-Bit";
			
			newimplus=new ImagePlus(); // a new image is created
			newimplus=IJ.createImage(titletiff+"film"+String.format("%02d",n)+".tif", bdepth + "black",r[(helpy-1)][(helpx-1)].width,r[(helpy-1)][(helpx-1)].height,1); // new image with dimensions of ROIs is created
			newimplus.show();
			IJ.selectWindow(titletiff+"film"+String.format("%02d",n)+".tif");
			IJ.run("Paste"); // ROI is pasted into new image
		} // for

		IJ.selectWindow(titles[0]);
		IJ.run("Close");
	} // cutting
	
	
	// rotate cut scans so errors in positioning them on the scanner are corrected
	public void rotate(int nfilm, String directory, String[] titles, String titletif){
	
		// Initialize
		double scalprod,absval,angle=0; // uses for calculation of the relative rotation angle
		ImagePlus implus=WindowManager.getImage(titletif+"film"+String.format("%02d",nfilm)+".tif");
		ImageProcessor ip;
		ImageStatistics stats;
		double rotall=0; // angle for absolute rotation
		double[][] results = new double[nfilm][2]; // Array containg coordinates of centres of mass of ROIs around the holes
		Rectangle clip=new Rectangle(); // Rectangle containing the final ROI that all the films are cropped to
		Toolbar tool = new Toolbar(); // accessing the ImageJ toolbar
		WaitForUserDialog wfud;
		Line vertline;
		
		for (int n=0; n<nfilm; n++){
			if (nfilm>1) IJ.selectWindow(titletif+"film"+String.format("%02d",(nfilm-n))+".tif"); // films are choosen backwards for user to define ROIs for rotation
      		implus=WindowManager.getCurrentImage(); // if only one film, that one is the active image
			
			
			implus.killRoi();
			
			tool.setTool(4); // LineRoi is set in the toolbar
			wfud=new WaitForUserDialog("Mark edge of clip with a line ROI, then press OK.");
			wfud.show();
			vertline = (Line) implus.getRoi();
			
			rotall=Math.acos((vertline.y2d-vertline.y1d)/(Math.sqrt(Math.pow((vertline.x2d-vertline.x1d),2)+Math.pow((vertline.y2d-vertline.y1d),2))))*360/(2*Math.PI);
			if(vertline.x1d>vertline.x2d) rotall=(-rotall);
			wfud.close();
			implus.killRoi();
			
			
			if (nfilm>1) IJ.selectWindow(titletif+"film"+String.format("%02d",(nfilm-n))+".tif"); // films are choosen backwards for user to define ROIs for rotation
			IJ.run("Duplicate...", titletif+"film"+String.format("%02d",(nfilm-n))+"_dupl.tif"); // originial images mustn't be changed
    
			implus=WindowManager.getCurrentImage();
			ip=implus.getProcessor();
			IJ.setThreshold(0,0,"Black & White"); // Threshold makes selection of ROI easier
			IJ.run("Make Binary"); // Binary so film is only white, holes are only black

			tool.setTool(8); // Wand is set in the toolbar
			wfud=new WaitForUserDialog("Select hole using the wand tool.");
			wfud.show();
			
			stats=implus.getStatistics(64);
			wfud.close();
			results[n][0]=stats.xCenterOfMass; // center of mass of each hole is used as reference point for rotation
			results[n][1]=stats.yCenterOfMass;
			
			
			implus.killRoi();
			IJ.run("Close");
			if (nfilm>1) IJ.selectWindow(titletif+"film"+String.format("%02d",(nfilm-n))+".tif"); // original film is selected again
			else IJ.selectWindow(titletif+".tif"); // if only one film, name hasn't been changed
			implus=WindowManager.getCurrentImage();
			

			IJ.run("Arbitrarily...", "angle="+(rotall)); // rotation relatively to each other
			
			// cut all images to the same size and center on hole in the middle
			implus.setRoi((int)(results[n][0]-results[0][0]+0.05*ip.getWidth()), (int)(results[n][1]-results[0][1]+0.05*ip.getHeight()), (int)(0.9*ip.getWidth()), (int)(0.9*ip.getHeight())); // cut all images to the same size
			IJ.run("Crop");
			if (nfilm>1) IJ.saveAs("Tiff", directory+titletif+"film"+String.format("%02d",(nfilm-n))+".tif");
			else IJ.saveAs("Tiff", directory+titletif+"_rot.tif");
		} // for
	} // rotate
	
	
	
	
	
	// Main
	public void run(String arg) {
		IJ.register( Cut_Film_LineRoi .class);
		
		// Initialize
		int numberoffiles;
		String[]savemodes={"Current directory", "Choose directory"}; // Scroll-down for saving options
		double xdir,ydir; // number of films on the scan
		GenericDialog gd;
		String savemode=new String(); // contains choosen saving option
		String[] titles; // contains the titles of the images for accessing them
		String titlewotif; // for saving image name without the file extension
		WaitForUserDialog check;
		ImagePlus img;

		numberoffiles=WindowManager.getImageCount(); // count opened images
		if (numberoffiles==0) {//if no image is opened, dialogue to open images is displayed
			titles=openFiles();
			numberoffiles = titles.length; 
		} // if
		else{ // Names of the opened images are written into the string titles to access them later
			titles=new String[numberoffiles];
			for (int i=0; i<numberoffiles; i++) { // Counts open files
				img = WindowManager.getCurrentImage();
				titles[i]=img.getTitle();
				IJ.run("Put Behind [tab]");
			} // for
		} // else
		titlewotif = titles[0].substring(0,titles[0].lastIndexOf(".")); // Name of Image without file extenstion
		
		gd = new GenericDialog("Info about scan", IJ.getInstance()); // Scan Geometry for Cutting of the films
		gd.addMessage("Enter Number of Films on the scan");
		gd.addNumericField("x-Direction",3,0); // Number of Films in x-direction
		gd.addNumericField("y-Direction",3,0); // Number of Films in x-direction
		gd.addChoice("Savemode?", savemodes, "Current directory");
		gd.showDialog();
		if (gd.wasCanceled()) return; // User may cancel Plugin
		xdir = gd.getNextNumber();
		ydir = gd.getNextNumber();
		savemode=gd.getNextChoice(); // returns save mode
		
		if(xdir%(int)xdir!=0 || ydir%(int)ydir!=0){ // Check if entered numbers are Integers
			check = new WaitForUserDialog("Error","Number of films must be integer value!");
			check.show();
			return;
		} // if
		else;
		
		// Choice where to save the image is made for all images at once; every image is saved seperately (in while-loop)		
		String dir = new String();
		if (savemode.equals("Choose directory")==true) dir = IJ.getDirectory("Directory to save cut films to..."); //case savemode=Choose directory
		else if (savemode.equals("Current directory")==true) dir = IJ.getDirectory("current"); //case savemode=Current directory
		
		
		// if number of films in both directions is greater than 1, images are cut		
		if (xdir>1 & ydir>1) cutting((int)xdir,(int)ydir, dir, titles, titlewotif);
		else;
	
		numberoffiles=WindowManager.getImageCount(); // count opened images
		rotate(numberoffiles, dir, titles, titlewotif);

	} // void run
} // class Cut_Film implements PlugIn