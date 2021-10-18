import java.io.*;
import java.util.ArrayList;

public class FileAppLayer implements BaseLayer {
    private int count = 0;
    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
    private String fileName; // 파일 이름
    private int receivedLength = 0; // 수신한 데이터의 크기
    private int targetLength = 0; // 수신해야하는 파일의 총 크기

    private File file; // 저장할 파일
    private ArrayList<byte[]> fileByteList; // 수신한 파일 프레임(정렬 전)
    private ArrayList<byte[]> fileSortList; // 수신한 파일을 정렬 하는데 사용하는 리스트

    public FileAppLayer(String pName) {
        // TODO Auto-generated constructor stub
        pLayerName = pName;
        fileByteList = new ArrayList();
    }
    public class _FAPP_HEADER {
        byte[] fapp_totlen;
        byte[] fapp_type;
        byte fapp_msg_type;
        byte fapp_unused;
        byte[] fapp_seq_num;
        byte[] fapp_data;

        public _FAPP_HEADER() {
            this.fapp_totlen = new byte[4];
            this.fapp_type = new byte[2];
            this.fapp_msg_type = 0x00;
            this.fapp_unused = 0x00;
            this.fapp_seq_num = new byte[4];
            this.fapp_data = null;
        }
    }

    _FAPP_HEADER m_sHeader = new _FAPP_HEADER();

    private void setFragmentation(int type){
        if(type == 0) { // 처음
            m_sHeader.fapp_type[0] = (byte) 0x0;
            m_sHeader.fapp_type[1] = (byte) 0x0;
        }
        else if(type == 1) { // 중간
            m_sHeader.fapp_type[0] = (byte) 0x0;
            m_sHeader.fapp_type[1] = (byte) 0x1;
        }
        else if(type == 2) { // 끝
            m_sHeader.fapp_type[0] = (byte) 0x0;
            m_sHeader.fapp_type[1] = (byte) 0x2;
        }
    }

    public void setFileMsgType(int type) { // fapp_msg_type 값을 설정
        m_sHeader.fapp_msg_type = (byte) type;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    } // 파일이름 설정자

    // 파일 크기 설정자
    public void setFileSize(int fileSize) {
        m_sHeader.fapp_totlen[0] = (byte)(0xff&(fileSize >> 24));
        m_sHeader.fapp_totlen[1] = (byte)(0xff&(fileSize >> 16));
        m_sHeader.fapp_totlen[2] = (byte)(0xff&(fileSize >> 8));
        m_sHeader.fapp_totlen[3] = (byte)(0xff & fileSize);
    }

    public int calcSeqNum(byte[] input) { // 몇 번째 Frame인지 계산(Frame은 0번부터 시작)
        int seqNum = 0;
        seqNum += (input[8] & 0xff) << 24;
        seqNum += (input[9] & 0xff) << 16;
        seqNum += (input[10] & 0xff) << 8;
        seqNum += (input[11] & 0xff);

        return seqNum;
    }

    public int calcFileFullLength(byte[] input) {
        int fullLength = 0;
        fullLength += (input[0] & 0xff) << 24;
        fullLength += (input[1] & 0xff) << 16;
        fullLength += (input[2] & 0xff) << 8;
        fullLength += (input[3] & 0xff);
        return fullLength;
    }


    public boolean fileInfoSend(byte[] input, int length) { // 파일 정보 송신 함수
        this.setFileMsgType(0); // 파일 정보 송신임을 나타냄
        this.Send(input, length); // 파일 정보 송신

        return true;
    }

    // 프레임을 다 받았는지 확인 후, 모두 정확히 수신했으면 정렬을 진행하는 함수
    public boolean sortFileList(int lastFrameNumber) {
        // 모든 프레임을 받았는지 확인
        if((fileByteList.size() - 1 != lastFrameNumber) || (receivedLength != targetLength)) {
            ((ARPDlg)this.GetUpperLayer(0)).ChattingArea.append("파일 수신 실패\n");
            return false;
        }

        // ArrayList에 SeqNum을 Index로 가지도록 삽입하여 정렬 진행
        fileSortList = new ArrayList();
        for(int checkSeqNum = 0; checkSeqNum < (lastFrameNumber + 1); ++checkSeqNum) {
            byte[] checkByteArray = fileByteList.remove(0);
            int arraySeqNum = this.calcSeqNum(checkByteArray);
            fileSortList.add(arraySeqNum, checkByteArray);
        }

        return true;
    }

