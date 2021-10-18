import java.util.ArrayList;

public class ARPLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	public ArrayList<ArrayList<byte[]>> cacheTable = new ArrayList<ArrayList<byte[]>>();
	public ArrayList<ArrayList<byte[]>> proxyCacheTable = new ArrayList<ArrayList<byte[]>>();
	private static LayerManager m_LayerMgr = new LayerManager();
	public int state = 0;

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

		ARP_FRAME() {
			this.sender_mac = new _ETHERNET_ADDR();
			this.target_mac = new _ETHERNET_ADDR();
			this.sender_ip = new _IP_ADDR();
			this.target_ip = new _IP_ADDR();
			this.opcode = new byte[2];
			this.hardsize = 1;
			this.protsize = 1;
			this.hardtype = new byte[2];
			this.prottype = new byte[2];
			this.opcode = intToByte2(1); // default 1
		}
	}

	ARP_FRAME frame = new ARP_FRAME();

	public ARPLayer(String pName) {
		pLayerName = pName;
		ResetFrame();
	}

	public void ResetFrame() {
		for (int i = 0; i < 6; i++) {
			frame.sender_mac.addr[i] = (byte) 0x00;
			frame.target_mac.addr[i] = (byte) 0x00;
		}
		for (int j = 0; j < 4; j++) {
			frame.sender_ip.addr[j] = (byte) 0x00;
			frame.target_ip.addr[j] = (byte) 0x00;
		}
		for (int k = 0; k < 2; k++) {
			frame.opcode[k] = (byte) 0x00;
			frame.hardtype[k] = (byte) 0x00;
			frame.prottype[k] = (byte) 0x00;
		}
		frame.hardsize = 1;
		frame.protsize = 1;
		frame.opcode = intToByte2(1);
	}

	private byte[] intToByte2(int value) {
		byte[] temp = new byte[2];
		temp[0] |= (byte) ((value & 0xFF00) >> 8);
		temp[1] |= (byte) (value & 0xFF);

		return temp;
	}

	private int byte2ToInt(byte value1, byte value2) {
		return (int) ((value1 << 8) | (value2));
	}

	public byte[] ObjToByte(ARP_FRAME frame, int length) {// frame을 구성해서 byte data로 return
		byte[] buf = new byte[28];
		if (byte2ToInt(frame.opcode[0], frame.opcode[1]) == 2) { // ARP응답
			_ETHERNET_ADDR temp = frame.sender_mac;
			frame.target_mac = temp;
			frame.sender_mac = temp;
		}

		for (int i = 0; i < 2; i++) {
			buf[i] = frame.hardtype[i];
			buf[i + 2] = frame.prottype[i];
			buf[i + 6] = frame.opcode[i];
		}

		buf[4] = frame.hardsize;
		buf[5] = frame.protsize;

		for (int i = 0; i < 6; i++) {
			buf[i + 8] = frame.sender_mac.addr[i];
			buf[i + 18] = frame.target_mac.addr[i];
		}
		for (int i = 0; i < 4; i++) {
			buf[i + 14] = frame.sender_ip.addr[i];
			buf[i + 24] = frame.target_ip.addr[i];
		}
		return buf;
	}

	public boolean ARPSend(byte[] ip_src, byte[] ip_dst) {
		System.out.println("arp send");
		System.out.println("arp의 src_ip는? : " + Byte.toUnsignedInt(ip_src[2]) + "." + Byte.toUnsignedInt(ip_src[3]));
		frame.prottype = intToByte2(0x0800);
		this.SetIpSrcAddress(ip_src);
		this.SetIpDstAddress(ip_dst);
		byte[] bytes = ObjToByte(frame, 28);
		((EthernetLayer) this.GetUnderLayer()).ARPSend(bytes, 28);
		return false;
	}

	public boolean addCacheTable(byte[] input){//cache table setting
	      ArrayList<byte[]> cache = new ArrayList<byte[]>();
	      //proxycacheTable dlg에서 proxy가져오기 
	      //1. input으로 들어온 src_arp의 ip와 mac주소 가져와서 cache table에 존재하는지 확인 -> 없으면 넣기
	      //2. 이미 존재하는 ip라면 table의 mac주소와 target_arp를 확인해서 틀리면 바꿔버려 -> Garp
	      
	      byte[] ip_buf = new byte[4];
	      for(int i=0; i<4; i++) {   
	         ip_buf[i] = input[14+i]; //input의 ip주소 buffer 임시저장
	      }
	      
	      byte[] mac_buf = new byte[6];
	      for(int i=0; i<6; i++) {   
	         mac_buf[i] = input[i+8]; //input의 mac주소 buffer 임시저장
	      }
	      
	      boolean hasIP = false;
	      for(int i=0; i<cacheTable.size(); i++) {
	         if(java.util.Arrays.equals(ip_buf, cacheTable.get(i).get(0))) { //cacheTable에 ip주소가 존재
	            hasIP = true;
	            if(!java.util.Arrays.equals(mac_buf, cacheTable.get(i).get(1))) { //cacheTable에 저장된 mac주소가 sender_mac과 다름
	               //garp
	               cacheTable.get(i).set(1, mac_buf);
	            }
	         }
	      }
	      
	      if(hasIP == false) {//cacheTable에 ip주소가 존재하지 않은 경우
	         cache.add(ip_buf);   // cache[0]에 ip 주소 넣기
	         cache.add(mac_buf);   // cache[1]에 mac 주소 넣기
	         cache.add(intToByte2(1));  // cache[2]에  Complete넣기.1이면 complete.
	         cacheTable.add(cache);
	      }
	      ((ARPDlg)ARPDlg.m_LayerMgr.GetLayer("GUI")).setArpCache(cacheTable);
	      return true; 
	   }

	// proxy Table에 채우는 함수
	public boolean addProxyTable(byte[] interNum, byte[] proxy_ip, byte[] proxy_mac) {
		ArrayList<byte[]> proxy = new ArrayList<byte[]>();

		proxy.add(interNum); // proxy[0]에는 interface number 넣기
		proxy.add(proxy_ip); // proxy[1]에는 ip 주소 넣기
		proxy.add(proxy_mac); // proxy[2]에는 mac 주소 넣기

		proxyCacheTable.add(proxy);

		return true;
	}

	public boolean IsItMine(byte[] input) {
		for (int i = 0; i < 4; i++) {
			if (frame.sender_ip.addr[i] == input[i])
				continue;
			else {
				return false;
			}
		}
		return true;
	}

	// proxy table의 ip와 dst의 ip와 같은지 확인
	public boolean ProxyCheck(byte[] dst_ip) {
		for (int i = 0; i < proxyCacheTable.size(); i++) {
			boolean flag = true;
			ArrayList<byte[]> proxy = proxyCacheTable.get(i);
			for (int j = 0; j < 4; j++) {
				if (proxy.get(1)[j] == dst_ip[j]) {
					continue;
				} else {
					flag = false;
				}
			}
			if (flag == true) {
				return true;
			}
		}
		return false;
	}

	public boolean ARPReceive(byte[] input) {
		int ARP_Request = byte2ToInt(input[6], input[7]); // ARP Opcode

		if (ARP_Request == 1) { // ARP Request
			System.out.println("arp receive request");
			// 1.  dst가 broadcast인 경우. 즉 처음 주소 물어볼 때.
			// 각 host는 자신이 목적지인지 확인하기 전에 table에 지금 ARP 요청 보낸 host(sender)의 IP와 MAC 저장
			// 각 host는 자신이 목적지가 맞는지 확인함. -> ARP message에 있는 target IP 보고
			// 목적지 아니면 drop. 맞으면 ARP message에 있는 target mac에 자신의 MAC 주소 넣음
			// ARP 응답 메시지를 seder에데 보내기 위해 ARP message에 있는 sender's와 target's swap
			// opcode 2로 바꿈. -> 왜냐면 ARP reply위해서.
			addCacheTable(input);
			
			byte[] ip_buf = new byte[4];
			for(int i = 0; i < 4; i++) {	
				ip_buf[i] = input[24+i];
			}
			
			System.out.println("난 ip_buf야: " + Byte.toUnsignedInt(ip_buf[2])+ "." + Byte.toUnsignedInt(ip_buf[3]));
			System.out.println("내 src ip는? " + Byte.toUnsignedInt(frame.sender_ip.addr[2]) + "." + Byte.toUnsignedInt(frame.sender_ip.addr[3]));
			
			byte[] send_ip_b = new byte[4];
			System.arraycopy(input, 24, send_ip_b, 0, 4);
			
			byte[] target_ip_b = new byte[4];
			System.arraycopy(input, 14, target_ip_b, 0, 4);
			
			if(IsItMine(ip_buf)) {	// 내가 목적지임
				System.out.println("arp receive request is the dst");

				for(int i = 0; i < 4; i++) {	// target ip 주소 바꾸기
					frame.target_ip.addr[i] = input[14+i];
				}
				
				for(int i = 0; i < 6; i++) {
					frame.target_mac.addr[i] = input[8+i];
				}
				
				frame.opcode = intToByte2(2);
		
				ARPSend(send_ip_b, target_ip_b);
				frame.opcode = intToByte2(1);
				
			}else { // 내가 목적지가 아닌 경우
				// proxy ARP
				System.out.println("proxy ip buf는?");
				// 자신의 proxy table 확인
				boolean check = ProxyCheck(ip_buf);
				
				// 만약 proxy table에 target's mac 주소 있으면 target's mac 주소 채움
				// proxy table에 있으면 Dlg로 table 보내주기.
				// sender's와 target's 위치 swap.
				// opcode 2로 변경
				if(check==true) {
					frame.opcode = intToByte2(2);
			
					ARPSend(send_ip_b, target_ip_b);
					frame.opcode = intToByte2(1);
					
					((ARPDlg) ARPDlg.m_LayerMgr.GetLayer("GUI")).setArpCache(cacheTable);
					}
				}
			

			return true;
		} else if (ARP_Request == 2) { // ARP Reply
			System.out.println("arp receive reply");

			// sender의 ARP Layer가 받음.
			// ARP messgae target's mac보고 sender는 table 채움.
			// ip, mac변수에 setting -> Dlg에서 get해서 화면에 출력
			addCacheTable(input);
			
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
			frame.target_mac.addr[i] = input[i];
		}
	}

	public _IP_ADDR GetIpSrcAddress() {
		return frame.sender_ip;
	}

	public _IP_ADDR GetIpDstAddress() {
		return frame.target_ip;
	}

	public void SetIpSrcAddress(byte[] input) {
		for (int i = 0; i < 4; i++) {
			frame.sender_ip.addr[i] = input[i];
		}
	}

	public void SetIpDstAddress(byte[] input) {
		for (int i = 0; i < 4; i++) {
			frame.target_ip.addr[i] = input[i];
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

	public ArrayList<ArrayList<byte[]>> getCacheTable() {
		return this.cacheTable;
	}

	public int getState() {
		return this.state;
	}
}
