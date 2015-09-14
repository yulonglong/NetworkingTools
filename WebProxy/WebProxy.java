// Steven Kester Yuwono
// A0080415N


// WebProxy has been tested using Firefox 40.0.3 on Windows 10 Education
// Before Running WebProxy, please take note of the following environment used for testing:
// 1. Make sure WebProxy.java and censor.txt is inside a new folder (it will create many cache files if you place it on desktop)
// 2. Compile with the following commad
//    $>> javac WebProxy.java
// 3. Run with the desired port (e.g. port 8000)
//    $>> java WebProxy 8000
// 4. Open Mozilla Firefox -> Options -> Advanced -> Network -> Connection -> Settings
// 5. Select "Manual Proxy Configurations", enter the IP-Address and port. 
//    By default is "localhost" or "127.0.0.1", and the port specified earlier. e.g. "8000"
// 6. Check "Use this proxy server for all protocols"
// 7. Type "about:config" in the url bar of Mozilla Firefox, and continue
// 8. Search for the following options and set it to the values below:
//    a. network.http.proxy.version  = 1.0
//    b. browser.cache.memory.enable = false
//    c. browser.cache.disk.enable = false
//    d. network.http.accept-encoding = "" (blank, erase the value)
// 9. Mozilla Firefox and WebProxy are ready!


// All of the following features are implemented:
// 1. Your proxy can handle small web objects like a small file or image. 
// 2. Your proxy can handle a complex web page with multiple objects, e.g., www.comp.nus.edu.sg.
// 3. Your proxy can handle very large files of up to 1 GB.
// 4. Your proxy should be able to handle erroneous requests such as a "404" response.
//    It should also return "502" response if cannot reach the web server.
// 5. Your proxy can also handle the POST method in addition to GET method,
//    and will correctly include the request body sent in the POST-request.

// 6. Simple caching. A typical proxy server will cache the web pages each time the client makes a particular request for the first time. 
//    The basic functionality of caching works as follows:
//    When the proxy gets a request, it checks if the requested object is cached, and if it is, it
//    simply returns the object from the cache, without contacting the server. If the object is not
//    cached, the proxy retrieves the object from the server, returns it to the client and caches a
//    copy for future requests.
//    Your implementation will need to be able to write responses to the disk (i.e., the cache) and
//    fetch them from the disk when you get a cache hit. For this you need to implement some
//    internal data structure in the proxy to keep track of which objects are cached and where they
//    are on the disk. You can keep this data structure in main memory; there is no need to make
//    it persist across shutdowns.
// 7. Advanced caching. Your proxy server must verify that the cached objects are still valid and
//    that they are the correct responses to the client’s requests. Your proxy can send a request
//    to the origin server with a header If-Modified-Since and the server will response with a
//    HTTP 304 - Not Modified if the object has not changed. This is also known as a Conditional Request.
// 8. Text censorship. A text file censor.txt containing a list of censored words is placed in
//    the same directory as your WebProxy. Each line of the file contains one word. Your proxy
//    should detect text or HTML files that are being transfered (from the Content-type header)
//    and replace any censored words with 3 dashes "---". The matching word should be case insensitive.
// 9. Multi-threading. Your proxy can concurrently handle multiple connections from several clients.



import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.charset.*;

// Class/Data-structure to cache webpages
class FileCache {
	Integer currNum = 0;
	String cacheFormat = ".cache";

	HashMap<String,String> map;
	HashMap<String,String> lastModifiedMap;

	FileCache() {
		map = new HashMap<String,String>();
		lastModifiedMap = new HashMap<String,String>();
	}

	public boolean isCacheExists(String url) {
		return (map.containsKey(url) && lastModifiedMap.containsKey(url));
	}

	public FileInputStream getCache(String url) {
		try {
			String filename = map.get(url);
			FileInputStream fos = new FileInputStream(filename);
			return fos;
		}
		catch (Exception e) {
			System.out.println("Failed in reading cache : " + e);
			return null;
		}
	}

	public void writeCache(String url, byte[] buffer, int off, int len) {
		String filename;
		FileOutputStream fos;
		try {
			if (!isCacheExists(url)) {
				filename = currNum.toString()+cacheFormat;
				map.put(url,currNum.toString()+cacheFormat);
				currNum++;
				fos = new FileOutputStream(filename);
			}
			else {
				filename = map.get(url);
				fos = new FileOutputStream(filename,true);
			}
			fos.write(buffer,off,len);
			fos.close();
		}
		catch (Exception e) {
			System.out.println("Failed in writing cache : " + e);
			return;
		}
	}

	public void writeLastModified(String url, String lastModified) {
		lastModifiedMap.put(url,lastModified);
	}

	public String getLastModified(String url) {
		return lastModifiedMap.get(url);
	}

	public void resetCacheUrl(String url) {
		map.remove(url);
	}
}

class MyThread implements Runnable {
	// Set this boolean to false to disable all console output
	private boolean debugMode = true;

	List<String> censorWords;
	FileCache fileCache;
	Socket client;
	Socket server;

	String method;
	String version;
	URL url;