    public void setAndStartSendFile() {
        ARPDlg upperLayer = (ARPDlg) this.GetUpperLayer(0);
        File sendFile = upperLayer.getFile();
        int sendTotalLength; // 보내야하는 총 크기
        int sendedLength; // 현재 보낸 크기
        this.resetSeqNum();

        try (FileInputStream fileInputStream = new FileInputStream(sendFile)) {
            sendedLength = 0;
            BufferedInputStream fileReader = new BufferedInputStream(fileInputStream);
            sendTotalLength = (int)sendFile.length();
            this.setFileSize(sendTotalLength);
            byte[] sendData =new byte[1448];
            ((ARPDlg)this.GetUpperLayer(0)).progressBar.setMaximum(sendTotalLength);
            if(sendTotalLength <= 1448) {
                // 파일 정보 송신
                setFragmentation(0);
                this.setFileMsgType(0);
                this.fileInfoSend(sendFile.getName().getBytes(), sendFile.getName().getBytes().length);

                // 파일 데이터 송신
                this.setFileMsgType(1);
                fileReader.read(sendData);
                this.Send(sendData, sendData.length);
                sendedLength += sendData.length;
                ((ARPDlg)this.GetUpperLayer(0)).progressBar.setValue(sendedLength);
            } else {
                sendedLength = 0;
                // 파일 정보 송신
                this.setFragmentation(0);
                this.setFileMsgType(0);
                this.fileInfoSend(sendFile.getName().getBytes(), sendFile.getName().getBytes().length);

                // 파일 데이터 송신
                this.setFileMsgType(1);
                this.setFragmentation(1);
                while(fileReader.read(sendData) != -1 && (sendedLength + 1448 < sendTotalLength)) {
                    this.Send(sendData, 1448);
                    try {
                        Thread.sleep(4);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    sendedLength += 1448;
                    this.increaseSeqNum();
                    ((ARPDlg)this.GetUpperLayer(0)).progressBar.setValue(sendedLength);
                }

                byte[] getRealDataFrame = new byte[sendTotalLength - sendedLength];
                this.setFragmentation(2);
                fileReader.read(sendData);

                for(int index = 0; index < getRealDataFrame.length; ++index) {
                    getRealDataFrame[index] = sendData[index];
                }

                this.Send(getRealDataFrame, getRealDataFrame.length);
                sendedLength += getRealDataFrame.length;
                count = 0;
                ((ARPDlg)this.GetUpperLayer(0)).progressBar.setValue(sendedLength);
            }
            fileInputStream.close();
            fileReader.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] RemoveCappHeader(byte[] input, int length) { // FileApp의 Header를 제거해주는 함수
        byte[] buf = new byte[length - 12];
        for(int dataIndex = 0; dataIndex < length - 12; ++dataIndex)
            buf[dataIndex] = input[12 + dataIndex];

        return buf;
    }

    public synchronized boolean Receive(byte[] input) { // 데이터를 수신 처리 함수
        byte[] data;
        if(checkReceiveFileInfo(input)) { // 파일의 정보를 받은 경우
            data = RemoveCappHeader(input, input.length); // Header없애기
            String fileName = new String(data);
            fileName = fileName.trim();
            targetLength = calcFileFullLength(input); // 받아야 하는 총 크기 초기화
            file = new File("./" + fileName); //받는 경로..

            // Progressbar 초기화
            ((ARPDlg)this.GetUpperLayer(0)).progressBar.setMinimum(0);
            ((ARPDlg)this.GetUpperLayer(0)).progressBar.setMaximum(targetLength);
            ((ARPDlg)this.GetUpperLayer(0)).progressBar.setValue(0);

            // 받은 크기) 초기화
            receivedLength = 0;
        } else {
            // 단편화를 하지 않은 데이터를 받은 경우
            if (checkNoFragmentation(input)) {
                data = RemoveCappHeader(input, input.length);
                fileByteList.add(this.calcSeqNum(input), data);
                try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    fileOutputStream.write(fileByteList.get(0));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // 단편화를 진행한 데이터를 받은 경우

                // 데이터 프레임 수신
                fileByteList.add(input);
                receivedLength += (input.length - 12); // 헤더의 길이는 제외

                // 마지막 프레임 수신
                if(checkLastDataFrame(input)) {
                    int lastFrameNumber = this.calcSeqNum(input);

                    if(sortFileList(lastFrameNumber)) {
                        try(FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                            for (int frameCount = 0; frameCount < (lastFrameNumber + 1); ++frameCount) {
                                data = RemoveCappHeader(fileSortList.get(frameCount), fileSortList.get(frameCount).length);
                                fileOutputStream.write(data);
                            }
                            ((ARPDlg)this.GetUpperLayer(0)).ChattingArea.append("파일 수신 및 생성 완료\n");
                            fileByteList = new ArrayList();
                        } catch (FileNotFoundException e) {
                            ((ARPDlg)this.GetUpperLayer(0)).ChattingArea.append("파일 수신 실패\n");
                            e.printStackTrace();
                        } catch (IOException e) {
                            ((ARPDlg)this.GetUpperLayer(0)).ChattingArea.append("파일 수신 실패\n");
                            e.printStackTrace();
                        }
                    }
                }
                ((ARPDlg)this.GetUpperLayer(0)).progressBar.setValue(receivedLength); // Progressbar 갱신
            }
        }

        return true;
    }

    public void resetSeqNum() {
        this.m_sHeader.fapp_seq_num[0] = (byte)0x0;
        this.m_sHeader.fapp_seq_num[1] = (byte)0x0;
        this.m_sHeader.fapp_seq_num[2] = (byte)0x0;
        this.m_sHeader.fapp_seq_num[3] = (byte)0x0;
    }

    public void increaseSeqNum() { // Frame 번호 증가 함수(Send시 Frame 번호 값 변경)
        if((this.m_sHeader.fapp_seq_num[3] & 0xff) < 255)
            ++this.m_sHeader.fapp_seq_num[3];
        else if((this.m_sHeader.fapp_seq_num[2] & 0xff) < 255) {
            ++this.m_sHeader.fapp_seq_num[2];
            this.m_sHeader.fapp_seq_num[3] = 0;
        } else if((this.m_sHeader.fapp_seq_num[1] & 0xff) < 255) {
            ++this.m_sHeader.fapp_seq_num[1];
            this.m_sHeader.fapp_seq_num[2] = 0;
            this.m_sHeader.fapp_seq_num[3] = 0;
        } else if((this.m_sHeader.fapp_seq_num[0] & 0xff) < 255) {
            ++this.m_sHeader.fapp_seq_num[0];
            this.m_sHeader.fapp_seq_num[1] = 0;
            this.m_sHeader.fapp_seq_num[2] = 0;
            this.m_sHeader.fapp_seq_num[3] = 0;
        }
    }

    public boolean Send(byte[] input, int length) { // 데이터 송신 함수
    	System.out.println("file app send");
        byte[] bytes = this.ObjToByte(m_sHeader, input, length);
        ((EthernetLayer)this.GetUnderLayer()).fileSend(bytes, length + 12);
        return true;
    }

    private byte[] ObjToByte(_FAPP_HEADER m_sHeader, byte[] input, int length) {
        byte[] buf = new byte[length + 12];
        buf[0] = m_sHeader.fapp_totlen[0];
        buf[1] = m_sHeader.fapp_totlen[1];
        buf[2] = m_sHeader.fapp_totlen[2];
        buf[3] = m_sHeader.fapp_totlen[3];
        buf[4] = m_sHeader.fapp_type[0];
        buf[5] = m_sHeader.fapp_type[1];
        buf[6] = m_sHeader.fapp_msg_type;
        buf[7] = m_sHeader.fapp_unused;
        buf[8] = m_sHeader.fapp_seq_num[0];
        buf[9] = m_sHeader.fapp_seq_num[1];
        buf[10] = m_sHeader.fapp_seq_num[2];
        buf[11] = m_sHeader.fapp_seq_num[3];

        for(int dataIndex = 0; dataIndex < length; ++dataIndex)
            buf[12 + dataIndex] = input[dataIndex];

        return buf;
    }
    public boolean checkReceiveFileInfo(byte[] input) {
        if(input[6] == (byte)0x00)
            return true;

        return false;
    }
    public boolean checkLastDataFrame(byte[] input) { // 마지막 Frame인지 확인
        if(input[4] == (byte) 0x0 && input[5] == (byte)0x0)
            return true;
        else if(input[4] == (byte) 0x0 && input[5] == (byte)0x2)
            return true;
        else
            return false;
    }

    public boolean checkNoFragmentation(byte[] input) { // File 데이터가 단편화를 진행하지 않았는지 검사하는 함수
        if(input[4] == (byte) 0x00 && input[5] == (byte)0x0)
            return true;

        return false;
    }



    @Override
    public String GetLayerName() {
        return pLayerName;
    }

    @Override
    public BaseLayer GetUnderLayer() {
        if(p_UnderLayer == null)
            return null;
        return p_UnderLayer;
    }

    @Override
    public BaseLayer GetUpperLayer(int nindex) {
        if(nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
            return null;
        return p_aUpperLayer.get(nindex);
    }

    @Override
    public void SetUnderLayer(BaseLayer pUnderLayer) {
        if(pUnderLayer == null)
            return;
        this.p_UnderLayer = pUnderLayer;
    }

    @Override
    public void SetUpperLayer(BaseLayer pUpperLayer) {
        if(pUpperLayer == null)
            return;
        this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
    }

    @Override
    public void SetUpperUnderLayer(BaseLayer pUULayer) {
        this.SetUpperLayer(pUULayer);
        pUULayer.SetUnderLayer(this);
    }
}