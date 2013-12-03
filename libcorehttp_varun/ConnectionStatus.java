package libcore.net.http;

//import android.os.Bundle;

public class ConnectionStatus extends Thread {
	
	private boolean useBoth;
	public static boolean WIFI;
	public static boolean MOBILE;
		
	public static Object lock;
	
	public static Object o_wifi;
	public static Object o_mobile;
	
	/*
	static {
		System.out.println("622 - ConnectionStatus: Loading android_runtime library");
		System.loadLibrary("android_runtime");
	}

	static native String native_get(String key);
	*/

	public ConnectionStatus() {
		useBoth = false;
		ConnectionStatus.WIFI = false;
		ConnectionStatus.MOBILE = false;
		lock = new Object();
		o_wifi = new Object();
		o_mobile = new Object();
		start();
	}

	public void run() {
		System.out.println("622 - ConnectionStatus Thread now running...");
		
		//Class sysPropClass = Class.forName("android.os.SystemProperties");
		//Method getProp = sysPropClass.getMethod("get", String.class);
		//getProp.setAccessible(true);
		
		
		while(true) {
			synchronized(ConnectionStatus.lock){
			String isWifiUp = System.getProperty("wifi.up");//native_get("wifi.up");//getProp.invoke(null, "wifi.up");//SystemProperties.get("wifi.up");			
			System.out.println("622 - ConnectionStatus Thread: Wifi = " + isWifiUp);
			
			if(isWifiUp != null && isWifiUp.equals("1"))
				ConnectionStatus.WIFI = true;
			else
				ConnectionStatus.WIFI = false;
				
			String isMobileUp = System.getProperty("mobile.up");//native_get("mobile.up");//getProp.invoke(null, "mobile.up");//SystemProperties.get("mobile.up");
			System.out.println("622 - ConnectionStatus Thread: Mobile = " + isMobileUp);
			if(isMobileUp != null && isMobileUp.equals("1"))
				ConnectionStatus.MOBILE = true;
			else
				ConnectionStatus.MOBILE = false;
			
			String s_canUseBoth = System.getProperty("useboth");//native_get("useboth");//getProp.invoke(null, "useboth");//SystemProperties.get("useboth");
			System.out.println("622 - ConnectionStatus Thread: UseBoth = " + s_canUseBoth);
			if(s_canUseBoth != null && s_canUseBoth.equals("1"))
				update(true);
			else
				update(false);
			}
			synchronized(HttpHelper.INSTANCE.lock) {
				if(canUseBoth()) {
					//update(true);
					HttpHelper.INSTANCE.useBoth = true;
					System.out.println("622 - ConnectionStatus: Both Interfaces are available");
				}
				else {
					//update(false);
					HttpHelper.INSTANCE.useBoth = false;
				}
			}	
			
			try {
				synchronized(ConnectionStatus.lock) {
					if(ConnectionStatus.WIFI)
					{
						synchronized(ConnectionStatus.o_wifi) {
							ConnectionStatus.o_wifi.notifyAll();
						}
					}
					if(ConnectionStatus.MOBILE)
					{
						synchronized(ConnectionStatus.o_mobile) {
							ConnectionStatus.o_mobile.notifyAll();
						}
					}
				}
			
			
				Thread.sleep(1000);
			} catch(InterruptedException e) {
				System.out.println("622 - ConnectionStatus: Interrupted Exception");
				e.printStackTrace();
			}
		}
	}

	public void update(boolean val) {
		useBoth = val;
	}
	
	public boolean canUseBoth() {
		return useBoth;
	}

}
