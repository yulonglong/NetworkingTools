// Steven Kester Yuwono
// A0080415

// All numbers below are represented in bytes
// First packet design:
// [8-Checksum]
// [255-DestinationFilepath]
// [8-Filesize]

import java.net.*;
import java.util.*;
import java.nio.*;

import java.nio.charset.*;
import java.util.zip.*;
import java.io.*;

public class SimpleUDPSender {
	// Filename max 255 characters (UTF-8 is 1 byte per character, hence 255 bytes)
	public static int filepathByteSize = 255;
	public static int headerSize = 8;
	public static int maxPacketSize = 1400;
	public static int maxDataSize = maxPacketSize-headerSize;

	private static byte[] toBytes(char[] chars) {
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
		Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return bytes;
	}

	public static void main(String[] args) throws Exception 
	{
		if (args.length != 4) {
			System.err.println("Usage: SimpleUDPSender <host> <port> <source_file> <dest_file>");
			System.exit(-1);
		}

		String sourceFilepath = args[2];
		byte[] byteArrDestFilepath = toBytes(args[3].toCharArray());

		File sourceFile;
		BufferedInputStream bis;

		try {
			sourceFile = new File(sourceFilepath);
			bis = new BufferedInputStream(new FileInputStream(sourceFile));
		} catch (Exception e) {
			System.out.println("File not found");
			return;
		}


		// Obtain Socket Address
		InetSocketAddress addr = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
		DatagramSocket sk = new DatagramSocket();
		DatagramPacket pkt;
		byte[] data = new byte[maxPacketSize];
		int len;
		ByteBuffer b = ByteBuffer.wrap(data);
		CRC32 crc = new CRC32();

		// First packet, send destination address only
		b.clear();
		b.putLong(0); // reserve space for checksum
		b.put(byteArrDestFilepath);
		b.put(new byte[filepathByteSize-byteArrDestFilepath.length]); // append with empty bytes
		b.putLong(sourceFile.length());
		crc.reset();
		crc.update(data, 8, data.length-8);
		long chksum = crc.getValue();
		b.rewind();
		b.putLong(chksum);
		pkt = new DatagramPacket(data, data.length, addr);
		sk.send(pkt);

		int counter = 1;
		byte[] dataSent = new byte[maxDataSize];
		while ((len = bis.read(dataSent)) > 0) {
			// First packet, send destination address only
			b.clear();
			b.putLong(0); // reserve space for checksum
			b.put(dataSent);
			System.out.println(counter++ +" Sending " + dataSent.length);
			crc.reset();
			crc.update(data, 8, data.length-8);
			chksum = crc.getValue();
			b.rewind();
			b.putLong(chksum);
			pkt = new DatagramPacket(data, data.length, addr);
			sk.send(pkt);
		}
	}

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
}
