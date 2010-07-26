// TODO: Refactor into proper class hierarchy.

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Hashtable;

public class WebSocketHandshake extends Hashtable<String, Object> implements WebSocketProtocol {

	// INSTANCE PROPERTIES /////////////////////////////////////////////////////
	private static final long serialVersionUID = -280930290679993232L;
	/**
	 * The raw handshake.
	 */
	private byte[] handshake;	
	/**
	 * The WebSocket Internet-Draft version.
	 */
	private Draft handshakeDraft;
	/**
	 * The type of the handshake. The refers to the source of the handshake, either
	 * client or server.
	 */
	private ClientServerType handshakeType;
	
	// CONSTRUCTOR /////////////////////////////////////////////////////////////
	public WebSocketHandshake() {
		super();
	}
	
	public WebSocketHandshake(byte[] handshake) {
		super();
		setHandshake(handshake);
	}
	
	public WebSocketHandshake(byte[] handshake, ClientServerType type, Draft draft) {
		super();
		setDraft(draft);
		setType(type);
		setHandshake(handshake);
	}
		
	// PUBLIC INSTANCE METHODS /////////////////////////////////////////////////	
	public void setDraft(Draft handshakeDraft) {
		this.handshakeDraft = handshakeDraft;
	}
		
	public Draft getDraft() {
		return handshakeDraft;
	}
	
	public void setHandshake(byte[] handshake) {
		this.handshake = handshake;
		if (handshakeDraft != null && handshakeType != null) {
			parseHandshake();
		}
	}

	public byte[] getHandshake() {
		if (handshake == null) {
			buildHandshake();
		}
		return handshake;
	}

	public void setType(ClientServerType handshakeType) {
		this.handshakeType = handshakeType;
	}

	public ClientServerType getType() {
		return handshakeType;
	}
	
	public String toString() {
		return new String(getHandshake(),UTF8_CHARSET);
	}
	
	public String getProperty(String fieldName) {
		if (!containsKey(fieldName)) {
			return null;
		}
		return (String) get(fieldName);
	}
	
	public byte[] getAsByteArray(String fieldName) {
		if (!containsKey(fieldName)) {
			return null;
		}
		return (byte[]) get(fieldName);
	}
	
	public void parseHandshake() {
		
		if (this.handshakeDraft == null) throw new NullPointerException("Handshake draft type must be set before parsing.");
		if (this.handshakeType == null) throw new NullPointerException("Handshake type type must be set before parsing.");
		
		// parse the HTTP request-line or HTTP status-line	
		
		String hs = this.toString();
		String[] requestLines = hs.split("\r\n"); 
		String line = requestLines[0].trim();
		
		if (this.handshakeType == ClientServerType.SERVER) {
			int sp1 = line.indexOf(" ");
			int sp2 = line.indexOf(" ",sp1+1);
			String httpVersion = line.substring(0,sp1);			
			String statusCode = line.substring(sp1+1,sp2);
			String reasonPhrase = line.substring(sp2+1,line.length());
			put("http-version", httpVersion);
			put("status-code", statusCode);
			put("reason-phrase", reasonPhrase);
		}
		
		if (this.handshakeType == ClientServerType.CLIENT) {
			int sp1 = line.indexOf(" ");
			int sp2 = line.indexOf(" ",sp1+1);
			String method = line.substring(0,sp1);			
			String requestURI = line.substring(sp1+1,sp2);
			String httpVersion = line.substring(sp2+1,line.length());
			put("method", method);
			put("request-uri", requestURI);
			put("http-version", httpVersion);
		}
		
		// parse fields
		
		for (int i=1; i<requestLines.length; i++) {
            line = requestLines[i];
            if (line.length() == 0) { break; }
            int firstColon = line.indexOf(":");
            String keyName = line.substring(0, firstColon).trim();
            String keyValue = line.substring(firstColon+1).trim();
            if (this.handshakeDraft == Draft.DRAFT76){			
    			keyName = keyName.toLowerCase();
    		} 
            put(keyName, keyValue);           
        }
		
		// get the key3 bytes
		
		if (this.handshakeDraft == Draft.DRAFT76) {
			int l = getHandshake().length;
			if (this.handshakeType == ClientServerType.CLIENT){
				//get last 8 bytes
				byte[] key3 = {
					getHandshake()[l-8],
					getHandshake()[l-7],
					getHandshake()[l-6],
					getHandshake()[l-5],
					getHandshake()[l-4],
					getHandshake()[l-3],
					getHandshake()[l-2],
					getHandshake()[l-1]
				};
				put("key3", key3);				
			}
			if (this.handshakeType == ClientServerType.SERVER){
				//get last 16 bytes
				byte[] key3 = {
					getHandshake()[l-16],
					getHandshake()[l-15],
					getHandshake()[l-14],
					getHandshake()[l-13],
					getHandshake()[l-12],
					getHandshake()[l-11],
					getHandshake()[l-10],
					getHandshake()[l-9],
					getHandshake()[l-8],
					getHandshake()[l-7],
					getHandshake()[l-6],
					getHandshake()[l-5],
					getHandshake()[l-4],
					getHandshake()[l-3],
					getHandshake()[l-2],
					getHandshake()[l-1]
				};
				put("key3", key3);
			}
		}	
	}
	
