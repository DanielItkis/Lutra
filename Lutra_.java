// This Plugin converts the Grey values on a RCF film into Dose values
// It takes an 24-Bit RGB .tiff and uses the signal on the red channel
// Saves the image as 32-Bit Float .tiff


import java.sql.*;
import ij.*;
import ij.gui.*; // for user interface, e.g. GenericDialog
import ij.io.*; // for FileOpener, OpenDialog 
import ij.plugin.*; // for incorporation of plugins
import ij.process.*;
import java.awt.*; // programme crashes otherwise
import java.io.*; // Random Access File
import javax.swing.*; // Java commands like try


public class Lutra_ implements PlugIn{
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
	public void CalculationOfDosered(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		
		IJ.run("Duplicate...", " ");	// Make a duplicate of the original input image for later manipulation
		
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
			IJ.saveAs("Png", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
			IJ.saveAs("Tif", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDose
	



// Calculates the dose from the green values in the image
	public void CalculationOfDosegreenno(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		
		IJ.run("Duplicate...", " ");	// Make a duplicate of the original input image for later manipulation
		
		IJ.selectWindow(title); // Original image is selected as active window
		IJ.run("Split Channels"); // Seperates RGB channels of the image
	
		IJ.selectWindow(title + " (red)"); // Red channel is selected as active window
		IJ.run("Close"); // Closes red channel
		IJ.selectWindow(title + " (blue)"); // Blue channel is selected as active window
		IJ.run("Close"); // Closes Blue channel
				
		// From now on, only Green channel is used
		implus = WindowManager.getImage(title+" (green)"); // reference needs to be renewed, as the image changed from xxx.tif to xxx (red).tif, otherwise nullpointerexception
		ip = implus.getProcessor(); // ImageProcessor contains the modify.pixel operations
		
		newimplus=new ImagePlus(); // a new image with 32 Bit is created to save the calculated Dose values in
		newimplus=IJ.createImage("Dose of "+title, "32-Bit black",ip.getWidth(),ip.getHeight(),1); // new image with 32 Bit, black background, dimensions of original image and one layer
		newimplus.show();
		IJ.selectWindow("Dose of "+title); // selects new image
		newip=newimplus.getProcessor().convertToFloat(); // FloatProcessor is needed to write float values into the image
	
		for (int y=0; y <= ip.getHeight(); y++){ // Calculates dose from green values in the image for every Pixel
			for (int x=0; x <=ip.getWidth(); x++){
				value = (double)ip.getPixel(x,y);
				if (value>unexp) value=unexp; // Pixel is brighter than unexposed film: unphysical, therefore set to background value. in that case, netOD becomes 0
				netOD = Math.log10((unexp-bckg)/(value-bckg)); // calculation of optical density
				Dosis = p1*netOD+p2*Math.pow(netOD, p3); // calculation of dose from optical density
				if (Dosis>maxdos) newip.putPixelValue (x, y, maxdos); // if dose is too large, it is set to maximum dose
				else newip.putPixelValue (x, y, Dosis);
			} // for
		} // for
		
		IJ.selectWindow(title+" (green)");
		IJ.run("Close"); // original image is closed
		IJ.run("Color Balance..."); // run Color Balance, otherwise image appears simply black
		IJ.selectWindow("Color");
		IJ.run("Close"); // close Color Balance window
		IJ.run("Brightness/Contrast...");
		IJ.resetMinAndMax();
		IJ.selectWindow("B&C");
		IJ.run("Close"); // close B&C window
		if (directory!="Empty"){ // Every single image is saved
		IJ.saveAs("Tif", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDosegreenno



	// Calculates the dose from the green values in the image
	public void CalculationOfDosegreen(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		
		IJ.run("Duplicate...", " ");	// Make a duplicate of the original input image for later manipulation
		
		IJ.selectWindow(title); // Original image is selected as active window
		IJ.run("Split Channels"); // Seperates RGB channels of the image
	
		IJ.selectWindow(title + " (red)"); // Red channel is selected as active window
		IJ.run("Close"); // Closes red channel
		IJ.selectWindow(title + " (blue)"); // Blue channel is selected as active window
		IJ.run("Close"); // Closes Blue channel
				
		// From now on, only Green channel is used
		implus = WindowManager.getImage(title+" (green)"); // reference needs to be renewed, as the image changed from xxx.tif to xxx (red).tif, otherwise nullpointerexception
		ip = implus.getProcessor(); // ImageProcessor contains the modify.pixel operations
		
		newimplus=new ImagePlus(); // a new image with 32 Bit is created to save the calculated Dose values in
		newimplus=IJ.createImage("Dose of "+title, "32-Bit black",ip.getWidth(),ip.getHeight(),1); // new image with 32 Bit, black background, dimensions of original image and one layer
		newimplus.show();
		IJ.selectWindow("Dose of "+title); // selects new image
		newip=newimplus.getProcessor().convertToFloat(); // FloatProcessor is needed to write float values into the image
	
		for (int y=0; y <= ip.getHeight(); y++){ // Calculates dose from green values in the image for every Pixel
			for (int x=0; x <=ip.getWidth(); x++){
				value = (double)ip.getPixel(x,y);
				if (value>unexp) value=unexp; // Pixel is brighter than unexposed film: unphysical, therefore set to background value. in that case, netOD becomes 0
				netOD = Math.log10((unexp-bckg)/(value-bckg)); // calculation of optical density
				Dosis = p1*netOD+p2*Math.pow(netOD, p3); // calculation of dose from optical density
				if (Dosis>maxdos) newip.putPixelValue (x, y, maxdos); // if dose is too large, it is set to maximum dose
				else newip.putPixelValue (x, y, Dosis);
			} // for
		} // for
		
		IJ.selectWindow(title+" (green)");
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
			IJ.saveAs("Png", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
			IJ.saveAs("Tif", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDosegreen






// Calculates the dose from the red values in the image
	public void CalculationOfDoseblue(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		
		IJ.run("Duplicate...", " ");	// Make a duplicate of the original input image for later manipulation
		
		IJ.selectWindow(title); // Original image is selected as active window
		IJ.run("Split Channels"); // Seperates RGB channels of the image
	
		IJ.selectWindow(title + " (red)"); // Red channel is selected as active window
		IJ.run("Close"); // Closes red channel
		IJ.selectWindow(title + " (green)"); // Green channel is selected as active window
		IJ.run("Close"); // Closes Green channel
				
		// From now on, only Red channel is used (Scanner sensitivity is highest)
		implus = WindowManager.getImage(title+" (blue)"); // reference needs to be renewed, as the image changed from xxx.tif to xxx (red).tif, otherwise nullpointerexception
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
		
		IJ.selectWindow(title+" (blue)");
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
			IJ.saveAs("Png", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
			IJ.saveAs("Tif", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDosegreen



// Calculates the dose from the red values in the image
	public void CalculationOfDoseblueno(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		
		IJ.run("Duplicate...", " ");	// Make a duplicate of the original input image for later manipulation
		
		IJ.selectWindow(title); // Original image is selected as active window
		IJ.run("Split Channels"); // Seperates RGB channels of the image
	
		IJ.selectWindow(title + " (red)"); // Red channel is selected as active window
		IJ.run("Close"); // Closes red channel
		IJ.selectWindow(title + " (green)"); // Green channel is selected as active window
		IJ.run("Close"); // Closes Green channel
				
		// From now on, only Red channel is used (Scanner sensitivity is highest)
		implus = WindowManager.getImage(title+" (blue)"); // reference needs to be renewed, as the image changed from xxx.tif to xxx (red).tif, otherwise nullpointerexception
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
		
		IJ.selectWindow(title+" (blue)");
		IJ.run("Close"); // original image is closed
		IJ.run("Color Balance..."); // run Color Balance, otherwise image appears simply black
		IJ.selectWindow("Color");
		IJ.run("Close"); // close Color Balance window
		IJ.run("Brightness/Contrast...");
		IJ.resetMinAndMax();
		IJ.selectWindow("B&C");
		IJ.run("Close"); // close B&C window
		if (directory!="Empty"){ // Every single image is saved
		IJ.saveAs("Tif", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDosegreenno

	

	// Calculates the dose from the red values in the image
	public void CalculationOfDoseredstack(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		int stak = implus.getImageStackSize();
		double value, netOD, Dosis; // variables for calculation of dose
		
		IJ.run("Duplicate...", "");	// Make a duplicate of the original input image for later manipulation
		
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
		newimplus=IJ.createImage("Dose of "+title, "32-Bit black",ip.getWidth(),ip.getHeight(),stak); // new image with 32 Bit, black background, dimensions of original image and one layer
		newimplus.show();
		IJ.selectWindow("Dose of "+title); // selects new image
		newip=newimplus.getProcessor().convertToFloat(); // FloatProcessor is needed to write float values into the image
	for(int z=0; z<= stak; ++z){
		implus.setSlice(z);
		newimplus.setSlice(z);
		for (int y=0; y <= ip.getHeight(); y++){ // Calculates dose from red values in the image for every Pixel
			for (int x=0; x <=ip.getWidth(); x++){
				value = (double)ip.getPixel(x,y);
				if (value>unexp) value=unexp; // Pixel is brighter than unexposed film: unphysical, therefore set to background value. in that case, netOD becomes 0
				netOD = Math.log10((unexp-bckg)/(value-bckg)); // calculation of optical density
				Dosis = p1*netOD+p2*Math.pow(netOD, p3); // calculation of dose from optical density
				if (Dosis>maxdos) newip.putPixelValue (x, y, maxdos); // if dose is too large, it is set to maximum dose
				else newip.putPixelValue (x, y, Dosis);
				}
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
			IJ.saveAs("Tif", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDoseredstack
	

	// Calculates the dose from the red values in the image
	public void CalculationOfDoseredstackno(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		int stak = implus.getImageStackSize();
		double value, netOD, Dosis; // variables for calculation of dose
		
		IJ.run("Duplicate...", "");	// Make a duplicate of the original input image for later manipulation
		
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
		newimplus=IJ.createImage("Dose of "+title, "32-Bit black",ip.getWidth(),ip.getHeight(),stak); // new image with 32 Bit, black background, dimensions of original image and one layer
		newimplus.show();
		IJ.selectWindow("Dose of "+title); // selects new image
		newip=newimplus.getProcessor().convertToFloat(); // FloatProcessor is needed to write float values into the image
	for(int z=0; z<= stak; ++z){
		implus.setSlice(z);
		newimplus.setSlice(z);
		for (int y=0; y <= ip.getHeight(); y++){ // Calculates dose from red values in the image for every Pixel
			for (int x=0; x <=ip.getWidth(); x++){
				value = (double)ip.getPixel(x,y);
				if (value>unexp) value=unexp; // Pixel is brighter than unexposed film: unphysical, therefore set to background value. in that case, netOD becomes 0
				netOD = Math.log10((unexp-bckg)/(value-bckg)); // calculation of optical density
				Dosis = p1*netOD+p2*Math.pow(netOD, p3); // calculation of dose from optical density
				if (Dosis>maxdos) newip.putPixelValue (x, y, maxdos); // if dose is too large, it is set to maximum dose
				else newip.putPixelValue (x, y, Dosis);
				}
			} // for
		} // for
		
		IJ.selectWindow(title+" (red)");
		IJ.run("Close"); // original image is closed
		IJ.run("Color Balance..."); // run Color Balance, otherwise image appears simply black
		IJ.selectWindow("Color");
		IJ.run("Close"); // close Color Balance window
		IJ.run("Brightness/Contrast...");
		IJ.resetMinAndMax();
		IJ.selectWindow("B&C");
		IJ.run("Close"); // close B&C window
		if (directory!="Empty"){ // Every single image is saved
		IJ.saveAs("Tif", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDoseredstackno


// Calculates the dose from the green values in the image
	public void CalculationOfDosegreenstackno(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		int stak = implus.getImageStackSize();
		
		IJ.run("Duplicate...", "");	// Make a duplicate of the original input image for later manipulation
		
		IJ.selectWindow(title); // Original image is selected as active window
		IJ.run("Split Channels"); // Seperates RGB channels of the image
	
		IJ.selectWindow(title + " (red)"); // Red channel is selected as active window
		IJ.run("Close"); // Closes red channel
		IJ.selectWindow(title + " (blue)"); // Blue channel is selected as active window
		IJ.run("Close"); // Closes Blue channel
				
		// From now on, only Green channel is used
		implus = WindowManager.getImage(title+" (green)"); // reference needs to be renewed, as the image changed from xxx.tif to xxx (red).tif, otherwise nullpointerexception
		ip = implus.getProcessor(); // ImageProcessor contains the modify.pixel operations
		
		newimplus=new ImagePlus(); // a new image with 32 Bit is created to save the calculated Dose values in
		newimplus=IJ.createImage("Dose of "+title, "32-Bit black",ip.getWidth(),ip.getHeight(),stak); // new image with 32 Bit, black background, dimensions of original image and one layer
		newimplus.show();
		IJ.selectWindow("Dose of "+title); // selects new image
		newip=newimplus.getProcessor().convertToFloat(); // FloatProcessor is needed to write float values into the image
	for(int z=0; z<= stak; ++z){
		implus.setSlice(z);
		newimplus.setSlice(z);
		for (int y=0; y <= ip.getHeight(); y++){ // Calculates dose from green values in the image for every Pixel
			for (int x=0; x <=ip.getWidth(); x++){
				value = (double)ip.getPixel(x,y);
				if (value>unexp) value=unexp; // Pixel is brighter than unexposed film: unphysical, therefore set to background value. in that case, netOD becomes 0
				netOD = Math.log10((unexp-bckg)/(value-bckg)); // calculation of optical density
				Dosis = p1*netOD+p2*Math.pow(netOD, p3); // calculation of dose from optical density
				if (Dosis>maxdos) newip.putPixelValue (x, y, maxdos); // if dose is too large, it is set to maximum dose
				else newip.putPixelValue (x, y, Dosis);
				}
			} // for
		} // for
		
		IJ.selectWindow(title+" (green)");
		IJ.run("Close"); // original image is closed
		IJ.run("Color Balance..."); // run Color Balance, otherwise image appears simply black
		IJ.selectWindow("Color");
		IJ.run("Close"); // close Color Balance window
		IJ.run("Brightness/Contrast...");
		IJ.resetMinAndMax();
		IJ.selectWindow("B&C");
		IJ.run("Close"); // close B&C window
		if (directory!="Empty"){ // Every single image is saved
		IJ.saveAs("Tif", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDosegreenstackno


// Calculates the dose from the green values in the image
	public void CalculationOfDosegreenstack(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		int stak = implus.getImageStackSize();
		
		IJ.run("Duplicate...", "");	// Make a duplicate of the original input image for later manipulation
		
		IJ.selectWindow(title); // Original image is selected as active window
		IJ.run("Split Channels"); // Seperates RGB channels of the image
	
		IJ.selectWindow(title + " (red)"); // Red channel is selected as active window
		IJ.run("Close"); // Closes red channel
		IJ.selectWindow(title + " (blue)"); // Blue channel is selected as active window
		IJ.run("Close"); // Closes Blue channel
				
		// From now on, only Green channel is used
		implus = WindowManager.getImage(title+" (green)"); // reference needs to be renewed, as the image changed from xxx.tif to xxx (red).tif, otherwise nullpointerexception
		ip = implus.getProcessor(); // ImageProcessor contains the modify.pixel operations
		
		newimplus=new ImagePlus(); // a new image with 32 Bit is created to save the calculated Dose values in
		newimplus=IJ.createImage("Dose of "+title, "32-Bit black",ip.getWidth(),ip.getHeight(),stak); // new image with 32 Bit, black background, dimensions of original image and one layer
		newimplus.show();
		IJ.selectWindow("Dose of "+title); // selects new image
		newip=newimplus.getProcessor().convertToFloat(); // FloatProcessor is needed to write float values into the image
	for(int z=0; z<= stak; ++z){
		implus.setSlice(z);
		newimplus.setSlice(z);
		for (int y=0; y <= ip.getHeight(); y++){ // Calculates dose from green values in the image for every Pixel
			for (int x=0; x <=ip.getWidth(); x++){
				value = (double)ip.getPixel(x,y);
				if (value>unexp) value=unexp; // Pixel is brighter than unexposed film: unphysical, therefore set to background value. in that case, netOD becomes 0
				netOD = Math.log10((unexp-bckg)/(value-bckg)); // calculation of optical density
				Dosis = p1*netOD+p2*Math.pow(netOD, p3); // calculation of dose from optical density
				if (Dosis>maxdos) newip.putPixelValue (x, y, maxdos); // if dose is too large, it is set to maximum dose
				else newip.putPixelValue (x, y, Dosis);
				}
			} // for
		} // for
		
		IJ.selectWindow(title+" (green)");
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
			IJ.saveAs("Tif", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDosegreenstack




// Calculates the dose from the blue values in the image
	public void CalculationOfDosebluestack(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		int stak = implus.getImageStackSize();
		
		IJ.run("Duplicate...", "");	// Make a duplicate of the original input image for later manipulation
		
		IJ.selectWindow(title); // Original image is selected as active window
		IJ.run("Split Channels"); // Seperates RGB channels of the image
	
		IJ.selectWindow(title + " (red)"); // Red channel is selected as active window
		IJ.run("Close"); // Closes red channel
		IJ.selectWindow(title + " (green)"); // Green channel is selected as active window
		IJ.run("Close"); // Closes Green channel
				
		// From now on, only Red channel is used (Scanner sensitivity is highest)
		implus = WindowManager.getImage(title+" (blue)"); // reference needs to be renewed, as the image changed from xxx.tif to xxx (red).tif, otherwise nullpointerexception
		ip = implus.getProcessor(); // ImageProcessor contains the modify.pixel operations
		
		newimplus=new ImagePlus(); // a new image with 32 Bit is created to save the calculated Dose values in
		newimplus=IJ.createImage("Dose of "+title, "32-Bit black",ip.getWidth(),ip.getHeight(),stak); // new image with 32 Bit, black background, dimensions of original image and one layer
		newimplus.show();
		IJ.selectWindow("Dose of "+title); // selects new image
		newip=newimplus.getProcessor().convertToFloat(); // FloatProcessor is needed to write float values into the image

	for(int z=0; z<= stak; ++z){
		implus.setSlice(z);
		newimplus.setSlice(z);
		for (int y=0; y <= ip.getHeight(); y++){ // Calculates dose from red values in the image for every Pixel
			for (int x=0; x <=ip.getWidth(); x++){
				value = (double)ip.getPixel(x,y);
				if (value>unexp) value=unexp; // Pixel is brighter than unexposed film: unphysical, therefore set to background value. in that case, netOD becomes 0
				netOD = Math.log10((unexp-bckg)/(value-bckg)); // calculation of optical density
				Dosis = p1*netOD+p2*Math.pow(netOD, p3); // calculation of dose from optical density
				if (Dosis>maxdos) newip.putPixelValue (x, y, maxdos); // if dose is too large, it is set to maximum dose
				else newip.putPixelValue (x, y, Dosis);
				}
			} // for
		} // for
		
		IJ.selectWindow(title+" (blue)");
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
			IJ.saveAs("Tif", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDosebluestack


// Calculates the dose from the blue values in the image
	public void CalculationOfDosebluestackno(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		int stak = implus.getImageStackSize();
		
		IJ.run("Duplicate...", "");	// Make a duplicate of the original input image for later manipulation
		
		IJ.selectWindow(title); // Original image is selected as active window
		IJ.run("Split Channels"); // Seperates RGB channels of the image
	
		IJ.selectWindow(title + " (red)"); // Red channel is selected as active window
		IJ.run("Close"); // Closes red channel
		IJ.selectWindow(title + " (green)"); // Green channel is selected as active window
		IJ.run("Close"); // Closes Green channel
				
		// From now on, only Red channel is used (Scanner sensitivity is highest)
		implus = WindowManager.getImage(title+" (blue)"); // reference needs to be renewed, as the image changed from xxx.tif to xxx (red).tif, otherwise nullpointerexception
		ip = implus.getProcessor(); // ImageProcessor contains the modify.pixel operations
		
		newimplus=new ImagePlus(); // a new image with 32 Bit is created to save the calculated Dose values in
		newimplus=IJ.createImage("Dose of "+title, "32-Bit black",ip.getWidth(),ip.getHeight(),stak); // new image with 32 Bit, black background, dimensions of original image and one layer
		newimplus.show();
		IJ.selectWindow("Dose of "+title); // selects new image
		newip=newimplus.getProcessor().convertToFloat(); // FloatProcessor is needed to write float values into the image

	for(int z=0; z<= stak; ++z){
		implus.setSlice(z);
		newimplus.setSlice(z);
		for (int y=0; y <= ip.getHeight(); y++){ // Calculates dose from red values in the image for every Pixel
			for (int x=0; x <=ip.getWidth(); x++){
				value = (double)ip.getPixel(x,y);
				if (value>unexp) value=unexp; // Pixel is brighter than unexposed film: unphysical, therefore set to background value. in that case, netOD becomes 0
				netOD = Math.log10((unexp-bckg)/(value-bckg)); // calculation of optical density
				Dosis = p1*netOD+p2*Math.pow(netOD, p3); // calculation of dose from optical density
				if (Dosis>maxdos) newip.putPixelValue (x, y, maxdos); // if dose is too large, it is set to maximum dose
				else newip.putPixelValue (x, y, Dosis);
				}
			} // for
		} // for
		
		IJ.selectWindow(title+" (blue)");
		IJ.run("Close"); // original image is closed
		IJ.run("Color Balance..."); // run Color Balance, otherwise image appears simply black
		IJ.selectWindow("Color");
		IJ.run("Close"); // close Color Balance window
		IJ.run("Brightness/Contrast...");
		IJ.resetMinAndMax();
		IJ.selectWindow("B&C");
		IJ.run("Close"); // close B&C window
		if (directory!="Empty"){ // Every single image is saved
		IJ.saveAs("Tif", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDosebluestackno

	
		// Calculates the dose from the red values in the image
		public void CalculationOfDoseredno(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		
		IJ.run("Duplicate...", " ");	// Make a duplicate of the original input image for later manipulation
		
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
		IJ.run("Color Balance..."); // run Color Balance, otherwise image appears simply black
		IJ.selectWindow("Color");
		IJ.run("Close"); // close Color Balance window
		IJ.run("Brightness/Contrast...");
		IJ.resetMinAndMax();
		IJ.selectWindow("B&C");
		IJ.run("Close"); // close B&C window
		if (directory!="Empty"){ // Every single image is saved
		IJ.saveAs("Tif", directory+"Dose of "+title); // case savemode "Choose directory" or "current directory"
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDose



	
	
	// Main
	public void run(String arg) {
		IJ.register( Lutra_ .class);
		
		// Initialize
		int numberoffiles; // number of files that are to be processed
		double p1,p2,p3,bckg,unexp,maxdos; // parameters for the calibration curve
		String[] savemodes={"Current directory", "Choose directory", "Don't save"}; // Scroll-down for saving options
		String[] titles; // String containing the titles of the images
		String savemode=new String(); // contains the choosen saving option
		String[] editp={"red", "green", "blue", "all"}; // Scroll-down for editing p-values
		String[] film = {"EBT3", "HD810"};
		String[] scan = {"Profil 3 (Gamma 1.8)", "Gamma 1"};
		String directory = new String(); // contains the directory that the image will be saved to
		GenericDialog gd, customgd; // interaction with the user
		ImagePlus img; // needed for accessing the opened images
		boolean evalu;
		
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
		gd = new GenericDialog("Welcome to Lutra!", IJ.getInstance()); 
		
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

			case 8: 
			String[] names8 = {titles[0], titles[1], titles[2],titles[3],titles[4],titles[5],titles[6],titles[7],"all"};
			gd.addChoice("Which files?", names8, titles[0]);
			break;

			case 9: 
			String[] names9 = {titles[0], titles[1], titles[2],titles[3],titles[4],titles[5],titles[6],titles[7],titles[8],"all"};
			gd.addChoice("Which files?", names9, titles[0]);
			break;

			case 10: 
			String[] names10 = {titles[0], titles[1], titles[2],titles[3],titles[4],titles[5],titles[6],titles[7],titles[8],titles[9],"all"};
			gd.addChoice("Which files?", names10, titles[0]);
			break;

			case 11: 
			String[] names11 = {titles[0], titles[1], titles[2],titles[3],titles[4],titles[5],titles[6],titles[7],titles[8],titles[9],titles[10],"all"};
			gd.addChoice("Which files?", names11, titles[0]);
			break;

			case 12: 
			String[] names12 = {titles[0], titles[1], titles[2],titles[3],titles[4],titles[5],titles[6],titles[7],titles[8],titles[9],titles[10],titles[11],"all"};
			gd.addChoice("Which files?", names12, titles[0]);
			break;

			case 13: 
			String[] names13 = {titles[0], titles[1], titles[2],titles[3],titles[4],titles[5],titles[6],titles[7],titles[8],titles[9],titles[10],titles[11],titles[12],"all"};
			gd.addChoice("Which files?", names13, titles[0]);
			break;

			case 14: 
			String[] names14 = {titles[0], titles[1], titles[2],titles[3],titles[4],titles[5],titles[6],titles[7],titles[8],titles[9],titles[10],titles[11],titles[12],titles[13],"all"};
			gd.addChoice("Which files?", names14, titles[0]);
			break;

			case 15: 
			String[] names15 = {titles[0], titles[1], titles[2],titles[3],titles[4],titles[5],titles[6],titles[7],titles[8],titles[9],titles[10],titles[11],titles[12],titles[13],titles[14],"all"};
			gd.addChoice("Which files?", names15, titles[0]);
			break;

			default: IJ.error("Lutra does not support more than 15 open Images or Stacks. Please close any superfluous windows.");
			return;		
		}
		gd.addChoice("Choose Color Channel", editp, "red");
		gd.addCheckbox("Do the files include Stacks?", false);
		gd.addChoice("Kind of film:", film, "EBT3");
		gd.addChoice("Scan parameter:", scan, "Profil 3 (Gamma 1.8");
		gd.addCheckbox("Autoevaluate?", false);
		gd.addChoice("Savemode?", savemodes, "Don't save");
		
		gd.showDialog();
		if (gd.wasCanceled()) return; // User may cancel Plugin
		
		// Return of user-added parameters and options for dose calculation
		boolean stacky = gd.getNextBoolean();
		String imagename = gd.getNextChoice();
		String editop = gd.getNextChoice();
		String films = gd.getNextChoice();
		String scans = gd.getNextChoice();
		evalu = gd.getNextBoolean();
		savemode=gd.getNextChoice();
		double p1r, p2r, p3r, bckgr, unexpr, maxdosr, p1g, p2g, p3g, bckgg, unexpg, maxdosg, p1b, p2b, p3b, bckgb, unexpb, maxdosb;  
		
		
		if(films == "EBT3"){
			if (scans == "Profil 3 (Gamma 1.8)"){ //Values for EBT3 RCFs with regular scanning settings
			p1r = -10; p2r = 22.80; p3r = 1.05; bckgr = 0; unexpr = 170; maxdosr = 15; 
			p1g = 29.19; p2g = 38.12; p3g = 2.24; bckgg = 0; unexpg = 170.9; maxdosg = 64; 
			p1b = 148.16; p2b = 161.83; p3b = 3.550; bckgb = 0; unexpb = 75.7; maxdosb = 64; 
			}
			else {  //Values for EBT3 RCFs with Gamma 1 scanning
			p1r = 27.55; p2r = 754.99; p3r = 5.96; bckgr = 0; unexpr =205.0; maxdosr = 64; 
			p1g = 57.84; p2g = 359.19; p3g = 2.99; bckgg = 0; unexpg = 207.9; maxdosg = 64; 
			p1b = 237.17; p2b = 1327; p3b = 2.798; bckgb = 0; unexpb = 141.1; maxdosb = 64;
			}}
		else{
			if (scans == "Profil 3 (Gamma 1.8)"){ //Values for HD810 RCFs with regular scanning settings
			p1r = 318.62; p2r = 0.02; p3r = 0; bckgr = 0; unexpr = 230.8; maxdosr = 64; 
			p1g = 1321.9; p2g = 0.484; p3g = 0; bckgg = 0; unexpg = 238.14; maxdosg = 64; 
			p1b = 0; p2b = 0; p3b = 0; bckgb = 0; unexpb = 236.95; maxdosb = 0;
			}
			else { //Values for HD810 RCFs with Gamma 1 scanning
			p1r = 0; p2r = 0; p3r = 0; bckgr = 0; unexpr = 0; maxdosr = 0; 
			p1g = 0; p2g = 0; p3g = 0; bckgg = 0; unexpg = 0; maxdosg = 0; 
			p1b = 0; p2b = 0; p3b = 0; bckgb = 0; unexpb = 0; maxdosb = 0;	
				}
			}

			if (editop.equals("blue")==true){
			customgd = new GenericDialog("Lutra", IJ.getInstance()); //Dialogue that only appears if p-values set to "customized"
			customgd.addMessage("Customize parameters blue"); 
			customgd.addNumericField("Blue p1", p1b, 3);
        	customgd.addNumericField("Blue p2", p2b, 3);
        	customgd.addNumericField("Blue p3", p3b, 3);
			customgd.addNumericField("Dark current of the scanner", bckgb, 2);
			customgd.addNumericField("Transparency of the unexposed film", unexpb, 2);
			customgd.addNumericField("Maximum dose film can detect", maxdosb, 0);

       		customgd.showDialog();
			if (customgd.wasCanceled()) return;

			
		p1 =  customgd.getNextNumber();
		p2 =  customgd.getNextNumber();
		p3 =  customgd.getNextNumber();
		bckg = customgd.getNextNumber();
		unexp =  customgd.getNextNumber();
		maxdos =  customgd.getNextNumber();

		}
		else if (editop.equals("green")==true){
			customgd = new GenericDialog("Lutra", IJ.getInstance()); //Dialogue that only appears if p-values set to "customized"
			customgd.addMessage("Customize parameters green");
			customgd.addNumericField("Green p1", p1g, 3);
        	customgd.addNumericField("Green p2", p2g, 3);
        	customgd.addNumericField("Green p3", p3g, 3);
        	customgd.addNumericField("Dark current of the scanner", bckgg, 2);
			customgd.addNumericField("Transparency of the unexposed film", unexpg, 2);
			customgd.addNumericField("Maximum dose film can detect", maxdosg, 0);
        	 customgd.showDialog();
			if (customgd.wasCanceled()) return;
        	p1 =  customgd.getNextNumber();
			p2 =  customgd.getNextNumber();
			p3 =  customgd.getNextNumber();
			bckg = customgd.getNextNumber();
			unexp =  customgd.getNextNumber();
			maxdos =  customgd.getNextNumber();

		}

		else if (editop.equals("red")==true){
	   		customgd = new GenericDialog("Lutra", IJ.getInstance()); //Dialogue that only appears if p-values set to "customized"
			customgd.addMessage("Customize parameters red");
			customgd.addNumericField("Red p1", p1r, 5); // Suggestions for parameters
			customgd.addNumericField("Red p2", p2r, 5);
			customgd.addNumericField("Red p3", p3r, 3);
			customgd.addNumericField("Dark current of the scanner", bckgr, 2);
			customgd.addNumericField("Transparency of the unexposed film", unexpr, 2);
			customgd.addNumericField("Maximum dose film can detect", maxdosr, 0);
        	 customgd.showDialog();
			if (customgd.wasCanceled()) return;
        	p1 =  customgd.getNextNumber();
			p2 =  customgd.getNextNumber();
			p3 =  customgd.getNextNumber();
			bckg = customgd.getNextNumber();
			unexp =  customgd.getNextNumber();
			maxdos =  customgd.getNextNumber();

		}
		else if (editop.equals("all")==true){
		customgd = new GenericDialog("Lutra", IJ.getInstance()); //Dialogue that only appears if p-values set to "customized"
		customgd.addMessage("Customize parameters all"); 
		//default values calculated by Johannes Konrad (j.konrad@hzdr.de), added 12092016
		customgd.addNumericField("Red (low dose) p1", 10.22, 5); // Suggestions for parameters
		customgd.addNumericField("Red (low dose) p2", 2.97, 5);
		customgd.addNumericField("Red(low dose) p3", 1.8, 3);
		customgd.addNumericField("Green (medium dose) p1", 19.18214, 3);
        customgd.addNumericField("Green (medium dose) p2", 44.81919, 3);
        customgd.addNumericField("Green (medium dose) p3", 2.04147, 3);
        customgd.addNumericField("Blue (high dose) p1", 103.98, 3);
        customgd.addNumericField("Blue (high dose) p2", 1.945, 3);
        customgd.addNumericField("Blue (high dose) p3", 0, 3);
        customgd.addNumericField("Dark current of the scanner", 0, 2);
		customgd.addNumericField("Transparency of the unexposed film", 204, 2);
		customgd.addNumericField("Maximum dose film can detect", 40, 0);
		customgd.showDialog();
		if (customgd.wasCanceled()) return; // User may cancel Plugin
				// Return of user-added parameters and options for dose calculation
		p1 =  customgd.getNextNumber();
		p2 =  customgd.getNextNumber();
		p3 =  customgd.getNextNumber();
		bckg = customgd.getNextNumber();
		unexp =  customgd.getNextNumber();
		maxdos =  customgd.getNextNumber();

		}else{p1 = 0;p2 = 0; p3=0; bckg = 0; unexp = 0; maxdos = 0; IJ.error("Invalid Color Channel. Please restart plugin.");
}
		
		
		// Choice where to save the images
		if (savemode.equals("Choose directory")==true) directory = IJ.getDirectory("Directory to save to..."); //case savemode=Choose directory
		else if (savemode.equals("Current directory")==true) directory = IJ.getDirectory("current"); //case savemode=Current directory
		else directory="Empty"; //case savemode=Don't save

		if (evalu == true){
		if (stacky == false){
		if (editop == "red"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDosered(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1]); // Calculates dose for every opened image
			i++;
		} // while
		}
		else if (imagename == titles[0]){IJ.selectWindow(titles[0]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[0]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[1]){IJ.selectWindow(titles[1]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[1]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[2]){IJ.selectWindow(titles[2]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[2]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[3]){IJ.selectWindow(titles[3]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[3]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[4]){IJ.selectWindow(titles[4]);
			CalculationOfDosered(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[4]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[5]){IJ.selectWindow(titles[5]);
			CalculationOfDosered(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[5]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[6]){IJ.selectWindow(titles[6]);
			CalculationOfDosered(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[6]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[7]){IJ.selectWindow(titles[7]);
			CalculationOfDosered(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[7]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[8]){IJ.selectWindow(titles[8]);
			CalculationOfDosered(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[8]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[9]){IJ.selectWindow(titles[9]);
			CalculationOfDosered(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[9]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[10]){IJ.selectWindow(titles[10]);
			CalculationOfDosered(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[10]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[11]){IJ.selectWindow(titles[11]);
			CalculationOfDosered(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[11]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[12]){IJ.selectWindow(titles[12]);
			CalculationOfDosered(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[12]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[13]){IJ.selectWindow(titles[13]);
			CalculationOfDosered(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[13]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[14]){IJ.selectWindow(titles[14]);
			CalculationOfDosered(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[14]); // Calculates dose for every opened image}	
		}
		else{}} 
		
		else if (editop == "green"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1]); // Calculates dose for every opened image
			i++;
		} // while
		}
		else if (imagename == titles[0]){IJ.selectWindow(titles[0]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[0]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[1]){IJ.selectWindow(titles[1]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[1]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[2]){IJ.selectWindow(titles[2]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[2]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[3]){IJ.selectWindow(titles[3]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[3]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[4]){IJ.selectWindow(titles[4]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[4]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[5]){IJ.selectWindow(titles[5]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[5]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[6]){IJ.selectWindow(titles[6]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[6]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[7]){IJ.selectWindow(titles[7]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[7]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[8]){IJ.selectWindow(titles[8]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[8]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[9]){IJ.selectWindow(titles[9]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[9]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[10]){IJ.selectWindow(titles[10]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[10]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[11]){IJ.selectWindow(titles[11]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[11]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[12]){IJ.selectWindow(titles[12]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[12]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[13]){IJ.selectWindow(titles[13]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[13]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[14]){IJ.selectWindow(titles[14]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[14]); // Calculates dose for every opened image}	
		}
		else{}}

		else if (editop == "blue"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1]); // Calculates dose for every opened image
			i++;
		} // while
		}
		else if (imagename == titles[0]){IJ.selectWindow(titles[0]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[0]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[1]){IJ.selectWindow(titles[1]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[1]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[2]){IJ.selectWindow(titles[2]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[2]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[3]){IJ.selectWindow(titles[3]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[3]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[4]){IJ.selectWindow(titles[4]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[4]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[5]){IJ.selectWindow(titles[5]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[5]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[6]){IJ.selectWindow(titles[6]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[6]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[7]){IJ.selectWindow(titles[7]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[7]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[8]){IJ.selectWindow(titles[8]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[8]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[9]){IJ.selectWindow(titles[9]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[9]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[10]){IJ.selectWindow(titles[10]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[10]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[11]){IJ.selectWindow(titles[11]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[11]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[12]){IJ.selectWindow(titles[12]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[12]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[13]){IJ.selectWindow(titles[13]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[13]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[14]){IJ.selectWindow(titles[14]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[14]); // Calculates dose for every opened image}
		}
		else{}} else{}}
		else{
		if (editop == "red"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1]); // Calculates dose for every opened image
			i++;
		} // while
		}
		else if (imagename == titles[0]){IJ.selectWindow(titles[0]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[0]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[1]){IJ.selectWindow(titles[1]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[1]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[2]){IJ.selectWindow(titles[2]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[2]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[3]){IJ.selectWindow(titles[3]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[3]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[4]){IJ.selectWindow(titles[4]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[4]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[5]){IJ.selectWindow(titles[5]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[5]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[6]){IJ.selectWindow(titles[6]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[6]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[7]){IJ.selectWindow(titles[7]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[7]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[8]){IJ.selectWindow(titles[8]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[8]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[9]){IJ.selectWindow(titles[9]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[9]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[10]){IJ.selectWindow(titles[10]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[10]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[11]){IJ.selectWindow(titles[11]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[11]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[12]){IJ.selectWindow(titles[12]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[12]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[13]){IJ.selectWindow(titles[13]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[13]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[14]){IJ.selectWindow(titles[14]);
			CalculationOfDoseredstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[14]); // Calculates dose for every opened image}	
		}
		else{}}
		
		else if (editop == "green"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1]); // Calculates dose for every opened image
			i++;
		} // while
		}
		else if (imagename == titles[0]){IJ.selectWindow(titles[0]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[0]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[1]){IJ.selectWindow(titles[1]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[1]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[2]){IJ.selectWindow(titles[2]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[2]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[3]){IJ.selectWindow(titles[3]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[3]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[4]){IJ.selectWindow(titles[4]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[4]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[5]){IJ.selectWindow(titles[5]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[5]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[6]){IJ.selectWindow(titles[6]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[6]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[7]){IJ.selectWindow(titles[7]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[7]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[8]){IJ.selectWindow(titles[8]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[8]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[9]){IJ.selectWindow(titles[9]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[9]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[10]){IJ.selectWindow(titles[10]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[10]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[11]){IJ.selectWindow(titles[11]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[11]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[12]){IJ.selectWindow(titles[12]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[12]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[13]){IJ.selectWindow(titles[13]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[13]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[14]){IJ.selectWindow(titles[14]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[14]); // Calculates dose for every opened image}	
		}
		else{}}

		else if (editop == "blue"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1]); // Calculates dose for every opened image
			i++;
		} // while
		}
		else if (imagename == titles[0]){IJ.selectWindow(titles[0]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[0]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[1]){IJ.selectWindow(titles[1]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[1]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[2]){IJ.selectWindow(titles[2]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[2]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[3]){IJ.selectWindow(titles[3]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[3]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[4]){IJ.selectWindow(titles[4]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[4]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[5]){IJ.selectWindow(titles[5]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[5]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[6]){IJ.selectWindow(titles[6]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[6]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[7]){IJ.selectWindow(titles[7]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[7]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[8]){IJ.selectWindow(titles[8]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[8]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[9]){IJ.selectWindow(titles[9]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[9]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[10]){IJ.selectWindow(titles[10]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[10]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[11]){IJ.selectWindow(titles[11]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[11]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[12]){IJ.selectWindow(titles[12]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[12]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[13]){IJ.selectWindow(titles[13]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[13]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[14]){IJ.selectWindow(titles[14]);
			CalculationOfDosebluestack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[14]); // Calculates dose for every opened image}	
		}
		else{}} else{}}} else{	
			
		if(imagename == "all"){
			if (stacky == false){
		if (editop == "red"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1]); // Calculates dose for every opened image
			i++;
		} // while
		}
		else if (imagename == titles[0]){IJ.selectWindow(titles[0]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[0]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[1]){IJ.selectWindow(titles[1]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[1]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[2]){IJ.selectWindow(titles[2]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[2]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[3]){IJ.selectWindow(titles[3]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[3]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[4]){IJ.selectWindow(titles[4]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[4]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[5]){IJ.selectWindow(titles[5]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[5]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[6]){IJ.selectWindow(titles[6]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[6]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[7]){IJ.selectWindow(titles[7]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[7]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[8]){IJ.selectWindow(titles[8]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[8]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[9]){IJ.selectWindow(titles[9]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[9]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[10]){IJ.selectWindow(titles[10]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[10]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[11]){IJ.selectWindow(titles[11]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[11]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[12]){IJ.selectWindow(titles[12]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[12]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[13]){IJ.selectWindow(titles[13]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[13]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[14]){IJ.selectWindow(titles[14]);
			CalculationOfDoseredno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[14]); // Calculates dose for every opened image}	
		}
		else{}} 
		
		else if (editop == "green"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1]); // Calculates dose for every opened image
			i++;
		} // while
		}
		else if (imagename == titles[0]){IJ.selectWindow(titles[0]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[0]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[1]){IJ.selectWindow(titles[1]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[1]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[2]){IJ.selectWindow(titles[2]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[2]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[3]){IJ.selectWindow(titles[3]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[3]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[4]){IJ.selectWindow(titles[4]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[4]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[5]){IJ.selectWindow(titles[5]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[5]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[6]){IJ.selectWindow(titles[6]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[6]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[7]){IJ.selectWindow(titles[7]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[7]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[8]){IJ.selectWindow(titles[8]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[8]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[9]){IJ.selectWindow(titles[9]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[9]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[10]){IJ.selectWindow(titles[10]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[10]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[11]){IJ.selectWindow(titles[11]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[11]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[12]){IJ.selectWindow(titles[12]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[12]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[13]){IJ.selectWindow(titles[13]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[13]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[14]){IJ.selectWindow(titles[14]);
			CalculationOfDosegreenno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[14]); // Calculates dose for every opened image}	
		}
		else{}}

		else if (editop == "blue"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1]); // Calculates dose for every opened image
			i++;
		} // while
		}
		else if (imagename == titles[0]){IJ.selectWindow(titles[0]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[0]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[1]){IJ.selectWindow(titles[1]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[1]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[2]){IJ.selectWindow(titles[2]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[2]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[3]){IJ.selectWindow(titles[3]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[3]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[4]){IJ.selectWindow(titles[4]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[4]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[5]){IJ.selectWindow(titles[5]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[5]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[6]){IJ.selectWindow(titles[6]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[6]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[7]){IJ.selectWindow(titles[7]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[7]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[8]){IJ.selectWindow(titles[8]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[8]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[9]){IJ.selectWindow(titles[9]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[9]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[10]){IJ.selectWindow(titles[10]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[10]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[11]){IJ.selectWindow(titles[11]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[11]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[12]){IJ.selectWindow(titles[12]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[12]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[13]){IJ.selectWindow(titles[13]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[13]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[14]){IJ.selectWindow(titles[14]);
			CalculationOfDoseblueno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[14]); // Calculates dose for every opened image}
		}
		else{}} else{}}
		else{
		if (editop == "red"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1]); // Calculates dose for every opened image
			i++;
		} // while
		}
		else if (imagename == titles[0]){IJ.selectWindow(titles[0]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[0]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[1]){IJ.selectWindow(titles[1]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[1]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[2]){IJ.selectWindow(titles[2]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[2]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[3]){IJ.selectWindow(titles[3]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[3]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[4]){IJ.selectWindow(titles[4]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[4]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[5]){IJ.selectWindow(titles[5]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[5]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[6]){IJ.selectWindow(titles[6]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[6]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[7]){IJ.selectWindow(titles[7]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[7]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[8]){IJ.selectWindow(titles[8]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[8]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[9]){IJ.selectWindow(titles[9]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[9]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[10]){IJ.selectWindow(titles[10]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[10]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[11]){IJ.selectWindow(titles[11]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[11]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[12]){IJ.selectWindow(titles[12]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[12]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[13]){IJ.selectWindow(titles[13]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[13]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[14]){IJ.selectWindow(titles[14]);
			CalculationOfDoseredstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[14]); // Calculates dose for every opened image}	
		}
		else{}}
		
		else if (editop == "green"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDosegreenstack(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1]); // Calculates dose for every opened image
			i++;
		} // while
		}
		else if (imagename == titles[0]){IJ.selectWindow(titles[0]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[0]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[1]){IJ.selectWindow(titles[1]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[1]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[2]){IJ.selectWindow(titles[2]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[2]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[3]){IJ.selectWindow(titles[3]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[3]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[4]){IJ.selectWindow(titles[4]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[4]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[5]){IJ.selectWindow(titles[5]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[5]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[6]){IJ.selectWindow(titles[6]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[6]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[7]){IJ.selectWindow(titles[7]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[7]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[8]){IJ.selectWindow(titles[8]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[8]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[9]){IJ.selectWindow(titles[9]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[9]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[10]){IJ.selectWindow(titles[10]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[10]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[11]){IJ.selectWindow(titles[11]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[11]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[12]){IJ.selectWindow(titles[12]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[12]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[13]){IJ.selectWindow(titles[13]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[13]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[14]){IJ.selectWindow(titles[14]);
			CalculationOfDosegreenstackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[14]); // Calculates dose for every opened image}	
		}
		else{}}

		else if (editop == "blue"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1]); // Calculates dose for every opened image
			i++;
		} // while
		}
		else if (imagename == titles[0]){IJ.selectWindow(titles[0]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[0]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[1]){IJ.selectWindow(titles[1]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[1]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[2]){IJ.selectWindow(titles[2]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[2]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[3]){IJ.selectWindow(titles[3]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[3]); // Calculates dose for every opened image}
		}	
		else if (imagename == titles[4]){IJ.selectWindow(titles[4]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[4]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[5]){IJ.selectWindow(titles[5]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[5]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[6]){IJ.selectWindow(titles[6]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[6]); // Calculates dose for every opened image}
		}
		else if (imagename == titles[7]){IJ.selectWindow(titles[7]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[7]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[8]){IJ.selectWindow(titles[8]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[8]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[9]){IJ.selectWindow(titles[9]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[9]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[10]){IJ.selectWindow(titles[10]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[10]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[11]){IJ.selectWindow(titles[11]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[11]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[12]){IJ.selectWindow(titles[12]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[12]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[13]){IJ.selectWindow(titles[13]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[13]); // Calculates dose for every opened image}	
		}
		else if (imagename == titles[14]){IJ.selectWindow(titles[14]);
			CalculationOfDosebluestackno(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[14]); // Calculates dose for every opened image}	
		}
		else{}} else{}}
		}}
	} // void run
} // class Dose_Calculation implements PlugIn
