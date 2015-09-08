package my.wifidemo.manager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.R.integer;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import my.wifidemo.R;
import my.wifidemo.activity.*;

public class ApClientManager {

	private WifiManager wifiManager;
	private Context context;
	private ArrayList<String> connectdIPArrayList;
	private static final String REG = ""
			+ "\\b(([01]?\\d?\\d|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d?\\d|2[0-4]\\d"
			+ "|25[0-5])\\b";
	
	public enum WIFI_AP_STATE {  
        WIFI_AP_STATE_DISABLING, WIFI_AP_STATE_DISABLED, WIFI_AP_STATE_ENABLING,  WIFI_AP_STATE_ENABLED, WIFI_AP_STATE_FAILED  
    }  

	public ApClientManager(Context context) {
		super();
		this.context = context;
		connectdIPArrayList = new ArrayList<String>();
		wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		//initConnectedIPs();
	}

	public void initConnectedIPs() {
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(
					"/proc/net/arp"));
			// 从arp文件读取arp表信息获取已经连接的设备的ip地址

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				String[] splittedString = line.split(" +");
				// 以空格分割从文件里获取的信息
				if (splittedString != null && splittedString.length >= 4) {
					String ip = splittedString[0];
					if (ip.trim().matches(REG)) {
						
						if(!connectdIPArrayList.contains(ip)){
							
							connectdIPArrayList.add(ip);
						}
						// 如果正则表达式匹配则加入ip列表
					}
				}
			}
			bufferedReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	/** 
     * wifi热点开关 
     * @param enabled   true：打开  false：关闭 
     * @return  true：成功  false：失败 
     */  
	public boolean setWifiApEnabled(boolean enabled) {

		if (enabled) { // disable WiFi in any case
			// wifi和热点不能同时打开，所以打开热点的时候需要关闭wifi
			wifiManager.setWifiEnabled(false);
		}
		try {
			// 热点的配置类
			WifiConfiguration apConfig = new WifiConfiguration();
			// 配置热点的名称(可以在名字后面加点随机数什么的)
			apConfig.SSID = context.getResources().getString(
					R.string.androidAPSSID);
			// 配置热点的密码
			apConfig.preSharedKey = context.getResources().getString(
					R.string.androidAPpwd);

			apConfig.status = WifiConfiguration.Status.ENABLED;
			apConfig.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			apConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			apConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			apConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.TKIP);
			apConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.CCMP);
			apConfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.TKIP);
			apConfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.CCMP);
					
			// 通过反射调用设置热点
			Method method = wifiManager.getClass().getMethod(
					"setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
			// 返回热点打开状态
			return (Boolean) method.invoke(wifiManager, apConfig, enabled);
		} catch (Exception e) {
			return false;
		}
	}
	
	public boolean isApEnadbled(){
		return getWifiApState() == WIFI_AP_STATE.WIFI_AP_STATE_ENABLED;  
	}
	
	private WIFI_AP_STATE getWifiApState(){  
        int tmp;  
        try {  
            Method method = wifiManager.getClass().getMethod("getWifiApState");  
            tmp = ((Integer) method.invoke(wifiManager));  
            // Fix for Android 4  
            if (tmp > 10) {  
                tmp = tmp - 10;  
            }  
            return WIFI_AP_STATE.class.getEnumConstants()[tmp];  
        } catch (Exception e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
            return WIFI_AP_STATE.WIFI_AP_STATE_FAILED;  
        }  
    }  

	public String getConnectedIp(int index) {
		return connectdIPArrayList.get(index);
	}

	public ArrayList<String> getConnectedIpList() {
		return connectdIPArrayList;
	}

	public boolean isEmpty() {
		return connectdIPArrayList.isEmpty();
	}
}