	public void buildHandshake() {
		
		if (this.handshakeDraft == null) throw new NullPointerException("Handshake draft type must be set before building.");
		if (this.handshakeType == null) throw new NullPointerException("Handshake type type must be set before building.");
		
		if (this.handshakeType == ClientServerType.SERVER) {
			
			String responseHandshake = "HTTP/1.1 101 Web Socket Protocol Handshake\r\n" +
            						   "Upgrade: WebSocket\r\n" +
            						   "Connection: Upgrade\r\n";
			
			if (this.handshakeDraft == Draft.DRAFT75) {
				responseHandshake += "WebSocket-Origin: " + getProperty("origin") + "\r\n" +
            						 "WebSocket-Location: ws://" + getProperty("host") + getProperty("request-uri") + "\r\n";
				if (containsKey("WebSocket-Protocol")) {
					responseHandshake += "WebSocket-Protocol: " + getProperty("websocket-protocol") + "\r\n";
				}
			}
			
			if (this.handshakeDraft == Draft.DRAFT76) {
				responseHandshake += "Sec-WebSocket-Location: ws://" + getProperty("host") + getProperty("request-uri") + "\r\n" +
									 "Sec-WebSocket-Origin: " + getProperty("origin") + "\r\n";
		        if (containsKey("sec-websocket-protocol")) {
		        	responseHandshake += "Sec-WebSocket-Protocol: " + getProperty("sec-websocket-protocol") + "\r\n";
		        }
				
			}
			
			responseHandshake += "\r\n";
			
			byte[] responseHandshakeBytes = responseHandshake.getBytes(UTF8_CHARSET);
						
	        try{
				ByteArrayOutputStream buff = new ByteArrayOutputStream();
				buff.write(responseHandshakeBytes);
				if (this.handshakeDraft == Draft.DRAFT76) {
					byte[] response = getAsByteArray("response");
					buff.write(response);
				}
				this.handshake = buff.toByteArray();
				return;
			} catch(IOException e) { 
				System.out.print(e);
			};        
			
		}
		
		if (this.handshakeType == ClientServerType.CLIENT) {
			
			String requestHandshake = "GET " + getProperty("request-uri") + " HTTP/1.1\r\n" +
				"Upgrade: WebSocket\r\n" +
				"Connection: Upgrade\r\n" +
				"Host: " + getProperty("host") + "\r\n" +
				"Origin: " + getProperty("origin") + "\r\n";
			
			if (getDraft() == Draft.DRAFT75 && containsKey("websocket-protocol")) {  
				requestHandshake += "WebSocket-Protocol: " + getProperty("websocket-protocol") + "\r\n";
			}
			
			if (getDraft() == Draft.DRAFT76 ) {
				if(containsKey("sec-webSocket-protocol")) { 
					requestHandshake += "Sec-WebSocket-Protocol: " + getProperty("sec-websocket-protocol") + "\r\n";
				}
				requestHandshake += "Sec-WebSocket-Key1: " + getProperty("sec-websocket-key1") + "\r\n";
				requestHandshake += "Sec-WebSocket-Key2: " + getProperty("sec-websocket-key2") + "\r\n";
			}
			
			requestHandshake += "\r\n";
			
			byte[] requestHandshakeBytes = requestHandshake.getBytes(UTF8_CHARSET);
			
	        try{
				ByteArrayOutputStream buff = new ByteArrayOutputStream();
				buff.write(requestHandshakeBytes);
				if (this.handshakeDraft == Draft.DRAFT76) {
					byte[] key3 = getAsByteArray("key3");
					buff.write(key3);
				}
				this.handshake = buff.toByteArray();
				return;
			} catch(IOException e) { 
				System.out.print(e);
			};      
				
		}
		
	}
}
