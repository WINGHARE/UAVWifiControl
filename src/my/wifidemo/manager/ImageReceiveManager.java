package my.wifidemo.manager;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class ImageReceiveManager {

	
	public static final String TAG = "IMAGE_RECEIVE_MANAGER";

	private int port;
	private int portLocal;
	private String ipstr;
	private Context mContext;
	private Handler myHandler;
	private static Boolean imageRecenable=true;
	private static Boolean cameraOpenFlagBoolean = false;
	
	
	public static final int REFRESH_VIEW = 0;
	public static final int DISPLY_DIALOG = 1;
	public static final int DISMISS_DIALOG = 2;
	public static final int RESET_BUTTON_STATUS=3;

	private ImageReceiveThread imageReceiveThread;
	private OpenCameraThread openCameraThread;
	
	public ImageReceiveManager(int port,int portlocal, String ipstr, Context mContext,Handler handler) {
		super();
		this.port = port;
		this.ipstr = ipstr;
		this.portLocal=portlocal;
		this.mContext = mContext;
		this.myHandler =handler;
	}


	
	/**
	 * 在子线程中将消息发送给UI线程显示Diaglog
	 * 
	 * @param messageString
	 *            String 需要在dialog中显示的消息内容
	 * **/
	private void displayDialog(String messageString) {
		Message message = new Message();
		message.what = DISPLY_DIALOG;
		message.obj = messageString;
		myHandler.sendMessage(message);
	}

	/**
	 * 将主线程的dialog清除
	 */
	private void dismissDialog() {
		Message message = new Message();
		message.what = DISMISS_DIALOG;
		myHandler.sendMessage(message);
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
	 * 在输入流中获取下一张图片的图片长度
	 * 
	 * @return bodyLength int 返回图片长度
	 * @param inputStream
	 *            InputStream 指定的输入流
	 * */
	private int getBodylength(InputStream inputStream) {
		byte[] buffer = new byte[4];
		try {
			inputStream.read(buffer, 0, 4);
			// 读取接受到的输入流的前四个字节
			ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			int bodyLength = byteBuffer.getInt();
			// 将接受到的前四个字节以小端储存的形式解析为整形获取本体长度。
			if (bodyLength < 0 || bodyLength > 65535) {
				// return getBodylength(inputStream);
			}
			// 处理获取数据流长度失败的情况
			return bodyLength;
			// 返回获取当前字节流表示的图片的长度
			// Log.i(TAG,""+bodyLength);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.i(TAG, "length error");
			return -1;
		}

	}

	public void startReceive(){
		imageRecenable=true;
		imageReceiveThread = new ImageReceiveThread("IMAGERECEIVE_THREAD");
		imageReceiveThread.start();
	}
	
	public void openCamera(){
		cameraOpenFlagBoolean=true;
		openCameraThread = new OpenCameraThread(
				"OPENCAMRERA_THREAD");
		openCameraThread.start();
	}
	
	public void closeCamera(){
		synchronized (cameraOpenFlagBoolean) {
			cameraOpenFlagBoolean=false;
		}
	}
	
	public void disableImageRec(){
		synchronized (imageRecenable) {
			imageRecenable=false;		
		}
	}

	/**
	 * 接收图像的线程
	 * */
	

	class ImageReceiveThread extends HandlerThread {

		private Socket socket = null;
		BufferedInputStream inputStream = null;

		public ImageReceiveThread(String name) {
			super(name);
		}

		@Override
		public void run() {
			Log.i(TAG, "[ImageReceiveThread]Current thread id: "
					+ Thread.currentThread().getId());

			try {
				if (socket == null) {
					socket = new Socket();
					socket.connect(
							new InetSocketAddress(ipstr, port),
							30000);
					socket.setSoTimeout(10000);
				}
				// 创建连接的套接字，设置服务器连接的超时时间，以及每次读取的超时时间

				if (socket.isConnected()) {
					dismissDialog();
					displayDialog("开始传输视频");
				}

				while (socket.isConnected() && cameraOpenFlagBoolean 
						&& imageRecenable) {
				
					if (socket.getInputStream().available() < 4) {
						continue;
					}
					// socket在没有接收齐4个字节的数据之前退出循环

					int bodyLength = 0;
					bodyLength = getBodylength(socket.getInputStream());
					// 获取本次接受到的jpg图片的长度

					if (bodyLength > 0 && bodyLength < 65535) {

						int sum = 0;
						int increment = 4;
						InputStream inputStream = socket.getInputStream();
						byte[] buffer = new byte[bodyLength];
						
						
						inputStream.read(buffer,sum,increment);
						sum+=increment;
						
						if(buffer[0]!=-1 ||
								buffer[1]!=-40){						
							Log.i(TAG, "kek");
							continue;
						}
						
						while (sum < bodyLength) {
							inputStream.read(buffer, sum, increment);
							sum += increment;
						}

						inputStream.read(buffer, sum, bodyLength - sum);

						// 从流中读取数据到缓冲区当中

						Message message = new Message();
						message.what = REFRESH_VIEW;
						message.obj = buffer;
						message.arg1 = bodyLength;
						myHandler.sendMessage(message);
						// 将接受到的数据发送给handler提交给UI线程刷新UI
					}
				}
				if (cameraOpenFlagBoolean == false && socket.isConnected()) {

					byte[] disbleCamera = { (byte) 0xaa, (byte) 0x55,
							(byte) 0xcc, (byte) 0x33 };

					socket.shutdownInput();
					OutputStream outputStream = socket.getOutputStream();
					outputStream.write(disbleCamera);
					outputStream.close();
					socket.close();

					Message message = new Message();
					message.what = REFRESH_VIEW;
					byte[] r = { (byte) 0xff };
					message.obj = r;
					message.arg1 = 1;
					myHandler.sendMessage(message);
					// 将View的图像清除

					displayDialog("连接已关闭");
				}
			} catch (SocketTimeoutException e) {
				// TODO: handle exception
				dismissDialog();
				displayDialog("服务器连接超时");
				sendTCPException();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				dismissDialog();
				displayDialog("主机地址错误");
				sendTCPException();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				dismissDialog();
				displayDialog("读写数据时出现问题");
				sendTCPException();

			}catch(ArrayIndexOutOfBoundsException e){ 
				e.printStackTrace();
				dismissDialog();
				displayDialog("数组溢出");
				sendTCPException();
			}
			finally {
				try {
					socket.close();
					resetButtonStatus();
				} catch (IOException e) {
					e.printStackTrace();
					dismissDialog();
					displayDialog("关闭主机时出现问题");
					resetButtonStatus();
					sendTCPException();
				}
			}
		}
	}
	
	
	/**
	 * Modification The thread to open camera
	 **/
	class OpenCameraThread extends HandlerThread {

		private Socket socket = null;

		public OpenCameraThread(String name) {
			super(name);
		}

		@Override
		public void run() {
	
			
			
			Log.i(TAG, "[OpenCameraThread] thread ID: "
					+ Thread.currentThread().getId());

			displayDialog("正在连接摄像头 IP: " + ipstr);

			// TODO Auto-generated method stub
			try {

				if (socket == null) {
					socket = new Socket();
					socket.connect(
							new InetSocketAddress(ipstr, port),
							30000);
				}
				// 创建连接的套接字，设置服务器连接的超时时间

				Log.i(TAG, "[OpenCameraThread]socket created ，thread ID:"
						+ Thread.currentThread().getId());
				if (socket.isConnected()) {

					Log.i(TAG, "[OpenCameraThread]Host connected");
					OutputStream outputStream;
					try {
						byte[] enableCamera = { (byte) 0x5a, (byte) 0xa5,
								(byte) 0xc3, (byte) 0x3c };
						outputStream = socket.getOutputStream();
						outputStream.write(enableCamera);
						outputStream.close();
						socket.close();
						synchronized (cameraOpenFlagBoolean) {
							cameraOpenFlagBoolean = true;
						}
						Log.i(TAG, "[OpenCameraThread]Command sent");
						dismissDialog();
						displayDialog("摄像头已打开 IP: " + ipstr);

						// 讲打开摄像头的字节码指令发送给摄像头
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					dismissDialog();
					displayDialog("服务器连接失败");
					resetButtonStatus();
				}
			} catch (SocketTimeoutException e) {
				e.printStackTrace();
				dismissDialog();
				displayDialog("服务器连接超时");
				resetButtonStatus();

			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				dismissDialog();
				displayDialog("服务器连接失败");
				resetButtonStatus();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				dismissDialog();
				displayDialog("摄像头指令发送失败");
				resetButtonStatus();
			}
			
		}
	}


	/**
	 * 使用UDP向开发板发送信号
	 * 
	 * @param command
	 *            String 需要发送的信号
	 * */
	private void sendUDPCommand(final String command) {

		new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				String udpMsg = command;
				DatagramSocket ds;
				try {
						ds = new DatagramSocket();
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
					
				}
				super.run();
			}

		}.start();
		;
	}

	/**
	 * 使用UDP向开发板发送TCP错误
	 * 
	 * */
	private void sendTCPException() {
		String udpMsg = "TCP_EXCEPTION";
		sendUDPCommand(udpMsg);
	}
}
