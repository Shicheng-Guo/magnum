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
package edu.mit.magnum.netprop;

import edu.mit.magnum.*;
import edu.mit.magnum.net.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;


/**
 * Run the different network analyses based on Settings
 */
public class NetpropMain {

	/** The network that is being analyzed */
	Network network_ = null;
	/** The different analyzers */
	ArrayList<NetworkProperties> analyzers_ = null;
			
	
	// ============================================================================
	// PUBLIC METHODS
	
	/** Constructor */
	public NetpropMain(Network network) {
		
		network_ = network;
		analyzers_ = new ArrayList<NetworkProperties>();

		// Basic node properties
		if (Settings.computeDegree_ || Settings.computeBetweenness_ || Settings.computeClusteringCoefficient_)
			analyzers_.add(new BasicProperties(network));
		
		// Shortest paths
		if (Settings.computeShortestPathLengths_)
			analyzers_.add(new ShortestPaths(network, Settings.exportNodeProperties_));
		
		// P-step kernel
		if (Settings.computePstepKernel_)
			analyzers_.add(new PstepKernel(network, Settings.pstepKernelAlpha_, Settings.pstepKernelP_, Settings.pstepKernelNormalize_, Settings.exportNodeProperties_));

		// Tanimoto coefficient between TFs
		if (Settings.computeTfTanimoto_)
			analyzers_.add(new TanimotoCoefficient(network, false, Settings.exportNodeProperties_));

		// Tanimoto coefficient between targets 
		if (Settings.computeTargetTanimoto_)
			analyzers_.add(new TanimotoCoefficient(network, true, Settings.exportNodeProperties_));
}
	
	
	// ----------------------------------------------------------------------------

	/** Run all analyzers */
	public ArrayList<Double> runAll() {
		
		ArrayList<Double> networkMeans = new ArrayList<Double>();
		
		for (int i=0; i<analyzers_.size(); i++) {
			ArrayList<Double> means = analyzers_.get(i).run();
			if (means != null)
				networkMeans.addAll(means);
		}
		return networkMeans;
	}

	
	// ----------------------------------------------------------------------------

	/** Export all results to text files */
	public void saveAll() {
		
		// Construct the output filename (remove path and file extension from input filename)
		String basicFilename = MagnumUtils.extractBasicFilename(network_.getFilename(), false);
		
		// Collect node properties from the analyzers and save together in one file
		saveNodeProperties(basicFilename);
		
		// Save other results
		for (int i=0; i<analyzers_.size(); i++)
			analyzers_.get(i).saveK(basicFilename);
	}

	
	// ============================================================================
	// PRIVATE METHODS

	/** Collect node properties from the analyzers and save together in one file */
	private void saveNodeProperties(String basicFilename) {
		
		// Collect node properties from analyzers
		LinkedHashMap<String,Number[]> nodeProperties = new LinkedHashMap<String,Number[]>();
		for (NetworkProperties a : analyzers_)
			a.addNodeProperties(nodeProperties);
		// Return if empty
		if (nodeProperties.size() == 0)
			return;
		
		// The ids of the node properties
		//Set<String> idset = nodeProperties.keySet();
		//String[] ids = (String[]) idset.toArray();
		ArrayList<String> ids = new ArrayList<String>(nodeProperties.keySet());
		
		// Add suffix to filename
		String directionality = network_.getIsDirected() ? "_dir" : "_undir";
		String weighted = network_.getIsWeighted() ? "_weighted" : "";
		basicFilename += "_nodeProperties" + weighted + directionality + ".txt";

		// The file writer
		FileExport writer = new FileExport(basicFilename, Settings.compressFiles_);
		
		// Write the header
		for (int i=0; i<ids.size(); i++)
			writer.print("\t" + ids.get(i));
		writer.print("\n");
		
		// The number of nodes
		int numNodes = network_.getNumNodes();
		// Check it's the same for all node properties
		for (Number[] prop : nodeProperties.values())
			if (prop.length != numNodes)
				throw new RuntimeException("Number of nodes in analyzer not consistent with network");
		
		for (int i=0; i<numNodes; i++) {
			// Node label
			writer.print(network_.getNode(i).getId());
			// Node properties
			for (Number[] prop : nodeProperties.values())
				writer.print("\t" + prop[i]);
			writer.print("\n");
		}
		// Close writer
		writer.close();		
	}

	
}