import java.io.IOException;

import net.tootallnate.websocket.WebSocket;
import net.tootallnate.websocket.WebSocketServer;

/**
 * A simple WebSocketServer implementation. Keeps track of a "chatroom".
 */
public class ChatServer extends WebSocketServer {


    public ChatServer(int port, String origin, String subprotocol, Draft draft) {
        super(port,origin,subprotocol,draft);
    }

    public void onClientOpen(WebSocket conn) {
        try {
            this.sendToAll(conn + " entered the room!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println(conn + " entered the room!");
        
    }

    public void onClientClose(WebSocket conn) {
        try {
            this.sendToAll(conn + " has left the room!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println(conn + " has left the room!");
    }

    public void onClientMessage(WebSocket conn, String message) {
        try {
            this.sendToAll(conn + ": " + message);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println(conn + ": " + message);
    }

    public static void main(String[] args) {        
        
        int port = (args.length >= 1) ? Integer.parseInt(args[0]) : 80;
        String origin = (args.length >=2) ? args[1] : null;
        String subprotocol = (args.length >=3) ? args[2] : null;
        Draft draft = (args.length >= 4) ? Draft.valueOf(args[3]) : Draft.AUTO;
        
        ChatServer s = new ChatServer(port,origin,subprotocol,draft);
        s.start();
        System.out.println("ChatServer started on port: " + s.getPort());
        if (origin != null) {
        	System.out.println("ChatServer origin: " + s.getOrigin());
        }
        if (subprotocol != null) {
        	System.out.println("ChatServer subprotocol: " + s.getSubProtocol());
        }
        if (draft != Draft.AUTO) {
        	System.out.println("ChatServer draft: " + s.getDraft().toString());
        }
    }
}
