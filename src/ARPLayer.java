import java.util.ArrayList;

public class ARPLayer implements BaseLayer{
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	public ArrayList<ArrayList<String>> cacheTable = new ArrayList<ArrayList<String>>();
	
	private class ARP_FRAME {
		byte[] sender_mac;
		byte[] target_mac;
		byte[] sender_ip;
		byte[] target_ip;
		byte[] opcode;
		byte hardsize;
		byte protsize;
		byte[] hardtype;
		byte[] prottype;
		
		ARP_FRAME(){
			byte[] sender_mac = new byte[6];
			byte[] target_mac = new byte[6];
			byte[] sender_ip = new byte[4];
			byte[] target_ip = new byte[4];
			byte[] opcode = new byte[2];
			byte hardsize = 1;
			byte protsize = 1;
			byte[] hardtype = new byte[2];
			byte[] prottype = new byte[2];
			opcode = intToByte2(1); //default 1
		}
	}
	
	ARP_FRAME frame = new ARP_FRAME();
	
	public ARPLayer(String pName) {
		pLayerName = pName;
		//ResetFrame();
	}
	
	public void ResetFrame() {
		for (int i=0; i<6; i++) {
			frame.sender_mac[i] = (byte) 0x00;
			frame.target_mac[i] = (byte) 0x00;
		}
		for (int j=0; j<4; j++) {
			frame.sender_ip[j] = (byte) 0x00;
			frame.target_ip[j] = (byte) 0x00;
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
//		if(byte2ToInt(frame.opcode[0], frame.opcode[1]) == 2){ //ARP응답
//			byte[] mac_dst = frame.sender_mac;
//			byte[] mac_src = 
//		}
		
		for(int i = 0; i < 2; i++) {
			buf[i] = frame.hardtype[i];
			buf[i+2] = frame.hardtype[i];
			buf[i+6] = frame.opcode[i];
		}
		
		buf[4] = frame.hardsize;
		buf[5] = frame.protsize;
		
		for(int i = 0; i < 6; i++) {
			buf[i+8] = frame.sender_mac[i];
			buf[i+18] = frame.target_mac[i];
		}	
		for(int i=0; i < 4; i++) {
			buf[i+14] = frame.sender_ip[i];
			buf[i+24] = frame.target_ip[i];
		}
		return buf;
	}
	
	public boolean ARPSend(byte[] ip_src, byte[] ip_dst) {
		frame.prottype = intToByte2(0x0800);
		frame.sender_ip = ip_src;
		frame.target_ip = ip_dst;
		byte[] bytes = ObjToByte(frame, 28);
		((EthernetLayer) this.GetUnderLayer()).ARPSend(bytes, 28);
		return false;
	}
	
	public boolean setCacheTable(byte[] input){
		ArrayList<String> cache = new ArrayList<String>();
		
		cacheTable.add(cache);
		return true; 
	}
	
	public boolean ARPReceive(byte[] input) {
		int protocol_type; //Protocol type
		int ARP_Request = 1 ; //ARP Request Opcode
		
		if(ARP_Request == 1) { //ARP요청
			
			return true;
		}
		else if(ARP_Request == 2) { //MAC주소 받아온 ARP
			return true;
		}
		return false;
	}
	
	public byte[] GetArpSrcAddress() {
		return frame.sender_mac;
	}
	
	public byte[] GetArpDstAddress() {
		return frame.target_mac;
	}
	
	public void SetArpSrcAddress(byte[] input) {
		for (int i = 0; i < 6; i++) {
			frame.sender_mac[i] = input[i];
		}
	}
	
	public void SetArpDstAddress(byte[] input) {
		for (int i = 0; i < 6; i++) {
			frame.sender_mac[i] = input[i];
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
