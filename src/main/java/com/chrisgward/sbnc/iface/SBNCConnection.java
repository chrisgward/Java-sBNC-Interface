package com.chrisgward.sbnc.iface;

import com.chrisgward.sbnc.iface.exception.SBNCException;
import lombok.Getter;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class SBNCConnection {
	@Getter private final String host;
	@Getter private final int port;
	@Getter private final String user;
	@Getter private final String pass;

	private final Socket socket;
	private final BufferedReader reader;
	private final BufferedWriter writer;
	public SBNCConnection(String host, int port, String user, String pass) throws IOException {
		this.host = host;
		this.port = port;
		this.user = user;
		this.pass = pass;

		this.socket = new Socket(host, port);
		this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

		writer.write("RPC_IFACE\n");
		writer.flush();

		long start = System.currentTimeMillis();
		while(System.currentTimeMillis() - start < 3000) {
			if(reader.ready()) {
				String line = reader.readLine();
				if(line.equals("RPC_IFACE_OK")) {
					return;
				} else if(line.equals("[RPC_BLOCK]")) {
					throw new SBNCException("Runtime error occured in the RPC system: This IP address is blocked.");
				}
			}
		}

		close();
		throw new SBNCException("The server did not respond in time.");
	}

	public void close() throws IOException {
		socket.close();
		reader.close();
		writer.close();
	}

	public Object call(String command, String... args) throws IOException, SBNCException{
		return callAs(null, command, args);
	}

	public Object callAs(String user, String command, String... args) throws IOException, SBNCException{
		ArrayList<Object> list = new ArrayList<Object>();
		if(user == null) {
			list.add(this.user);
			list.add(this.pass);
		} else {
			list.add(user);
			list.add(this.user + ":" + this.pass);
		}

		list.add(command);
		list.add(args);

		String output = SBNCSerializer.serialize(list);
		writer.write(output);
		writer.write("\n");
		writer.flush();

		long start = System.currentTimeMillis();
		while(System.currentTimeMillis() - start < 3000) {
			if(reader.ready()) {
				String line = reader.readLine();
				if(line.equals("RPC_OK")) {
					return null;
				} else if(line.equals("RPC_PARAMCOUNT")) {
					throw new SBNCException("Incorrect parameter count");
				}

				Object[] result = SBNCSerializer.parse(line);
				if(result.length != 1) {
					return line;
				} else if (result[0] instanceof SBNCException) {
					throw new SBNCException((SBNCException)result[0]);
				}

				return result;
			}
		}

		throw new SBNCException("Server did not respond in time");
	}
}
