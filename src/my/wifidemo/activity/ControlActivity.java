package my.wifidemo.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import my.wifidemo.R;
import my.wifidemo.manager.AierialControlManager;
import my.wifidemo.manager.ImageReceiveManager;
import my.wifidemo.observer.ScreenObserver;
import my.wifidemo.observer.ScreenObserver.ScreenStateListener;
import my.wifidemo.protocol.ControlPacket;
import my.wifidemo.views.VerticalSeekBar;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.zerokol.views.JoystickView;
import com.zerokol.views.JoystickView.OnJoystickMoveListener;

/**
 * @author chjy 使用socket连接无人机的TCP服务器
 * 
 * 
 */

public class ControlActivity extends Activity implements OnClickListener {
	public static final String TAG = "RECEIVE_ACTIVITY";
	private int version;

	private Button btnLED1On = null;
	private Button btnLED1Off = null;
	private Button btnLED2On = null;
	private Button btnLED2Off = null;
	private Button btnLED3On = null;
	private Button btnLED3Off = null;
	private Button btnStartReceive = null;
	private Button btnOpenCamera = null;
	private Button btnCloseCamera = null;
	private Button btnCaptureScreen = null;
	private Button btnBack=null;
	private ImageView imageViewVideo = null;
	private ImageView imageViewMask=null;
	private TextView ipTextView = null;
	private TextView angleTextView = null;
	private TextView powerTextView = null;
	private TextView directionTextView = null;
	private TextView throttleTextView =null;
	private JoystickView joystickLeft = null;
	private VerticalSeekBar throttleSeekBar=null;
	
	private Dialog dialog = null;
	private ScreenObserver screenObserver=null;

	// private Socket socket = null;

	private MyHandler myHandler = new MyHandler();
	public static final int REFRESH_VIEW = 0;
	public static final int DISPLY_DIALOG = 1;
	public static final int DISMISS_DIALOG = 2;
	public static final int RESET_BUTTON_STATUS=3;

	// private Button btnLED4On = null;
	// private Button btnLED4Off = null;

	// private EditText field_name; // 接收用户名的组件
	// private String ipstr = "192.168.43.174";

	private static String ipstr = "";
	private static int UDP_SERVER_PORT = 10086;
	private static int UDP_SERVER_PORT_LOCAL=12306;
	
/*	private ImageReceiveThread imageReceiveThread = null;
*/	
	private ImageReceiveManager iManager;
	private AierialControlManager aManager;
	
/*	private HeartBeatThread heartBeatThread;
*/	private ChangeCtrlMsgThread changeCtrlMsgThread;
/*	private ControlInfoReceiveThread controlInfoReceiveThread;
*/	
	
	public static String controlMsg="0";
	public static ControlPacket mControlPacket=new ControlPacket();
	
	
	/**
	 * 方便其他页面可以启动该页面
	 * @param context Context当前上下文
	 * @param ip String 用于连接服务器的IP地址
	 * @param port String 用于连接应用的端口号
	 * */
	public static void startControlActivity(Context context,String ip,String port){
		Intent intent=new Intent(context,ControlActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("IP", ip);
		intent.putExtra("PORT", port);
		context.startActivity(intent);
	}

	@Override
	public void onBackPressed() {
		
		aManager.disconnect();
		aManager.closeSocket();
		backToPage();
	}
	
	
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		aManager.disconnect();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	/*	heartBeatThread=new HeartBeatThread("HEART_BEAT",datagramSocket,this);
		heartBeatThread.start();*/
		
		aManager.connect();
		changeCtrlMsgThread = new ChangeCtrlMsgThread("CHANGE_CTRL");
		changeCtrlMsgThread.start();
		
		/*controlInfoReceiveThread=new ControlInfoReceiveThread("CTRL_INFO",datagramSocket);
		controlInfoReceiveThread.start();
		*/
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		aManager.disconnect();
		super.onDestroy();
	}

