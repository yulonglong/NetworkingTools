// Steven Kester Yuwono
// A0080415N

//// (2 marks) Your proxy can handle small web objects like a small file or image. 
//// (2 marks) Your proxy can handle a complex web page with multiple objects, e.g., www.comp.nus.edu.sg.
// (2 marks) Your proxy can handle very large files of up to 1 GB.
// (2 marks) Your proxy should be able to handle erroneous requests such as a “404” response.
//           It should also return a “502” response if cannot reach the web server.
// (2 marks) Your proxy can also handle the POST method in addition to GET method,
//           and will correctly include the request body sent in the POST-request.

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
// (2 marks) Text censorship. A text file censor.txt containing a list of censored words is placed in
//           the same directory as your WebProxy. Each line of the file contains one word. Your proxy
//           should detect text or HTML files that are being transfered (from the Content-type header)
//           and replace any censored words with 3 dashes “–-”. The matching word should be case insensitive.
//// (2 marks) Multi-threading. Your proxy can concurrently handle multiple connections from several clients.


import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;

class MyThread implements Runnable {
	Socket client;
	public MyThread(Socket _client) {
		client = _client;
	}

	public void run() {
		String method;
		String version;
		URL url;
		BufferedReader fromClient;
		BufferedOutputStream toClient;
		try {
			/** Read client's HTTP request **/
			fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			toClient = new BufferedOutputStream(client.getOutputStream());
			// toClient = new PrintWriter(client.getOutputStream(), true);
			
			String firstLine = fromClient.readLine();
			String[] tmp = firstLine.split(" ");
			method = tmp[0];
			url = new URL(tmp[1]);
			version = tmp[2];
		}
		catch (Exception e) {
			System.out.println("Error reading request from client: " + e);
			return;
		}

		System.out.println("Cache miss! Trying to connect to " + url);
		try {
			/** connect to server and relay client's request **/
			Socket server = new Socket(url.getHost(),80);
			
			System.out.println("Connected to server!");
			/** Get response from server **/

			PrintWriter toServer = new PrintWriter(server.getOutputStream());
			toServer.println(method + " " + url.getFile() + " " + version);
			System.out.println(method + " " + url.getFile() + " " + version);
			while (fromClient.ready()) {
				String command = fromClient.readLine();
				if (command.contains("keep-alive")) continue; // Disable keep alive
				toServer.println(command);
				System.out.println(command);
			}
			toServer.flush();

			System.out.println("Request sent to server!");

			BufferedInputStream fromServer = new BufferedInputStream(server.getInputStream());

			byte[] buff = new byte[64 * 1024];
			int len;
			while ((len = fromServer.read(buff)) > 0) {
				System.out.println("Trying to write : " + len + " bytes");
				toClient.write(buff, 0, len);
				toClient.flush();
			}
			System.out.println("Trying to close server InputStream");
			fromServer.close();
			System.out.println("Trying to close client OutputStream");
			toClient.close();

			System.out.println("Response from server has been sent to client!");

			server.close();
			/** Cache the contents as appropriate **/

			/** Send respose to client **/
			client.close();
		}
		catch (IOException e) {

		}
	}
}

public class WebProxy {
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
				System.out.println("'Received a connection from: " + client);
			} 
			catch (Exception e) {
				System.out.println("Error accepting socket: " + e);
				continue;
			}

			Runnable r = new MyThread(client);
			new Thread(r).start();
		}
	}
}
