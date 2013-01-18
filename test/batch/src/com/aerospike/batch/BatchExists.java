/*
 *  Copyright 2012 by Aerospike, Inc.  All rights reserved.
 *  
 *  Availability of this source code to partners and customers includes
 *  redistribution rights covered by individual contract. Please check
 *  your contract for exact rights and responsibilities.
 */
package com.aerospike.batch;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.policy.Policy;

public class BatchExists {

	public static boolean batchExistsTest(Parameters params) throws AerospikeException {
		AerospikeClient client = params.client;

		if (params.verbose)
		  System.out.println("Calling batchExists() API....");

		Policy policy = new Policy();
		policy.timeout = params.timeout;

		boolean cr[] = client.exists(policy, params.keys);

		int numFound = 0;
		for (int i = 0; i < cr.length; i++) {
			if (cr[i]) {
				numFound++;
				
				if (params.verbose)
					System.out.println("\tKey \"" + params.keys[i].userKey + "\" exists.");
			} 
			else {
				if (params.verbose)
					System.out.println("\tError! batchExists() returned " + false + " for key \"" + params.keys[i].userKey + "\"");
			}
		} 

		System.out.println("\tIn the batch of " + params.keys.length + " keys, " + numFound + " keys exist.");

		if (params.verbose)
		  System.out.println("\tOK ~~ batchExists() succeeded.");

		return true;
	}

	public static void main(String[] args) {

		Options arg_options = new Options();
		arg_options.addOption("h", "host", true, "hostname to query");
		arg_options.addOption("p", "port", true, "port to query [3000]");
		arg_options.addOption("n", "namespace", true, "namespace for all queries");
		arg_options.addOption("u", "usage", false, "print the usage info");
		arg_options.addOption("s", "set", true, "set to use for this test (default: emptystring)");
		arg_options.addOption("b", "bin", true, "bin to use for this test (default: emptystring)");
		arg_options.addOption("i", "insert", true, "insert data (false | true) (default: false)");
		arg_options.addOption("t", "startKey", true, "starting key (default: 0)");
		arg_options.addOption("k", "keys", true, "number of keys (default: 10)");
		arg_options.addOption("l", "keylength", true, "key length (default: 10)");
		arg_options.addOption("m", "timeout", true, "timeout in milliseconds");		
		arg_options.addOption("r", "iteRations", true, "number of iterations (default: 1)");
		arg_options.addOption("v", "verbose", true, "verbose (false | true) (default: false)");
		arg_options.addOption("", "help", false, "get help quick!");
		CommandLineParser clp = new GnuParser();

		try {
			CommandLine cl = clp.parse(arg_options, args, false);

			if (cl.hasOption("help") || cl.hasOption("usage")) {
				// Automatically generate the usage information.
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("batch_exists_java {<Option>*}", arg_options);
				return;
			}
			
			Parameters params = new Parameters();
			params.host = cl.getOptionValue("h", "127.0.0.1");
			String port_s = cl.getOptionValue("p", "3000");
			params.port = Integer.valueOf(port_s);
			params.namespace = cl.getOptionValue("n", "test");
			params.set = cl.getOptionValue("s", "");
			params.bin = cl.getOptionValue("b", "");
			params.insertData = cl.getOptionValue("i", "false").equals("true");
			String startKey_s = cl.getOptionValue("t", "0");
			params.startKey = Integer.valueOf(startKey_s);
			String numKeys_s = cl.getOptionValue("k", "10");
			params.numKeys = Integer.valueOf(numKeys_s);
			
			String keylength_s = cl.getOptionValue("l", "0");
			params.keyLength = Integer.valueOf(keylength_s);			
			if (params.keyLength == 0) {
				params.keyLength = (Integer.toString(params.numKeys + params.startKey)).length();	
			}
			
			String timeout_s = cl.getOptionValue("m", "0");
			params.timeout = Integer.valueOf(timeout_s);			

			String numIterations_s = cl.getOptionValue("r", "1");
			params.numIterations = Integer.valueOf(numIterations_s);
			params.verbose =  cl.getOptionValue("v", "false").equals("true");
			
			runTests(params);
			
		} catch (Exception ex) {
			System.out.println("Error: " + ex.toString());
		}	
	}
	
	private static void runTests(Parameters params) throws AerospikeException {

		System.out.println(" Java Batch Key Exists Test : cluster " + params.host + ":" + params.port + " ns: " +
						   params.namespace + " set: " + params.set + " bin: " + params.bin +
						   " insert data: " + params.insertData + " start key: " + params.startKey + 
						   " num keys: " + params.numKeys + " key length: " + params.keyLength);

		// Connect to the configured cluster
		params.client = new AerospikeClient(params.host, params.port);
		
		try {
			runBatchExists(params);
		}
		finally {
			params.client.close();
		}
	}
	
	private static void runBatchExists(Parameters params) throws AerospikeException {

		System.out.println("\nRunning Java Batch Key Existence Tests....\n");

		// Generate the array of keys to be used in the test.
		System.out.println("Generating keys....");
		if (!Util.generateKeys(params)) {
			System.out.println("\tError! Failed to generate keys.");
			return;
		}
		System.out.println("\tOK ~~ Generated keys.");

		if (params.insertData) {
			System.out.println("\nInserting test data....");
			Util.batchInsertTestData(params);
		}

		// Test the batch key exists API.
		System.out.println("\nTesting batch key exists feature....");

		for (int i = 0; i < params.numIterations; i++) {
			if (params.verbose)
			  System.out.println("***Iteration " + i + "***");
			batchExistsTest(params);
		}
	}
}