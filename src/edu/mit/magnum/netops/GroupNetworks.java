/*
Copyright (c) 2013-2015 Daniel Marbach

We release this software open source under an MIT license (see below). If this
software was useful for your scientific work, please cite our paper available at:
http://regulatorycircuits.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package edu.mit.magnum.netops;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import edu.mit.magnum.FileParser;
import edu.mit.magnum.Magnum;
import edu.mit.magnum.MagnumUtils;
import edu.mit.magnum.net.Network;


/**
 * Do operations on sets of networks
 */
public class GroupNetworks {

	/** The network directory */
	File networkDir_ = null;
	/** The prefix of the network files */
	String networkFilesPrefix_ = null;
	/** Each entry gives the name and the files of a network set */
	HashMap<String, ArrayList<String>> networkSets_ = null;

	
	// ============================================================================
	// PUBLIC METHODS
	
	/** Constructor */
	public GroupNetworks(File networkDir, File networkGroupFile, String networkFilesPrefix) {
		
		networkDir_ = networkDir;
		networkFilesPrefix_ = networkFilesPrefix;
		initialize(networkGroupFile);
	}

	
	// ----------------------------------------------------------------------------

	/** Perform operation on given network sets */
	public void run() {
		
		for (String name : networkSets_.keySet()) {
			ArrayList<String> filenames = networkSets_.get(name);
			Magnum.log.println("- " + name + " (" + filenames.size() + " networks)\n");
			
			Union union = new Union(networkDir_, filenames);
			Network net = union.run();
			
			String filename = new File(Magnum.set.outputDirectory_, networkFilesPrefix_ + name + ".txt").getPath();
			net.write(filename);
		}
	}
	
	
	// ============================================================================
	// PRIVATE METHODS

	/** Initialize the sets of network files for which the union will be computed */
	private void initialize(File networkGroupFile) {
		
		networkSets_ = new HashMap<String, ArrayList<String>>();
		
		if (networkGroupFile == null) {
			ArrayList<String> filenames = MagnumUtils.listFiles(networkDir_);
			networkSets_.put("_networkUnion", filenames);
			Magnum.log.println("- " + filenames.size() + " files in network directory");
			return;
		}

		FileParser parser = new FileParser(networkGroupFile);
		// Skip header
		//parser.skipLines(1);

		while (true) {
			// Read line
			String[] nextLine = parser.readLine();
			if (nextLine == null)
				break;
			
			if (nextLine.length != 2)
				parser.error("Expected two columns");
			
			// The name
			String name = nextLine[1].replace(" ", "_").replace("(", "").replace(")", "").replace("'", "").replace(",", "").toLowerCase();
			// Get this set, create new one if it doesn't exist yet
			ArrayList<String> files = networkSets_.get(name);
			if (files == null) {
				files = new ArrayList<String>();
				networkSets_.put(name, files);
			}
			
			// Create filename
			String filename = networkFilesPrefix_ + nextLine[0] + ".txt.gz";
			
			// Add the file
			if (files.contains(filename))
				parser.error("File listed multiple times for the same set");
			files.add(filename);	
		}
		parser.close();		
		Magnum.log.println("- Initialized " + networkSets_.size() + " network sets");
		Magnum.log.println();
	}

	
	// ============================================================================
	// GETTERS AND SETTERS
	

}

