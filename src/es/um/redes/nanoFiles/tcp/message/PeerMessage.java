package es.um.redes.nanoFiles.tcp.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import es.um.redes.nanoFiles.util.FileInfo;

public class PeerMessage {

	private byte opcode;

	private FileInfo[] fileList;
	private String hash;
	// Atributos requeridos para pedir trozos concretos de ficheros (Fase 2 del test)
	private long offset;
	private int length;
	private byte[] data;

	public PeerMessage() {
		opcode = PeerMessageOps.OPCODE_INVALID_CODE;
	}

	public PeerMessage(byte op) {
		opcode = op;
	}

	// Constructor para enviar un hash o un subhash
	public PeerMessage(byte op, String hash) {
		this.opcode = op;
		this.hash = hash;
	}

	// Constructor para enviar una petición fragmentada con offset
	public PeerMessage(byte op, String hash, long offset, int length) {
		this.opcode = op;
		this.hash = hash;
		this.offset = offset;
		this.length = length;
	}

	// Constructor para enviar la lista de ficheros
	public PeerMessage(byte op, FileInfo[] fileList) {
		this.opcode = op;
		this.fileList = fileList;
	}

	// Constructor para enviar datos crudos
	public PeerMessage(byte op, byte[] data) {
		this.opcode = op;
		this.data = data;
	}

	public byte getOpcode() {
		return opcode;
	}

	// Getters adaptados a los nombres exactos que usa el profesor en su test
	public FileInfo[] getPeerFiles() {
		return fileList;
	}

	public String getSubHash() {
		return hash;
	}

	public long getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}

	public byte[] getData() {
		return data;
	}

	public static PeerMessage readMessageFromInputStream(DataInputStream dis) throws IOException {
		PeerMessage message = new PeerMessage();
		byte opcode = dis.readByte();
		message.opcode = opcode;
		
		switch (opcode) {
		case PeerMessageOps.OPCODE_PEER_FILES_REQ:
			// Sin carga útil
			break;
		case PeerMessageOps.OPCODE_PEER_FILES_REPLY:
			int listLen = dis.readInt();
			byte[] listBytes = new byte[listLen];
			dis.readFully(listBytes);
			message.fileList = FileInfo.deserializeList(listBytes);
			break;
		case PeerMessageOps.OPCODE_PEER_FILE_DL_REQ:
		case PeerMessageOps.OPCODE_PEER_FILE_DL_REPLY:
			// Ambos leen solo un string (subhash o hash completo)
			message.hash = dis.readUTF();
			break;
		case PeerMessageOps.OPCODE_PEER_FILE_DL_FILE:
			// Petición de descarga de un fragmento
			message.hash = dis.readUTF();
			message.offset = dis.readLong();
			message.length = dis.readInt();
			break;
		case PeerMessageOps.OPCODE_PEER_FILE_DL_DATA:
			// Lectura de los datos en bruto
			int dataLen = dis.readInt();
			byte[] dataBytes = new byte[dataLen];
			dis.readFully(dataBytes);
			message.data = dataBytes;
			break;
		default:
			System.err.println("PeerMessage.readMessageFromInputStream doesn't know how to parse this message opcode: "
					+ PeerMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return message;
	}

	public void writeMessageToOutputStream(DataOutputStream dos) throws IOException {
		dos.writeByte(opcode);
		
		switch (opcode) {
		case PeerMessageOps.OPCODE_PEER_FILES_REQ:
			// Sin carga útil
			break;
		case PeerMessageOps.OPCODE_PEER_FILES_REPLY:
			byte[] listBytes = FileInfo.serializeList(fileList);
			dos.writeInt(listBytes.length);
			dos.write(listBytes);
			break;
		case PeerMessageOps.OPCODE_PEER_FILE_DL_REQ:
		case PeerMessageOps.OPCODE_PEER_FILE_DL_REPLY:
			dos.writeUTF(hash);
			break;
		case PeerMessageOps.OPCODE_PEER_FILE_DL_FILE:
			dos.writeUTF(hash);
			dos.writeLong(offset);
			dos.writeInt(length);
			break;
		case PeerMessageOps.OPCODE_PEER_FILE_DL_DATA:
			dos.writeInt(data.length);
			dos.write(data);
			break;
		default:
			System.err.println("PeerMessage.writeMessageToOutputStream found unexpected message opcode " + opcode + "("
					+ PeerMessageOps.opcodeToOperation(opcode) + ")");
		}
	}
}