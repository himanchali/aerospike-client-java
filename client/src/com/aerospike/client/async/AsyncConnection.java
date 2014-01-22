/*
 * Aerospike Client - Java Library
 *
 * Copyright 2013 by Aerospike, Inc. All rights reserved.
 *
 * Availability of this source code to partners and customers includes
 * redistribution rights covered by individual contract. Please check your
 * contract for exact rights and responsibilities.
 */
package com.aerospike.client.async;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Log;
import com.aerospike.client.util.Util;

/**
 * Asynchronous socket channel connection wrapper.
 */
public final class AsyncConnection {
	private final SocketChannel socketChannel;
	private final SelectorManager manager;
	private SelectionKey key;
	private final long maxSocketIdleMillis;
	private long lastUsed;
	
	public AsyncConnection(InetSocketAddress address, AsyncCluster cluster) throws AerospikeException.Connection {
		this.manager = cluster.getSelectorManager();
		this.maxSocketIdleMillis = (long)cluster.getMaxSocketIdle() * 1000L;
		
		try {
			socketChannel = SocketChannel.open();
		}
		catch (Exception e) {
			throw new AerospikeException.Connection("SocketChannel open error: " + e.getMessage());
		}

		try {
			socketChannel.configureBlocking(false);
			Socket socket = socketChannel.socket();
			socket.setTcpNoDelay(true);
			
			// These options are useful when the connection pool is poorly bounded or there are a large
			// amount of network errors.  Since these conditions are not the normal use case and
			// the options could theoretically result in latent data being sent to new commands, leave
			// them out for now.
			// socket.setReuseAddress(true);
			// socket.setSoLinger(true, 0);
			
			socketChannel.connect(address);
			lastUsed = System.currentTimeMillis();
		}
		catch (Exception e) {
			close();
			throw new AerospikeException.Connection("SocketChannel init error: " + e.getMessage());
		}
	}
	
	public void execute(AsyncCommand command) {
		manager.execute(command);
	}

    public void register(AsyncCommand command, Selector selector) throws ClosedChannelException {
    	if (key != null) {
			key.attach(command);
			key.interestOps(SelectionKey.OP_WRITE);
    	}
    	else {
    		key = socketChannel.register(selector, SelectionKey.OP_CONNECT, command);    		
    	}
    }
    
    public void unregister() {
    	key.interestOps(0);
    	key.attach(null);
    }

    public void write(ByteBuffer byteBuffer) throws IOException {
		socketChannel.write(byteBuffer);
    	
		if (! byteBuffer.hasRemaining()) {
			byteBuffer.clear();
			byteBuffer.limit(8);
			key.interestOps(SelectionKey.OP_READ);
		}
    }
    
    public void setReadable() {   	
		key.interestOps(SelectionKey.OP_READ);
    }

    /**
     * Read till byteBuffer limit reached or received would-block.
     */
    public boolean read(ByteBuffer byteBuffer) throws IOException {
		while (byteBuffer.hasRemaining()) {
			int len = socketChannel.read(byteBuffer);
			
			if (len == 0) {			
				// Got would-block.
				return false;
			}
			
			if (len < 0) {
				// Server has shutdown socket.
		    	throw new EOFException();
			}
		}
		return true;
    }

	/**
	 * Is socket connected and used within specified limits.
	 */
	public boolean isValid() {
		return socketChannel.isConnected() && (System.currentTimeMillis() - lastUsed) <= maxSocketIdleMillis;
	}
	
	/**
	 * Is socket connected.
	 */
	public boolean isConnected() {
		return socketChannel.isConnected();
	}
		
	public void updateLastUsed() {
		lastUsed = System.currentTimeMillis();
	}

	/**
	 * Close socket channel.
	 */
	public void close() {
		if (key != null) {
			key.cancel();
			key = null;
		}
		
		try {
			socketChannel.close();
		}
		catch (Exception e) {
			if (Log.debugEnabled()) {
				Log.debug("Error closing socket: " + Util.getErrorMessage(e));
			}
		}
	}
}
