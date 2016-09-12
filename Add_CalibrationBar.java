// This Plugin takes the cut films and applies a LookUpTable
// also, it changes the image size
// and adds a calibration bar into the free space
// it stacks the films and can make a montage



import ij.*;
import ij.gui.*; // for user interface, e.g. GenericDialog
import ij.io.*; // for FileOpener, OpenDialog 
import ij.plugin.*; // for incorporation of plugins
import ij.plugin.ImagesToStack;
import ij.process.*;
import java.awt.*; // programme crashes otherwise
import java.io.*; // Random Access File
import javax.swing.*; // Java commands like try


public class  Add_CalibrationBar implements PlugIn{
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
		
		/*
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
*/	} //String openFiles()

	
	
	// Adds CalibrationBar to montage and resets Canvas Size for bar to fit
	public void CalibrationBar(ImagePlus imcal) {	
		
		// Initialize
		ImageProcessor ipcal;
		int w,h,zoom;
		String parameters = new String();
		
		ipcal = imcal.getProcessor();
		IJ.run("Reset...", "reset=[Locked Image]");	 // image has to be unlocked			
		
		w = ipcal.getWidth();
		h = ipcal.getHeight();
		zoom = (h-(h%500))/500; // zoomfactor depending on image size
		if (zoom==0) zoom=1; // if image is too small, and zoomfactor is therefor set to zero, CalibrationBar won't be visible
		parameters = "width=" + Math.round ((float) w+zoom*65+50)+ " height=" +h+ " position=Center-Left zero"; // String for parameters to change Canvas Size
		
		IJ.run("Canvas Size...", parameters); // Change Canvas size
		IJ.run("Calibration Bar...", "location=[Upper Right] fill=White label=Black number=5 decimal=2 font=15 zoom="+zoom); // adds CalibrationBar in size depending on Image Size
	} // CalibrationBar
	
	
	
	
