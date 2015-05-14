import java.net.*;
import java.io.*;
import java.nio.charset.Charset;

/**
 * This is a representation of a PD Client Connection to be used by the server.
 * @author Isaac Banner
 */
public class PubDiceClientConnection {
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;
	
	/**
	 * Creates a new client wrapped around the passed socket.
	 * @param socket - the socket to wrap around
	 */
	public PubDiceClientConnection(Socket socket) throws IOException {
		this.socket = socket;
		this.in = new DataInputStream(socket.getInputStream());
		this.out = new DataOutputStream(socket.getOutputStream());
		String name = "";
	}

	/**
	 * Writes to the output stream of this client connection.
	 * @param message - the message to write
	 */
	public void write(String message) throws IOException {
		int len = message.length();
		byte[] mBytes = message.getBytes(Charset.forName("UTF-8"));
		this.out.writeInt(len);
		this.out.write(mBytes, 0, len);
	}

	/**
	 * Reads in a message from the client
	 * @return a message from the client
	 */
	public String read() throws IOException {
		int len = this.in.readInt();
		byte[] data = new byte[len];
		this.in.readFully(data);

		return new String(data);
	}
}
