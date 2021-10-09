
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jnetpcap.PcapIf;

public class ChatFileDlg extends JFrame implements BaseLayer {

	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	private static LayerManager m_LayerMgr = new LayerManager();

	private JTextField ChattingWrite;
	private JTextField PathWrite;
	private JTextField dstIpWrite;
	private JTextField proxyDeviceWrite;
	private JTextField proxyIpWrite;
	private JTextField proxyMacWrite; 
	private JTextField GArpAddressWrite;
	
	Container contentPane;

	JTextArea ChattingArea;
	JTextArea fileArea;
	JTextArea srcMacAddress;
	JTextArea IpAddress;
	JTextArea cacheArea;
	JTextArea proxyArpArea;

	JLabel lblsrc;
	JLabel lbldst;
	JLabel dstIpLabel;
	JLabel proxyDevice;
	JLabel proxyIp;
	JLabel proxyMac;
	JLabel GArpAddress;
	
	JButton Setting_Button;
	JButton Chat_send_Button;
	JButton File_send_Button;
	JButton openFileButton;
	JButton itemDeleteButton;
	JButton allDeleteButton;
	JButton dstIpSendButton; 
	JButton proxyAddButton;
	JButton proxyDeleteButton;
	JButton GArpSendButton;
	
	static JComboBox<String> NICComboBox;

	int adapterNumber = 0;

	String Text;
	JProgressBar progressBar;

	File file;

	public static void main(String[] args) {

		////////////////

		m_LayerMgr.AddLayer(new NILayer("NI"));
		m_LayerMgr.AddLayer(new EthernetLayer("Ethernet"));
		m_LayerMgr.AddLayer(new ChatAppLayer("ChatApp"));
		m_LayerMgr.AddLayer(new FileAppLayer("FileApp"));
		m_LayerMgr.AddLayer(new ChatFileDlg("GUI"));

		m_LayerMgr.ConnectLayers(" NI ( *Ethernet ( *ChatApp ( *GUI ) *FileApp ( +GUI ) ) )");
		///////////////////
	}

	class setAddressListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {

			if (e.getSource() == Setting_Button) {

				if (Setting_Button.getText() == "Reset") {
					srcMacAddress.setText("");
					IpAddress.setText("");
					Setting_Button.setText("Setting");
					srcMacAddress.setEnabled(true);
					IpAddress.setEnabled(true);
				} else {
					byte[] srcAddress = new byte[6];
					byte[] dstAddress = new byte[6];

					String src = srcMacAddress.getText();
					String dst = IpAddress.getText();
					System.out.println("srcMacAddress : "+ src);
					System.out.println("dstMacAddress : "+ dst);
					String[] byte_src = src.split("-");
					for (int i = 0; i < 6; i++) {
						srcAddress[i] = (byte) Integer.parseInt(byte_src[i], 16);
					}

					String[] byte_dst = dst.split("-");
					for (int i = 0; i < 6; i++) {
						dstAddress[i] = (byte) Integer.parseInt(byte_dst[i], 16);
					}

					((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).SetEnetSrcAddress(srcAddress);
					((EthernetLayer) m_LayerMgr.GetLayer("Ethernet")).SetEnetDstAddress(dstAddress);

					((NILayer) m_LayerMgr.GetLayer("NI")).SetAdapterNumber(adapterNumber);

					Setting_Button.setText("Reset");
					IpAddress.setEnabled(false);
					srcMacAddress.setEnabled(false);

				}
			}

			if (e.getSource() == Chat_send_Button) {
				if (Setting_Button.getText() == "Reset") {
					for (int i = 0; i < 10; i++) {
						String input = ChattingWrite.getText();
						ChattingArea.append("[SEND] : " + input + "\n");
						byte[] bytes = input.getBytes();
						m_LayerMgr.GetLayer("ChatApp").Send(bytes, bytes.length);
						if (m_LayerMgr.GetLayer("GUI").Receive()) {
							input = Text;
							ChattingArea.append("[RECV] : " + input + "\n");
							continue;
						}
						break;
					}
				} else {
					JOptionPane.showMessageDialog(null, "Address Configuration Error");
				}
			}
			if (e.getSource() == openFileButton) {
				FileNameExtensionFilter filter = new FileNameExtensionFilter("txt","txt");
				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(filter);
				int ret = chooser.showOpenDialog(null);
				if(ret == JFileChooser.APPROVE_OPTION) {
					String filePath = chooser.getSelectedFile().getPath();
					fileArea.setText(filePath);
					File_send_Button.setEnabled(true);
					file = chooser.getSelectedFile();
					
				}
				
			}
			if (e.getSource() == File_send_Button) {
				((FileAppLayer)m_LayerMgr.GetLayer("FileApp")).setAndStartSendFile();
				File_send_Button.setEnabled(false);
			}
		}
	}