/*	// Adds CalibrationBar to Stacked imageses
	public void CalibrationBarStack(ImagePlus imcalstack, String[] titles) {
		//setBatchMode(true); // images aren't displayed during calculation, runs faster
		
		// Initialize
		int nSlices,w,h,zoom; 
		String parameters=new String();
		ImagePlus nobarID;
		ImagePlus barID;
		
		IJ.run("Select None");

		nSlices = imcalstack.getStackSize();
		w = imcalstack.getWidth();
		h = imcalstack.getHeight();
		zoom = (h-(h%500))/500; // zoomfactor depending on image size
		if (zoom==0) zoom=1; // if image is too small, and zoomfactor is therefor set to zero, CalibrationBar won't be visible
		parameters = "width=" + Math.round ((float) w+zoom*65+50)+ " height=" +h+ " position=Center-Left zero"; // String for parameters to change Canvas Size
		
		nobarID = WindowManager.getCurrentImage(); // stack that bar isn't added to
		imcalstack.setSlice(1);
		
		barID=new ImagePlus(); // a new image is created
		barID=IJ.createImage(nobarID.getTitle()+"Bar", "32-Bit black",Math.round ((float) w+zoom*65+50),h,1); // new image with dimensions of ROIs is created
		barID.show();
		IJ.selectWindow(nobarID.getTitle()+"Bar");
//		IJ.run("Paste"); // ROI is pasted into new image
		
		
//		IJ.run("Canvas Size...", parameters); // First run is necessary to create second stack (barID)
//		IJ.run("Calibration Bar...", "location=[Upper Right] fill=White label=Black number=5 decimal=2 font=15 zoom="+zoom); // adds CalibrationBar in size depending on Image Size
		barID = WindowManager.getCurrentImage(); // stack that bar is added to
		
		for (int n=1; n<=nSlices; n++) {
			IJ.selectWindow(nobarID.getTitle());
			imcalstack.setSlice(n);
			IJ.run("Calibration Bar...", "location=[Upper Right] fill=White label=Black number=5 decimal=2 font=15 zoom="+zoom); // // topmost image of the stack is copied into new image and then CalibrationBar is added to it
			IJ.run("Cut"); // new image is cut
			IJ.run("Close"); // and closed
			IJ.selectWindow(barID.getTitle()); // change into stack with bar
			IJ.run("Add Slice"); // new slice is added to that stack
			IJ.run("Paste"); // new image (slice n from stack without bar) is added to stack with bar
		} // for
		IJ.run("Select None");
		imcalstack.setSlice(2); // both stacks are set to the first slice and brought to the top
		IJ.selectWindow(nobarID.getTitle());
		imcalstack.setSlice(2);
		IJ.selectWindow(barID.getTitle());
		//setBatchMode(false);
	} // CalibrationBarStack
*/	

	// Main
	public void run(String arg) {
		IJ.register( Add_CalibrationBar .class);
			
		// Initialize	
		Boolean montage;
		int numberoffiles;
		ImagePlus imagecalib;
		String[]titles;
		String[]savemodes={"Current directory", "Choose directory", "Don't save"}; // Scroll-down for saving options
		String savemode=new String();
		String directory = new String();
		ImageProcessor ip;
		GenericDialog gd;
		
		
		numberoffiles=WindowManager.getImageCount(); // count opened images
		if (numberoffiles==0) {//if no image is opened, dialogue to open images is displayed
			titles=openFiles();
			numberoffiles = titles.length; 
		} // if
		else{ // Names of the opened images are written into the string titles to access them later
			titles=new String[numberoffiles];
			for (int i=0; i<numberoffiles; i++) { // Counts open files
				imagecalib = WindowManager.getCurrentImage();
				titles[i]=imagecalib.getTitle();
				IJ.run("Put Behind [tab]");
			} // for
		} // else
		
		
		gd = new GenericDialog("Enter Parameters", IJ.getInstance()); //Dialogue to get parameters
		if (numberoffiles>1){gd.addCheckbox("Make montage of all images?", true);} // more than 1 image opened --> Add checkbox
		gd.addChoice("Savemode?", savemodes, "Current directory");
		gd.showDialog();
		if (gd.wasCanceled()) return; // User may cancel Plugin
			
		// Initialising and return of user-added parameters for dose calculation
		montage=gd.getNextBoolean(); //returns if montage was checked
		savemode=gd.getNextChoice(); // returns save mode
		
		// Choice where to save the image is made for all images at once; every image is saved seperately (in while-loop)		
		if (savemode.equals("Choose directory")==true) directory = IJ.getDirectory("Directory to save to..."); //case savemode=Choose directory
		else if (savemode.equals("Current directory")==true) directory = IJ.getDirectory("current"); //case savemode=Current directory
		else directory="Empty"; //case savemode=Don't save

		
/*		if (numberoffiles>1) {
			IJ.run("Images to Stack", "title=[]"); // if more than one image is opened Images are stacked
			imagecalib=WindowManager.getImage("Stack");
			imagecalib.setSlice(1);
			IJ.open("C:\\Program Files (x86)\\ImageJ\\luts\\RCF.lut"); // apply LookUpTable
			ip=imagecalib.getProcessor();
			ip.setMinAndMax(0.00, 25.00); // Color Balance is set that image displays full dose range
			IJ.saveAs("Tiff", directory+titles[0].substring(0,titles[0].lastIndexOf("film"))+"_stack.tif");
			
			if (montage==true){
				IJ.run("Make Montage...","use"); // all images are mounted into a montage
				IJ.selectWindow("Montage");
				imagecalib = WindowManager.getCurrentImage();
				CalibrationBar(imagecalib); // Adds CalibrationBar to montage and resets Canvas Size for bar to fit
			
				IJ.selectWindow("Montage");
				IJ.run("Close");
				
				IJ.selectWindow("Montage with bar");
				IJ.saveAs("Tiff", directory+titles[0].substring(0,titles[0].lastIndexOf("film"))+"_montage.tif");
			} // if
		} // if
	
			else{ // only one image is opened
*/			IJ.selectWindow(titles[0]);	
			imagecalib = WindowManager.getCurrentImage();
			IJ.open("C:\\Program Files (x86)\\ImageJ\\luts\\RCF.lut"); // apply LookUpTable
			ip=imagecalib.getProcessor();
			ip.setMinAndMax(0.00, 25.00); // Color Balance is set that image displays full dose range
			CalibrationBar(imagecalib); // Adds CalibrationBar to image and resets Canvas Size for bar to fit
			IJ.selectWindow(titles[0]);
			IJ.run("Close");
			IJ.saveAs("Tiff", directory+titles[0].substring(0,titles[0].lastIndexOf("."))+"_lut.tif");
		} // else
		
	} // void run
} // class Dose_Calculation implements PlugIn