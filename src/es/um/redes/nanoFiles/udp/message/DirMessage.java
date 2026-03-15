package es.um.redes.nanoFiles.udp.message;




/**
 * Clase que modela los mensajes del protocolo de comunicación entre pares para
 * implementar el explorador de ficheros remoto (servidor de ficheros). Estos
 * mensajes son intercambiados entre las clases DirectoryServer y
 * DirectoryConnector, y se codifican como texto en formato "campo:valor".
 * 
 *
 *
 */
public class DirMessage {
	public static final int PACKET_MAX_SIZE = 65507; // 65535 - 8 (UDP header) - 20 (IP header)

	private static final char DELIMITER = ':'; // Define el delimitador
	private static final char END_LINE = '\n'; // Define el carácter de fin de línea

	
	private static final String FIELDNAME_OPERATION = "operation";
	private static final String FIELDNAME_PROTOCOLID = "protocolid";
	
	private String operation = DirMessageOps.OPERATION_INVALID;
	/**
	 * Identificador de protocolo usado, para comprobar compatibilidad del directorio.
	 */
	private String protocolId;
	

	public DirMessage(String op) {
		operation = op;
	}

	
	public String getOperation() {
		return operation;
	}

	
	public void setProtocolID(String protocolIdent) {
		if (!operation.equals(DirMessageOps.OPERATION_PING)) {
			throw new RuntimeException(
					"DirMessage: setProtocolId called for message of unexpected type (" + operation + ")");
		}
		protocolId = protocolIdent;
	}

	public String getProtocolId() {

		return protocolId;
	}

 /**
     * Método para parsear el mensaje línea a ínea,
     * extrayendo para cada línea el nombre del campo y el valor,
     * usando el delimitador DELIMITER, y guardarlo en variables locales
*/


	public static DirMessage fromString(String message) {
		
		String[] lines = message.split(END_LINE + "");
		// Local variables to save data during parsing
		DirMessage m = null;
		System.out.println("DirMessage.fromString()");
		for (String line : lines) {
			int idx = line.indexOf(DELIMITER); // Posición del delimitador
			String fieldName = line.substring(0, idx).toLowerCase(); // minúsculas
			String value = line.substring(idx + 1).trim();
			
			System.out.println(">Linea: "+line);
			switch (fieldName) {
				case FIELDNAME_OPERATION: 
					assert (m == null);
					m = new DirMessage(value);
					break;
				
				case FIELDNAME_PROTOCOLID:
					m.setProtocolID(value);
					break;

			default:
				System.err.println("PANIC: DirMessage.fromString - message with unknown field name " + fieldName);
				System.err.println("Message was:\n" + message);
				System.exit(-1);
			}
		}

		return m;
	}

	/**
	 * Método que devuelve una cadena de caracteres con la codificación del mensaje
	 * según el formato campo:valor, a partir del tipo y los valores almacenados en
	 * los atributos.
	 * 
	 * @return La cadena de caracteres con el mensaje a enviar por el socket.
	 */
	public String toString() {

		StringBuffer sb = new StringBuffer();
		sb.append(FIELDNAME_OPERATION + DELIMITER + operation + END_LINE); // Construimos el campo
		
		// Se ha implementado la concatenación de campos según el Boletín MensajesASCII
		if (getOperation().equals(DirMessageOps.OPERATION_PING))
		{
			sb.append(FIELDNAME_PROTOCOLID+ DELIMITER + getProtocolId() + END_LINE); // Añadimos el protcolo 
		}

		sb.append(END_LINE); // Marcamos el final del mensaje
		return sb.toString();
	}

}
