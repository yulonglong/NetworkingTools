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
import java.util.zip.*;
import java.io.*;

public class SimpleUDPReceiver {
	// Filename max 255 characters (UTF-8 is 1 byte per character, hence 255 bytes)
	public static int filepathByteSize = 255;
	public static int headerSize = 8;
	public static int maxPacketSize = 1400;
	public static int maxDataSize = maxPacketSize-headerSize;

	public static void main(String[] args) throws Exception 
	{
		if (args.length != 1) {
			System.err.println("Usage: SimpleUDPReceiver <port>");
			System.exit(-1);
		}
		int port = Integer.parseInt(args[0]);
		DatagramSocket sk = new DatagramSocket(port);
		byte[] data = new byte[maxPacketSize];
		DatagramPacket pkt = new DatagramPacket(data, data.length);
		ByteBuffer b = ByteBuffer.wrap(data);
		CRC32 crc = new CRC32();

		String destFilename = "";
		long destFilesize = 0;

		long totalBytesReceived = 0;

		// Receive first packet with the destination filename
		if (true) {
			pkt.setLength(data.length);
			sk.receive(pkt);
			if (pkt.getLength() < 8)
			{
				System.out.println("Pkt too short");
				return;
			}
			b.rewind();
			long chksum = b.getLong();
			crc.reset();
			crc.update(data, 8, pkt.getLength()-8);
			// Debug output
			// System.out.println("Received CRC:" + crc.getValue() + " Data:" + bytesToHex(data, pkt.getLength()));
			if (crc.getValue() != chksum)
			{
				System.out.println("Pkt corrupt");
			}
			else
			{
				// Get filename
				byte[] byteDest = new byte[filepathByteSize];
				b.get(byteDest, 0, filepathByteSize);
				destFilename = new String(byteDest).trim();
				destFilesize = b.getLong();

				System.out.println("Pkt " + destFilename);
				
				// DatagramPacket ack = new DatagramPacket(new byte[0], 0, 0, pkt.getSocketAddress());
				// sk.send(ack);
			}	
		}

		BufferedOutputStream bos;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(destFilename));
		}
		catch (Exception e) {
			System.out.println("Failed in writing file!");
			return;
		}

		int counter = 1;
		
		while(true)
		{
			pkt.setLength(data.length);
			sk.receive(pkt);
			if (pkt.getLength() < 8)
			{
				System.out.println("Pkt too short");
				continue;
			}
			b.rewind();
			long chksum = b.getLong();
			crc.reset();
			crc.update(data, 8, pkt.getLength()-8);
			// Debug output
			// System.out.println("Received CRC:" + crc.getValue() + " Data:" + bytesToHex(data, pkt.getLength()));
			if (crc.getValue() != chksum)
			{
				System.out.println("Pkt corrupt");
			}
			else
			{
				byte[] dataRcvd = new byte[maxDataSize];
				b.get(dataRcvd);
				bos.write(dataRcvd);
				bos.flush();

				totalBytesReceived += dataRcvd.length;

				System.out.println("Pkt received " + counter++ + " data size " + dataRcvd.length);
				// DatagramPacket ack = new DatagramPacket(new byte[0], 0, 0, pkt.getSocketAddress());
				// sk.send(ack);
			}

			if (totalBytesReceived >= destFilesize) {
				System.out.println(totalBytesReceived);
				bos.close();
				break;
			}
		}

	}

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
}
