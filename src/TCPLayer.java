import java.util.ArrayList;

public class TCPLayer implements BaseLayer{
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	private class _TCP_HEADER {
		byte[] tcp_sport;	// source port
		byte[] tcp_dport;	// destination port
		byte[] tcp_seq;		// sequence number
		byte[] tcp_ack;		// acknowledge sequence
		byte[] tcp_offset;	// no use
		byte[] tcp_flag;	// control flag
		byte[] tcp_window;	// no use
		byte[] tcp_cksum;	// check sum
		byte[] tcp_urgptr;	// no use
		byte[] padding;		
		byte[] tcp_data;

		public _TCP_HEADER() {
			this.tcp_sport = new byte[2];
			this.tcp_dport = new byte[2];
			this.tcp_seq = new byte[4];
			this.tcp_ack = new byte[4];
			this.tcp_offset = new byte[1];
			this.tcp_flag = new byte[1];
			this.tcp_window = new byte[2];
			this.tcp_cksum = new byte[2];
			this.tcp_urgptr = new byte[2];
			this.padding = new byte[4];
			this.tcp_data = null;
		}
	}
	
	_TCP_HEADER m_sHeader = new _TCP_HEADER();
	
	public TCPLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		ResetHeader();
	}
	
	public void ResetHeader() {
		for(int i = 0 ; i < 2; i++) {
			m_sHeader.tcp_sport[i] = (byte) 0x00;
			m_sHeader.tcp_dport[i] = (byte) 0x00;
			m_sHeader.tcp_window[i] = (byte) 0x00;
			m_sHeader.tcp_cksum[i] = (byte) 0x00;
			m_sHeader.tcp_urgptr[i] = (byte) 0x00;
		}
		for(int i = 0 ; i < 4; i++) {
			m_sHeader.tcp_seq[i] = (byte) 0x00;
			m_sHeader.tcp_ack[i] = (byte) 0x00;
			m_sHeader.padding[i] = (byte) 0x00;
		}
		m_sHeader.tcp_offset[0] = (byte) 0x00;
		m_sHeader.tcp_flag[0] = (byte) 0x00;
		m_sHeader.tcp_data = null;
	}
//
//	public byte[] GetPortDstAddress() {
//		return m_sHeader.tcp_dport;
//	}
//
//	public byte[] GetPortSrcAddress() {
//		return m_sHeader.tcp_sport;
//	}
//
//	public void SetEnetType(byte[] input) {
//		for (int i = 0; i < 2; i++) {
//			m_sHeader.enet_type[i] = input[i];
//		}
//	}
//
//	public void SetEnetDstAddress(byte[] input) {
//		for (int i = 0; i < 6; i++) {
//			m_sHeader.enet_dstaddr.addr[i] = input[i];
//		}
//	}
//
//	public void SetEnetSrcAddress(byte[] input) {
//		for (int i = 0; i < 6; i++) {
//			m_sHeader.enet_srcaddr.addr[i] = input[i];
//		}
//	}
//
//	public byte[] ObjToByte(_TCP_HEADER Header, byte[] input, int length) {//data에 헤더 붙여주기
//		byte[] buf = new byte[length + 14];
//		for(int i = 0; i < 6; i++) {
//			buf[i] = Header.enet_dstaddr.addr[i];
//			buf[i+6] = Header.enet_srcaddr.addr[i];
//		}			
//		buf[12] = Header.enet_type[0];
//		buf[13] = Header.enet_type[1];
//		for (int i = 0; i < length; i++)
//			buf[14 + i] = input[i];
//
//		return buf;
//	}
//	
//	private byte[] intToByte2(int value) {
//        byte[] temp = new byte[2];
//        temp[0] |= (byte) ((value & 0xFF00) >> 8);
//        temp[1] |= (byte) (value & 0xFF);
//
//        return temp;
//    }
//
//    private int byte2ToInt(byte value1, byte value2) {
//        return (int)((value1 << 8) | (value2));
//    }
//
//    // 브로드 캐스트일 경우, type이 0xff
//	public boolean Send(byte[] input, int length) {
//		m_sHeader.enet_type = intToByte2(0x2080);
//		m_sHeader.enet_data = input;
//		byte[] bytes = ObjToByte(m_sHeader, input, length);
//		this.GetUnderLayer().Send(bytes, length + 14);
//
//		return true;
//	}
//    // 파일 보낼 때
//    // type을 0으로 설정
//	public boolean fileSend(byte[] input, int length) {
//		m_sHeader.enet_type = intToByte2(0x2090);
//		byte[] bytes = ObjToByte(m_sHeader, input, length);
//		this.GetUnderLayer().Send(bytes, length + 14);
//		return true;
//		
//	}
//
//	public byte[] RemoveEtherHeader(byte[] input, int length) {
//		byte[] data = new byte[length - 14];
//		for (int i = 0; i < length - 14; i++)
//			data[i] = input[14 + i];
//		return data;
//	}
//
//	public boolean IsItMyPacket(byte[] input) {
//		for (int i = 0; i < 6; i++) {
//			if (m_sHeader.enet_srcaddr.addr[i] == input[6 + i])
//				continue;
//			else
//				return false;
//		}
//		return true;
//	}
//
//	public boolean IsItMine(byte[] input) {
//		for (int i = 0; i < 6; i++) {
//			if (m_sHeader.enet_srcaddr.addr[i] == input[i])
//				continue;
//			else {
//				return false;
//			}
//		}
//		return true;
//	}
//
//	public boolean IsItBroadcast(byte[] input) {
//		for (int i = 0; i < 6; i++) {
//			if (input[i] == 0xff) {
//				continue;
//			} else
//				return false;
//		}
//		return true;
//	}
//
//	public boolean Receive(byte[] input) {
//		byte[] data;
//		int temp_type = byte2ToInt(input[12], input[13]); 
//		if(temp_type == (byte)0x2080) { //data
//			if(chkAddr(input) || (IsItBroadcast(input)) || !IsItMyPacket(input)) {
//				data = RemoveEtherHeader(input, input.length);
//				this.GetUpperLayer(0).Receive(data);
//				return true;
//			}
//		}
//		else if(temp_type == (byte)0x2090) { //file
//			if(chkAddr(input) || (IsItBroadcast(input)) || !IsItMyPacket(input)) {
//				data = RemoveEtherHeader(input, input.length);
//				this.GetUpperLayer(1).Receive(data);
//				return true;
//			}
//		}
//		return false; 
//	
//	}
//	
//	private boolean chkAddr(byte[] input) {
//		byte[] temp = m_sHeader.enet_srcaddr.addr;
//		for(int i = 0; i< 6; i++)
//			if(m_sHeader.enet_srcaddr.addr[i] != input[i])
//				return false;
//		return true;
//	}

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
		// nUpperLayerCount++;
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
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);

	}
	
	// Application에서 받은 ip 주소를 IP Layer에게 ip 주소 넘기기
	public void ARPsend(byte[] src_ip, byte[] dst_ip) {
		IPLayer.ARPsend(src_ip, dst_ip);
	}

	
}
