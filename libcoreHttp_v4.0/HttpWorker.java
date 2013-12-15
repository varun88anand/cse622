package libcore.net.http;

import java.io.InputStream;
import libcore.io.IoUtils;
import libcore.util.Objects;

public class HttpWorker extends Thread {
	
	//private HttpEngine engine;
	//private RawHeaders header;
	private HttpURLConnectionImpl impl;
	private HttpHelper helper;
	private String type;
	private String method = HttpEngine.POST;


	int marker;
	int chunkSize;

	public HttpWorker(HttpURLConnectionImpl i, HttpHelper hp, String t) {
		helper = hp;
		//engine = e;
		//header = h;
		type = t;
		impl = i;
		start();
	}

	public void run() {
		chunkSize = helper.getChunkSize();
		int flag = 0;
		int start=0, end=0;
		while(true) {
		if(flag == 1)
			break;
		
		try{
		
		if(type.equals("wifi")) {
			synchronized(ConnectionStatus.o_wifi) {
			if(!ConnectionStatus.WIFI)
			{
				System.out.println("622 - Worker: <" + type + ">... Waiting for WIFI to be AVAILABLE");
				//synchronized(ConnectionStatus.o_wifi) {
					ConnectionStatus.o_wifi.wait();
				//}
			}
			}
		}
		else { 
			synchronized(ConnectionStatus.o_mobile) {
			if(!ConnectionStatus.MOBILE)
			{
				System.out.println("622 - Worker: <" + type + ">... Waiting for MOBILE to be AVAILABLE");
				//synchronized(ConnectionStatus.o_mobile) {
					ConnectionStatus.o_mobile.wait();
				//}
			}
			}
		}
		
		synchronized(helper.lock) {
			if(helper.isComplete) {
		    	System.out.println("622 - Worker: <" + type + ">... EXITING");
				return;
			} 
			marker = helper.getMarker();
			int newMarker = marker + chunkSize;
			helper.setMarker(newMarker);
		}
		
		start = marker;
		end = start + chunkSize - 1;
		
			RawHeaders tmpHeader = new RawHeaders();
			tmpHeader.set("Range", "bytes=" + start + "-" + end);
			HttpEngine engine = new HttpEngine(impl, method, tmpHeader, null, null);
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
			if (responseCode >= 400/*HTTP_BAD_REQUEST*/) {
				flag = 1;
				synchronized(helper.lock) {
					
					helper.isComplete = true;
					//System.out.println("622 - Worker: <" + type + ">...reached the End of Stream... notifying parent thread");
					//helper.lock.notify();
				}
				System.out.println("622 - BAD Request for Worker: <" + type + ">...sending" + " bytes=" + start + " - " + end);
			}
			//InputStream result = engine.getResponseBody();
			if(/*result != null && */flag == 0) {
				//synchronized(helper.lock) {
				InputStream result = engine.getResponseBody();
				if(result != null) {
					System.out.println("622 - Worker: <" + type + ">...Inserting to map, KEY = " + start);
					helper.insertResultToMap(start, result); 
				}
				else {
					System.out.println("622 - Worker: <" + type + ">... NULL INPUTSTREAM FOR KEY = " + start);
				}
				//if(result.available() <= 0) {
				//	synchronized(helper.lock) {
				//		helper.isComplete = true;
				//	}
				//}
				//}
			}
			/*
			else
			{
				if(flag != 1)
					System.out.println("622 - Null InputStream returned for Worker: <" + type + ">... sending" + " bytes=" + start + " - " + end);
				flag = 1;
			}*/
		} catch(InterruptedException e) {
			System.out.println("622 - EXCEPTION Worker: <" + type + "> ...Interrupted");	
		} 
		catch(Exception e) {
			System.out.println("622 - EXCEPTION Worker: <" + type + ">...adding chunk to missing list-->" + " bytes=" + start + " - " + end);
			e.printStackTrace();
			//InputStream check = helper.getFromMap(start);
			//if(check == null) {
			
			helper.insertToMissingList(start);
			//}
			//flag = 1;
		}

		}

		System.out.println("622 - Http Worker: <" + type + "> ... exiting");
	}

}
