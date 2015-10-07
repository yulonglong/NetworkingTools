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

import java.nio.charset.*;
import java.util.zip.*;
import java.io.*;

public class FileSender {
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	private static byte[] toBytes(char[] chars) {
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
		Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return bytes;
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

	private InetSocketAddress addr = null;
	private CRC32 crc = null;
	private DatagramSocket sk = null;
	private DatagramPacket pkt = null;

	private String sourceFilepath;
	private long sourceFileSize;
	private String destFilepath;
	private BufferedInputStream sourceFileStream;

	FileSender(String hostname, int port, String _sourceFilepath, String _destFilepath) throws Exception {
		// Obtain Socket Address
		addr = new InetSocketAddress(hostname,port);
		crc = new CRC32();
		sk = new DatagramSocket();

		sourceFilepath = _sourceFilepath;
		destFilepath = _destFilepath;


		try {
			File sourceFile = new File(sourceFilepath);
			sourceFileSize = sourceFile.length();
			sourceFileStream = new BufferedInputStream(new FileInputStream(sourceFile));
		} catch (Exception e) {
			System.out.println("File not found");
			return;
		}
	}

	public void sendHeader() throws Exception {
		byte[] headerByteArray = new byte[s_headerLength];
		ByteBuffer headerByteBuffer = ByteBuffer.wrap(headerByteArray);

		// First packet, send destination address only
		headerByteBuffer.clear();
		headerByteBuffer.putLong(0); // reserve space for checksum
		headerByteBuffer.putChar('H');

		byte[] byteArrDestFilepath = toBytes(destFilepath.toCharArray());
		headerByteBuffer.put(byteArrDestFilepath);

		headerByteBuffer.put(new byte[s_destinationFilepathLength-byteArrDestFilepath.length]); // append with empty bytes
		headerByteBuffer.putLong(sourceFileSize);
		crc.reset();
		crc.update(headerByteArray, 8, headerByteArray.length-8);
		long chksum = crc.getValue();
		headerByteBuffer.rewind();
		headerByteBuffer.putLong(chksum);
		pkt = new DatagramPacket(headerByteArray, headerByteArray.length, addr);
		sk.send(pkt);

		waitForAck(0, headerByteArray);
	}

	public void sendBody() throws Exception{
		byte[] data = new byte[s_packetLength];
		ByteBuffer b = ByteBuffer.wrap(data);
		int bytesReadFromSource;

		int counter = 1;
		byte[] dataSent = new byte[s_dataLength];
		while ((bytesReadFromSource = sourceFileStream.read(dataSent)) > 0) {
			b.clear();
			b.putLong(0); // reserve space for checksum
			
			b.putChar('B');
			b.putInt(counter);
			b.putInt(bytesReadFromSource);
			b.put(dataSent,0,bytesReadFromSource);
			System.out.println(counter +" Sending " + bytesReadFromSource);
			crc.reset();
			crc.update(data, 8, data.length-8);
			long chksum = crc.getValue();
			b.rewind();
			b.putLong(chksum);
			pkt = new DatagramPacket(data, data.length, addr);
			sk.send(pkt);

			waitForAck(counter, data);
			// If not it will send too fast, the receiver will miss some files
			// Thread.sleep(1);

			counter++;
		}
	}

	public void waitForAck(int expectedCounter, byte[] data) throws Exception {
		boolean isAck = false;
		sk.setSoTimeout(1);

		while (!isAck) {
			try {
				// Check ack packet
				byte[] ackPacket = new byte[s_ackLength];
				ByteBuffer ackBuffer = ByteBuffer.wrap(ackPacket);
				DatagramPacket ackPkt = new DatagramPacket(ackPacket, s_ackLength);
				ackPkt.setLength(s_ackLength);
				sk.receive(ackPkt);

				if (ackPkt.getLength() != s_ackLength) {
					// Packet somehow too short, corrupted
					System.out.println("Invalid ackpkt length");
					continue;
				}

				ackBuffer.rewind();
				long ackChksum = ackBuffer.getLong();
				int ackCounter = ackBuffer.getInt();
				crc.reset();
				crc.update(ackPacket, 8, ackPkt.getLength()-8);
				if (crc.getValue() != ackChksum) {
					// Packet Corrupted
					System.out.println("Ack Pkt corrupt");
				}
				else {
					if (ackCounter == expectedCounter) {
						isAck = true;
						System.out.println("ACK " + ackCounter);
					}
					else if (ackCounter == -1) {
						System.out.println("ACK FINAL");
						isAck = true;
						break;
					}
				}
			}
			catch (SocketTimeoutException e) {
				pkt = new DatagramPacket(data, data.length, addr);
				sk.send(pkt);
				sk.setSoTimeout(1);
			}
		}

	}

	public static void main(String[] args) throws Exception 
	{
		if (args.length != 4) {
			System.err.println("Usage: SimpleUDPSender <host> <port> <source_file> <dest_file>");
			System.exit(-1);
		}

		try {
			FileSender fileSender = new FileSender(args[0],  Integer.parseInt(args[1]), args[2], args[3]);
			fileSender.sendHeader();
			fileSender.sendBody();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
