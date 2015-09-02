// Steven Kester Yuwono
// A0080415N

import java.io.*;
import java.util.*;

class TimePrinter extends TimerTask {
	String outStr;
	TimePrinter(String _outStr) {
		outStr = _outStr;
	}

	public void run() {
		System.out.println(outStr);
	}

	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("Please enter 2 arguments, startTime and interval!");
			return;
		}
		String outputString = args[0];
		long start = Integer.parseInt(args[1]);
		long interval = Integer.parseInt(args[2]);

		Timer timer = new Timer();
		timer.schedule(new TimePrinter(outputString), start*1000 ,interval*1000 );

		Scanner cin = new Scanner(System.in);
		while (true) {
			String command = cin.next();
			if (command.toLowerCase().equals("q")) {
				timer.cancel();
				return;
			}
		}
	}
}
