package es.um.redes.nanoFiles.tcp.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import es.um.redes.nanoFiles.util.FileDatabase;
import es.um.redes.nanoFiles.util.FileInfo;

public class PeerMessageTestCompleto {

	public static void main(String[] args) throws IOException {

		/*
		 * Nuevas pruebas
		 */
		final int MAX_CHUNK_SIZE = 5000;
		String nombreFichero = null;

		String nombreArchivo = "peermsg.bin";
		DataOutputStream fos = new DataOutputStream(new FileOutputStream(nombreArchivo));
		DataInputStream fis = new DataInputStream(new FileInputStream(nombreArchivo));

		FileInfo[] directoryFiles;
		String directoryFilesPath = "dir-shared";

		File dir = new File(directoryFilesPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		directoryFiles = FileInfo.loadFilesFromFolder(directoryFilesPath);
		System.out.println("* Directory loaded " + directoryFiles.length + " files from " + directoryFilesPath);

		// Simulamos el envío...
		PeerMessage peerListReq = new PeerMessage(PeerMessageOps.OPCODE_PEER_FILES_REPLY, directoryFiles);
		peerListReq.writeMessageToOutputStream(fos);

		// Simulamos la recepción...
		PeerMessage peerListReply = PeerMessage.readMessageFromInputStream((DataInputStream) fis);

		System.out.println("Código op enviado: " + PeerMessageOps.opcodeToOperation(peerListReq.getOpcode()));
		System.out.println("Código op recibido: " + PeerMessageOps.opcodeToOperation(peerListReply.getOpcode()));

		System.out.println("Lista ficheros enviados: ");
		for (FileInfo f : peerListReq.getPeerFiles()) {
			System.out.println("Fichero: " + f.toString());
		}
		System.out.println("*************************************************************************");

		System.out.println("Lista ficheros recibidos: ");
		for (FileInfo f : peerListReply.getPeerFiles()) {
			System.out.println("Fichero: " + f.toString());
		}
		System.out.println("*************************************************************************");

		/*
		 * *************************************************************************
		 * ENVIO DE FICHEROS
		 *************************************************************************/

		// ===============================================================================================
		// Fase 1.
		// Mandamos un hash para ver si el peer tiene un fichero que coincida con ese
		// hash (o subcadena del hash)
		// y recibimos el hash completo del fichero a descargar (en caso de que exista
		// un único fichero que coincida
		// con la subcadena del hash enviada en la solicitud de descarga)
		// ===============================================================================================
		//  ------------------ CLIENTE ------------------ 
		// Generación del envío de un mensaje de descarga de un fichero a partir de su hash (o subcadena)	
		// Obtenemos dinámicamente los primeros 7 caracteres del hash del primer fichero de tu carpeta
		String subHashPrueba = directoryFiles[0].fileHash.substring(0, 7);
		System.out.println("Solicitando descarga con subHash: " + subHashPrueba);

		PeerMessage fileReqMessage = new PeerMessage(PeerMessageOps.OPCODE_PEER_FILE_DL_REQ, subHashPrueba);
		fileReqMessage.writeMessageToOutputStream(fos);

		// ------------------ SERVIDOR ------------------
		// Simulamos la recepción...
		PeerMessage fileReqMessageLeido = PeerMessage.readMessageFromInputStream((DataInputStream) fis);
		FileInfo[] resultados = FileInfo.lookupHashSubstring(directoryFiles, fileReqMessageLeido.getSubHash());
		FileInfo fichero = null;

		// Revisamos los resultados de la búsqueda del fichero a descargar a partir de
		// la subcadena del hash recibida en el mensaje de descarga
		if (resultados.length == 1) {
			fichero = resultados[0];
			System.out.println("Fichero encontrado: " + fichero.toString());
			nombreFichero = fichero.fileName;

		} else if (resultados.length > 1) {
			System.out.println("Se han encontrado varios ficheros con hash que contiene la subcadena: "
					+ fileReqMessageLeido.getSubHash());
			for (FileInfo f : resultados) {
				System.out.println("Fichero encontrado: " + f.toString());
			}
		} else {
			System.out.println("No se ha encontrado ningún fichero con hash que contenga la subcadena: "
					+ fileReqMessageLeido.getSubHash());
		}

		// Contestamos enviando el Hash completo del fichero a descargar (en un mensaje
		// con opcode PeerMessageOps.OPCODE_PEER_FILE_DL_REPLY) para que el cliente
		// pueda identificarlo de forma unívoca y solicitar su descarga posteriormente.
		PeerMessage fileReply = new PeerMessage(PeerMessageOps.OPCODE_PEER_FILE_DL_REPLY, fichero.fileHash);
		fileReply.writeMessageToOutputStream(fos);
		// ===============================================================================================

		// ===============================================================================================
		// Fase 2.
		// Con el hash completo del fichero a descargar, el cliente puede solicitar la
		// descarga de un fragmento del fichero a partir
		// de un offset y un tamaño (en un mensaje con opcode
		// PeerMessageOps.OPCODE_PEER_FILE_DL_FILE) y el servidor le responde
		// con un mensaje con los datos del fragmento solicitado (con opcode
		// PeerMessageOps.OPCODE_PEER_FILE_DL_DATA).
		// ===============================================================================================
		// ------------------ CLIENTE ------------------
		// Leemos el mensaje de respuesta a la solicitud de descarga del fichero (con
		// opcode PeerMessageOps.OPCODE_PEER_FILE_DL_REPLY)
		PeerMessage fileReplyLeido = PeerMessage.readMessageFromInputStream((DataInputStream) fis);
		System.out.println("Código op recibido: " + fileReplyLeido.getOpcode());
		System.out.println("Hash recibido: " + fileReplyLeido.getSubHash());

		// Ahora con el hash completo, empezamos a pedir datos del fichero a descargar,
		// simulando la descarga de
		// un fichero completo (en un mensaje con opcode
		// PeerMessageOps.OPCODE_PEER_FILE_DL_FILE) y luego la recepción de un mensaje
		// con los datos del fichero (con opcode
		// PeerMessageOps.OPCODE_PEER_FILE_DL_DATA).
		PeerMessage fileDataReqMsg = new PeerMessage(PeerMessageOps.OPCODE_PEER_FILE_DL_FILE,
				fileReplyLeido.getSubHash(), 0, MAX_CHUNK_SIZE);
		fileDataReqMsg.writeMessageToOutputStream(fos);

		// ------------------ SERVIDOR ------------------
		// Simulación de la recepción del mensaje de solicitud de datos del fichero a
		// descargar
		PeerMessage fileDataReqLeido = PeerMessage.readMessageFromInputStream((DataInputStream) fis);
		System.err.println("------------------ SERVIDOR ------------------");
		System.out.println("Código op recibido: " + PeerMessageOps.opcodeToOperation(fileDataReqLeido.getOpcode()));
		System.out.println("Hash recibido:   " + fileDataReqLeido.getSubHash());
		System.out.println("Offset recibido: " + fileDataReqLeido.getOffset());
		System.out.println("Length recibido: " + fileDataReqLeido.getLength());

		// Abrimos el fichero que nos han pedido y leemos el fragmento solicitado a
		// partir del offset y el tamaño indicados en el mensaje de solicitud de datos
		// del fichero a descargar, para luego enviar un mensaje con los datos del
		// fragmento solicitado (con opcode PeerMessageOps.OPCODE_PEER_FILE_DL_DATA).
		RandomAccessFile f = new RandomAccessFile(fichero.filePath, "r");
		int cantidad = (int) Math.min(MAX_CHUNK_SIZE, fichero.fileSize - fileDataReqLeido.getOffset());
		System.out.println("Cantidad a leer: " + cantidad);
		byte[] data = new byte[cantidad];
		f.seek(fileDataReqLeido.getOffset());
		f.readFully(data);
		f.close();

		// Envío de un mensaje con los datos del fichero a descargar
		PeerMessage fileDataMsg = new PeerMessage(PeerMessageOps.OPCODE_PEER_FILE_DL_DATA, data);
		fileDataMsg.writeMessageToOutputStream(fos);

		// ===============================================================================================
		// Fase 3.
		// Simulación de la recepción del mensaje con los datos del fichero a descargar,
		// para luego escribir esos datos en un nuevo fichero para comprobar que se han
		// recibido correctamente.
		//
		// ===============================================================================================

		// ------------------ CLIENTE ------------------
		// simulación de la recepción del mensaje con los datos del fichero a descargar
		PeerMessage fileDataMsg2 = PeerMessage.readMessageFromInputStream((DataInputStream) fis);
		byte[] dataRecibida = fileDataMsg2.getData();
		System.out.println("------------------- CLIENTE ------------------");
		System.out.println("Código op recibido: " + PeerMessageOps.opcodeToOperation(fileDataMsg2.getOpcode()));
		System.out.println("Cantidad de datos recibidos: " + dataRecibida.length);

		// RESPUESTA A LA OBTENCIÓN DEL NOMBRE:
		// Se obtiene a partir del hash buscando en la lista de FileInfo[] previamente
		// recibida.
		// Simulamos esta búsqueda asignando el nombre del primer fichero de nuestra
		// lista 'directoryFiles'
		if (directoryFiles != null && directoryFiles.length > 0) {
			// Le añadimos un prefijo para no sobrescribir el fichero original en la carpeta
			nombreFichero = "test_descargado_" + directoryFiles[0].fileName;
		} else {
			// Fallback de seguridad
			nombreFichero = "fichero_prueba_descargado.bin";
		}

		System.out.println("Nombre del fichero resuelto: " + nombreFichero);

		FileOutputStream fosFichero = new FileOutputStream(nombreFichero);
		fosFichero.write(dataRecibida);
		fosFichero.close();
		System.out.println("Prueba superada: Datos escritos correctamente en el disco local.");

	}
}
