// Steven Kester Yuwono
// A0080415N

import java.util.*;
import java.util.zip.CRC32;
import java.io.*;

class Checksum {
	public static void main (String[] args) {
		if (args.length != 1) {
			System.out.println("Invalid Arguments! Please enter one filename only!");
			return;
		}
		String source = args[0];
		File sourceFile;
		BufferedInputStream bis;
		try {
			sourceFile = new File(source);
			bis = new BufferedInputStream(new FileInputStream(sourceFile));
			byte[] data = new byte[(int) sourceFile.length()];
			bis.read(data);
			CRC32 check = new CRC32();
			check.update(data);
			System.out.println(check.getValue());
		} catch (Exception e) {
			System.out.println("Exception caught! Error!");
			return;
		}
	}
}