	BufferedReader fromClient;
	BufferedOutputStream toClient;
	BufferedInputStream fromServer;
	PrintWriter toServer;

	byte[] buff = new byte[64 * 1024];
	int len = -1;
	int clientContentLength = 0;

	boolean isTextHtml = false;
	boolean isEncoded = false;
	boolean isModified = true;
	

	public MyThread(Socket _client, List<String> _censorWords, FileCache _fileCache) {
		client = _client;
		censorWords = _censorWords;
		fileCache = _fileCache;
	}

	private void printStartMessage() {
		if (debugMode) System.out.println("\n------START CONNECTION FROM: " + client + "-------\n");
	}
	private void printEndMessage() {
		if (debugMode) System.out.println("\n------END CONNECTION FROM: " + client + "-------\n");
	}
	private void printLog(String message) {
		if (debugMode) System.out.println(message);
	}

	private boolean initializeStreamFromClient() {
		try {
			// Read client's HTTP request
			fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			toClient = new BufferedOutputStream(client.getOutputStream());
			
			String firstLine = fromClient.readLine();
			String[] tmp = firstLine.split(" ");
			method = tmp[0];
			url = new URL(tmp[1]);
			version = tmp[2];
			return true;
		}
		catch (Exception e) {
			printLog("Error reading request from client: " + e);
			return false;
		}
	}

	private void closeServer() {
		try {
			fromServer.close();
			toServer.close();
			server.close();
		}
		catch (Exception e) {}
	}
	private void closeClient() {
		try {
			fromClient.close();
			toClient.close();	
			client.close();
		}
		catch (Exception e) {}
	}
	private void closeAllConnections() {
		closeClient();
		closeServer();
	}

	private ArrayList<String> getClientRequest() {
		ArrayList<String> clientRequest = new ArrayList<String>();

		printLog("-------START READING REQUEST FROM CLIENT------");
		try {
			clientRequest.add(method + " " + url.getFile() + " " + version);
			printLog(method + " " + url.getFile() + " " + version);

			// If cache exists, ask if modified since to get 304 response
			if (fileCache.isCacheExists(url.toString())) {
				clientRequest.add("If-Modified-Since:" + fileCache.getLastModified(url.toString()));
				printLog("If-Modified-Since:" + fileCache.getLastModified(url.toString()));
			}

			clientContentLength = 0;
			while (fromClient.ready()) {
				String command = fromClient.readLine();
				if (command.toLowerCase().contains("keep-alive")) continue; // Disable keep alive
				if (command.toLowerCase().contains("content-length")) {
					String length = command.substring(command.indexOf(" ")+1);
					clientContentLength = Integer.parseInt(length);
				}
				clientRequest.add(command);
				printLog(command);
				if (command.length() == 0) break;
			}
			printLog("-------END READING REQUEST FROM CLIENT------");
			return clientRequest;
		}
		catch (IOException e) {
			printLog("Error while reading client HTTP Request : " + e);
			printLog("-------END READING REQUEST FROM CLIENT------");
			return null;
		}
	}

	private boolean sendRequestToServer(ArrayList<String> clientRequest) {
		if (clientRequest == null) {
			return false;
		}
		printLog("-------START SENDING REQUEST TO SERVER------");
		try {
			toServer = new PrintWriter(server.getOutputStream());
			for (String line : clientRequest) {
				toServer.println(line);
			}

			// Read necessary POST information and send to Server
			if (method.equals("POST")) {
				int lenRead = 0;
				char[] charBuff = new char[64 * 1024];;
				int lenCharBuff;
				while ((lenCharBuff = fromClient.read(charBuff)) > 0) {
					String postString = String.copyValueOf(charBuff, 0, lenCharBuff);
					toServer.println(postString);
					printLog(postString);
					lenRead += lenCharBuff;
					if (lenRead >= clientContentLength) break;
				}
			}

			toServer.flush();
			printLog("-------END SENDING REQUEST TO SERVER------");
			return true;
		}
		catch (IOException e) {
			printLog("Error while sending HTTP Request to server : " + e);
			printLog("-------END SENDING REQUEST TO SERVER------");
			return false;
		}
	}

