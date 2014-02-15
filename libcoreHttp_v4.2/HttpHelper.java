/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package libcore.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import libcore.io.IoUtils;
import libcore.util.Objects;
import java.util.LinkedList;
/**

 */
public class HttpHelper {

	public static final HttpHelper INSTANCE = new HttpHelper();

	public boolean WIFI;
	public boolean MOBILE;
	
	public Socket wifiSocket;
	public Socket mobileSocket;
	private final int chunkSize;
	private int marker;
	public boolean isComplete;
	
	public boolean isGarbled; /* For more redirects exceeding HttpEngine.MAX_REDIRECTS */

	public boolean supportByteRequest;
	private LinkedList<Integer> missingChunks;

	public Object lock;
	private final int noOfBuckets = 6;
	private InputStream noByteRequestSupport = null;
	public ByteArrayBackedInputStream bArrInpStream = null;
	
	public static Object joinLock = new Object();

	public HttpHelper(){
		chunkSize = 1000000;
		marker = 0;
		lock = new Object();
		isComplete = false;
		isGarbled = false;
		supportByteRequest = true;
		bArrInpStream = new ByteArrayBackedInputStream(this, noOfBuckets, chunkSize);
		missingChunks = new LinkedList<Integer>();
	}
	
	public int getChunkSize() {
		return this.chunkSize;
	}

	public synchronized int getMarker() {
		return this.marker;
	}
	
	public synchronized void setMarker(int value) {
		this.marker = value;
	}

	/* Returns the consolidated stream */
	public InputStream getStream() throws Exception{
		
		if(!supportByteRequest) {
			System.out.println("622 - getStream(): NO Support for BYTE RANGE REQUESTs");
			return noByteRequestSupport;
		}
		else {
			System.out.println("622 - getStream(): Buffering Case");
			return bArrInpStream;
		}
		
	}

	
	public synchronized void setInputStream(InputStream value) {
		noByteRequestSupport = value;
	}

	public synchronized void insertToMissingList(int value) {
		if(!missingChunks.contains(value))
			missingChunks.add(value);
	}

	public synchronized int getNextMissingChunk() {
		if(missingChunks.size() != 0)
			return missingChunks.removeFirst();
		return -1;
	}
	
	public void connectionFor(String network) {
		if(network.equals("wifi")) {
			this.WIFI = true;
			this.MOBILE = false;
		}
		else if(network.equals("mobile")) {
			this.WIFI = false;
			this.MOBILE = true;
		}			
	}

}
