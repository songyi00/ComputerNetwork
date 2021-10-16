
import java.util.ArrayList;

public class EthernetLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

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

	private class _ETHERNET_HEADER {
		_ETHERNET_ADDR enet_dstaddr;
		_ETHERNET_ADDR enet_srcaddr;
		byte[] enet_type;
		byte[] enet_data;

		public _ETHERNET_HEADER() {
			this.enet_dstaddr = new _ETHERNET_ADDR();
			this.enet_srcaddr = new _ETHERNET_ADDR();
			this.enet_type = new byte[2];
			this.enet_data = null;
		}
	}

	_ETHERNET_HEADER m_sHeader = new _ETHERNET_HEADER();

	public EthernetLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		ResetHeader();
	}

	public void ResetHeader() {
		for (int i = 0; i < 6; i++) {
			m_sHeader.enet_dstaddr.addr[i] = (byte) 0x00;
			m_sHeader.enet_srcaddr.addr[i] = (byte) 0x00;
		}
		m_sHeader.enet_type[0] = (byte) 0x00;
		m_sHeader.enet_type[1] = (byte) 0x00;
		m_sHeader.enet_data = null;
	}

	public _ETHERNET_ADDR GetEnetDstAddress() {
		return m_sHeader.enet_dstaddr;
	}

	public _ETHERNET_ADDR GetEnetSrcAddress() {
		return m_sHeader.enet_srcaddr;
	}

	public void SetEnetType(byte[] input) {
		for (int i = 0; i < 2; i++) {
			m_sHeader.enet_type[i] = input[i];
		}
	}

	public void SetEnetDstAddress(byte[] input) {
		for (int i = 0; i < 6; i++) {
			m_sHeader.enet_dstaddr.addr[i] = input[i];
		}
	}

	public void SetEnetSrcAddress(byte[] input) {
		for (int i = 0; i < 6; i++) {
			m_sHeader.enet_srcaddr.addr[i] = input[i];
		}
	}

	public byte[] ObjToByte(_ETHERNET_HEADER Header, byte[] input, int length) {//data占쎈� 占쎈엘占쎈�� �븐��肉т��⑤┛
		byte[] buf = new byte[length + 14];
		for(int i = 0; i < 6; i++) {
			buf[i] = Header.enet_dstaddr.addr[i];
			buf[i+6] = Header.enet_srcaddr.addr[i];
		}			
		buf[12] = Header.enet_type[0];
		buf[13] = Header.enet_type[1];
		for (int i = 0; i < length; i++)
			buf[14 + i] = input[i];

		return buf;
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
    
	public boolean Send(byte[] input, int length) {
		m_sHeader.enet_type = intToByte2(0x2080);
		m_sHeader.enet_data = input;
		byte[] bytes = ObjToByte(m_sHeader, input, length);
		this.GetUnderLayer().Send(bytes, length + 14);

		return true;
	}
	
	public boolean fileSend(byte[] input, int length) {
		m_sHeader.enet_type = intToByte2(0x2090);
		byte[] bytes = ObjToByte(m_sHeader, input, length);
		this.GetUnderLayer().Send(bytes, length + 14);
		return true;
	}
	
	// input �� ARP Message
	public boolean ARPSend(byte[] input, int length) {
		m_sHeader.enet_type = intToByte2(0x0806);
		m_sHeader.enet_data = input;
		byte[] bytes = ObjToByte(m_sHeader, input, length);
		this.GetUnderLayer().Send(bytes, length+14);
		return true;
	}

	public byte[] RemoveEtherHeader(byte[] input, int length) {
		byte[] data = new byte[length - 14];
		for (int i = 0; i < length - 14; i++)
			data[i] = input[14 + i];
		return data;
	}
	
	// �닿� 蹂대���� packet�� �ㅼ�� ����寃� �� 寃쎌�곗�몄�
	public boolean IsItMyPacket(byte[] input) {
		for (int i = 0; i < 6; i++) {
			if (m_sHeader.enet_srcaddr.addr[i] == input[6 + i])
				continue;
			else
				return false;
		}
		return true;
	}
	
	// 紐⑹��吏�媛� ���몄�
	public boolean IsItMine(byte[] input) {
		for (int i = 0; i < 6; i++) {
			if (m_sHeader.enet_srcaddr.addr[i] == input[i])
				continue;
			else {
				return false;
			}
		}
		return true;
	}

	public boolean IsItBroadcast(byte[] input) {
		for (int i = 0; i < 6; i++) {
			if (input[i] == 0xff) {
				continue;
			} else
				return false;
		}
		return true;
	}

	public boolean Receive(byte[] input) {
		byte[] data;
		System.out.println("ethernet receive");
		int temp_type = byte2ToInt(input[12], input[13]);
		System.out.println(temp_type);
		if(temp_type == Integer.decode("0x2080")) { //data
			System.out.println("2080");
			if(chkAddr(input) || (IsItBroadcast(input)) || !IsItMyPacket(input)) {
				data = RemoveEtherHeader(input, input.length);
				this.GetUpperLayer(0).Receive(data);
				return true;
			}
		}
		else if(temp_type == Integer.decode("0x2090")) { //file
			System.out.println("2090");
			if(chkAddr(input) || (IsItBroadcast(input)) || !IsItMyPacket(input)) {
				data = RemoveEtherHeader(input, input.length);
				this.GetUpperLayer(1).Receive(data);
				return true;
			}
		}else if(temp_type == Integer.decode("0x0806")) {
			System.out.println("0806");
			this.ARPReceive(input);
		}
		return false; 
	
	}
	
	public boolean ARPReceive(byte[] input) {
		byte[] data;
		// type�� 0x0806�대㈃ ARP
		System.out.println("ethernet arp receive");
		int temp_type = byte2ToInt(input[12], input[13]);
		if(temp_type == Integer.decode("0x0806")) {
			
			if(chkAddr(input) || !IsItMyPacket(input) || (IsItBroadcast(input))) {	// 
				data = RemoveEtherHeader(input, input.length);
				((ARPLayer) this.GetUpperLayer(0)).ARPReceive(data);
				return true;
			}
		}
		return false;
	}
	
	// 紐⑹��吏�媛� ���몄�(ethernet header�� dst二쇱��媛� ���몄� ����) 
	private boolean chkAddr(byte[] input) {
		byte[] temp = m_sHeader.enet_srcaddr.addr;
		for(int i = 0; i< 6; i++)
			if(m_sHeader.enet_srcaddr.addr[i] != input[i])
				return false;
		return true;
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

	
}
