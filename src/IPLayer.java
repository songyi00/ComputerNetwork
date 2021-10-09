import java.util.ArrayList;

public class IPLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	
	private class _IP_ADDR {
		private byte[] addr = new byte[4];

		public _IP_ADDR() {
			this.addr[0] = (byte) 0x00;
			this.addr[1] = (byte) 0x00;
			this.addr[2] = (byte) 0x00;
			this.addr[3] = (byte) 0x00;
		}
	}

	private class _IP_HEADER {
		byte ip_verlen;
		byte ip_tos;
		byte[] ip_len;
		byte[] ip_id;
		byte[] ip_fragoff;
		byte ip_ttl;
		byte ip_proto;
		byte[] ip_cksum;
		_IP_ADDR ip_src;
		_IP_ADDR ip_dst;
		byte[] ip_data;

		public _IP_HEADER() {
			this.ip_src = new _IP_ADDR();
			this.ip_dst = new _IP_ADDR();
			this.ip_len = new byte[2];
			this.ip_id = new byte[2];
			this.ip_fragoff = new byte[2];
			this.ip_cksum = new byte[2];
			this.ip_data = null;
		}
	}

	_IP_HEADER m_sHeader = new _IP_HEADER();

	public IPLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		ResetHeader();
	}
	
	public void ResetHeader() {
		for (int i = 0; i < 4; i++) {
			m_sHeader.ip_dst.addr[i] = (byte) 0x00;
			m_sHeader.ip_src.addr[i] = (byte) 0x00;
		}
		for (int i=0; i<2; i++) {
			m_sHeader.ip_len[i] = (byte) 0x00;
			m_sHeader.ip_id[i] = (byte) 0x00;
			m_sHeader.ip_fragoff[i] = (byte) 0x00;
			m_sHeader.ip_cksum[i] = (byte) 0x00;
		}
		m_sHeader.ip_verlen = (byte) 0x00;;
		m_sHeader.ip_tos = (byte) 0x00;;
		m_sHeader.ip_ttl = (byte) 0x00;;
		m_sHeader.ip_proto = (byte) 0x00;;
		m_sHeader.ip_data = null;
	}
	
	public _IP_ADDR GetIPDstAddress() {
		return m_sHeader.ip_dst;
	}

	public _IP_ADDR GetIPSrcAddress() {
		return m_sHeader.ip_src;
	}
	
	public void SetIpDstAddress(byte[] input) {
		for (int i = 0; i < 6; i++) {
			m_sHeader.ip_dst.addr[i] = input[i];
		}
	}

	public void SetIpSrcAddress(byte[] input) {
		for (int i = 0; i < 6; i++) {
			m_sHeader.ip_src.addr[i] = input[i];
		}
	}
	
	public void ARPSend(byte[] src, byte[] dst) {
		this.SetIpSrcAddress(src);
		this.SetIpDstAddress(dst);
		((ARPLayer) this.GetUnderLayer()).ARPSend(src, dst);
	}
	
//	public byte[] ObjToByte(_IP_HEADER Header, byte[] input, int length) {//data
//		byte[] buf = new byte[length + 14];
//		for(int i = 0; i < 4; i++) {
//			buf[i] = Header.ip_dst.addr[i];
//			buf[i+4] = Header.ip_src.addr[i];
//		}			
//		buf[] = Header.enet_type[0];
//		buf[13] = Header.enet_type[1];
//		for (int i = 0; i < length; i++)
//			buf[14 + i] = input[i];
//
//		return buf;
//	}
//	
//	public boolean Send(byte[] input, int length) {
//		m_sHeader.enet_type = intToByte2(0x2080);
//		m_sHeader.enet_data = input;
//		byte[] bytes = ObjToByte(m_sHeader, input, length);
//		this.GetUnderLayer().Send(bytes, length + 14);
//
//		return true;
//	}
//	
//	public boolean fileSend(byte[] input, int length) {
//		m_sHeader.enet_type = intToByte2(0x2090);
//		byte[] bytes = ObjToByte(m_sHeader, input, length);
//		this.GetUnderLayer().Send(bytes, length + 14);
//		return true;
//	}
	
	@Override
	public String GetLayerName() {
		return pLayerName;
	}
	@Override
	public BaseLayer GetUnderLayer() {
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}
	@Override
	public BaseLayer GetUpperLayer(int nindex) {
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}
	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		if (pUnderLayer == null)
			return;
		this.p_UnderLayer = pUnderLayer;
	}
	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
	}
	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}
}
