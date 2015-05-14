import java.net.*;
import java.io.*;

public class PubDiceServer {

	/**
	 * Opens a socket on the passed host and post, then waits for clients to
	 * connect, and spins them off in new ClientThreads as they arrive.
	 */
	public static void main(String args[]) {
		if(args.length != 2) {
			System.err.println("Usage: java PubDiceServer <hostname> <port>");
			return;
		}

		String host;
		int port;
		
		try {
			host = args[0];
			port = Integer.parseInt(args[1]);
		} catch(NumberFormatException ex) {
			System.err.println("Usage: java PubDiceServer <hostname> <port>");
			return;
		}

		ServerSocket welcomeSocket;

		try {	
			welcomeSocket = new ServerSocket();
			welcomeSocket.bind(new InetSocketAddress (host, port));
		} catch (IOException ex) {
			System.err.println(ex);
			return;
		}

		try {
			while(true) 
			{
				Socket s = welcomeSocket.accept();
				PubDiceClientConnection c = new PubDiceClientConnection(s);
				PubDiceClientThread t = new PubDiceClientThread(c);

				t.start();		
			}
		} catch (IOException ex) {
			System.err.println(ex);
		}
	}
}
