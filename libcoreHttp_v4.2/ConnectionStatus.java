package libcore.net.http;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class ConnectionStatus extends Thread {
	
	private boolean useBoth;
	public static boolean WIFI;
	public static boolean MOBILE;
		
	//public static Object lock;
	
	public static Object o_wifi;
	public static Object o_mobile;
	
	private HttpHelper helper;
	/*
	static {
		System.out.println("622 - ConnectionStatus: Loading android_runtime library");
		System.loadLibrary("android_runtime");
	}

	static native String native_get(String key);
	*/

	public ConnectionStatus(HttpHelper h) {
		helper = h;
		useBoth = false;
		ConnectionStatus.WIFI = false;
		ConnectionStatus.MOBILE = false;
		//lock = new Object();
		o_wifi = new Object();
		o_mobile = new Object();
		start();
	}

	public void run() {
		System.out.println("622 - ConnectionStatus Thread now running...");
		
		DataInputStream in = null;
		DataOutputStream out = null;
		BufferedReader inFromServer = null;
		try {
			Socket socket = new Socket("127.0.0.1", 1234);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			//String toSend = "wifi";
			//out.writeBytes(toSend + '\n');
			inFromServer = new BufferedReader(new InputStreamReader(in));
			//String fromServer = inFromServer.readLine();
			//System.out.println("622 - ConnectionStatus: Response from server = " + fromServer);
		} catch(Exception e) {
			System.out.println("622 - ConnectionStatus Thread - Unable to open socket to server");
		}
		String toSend;
		boolean runOnce = true;
		while(true) {
			synchronized(helper.lock) {
				if(helper.isComplete) {
					System.out.println("622 - ConnectionStatus Thread - exiting...");
					notifyWorkers(true);
					return;
				}
			}
			//synchronized(ConnectionStatus.lock){
			try{
			toSend = "wifi";
			out.writeBytes(toSend + '\n');
			String isWifiUp = inFromServer.readLine();//System.getProperty("wifi.up");//native_get("wifi.up");//getProp.invoke(null, "wifi.up");//SystemProperties.get("wifi.up");			
			System.out.println("622 - ConnectionStatus Thread: Wifi = " + isWifiUp);
			synchronized(ConnectionStatus.o_wifi) {
			if(isWifiUp != null && isWifiUp.equals("true"))
				ConnectionStatus.WIFI = true;
			else
				ConnectionStatus.WIFI = false;
			}
			toSend = "mobile";
			out.writeBytes(toSend + '\n');
			String isMobileUp = inFromServer.readLine();//System.getProperty("mobile.up");//native_get("mobile.up");//getProp.invoke(null, "mobile.up");//SystemProperties.get("mobile.up");
			System.out.println("622 - ConnectionStatus Thread: Mobile = " + isMobileUp);
			synchronized(ConnectionStatus.o_mobile) {
			if(isMobileUp != null && isMobileUp.equals("true"))
				ConnectionStatus.MOBILE = true;
			else
				ConnectionStatus.MOBILE = false;
			}
			toSend = "both";
			out.writeBytes(toSend + '\n');
			String s_canUseBoth = inFromServer.readLine();//System.getProperty("useboth");//native_get("useboth");//getProp.invoke(null, "useboth");//SystemProperties.get("useboth");
			System.out.println("622 - ConnectionStatus Thread: UseBoth = " + s_canUseBoth);
			if(s_canUseBoth != null && s_canUseBoth.equals("true"))
			{
				update(true);
				//ConnectionStatus.MOBILE = true;
				//ConnectionStatus.WIFI = true;
			}
			else
				update(false);
			
			}catch(Exception e) {
				System.out.println("622 - ConnectionStatus Thread: Exception sending data");
				e.printStackTrace();
			}
			
			
			try {
				notifyWorkers(false);	
				Thread.sleep(1000);
			} catch(InterruptedException e) {
				System.out.println("622 - ConnectionStatus: Interrupted Exception");
				e.printStackTrace();
			}
		}
	}

	private void notifyWorkers(boolean isComplete) {
		synchronized(ConnectionStatus.o_wifi) {
        	if(ConnectionStatus.WIFI || isComplete)
            {   
            	//synchronized(ConnectionStatus.o_wifi) {
                	ConnectionStatus.o_wifi.notifyAll();
                //}   
            }
		}
        synchronized(ConnectionStatus.o_mobile) {    
			if(ConnectionStatus.MOBILE || isComplete)
            {   
            	//synchronized(ConnectionStatus.o_mobile) {
                	ConnectionStatus.o_mobile.notifyAll();
                //}   
            }   
        }   
	}
	
	private void update(boolean val) {
		useBoth = val;
	}
	
	private boolean canUseBoth() {
		return useBoth;
	}

}
