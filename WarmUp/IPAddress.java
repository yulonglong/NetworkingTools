// Steven Kester Yuwono
// A0080415N

import java.util.*;

class IPAddress {
	public static void main(String[] args) {
		Scanner cin = new Scanner(System.in);
		String binaryStr = cin.next();
		if (binaryStr.length() != 32) {
			System.out.println("Invalid input! Please enter a 32-character string!");
			return;
		}
		try {
			// Substring for each 8-bit string
			String first = binaryStr.substring(0,8);
			String second = binaryStr.substring(8,16);
			String third = binaryStr.substring(16,24);
			String fourth = binaryStr.substring(24,32);
			// Convert the binary representation into base 10 decimal
			int firstInt = Integer.parseInt(first, 2);
			int secondInt = Integer.parseInt(second, 2);
			int thirdInt = Integer.parseInt(third, 2);
			int fourthInt = Integer.parseInt(fourth, 2);
			// Print in the desired format
			System.out.println(firstInt + "." + secondInt + "." + thirdInt + "." + fourthInt);
		}
		catch (Exception e) {
			System.out.println("Exception caught! Input is invalid!");
		}
		return;
	}
}