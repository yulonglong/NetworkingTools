// Steven Kester Yuwono
// A0080415N

import java.util.*;
import java.io.*;

class Fields {
	public static void main(String[] args) {
		TreeMap<String,String> map = new TreeMap<String,String>();
		Scanner cin = new Scanner(System.in);
		boolean query = false;
		while (cin.hasNext()) {
			String line = cin.nextLine();
			if (line.toLowerCase().equals("quit")) {
				break;
			}
			if (line.length() == 0) {
				query = true;
				continue;
			}
			if (!query) {
				String field = (line.substring(0,line.indexOf(":"))).toLowerCase();
				String content = line.substring((line.indexOf(":"))+2); // Substring removing the colon ":" and space
				map.put(field,content);
			}
			else {
				String content = map.get(line.toLowerCase());
				if (content == null){
					System.out.println("Unknown field");
				}
				else {
					System.out.println(content);
				}
			}
		}
	}
}
