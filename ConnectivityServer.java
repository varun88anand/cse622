package com.android.server;

import android.util.Log;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class ConnectivityServer extends Thread {

	public ConnectivityServer() {
		start();
	}

	public void run() {
	Log.i("622", "ConnectivityServer Thread started");
	try {
		ServerSocket socket = new ServerSocket(1234);
		while(true) {
			new ThreadWorker(socket.accept());
		}
	} catch(Exception e) {
		Log.i("CSE622", "Exception in ConnectivityServer thread");
		e.printStackTrace();
	}	

	}

}

class ThreadWorker extends Thread {
	
	private Socket s;

	public ThreadWorker(Socket s) {
		this.s = s;
		start();
	}

	public void run() {
		Log.i("622", "ThreadWorker: New Connection received");
		try {
		DataInputStream in = new DataInputStream(s.getInputStream());
		DataOutputStream out = new DataOutputStream(s.getOutputStream());
		BufferedReader inFromClient = new BufferedReader(new InputStreamReader(in));
		String clientInput;
		while(!(clientInput = inFromClient.readLine()).equals("exit")) {
			//Log.i("CSE622", "Input From CLient = " + clientInput);			
			String toSend = "";
			if(clientInput.equals("wifi"))
			{
				if(ConnectivityService.getState(ConnectivityService.info_wifi))
					toSend = "true";
				else
					toSend = "false";
			}
			else if(clientInput.equals("mobile"))
			{
				if(ConnectivityService.getState(ConnectivityService.info_3g))
					toSend = "true";
				else
					toSend = "false";
			}
			else if(clientInput.equals("both"))
			{
				if(ConnectivityService.getState(ConnectivityService.info_3g) && ConnectivityService.getState(ConnectivityService.info_wifi))
					toSend = "true";
				else
					toSend = "false";
			}

			out.writeBytes(toSend + '\n');
		}	
		} catch(Exception e) {
			Log.i("CSE622", "Exception in ThreadWorker!!");
			e.printStackTrace();
		}
	}
}
