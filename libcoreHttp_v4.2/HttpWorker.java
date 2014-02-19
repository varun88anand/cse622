package libcore.net.http;

import java.io.InputStream;
import libcore.io.IoUtils;
import libcore.util.Objects;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

public class HttpWorker extends Thread {
	
	//private HttpEngine engine;
	//private RawHeaders header;
	private HttpURLConnectionImpl impl;
	private HttpHelper helper;
	private String type;
	private String method = HttpEngine.POST;

	int redirectionCount;
	int marker;
	int chunkSize;

	public HttpWorker(HttpURLConnectionImpl i, HttpHelper hp, String t) {
		helper = hp;
		//engine = e;
		//header = h;
		type = t;
		impl = i;
		redirectionCount = 0;
		start();
	}
	
	private void done() {
		synchronized(helper.lock) {
			helper.isComplete = true;
		}
	}

	public void run() {
		chunkSize = helper.getChunkSize();
		int flag = 0;
		int start=0, end=0;
		boolean redirection = false;
		boolean runOnce = true;
		boolean runOnceMore = true;
		byte data[] = new byte[chunkSize];
		

		while(true) {
		if(flag == 1)
			break;
		
		try{
		
		if(type.equals("wifi")) {
			synchronized(ConnectionStatus.o_wifi) {
			if(!ConnectionStatus.WIFI)
			{
				System.out.println("622 - Worker: <" + type + ">... Waiting for WIFI to be AVAILABLE");
				ConnectionStatus.o_wifi.wait();
			}
			}
		}
		else { 
			synchronized(ConnectionStatus.o_mobile) {
			if(!ConnectionStatus.MOBILE)
			{
				System.out.println("622 - Worker: <" + type + ">... Waiting for MOBILE to be AVAILABLE");
				ConnectionStatus.o_mobile.wait();
			}
			}
		}
	
		if(!redirection) {
		synchronized(helper.lock) {
			if(helper.isComplete) {
		    	System.out.println("622 - Worker: <" + type + ">... EXITING");
				return;
			} 
		}
			int missingChunkMarker = helper.getNextMissingChunk();
			if(missingChunkMarker == -1) {
				marker = helper.getMarker();
				int newMarker = marker + chunkSize;
				helper.setMarker(newMarker);
			} else {
				marker = missingChunkMarker;
			}
		}
		start = marker;
		end = start + chunkSize - 1;
		
			RawHeaders tmpHeader = new RawHeaders();
			tmpHeader.set("Range", "bytes=" + start + "-" + end);
			HttpEngine engine = null;
			//if(!redirection)
				engine = new HttpEngine(impl, method, tmpHeader, null, null);
			//else /* 302 - Client is not allowed to change the request method, however most of the user-agents modify it to GET*/
			//	engine = new HttpEngine(impl, method/*HttpEngine.GET*/, tmpHeader, null, null);
			//engine.updateHeader(tmpHeader);
			System.out.println("622 - Worker: <" + type + ">...sending to " + impl.mUrl.toString() + " ........bytes=" + start + " - " + end);
			
			synchronized(helper.lock) {
				if(type.equals("wifi"))
					HttpHelper.INSTANCE.connectionFor("wifi");
				else
					HttpHelper.INSTANCE.connectionFor("mobile");
				engine.sendRequest();
				//impl.httpEngine = engine;
			}
			//if(type.equals("wifi"))
			//	helper.wifiEngine = engine;
			//else
			//	helper.mobileEngine = engine;
			
			engine.readResponse();
			int responseCode = engine.getResponseCode();
			System.out.println("622 - Worker: <" + type + ">...response code for bytes = " + start + " - " + end + " == " + responseCode);
			
			if(responseCode == 504) {
				System.out.println("622 - Worker: <" + type + ">...Response Code = 504, Gateway timed out...Retry...");
				helper.insertToMissingList(start);
			}
			else if (responseCode >= 400/*HTTP_BAD_REQUEST*/) {
				flag = 1;
				done();
				System.out.println("622 - BAD Request for Worker: <" + type + ">...sending" + " bytes=" + start + " - " + end);
				helper.bArrInpStream.write(start, null, -1, 0, 0, 0);
				//int wifiBytes = helper.bArrInpStream.wifi_bytes();
				//int mobileBytes = helper.bArrInpStream.mobile_bytes();
				//int total = wifiBytes + mobileBytes;
				//System.out.println("622 - STATISTICS: WIFI = " + wifiBytes + "/" + total + " and MOBILE = " + mobileBytes + "/" + total);
			}
			else if(responseCode == HttpURLConnection.HTTP_MULT_CHOICE || responseCode == HttpURLConnection.HTTP_MOVED_PERM
						|| responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_SEE_OTHER) 			 
			{
						
				if (++redirectionCount > HttpEngine.MAX_REDIRECTS) {
					System.out.println("622 - Worker: <" + type + ">... Too Many Redirects...exiting");
					done();
					helper.isGarbled = true;
					return;
					
				} 

				String location = "";
				RawHeaders rawHeaders = engine.getResponseHeaders().getHeaders();
				location = rawHeaders.get("Location");
				System.out.println("622 - Worker: <" + type + ">...redirecting to = " + location + " ...for bytes=" + start + " - " + end);
				try {
					impl.setUrl(location);
				} catch(IOException e) {
					done();
					helper.isGarbled = true;
					System.out.println("622 - Worker: <" + type + ">...Error changing to new URL...exiting");
					e.printStackTrace();
					return;
				}
				redirection = true;
				//engine.release(true);
			}
			else/*Successful*/ {

				InputStream result = engine.getResponseBody();
				redirectionCount = 0;
				redirection = false;
				/* Fix for CaptivePortal tracker - 204 */
				/* Fix for HTTP_OK - 200. Some servers like wikipedia return 200 instead of 206 i.e. these servers does not serve byte-range requests */
				if(responseCode == HttpURLConnection.HTTP_NO_CONTENT /*204*/ || responseCode == HttpURLConnection.HTTP_OK/*200*/)
				{
					done();
					helper.supportByteRequest = false;
					impl.httpEngine = engine;
					System.out.println("622 - Worker: <" + type + ">... No Content <204> OR 200 ok ... exiting");
					/* There should be just one inputstream and that's it! */
					helper.setInputStream(result);
					synchronized(HttpHelper.joinLock) {
						HttpHelper.joinLock.notify();
					}
					return;
				}

                if(result != null) {
                    System.out.println("622 - Worker: <" + type + ">...Inserting to Buffer, KEY = " + start);
                    //helper.insertResultToMap(start, result); 
                	//int bytesRead = result.read(bucket);
					int nRead;
					int bytesRead = 0;
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					long startTime = System.currentTimeMillis();
					while ((nRead = result.read(data, 0, data.length)) != -1) {
  						buffer.write(data, 0, nRead);
						bytesRead += nRead;
					}
					long endTime = System.currentTimeMillis();
					buffer.flush();
					data = buffer.toByteArray();
					System.out.println("622 - Worker: <" + type + ">... KEY = " + start + " read bytes = " + bytesRead + "/" + chunkSize);
					int TYPE = (type.equals("wifi")) ? 1:2;
					//helper.insertStatistics(start, startTime, endTime, TYPE);
					helper.bArrInpStream.write(start, data, TYPE, bytesRead, startTime, endTime);
				}   
                else {
                    System.out.println("622 - Worker: <" + type + ">... NULL INPUTSTREAM FOR KEY = " + start);
                }   

			
				if(runOnce) {
					synchronized(HttpHelper.joinLock) {
						HttpHelper.joinLock.notify();
					}
					runOnce = false;
				}
			}
		} catch(InterruptedException e) {
			System.out.println("622 - EXCEPTION Worker: <" + type + "> ...Interrupted");	
		} 
		catch(Exception e) {
			System.out.println("622 - EXCEPTION Worker: <" + type + ">...adding chunk to missing list-->" + " bytes=" + start + " - " + end);
			helper.insertToMissingList(start);
			e.printStackTrace();
			
		}

		}

		System.out.println("622 - Http Worker: <" + type + "> ... exiting");
	}

}
