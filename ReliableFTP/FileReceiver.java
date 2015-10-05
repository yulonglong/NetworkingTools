// Steven Kester Yuwono
// A0080415

// All numbers below are represented in bytes
// Header Packet design:
// [8   - Checksum]
// [2   - PacketType (char 'H')]
// [255 - DestinationFilepath]
// [8   - CompleteFilesize]

// Body Packet Design:
// [8   - Checksum]
// [2   - PacketType (char 'B')]
// [4   - SequenceNumber]
// [4   - attachedDataSize]
// [xxx - Data]

import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.zip.*;
import java.io.*;

public class FileReceiver {
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes, int len) {
	    char[] hexChars = new char[len * 2];
	    for ( int j = 0; j < len; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	public static int s_packetLength = 1000;
	public static int s_checksumLength = 8;
	public static int s_packetTypeLength = 2;
	// Filename max 255 characters (UTF-8 is 1 byte per character, hence 255 bytes)
	public static int s_destinationFilepathLength = 255; 
	public static int s_completeFilesizeLength = 8;
	public static int s_attachedDataSizeLength = 4;
	public static int s_sequenceNumberLength = 4;
	public static int s_dataLength = s_packetLength-s_checksumLength-s_packetTypeLength-s_attachedDataSizeLength-s_sequenceNumberLength;
	public static int s_headerLength = s_checksumLength + s_packetTypeLength + s_destinationFilepathLength + s_completeFilesizeLength;
	public static int s_ackLength = s_checksumLength + s_sequenceNumberLength;

	private int port = -1;
	private DatagramSocket sk = null;
	private CRC32 crc = null;
	private String destFilename;
	private long destFilesize;
	private byte[] incomingPacket;
	private ByteBuffer b;
	private BufferedOutputStream bos;
	private long totalBytesReceived = 0;
	private int counter = 1;

	private boolean[] sequenceNumberReceived;
	private int maxSequenceNumber;

	FileReceiver(int _port) throws Exception {
		incomingPacket = new byte[s_packetLength];
		crc = new CRC32();
		port = _port;
		sk = new DatagramSocket(port);
		b = ByteBuffer.wrap(incomingPacket);

		maxSequenceNumber = 50000;
		sequenceNumberReceived = new boolean[maxSequenceNumber];
	}

	public void waitingToReceivePacket() throws Exception{
		
		DatagramPacket pkt = new DatagramPacket(incomingPacket, s_packetLength);
		
		// Receive first packet with the destination filename
		while(true) {
			pkt.setLength(s_packetLength);
			sk.receive(pkt);
			SocketAddress packetSocketAddress = pkt.getSocketAddress();

			if ((pkt.getLength() != s_packetLength) && (pkt.getLength() != s_headerLength)){
				// Packet somehow too short, corrupted
				System.out.println("Invalid pkt length");
				continue;
			}

			b.rewind();
			long chksum = b.getLong();
			char type = b.getChar();
			crc.reset();
			crc.update(incomingPacket, 8, pkt.getLength()-8);
			if (crc.getValue() != chksum) {
				// Packet Corrupted
				System.out.println("Pkt corrupt");
			}
			else {
				if (type == 'H') {
					getHeader();
				}
				else if (type == 'B'){
					getBody(packetSocketAddress);
					if (totalBytesReceived >= destFilesize) {
						System.out.println(totalBytesReceived);
						bos.close();
						break;
					}
				}
			}
		}
	}

	public void getHeader() throws Exception {
		// Get filename
		byte[] destinationFilepathByteArray = new byte[s_destinationFilepathLength];
		b.get(destinationFilepathByteArray, 0, s_destinationFilepathLength);
		destFilename = new String(destinationFilepathByteArray).trim();
		destFilesize = b.getLong();
		System.out.println("Pkt " + destFilename + " - " + destFilesize + " bytes");
		
		// DatagramPacket ack = new DatagramPacket(new byte[0], 0, 0, pkt.getSocketAddress());
		// sk.send(ack);

		try {
			bos = new BufferedOutputStream(new FileOutputStream(destFilename));
		}
		catch (Exception e) {
			System.out.println("Failed in writing file!");
			return;
		}
	}

	public void getBody(SocketAddress packetSocketAddress) throws Exception {
		int sequenceNumber = b.getInt();
		int dataRcvdSize = b.getInt();

		if (sequenceNumberReceived[sequenceNumber]) {
			sendAck(packetSocketAddress, sequenceNumber);
			return;
		}

		byte[] dataRcvd = new byte[dataRcvdSize];
		b.get(dataRcvd,0,dataRcvdSize);
		bos.write(dataRcvd);
		bos.flush();

		totalBytesReceived += dataRcvdSize;
		sequenceNumberReceived[sequenceNumber] = true;
		System.out.println("Pkt received " + sequenceNumber + " data size " + dataRcvdSize + " - " + totalBytesReceived + "/" + destFilesize);

		sendAck(packetSocketAddress, sequenceNumber);
	}


	public void sendAck(SocketAddress packetSocketAddress, int sequenceNumber) throws Exception {
		// Set up ACK packet
		byte[] ackByteArray = new byte[s_ackLength];
		ByteBuffer ackBuffer = ByteBuffer.wrap(ackByteArray);
		ackBuffer.putLong(0); // checksum reserver
		ackBuffer.putInt(sequenceNumber);
		crc.reset();
		crc.update(ackByteArray, 8, ackByteArray.length-8);
		long chksum = crc.getValue();
		ackBuffer.rewind();
		ackBuffer.putLong(chksum);
		DatagramPacket ack = new DatagramPacket(ackByteArray, ackByteArray.length, packetSocketAddress);
		sk.send(ack);
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: FileReceiver <port>");
			System.exit(-1);
		}

		try {
			FileReceiver fileReceiver = new FileReceiver(Integer.parseInt(args[0]));
			fileReceiver.waitingToReceivePacket();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
