// Steven Kester Yuwono
// A0080415N

//// (2 marks) Your proxy can handle small web objects like a small file or image. 
//// (2 marks) Your proxy can handle a complex web page with multiple objects, e.g., www.comp.nus.edu.sg.
//// (2 marks) Your proxy can handle very large files of up to 1 GB.
//// (2 marks) Your proxy should be able to handle erroneous requests such as a "404" response.
////           It should also return "502" response if cannot reach the web server.
//// (2 marks) Your proxy can also handle the POST method in addition to GET method,
////           and will correctly include the request body sent in the POST-request.

// (2 marks) Simple caching. A typical proxy server will cache the web pages each time the client makes a particular request for the first time. 
//           The basic functionality of caching works as follows:
//           When the proxy gets a request, it checks if the requested object is cached, and if it is, it
//           simply returns the object from the cache, without contacting the server. If the object is not
//           cached, the proxy retrieves the object from the server, returns it to the client and caches a
//           copy for future requests.
//           Your implementation will need to be able to write responses to the disk (i.e., the cache) and
//           fetch them from the disk when you get a cache hit. For this you need to implement some
//           internal data structure in the proxy to keep track of which objects are cached and where they
//           are on the disk. You can keep this data structure in main memory; there is no need to make
//           it persist across shutdowns.
// (2 marks) Advanced caching. Your proxy server must verify that the cached objects are still valid and
//           that they are the correct responses to the client’s requests. Your proxy can send a request
//           to the origin server with a header If-Modified-Since and the server will response with a
//           HTTP 304 - Not Modified if the object has not changed. This is also known as a Conditional Request.
//// (2 marks) Text censorship. A text file censor.txt containing a list of censored words is placed in
////           the same directory as your WebProxy. Each line of the file contains one word. Your proxy
////           should detect text or HTML files that are being transfered (from the Content-type header)
////           and replace any censored words with 3 dashes "–-". The matching word should be case insensitive.
//// (2 marks) Multi-threading. Your proxy can concurrently handle multiple connections from several clients.


import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.charset.*;

class MyThread implements Runnable {
	List<String> censorWords;
	Socket client;
	public MyThread(Socket _client, List<String> _censorWords) {
		client = _client;
		censorWords = _censorWords;
	}

	private void printStartMessage() {
		System.out.println("\n------START CONNECTION FROM: " + client + "-------\n");
	}
	private void printEndMessage() {
		System.out.println("\n------END CONNECTION FROM: " + client + "-------\n");
	}

