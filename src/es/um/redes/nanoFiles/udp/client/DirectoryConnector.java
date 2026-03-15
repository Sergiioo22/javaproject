package es.um.redes.nanoFiles.udp.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.LinkedHashMap;
import es.um.redes.nanoFiles.tcp.client.NFConnector;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	/**
	 * Puerto en el que atienden los servidores de directorio
	 */
	private static final int DIRECTORY_PORT = 6868;
	/**
	 * Tiempo máximo en milisegundos que se esperará a recibir una respuesta por el
	 * socket antes de que se deba lanzar una excepción SocketTimeoutException para
	 * recuperar el control
	 */
	private static final int TIMEOUT = 1000;
	/**
	 * Número de intentos máximos para obtener del directorio una respuesta a una
	 * solicitud enviada. Cada vez que expira el timeout sin recibir respuesta se
	 * cuenta como un intento.
	 */
	private static final int MAX_NUMBER_OF_ATTEMPTS = 5;

	/**
	 * Socket UDP usado para la comunicación con el directorio
	 */
	private DatagramSocket socket;
	/**
	 * Dirección de socket del directorio (IP:puertoUDP)
	 */
	private InetSocketAddress directoryAddress;
	/**
	 * Nombre/IP del host donde se ejecuta el directorio
	 */
	private String directoryHostname;

	public static class DownloadedFile {
		public final String filename;
		public final long filesize;
		public final byte[] data;
		public final String filehash;

		public DownloadedFile(String filename, long fsize, byte[] data, String filehash) {
			this.filename = filename;
			this.filesize = fsize;
			this.data = data;
			this.filehash = filehash;
		}
	}

	public DirectoryConnector(String hostname) throws IOException {
		// Guardamos el string con el nombre/IP del host
		directoryHostname = hostname;
		/*
		 * TODO: (Boletín SocketsUDP) Convertir el string 'hostname' a InetAddress y
		 * guardar la dirección de socket (address:DIRECTORY_PORT) del directorio en el
		 * atributo directoryAddress, para poder enviar datagramas a dicho destino.
		 */
		 // SOLUCION
		 InetAddress dirInetAddress = InetAddress.getByName(hostname);
		 directoryAddress = new InetSocketAddress(dirInetAddress, DIRECTORY_PORT);
		/*
		 * TODO: (Boletín SocketsUDP) Crea el socket UDP en cualquier puerto para enviar
		 * datagramas al directorio
		 */
		 
		 // SOLUCION (CREO)
		 socket = new DatagramSocket();

	}

	/**
	 * Método para enviar y recibir datagramas al/del directorio
	 * 
	 * @param requestData los datos a enviar al directorio (mensaje de solicitud)
	 * @return los datos recibidos del directorio (mensaje de respuesta)
	 */
	private byte[] sendAndReceiveDatagrams(byte[] requestData) {
		byte responseData[] = new byte[DirMessage.PACKET_MAX_SIZE];
		byte response[] = null;
		if (directoryAddress == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP server destination address is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"directoryAddress\"");
			System.exit(-1);

		}
		if (socket == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP socket is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"socket\"");
			System.exit(-1);
		}

		// SE AÑADE directoryAddress AL PAQUETE PARA QUE SEPA A DÓNDE ENVIARLO
		DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, directoryAddress);
		
		// SE AÑADEN VARIABLES PARA CONTROLAR EL BUCLE DE REINTENTOS
		int attempts = 0;
		boolean received = false;

		// BUCLE QUE REINTENTA HASTA ALCANZAR EL LÍMITE MAX_NUMBER_OF_ATTEMPTS
		while (attempts < MAX_NUMBER_OF_ATTEMPTS && !received) {
			try {
				socket.send(requestPacket);
				DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length);
				socket.setSoTimeout(TIMEOUT);
				socket.receive(responsePacket);
				
				int respDataLength = responsePacket.getLength();
				response = new byte[respDataLength];
				System.arraycopy(responseData, 0, response, 0, respDataLength);
				
				// SI LA RECEPCIÓN ES EXITOSA, SALIMOS DEL BUCLE
				received = true;
			} catch (SocketTimeoutException e) {
				// SI SALTA EL TIMEOUT, SUMAMOS UN INTENTO Y AVISAMOS
				attempts++;
				System.err.println("Warning: the server is not responding, retrying (" + attempts + "/" + MAX_NUMBER_OF_ATTEMPTS + ")");
			} catch (IOException e) {
				System.err.println(
						"Check your connection, cannot communicate with directory at " + directoryHostname);
				e.printStackTrace();
				System.exit(-1);
			}
		}

		// SI TRAS LOS INTENTOS NO SE HA RECIBIDO NADA, SE ABORTA
		if (!received) {
			System.err.println("Error: Maximum number of attempts reached. Aborting.");
			System.exit(-1);
		}

		if (response != null && response.length == responseData.length) {
			System.err.println("Your response is as large as the datagram reception buffer!!\n"
					+ "You must extract from the buffer only the bytes that belong to the datagram!");
		}
		return response;
	}

	/**
	 * Método para probar la comunicación con el directorio mediante el envío y
	 * recepción de mensajes sin formatear ("en crudo")
	 * 
	 * @return verdadero si se ha enviado un datagrama y recibido una respuesta
	 */
	public boolean testSendAndReceive() {
		/*
		 * TODO: (Boletín SocketsUDP) Probar el correcto funcionamiento de
		 * sendAndReceiveDatagrams. Se debe enviar un datagrama con la cadena "ping" y
		 * comprobar que la respuesta recibida empieza por "pingok". En tal caso,
		 * devuelve verdadero, falso si la respuesta no contiene los datos esperados.
		 */
		boolean success = false;
		// SOLUCION
		byte[] request = new String("ping").getBytes();
		byte[] response = sendAndReceiveDatagrams(request);
		String respStr = new String(response);
		System.out.println("Contents of received datagram: " + respStr) ;
		success = (respStr.startsWith("pingok"));

		return success;
	}

	public String getDirectoryHostname() {
		return directoryHostname;
	}

	/**
	 * Método para "hacer ping" al directorio, comprobar que está operativo y que
	 * usa un protocolo compatible. Este método no usa mensajes bien formados.
	 * 
	 * @return Verdadero si
	 */
	public boolean pingDirectoryRaw() {
		boolean success = false;
		
		byte[] request = new String("ping&" + NanoFiles.PROTOCOL_ID).getBytes();
		byte[] response = sendAndReceiveDatagrams(request);
		String respStr = new String(response);
		System.out.println("Contents of received datagram: " + respStr);
		
		// Se comprueba si la cadena recibida es "welcome" e informa por pantalla
		if (respStr.equals("welcome")) {
			System.out.println("* Ping raw exitoso: El directorio utiliza un protocolo compatible.");
			success = true;
		} else {
			System.err.println("* Ping raw fallido. El directorio devolvió: " + respStr);
			success = false;
		}

		return success;
	}

	/**
	 * Método para "hacer ping" al directorio, comprobar que está operativo y que es
	 * compatible.
	 * 
	 * @return Verdadero si el directorio está operativo y es compatible
	 */
	public boolean pingDirectory() {
		boolean success = false;
		
		// 1. Crear el mensaje a enviar (objeto DirMessage) con la operación PING
		DirMessage requestMsg = new DirMessage(DirMessageOps.OPERATION_PING);
		requestMsg.setProtocolID(NanoFiles.PROTOCOL_ID);
		
		// 2. Convertir el objeto a string y extraer bytes
		String requestStr = requestMsg.toString();
		byte[] requestData = requestStr.getBytes();
		
		// 3. Enviar datagrama y recibir respuesta
		byte[] responseData = sendAndReceiveDatagrams(requestData);
		
		if (responseData != null) {
			// 4. Convertir respuesta a DirMessage
			String responseStr = new String(responseData);
			DirMessage responseMsg = DirMessage.fromString(responseStr);
			
			// 5. Extraer datos y procesarlos
			if (responseMsg.getOperation().equals(DirMessageOps.OPERATION_PING_OK)) {
				success = true;
			}
		}

		return success;
	}

	/**
	 * Método para dar de alta como servidor de ficheros en el puerto indicado.
	 * 
	 * @param serverPort El puerto TCP en el que este peer sirve ficheros a otros
	 * @return Verdadero si el directorio tiene registrado a este peer como servidor
	 *         y acepta la lista de ficheros, falso en caso contrario.
	 */
	public boolean registerFileServer(int serverPort) {
		boolean success = false;

		// TODO: Ver TODOs en pingDirectory y seguir esquema similar

		return success;
	}

	/**
	 * Método para obtener la lista de ficheros alojados en el directorio. Para cada
	 * fichero se debe obtener un objeto FileInfo con nombre, tamaño y hash.
	 * 
	 * @return Los ficheros disponibles en el directorio, o null si el directorio no
	 *         pudo satisfacer nuestra solicitud
	 */
	public FileInfo[] getFileList() {
		FileInfo[] filelist = new FileInfo[0];
		// TODO: Ver TODOs en pingDirectory y seguir esquema similar

		return filelist;
	}

	public Map<String, InetSocketAddress> getPeerList() {
		Map<String, InetSocketAddress> peers = new LinkedHashMap<String, InetSocketAddress>();

		return peers;
	}

	public Map<String, InetSocketAddress[]> searchFilesByHash(String hashSubstring) {
		Map<String, InetSocketAddress[]> results = new LinkedHashMap<String, InetSocketAddress[]>();

		return results;
	}

	public DownloadedFile downloadFileFromDirectory(String hashSubstring) {
		byte[] fileData = null;
		String filename = null;
		long filesize = -1;
		String filehash = null;

		return new DownloadedFile(filename, filesize, fileData, filehash);
	}

	/**
	 * Método para darse de baja como servidor de ficheros.
	 * 
	 * @return Verdadero si el directorio tiene registrado a este peer como servidor
	 *         y ha dado de baja sus ficheros.
	 */
	public boolean unregisterFileServer() {
		boolean success = false;

		return success;
	}

}
