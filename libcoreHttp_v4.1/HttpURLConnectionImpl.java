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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.SocketPermission;
import java.net.URL;
import java.nio.charset.Charsets;
import java.security.Permission;
import java.util.List;
import java.util.Map;
import libcore.io.Base64;
import libcore.io.IoUtils;
import java.io.SequenceInputStream;

/**
 * This implementation uses HttpEngine to send requests and receive responses.
 * This class may use multiple HttpEngines to follow redirects, authentication
 * retries, etc. to retrieve the final response body.
 *
 * <h3>What does 'connected' mean?</h3>
 * This class inherits a {@code connected} field from the superclass. That field
 * is <strong>not</strong> used to indicate not whether this URLConnection is
 * currently connected. Instead, it indicates whether a connection has ever been
 * attempted. Once a connection has been attempted, certain properties (request
 * header fields, request method, etc.) are immutable. Test the {@code
 * connection} field on this class for null/non-null to determine of an instance
 * is currently connected to a server.
 */
class HttpURLConnectionImpl extends HttpURLConnection {

	//private int flag = 0;
	private final int defaultPort;

    private Proxy proxy;

    private final RawHeaders rawRequestHeaders = new RawHeaders();

    private int redirectionCount;
    
    public URL mUrl;
    protected IOException httpEngineFailure;
    protected HttpEngine httpEngine;
 
   /*NNNNNNN 622 new NNNNNN*/
    //protected HttpEngine httpEngine2;
    
    //private RawHeaders wifiRawReqHeaders = new RawHeaders();
    //private RawHeaders mobileRawReqHeaders = new RawHeaders();		

    private HttpHelper httpHelper = new HttpHelper();	
	
	private volatile ConnectionStatus connStatus = new ConnectionStatus(httpHelper);

    private volatile HttpWorker wkr_wifi;
	private volatile HttpWorker wkr_mobile;
	
	boolean isAnyInterfaceUp = false;

	//protected IOException wifiHttpEngineFailure;
    //protected IOException mobileHttpEngineFailure;

    //private int mobileRedirectCount;
    //private int wifiRedirectCount;
    	

   /*NN*/

    protected HttpURLConnectionImpl(URL url, int port) {
        super(url);
		mUrl = url;
        defaultPort = port;
    }

    protected HttpURLConnectionImpl(URL url, int port, Proxy proxy) {
        this(url, port);
		mUrl = url;
        this.proxy = proxy;
    }

    @Override public final void connect() throws IOException {
//	HttpHelper.INSTANCE.check();
		
	//if(httpHelper == null) {
	    //httpHelper = new HttpHelper();	
	//	System.out.println("CSE622 - This shouldn't be possible... exiting");
	//	System.exit(1);
	//}
        /* init will create two httpEngine objects */ 
	
		//synchronized(HttpHelper.INSTANCE.lock) {
		//System.out.println("622 - From HttpUrlConnection: <<<<<I Should Not Be HERE>>>>>>>CONNECT CALLED !!!");
		initHttpEngine();
		
		//    System.out.println("CSE622: From HttpURLConnImpl - sending request for httpengine");
        //    httpEngine.sendRequest();
	    /*NNN 622 New */ 
	    /*
		if(HttpHelper.INSTANCE.useBoth) {
		
	        if (httpHelper.wifiEngine == null) {
             	    System.out.println("CSE622: From HttpURLConnImpl - sending request for httpengine");
	            HttpHelper.INSTANCE.connectionFor("wifi");
				//httpHelper.connectionFor("wifi");
		    try {	
	                httpEngine.sendRequest();
                    httpHelper.wifiEngine = httpEngine;
		    } catch (IOException e) {
		        wifiHttpEngineFailure = e;
            	throw e;
		    }
		}	
	     
    		if (httpHelper.mobileEngine == null) {
                    System.out.println("CSE622: From HttpURLConnImpl - sending request for httpengine2");
		    	HttpHelper.INSTANCE.connectionFor("mobile");
				//httpHelper.connectionFor("mobile");
		    try {	
	                httpEngine2.sendRequest();
        	        httpHelper.mobileEngine = httpEngine2;
		    } catch (IOException e) {
	                mobileHttpEngineFailure = e;
                        throw e;
	            }
		}
	    }*/
	    //else {
		try { 
	          httpEngine.sendRequest();
	    	} catch (IOException e) {
		    httpEngineFailure = e;
		    throw e;
		}	
	    //} 
    //}
}
    @Override public final void disconnect() {
        // Calling disconnect() before a connection exists should have no effect.
       	
	if (httpEngine != null) {
            // We close the response body here instead of in
            // HttpEngine.release because that is called when input
            // has been completely read from the underlying socket.
            // However the response body can be a GZIPInputStream that
            // still has unread data.
            if (httpEngine.hasResponse()) {
                IoUtils.closeQuietly(httpEngine.getResponseBody());
            }
            httpEngine.release(false);
        }

	/*NN 622*/
	/*
	if (HttpHelper.INSTANCE.useBoth && httpEngine2 != null) {
	    if (httpEngine2.hasResponse()) {
		IoUtils.closeQuietly(httpEngine2.getResponseBody());
	    }
	    httpEngine2.release(false);	
	}*/
	
    }

