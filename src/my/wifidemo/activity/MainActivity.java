package my.wifidemo.activity;

import java.util.ArrayList;

import my.wifidemo.R;
import my.wifidemo.manager.ApClientManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

	private ApClientManager apClientManager = null;
	private String ipString = "";
	private ArrayList<String> ipStringSet;
	private final static String adhocIp = "192.168.10.10";
	
	private EditText portText=null;
	private String port="10086";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		portText=(EditText)findViewById(R.id.editTextPort);
		portText.setText(port);
		
		ipStringSet=new ArrayList<String>();
		apClientManager = new ApClientManager(this);

	}

	public void onAdhocClick(View view) {

		port=portText.getText().toString().trim();
		StartDisplayActivity(adhocIp,port);
		// 若选择adhoc模式则直接将固定IP设置并且启动监控页面
	}

	public void onInfraClick(View view) {
		
		port=portText.getText().toString().trim();

		if (apClientManager.isApEnadbled()) {
			apClientManager.initConnectedIPs();
			if (!apClientManager.isEmpty()) {
				//ipString = apClientManager.getConnectedIp(0);
				
					ipStringSet=apClientManager.getConnectedIpList();
				//StartDisplayActivity(ipString,port);

				CharSequence[] cSequence=(CharSequence[])ipStringSet.toArray(
						new CharSequence[ipStringSet.size()]);
				//将LIST转化为CHATSEQ对象再选择IP地址
				AlertDialog.Builder builder=new AlertDialog.Builder(this);
				builder.setTitle("请选需要连接的设备");
				builder.setItems(cSequence,new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface arg0, int pos) {
						// TODO Auto-generated method stub
						ipString=ipStringSet.get(pos);
						StartDisplayActivity(ipString,port);

					}
				});
				builder.show();

				System.out.println(ipStringSet.size());
				System.out.println(cSequence.length);
				// 若WIFI热点已经打开并且已经成功连接，则启动视频监控界面
			} else {
				Toast.makeText(getApplicationContext(), "尚未有设备连接上本机WIFI热点",
						Toast.LENGTH_SHORT).show();
				// WIFI热点打开，没有设备连接则开启提示
			}
		} else {

			Intent intent = new Intent("/");
			ComponentName cm = new ComponentName("com.android.settings",
					"com.android.settings.TetherSettings");
			intent.setComponent(cm);
			intent.setAction("android.intent.action.VIEW");
			this.startActivityForResult(intent, 0);
			// WIFI热点尚未打开则打开WIFI热点页面

		}
	}

	/**
	 * 启动视频监控页面并且发送IP地址
	 * 
	 * @param ip String:要发送的IP地址
	 * @param port String:程序的端口号           
	 * */
	private void StartDisplayActivity(String ip,String port) {
		ControlActivity.startControlActivity(getApplicationContext(), ip, port);
		finish();
	}

}
