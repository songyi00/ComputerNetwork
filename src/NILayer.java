import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;

public class NILayer implements BaseLayer {

	static {
		try {
			// native Library Load
			System.load(new File("jnetpcap.dll").getAbsolutePath());
			System.out.println(new File("jnetpcap.dll").getAbsolutePath());
		} catch (UnsatisfiedLinkError e) {
			System.out.println("Native code library failed to load.\n" + e);
			System.exit(1);
		}
	}

	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	int m_iNumAdapter;
	public Pcap m_AdapterObject;
	public PcapIf device;
	public ArrayList<PcapIf> m_pAdapterList;
	StringBuilder errbuf = new StringBuilder();
	long start;
	
	public NILayer(String pName) {
		// super(pName);
		pLayerName = pName;

		m_pAdapterList = new ArrayList<PcapIf>();
		m_iNumAdapter = 0;
		SetAdapterList();
	}

	public void PacketStartDriver() {
		int snaplen = 64 * 1024; // Capture all packets, no trucation
		int flags = Pcap.MODE_PROMISCUOUS; // capture all packets
		int timeout = 10 * 1000; // 10 seconds in millis
		m_AdapterObject = Pcap.openLive(m_pAdapterList.get(m_iNumAdapter)
				.getName(), snaplen, flags, timeout, errbuf);
	}

	public PcapIf GetAdapterObject(int iIndex) {
		return m_pAdapterList.get(iIndex);
	}

	public void SetAdapterNumber(int iNum) {
		m_iNumAdapter = iNum;
		PacketStartDriver();
		Receive();
	}

	public void SetAdapterList() {
		// Bring All Network Adapter list of Host PC
		int r = Pcap.findAllDevs(m_pAdapterList, errbuf);
		System.out.println("Number of I/F : "+m_pAdapterList.size());
		// Error if there are no Network Adapter
		if (r == Pcap.NOT_OK || m_pAdapterList.isEmpty())
			System.out.println("[Error] Cannot read NIC. Error : "
					+ errbuf.toString());
	}

	public ArrayList<PcapIf> getAdapterList() {
		return m_pAdapterList;
	}

	public boolean Send(byte[] input, int length) {
		ByteBuffer buf = ByteBuffer.wrap(input);
		// start = System.currentTimeMillis();
		if (m_AdapterObject.sendPacket(buf) != Pcap.OK) {
			System.err.println(m_AdapterObject.getErr());
			return false;
		}
		return true;
	}

	public boolean Receive() {
		Receive_Thread thread = new Receive_Thread(m_AdapterObject,
				this.GetUpperLayer(0));
		Thread obj = new Thread(thread);
		obj.start();

		return false;
	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		// TODO Auto-generated method stub
		if (pUnderLayer == null)
			return;
		p_UnderLayer = pUnderLayer;
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

class Receive_Thread implements Runnable {
	byte[] data;
	Pcap AdapterObject;
	BaseLayer UpperLayer;

	public Receive_Thread(Pcap m_AdapterObject, BaseLayer m_UpperLayer) {
		// TODO Auto-generated constructor stub
		AdapterObject = m_AdapterObject;
		UpperLayer = m_UpperLayer;
	}

	@Override
	public void run() {
		while (true) {
			PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {
				public void nextPacket(PcapPacket packet, String user) {
					data = packet.getByteArray(0, packet.size());
					UpperLayer.Receive(data);
				}
			};

			AdapterObject.loop(100000, jpacketHandler, "");
		}
	}
}