    /**
     * Returns an input stream from the server in the case of error such as the
     * requested file (txt, htm, html) is not found on the remote server.
     */
    @Override public final InputStream getErrorStream() {
        try {
            HttpEngine response = getResponse();
            if (response.hasResponseBody()
                    && response.getResponseCode() >= HTTP_BAD_REQUEST) {
                return response.getResponseBody();
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the value of the field at {@code position}. Returns null if there
     * are fewer than {@code position} headers.
     */
    @Override public final String getHeaderField(int position) {
        try {
            return getResponse().getResponseHeaders().getHeaders().getValue(position);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the value of the field corresponding to the {@code fieldName}, or
     * null if there is no such field. If the field has multiple values, the
     * last value is returned.
     */
    @Override public final String getHeaderField(String fieldName) {
        try {
            RawHeaders rawHeaders = getResponse().getResponseHeaders().getHeaders();
            return fieldName == null
                    ? rawHeaders.getStatusLine()
                    : rawHeaders.get(fieldName);
        } catch (IOException e) {
            return null;
        }
    }

    @Override public final String getHeaderFieldKey(int position) {
        try {
            return getResponse().getResponseHeaders().getHeaders().getFieldName(position);
        } catch (IOException e) {
            return null;
        }
    }

    @Override public final Map<String, List<String>> getHeaderFields() {
        try {
            return getResponse().getResponseHeaders().getHeaders().toMultimap();
        } catch (IOException e) {
            return null;
        }
    }

    @Override public final Map<String, List<String>> getRequestProperties() {
        if (connected) {
            throw new IllegalStateException(
                    "Cannot access request header fields after connection is set");
        }
        return rawRequestHeaders.toMultimap();
    }

	
	public void setUrl(String newUrl) throws IOException{
		URL previousUrl = url;
		url = new URL(previousUrl, newUrl);
		mUrl = url;
	}

    @Override public final InputStream getInputStream() throws IOException {
        if (!doInput) {
            throw new ProtocolException("This protocol does not support input");
        }
		System.out.println("CSE622 From HttpURLConnectionImpl.java: Inside getInputStream() Calling getResponse() from within\n");

        //HttpEngine response = getResponse();
		getResponse();
		if(httpHelper.isGarbled)
			throw new ProtocolException("Too many redirects");
        /*
         * if the requested file does not exist, throw an exception formerly the
         * Error page from the server was returned if the requested file was
         * text/html this has changed to return FileNotFoundException for all
         * file types
         */
        //if (getResponseCode() >= HTTP_BAD_REQUEST && !HttpHelper.INSTANCE.useBoth) {
        //    throw new FileNotFoundException(url.toString());
        //}
        //System.out.println("CSE622 From HttpURLConnectionImpl.java: Inside getInputStream() Calling getResponseBody() on the HttpEngine object response.\n");
        InputStream result = null;
	//synchronized(HttpHelper.INSTANCE.lock) {
	//if (HttpHelper.INSTANCE.useBoth) {
	    //if(isAnyInterfaceUp) {
	    //InputStream wifiResult = httpEngine.getResponseBody();
	    //InputStream mobileResult = httpEngine2.getResponseBody();
	    try{
			//if(flag == 0) {
				System.out.println("CSE622 INside getinputStream - using both streams");
				System.out.println("CSE622 concatenating streams !!!");
				result = httpHelper.getStream();
			/*}
			else
			{
				synchronized(httpHelper.lock) {
					httpHelper.isComplete = true;
				}
				result = httpEngine.getResponseBody();
			}*/
		} catch(Exception e) {
			//if(flag == 0)
				System.out.println("622 - HttpHelper: getStream() exception !!!");
			//else
			//	System.out.println("622 - Exception from getResponse(): 0 or null content-length case");
			e.printStackTrace();
		}
		//}
		//result = new SequenceInputStream(wifiResult, mobileResult);   	
	//}
 	//else {
	//    result = response.getResponseBody();
	//}
	//}
    if (result == null) {
    	System.out.println("622 - InputStream is null!!!");
		throw new IOException("No response body exists");//; responseCode=" + getResponseCode());
    }
	//else if(!isAnyInterfaceUp) {
	//	System.out.println("622 - getInputStream() - Network unreachable");
	//	throw new IOException("622 : Network Unreachable - None of the interfaces are available");
	//}
	//System.out.println("622 - HttpURLConnectionImpl - Killing all the workers");

        //wkr_wifi = null;
        //wkr_mobile = null;
	
	//connStatus = null;
        return result;
    }

    @Override public final OutputStream getOutputStream() throws IOException {
        connect();

        OutputStream result = httpEngine.getRequestBody();
        if (result == null) {
            throw new ProtocolException("method does not support a request body: " + method);
        } else if (httpEngine.hasResponse()) {
            throw new ProtocolException("cannot write request body after response has been read");
        }

        return result;
    }

    @Override public final Permission getPermission() throws IOException {
        String connectToAddress = getConnectToHost() + ":" + getConnectToPort();
        return new SocketPermission(connectToAddress, "connect, resolve");
    }

    private String getConnectToHost() {
        return usingProxy()
                ? ((InetSocketAddress) proxy.address()).getHostName()
                : getURL().getHost();
    }

    private int getConnectToPort() {
        int hostPort = usingProxy()
                ? ((InetSocketAddress) proxy.address()).getPort()
                : getURL().getPort();
        return hostPort < 0 ? getDefaultPort() : hostPort;
    }

    @Override public final String getRequestProperty(String field) {
        if (field == null) {
            return null;
        }
        return rawRequestHeaders.get(field);
    }

    private void initHttpEngine() throws IOException {
	/*NN 622 */
    /*
		if (HttpHelper.INSTANCE.useBoth) {
	    	if (wifiHttpEngineFailure != null) 
				throw wifiHttpEngineFailure;
	    	else if (mobileHttpEngineFailure != null) 
				throw mobileHttpEngineFailure;
	    	else if (httpEngine != null && httpEngine2 != null)
				return;
	}*/
	/*NN*/
	//else {
	    if (httpEngineFailure != null) 
            throw httpEngineFailure;
        else if (httpEngine != null) 
	        return;
    //}
        connected = true;
        try {
            if (doOutput) {
                if (method == HttpEngine.GET) {
                    // they are requesting a stream to write to. This implies a POST method
                    method = HttpEngine.POST;
                } else if (method != HttpEngine.POST && method != HttpEngine.PUT) {
                    // If the request method is neither POST nor PUT, then you're not writing
                    throw new ProtocolException(method + " does not support writing");
                }
            }
		rawRequestHeaders.set("Range", "bytes=0-");
		httpEngine = newHttpEngine(method, rawRequestHeaders, null, null);
		//httpEngine.sendRequest();
		//httpEngine.readResponse();
	} catch (IOException e) {
	    httpEngineFailure = e;
		throw e;
	}	

//        System.out.println("CSE622: From HttpURLConnectionImpl.java: Inside (2) initHttpEngine() requesting new HttpEngine with null HttpConnection object");
	/*NNN 622 new */
  	/*
	if (HttpHelper.INSTANCE.useBoth) { 
            if (httpEngine == null) {
                //wifiRawReqHeaders.set("Range", "bytes=0-10");
	        	try {
	            	System.out.println("CSE622: From HttpURLConnectionImpl.java: Inside (2) initHttpEngine() requesting new WIFI HttpEngine with null HttpConnection object");
	            	httpEngine = newHttpEngine(method, wifiRawReqHeaders, null, null);
                    //httpEngine.httpHelper = new HttpHelper("wifi");  
				}catch (IOException e) {
		   			wifiHttpEngineFailure = e;
                	System.out.println("CSE622: From HttpURLConnectionImpl.java: Inside (2) initHttpEngine() EXCEPTION requesting new WIFI HttpEngine");
		   			throw e; 	
				}
	    	}	
	    	if (httpEngine2 == null) {
                //mobileRawReqHeaders.set("Range", "bytes=100-110");
				try {
	            	System.out.println("CSE622: From HttpURLConnectionImpl.java: Inside (2) initHttpEngine() requesting new MOBILE HttpEngine with null HttpConnection object");
		    		httpEngine2 = newHttpEngine(method, mobileRawReqHeaders, null, null);
	            	//httpEngine2.httpHelper = new HttpHelper("mobile"); 
				} catch (IOException e) {
		    		mobileHttpEngineFailure = e;
                    System.out.println("CSE622: From HttpURLConnectionImpl.java: Inside (2) initHttpEngine() EXCEPTION requesting new MOBILE HttpEngine");
		    		throw e; 
				}
	    }
	} 
	else {
	    try { 
	        System.out.println("CSE622: From HttpURLConnectionImpl.java: Inside (2) initHttpEngine() requesting new HttpEngine with null HttpConnection object");
			httpEngine = newHttpEngine(method, rawRequestHeaders, null, null);
	    } catch (IOException e) {
        	httpEngineFailure = e;
            System.out.println("CSE622: From HttpURLConnectionImpl.java: Inside (2) initHttpEngine() EXCEPTION requesting new HttpEngine");
            throw e;
        }
	}*/
    }
    /**
     * Create a new HTTP engine. This hook method is non-final so it can be
     * overridden by HttpsURLConnectionImpl.
     */
    protected HttpEngine newHttpEngine(String method, RawHeaders requestHeaders,
            HttpConnection connection, RetryableOutputStream requestBody) throws IOException {
        return new HttpEngine(this, method, requestHeaders, connection, requestBody);
    }

    /**
     * Aggressively tries to get the final HTTP response, potentially making
     * many HTTP requests in the process in order to cope with redirects and
     * authentication.
     */
    
	/*
		Modification - return 0 if we use 2 worker approach, else 1
	*/
	private HttpEngine getResponse() throws IOException {
        //System.out.println("CSE622 From HttpURLConnectionImpl.java: Inside getResponse() Calling initHttpEngine() from within");
	//if(httpHelper == null) {
            //httpHelper = new HttpHelper();
    //    	System.out.println("622 - Shouldn't be here... exiting");
	//		System.exit(1);
	//	}
        /* init will create two httpEngine objects */
    //synchronized(HttpHelper.INSTANCE.lock) {
		initHttpEngine();
	//}
	//if (HttpHelper.INSTANCE.useBoth) {
	//    if (httpEngine.hasResponse() && httpEngine2.hasResponse()) {
	 //       System.out.println("CSE622 From HttpURLConnectionImpl.java: Inside getResponse() returning - both engines already have Response");
	     //   return httpEngine;/* join their responses and return new engine */
	   // }
	//}
        //else {
            //if (httpEngine.hasResponse()) {
            //  System.out.println("CSE622 From HttpURLConnectionImpl.java: Inside getResponse() returning httpEngine - already hasResponse");
              //  return httpEngine;
	    //}
	//}	       
            /*
			System.out.println("CSE622: From HttpURLConnImpl - Checking if any interface(s) available");
            synchronized(ConnectionStatus.o_wifi) {
                if(!ConnectionStatus.WIFI)
                    {   
                        synchronized(ConnectionStatus.o_mobile) {
                            if(!ConnectionStatus.MOBILE)
                            {   
                                System.out.println("CSE622: From HttpURLConnImpl - No Interfaces available...");
                                synchronized(httpHelper.lock) {
                                    httpHelper.isComplete = true;
                                }   
                                throw new IOException("622 : Network Unreachable - None of the interfaces are available");
                                
								//isAnyInterfaceUp = false;
                                //return httpEngine;
                            }   
                        }   
                    }   
            } */  
	
      	//while (true) {
            try {
		/*NN 622 */
		//if (HttpHelper.INSTANCE.useBoth) {
			//boolean flag_w;
			//boolean flag_m;
			
			// Check Content-Length, before requesting for data
			/*
			httpEngine.sendRequest();
			httpEngine.readResponseHeaders();
			//httpEngine.readResponse();
			String sLength = "empty";
			List values = httpEngine.getResponseHeaders().getHeaders().toMultimap().get("content-length");
			if (values != null && !values.isEmpty())
				sLength = (String) values.get(0);
			System.out.println("CSE622: From HttpURLConnImpl - inside getResponse(): Content Length = " + sLength);
			
			if(sLength.equals("0") || sLength.equals("empty"))
			{
				synchronized(httpHelper.lock) {
					httpHelper.isComplete = true;
				}
				httpEngine.readResponse();
				flag = 1;
				return httpEngine;
			}*/
			
			System.out.println("CSE622: From HttpURLConnImpl - inside getResponse() creating Worker WIFI");
			//HttpWorker wkr_wifi = new HttpWorker(httpHelper, httpEngine, wifiRawReqHeaders, "wifi");
			wkr_wifi = new HttpWorker(this, httpHelper, "wifi");
			System.out.println("CSE622: From HttpURLConnImpl - inside getResponse() creating Worker MOBILE");
			//HttpWorker wkr_mobile = new HttpWorker(httpHelper, httpEngine2, mobileRawReqHeaders, "mobile");
			wkr_mobile = new HttpWorker(this, httpHelper, "mobile");
			/*Wait for Workers to finish*/
			
			//synchronized(httpHelper.lock) {
			System.out.println("622 - HttpURLConnectionImpl - Waiting for workers to complete");
			//	httpHelper.lock.wait();
			//}
			
			//System.out.println("622 - HttpURLConnectionImpl - I got notified by one of the worker");
			//System.out.println("622 - HttpURLConnectionImpl - Killing all the workers");
			
			//wkr_wifi = null;
			//wkr_mobile = null;
			wkr_wifi.join();
			wkr_mobile.join();

            //HttpHelper.INSTANCE.connectionFor("wifi");
	        /*
			wifiRawReqHeaders.set("Range", "bytes=0-10");   
		    httpEngine.updateHeader(wifiRawReqHeaders);
		    System.out.println("CSE622 IMPl: wifiHeader: "+ wifiRawReqHeaders.toHeaderString());
		    httpEngine.sendRequest();
	        httpHelper.wifiEngine = httpEngine;

            System.out.println("CSE622: From HttpURLConnImpl - inside getResponse() sending request for httpengine2");
	   	    //HttpHelper.INSTANCE.connectionFor("mobile");
		    mobileRawReqHeaders.set("Range", "bytes=20-30");
		    httpEngine2.updateHeader(mobileRawReqHeaders);
		    System.out.println("CSE622 impl: mobileHeader: "+ mobileRawReqHeaders.toHeaderString());
            httpEngine2.sendRequest();
            httpHelper.mobileEngine = httpEngine2;

	    	httpEngine.readResponse();
		    httpEngine2.readResponse();
			*/
		//}
		//else {
		    //httpEngine.sendRequest();	
            //httpEngine.readResponse();
		//}			
		/*NN*/
		
            } //catch (IOException e) {
                /*
                 * If the connection was recycled, its staleness may have caused
                 * the failure. Silently retry with a different connection.
                 */
                /*OutputStream requestBody;
		//if (!HttpHelper.INSTANCE.useBoth) {
		    requestBody = httpEngine.getRequestBody();
                    System.out.println("CSE622 Exception - sending request");
               	    e.printStackTrace(); 
    	            if (httpEngine.hasRecycledConnection()
                           && (requestBody == null || requestBody instanceof RetryableOutputStream)) {
                        httpEngine.release(false);
                        httpEngine = newHttpEngine(method, rawRequestHeaders, null,
                            (RetryableOutputStream) requestBody);
                        continue;
                     }
		//}
		/* NN 622 */
		/*
		else if (HttpHelper.INSTANCE.useBoth) {
		    requestBody = httpEngine.getRequestBody();
		    if (httpEngine.hasRecycledConnection()
                           && (requestBody == null || requestBody instanceof RetryableOutputStream)) {
                        httpEngine.release(false);
                        httpEngine = newHttpEngine(method, wifiRawReqHeaders, null,
                            (RetryableOutputStream) requestBody);
                        continue;
                    } 
	
	            requestBody = httpEngine2.getRequestBody();
		    if (httpEngine2.hasRecycledConnection()
                           && (requestBody == null || requestBody instanceof RetryableOutputStream)) {
                        httpEngine2.release(false);
                        httpEngine2 = newHttpEngine(method, mobileRawReqHeaders, null,
                            (RetryableOutputStream) requestBody);
                        continue;
                    }
		}*/
		/*NN*/
             //   httpEngineFailure = e;
               // throw e;
            //}
			catch(InterruptedException e) {
				System.out.println("622 - Thread JOIN Interrupted!!!");
				e.printStackTrace();
			}
	    /*NN 622 ???*/	
	    //if(HttpHelper.INSTANCE.useBoth) 
			
			//if(flag == 1)
				return httpEngine;
			/*
            Retry retry = processResponseHeaders();
            if (retry == Retry.NONE) {
                httpEngine.automaticallyReleaseConnectionToPool();
                return httpEngine;
            }*/

            /*
             * The first request was insufficient. Prepare for another...
             */
            //String retryMethod = method;
            //OutputStream requestBody = httpEngine.getRequestBody();

            /*
             * Although RFC 2616 10.3.2 specifies that a HTTP_MOVED_PERM
             * redirect should keep the same method, Chrome, Firefox and the
             * RI all issue GETs when following any redirect.
             */
            /*
			int responseCode = getResponseCode();
            if (responseCode == HTTP_MULT_CHOICE || responseCode == HTTP_MOVED_PERM
                    || responseCode == HTTP_MOVED_TEMP || responseCode == HTTP_SEE_OTHER) {
                retryMethod = HttpEngine.GET;
                requestBody = null;
            }

            if (requestBody != null && !(requestBody instanceof RetryableOutputStream)) {
                throw new HttpRetryException("Cannot retry streamed HTTP body",
                        httpEngine.getResponseCode());
            }

            if (retry == Retry.DIFFERENT_CONNECTION) {
                httpEngine.automaticallyReleaseConnectionToPool();
            } else {
                httpEngine.markConnectionAsRecycled();
            }

            httpEngine.release(true);
			*/
		/*
	    if(HttpHelper.INSTANCE.useBoth) {
		httpEngine2.release(true);
		httpEngine = newHttpEngine(retryMethod, wifiRawReqHeaders,
                    httpEngine.getConnection(), (RetryableOutputStream) requestBody);
		httpEngine2 = newHttpEngine(retryMethod, mobileRawReqHeaders,
                    httpEngine.getConnection(), (RetryableOutputStream) requestBody);
	    }*/
	    //else {
              //  httpEngine = newHttpEngine(retryMethod, rawRequestHeaders,
                //    httpEngine.getConnection(), (RetryableOutputStream) requestBody);
    	  //  }
	//}
    
	}

    HttpEngine getHttpEngine() {
        return httpEngine;
    }

    enum Retry {
        NONE,
        SAME_CONNECTION,
        DIFFERENT_CONNECTION
    }

    /**
     * Returns the retry action to take for the current response headers. The
     * headers, proxy and target URL or this connection may be adjusted to
     * prepare for a follow up request.
     */
    private Retry processResponseHeaders() throws IOException {
        switch (getResponseCode()) {
        case HTTP_PROXY_AUTH:
            if (!usingProxy()) {
                throw new IOException(
                        "Received HTTP_PROXY_AUTH (407) code while not using proxy");
            }
            // fall-through
        case HTTP_UNAUTHORIZED:
            boolean credentialsFound = processAuthHeader(getResponseCode(),
                    httpEngine.getResponseHeaders(), rawRequestHeaders);
            return credentialsFound ? Retry.SAME_CONNECTION : Retry.NONE;

        case HTTP_MULT_CHOICE:
        case HTTP_MOVED_PERM:
        case HTTP_MOVED_TEMP:
        case HTTP_SEE_OTHER:
            if (!getInstanceFollowRedirects()) {
                return Retry.NONE;
            }
            if (++redirectionCount > HttpEngine.MAX_REDIRECTS) {
                throw new ProtocolException("Too many redirects");
            }
            String location = getHeaderField("Location");
            if (location == null) {
                return Retry.NONE;
            }
            URL previousUrl = url;
            url = new URL(previousUrl, location);
            if (!previousUrl.getProtocol().equals(url.getProtocol())) {
                return Retry.NONE; // the scheme changed; don't retry.
            }
            if (previousUrl.getHost().equals(url.getHost())
                    && previousUrl.getEffectivePort() == url.getEffectivePort()) {
                return Retry.SAME_CONNECTION;
            } else {
                return Retry.DIFFERENT_CONNECTION;
            }

        default:
            return Retry.NONE;
        }
    }

    /**
     * React to a failed authorization response by looking up new credentials.
     *
     * @return true if credentials have been added to successorRequestHeaders
     *     and another request should be attempted.
     */
    final boolean processAuthHeader(int responseCode, ResponseHeaders response,
            RawHeaders successorRequestHeaders) throws IOException {
        if (responseCode != HTTP_PROXY_AUTH && responseCode != HTTP_UNAUTHORIZED) {
            throw new IllegalArgumentException("Bad response code: " + responseCode);
        }

        // keep asking for username/password until authorized
        String challengeHeader = responseCode == HTTP_PROXY_AUTH
                ? "Proxy-Authenticate"
                : "WWW-Authenticate";
        String credentials = getAuthorizationCredentials(response.getHeaders(), challengeHeader);
        if (credentials == null) {
            return false; // could not find credentials, end request cycle
        }

        // add authorization credentials, bypassing the already-connected check
        String fieldName = responseCode == HTTP_PROXY_AUTH
                ? "Proxy-Authorization"
                : "Authorization";
        successorRequestHeaders.set(fieldName, credentials);
        return true;
    }

    /**
     * Returns the authorization credentials on the base of provided challenge.
     */
    private String getAuthorizationCredentials(RawHeaders responseHeaders, String challengeHeader)
            throws IOException {
        List<Challenge> challenges = HeaderParser.parseChallenges(responseHeaders, challengeHeader);
        if (challenges.isEmpty()) {
            throw new IOException("No authentication challenges found");
        }

        for (Challenge challenge : challenges) {
            // use the global authenticator to get the password
            PasswordAuthentication auth = Authenticator.requestPasswordAuthentication(
                    getConnectToInetAddress(), getConnectToPort(), url.getProtocol(),
                    challenge.realm, challenge.scheme);
            if (auth == null) {
                continue;
            }

            // base64 encode the username and password
            String usernameAndPassword = auth.getUserName() + ":" + new String(auth.getPassword());
            byte[] bytes = usernameAndPassword.getBytes(Charsets.ISO_8859_1);
            String encoded = Base64.encode(bytes);
            return challenge.scheme + " " + encoded;
        }

        return null;
    }

    private InetAddress getConnectToInetAddress() throws IOException {
        return usingProxy()
                ? ((InetSocketAddress) proxy.address()).getAddress()
                : InetAddress.getByName(getURL().getHost());
    }

    final int getDefaultPort() {
        return defaultPort;
    }

    /** @see HttpURLConnection#setFixedLengthStreamingMode(int) */
    final int getFixedContentLength() {
        return fixedContentLength;
    }

    /** @see HttpURLConnection#setChunkedStreamingMode(int) */
    final int getChunkLength() {
        return chunkLength;
    }

    final Proxy getProxy() {
        return proxy;
    }

    final void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    @Override public final boolean usingProxy() {
        return (proxy != null && proxy.type() != Proxy.Type.DIRECT);
    }

    @Override public String getResponseMessage() throws IOException {
        return getResponse().getResponseHeaders().getHeaders().getResponseMessage();
    }

    @Override public final int getResponseCode() throws IOException {
        return getResponse().getResponseCode();
    }

    @Override public final void setRequestProperty(String field, String newValue) {
        if (connected) {
            throw new IllegalStateException("Cannot set request property after connection is made");
        }
        if (field == null) {
            throw new NullPointerException("field == null");
        }
        rawRequestHeaders.set(field, newValue);
    }

    @Override public final void addRequestProperty(String field, String value) {
        if (connected) {
            throw new IllegalStateException("Cannot add request property after connection is made");
        }
        if (field == null) {
            throw new NullPointerException("field == null");
        }
        rawRequestHeaders.add(field, value);
    }
}
