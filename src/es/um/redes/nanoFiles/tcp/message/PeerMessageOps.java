package es.um.redes.nanoFiles.tcp.message;

import java.util.Map;
import java.util.TreeMap;

public class PeerMessageOps {

	public static final byte OPCODE_INVALID_CODE = 0;

	// Constantes actualizadas para coincidir exactamente con el test del profesor
	public static final byte OPCODE_PEER_FILES_REQ = 1;
	public static final byte OPCODE_PEER_FILES_REPLY = 2;
	public static final byte OPCODE_PEER_FILE_DL_REQ = 3;
	public static final byte OPCODE_PEER_FILE_DL_REPLY = 4;
	public static final byte OPCODE_PEER_FILE_DL_FILE = 5;
	public static final byte OPCODE_PEER_FILE_DL_DATA = 6;

	// Array actualizado con los opcodes en el mismo orden que los strings
	private static final Byte[] _valid_opcodes = { OPCODE_INVALID_CODE,
		OPCODE_PEER_FILES_REQ, OPCODE_PEER_FILES_REPLY, 
		OPCODE_PEER_FILE_DL_REQ, OPCODE_PEER_FILE_DL_REPLY, 
		OPCODE_PEER_FILE_DL_FILE, OPCODE_PEER_FILE_DL_DATA
	};
	
	// Representación textual actualizada
	private static final String[] _valid_operations_str = { "INVALID_OPCODE",
		"PEER_FILES_REQ", "PEER_FILES_REPLY", 
		"PEER_FILE_DL_REQ", "PEER_FILE_DL_REPLY", 
		"PEER_FILE_DL_FILE", "PEER_FILE_DL_DATA"
	};

	private static Map<String, Byte> _operation_to_opcode;
	private static Map<Byte, String> _opcode_to_operation;

	static {
		_operation_to_opcode = new TreeMap<>();
		_opcode_to_operation = new TreeMap<>();
		for (int i = 0; i < _valid_operations_str.length; ++i) {
			_operation_to_opcode.put(_valid_operations_str[i].toLowerCase(), _valid_opcodes[i]);
			_opcode_to_operation.put(_valid_opcodes[i], _valid_operations_str[i]);
		}
	}

	protected static byte operationToOpcode(String opStr) {
		return _operation_to_opcode.getOrDefault(opStr.toLowerCase(), OPCODE_INVALID_CODE);
	}

	public static String opcodeToOperation(byte opcode) {
		return _opcode_to_operation.getOrDefault(opcode, null);
	}
}