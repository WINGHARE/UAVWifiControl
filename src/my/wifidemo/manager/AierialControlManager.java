package my.wifidemo.manager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import my.wifidemo.protocol.ControlPacket;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class AierialControlManager {
	
	public static final String TAG = "AIERIAL_CONTROL_MANAGER";
	
	private int port;
	private int portLocal;
	private String ipstr;
	private Context mContext;
	private Handler myHandler;
	private static Boolean HeartBeatThreadEnable=true;
	private static Boolean ctrlInfoThreadEnable=true;
	private static ControlPacket mControlPacket=new ControlPacket();	
	private static DatagramSocket datagramSocket;

	
	public static final int REFRESH_VIEW = 0;
	public static final int DISPLY_DIALOG = 1;
	public static final int DISMISS_DIALOG = 2;
	public static final int RESET_BUTTON_STATUS=3;
	
	
	private HeartBeatThread heartBeatThread;
	private ControlInfoReceiveThread controlInfoReceiveThread;
	
	
	
	
	public AierialControlManager(int port, int portLocal, String ipstr,
			Context mContext, Handler myHandler) {
		super();
		this.port = port;
		this.portLocal = portLocal;
		this.ipstr = ipstr;
		this.mContext = mContext;
		this.myHandler = myHandler;
	}

	
	
	public boolean connectSocket(){
		
		try {
			datagramSocket=new DatagramSocket(portLocal);
		} catch (SocketException e) {
			e.printStackTrace();
			Log.e(TAG, "[UDPSOCKET]创建socket失败");
			return false;
		}
		
		return true;
	}
	
	public void connect(){
		heartBeatThread=new HeartBeatThread("HEART_BEAT",datagramSocket,mContext);
		heartBeatThread.start();
		controlInfoReceiveThread=new ControlInfoReceiveThread("CTRL_INFO",datagramSocket);
		controlInfoReceiveThread.start();
	}
	
	public void disconnect(){
		synchronized (HeartBeatThreadEnable) {
			HeartBeatThreadEnable=false;
		}
		synchronized (ctrlInfoThreadEnable) {
			ctrlInfoThreadEnable=false;
		}
	}
	
	public boolean socketIsConnected(){
		return datagramSocket.isConnected();
	}

	public void setControlMsg(ControlPacket controlPacket){
		mControlPacket=controlPacket;
	}

	/**
	 * 发送心跳包的线程，确认飞机和手机的连接状态
	 * */
	class HeartBeatThread extends HandlerThread {

		private DatagramSocket ds = null;
		private String udpMsg = "";
		private String ctrString = "";
		private ControlPacket controlPacket;
		private int count = 0;
		private boolean flags = true;
		private Context mContext;

		// public Handler mHandler;
		public HeartBeatThread(String name, DatagramSocket datagramSocket,
				Context context) {
			super(name);
			mContext = context;
			// TODO Auto-generated constructor stub
			ds = datagramSocket;
		}

		@Override
		public void run() {

			try {
				if (ds == null) {

					ds = new DatagramSocket(portLocal);
				}

				InetAddress serverAddr = InetAddress.getByName(ipstr);
				while (true && HeartBeatThreadEnable) {
					DatagramPacket dp;
					controlPacket = mControlPacket;
					byte[] command = controlPacket.getCommand();
					dp = new DatagramPacket(command, command.length,
							serverAddr, port);
					ds.send(dp);
					/*
					 * Log.i(TAG,udpMsg); Log.i(TAG,ctrString);
					 */

					/*
					 * synchronized (mControlPacket) {
					 * 
					 * if (mControlPacket==null) { mControlPacket=new
					 * ControlPacket(); }
					 * mControlPacket.setHeader(ControlPacket.HEADER_OUT);
					 * mControlPacket.setType(ControlPacket.TYPE_CONTROL);
					 * mControlPacket.setBody(0, 0, 0, 0); }
					 */
					sleep(125);
				}
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
					// ds.close();
				}
			}
			// Looper.loop();

		}

	}


	/**
	 * 接收来自飞机的飞行信息的线程
	 * */
	
	class ControlInfoReceiveThread extends HandlerThread {

		DatagramSocket datagramSocket = null;

		public ControlInfoReceiveThread(String name, DatagramSocket ds) {
			super(name);
			// TODO Auto-generated constructor stub
			datagramSocket = ds;

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

			try {

				sleep(500);

				if (datagramSocket == null) {

					datagramSocket = new DatagramSocket(portLocal);

				}
				byte[] controlDataByte = new byte[13];
				datagramSocket.setSoTimeout(200);
				datagramSocket.setBroadcast(true);
				DatagramPacket datagramPacket = new DatagramPacket(
						controlDataByte, controlDataByte.length);
				InetAddress address = InetAddress.getByName(ipstr);
				datagramSocket.connect(address, port);

				Log.i(TAG, "" + datagramSocket.getLocalPort() + " "
						+ datagramSocket.getLocalSocketAddress());

				Log.i(TAG,
						"[UDPSOCKET]is connected "
								+ datagramSocket.getRemoteSocketAddress()
								+ datagramSocket.isConnected());
				while (ctrlInfoThreadEnable == true
						&& datagramSocket.isConnected()) {
					try {
						datagramSocket.receive(datagramPacket);

						String dataString = new String(
								datagramPacket.getData(),
								datagramPacket.getOffset(),
								datagramPacket.getLength());

						ControlPacket controlPacket = new ControlPacket(
								datagramPacket.getData());
						String str = controlPacket.getPacketString();
						Log.d(TAG, "[UDPreceive]" + str);

					} catch (SocketTimeoutException e) {
						// Log.i(TAG, "[UDPSOCKET]timeout");
					} catch (IOException e) {
						// TODO: handle exception
						// Log.i(TAG, "[UDPreceive]"+e);
					} catch (Exception e) {
						// TODO: handle exception
					} finally {
						// multicastLock.release();

					}
				}

				if (datagramSocket != null)
					datagramSocket.close();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				// if(datagramSocket!=null)datagramSocket.close();
			}
			super.run();
		}

	}
}
