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
import java.util.HashMap;
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
	/* Minimum value should be 4, 2 buckets for each worker and 2 for the SPECIAL case */
	private final int noOfBuckets = 4;//6;
	private InputStream noByteRequestSupport = null;
	public ByteArrayBackedInputStream bArrInpStream = null;
	
	public static Object joinLock = new Object();
	
	private class Stats {
		public long startTime;
		public long endTime;
		public int type;
		
		public Stats(long start, long end, int type) {
			startTime = start;
			endTime = end;
			this.type = type;
		}
	}

	private HashMap<Integer, Stats> statistics;

	public HttpHelper(){
		chunkSize = 100;// 100 bytes
		marker = 0;
		lock = new Object();
		isComplete = false;
		isGarbled = false;
		supportByteRequest = true;
		bArrInpStream = new ByteArrayBackedInputStream(this, noOfBuckets, chunkSize);
		missingChunks = new LinkedList<Integer>();
		statistics = new HashMap<Integer, Stats>(400);
	}

	public void dumpStatistics(int wifiBytes, int mobileBytes, int total) {
		System.out.println("622 - **************STATISTICS********************");
		int start = 0;
		long totalTime = 0;
		long wifiTime = 0;
		long mobileTime = 0;
		int wifiDownloads = 0;
		int mobileDownloads = 0;
		for(int i=0; i< statistics.size(); i++) {
			Stats obj = statistics.get(start);
			long time = (obj.endTime - obj.startTime);
			String type = (obj.type == 1)?"WIFI":"MOBILE";
			System.out.println("622: Start = " + start + " || Time(ms) = " + time + " || Type = " + type);  
			if(obj.type == 1) {
				wifiTime += time;
				wifiDownloads++;
			}
			else {
				mobileTime += time;
				mobileDownloads++;
			}
			start += chunkSize;
			//totalTime += time;
		}
		/* Total time is the greater of the two, because they overlap */
		totalTime = (wifiTime > mobileTime)?wifiTime:mobileTime;
		float netRate = (float)(total/1024)/(float)(totalTime/1000);
		float wifiRate = (float)(wifiBytes/1024)/(float)(wifiTime/1000);
		float mobileRate = (float)(mobileBytes/1024)/(float)(mobileTime/1000);
		System.out.println("622 - *****************************Results*******************************");
		System.out.println("622: Wifi: #chunks = " + wifiDownloads + " || time taken(ms) = " + wifiTime + " || #bytes = "
							+ wifiBytes + " || Download Rate(kbps) = " + wifiRate);
		System.out.println("622: Mobile: #chunks = " + mobileDownloads + " || time taken(ms) = " + mobileTime + " || #bytes = "
							+ mobileBytes + " || Download Rate(kbps) = " + mobileRate);
		System.out.println("622: Downloaded total " + total + " bytes in " + totalTime + " ms" + " at an average rate = " + netRate);
	}

	public synchronized void insertStatistics(int chunkStart, long start, long end, int type) {
		Stats obj = new Stats(start, end, type);
		statistics.put(chunkStart, obj);
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
