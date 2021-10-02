import java.util.ArrayList;
   
public class ChatAppLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	private byte[] fragBytes;
    private int fragCount = 0;
    
	private class _CHAT_APP {
		byte[] capp_totlen;
		byte capp_type;
		byte capp_unused;
		byte[] capp_data;

		public _CHAT_APP() {
			this.capp_totlen = new byte[2];
			this.capp_type = 0x00;
			this.capp_unused = 0x00;
			this.capp_data = null;
		}
	}

	_CHAT_APP m_sHeader = new _CHAT_APP();

	public ChatAppLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		ResetHeader();
	}

	public void ResetHeader() {
		for (int i = 0; i < 2; i++) {
			m_sHeader.capp_totlen[i] = (byte) 0x00;
			m_sHeader.capp_type = 0x00;
			m_sHeader.capp_unused = 0x00;
		}
		m_sHeader.capp_data = null;
	}

	public byte[] ObjToByte(_CHAT_APP Header, byte[] input, int length) {
		byte[] buf = new byte[length + 4];
		
		buf[0] = Header.capp_totlen[0];
		buf[1] = Header.capp_totlen[1];
		buf[2] = Header.capp_type;
		buf[3] = Header.capp_unused;
		

		for (int i = 0; i < length; i++)
			buf[4 + i] = input[i];

		return buf;
	}
	private void fragSend(byte[] input, int length) {
        byte[] bytes = new byte[10];
        int i = 0;
        m_sHeader.capp_totlen = intToByte2(length);
        m_sHeader.capp_type = (byte) (0x01);

        System.arraycopy(input, 0, bytes, 0, 10);
        bytes = ObjToByte(m_sHeader, bytes, 10);
        this.GetUnderLayer().Send(bytes, bytes.length);

        int maxLen = length / 1456;
        m_sHeader.capp_type = (byte) (0x02);
        m_sHeader.capp_totlen = intToByte2(1456);
        
        for( i = 1; i<maxLen; i++) {
        	if(i+1<maxLen && length%1456 == 0) {
        		m_sHeader.capp_type = (byte) (0x03);
        	}
        	System.arraycopy(input,1456 * i, bytes, 0, 1456);
        	bytes = ObjToByte(m_sHeader, bytes, 1456);
        	this.GetUnderLayer().Send(bytes, bytes.length);
        }
        
        if (length % 1456 != 0) {
            m_sHeader.capp_type = (byte) (0x03);
            m_sHeader.capp_totlen = intToByte2(length%1456);
            bytes = new byte[length % 1456];
            System.arraycopy(input, length - (length%1456), bytes, 0, length % 1456);
            bytes = ObjToByte(m_sHeader, bytes, bytes.length);
            this.GetUnderLayer().Send(bytes, bytes.length);
        }
    }

	public boolean Send(byte[] input, int length) {
		
		byte[] bytes;
        m_sHeader.capp_totlen = intToByte2(length);
        m_sHeader.capp_type = (byte) (0x00);
        m_sHeader.capp_data = input;
 
        if( length > 1456 ) {
        	fragSend(input, length);
        } else {
        	bytes = ObjToByte(m_sHeader, input, input.length);
        	this.GetUnderLayer().Send(bytes, bytes.length);
        }
		return true;
	}

	public byte[] RemoveCappHeader(byte[] input, int length) {
		byte[] buf = new byte[4];
		byte[] input2 = new byte[input.length-4];
		
		for (int i =0; i<4; i++) {
			buf[i] = input[i];
		}
		for (int i=0; i<input.length-4; i++) {
			input2[i] = input[i+4];
		}
		return input2;
	}

	public synchronized boolean Receive(byte[] input) {

		byte[] data, tempBytes;
        int tempType = 0;
        
        tempType |= (byte) (input[2] & 0xFF);
        
        if(tempType == 0) {
        	data = RemoveCappHeader(input, input.length);
        	this.GetUpperLayer(0).Receive(data);
        }
        else{
        	if(tempType == 1) {
        		int size = byte2ToInt(input[0], input[1]);
        		fragBytes = new byte[size];
        		fragCount = 1;
        		tempBytes = RemoveCappHeader(input, input.length);
        		System.arraycopy(tempBytes, 0, fragBytes, 0, 10);
        	}
        	else {
        		tempBytes = RemoveCappHeader(input, input.length);
        		System.arraycopy(tempBytes, 0, fragBytes, (fragCount++)*10, byte2ToInt(input[0], input[1]));
        		if(tempType == 3) {
        			this.GetUpperLayer(0).Receive(fragBytes);
        		}
        	}
        }

		return true;
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
		// nUpperLayerCount++;

	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}

}

