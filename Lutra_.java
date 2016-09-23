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
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;


public class Lutra_ implements PlugIn{
	static File dir; // directory that images are saved to
	String valuefiledirectory = "/Users/Daniel_Itkis/Desktop/Lutraparameters.csv";  //Change directory of file for p-values here
	
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
	public void CalculationOfDosered(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title, String saveas, boolean overlay){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		int stak;
		stak = implus.getImageStackSize();
		if (stak  == 1){IJ.run("Duplicate...", " ");	// Make a duplicate of the original input image for later manipulation
		}else{
		IJ.run("Duplicate...", "");	// Make a duplicate of the original input image for later manipulation 
		}
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
		
		if (overlay){
		IJ.run("Set Scale...", "distance=117 known=1 pixel=1 unit=cm global");
		IJ.run("RCF-Rainbow");
		//IJ.run("Duplicate...", "Dose of "+title+"+Bars");
		IJ.run("Calibration Bar...", "location=[Lower Left] fill=Black label=White number=5 decimal=0 font=16 zoom=1 bold overlay");
		//IJ.run("RGB Color");
		IJ.run("Scale Bar...", "width=1 height=2 font=16 color=White background=None location=[Lower Right] bold overlay");
		}else{}

		if (directory!="Empty"){ // Every single image is saved
				int z1 = 1;
				while(z1 <= stak){
				newimplus.setSlice(z1);
				IJ.saveAs(saveas, directory + "Dose of " + title + "_red_" + z1); // case savemode "Choose directory" or "current directory"
				z1++;
				}
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDoseredstack
	



// Calculates the dose from the green values in the image
	public void CalculationOfDosegreen(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title, String saveas, boolean overlay){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		int stak;
		stak = implus.getImageStackSize();
		if (stak  == 1){IJ.run("Duplicate...", " ");	// Make a duplicate of the original input image for later manipulation
		}else{
		IJ.run("Duplicate...", "");	// Make a duplicate of the original input image for later manipulation 
		}
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
		
		if (overlay){
		IJ.run("Set Scale...", "distance=117 known=1 pixel=1 unit=cm global");
		IJ.run("RCF-Rainbow");
		//IJ.run("Duplicate...", "Dose of "+title+"+Bars");
		IJ.run("Calibration Bar...", "location=[Lower Left] fill=Black label=White number=5 decimal=0 font=16 zoom=1 bold overlay");
		//IJ.run("RGB Color");
		IJ.run("Scale Bar...", "width=1 height=2 font=16 color=White background=None location=[Lower Right] bold overlay");
		}else{}
		if (directory!="Empty"){ // Every single image is saved
				for(int z1=0; z1 <= stak; ++z1){
				newimplus.setSlice(z1);
			IJ.saveAs(saveas, directory+"Dose of " + title + "_green_" + z1); // case savemode "Choose directory" or "current directory"
				}
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDosegreenstack






// Calculates the dose from the blue values in the image
	public void CalculationOfDoseblue(String directory, int i, double p1, double p2, double p3, double bckg, double unexp, double maxdos, String title, String saveas, boolean overlay){
		
		// Initialize
		String name;
		ImagePlus implus = WindowManager.getImage(title); // accesses the original image
		
		ImagePlus newimplus; // accesses the new image that Dose Values are saved into
		ImageProcessor ip; // IP for implus
		ImageProcessor newip; // IP for new implus
		double value, netOD, Dosis; // variables for calculation of dose
		int stak;
		stak = implus.getImageStackSize();
		if (stak  == 1){IJ.run("Duplicate...", " ");	// Make a duplicate of the original input image for later manipulation
		}else{
		IJ.run("Duplicate...", "");	// Make a duplicate of the original input image for later manipulation 
		}
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
		
		if (overlay){
		IJ.run("Set Scale...", "distance=117 known=1 pixel=1 unit=cm global");
		IJ.run("RCF-Rainbow");
		//IJ.run("Duplicate...", "Dose of "+title+"+Bars");
		IJ.run("Calibration Bar...", "location=[Lower Left] fill=Black label=White number=5 decimal=0 font=16 zoom=1 bold overlay");
		//IJ.run("RGB Color");
		IJ.run("Scale Bar...", "width=1 height=2 font=16 color=White background=None location=[Lower Right] bold overlay");
		}else{}
		if (directory!="Empty"){ // Every single image is saved
				for(int z1=0; z1 <= stak; ++z1){
				newimplus.setSlice(z1);
			IJ.saveAs(saveas, directory+"Dose of "+title + "_blue_" + z1); // case savemode "Choose directory" or "current directory"
				}
		} //if
		else; //case savemode=Don't save
	}// CalculationOfDosebluestack



			public double[] show() throws FileNotFoundException {  //Method for reading out file including setting parameters
	        double[] parameter = {0,0,0,0,0,0,0};
	        String selection;
	        int j1 = 0;
	        int k1 = 0;
	        int num1 = 10;
	        int j = 0;
	        int k = 1;
	        int num = 10;
	        
	        GenericDialog gad = new GenericDialog("Parameter values", IJ.getInstance());
	        Scanner scanne = new Scanner(new File(valuefiledirectory)); 	        // -define .csv file in app
	        scanne.useDelimiter(";");
	        while(scanne.hasNext()){
	        if (j1 == num1){
	        num1 = num1 + 10; k1++;
	        }else{scanne.next(); }
	        j1++;
	        }
	        scanne.close();
	        String[] paramset = new String[k1 + 1];
	        paramset[0] = "select profile";
	        
	        Scanner scann = new Scanner(new File(valuefiledirectory)); 	        // -define .csv file in app
	        scann.useDelimiter(";");
	        while(scann.hasNext()){
	        if (j == num){
	        	scann.next();
	        paramset[k] = scann.next(); //+ "     	" + scanne.next();
	        num = num + 8; 
	        k++;
	        }else{scann.next(); }
	        j++;
	        }
	        gad.addChoice("Number of parameter set to be loaded:", paramset, "select profile" );
	        gad.addMessage("To create a new profile add it to Lutraparameters.csv");
	        if (gad.wasCanceled()){return(parameter);}else{} // User may cancel Plugin
	        gad.showDialog();
	        selection = gad.getNextChoice();
	        if (gad.wasCanceled()){return(parameter);}else{
	        scann.close();
	       	Lutra_ d = new Lutra_();
			try{
			parameter = d.read(selection);
			return(parameter);
			}
			catch (FileNotFoundException f){
		 	IJ.error("File with parameters for calculation not found.");
			return(parameter);		}
	   		}}

	
     public double[] read(String selection) throws FileNotFoundException {
        // -define .csv file in app
        int num = 0;
         Scanner scannen = new Scanner(new File(valuefiledirectory)); 	        // -define .csv file in app
	        scannen.useDelimiter(";");
	        while(scannen.hasNext()){
	        if (selection.equals(scannen.next())){
	        num++;
	        break;
	        }else{num++;}
	        }
	        scannen.close();
        Scanner scanner = new Scanner(new File(valuefiledirectory));
        scanner.useDelimiter(";");
        double parameter[] = new double[7];
        int i = 0;
        while(scanner.hasNext()){
        if (i -1 == num){
        	//IJ.error("now");
        parameter[0] = scanner.nextDouble();
        parameter[1] = scanner.nextDouble();
        parameter[2] = scanner.nextDouble();
        parameter[3] = scanner.nextDouble();
        parameter[4] = scanner.nextDouble();
        parameter[5] = scanner.nextDouble(); 
        parameter[6] = 1;
        break;
        }else{scanner.next(); i++ ; /* IJ.error("i"); */}
        }
        scanner.close();
        return parameter;
    }


	
	
	// Main
	public void run(String arg) {
		IJ.register( Lutra_ .class);
		
		// Initialize
		int numberoffiles; // number of files that are to be processed
		double p1,p2,p3,bckg,unexp,maxdos; // parameters for the calibration curve
		String[] savemodes={"Current directory", "Choose directory", "Don't save"}; // Scroll-down for saving options
		String[] saveform ={"tif", "png", "raw"};
		String[] titles; // String containing the titles of the images
		String savemode=new String(); // contains the choosen saving option
		String[] editp={"red", "green", "blue"}; // Scroll-down for editing p-values
		String directory = new String(); // contains the directory that the image will be saved to
		GenericDialog gd, customgd; // interaction with the user
		ImagePlus img; // needed for accessing the opened images
		
		numberoffiles=WindowManager.getImageCount(); // count opened images
		if (numberoffiles==0) {//if no image is opened, dialogue to open images is displayed
			titles=openFiles();
			numberoffiles = titles.length; 
		} // if
		else{ // Names of the opened images are written into the string titles to access them later
			titles=new String[numberoffiles + 1];
			for (int i=0; i<numberoffiles; i++) { // Counts open files
				img = WindowManager.getCurrentImage();
				titles[i]=img.getTitle();
				IJ.run("Put Behind [tab]");
			} // for
			titles[numberoffiles] = "all";
		} // else
		
		
		//Dialogue to get parameters and options
		gd = new GenericDialog("Welcome to Lutra!", IJ.getInstance()); 
		gd.addChoice("Which files?", titles, "all");
		gd.addChoice("Choose Color Channel", editp, "red");
		gd.addCheckbox("Load preset profile?", false);
		gd.addCheckbox("Automatically process (overlay, LUT)", true);
		gd.addChoice("Savemode?", savemodes, "Don't save");
		gd.addChoice("Save as:", saveform, "TIF");
		gd.showDialog();
		if (gd.wasCanceled()) return; // User may cancel Plugin
		
		// Return of user-added parameters and options for dose calculation
		boolean choose = gd.getNextBoolean();
		boolean overlay = gd.getNextBoolean();
		String imagename = gd.getNextChoice();
		String editop = gd.getNextChoice();
		savemode=gd.getNextChoice();
		String saveas = gd.getNextChoice();

		p1 = p2 = p3 = bckg = unexp = maxdos =0;
		if(choose == true){
		Lutra_ da = new Lutra_();
		try{
		double[] parameter = da.show();
		if(parameter[6] == 1){}else{
			IJ.error("No profile selected or selected profile in incorrect formate.");
			return;}
		p1 =  parameter[0];
		p2 =  parameter[1];
		p3 =  parameter[2];
		bckg = parameter[3];
		unexp =  parameter[4];
		maxdos =  parameter[5];
		}
		catch (FileNotFoundException e){
		 IJ.error("File with parameters for calculation not found. Directory set to /Users/Daniel_Itkis/Desktop/Lutraparameters.csv");
			return;}
		}else{}


		if (choose == true){
			customgd = new GenericDialog("Lutra", IJ.getInstance()); //Dialogue that only appears if p-values set to "customized"
			customgd.addMessage("Customize parameters"); 
			customgd.addNumericField(editop + "  " + "p1",p1 , 2);
        	customgd.addNumericField(editop + "  "+ "p2", p2, 2);
        	customgd.addNumericField(editop + "  "+ "p3", p3, 2);
			customgd.addNumericField("Dark current of the scanner", bckg, 1);
			customgd.addNumericField("Transparency of the unexposed film", unexp, 1);
			customgd.addNumericField("Maximum dose film can detect", maxdos, 0);

       		customgd.showDialog();
			if (customgd.wasCanceled()){return;}else{}
		}	
					
			else if (editop.equals("blue")==true && choose == false){
			customgd = new GenericDialog("Lutra", IJ.getInstance()); //Dialogue that only appears if p-values set to "customized"
			customgd.addMessage("Customize parameters blue"); 
			customgd.addNumericField("Blue p1", 103.98, 3);
        	customgd.addNumericField("Blue p2", 1.945, 3);
        	customgd.addNumericField("Blue p3", 0, 3);
			customgd.addNumericField("Dark current of the scanner", 0, 2);
			customgd.addNumericField("Transparency of the unexposed film", 75.7, 2);
			customgd.addNumericField("Maximum dose film can detect", 40, 0);

       		customgd.showDialog();
			if (customgd.wasCanceled()) return;
		p1 =  customgd.getNextNumber();
		p2 =  customgd.getNextNumber();
		p3 =  customgd.getNextNumber();
		bckg = customgd.getNextNumber();
		unexp =  customgd.getNextNumber();
		maxdos =  customgd.getNextNumber();

		}
		else if (editop.equals("green")==true && choose == false){
			customgd = new GenericDialog("Lutra", IJ.getInstance()); //Dialogue that only appears if p-values set to "customized"
			customgd.addMessage("Customize parameters green");
			customgd.addNumericField("Green p1", 19.18214, 3);
        	customgd.addNumericField("Green p2", 44.81919, 3);
        	customgd.addNumericField("Green p3", 2.04147, 3);
        	customgd.addNumericField("Dark current of the scanner", 0, 2);
			customgd.addNumericField("Transparency of the unexposed film", 170.9, 2);
			customgd.addNumericField("Maximum dose film can detect", 40, 0);
        	 customgd.showDialog();
			if (customgd.wasCanceled()) return;
        	p1 =  customgd.getNextNumber();
			p2 =  customgd.getNextNumber();
			p3 =  customgd.getNextNumber();
			bckg = customgd.getNextNumber();
			unexp =  customgd.getNextNumber();
			maxdos =  customgd.getNextNumber();

		}

		else if (editop.equals("red")==true && choose == false){
	   		customgd = new GenericDialog("Lutra", IJ.getInstance()); //Dialogue that only appears if p-values set to "customized"
			customgd.addMessage("Customize parameters red");
			customgd.addNumericField("Red p1", 10.22, 5); // Suggestions for parameters
			customgd.addNumericField("Red p2", 2.97, 5);
			customgd.addNumericField("Red p3", 1.8, 3);
			customgd.addNumericField("Dark current of the scanner", 0, 2);
			customgd.addNumericField("Transparency of the unexposed film", 170, 2);
			customgd.addNumericField("Maximum dose film can detect", 40, 0);
        	 customgd.showDialog();
			if (customgd.wasCanceled()) return;
        	p1 =  customgd.getNextNumber();
			p2 =  customgd.getNextNumber();
			p3 =  customgd.getNextNumber();
			bckg = customgd.getNextNumber();
			unexp =  customgd.getNextNumber();
			maxdos =  customgd.getNextNumber();

		}
		else{return;}
		
		
		// Choice where to save the images
		if (savemode.equals("Choose directory")==true) directory = IJ.getDirectory("Directory to save to..."); //case savemode=Choose directory
		else if (savemode.equals("Current directory")==true) directory = IJ.getDirectory("current"); //case savemode=Current directory
		else directory="Empty"; //case savemode=Don't save

		

		if (editop == "red"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDosered(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1],saveas,overlay); // Calculates dose for every opened image
			i++;
		} // while
		}
		else {IJ.selectWindow(imagename);
			CalculationOfDosered(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,imagename,saveas,overlay); // Calculates dose for every opened image}
		}}
		 
		
		else if (editop == "green"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1],saveas,overlay); // Calculates dose for every opened image
			i++;
		} // while
			}
		else {IJ.selectWindow(imagename);
			CalculationOfDosegreen(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,imagename,saveas,overlay); // Calculates dose for every opened image}
		}}


		else if (editop == "blue"){
		if(imagename == "all"){
		int i = 1; //start value for while
		while (i <= numberoffiles) { // go through each of the opened images 
			IJ.selectWindow(titles[i-1]);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,titles[i-1],saveas,overlay); // Calculates dose for every opened image
			i++;
		} // while
			}
		else {IJ.selectWindow(imagename);
			CalculationOfDoseblue(directory,numberoffiles,p1,p2,p3,bckg,unexp,maxdos,imagename,saveas,overlay); // Calculates dose for every opened image}
		}}
		else{}
	} // void run
} // class Dose_Calculation implements PlugIn