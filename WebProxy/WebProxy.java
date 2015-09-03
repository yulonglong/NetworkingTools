import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;

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
		Socket client = null;
		
		while (true) {
			String[] requestString = new String[100];
			int indexString = 1;
			String method;
			String URI;
			String version;
			BufferedReader in;
			try {
				client = socket.accept();
				System.out.println("'Received a connection from: " + client);

				/** Read client's HTTP request **/
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				
				requestString[0] = in.readLine();
				String[] tmp = requestString[0].split(" ");
				method = tmp[0];
				URI = tmp[1];
				version = tmp[2];
				while (in.ready()) {
					requestString[indexString++] = in.readLine();
					System.out.println(requestString[indexString-1]);
				}
			} 
			catch (IOException e) {
				System.out.println("Error reading request from client: " + e);
				/* Definitely cannot continue, so skip to next
				 * iteration of while loop. */
				continue;
			}
			
			/** Check cache if file exists **/
			File file = new File(URI);
			if (file.exists()) {
				try {
					/** Read the file **/
					byte[] fileArray;
					fileArray = Files.readAllBytes(file.toPath());
				}
				catch (IOException e) {
					System.out.println("Error reading from cache: " + e);
					return;
				}
				
				/** generate appropriate respond headers and send the file contents **/
				
			}
			else {
				System.out.println("Cache miss! Trying to connect to " + URI);
				try {
					/** connect to server and relay client's request **/
					Socket server = new Socket(URI,80);
					
					/** Get response from server **/

					server.close();
					/** Cache the contents as appropriate **/
					
					/** Send respose to client **/
					client.close();
				}
				catch (IOException e) {
				
				}
			}
			
		}

	}

}
