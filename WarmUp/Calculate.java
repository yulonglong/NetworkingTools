// Steven Kester Yuwono
// A0080415N

import java.util.*;

class Calculate {
	public static boolean isAllDigit(String word) {
		for(int i=0;i<(int)word.length();i++){
			if (!Character.isDigit(word.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static void main(String[] args) {
		if(args.length != 3) {
			System.out.println("Incorrect number of arguments");
			return;
		}
		if (!isAllDigit(args[0]) || !isAllDigit(args[2])) {
			System.out.println("Invalid inputs");
			return;
		}

		try {
			int firstNum = Integer.parseInt(args[0],10);
			int secondNum = Integer.parseInt(args[2],10);
			int ans = 1;
			if (args[1].equals("+")) {
				ans = firstNum + secondNum;
			}
			else if (args[1].equals("-")) {
				ans = firstNum - secondNum;
			}
			else if (args[1].equals("*")) {
				ans = firstNum * secondNum;
			}
			else if (args[1].equals("/")) {
				if (secondNum == 0) {
					System.out.println("Division by zero");
					return;
				}
				ans = firstNum / secondNum;
			}
			else if (args[1].equals("**")) {
				for(int i=0;i<secondNum;i++){
					ans = ans * firstNum;
				}
			}
			else {
				System.out.println("Invalid inputs");
				return;
			}
			System.out.println(ans);
		}
		catch (Exception e) {
			System.out.println("Exception caught! Invalid Input!");
		}
	}
}
