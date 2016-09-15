// This Plugin converts the Grey values on a RCF film into Dose values
// It takes an 24-Bit RGB .tiff and uses the signal on the red channel
// Saves the image as 32-Bit Float .tiff



import ij.*;
import ij.gui.*; // for user interface, e.g. GenericDialog
import ij.io.*; // for FileOpener, OpenDialog 
import ij.plugin.*; // for incorporation of plugins
import ij.process.*;
import java.awt.*; // programme crashes otherwise
import java.io.*; // Random Access File
import javax.swing.*; // Java commands like try


public class Dose_Calculation_red implements PlugIn{
	static File dir; // directory that images are saved to
	
	// Function to determine number of opened files
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

	
	
	
	// Calculates the dose from the red values in the image
	public void CalculationOfDosestack(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		int stak = implus.getImageStackSize();
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		
		IJ.run("Duplicate...", "");	// Make a duplicate of the original input image for later manipulation
		IJ.run("Split Channels"); // Seperates RGB channels of the image
	
		IJ.selectWindow(title + "-1 (green)"); // Green channel is selected as active window
		IJ.run("Close"); // Closes Green channel
		IJ.selectWindow(title + "-1 (blue)"); // Blue channel is selected as active window
		IJ.run("Close"); // Closes Blue channel
				
		// From now on, only Red channel is used (Scanner sensitivity is highest)
		implus = WindowManager.getImage(title+"-1 (red)"); // reference needs to be renewed, as the image changed from xxx.tif to xxx (red).tif, otherwise nullpointerexception 
		ip = implus.getProcessor(); // ImageProcessor contains the modify.pixel operations
		
		
		newimplus=new ImagePlus(); // a new image with 32 Bit is created to save the calculated Dose values in
		newimplus=IJ.createImage("Dose of "+title, "32-Bit Black",ip.getWidth(),ip.getHeight(), stak); // new image with 32 Bit, black background, dimensions of original image and one layer
		
		newimplus.show();
		IJ.selectWindow("Dose of "+title); // selects new image
		newip=newimplus.getProcessor().convertToFloat(); // FloatProcessor is needed to write float values into the image
	for(int z=0; z<= stak; ++z){
		implus.setSlice(z);
		newimplus.setSlice(z);
		for (int y=0; y <= ip.getHeight(); y++){ // Calculates dose from red values in the image for every Pixel
			for (int x=0; x <= ip.getWidth(); x++){
				value = (double) ip.getPixel(x,y);
				if (value>unexp) value=unexp; // Pixel is brighter than unexposed film: unphysical, therefore set to background value. in that case, netOD becomes 0
				netOD = Math.log10((unexp-bckg)/(value-bckg)); // calculation of optical density
				Dosis = p1*netOD+p2*Math.pow(netOD, p3); // calculation of dose from optical density
				if (Dosis>maxdos) newip.putPixelValue (x, y, maxdos); // if dose is too large, it is set to maximum dose
				else newip.putPixelValue (x, y, Dosis);
				}
			} // for
		} // for
		
		IJ.selectWindow(title+"-1 (red)");
		IJ.run("Close"); // original image is closed
		IJ.run("Set Scale...", "distance=117 known=1 pixel=1 unit=cm global");
		IJ.run("Color Balance..."); // run Color Balance, otherwise image appears simply black
		IJ.selectWindow("Color");
		IJ.run("Close"); // close Color Balance window
		IJ.run("Brightness/Contrast...");
		IJ.resetMinAndMax();
		IJ.selectWindow("B&C");
		IJ.run("Close"); // close B&C window
		IJ.run("RCF-Rainbow");
		//IJ.run("Duplicate...", "Dose of "+title+"+Bars");
		IJ.run("Calibration Bar...", "location=[Lower Left] fill=Black label=White number=5 decimal=0 font=16 zoom=1 bold overlay");
		//IJ.run("RGB Color");
		IJ.run("Scale Bar...", "width=1 height=2 font=16 color=White background=None location=[Lower Right] bold overlay");
		if (directory!="Empty"){ // Every single image is saved
			IJ.saveAs("Tiff", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDose







// Calculates the dose from the red values in the image
	public void CalculationOfDose(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title){

		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		
		IJ.run("Duplicate...", " ");	// Make a duplicate of the original input image for later manipulation
		IJ.run("Flip Horizontally");
		
		IJ.selectWindow(title); // Original image is selected as active window
		IJ.run("Split Channels"); // Seperates RGB channels of the image
	
		IJ.selectWindow(title + " (green)"); // Green channel is selected as active window
		IJ.run("Close"); // Closes Green channel
		IJ.selectWindow(title + " (blue)"); // Blue channel is selected as active window
		IJ.run("Close"); // Closes Blue channel
				
		// From now on, only Red channel is used (Scanner sensitivity is highest)
		implus = WindowManager.getImage(title+" (red)"); // reference needs to be renewed, as the image changed from xxx.tif to xxx (red).tif, otherwise nullpointerexception
		ip = implus.getProcessor(); // ImageProcessor contains the modify.pixel operations
		
		newimplus=new ImagePlus(); // a new image with 32 Bit is created to save the calculated Dose values in
		newimplus=IJ.createImage("Dose of "+title, "32-Bit black",ip.getWidth(),ip.getHeight(),1); // new image with 32 Bit, black background, dimensions of original image and one layer
		newimplus.show();
		IJ.selectWindow("Dose of "+title); // selects new image
		newip=newimplus.getProcessor().convertToFloat(); // FloatProcessor is needed to write float values into the image
	
		for (int y=0; y <= ip.getHeight(); y++){ // Calculates dose from red values in the image for every Pixel
			for (int x=0; x <=ip.getWidth(); x++){
				value = (double)ip.getPixel(x,y);
				if (value>unexp) value=unexp; // Pixel is brighter than unexposed film: unphysical, therefore set to background value. in that case, netOD becomes 0
				netOD = Math.log10((unexp-bckg)/(value-bckg)); // calculation of optical density
				Dosis = p1*netOD+p2*Math.pow(netOD, p3); // calculation of dose from optical density
				if (Dosis>maxdos) newip.putPixelValue (x, y, maxdos); // if dose is too large, it is set to maximum dose
				else newip.putPixelValue (x, y, Dosis);
			} // for
		} // for
		
		IJ.selectWindow(title+" (red)");
		IJ.run("Close"); // original image is closed
		IJ.run("Set Scale...", "distance=117 known=1 pixel=1 unit=cm global");
		IJ.run("Color Balance..."); // run Color Balance, otherwise image appears simply black
		IJ.selectWindow("Color");
		IJ.run("Close"); // close Color Balance window
		IJ.run("Brightness/Contrast...");
		IJ.resetMinAndMax();
		IJ.selectWindow("B&C");
		IJ.run("Close"); // close B&C window
		IJ.run("RCF-Rainbow");
		//IJ.run("Duplicate...", "Dose of "+title+"+Bars");
		IJ.run("Calibration Bar...", "location=[Lower Left] fill=Black label=White number=5 decimal=0 font=16 zoom=1 bold overlay");
		//IJ.run("RGB Color");
		IJ.run("Scale Bar...", "width=1 height=2 font=16 color=White background=None location=[Lower Right] bold overlay");
		if (directory!="Empty"){ // Every single image is saved
			IJ.saveAs("Tiff", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDose

	

	
	
	// Main
	public void run(String arg) {
		IJ.register( Dose_Calculation_red .class);
		
		// Initialize
		int numberoffiles; // number of files that are to be processed
		double p1,p2,p3,bckg,unexp,maxdos; // parameters for the calibration curve
		String[] savemodes={"Current directory", "Choose directory", "Don't save"}; // Scroll-down for saving options
		String[] titles; // String containing the titles of the images
		String savemode=new String(); // contains the choosen saving option
		String directory = new String(); // contains the directory that the image will be saved to
		GenericDialog gd; // interaction with the user
		ImagePlus img; // needed for accessing the opened images
		
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
		
		
		//Dialogue to get parameters and options
		gd = new GenericDialog("Enter Parameters RED Channel", IJ.getInstance()); 
		
		switch (numberoffiles){
			case 1: 
			String[] names1 = {titles[0]};
			gd.addChoice("Which files?", names1, titles[0]);
			break;
			
			case 2: 
			String[] names2 = {titles[0], titles[1], "all"};
			gd.addChoice("Which files?", names2, "all");
			break;
			
			case 3: 
			String[] names3 = {titles[0], titles[1], titles[2],"all"};
			gd.addChoice("Which files?", names3, titles[0]);
			break;
			
			case 4: 
			String[] names4 = {titles[0], titles[1], titles[2],titles[3],"all"};
			gd.addChoice("Which files?", names4, titles[0]);
			break;
			
			case 5: 
			String[] names5 = {titles[0], titles[1], titles[2],titles[3],titles[4],"all"};
			gd.addChoice("Which files?", names5, titles[0]);
			break;
			
			case 6: 
			String[] names6 = {titles[0], titles[1], titles[2],titles[3],titles[4],titles[5],"all"};
			gd.addChoice("Which files?", names6, titles[0]);
			break;
			
			case 7: 
			String[] names7 = {titles[0], titles[1], titles[2],titles[3],titles[4],titles[5],titles[6],"all"};
			gd.addChoice("Which files?", names7, titles[0]);
			break;		
		}
		
		gd.addCheckbox("Are the chosen files Stacks?", false);
		
		gd.addMessage("Please insert your parameters for calibration");
		//parameters from C.Richter Kalibrierung (5MeV), eingefuegt 07062011
		gd.addNumericField("p1", 8.50457, 5); // Suggestions for parameters
		gd.addNumericField("p2", 16.92152, 5);
		gd.addNumericField("p3", 2.40561, 3);
		gd.addMessage("Please insert your constant parameters");
		gd.addNumericField("Dark current of the scanner", 0, 2);
		gd.addNumericField("Transparency of the unexposed film", 204, 2);
		gd.addNumericField("Maximum dose film can detect", 40, 0);
		gd.addChoice("Savemode?", savemodes, "Don't save");
		gd.showDialog();
		if (gd.wasCanceled()) return; // User may cancel Plugin
		
		// Return of user-added parameters and options for dose calculation
		p1 =  gd.getNextNumber();
		p2 =  gd.getNextNumber();
		p3 =  gd.getNextNumber();
		bckg = gd.getNextNumber();
		unexp =  gd.getNextNumber();
		maxdos =  gd.getNextNumber();
		boolean stacky = gd.getNextBoolean();
		String imagename = gd.getNextChoice();
		
		savemode=gd.getNextChoice();
				
		// Choice where to save the images
		if (savemode.equals("Choose directory")==true) directory = IJ.getDirectory("Directory to save to..."); //case savemode=Choose directory
		else if (savemode.equals("Current directory")==true) directory = IJ.getDirectory("current"); //case savemode=Current directory
		else directory="Empty"; //case savemode=Don't save


	if (stacky == true){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDosestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1]); // Calculates dose for every opened image
			i++;
		} // while
		}
		else if (imagename == titles[0]){IJ.selectWindow(titles[0]);
			CalculationOfDosestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[0]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[1]){IJ.selectWindow(titles[1]);
			CalculationOfDosestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[1]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[2]){IJ.selectWindow(titles[2]);
			CalculationOfDosestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[2]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[3]){IJ.selectWindow(titles[3]);
			CalculationOfDosestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[3]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[4]){IJ.selectWindow(titles[4]);
			CalculationOfDosestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[4]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[5]){IJ.selectWindow(titles[5]);
			CalculationOfDosestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[5]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[6]){IJ.selectWindow(titles[6]);
			CalculationOfDosestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[6]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[7]){IJ.selectWindow(titles[7]);
			CalculationOfDosestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[7]); // Calculates dose for every opened image}	
		}
		else{}
	}
	else{	
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDose(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1]); // Calculates dose for every opened image
			i++;
		} // while
		}
		else if (imagename == titles[0]){IJ.selectWindow(titles[0]);
			CalculationOfDose(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[0]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[1]){IJ.selectWindow(titles[1]);
			CalculationOfDose(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[1]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[2]){IJ.selectWindow(titles[2]);
			CalculationOfDose(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[2]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[3]){IJ.selectWindow(titles[3]);
			CalculationOfDose(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[3]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[4]){IJ.selectWindow(titles[4]);
			CalculationOfDose(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[4]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[5]){IJ.selectWindow(titles[5]);
			CalculationOfDose(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[5]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[6]){IJ.selectWindow(titles[6]);
			CalculationOfDose(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[6]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[7]){IJ.selectWindow(titles[7]);
			CalculationOfDose(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[7]); // Calculates dose for every opened image}	
		}
		else{}
	}
	} // void run
} // class Dose_Calculation implements PlugIn
