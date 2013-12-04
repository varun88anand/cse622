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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import libcore.io.IoUtils;
import libcore.util.Objects;
import org.apache.harmony.xnet.provider.jsse.OpenSSLSocketImpl;
import java.net.NetworkInterface;
//import java.util.TreeMap;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.SequenceInputStream;
/**

 */
public class HttpHelper {

public static final HttpHelper INSTANCE = new HttpHelper();
	//public final HttpHelper INSTANCE2 = new HttpHelper();

	public volatile boolean useBoth;
	public boolean WIFI;
	public boolean MOBILE;
	
	//public HttpConnection wifiConnection;
	//public HttpConnection mobileConnection;
	public HttpEngine wifiEngine;
	public HttpEngine mobileEngine;
	public Socket wifiSocket;
	public Socket mobileSocket;
	private final int chunkSize;
	private int marker;
	public boolean isComplete;
	

	public Object lock;
	//@TODO - Change it to HashMap
	//private TreeMap<Integer, InputStream> map;
	private HashMap<Integer, InputStream> map;
	
	public HttpHelper(){
		useBoth = false;
		chunkSize = 1000000;
		marker = 0;
		lock = new Object();
		map = new HashMap<Integer, InputStream>();
		isComplete = false;
	}
	
	public int getChunkSize() {
		return this.chunkSize;
	}

	public int getMarker() {
		return this.marker;
	}
	
	public void setMarker(int value) {
		this.marker = value;
	}

	/* Returns the consolidated stream */
	public InputStream getStream() {
		//return this.map.get(key);
		InputStream result = null;
		synchronized(this) {
		int start = 0;
		int size = map.size();
		System.out.println("622 - Size of MAP = " + size);
		
		if(size == 0)
			return result;
		result = map.get(start);
		int i = 1;
		ArrayList<Integer> missingChunks = new ArrayList<Integer>();
		if(result == null)
			missingChunks.add(start);
		while(i < size){
			start = start + getChunkSize();
			InputStream tmp = map.get(start);
			if(tmp == null)
				missingChunks.add(start);
			else
			{
				InputStream intrm = new SequenceInputStream(result, tmp);
				result = intrm;
			}
			i++;
		}

		if(missingChunks.size() > 0)
			System.out.println("622 - getStream(): No. of Missing chunks = " + missingChunks.size() + "\nvalues = " + missingChunks);
		}
		return result;
	}

	public void insertResultToMap(int key, InputStream value) {
		synchronized(this) {
			map.put(key, value);
		}	
	}


	/*
	public HttpHelper(String network) {
		useBoth = true;
		WIFI = network.equals("wifi")? true:false;
		MOBILE = network.equals("mobile")? true:false;
	}
	*/

	
	public void update(HttpHelper httpHelper) {
		this.useBoth = httpHelper.useBoth;
		this.WIFI = httpHelper.WIFI;
		this.MOBILE = httpHelper.MOBILE;
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
	
	public void check() {
		String checkBoth = System.getProperty("network.useBoth");
		if(checkBoth != null && Boolean.parseBoolean(checkBoth)) {
			useBoth = true;
			WIFI = true;
			MOBILE = true;
		}
	}

	public void setWifiAddress(HttpHelper httpHelper) {
	
	}
	
	public void setMobileAddress(HttpHelper httpHelper) {
	
	}

	public void setEngine(HttpEngine httpEngine) {
		
	
	
	}
	
	public void setConnection(HttpConnection connection) {
	
	
	}
	
	
	public void setSocket(Socket sock) {
	
	
	}
	
    
}
