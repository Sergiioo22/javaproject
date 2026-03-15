package es.um.redes.nanoFiles.udp.server;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.LinkedHashMap;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;
import es.um.redes.nanoFiles.util.NickGenerator;

public class NFDirectoryServer {
	/**
	 * Número de puerto UDP en el que escucha el directorio
	 */
	public static final int DIRECTORY_PORT = 6868;

	/**
	 * Socket de comunicación UDP con el cliente UDP (DirectoryConnector)
	 */
	private DatagramSocket socket = null;
	/*
	 * TODO: Añadir aquí como atributos las estructuras de datos que sean necesarias
	 * para mantener en el directorio cualquier información necesaria para la
	 * funcionalidad del sistema nanoFilesP2P: ficheros alojados, servidores
	 * registrados, etc.
	 */
	/**
	 * Lista de ficheros alojados en el directorio.
	 */
	private FileInfo[] directoryFiles;
	/**
	 * Lista de servidores registrados (IP, puerto TCP).
	 */
	private LinkedHashMap<String, InetSocketAddress> registeredPeers;

	/**
	 * Probabilidad de descartar un mensaje recibido en el directorio (para simular
	 * enlace no confiable y testear el código de retransmisión)
	 */
	private double messageDiscardProbability;

	public NFDirectoryServer(double corruptionProbability, String directoryFilesPath) throws SocketException {
		/*
		 * Guardar la probabilidad de pérdida de datagramas (simular enlace no
		 * confiable)
		 */
		messageDiscardProbability = corruptionProbability;
		/*
		 * Cargar los ficheros del directorio compartido.
		 */
		File dir = new File(directoryFilesPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		directoryFiles = FileInfo.loadFilesFromFolder(directoryFilesPath);
		System.out.println("* Directory loaded " + directoryFiles.length + " files from " + directoryFilesPath);

		socket = new DatagramSocket(DIRECTORY_PORT);

		/*
		 * TODO: (Boletín SocketsUDP) Inicializar atributos que mantienen el estado del
		 * servidor de directorio: peers registrados, etc.)
		 */

		if (NanoFiles.testModeUDP) {
			if (socket == null) {
				System.err.println("[testMode] NFDirectoryServer: code not yet fully functional.\n"
						+ "Check that all TODOs in its constructor and 'run' methods have been correctly addressed!");
				System.exit(-1);
			}
		}
	}

	public DatagramPacket receiveDatagram() throws IOException {
		DatagramPacket datagramReceivedFromClient = null;
		boolean datagramReceived = false;
		while (!datagramReceived) {

			byte[] receptionBuffer = new byte[DirMessage.PACKET_MAX_SIZE];
			datagramReceivedFromClient = new DatagramPacket(receptionBuffer, receptionBuffer.length);
			socket.receive(datagramReceivedFromClient);

			if (datagramReceivedFromClient == null) {
				System.err.println("[testMode] NFDirectoryServer.receiveDatagram: code not yet fully functional.\n"
						+ "Check that all TODOs have been correctly addressed!");
				System.exit(-1);
			} else {
				// Vemos si el mensaje debe ser ignorado (simulación de un canal no confiable)
				double rand = Math.random();
				if (rand < messageDiscardProbability) {
					System.err.println(
							"Directory ignored datagram from " + datagramReceivedFromClient.getSocketAddress());
				} else {
					datagramReceived = true;
				}
			}

		}

		return datagramReceivedFromClient;
	}

	public void runTest() throws IOException {

		System.out.println("[testMode] Directory starting...");

		System.out.println("[testMode] Attempting to receive 'ping' message...");
		DatagramPacket rcvDatagram = receiveDatagram();
		sendResponseTestMode(rcvDatagram);

		System.out.println("[testMode] Attempting to receive 'ping&PROTOCOL_ID' message...");
		rcvDatagram = receiveDatagram();
		sendResponseTestMode(rcvDatagram);
	}

	private void sendResponseTestMode(DatagramPacket pkt) throws IOException {

		String messageFromClient = new String(pkt.getData(), 0, pkt.getLength());
		System.out.println("Data received: " + messageFromClient);

		String responseText;
		InetSocketAddress clientAddr = (InetSocketAddress) pkt.getSocketAddress();
		
		if(messageFromClient.equals("ping")) {
			responseText = "pingok";
		} else if(messageFromClient.startsWith("ping&")) {
			// Uso de array seguro para prevenir ArrayIndexOutOfBoundsException si no hay datos tras el '&'
			String[] parts = messageFromClient.split("&");
			if(parts.length > 1 && parts[1].equals(NanoFiles.PROTOCOL_ID)) {
				responseText = "welcome";
			} else {
				responseText = "denied";
			}
		} else {
			responseText = "invalid";
		}
		
		byte[] responseData = responseText.getBytes();
		System.out.println("[test mode] Sent response to client at addr: " + clientAddr + " -Contents " + new String(responseData));
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);

	}

	public void run() throws IOException {

		System.out.println("Directory starting...");

		while (true) { // Bucle principal del servidor de directorio
			DatagramPacket rcvDatagram = receiveDatagram();

			sendResponse(rcvDatagram);

		}
	}

	private void sendResponse(DatagramPacket pkt) throws IOException {
		// Construir String a partir de los datos recibidos e imprimir para depuración
		String messageFromClient = new String(pkt.getData(), 0, pkt.getLength());
		System.out.println("Data received: \n" + messageFromClient);

		// Construir el objeto DirMessage
		DirMessage request = DirMessage.fromString(messageFromClient);
		String operation = request.getOperation(); 

		DirMessage responseMsg = null;

		switch (operation) {
		case DirMessageOps.OPERATION_PING: {
			// Comprobamos si el protocolId del mensaje coincide con el nuestro
			if (request.getProtocolId().equals(NanoFiles.PROTOCOL_ID)) {
				System.out.println("* Ping procesado: protocolo compatible (" + request.getProtocolId() + ")");
				responseMsg = new DirMessage(DirMessageOps.OPERATION_PING_OK);
			} else {
				System.err.println("* Ping procesado: protocolo incompatible (" + request.getProtocolId() + ")");
				responseMsg = new DirMessage(DirMessageOps.OPERATION_PING_ERROR);
			}
			break;
		}
		default:
			System.err.println("Unexpected message operation: \"" + operation + "\"");
			responseMsg = new DirMessage(DirMessageOps.OPERATION_INVALID);
		}

		// Convertir a String el objeto DirMessage de respuesta, extraer bytes y enviar
		String responseStr = responseMsg.toString();
		byte[] responseData = responseStr.getBytes();
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, pkt.getSocketAddress());
		socket.send(responsePacket);
	}

}