	public ChatFileDlg(String pName) {
		pLayerName = pName;

		setTitle("Packet_Send_Test");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(250, 250, 1000, 700);
		contentPane = new JPanel();
		((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		//ARP Cache panel 
		JPanel arpCachePanel = new JPanel();
		arpCachePanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "ARP Cache",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		arpCachePanel.setBounds(10,5, 370, 371);
		contentPane.add(arpCachePanel);
		arpCachePanel.setLayout(null);
		
		JPanel arpCacheEditorPanel = new JPanel();
		arpCacheEditorPanel.setBounds(10, 15, 350, 230);
		arpCachePanel.add(arpCacheEditorPanel);
		arpCacheEditorPanel.setLayout(null);
		
		cacheArea = new JTextArea();
		cacheArea.setEditable(false);
		cacheArea.setBounds(0, 0, 350, 220);
		arpCacheEditorPanel.add(cacheArea);// chatting edit
		
		itemDeleteButton = new JButton("Item Delete");
		itemDeleteButton.setBounds(70, 250, 100, 30);
		
		allDeleteButton = new JButton("All Delete");
		allDeleteButton.setBounds(200, 250, 100, 30);
		/*add Action Listener for delete button*/
		arpCachePanel.add(itemDeleteButton);
		arpCachePanel.add(allDeleteButton);
		
		dstIpLabel = new JLabel("IP_Addr");
		dstIpLabel.setBounds(15, 300, 100, 20);
		arpCachePanel.add(dstIpLabel);
		
		dstIpWrite = new JTextField();
		dstIpWrite.setBounds(70, 300, 200, 20);// 249
		arpCachePanel.add(dstIpWrite);
		dstIpWrite.setColumns(10);// target ip address writing area
		dstIpSendButton = new JButton("Send");
		dstIpSendButton.setBounds(285,300,70,20);
		arpCachePanel.add(dstIpSendButton);
		
		// chatting panel
		JPanel chattingPanel = new JPanel();
		chattingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "chatting",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		chattingPanel.setBounds(10, 380, 360, 276);
		contentPane.add(chattingPanel);
		chattingPanel.setLayout(null);

		JPanel chattingEditorPanel = new JPanel();// chatting write panel
		chattingEditorPanel.setBounds(10, 15, 340, 180);
		chattingPanel.add(chattingEditorPanel);
		chattingEditorPanel.setLayout(null);

		ChattingArea = new JTextArea();
		ChattingArea.setEditable(false);
		ChattingArea.setBounds(0, 0, 340, 210);
		chattingEditorPanel.add(ChattingArea);// chatting edit

		JPanel chattingInputPanel = new JPanel();// chatting write panel
		chattingInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		chattingInputPanel.setBounds(10, 230, 250, 20);
		chattingPanel.add(chattingInputPanel);
		chattingInputPanel.setLayout(null);

		ChattingWrite = new JTextField();
		ChattingWrite.setBounds(2, 2, 250, 20);// 249
		chattingInputPanel.add(ChattingWrite);
		ChattingWrite.setColumns(10);// writing area

		// file panel
		JPanel fileTransferPanel = new JPanel();// file panel
		fileTransferPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "file transfer",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		fileTransferPanel.setBounds(380, 380, 350, 90);
		contentPane.add(fileTransferPanel);
		fileTransferPanel.setLayout(null);

		JPanel fileEditorPanel = new JPanel();// chatting write panel
		fileEditorPanel.setBounds(10, 20, 330, 60);
		fileTransferPanel.add(fileEditorPanel);
		fileEditorPanel.setLayout(null);

		fileArea = new JTextArea();
		fileArea.setEditable(false);
		fileArea.setBounds(0, 5, 250, 20);
		fileEditorPanel.add(fileArea);// chatting edit

		openFileButton = new JButton("File...");
		openFileButton.setBounds(260, 5, 70, 20);
		openFileButton.addActionListener(new setAddressListener());
		fileEditorPanel.add(openFileButton);

		this.progressBar = new JProgressBar(0, 100);
		this.progressBar.setBounds(0, 40, 250, 20);
		this.progressBar.setStringPainted(true);
		fileEditorPanel.add(this.progressBar);

		File_send_Button = new JButton("전송");
		File_send_Button.setBounds(260, 40, 70, 20);
		fileEditorPanel.add(File_send_Button);
		File_send_Button.addActionListener(new setAddressListener());
		File_send_Button.setEnabled(false);
		
		// proxy arp entry panel 
		JPanel proxyArpPanel = new JPanel();
		proxyArpPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Proxy Arp Entry",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		proxyArpPanel.setBounds(380, 5, 350, 370);
		contentPane.add(proxyArpPanel);
		proxyArpPanel.setLayout(null);
		
		JPanel proxyEditorPanel = new JPanel();// proxy editor panel
		proxyEditorPanel.setBounds(5, 15, 330, 160);
		proxyArpPanel.add(proxyEditorPanel);
		proxyEditorPanel.setLayout(null);
		
		proxyArpArea = new JTextArea();
		proxyArpArea.setEditable(false);
		proxyArpArea.setBounds(5, 5, 420, 150);
		proxyEditorPanel.add(proxyArpArea);// proxy arp entry
		
		JPanel proxyInputPanel = new JPanel();
		proxyInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED,null,null,null,null));
		proxyInputPanel.setBounds(10,200,320,150);
		proxyInputPanel.setLayout(null);
		proxyArpPanel.add(proxyInputPanel);
		
		proxyDevice= new JLabel("Device");
		proxyDevice.setBounds(20, 10, 60, 20);
		proxyInputPanel.add(proxyDevice);
		
		proxyIp = new JLabel("IP 주소");
		proxyIp.setBounds(20,40,60,20);
		proxyInputPanel.add(proxyIp);
		
		proxyMac = new JLabel("Mac 주소");
		proxyMac.setBounds(20,70,60,20);
		proxyInputPanel.add(proxyMac);
		
		proxyDeviceWrite = new JTextField();
		proxyDeviceWrite.setBounds(100,10,200,20);
		proxyInputPanel.add(proxyDeviceWrite);
		
		proxyIpWrite = new JTextField();
		proxyIpWrite.setBounds(100,40,200,20);
		proxyInputPanel.add(proxyIpWrite);
		
		proxyMacWrite = new JTextField();
		proxyMacWrite.setBounds(100,70,200,20);
		proxyInputPanel.add(proxyMacWrite);
		
		proxyAddButton = new JButton("Add");
		proxyAddButton.setBounds(70,100,80,30);
		proxyDeleteButton = new JButton("Delete");
		proxyDeleteButton.setBounds(180,100,80,30);
		proxyInputPanel.add(proxyAddButton);
		proxyInputPanel.add(proxyDeleteButton);
		
		// gratuitous panel 
		JPanel gratuitousPanel = new JPanel();
		gratuitousPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Gratuitous ARP",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		gratuitousPanel.setBounds(730, 5, 250, 200);
		contentPane.add(gratuitousPanel);
		gratuitousPanel.setLayout(null);
		
		GArpAddress = new JLabel("H/W address");
		GArpAddress.setBounds(10,30,100,10);
		GArpAddressWrite = new JTextField();
		GArpAddressWrite.setBounds(20,60,200,20);
		GArpSendButton = new JButton("전송");
		GArpSendButton.setBounds(70,100,100,30);
		gratuitousPanel.add(GArpAddress);
		gratuitousPanel.add(GArpAddressWrite);
		gratuitousPanel.add(GArpSendButton);
		
		// setting panel
		JPanel settingPanel = new JPanel();
		settingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "setting",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		settingPanel.setBounds(730, 380, 250, 270);
		contentPane.add(settingPanel);
		settingPanel.setLayout(null);

		JPanel sourceAddressPanel = new JPanel();
		sourceAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		sourceAddressPanel.setBounds(10, 96, 170, 20);
		settingPanel.add(sourceAddressPanel);
		sourceAddressPanel.setLayout(null);

		lblsrc = new JLabel("Mac Address");
		lblsrc.setBounds(10, 75, 170, 20);
		settingPanel.add(lblsrc);

		srcMacAddress = new JTextArea();
		srcMacAddress.setBounds(2, 2, 170, 20);
		sourceAddressPanel.add(srcMacAddress);// src address

		JPanel IpPanel = new JPanel();
		IpPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		IpPanel.setBounds(10, 150, 170, 20);
		settingPanel.add(IpPanel);
		IpPanel.setLayout(null);

		lbldst = new JLabel("IP Address");
		lbldst.setBounds(10, 125, 190, 20);
		settingPanel.add(lbldst);

		IpAddress = new JTextArea();
		IpAddress.setBounds(2, 2, 170, 20);
		IpPanel.add(IpAddress);// 자신의 ip address

		JLabel NICLabel = new JLabel("Select NIC");
		NICLabel.setBounds(10, 20, 170, 20);
		settingPanel.add(NICLabel);

		NICComboBox = new JComboBox();
		NICComboBox.setBounds(10, 49, 170, 20);
		settingPanel.add(NICComboBox);

		for (int i = 0; ((NILayer) m_LayerMgr.GetLayer("NI")).getAdapterList().size() > i; i++) {
			NICComboBox.addItem(((NILayer) m_LayerMgr.GetLayer("NI")).GetAdapterObject(i).getDescription());
		}

		NICComboBox.addActionListener(new ActionListener() { // Event Listener

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub

				adapterNumber = NICComboBox.getSelectedIndex();
				System.out.println("Index: " + adapterNumber);
				try {
					srcMacAddress.setText("");
					srcMacAddress.append(get_MacAddress(((NILayer) m_LayerMgr.GetLayer("NI"))
							.GetAdapterObject(adapterNumber).getHardwareAddress()));

				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});

		try {// Init MAC Address
			srcMacAddress.append(get_MacAddress(
					((NILayer) m_LayerMgr.GetLayer("NI")).GetAdapterObject(adapterNumber).getHardwareAddress()));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		;

		Setting_Button = new JButton("Setting");// setting
		Setting_Button.setBounds(80, 180, 100, 20);
		Setting_Button.addActionListener(new setAddressListener());
		settingPanel.add(Setting_Button);// setting

		Chat_send_Button = new JButton("Send");
		Chat_send_Button.setBounds(270, 230, 80, 20);
		Chat_send_Button.addActionListener(new setAddressListener());
		chattingPanel.add(Chat_send_Button);// chatting send button

		setVisible(true);

	}
	
	public File getFile() {
		return this.file;
	}

	public String get_MacAddress(byte[] byte_MacAddress) {

		String MacAddress = "";
		for (int i = 0; i < 6; i++) {
			MacAddress += String.format("%02X%s", byte_MacAddress[i], (i < MacAddress.length() - 1) ? "" : "");
			if (i != 5) {
				MacAddress += "-";
			}
		}

		System.out.println("present MAC address: " + MacAddress);
		return MacAddress;
	}

	public boolean Receive(byte[] input) {
		if (input != null) {
			byte[] data = input;
			Text = new String(data);
			ChattingArea.append("[RECV] : " + Text + "\n");
			return false;
		}
		return false;
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
}
