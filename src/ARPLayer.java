import java.util.ArrayList;

public class ARPLayer implements BaseLayer{
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	public ArrayList<ArrayList<String>> cacheTable = new ArrayList<ArrayList<String>>();
	
	private class _IP_ADDR {
		private byte[] addr = new byte[4];

		public _IP_ADDR() {
			this.addr[0] = (byte) 0x00;
			this.addr[1] = (byte) 0x00;
			this.addr[2] = (byte) 0x00;
			this.addr[3] = (byte) 0x00;
		}
	}
	
	private class _ETHERNET_ADDR {
		private byte[] addr = new byte[6];

		public _ETHERNET_ADDR() {
			this.addr[0] = (byte) 0x00;
			this.addr[1] = (byte) 0x00;
			this.addr[2] = (byte) 0x00;
			this.addr[3] = (byte) 0x00;
			this.addr[4] = (byte) 0x00;
			this.addr[5] = (byte) 0x00;
		}
	}
	
	private class ARP_FRAME {
		_ETHERNET_ADDR sender_mac;
		_ETHERNET_ADDR target_mac;
		_IP_ADDR sender_ip;
		_IP_ADDR target_ip;
		byte[] opcode;
		byte hardsize;
		byte protsize;
		byte[] hardtype;
		byte[] prottype;
		
		ARP_FRAME(){
			this.sender_mac = new _ETHERNET_ADDR();
			this.target_mac = new _ETHERNET_ADDR();
			this.sender_ip = new _IP_ADDR();
			this.target_ip = new _IP_ADDR();
			this.opcode = new byte[2];
			this.hardsize = 1;
			this.protsize = 1;
			this.hardtype = new byte[2];
			this.prottype = new byte[2];
			this.opcode = intToByte2(1); //default 1
		}
	}
	
	ARP_FRAME frame = new ARP_FRAME();
	
	public ARPLayer(String pName) {
		pLayerName = pName;
		ResetFrame();
	}
	
	public void ResetFrame() {
		for (int i=0; i<6; i++) {
			frame.sender_mac.addr[i] = (byte) 0x00;
			frame.target_mac.addr[i] = (byte) 0x00;
		}
		for (int j=0; j<4; j++) {
			frame.sender_ip.addr[j] = (byte) 0x00;
			frame.target_ip.addr[j] = (byte) 0x00;
		}
		for (int k=0; k<2; k++) {
			frame.opcode[k] = (byte) 0x00;
			frame.hardtype[k] = (byte) 0x00;
			frame.prottype[k] = (byte) 0x00;
		}
	}
	
	private byte[] intToByte2(int value) {
        byte[] temp = new byte[2];
        temp[0] |= (byte) ((value & 0xFF00) >> 8);
        temp[1] |= (byte) (value & 0xFF);

        return temp;
    }
	
	private int byte2ToInt(byte value1, byte value2) {
        return (int)((value1 << 8) | (value2));
    }
	
	public byte[] ObjToByte(ARP_FRAME frame, int length) {//frame을 구성해서 byte data로 return
		byte[] buf = new byte[28];
		if(byte2ToInt(frame.opcode[0], frame.opcode[1]) == 2){ //ARP응답
			_ETHERNET_ADDR temp = frame.sender_mac;
			frame.target_mac = temp;
			frame.sender_mac = temp; 
		}
		
		for(int i = 0; i < 2; i++) {
			buf[i] = frame.hardtype[i];
			buf[i+2] = frame.prottype[i];
			buf[i+6] = frame.opcode[i];
		}
		
		buf[4] = frame.hardsize;
		buf[5] = frame.protsize;
		
		for(int i = 0; i < 6; i++) {
			buf[i+8] = frame.sender_mac.addr[i];
			buf[i+18] = frame.target_mac.addr[i];
		}	
		for(int i=0; i < 4; i++) {
			buf[i+14] = frame.sender_ip.addr[i];
			buf[i+24] = frame.target_ip.addr[i];
		}
		return buf;
	}
	
	public boolean ARPSend(byte[] ip_src, byte[] ip_dst) {
		frame.prottype = intToByte2(0x0800);
		this.SetIpSrcAddress(ip_src);
		this.SetIpDstAddress(ip_dst);
		byte[] bytes = ObjToByte(frame, 28);
		((EthernetLayer) this.GetUnderLayer()).ARPSend(bytes, 28);
		return false;
	}
	
	public boolean setCacheTable(byte[] input){//cache table setting
		ArrayList<String> cache = new ArrayList<String>();
		
		cacheTable.add(cache);
		return true; 
	}
	
	public boolean ARPReceive(byte[] input) {
		// 1. ARP Message다 채워져서 ethernet에 도착했을 때
		// 송신자의 Ethernet으로 다시 돌아옴
		// 2. ethernet header의 dst가 broadcast인 경우
		// 처음 수신자의 ARP Layer에 도달
		int ARP_Request = byte2ToInt(input[6], input[7]); //ARP Opcode
		
		if(ARP_Request == 1) { //ARP Request
			// 1. broadcast일때. 즉 처음 주소 물어볼 때.
			// 각 host는 자신이 목적지인지 확인하기 전에 table에 지금 ARP 요청 보낸 host(sender)의 IP와 MAC 저장
			// 각 host는 자신이 목적지가 맞는지 확인함. -> ARP message에 있는 target IP 보고
			// 목적지 아니면 drop. 맞으면 ARP message에 있는 target mac에 자신의 MAC 주소 넣음 
			// ARP 응답 메시지를 seder에데 보내기 위해 ARP message에 있는 sender's와 target's swap
			// opcode 2로 바꿈. -> 왜냐면 ARP reply위해서.
			
			setCacheTable(input);
			
			return true;
		}
		else if(ARP_Request == 2) { //ARP Reply
			// sender의 ARP Layer가 받음. 
			// ARP messgae target's hardware보고 sender는 table 채움.
			//ip, mac변수에 setting -> Dlg에서 get해서 화면에 출력
			return true;
		}
		return false;
	}
	
	public _ETHERNET_ADDR GetArpSrcAddress() {
		return frame.sender_mac;
	}
	
	public _ETHERNET_ADDR GetArpDstAddress() {
		return frame.target_mac;
	}
	
	public void SetArpSrcAddress(byte[] input) {
		for (int i = 0; i < 6; i++) {
			frame.sender_mac.addr[i] = input[i];
		}
	}
	
	public void SetArpDstAddress(byte[] input) {
		for (int i = 0; i < 6; i++) {
			frame.sender_mac.addr[i] = input[i];
		}
	}
	
	public _IP_ADDR GetIpSrcAddress() {
		return frame.sender_ip;
	}
	
	public _IP_ADDR GetIpDstAddress() {
		return frame.target_ip;
	}
	
	public void SetIpSrcAddress(byte[] input) {
		for (int i = 0; i < 6; i++) {
			frame.sender_ip.addr[i] = input[i];
		}
	}
	
	public void SetIpDstAddress(byte[] input) {
		for (int i = 0; i < 6; i++) {
			frame.sender_ip.addr[i] = input[i];
		}
	}
	
	@Override
	public String GetLayerName() {
		// TODO Auto-generated method stub
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {
		// TODO Auto-generated method stub
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) {
		// TODO Auto-generated method stub
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		// TODO Auto-generated method stub
		if (pUnderLayer == null)
			return;
		this.p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		// TODO Auto-generated method stub
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		// TODO Auto-generated method stub
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}
}
