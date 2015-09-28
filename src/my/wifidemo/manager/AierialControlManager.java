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
	private static Boolean HeartBeatThreadEnable = true;
	private static Boolean ctrlInfoThreadEnable = true;
	private static ControlPacket mControlPacket = new ControlPacket();
	private DatagramSocket datagramSocket;

	public static final int REFRESH_VIEW = 0;
	public static final int DISPLY_DIALOG = 1;
	public static final int DISMISS_DIALOG = 2;
	public static final int RESET_BUTTON_STATUS = 3;

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

	public boolean connectSocket() {
		
		if (datagramSocket == null) {
			try {
				datagramSocket = new DatagramSocket(portLocal);
				datagramSocket.setReuseAddress(true);
			} catch (SocketException e) {
				e.printStackTrace();
				Log.e(TAG, "[UDPSOCKET]创建socket失败");
				return false;
			}
		} else if (!datagramSocket.isConnected() || datagramSocket.isClosed()) {

			InetAddress address;
			try {
				address = InetAddress.getByName(ipstr);
				datagramSocket.connect(address, port);
				return true;
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}

	public void closeSocket() {
		datagramSocket.close();
	}

	public void connect() {
		heartBeatThread = new HeartBeatThread("HEART_BEAT", datagramSocket,
				mContext);
		heartBeatThread.start();
		controlInfoReceiveThread = new ControlInfoReceiveThread("CTRL_INFO",
				datagramSocket);
		controlInfoReceiveThread.start();
	}

	public void disconnect() {
		synchronized (HeartBeatThreadEnable) {
			HeartBeatThreadEnable = false;
		}
		synchronized (ctrlInfoThreadEnable) {
			ctrlInfoThreadEnable = false;
		}
		
		synchronized (datagramSocket) {
			
			datagramSocket.close();
		}
	}

	public boolean socketIsConnected() {
		return datagramSocket.isConnected();
	}

	public void setControlMsg(ControlPacket controlPacket) {
		mControlPacket = controlPacket;
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
				} else if (ds.isClosed()) {
					InetAddress serverAddr = InetAddress.getByName(ipstr);
					ds.connect(serverAddr, port);
				}

				InetAddress serverAddr = InetAddress.getByName(ipstr);
				while (true && HeartBeatThreadEnable) {
					DatagramPacket dp;
					controlPacket = mControlPacket;
					byte[] command = controlPacket.getCommand();
					dp = new DatagramPacket(command, command.length,
							serverAddr, port);
					ds.send(dp);
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
					ds.disconnect();
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

				} else if (datagramSocket.isClosed()) {
					InetAddress serverAddr = InetAddress.getByName(ipstr);
					datagramSocket.connect(serverAddr, port);
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
						&& datagramSocket.isConnected()&&
						!datagramSocket.isClosed()) {
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
						e.printStackTrace();
					} catch (IOException e) {
						// TODO: handle exception
						// Log.i(TAG, "[UDPreceive]"+e);
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
						// TODO: handle exception
					} finally {
						// multicastLock.release();
						//datagramSocket.disconnect();
					}
				}

				synchronized (datagramSocket) {

					if (datagramSocket != null)
						datagramSocket.disconnect();
				}
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

	/**
	 * 使用UDP向开发板发送信号
	 * 
	 * @param command
	 *            String 需要发送的信号
	 * */
	public void sendUDPCommand(final String command) {

		new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				String udpMsg = command;
				DatagramSocket ds = datagramSocket;
				try {
					if (ds == null) {
						ds = new DatagramSocket(portLocal);
					} else if (!ds.isConnected()) {
						connectSocket();
					}
					InetAddress serverAddr = InetAddress.getByName(ipstr);
					DatagramPacket dp;
					dp = new DatagramPacket(udpMsg.getBytes(), udpMsg.length(),
							serverAddr, port);
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
						// ds.close();
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
	public void sendUDPCommand(final byte[] data) {

		new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				// String udpMsg = command;
				DatagramSocket ds = datagramSocket;
				try {
					if (ds == null) {

						ds = new DatagramSocket(portLocal);
					} else if (!ds.isConnected()) {
						connectSocket();
					}

					InetAddress serverAddr = InetAddress.getByName(ipstr);
					DatagramPacket dp;
					dp = new DatagramPacket(data, data.length, serverAddr, port);
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
						// ds.close();
					}
				}
				super.run();
			}

		}.start();
		;
	}
}
