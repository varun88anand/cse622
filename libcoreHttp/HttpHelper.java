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


/**

 */
public class HttpHelper {

	public static final HttpHelper INSTANCE = new HttpHelper();
	
	public boolean useBoth;
	public boolean WIFI;
	public boolean MOBILE;
	
	public HttpConnection wifiConnection;
	public HttpConnection mobileConnection;
	public HttpEngine wifiEngine;
	public HttpEngine mobileEngine;
	public Socket wifiSocket;
	public Socket mobileSocket;
	
	public HttpHelper(){
		/* Check system properties or other service to see if two connections will be used */
		String checkBoth = System.getProperty("network.useBoth");
		if(checkBoth != null && Boolean.parseBoolean(checkBoth)) {
			useBoth = true;
		//	WIFI = true;
		//	MOBILE = true;
		}
		else {
			useBoth = true;
		//	WIFI = true;
		//	MOBILE = true;
		}
	}
	
	public HttpHelper(String network) {
		useBoth = true;
		WIFI = network.equals("wifi")? true:false;
		MOBILE = network.equals("mobile")? true:false;
	}
	
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
