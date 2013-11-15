package com.android.server;

import android.util.Log;
import android.net.LinkProperties;
import android.net.ConnectivityManager;
import java.net.InetAddress;
import android.net.RouteInfo;
import java.util.Collection;

public class ConnectivityServiceHelper {
	
	private wifiInfo newWifi = new wifiInfo();
	private cdmaInfo newCdma = new cdmaInfo();

    private wifiInfo oldWifi = new wifiInfo();
    private cdmaInfo oldCdma = new cdmaInfo();
	
	public void addDefaultRoute(int type)
	{
		String su = "su 0 ";
		String cmd;
		if(type == ConnectivityManager.TYPE_WIFI)
		{
			Log.i("622", "Adding Default route through WIFI for main table");
			cmd = su + "ip route add default via " + newWifi.gateway;
		}
		else
		{
			Log.i("622", "Adding Default route through CDMA for main table");
			cmd = su + "ip route add default via " + newCdma.gateway;
		}
		this.commandShell(cmd);
	}

	public void removeRule(int type)
	{
		String su = "su 0 ";
		String cmd;
		if(type == ConnectivityManager.TYPE_WIFI)
		{
			if(!newWifi.toString().equals(""))
			{
				Log.i("622", "Remove IP RULE for WIFI");
				cmd = su + "ip rule del from " + newWifi.ipaddr + " table " + newWifi.table;
				this.commandShell(cmd);
				cmd = su + "ip rule del to " + newWifi.ipaddr + " table " + newWifi.table;
				this.commandShell(cmd);
				newWifi.ipaddr = newWifi.gateway = newWifi.link = "";
			}
		}
		else
		{
            if(!newCdma.toString().equals(""))
            {   
				Log.i("622", "Remove IP RULE for CDMA");
                cmd = su + "ip rule del from " + newCdma.ipaddr + " table " + newCdma.table;
                this.commandShell(cmd);
                cmd = su + "ip rule del to " + newCdma.ipaddr + " table " + newCdma.table;
                this.commandShell(cmd);
                newCdma.ipaddr = newCdma.gateway = newCdma.link = ""; 
            }   
		}
	}
	
	public void setupTable(int type)
	{
		String su = "su 0 ";
		String cmd;
		if(type == ConnectivityManager.TYPE_WIFI)
		{
			//this.commandShell("su 0 ip route list table WIFI");
			cmd = su + "ip route add " + newWifi.link + " dev " + newWifi.iface + " src " + newWifi.ipaddr + " table " + newWifi.table;
			this.commandShell(cmd);
			cmd = su + "ip route add default via " + newWifi.gateway + " table " + newWifi.table;
			this.commandShell(cmd);
			cmd = su + "ip rule add from " + newWifi.ipaddr + " table " + newWifi.table;
			this.commandShell(cmd);
			cmd = su + "ip rule add to " + newWifi.ipaddr + " table " + newWifi.table;
			this.commandShell(cmd);
		}
		else
		{
			//this.commandShell("su 0 ip route list table CDMA");
            cmd = su + "ip route add " + newCdma.link + " dev " + newCdma.iface + " src " + newCdma.ipaddr + " table " + newCdma.table;
            this.commandShell(cmd);
            cmd = su + "ip route add default via " + newCdma.gateway + " table " + newCdma.table;
            this.commandShell(cmd);
            cmd = su + "ip rule add from " + newCdma.ipaddr + " table " + newCdma.table;
            this.commandShell(cmd);
            cmd = su + "ip rule add to " + newCdma.ipaddr + " table " + newCdma.table;
			this.commandShell(cmd);
		}
	}
	
	public boolean areEqual(wifiInfo oldInfo, wifiInfo newInfo)
	{
		boolean result = false;
			if(oldInfo.toString().equals(newInfo.toString()))
				result = true;
		return result;
	}

    public boolean areEqual(cdmaInfo oldInfo, cdmaInfo newInfo)
    {   
        boolean result = false;
            if(oldInfo.toString().equals(newInfo.toString()))
                result = true;
        return result;
    }   


	
	public synchronized void commandShell(String cmd)
	{
		// Call to native function
		Log.i("622", "To execute = " + cmd);
		VibratorService.myexecuteCommand(cmd);
	}
	
	public String getLink(String ip)
	{
		int i=0;
		int j=0;
		for(char c : ip.toCharArray()) {
			if(c=='.')
				i++;
			if(i==3)
				break;
			j++;
		}
		return ip.substring(0, j) + ".0";	
	}

	public void setInfo(LinkProperties lp, int type)
	{
		Log.i("622", "Inside setInfo function");
		Collection<InetAddress> ip = lp.getAddresses();
		Collection<RouteInfo> route = lp.getRoutes();
		/*Assumption - There can be only one IP address and associated 
		  @TODO - Handle case for multiple addresses
		*/
		if(ip.size() != 1)
		{
			Log.i("622", "Error - Unhandled case, no. of associated IP = " + ip.size() + " for interface = " + lp.getInterfaceName());
			return;
		}
		/*Assumption - There is just one route i.e. via gateway to destination
		  @TODO - Handle cases for multiple routes
		*/
		if(route.size() != 1)
		{
			Log.i("622", "Error - Unhandled case, no. of associated routes = " + route.size() + " for interface = " + lp.getInterfaceName());
			return;
		}
		
		Log.i("622", "Checkpoint - 1");
		this.oldWifi.ipaddr = this.newWifi.ipaddr;
		this.oldWifi.gateway = this.newWifi.gateway;
		this.oldWifi.link = this.newWifi.link;
		
		this.oldCdma.ipaddr = this.newCdma.ipaddr;
		this.oldCdma.gateway = this.newCdma.gateway;
		this.oldCdma.link = this.newCdma.link;

		String IP = "";
		String gateway = "";
		for(InetAddress i : ip)
			IP = i.getHostAddress();

		for(RouteInfo r : route)
			gateway = r.getGateway().getHostAddress();
		
		String link = getLink(IP);

		Log.i("622", "Helper: setInfo: IP = " + IP + ", gateway = " + gateway + ", link = " + link);
		
		if(type == ConnectivityManager.TYPE_WIFI)
		{
			this.newWifi.ipaddr = IP;
			this.newWifi.gateway = gateway;
			this.newWifi.link = link;
			if(!areEqual(this.oldWifi, this.newWifi))
			{
				Log.i("622", "Execute commands for setting up WIFI table");		
				setupTable(type);
			}
			else
				Log.i("622", "No change in WIFI configuration");
		}
		else if(type == ConnectivityManager.TYPE_MOBILE)
		{
			this.newCdma.ipaddr = IP;
			this.newCdma.gateway = gateway;
			this.newCdma.link = link;
			if(!areEqual(this.oldCdma, this.newCdma))
			{
				Log.i("622", "Execute commands for setting up CDMA table");
				setupTable(type);
			}
			else
				Log.i("622", "No change in CDMA configuration");
		}
		
	}

}

final class wifiInfo {
	String ipaddr;
	String gateway;
	final String iface = "wlan0";
	final String table = "WIFI";
	String link;
	
	public wifiInfo() {
		ipaddr = gateway = link = "";
	}

	public String toString() {
		return ipaddr + gateway + link;
	}
}

final class cdmaInfo {
    String ipaddr;
    String gateway;
    final String iface = "cdma_rmnet4";
    final String table = "CDMA";
    String link;

	public cdmaInfo() {
		ipaddr = gateway = link = "";
	}

	public String toString() {
		return ipaddr + gateway + link;
	}
}