	private boolean processHttpResponseHeader() {
		// Get the header response from the server
		try {
			isTextHtml = false;
			isEncoded = false;
			isModified = true;
			if ((len = fromServer.read(buff)) > 0) {
				// Get just the header rom the byte array
				String currString = new String(buff, 0 ,len);
				int endHeaderIndex = currString.indexOf("\r\n\r\n");
				if (endHeaderIndex != -1) {
					currString = currString.substring(0,endHeaderIndex);
				}
				printLog("---- START HTTP RESPONSE FROM SERVER ----");
				printLog(currString);
				printLog("---- END HTTP RESPONSE FROM SERVER ----");
				
				// Parse the http header, detect error code and return the appropriate response
				// Detect if there is a cache and 304 not modified response is returned
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
						return false;
					}
					else if (errorCode.equals("304")) {
						isModified = false;
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
					else if (fieldTitle.toLowerCase().contains("date")) {
						fileCache.writeLastModified(url.toString(),fieldAttribute);
					}
				}
			}
			return true;
		}
		catch (IOException e ) {
			printLog("Error while parsing HTTP response header: " + e);
			return false;
		}
	}

	private boolean sendResponseFromCache() {
		// Check whether cache exists, if yes, write to toClient stream
		if ((!isModified) && (fileCache.isCacheExists(url.toString()))) {
			printLog("Cache Hit! and Not Modified!");
			FileInputStream fromCache = fileCache.getCache(url.toString());
			try {
				while ((len = fromCache.read(buff)) > 0) {
					printLog("Trying to write : " + len + " bytes");
					toClient.write(buff, 0, len);
					toClient.flush();
				}
				return true;
			}
			catch (IOException e) {
				printLog("Error while reading and writing from cache : " + e);
				return false;
			}
		}
		else {
			fileCache.resetCacheUrl(url.toString());
			return false;
		}
	}

	private boolean sendResponseToClient() {
		try {
			printLog("Cache Miss!");
			printLog("----START SENDING RESPONSE FROM SERVER TO CLIENT-----");

			// The remaining byte information from reading the header earlier
			if (len > 0) {
				printLog("Trying to write : " + len + " bytes");
				if (isTextHtml && !isEncoded) {
					String htmlString =  new String(buff, 0 , len);
					for (int i=0;i<censorWords.size();i++) {
						htmlString = htmlString.replaceAll("(?i)" + censorWords.get(i), "---");
					}
					byte[] outBuffer = htmlString.getBytes(Charset.forName("UTF-8"));
					toClient.write(outBuffer, 0, outBuffer.length);
					fileCache.writeCache(url.toString(),outBuffer, 0 , outBuffer.length);
				}
				else {
					toClient.write(buff, 0, len);
					fileCache.writeCache(url.toString(), buff, 0 , len);
				}
				toClient.flush();
			}

			// Read the remaining bytes
			while ((len = fromServer.read(buff)) > 0) {
				printLog("Trying to write : " + len + " bytes");
				if (isTextHtml && !isEncoded) {
					String htmlString =  new String(buff, 0 , len);
					for (int i=0;i<censorWords.size();i++) {
						htmlString = htmlString.replaceAll("(?i)" + censorWords.get(i), "---");
					}
					byte[] outBuffer = htmlString.getBytes(Charset.forName("UTF-8"));
					toClient.write(outBuffer, 0, outBuffer.length);
					fileCache.writeCache(url.toString(),outBuffer, 0 , outBuffer.length);
				}
				else {
					toClient.write(buff, 0, len);
					fileCache.writeCache(url.toString(), buff, 0 , len);
				}
				toClient.flush();
			}
			printLog("----END SENDING RESPONSE FROM SERVER TO CLIENT-----");
			return true;
		}
		catch (IOException e) {
			printLog("----END SENDING RESPONSE FROM SERVER TO CLIENT-----");
			printLog("Error while sending response to client : " + e);
			return false;
		}
	}

	private void executeProxy() throws IOException {
		// connect to server and relay client's request
		server = new Socket(url.getHost(), 80);
		printLog("Connected to server!\n");

		if (!sendRequestToServer(getClientRequest())) return;

		fromServer = new BufferedInputStream(server.getInputStream());

		if (!processHttpResponseHeader()) return;
		if (sendResponseFromCache()) return;
		sendResponseToClient();
	}

	public void run() {
		printStartMessage();
		if (!initializeStreamFromClient()) return;
		try {
			executeProxy();
			closeAllConnections();
		}
		catch (IOException e) {
			printLog("Error in relaying client's request to server: " + e);
			try {
				String errorHtml = "<h1> " + e.toString() + "</h1>\n";
				byte[] buffer = errorHtml.getBytes(Charset.forName("UTF-8"));
				toClient.write(buffer, 0, buffer.length);
				toClient.flush();
				closeAllConnections();
			}
			catch (IOException e2) {}
		}
		printEndMessage();
	}
}

public class WebProxy {
	private static List<String> censorWords;
	private static FileCache fileCache;

	private static int port;
	private static ServerSocket socket;
	
	public static void main(String args[]) {
		if (args.length != 1) {
			System.out.println("Wrong number of arguments! Please enter the port as arg!");
			return;
		}
		else {
			port = Integer.parseInt(args[0]);
		}

		try {
			fileCache = new FileCache();
			// Read censor.txt for the list of words to be censored
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
			// Create a server socket, bind it to a port and start listening
			socket = new ServerSocket(port);
		}
		catch (IOException e) {
			System.out.println("Error in creating ServerSocket: " + e);
			return;
		}

		// Main loop. Listen for incoming connections
		while (true) {
			Socket client = null;
			try {
				client = socket.accept();
			} 
			catch (Exception e) {
				System.out.println("Error accepting socket: " + e);
				continue;
			}

			// Run each incoming connection in new thread, multi-threading
			Runnable r = new MyThread(client,censorWords,fileCache);
			new Thread(r).start();
		}
	}
}