	/** Called when the activity is first created. */
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 隐藏虚拟按键
		int version = android.os.Build.VERSION.SDK_INT;
		final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
				| View.KEEP_SCREEN_ON;
		// This work only for android 4.4+
		if (version >= Build.VERSION_CODES.KITKAT) {

			getWindow().getDecorView().setSystemUiVisibility(flags);

			// Code below is to handle presses of Volume up or Volume down.
			// Without this, after pressing volume buttons, the navigation bar
			// will
			// show up and won't hide
			final View decorView = getWindow().getDecorView();
			decorView
					.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {

						public void onSystemUiVisibilityChange(int visibility) {
							if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
								decorView.setSystemUiVisibility(flags);
							}
						}
					});
		}

		setContentView(R.layout.activity_control);
		
		btnLED1On = (Button) findViewById(R.id.ButtonLED1On);
		btnLED1Off = (Button) findViewById(R.id.ButtonLED1Off);
		btnLED2On = (Button) findViewById(R.id.ButtonLED2On);
		btnLED2Off = (Button) findViewById(R.id.ButtonLED2Off);
		btnLED3On = (Button) findViewById(R.id.ButtonLED3On);
		btnLED3Off = (Button) findViewById(R.id.ButtonLED3Off);
		btnStartReceive = (Button) findViewById(R.id.ButtonStartRecevie);
		btnOpenCamera = (Button) findViewById(R.id.ButtonOpenCamera);
		btnCloseCamera = (Button) findViewById(R.id.ButtonCloseCamera);
		btnCaptureScreen = (Button) findViewById(R.id.buttonCapturePic);
		btnBack=(Button)findViewById(R.id.buttonBack);
		imageViewVideo = (ImageView) findViewById(R.id.VideoImage);
		imageViewMask=(ImageView)findViewById(R.id.imageViewbg);
		ipTextView = (TextView) findViewById(R.id.textViewip);
		angleTextView = (TextView) findViewById(R.id.textViewAngle);
		powerTextView = (TextView) findViewById(R.id.textViewPower);
		directionTextView = (TextView) findViewById(R.id.textViewDirection);
		throttleTextView=(TextView)findViewById(R.id.textViewFlightHeight);
		joystickLeft = (JoystickView) findViewById(R.id.joystickLeft);
		throttleSeekBar=(VerticalSeekBar)findViewById(R.id.SeekBarHeight);

		btnLED1On.setOnClickListener(this);

		btnLED1Off.setOnClickListener(this);

		btnLED2On.setOnClickListener(this);

		btnLED2Off.setOnClickListener(this);

		btnLED3On.setOnClickListener(this);

		btnLED3Off.setOnClickListener(this);

		btnStartReceive.setOnClickListener(this);

		btnOpenCamera.setOnClickListener(this);

		btnCloseCamera.setOnClickListener(this);

		btnCaptureScreen.setOnClickListener(this);
		
		btnBack.setOnClickListener(this);

		joystickLeft.setOnJoystickMoveListener(joystickLeftListener, JoystickView.DEFAULT_LOOP_INTERVAL);
		
		screenObserver= new ScreenObserver(getApplicationContext());
		screenObserver.requestScreenStateUpdate(screenStateListener);
		//监听屏幕的锁屏状态
		
		Bundle bundle = getIntent().getExtras();
		ipstr = bundle.getString("IP");
		UDP_SERVER_PORT = Integer.parseInt(bundle.getString("PORT").trim());
		//获得IP地址和端口号
		
		ipTextView.setText(ipstr);
		

		iManager = new ImageReceiveManager(UDP_SERVER_PORT,
				UDP_SERVER_PORT_LOCAL, ipstr, this, myHandler);
		aManager = new AierialControlManager(UDP_SERVER_PORT,
				UDP_SERVER_PORT_LOCAL, ipstr, this, myHandler);
	/*	heartBeatThread=new HeartBeatThread("HEART_BEAT",datagramSocket,this);
		heartBeatThread.start();
		controlInfoReceiveThread=new ControlInfoReceiveThread("CTRL_INFO",datagramSocket);
		controlInfoReceiveThread.start();
		*/
		changeCtrlMsgThread = new ChangeCtrlMsgThread("CHANGE_CTRL");
		changeCtrlMsgThread.start();
		
				
		//开启心跳线程，确认飞机和遥控端的连接状态
		
		throttleSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
		//设置油门条的监听事件

	}

	/** 设置布局中按钮的点击事件 **/
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.ButtonLED1On:
			aManager.sendUDPCommand("LED_OPEN1");
			break;
		case R.id.ButtonLED1Off:
			aManager.sendUDPCommand(new byte[]{(byte)0xaa,(byte)0xaf,(byte)0x01,(byte)0x00,
					(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
					(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,}
					);
			break;
		case R.id.ButtonLED2On:
			aManager.sendUDPCommand("LED_OPEN2");
			
			
			imageViewVideo.setDrawingCacheEnabled(true);
			Bitmap detectBitmap = Bitmap.createBitmap(imageViewVideo
					.getDrawingCache());
			imageViewVideo.setDrawingCacheEnabled(false);

			break;
		case R.id.ButtonLED2Off:
			aManager.sendUDPCommand("TCP_EXCEPTION");
			break;
		case R.id.ButtonLED3On:
			aManager.sendUDPCommand("JDQ_OPEN");
			break;
		case R.id.ButtonLED3Off:
			aManager.sendUDPCommand("JDQ_CLOSE");
			break;
		case R.id.ButtonOpenCamera: {

			btnCloseCamera.setVisibility(View.VISIBLE);
			btnStartReceive.setVisibility(View.VISIBLE);
			btnOpenCamera.setVisibility(View.GONE);

			
			/*OpenCameraThread OpenCameraThread = new OpenCameraThread(
					"OPENCAMRERA_THREAD");
			OpenCameraThread.start();*/
			
			iManager.openCamera();
			Log.i(TAG, "[btnOpenCamera]Thread create");
			break;
		}
		case R.id.ButtonStartRecevie: {
			btnStartReceive.setVisibility(View.GONE);
			/*imageReceiveThread = new ImageReceiveThread("IMAGERECEIVE_THREAD");
			imageReceiveThread.start();*/
			iManager.startReceive();
			break;
		}
		case R.id.ButtonCloseCamera: {
			btnCloseCamera.setVisibility(View.GONE);
			btnOpenCamera.setVisibility(View.VISIBLE);
			btnStartReceive.setVisibility(View.GONE);
			/*CloseCameraThread thread = new CloseCameraThread(
					"CLOSECAMERA_THREAD");
			thread.start();*/
			iManager.closeCamera();
			break;
		}
		
		case R.id.buttonBack:{
			backToPage();
			break;
		}

		case R.id.buttonCapturePic: {
			try {
				String path = getSDPath();
				File dir=new File(path+"/Capture/");
				
				if(!dir.exists()){
					dir.mkdir();					
				}		    
				imageViewVideo.setDrawingCacheEnabled(true);
				Bitmap caputreBitmap = Bitmap.createBitmap(imageViewVideo
						.getDrawingCache());
				
				SimpleDateFormat formatter = new SimpleDateFormat(
						"yyyyMMdd_HHmmss");
				Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
				String fileName = formatter.format(curDate);
				
				File file = new File(path + "/Capture/", fileName+".jpg");
				boolean isFileCreated=file.createNewFile();
				
				if(isFileCreated){
					FileOutputStream out = new FileOutputStream(file);
					caputreBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
					out.flush();
					out.close();					
					Log.i(TAG, "已经保存图片");	
				}
				
				Animation animation=AnimationUtils.loadAnimation(this, R.anim.abc_fade_out);
				imageViewVideo.startAnimation(animation);
				imageViewVideo.setDrawingCacheEnabled(false);

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch (Exception e){
				e.printStackTrace();
				Log.e(TAG, e.toString());
			}
			break;
		}
		default:
			break;
		}
	}
	
	/**
	 * 设置左摇杆的移动事件
	 * */
	
	OnJoystickMoveListener joystickLeftListener= new OnJoystickMoveListener(){

		public void onValueChanged(int angle, int power, int direction) {
			// TODO Auto-generated method stub
			angleTextView.setText(" " + String.valueOf(angle) + "°");
			powerTextView.setText(" " + String.valueOf(power) + "%");
			//changeCtrlMsgThread.getHandler().sendEmptyMessage(1);
			synchronized (controlMsg) {
				controlMsg="1";
			}
			switch (direction) {
			case JoystickView.FRONT:
				directionTextView.setText("N");
				break;
			case JoystickView.FRONT_RIGHT:
				directionTextView.setText("NE");
				break;
			case JoystickView.RIGHT:
				directionTextView.setText("E");
				break;
			case JoystickView.RIGHT_BOTTOM:
				directionTextView.setText("SE");
				break;
			case JoystickView.BOTTOM:
				directionTextView.setText("S");
				break;
			case JoystickView.BOTTOM_LEFT:
				directionTextView.setText("NW");
				break;
			case JoystickView.LEFT:
				directionTextView.setText("W");
				break;
			case JoystickView.LEFT_FRONT:
				directionTextView.setText("S");
				break;
			default:
				directionTextView.setText("C");
			}
		}
	};
	
	
	/**
	 * 油门条的监听事件
	 * */
	
     OnSeekBarChangeListener seekBarChangeListener=new OnSeekBarChangeListener() {
		
		public void onStopTrackingTouch(SeekBar arg0) {
			// TODO Auto-generated method stub
			
		}
		
		public void onStartTrackingTouch(SeekBar arg0) {
			// TODO Auto-generated method stub
			
		}
		
		public void onProgressChanged(SeekBar arg0, int progress, boolean fromUser) {
			// TODO Auto-generated method stub
			ControlPacket controlPacket=new ControlPacket();
			controlPacket.setHeader(ControlPacket.HEADER_OUT);
			controlPacket.setType(ControlPacket.TYPE_CONTROL);
			
			int throttle=(progress*5==500)?(499):(progress*5);
			controlPacket.setBody(throttle,0,0,0);
			
			
			throttleTextView.setText(""+controlPacket.getThrottle());
			Log.d(TAG,""+controlPacket.getThrottle());
			
			
			controlPacket.calculateChecksum();
			
			
			synchronized (aManager) {
				aManager.setControlMsg(controlPacket);
				
			}
			aManager.sendUDPCommand(controlPacket.getCommand());
		//	Log.i(TAG, ""+progress);
		}
	};
	
	/**
	 * 监听屏幕的锁屏状态并且执行操作
	 * */
	ScreenStateListener screenStateListener=new ScreenStateListener() {
		
		public void onScreenOn() {
			// TODO Auto-generated method stub		
			
		}
		
		public void onScreenOff() {
			// TODO Auto-generated method stub
			iManager.closeCamera();	
			aManager.disconnect();
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			resetButtonStatus();

		}
	};
	

	/**
	 * 修改飞机控制信号的线程
	 * */
	class ChangeCtrlMsgThread extends HandlerThread{

		
		private Handler handler;
		public Handler getHandler() {
			return handler;
		}

		public ChangeCtrlMsgThread(String name) {
			super(name);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Looper.prepare();
			handler = new Handler(){

				@Override
				public void handleMessage(Message msg) {
					// TODO Auto-generated method stub
					super.handleMessage(msg);
					synchronized (controlMsg) {
						controlMsg="1";
					}
				}
				
			};
			Looper.loop();
			super.run();
		}
		
	}

	
	/**
	 * 主线程的handler*/
	class MyHandler extends Handler {
		public MyHandler() {

		}

		public MyHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == REFRESH_VIEW) {
				byte[] buffer = (byte[]) msg.obj;
				int bodyLength = msg.arg1;
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 1;
				options.inMutable=true;
				options.inPreferredConfig=Bitmap.Config.RGB_565;
				Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0,
						bodyLength, options);
				imageViewVideo.setImageBitmap(bitmap);
				
				// 将从线程中获取的数据展示在UI的imageview当中。
			} else if (msg.what == DISPLY_DIALOG) {
				String messageString = (String) msg.obj;
				dialog = new AlertDialog.Builder(ControlActivity.this).setMessage(
						messageString).create();
				dialog.show();
				// 在Dialog中显示消息
			} else if (msg.what == DISMISS_DIALOG) {
				if (dialog != null) {
					dialog.dismiss();
				}
				// 将DialogDismiss
			}else if (msg.what==RESET_BUTTON_STATUS){
				btnCloseCamera.setVisibility(View.GONE);
				btnOpenCamera.setVisibility(View.VISIBLE);
				btnStartReceive.setVisibility(View.GONE);	
				//重置按钮状态
			}
		}
	}
	/**
	 * 重置按钮布局
	 */
	private void resetButtonStatus() {
		Message message = new Message();
		message.what = RESET_BUTTON_STATUS;
		myHandler.sendMessage(message);
	}


	

	/**
	 * 使用UDP向开发板发送信号
	 * 
	 * @param command
	 *            String 需要发送的信号
	 * */
	private void sendUDPCommand(final String command,final DatagramSocket datagramSocket) {

		new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				String udpMsg = command;
				DatagramSocket ds = datagramSocket;
				try {
					if (ds == null) {
						ds = new DatagramSocket(UDP_SERVER_PORT_LOCAL);
					}
					InetAddress serverAddr = InetAddress.getByName(ipstr);
					DatagramPacket dp;
					dp = new DatagramPacket(udpMsg.getBytes(), udpMsg.length(),
							serverAddr, UDP_SERVER_PORT);
					ds.send(dp);
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (ds != null) {
					//	ds.close();
					}
				}
				super.run();
			}

		}.start();
		;
	}
	
	/**
	 * 使用UDP向开发板发送信号
	 * 
	 * @param data
	 *            byte[] 需要发送的字节码
	 * */
	private void sendUDPCommand(final byte[] data,final DatagramSocket datagramSocket) {

		new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
			//	String udpMsg = command;
				DatagramSocket ds = datagramSocket;
				try {
					if(ds==null){
						
						ds = new DatagramSocket(UDP_SERVER_PORT_LOCAL);
					}
					InetAddress serverAddr = InetAddress.getByName(ipstr);
					DatagramPacket dp;
					dp = new DatagramPacket(data, data.length,
							serverAddr, UDP_SERVER_PORT);
					ds.send(dp);
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (ds != null) {
					//	ds.close();
					}
				}
				super.run();
			}

		}.start();
		;
	}

/*	*//**
	 * 改变线程的标记位关闭相应的线程
	 * *//*
	private void closeControlThreads(){
		synchronized (imageRecenable) {
			imageRecenable = false;
		}
		synchronized (HeartBeatThreadEnable) {
			HeartBeatThreadEnable=false;
		}
		synchronized (ctrlInfoThreadEnable) {
			ctrlInfoThreadEnable=false;
		}
	}*/

	/** 获取SD卡路径 */
	public String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
		}
		return sdDir.toString();

	}
	
	/**
	 * 
	 * 返回主页
	 *  
	 * */
	private void backToPage(){
		/*CloseCameraThread cameraThread = new CloseCameraThread(
				"CLOSE_CAMERATHREAD");
		cameraThread.start();*/
		iManager.closeCamera();
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Intent intent = new Intent();
		intent.setClass(this, MainActivity.class);
		startActivity(intent);
		finish();
	}
	

	@SuppressLint("NewApi")
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (version >= Build.VERSION_CODES.KITKAT && hasFocus) {
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
							| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
							| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_FULLSCREEN
							| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

}