	public void run() {
		String method;
		String version;
		URL url;
		BufferedReader fromClient;
		BufferedOutputStream toClient;

		printStartMessage();		
		try {
			/** Read client's HTTP request **/
			fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			toClient = new BufferedOutputStream(client.getOutputStream());
			
			String firstLine = fromClient.readLine();
			String[] tmp = firstLine.split(" ");
			method = tmp[0];
			url = new URL(tmp[1]);
			version = tmp[2];
		}
		catch (Exception e) {
			System.out.println("Error reading request from client: " + e);
			printEndMessage();
			return;
		}

		try {
			/** connect to server and relay client's request **/
			Socket server = new Socket(url.getHost(),80);
			System.out.println("Connected to server!\n");
			/** Get response from server **/

			System.out.println("-------START REQUEST FROM CLIENT------");
			PrintWriter toServer = new PrintWriter(server.getOutputStream());
			toServer.println(method + " " + url.getFile() + " " + version);
			System.out.println(method + " " + url + " " + version);
			System.out.println(method + " " + url.getFile() + " " + version);
			while (fromClient.ready()) {
				String command = fromClient.readLine();
				if (command.toLowerCase().contains("keep-alive")) continue; // Disable keep alive
				toServer.println(command);
				System.out.println(command);
				if (command.length() == 0) break;
			}
			// Read necessary POST information
			if (method.equals("POST")) {
				char[] charBuff = new char[64 * 1024];;
				int lenCharBuff = fromClient.read(charBuff);
				String postString = String.copyValueOf(charBuff, 0, lenCharBuff);
				toServer.println(postString);
				System.out.println(postString);
			}

			System.out.println("------- END REQUEST FROM CLIENT------\n");
			toServer.flush();
			System.out.println("Request sent to server!");

			BufferedInputStream fromServer = new BufferedInputStream(server.getInputStream());
			byte[] buff = new byte[64 * 1024];
			int len = -1;
			boolean isTextHtml = false;
			boolean isEncoded = false;

			// Get the header response from the server
			if ((len = fromServer.read(buff)) > 0) {
				String currString = new String(buff, 0 ,len);
				int endHeaderIndex = currString.indexOf("\r\n\r\n");
				if (endHeaderIndex != -1) {
					currString = currString.substring(0,endHeaderIndex);
				}
				System.out.println("---- START HTTP RESPONSE FROM SERVER ----");
				System.out.println(currString);
				System.out.println("---- END HTTP RESPONSE FROM SERVER ----");
				
				Scanner scanHeader = new Scanner(currString);
				if (scanHeader.hasNext()) {
					String httpVersion = scanHeader.next();
					String errorCode = scanHeader.next();
					String errorDescription = scanHeader.nextLine();
					if ((errorCode.length() == 3) && ((errorCode.substring(0,1).equals("4")) ||(errorCode.substring(0,1).equals("5")))) {
						String errorHtml = "<h1> " + errorCode + " " + errorDescription + "</h1>\n";
						byte[] buffer = errorHtml.getBytes(Charset.forName("UTF-8"));
						toClient.write(buffer, 0, buffer.length);
						toClient.flush();
						fromServer.close();
						toServer.close();
						fromClient.close();
						toClient.close();
						return;
					}
				}

				while (scanHeader.hasNext()) {
					String fieldTitle = scanHeader.next();
					String fieldAttribute = scanHeader.nextLine();
					if ((fieldTitle.toLowerCase().contains("content-type")) && (fieldAttribute.toLowerCase().contains("text/html"))) {
						isTextHtml = true;
					}
					else if (fieldTitle.toLowerCase().contains("content-encoding")) {
						isEncoded = true;
					}
				}
			}

			// if the content type received is text/html, censor text from censor.txt
			if (isTextHtml && !isEncoded) {
				System.out.println("----START SENDING TEXT RESPONSE FROM SERVER TO CLIENT-----");
				StringBuilder htmlPage = new StringBuilder();
				// Append bytes previously read
				if (len > 0) {
					String temp =  new String(buff, 0 , len);
					htmlPage.append(temp);
				}
				// append bytes to string until the end of stream
				while ((len = fromServer.read(buff)) > 0) {
					System.out.println("Trying to write text : " + len + " bytes");
					String temp =  new String(buff, 0 , len);
					htmlPage.append(temp);
				}
				// Iterate through the words to be censored, and replace it by --- (not case sensitive)
				String htmlPageString = htmlPage.toString();
				System.out.println(htmlPageString);
				for (int i=0;i<censorWords.size();i++) {
					htmlPageString = htmlPageString.replaceAll("(?i)" + censorWords.get(i), "---");
				}
				// Convert the string back to bytes and send to client
				byte[] outBuffer = htmlPageString.getBytes(Charset.forName("UTF-8"));
				toClient.write(outBuffer, 0, outBuffer.length);
				toClient.flush();
				System.out.println("----END SENDING TEXT RESPONSE FROM SERVER TO CLIENT-----");
			}
			// if the content is not text/html, read all the bytes and send to client straight away
			else {
				System.out.println("----START SENDING NON-TEXT RESPONSE FROM SERVER TO CLIENT-----");
				// The remaining byte information from reading the header earlier
				if (len > 0) {
					System.out.println("Trying to write : " + len + " bytes");
					toClient.write(buff, 0, len);
					toClient.flush();
				}

				while ((len = fromServer.read(buff)) > 0) {
					System.out.println("Trying to write : " + len + " bytes");
					toClient.write(buff, 0, len);
					toClient.flush();
				}
				System.out.println("----END SENDING NON-TEXT RESPONSE FROM SERVER TO CLIENT-----");
			}

			fromServer.close();
			toClient.close();
			fromClient.close();
			toServer.close();

			server.close();
			client.close();
		}
		catch (IOException e) {
			System.out.println("Error in relaying client's request to server: " + e);
			printEndMessage();
			return;
		}
		printEndMessage();
	}
}

public class WebProxy {
	private static List<String> censorWords;

	/** Port for the proxy */
	private static int port;

	/** Socket for client connections */
	private static ServerSocket socket;
	
	public static void main(String args[]) {
		/** Read command line arguments and start proxy */
		if (args.length != 1) {
			System.out.println("Wrong number of arguments! Please enter the port as arg!");
			return;
		}
		/** Read port number as command-line argument **/
		else {
			port = Integer.parseInt(args[0]);
		}

		// Read censor.txt for the list of words to be censored
		try {
			censorWords = new ArrayList<String>();
			Scanner scanCensor = new Scanner(new File("censor.txt"));
			while (scanCensor.hasNext()) {
				String line = scanCensor.nextLine();
				censorWords.add(line);
			}
		}
		catch (Exception e) {
			System.out.println("Error in reading censor.txt : " + e);
		}

		try {
			/** Create a server socket, bind it to a port and start listening **/
			socket = new ServerSocket(port);
		}
		catch (IOException e) {
			System.out.println("Error in creating ServerSocket: " + e);
			return;
		}

		/** Main loop. Listen for incoming connections **/
		while (true) {
			Socket client = null;
			try {
				client = socket.accept();
			} 
			catch (Exception e) {
				System.out.println("Error accepting socket: " + e);
				continue;
			}

			Runnable r = new MyThread(client,censorWords);
			// r.run();
			new Thread(r).start();
		}
	}
}
