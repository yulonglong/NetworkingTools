// Steven Kester Yuwono
// A0080415N

import java.util.*;
import java.io.*;

class Copier {
	public static void main (String[] args) {
		if (args.length != 2) {
			System.out.println("Invalid Arguments! Please enter two paths to source and dest file!");
			return;
		}
		String source = args[0];
		String dest = args[1];
		File sourceFile;
		File destFile;
		BufferedInputStream bis;
		try {
			sourceFile = new File(source);
			destFile = new File(dest);
			bis = new BufferedInputStream(new FileInputStream(sourceFile));
		} catch (Exception e) {
			System.out.println("File not found");
			return;
		}

		try {
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile));
			byte[] buff = new byte[32 * 1024];
			int len;
			while ((len = bis.read(buff)) > 0) {
				bos.write(buff, 0, len);
			}
			System.out.println(source + " successfully copied to " + dest);
			bis.close();
			bos.close();
		}
		catch (Exception e) {
			System.out.println("Failed in writing file!");
			return;
		}
	}
}