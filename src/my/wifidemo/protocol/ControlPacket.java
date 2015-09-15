package my.wifidemo.protocol;

public class ControlPacket {

	
	private byte[] header={0,0};
	private byte[] type={0};
	private byte[] body={0,0,0,0,0,0,0,0};
	private byte[] checksum={0};
	
	private byte[] wholePacket;
	
	public ControlPacket() {
		
		
	}
	
	public byte[] getHeader() {
		return header;
	}
	public void setHeader(byte[] header) {
		this.header = header;
	}
	public byte[] getType() {
		return type;
	}
	public void setType(byte[] type) {
		this.type = type;
	}
	public byte[] getBody() {
		return body;
	}
	public void setBody(byte[] body) {
		this.body = body;
	}
	public byte[] getChecksum() {
		return checksum;
	}
	public void setChecksum(byte[] checksum) {
		this.checksum = checksum;
	}
	
	
	
}
