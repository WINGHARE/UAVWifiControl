package my.wifidemo.protocol;

import java.io.UnsupportedEncodingException;


public class ControlPacket {

	public static final int HEADER_UNKNWON=0;
	public static final int HEADER_IN=1;
	public static final int HEADER_OUT=2;

	public static final int TYPR_UNKNWON=0;
	public static final int TYPE_ADJUST=1;
	public static final int TYPE_RESERVE=2;
	public static final int TYPE_CONTROL=3;
	public static final int TYPE_PID1=4;
	public static final int TYPE_PID2=5;
	
	private byte[] command;
	
	public ControlPacket() {
		
		command=new byte[13];

	}
	public ControlPacket(byte[] byteArray){
		command=byteArray;
	}
	
	public static ControlPacket parse(byte[] byteArray){	
		ControlPacket controlPacket=new ControlPacket(byteArray);
		return controlPacket;
	}
	
	public byte[] getHeader() {
		byte[] header={0,0};
		header[0]=command[0];
		header[1]=command[1];
		return header;
	}
	
	public void setHeader(int typeCode){
		switch (typeCode) {
		case HEADER_IN:

			break;
		case HEADER_OUT:
		{
			command[0]=(byte)0xAA;
			command[1]=(byte)0xAF;
		}
		default:
			break;
		}
		
		calculateChecksum();
	}
	
	public int judgeHeaderType(){
		
		switch (command[1]) {
		case (byte) 0xAf:
			return HEADER_IN;
		case (byte) 0xAA:
			return HEADER_OUT;

		default:
			break;
		}
		return HEADER_UNKNWON;
	}
	
	/**设置本条指令协议的类型
	 * @param typeCode int 协议类型,具体类型封装于ControlPacket中的字段当中
	 * */
	public void setType(int typeCode){
		switch (typeCode) {
		case TYPE_ADJUST:
			command[2]=(byte)0x01;
			break;
		case TYPE_RESERVE:
			command[2]=(byte)0x02;
			break;
		case TYPE_CONTROL:
			command[2]=(byte)0x03;
			break;
		case TYPE_PID1:
			command[2]=(byte)0x10;
			break;
		case TYPE_PID2:
			command[2]=(byte)0x11;
			break;
		default:
			break;
		}
		
		calculateChecksum();
	}
	
	public int getType(){
		switch (command[2]) {
		case (byte)0x01:
			return TYPE_ADJUST;
		case (byte)0x02:
			return TYPE_RESERVE;
		case (byte)0x03:
			return TYPE_CONTROL;
		case (byte)0x10:
			return TYPE_PID1;
		case (byte)0x11:
			return TYPE_PID2;
		default:
			break;
		}	
		return TYPR_UNKNWON;
	}
	
	/**
	 * 设置要发送的飞行数据
	 * @param throttle
	 * @param yaw
	 * @param roll
	 * @param pitch
	 * 
	 * */
	
	public void setBody(short throttle,short yaw,short roll,short pitch ){
	//	byte[] buffer=new byte[2];
		command[5]=(byte)(throttle & 0xFF);
		command[6]=(byte)(throttle>>8 & 0xFF);
		command[7]=(byte)(yaw & 0xFF);
		command[8]=(byte)(yaw>>8 & 0xFF);
		command[9]=(byte)(roll & 0xFF);
		command[10]=(byte)(roll>>8 & 0xFF);
		command[11]=(byte)(pitch & 0xFF);
		command[12]=(byte)(pitch>>8 & 0xFF);
		
		calculateChecksum();
		
	}
	
	public void setBody(int throttle,int yaw,int roll,int pitch ){
		
		    short[] temp={(short) throttle,(short) yaw,(short) roll,(short) pitch};
			command[5]=(byte)(temp[0] & 0xFF);
			command[6]=(byte)(temp[0]>>8 & 0xFF);
			command[7]=(byte)(temp[1] & 0xFF);
			command[8]=(byte)(temp[1]>>8 & 0xFF);
			command[9]=(byte)(temp[2] & 0xFF);
			command[10]=(byte)(temp[2]>>8 & 0xFF);
			command[11]=(byte)(temp[3] & 0xFF);
			command[12]=(byte)(temp[3]>>8 & 0xFF);
			
			calculateChecksum();
			
	}

	public void calculateChecksum(){
		long sum=0;
		for(int i=0;i<12;i++){
			sum+=command[i];
		}
		command[12]=(byte)(sum & 0xFF);
	}
	public byte getChecksum() {
		return command[12];
	}
	
	
	/** *//**  
	    * 把字节数组转换成16进制字符串  
	    * @param bArray  
	    * @return  
	    */   
	private String bytesToHexString(byte[] bArray) {   
	    StringBuffer sb = new StringBuffer(bArray.length);   
	    String sTemp;   
	    for (int i = 0; i < bArray.length; i++) {   
	     sTemp = Integer.toHexString(0xFF & bArray[i]);   
	     if (sTemp.length() < 2)   
	      sb.append(0);   
	     sb.append(sTemp.toUpperCase());   
	    }   
	    return sb.toString();   
	}  
	
	public String getPacketString(){
		
		return bytesToHexString(command);
	}
	public byte[] getCommand() {
		return command;
	}
	
	
	
}

