package com.aerospike.examples;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;

public class Batch extends Example {

	public Batch(Console console) {
		super(console);
	}

	/**
	 * Batch multiple gets in one call to the server.
	 */
	@Override
	public void runExample(AerospikeClient client, Parameters params) throws Exception {
		String keyPrefix = "batchkey";
		String valuePrefix = "batchvalue";
		String binName = params.getBinName("batchbin");  
		int size = 8;

		writeRecords(client, params, keyPrefix, binName, valuePrefix, size);
		batchExists(client, params, keyPrefix, size);
		batchReads(client, params, keyPrefix, binName, size);
	}

	/**
	 * Write records individually.
	 */
	private void writeRecords(
		AerospikeClient client,
		Parameters params,
		String keyPrefix,
		String binName,
		String valuePrefix,
		int size
	) throws Exception {
		for (int i = 1; i <= size; i++) {
			Key key = new Key(params.namespace, params.set, keyPrefix + i);
			Bin bin = new Bin(binName, valuePrefix + i);
			
			console.info("Put: ns=%s set=%s key=%s bin=%s value=%s",
				key.namespace, key.setName, key.userKey, bin.name, bin.value);
			
			client.put(params.writePolicy, key, bin);
		}
	}

	/**
	 * Check existence of records in one batch.
	 */
	private void batchExists (
		AerospikeClient client, 
		Parameters params,
		String keyPrefix,
		int size
	) throws Exception {
		// Batch into one call.
		Key[] keys = new Key[size];
		for (int i = 0; i < size; i++) {
			keys[i] = new Key(params.namespace, params.set, keyPrefix + (i + 1));
		}

		boolean[] existsArray = client.exists(params.policy, keys);

		for (int i = 0; i < existsArray.length; i++) {
			Key key = keys[i];
			boolean exists = existsArray[i];
            console.info("Record: ns=%s set=%s key=%s, exists=%s",
            	key.namespace, key.setName, key.userKey, exists);                        	
        }
    }

	/**
	 * Read records in one batch.
	 */
	private void batchReads (
		AerospikeClient client, 
		Parameters params,
		String keyPrefix,
		String binName,
		int size
	) throws Exception {
		// Batch gets into one call.
		Key[] keys = new Key[size];
		for (int i = 0; i < size; i++) {
			keys[i] = new Key(params.namespace, params.set, keyPrefix + (i + 1));
		}

		Record[] records = client.get(params.policy, keys, binName);

		for (int i = 0; i < records.length; i++) {
			Key key = keys[i];
			Record record = records[i];
            console.info("Record: ns=%s set=%s key=%s, bin=%s value=%s",
            	key.namespace, key.setName, key.userKey, binName, record.getValue(binName));                        	
        }
		
		if (records.length != size) {
        	console.error("Record size mismatch. Expected %d. Received %d.", size, records.length);
		}
    }
}