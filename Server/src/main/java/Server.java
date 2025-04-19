import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.ListView;
/*
 * Clicker: A: I really get it    B: No idea what you are talking about
 * C: kind of following
 */

public class Server{

	int count = 1;	
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	TheServer server;
	private Consumer<Message> callback;
	
	
	Server(Consumer<Message> call){
	
		callback = call;
		server = new TheServer();
		server.start();
	}
	
	
	public class TheServer extends Thread{
		
		public void run() {
		
			try(ServerSocket mysocket = new ServerSocket(5555);){
		    System.out.println("Server is waiting for a client!");
		  
			
		    while(true) {
		
				ClientThread c = new ClientThread(mysocket.accept(), count);
				callback.accept(new Message(count,true));
				clients.add(c);
				c.start();
				
				count++;
				
			    }
			}//end of try
				catch(Exception e) {
					callback.accept(new Message("Server did not launch"));
				}
			}//end of while
		}
	

		class ClientThread extends Thread{
			
		
			Socket connection;
			int count;
			ObjectInputStream in;
			ObjectOutputStream out;
			
			ClientThread(Socket s, int count){
				this.connection = s;
				this.count = count;	
			}
			
			public void updateClients(Message message) {
				switch(message.type){
					case TEXT:
						for(ClientThread t: clients){
							if(message.recipient==-1 || message.recipient==t.count ) {
								try {
									t.out.writeObject(message);
								} catch (Exception e) {
									System.err.println("New User Error");
								}
							}
						}
					break;
					case NEWUSER:
						for(ClientThread t : clients) {
							if(this != t) {
								try {
									t.out.writeObject(message);
								} catch (Exception e) {
									System.err.println("New User Error");
								}
							}
						}
					break;
					case DISCONNECT:
						for(ClientThread t : clients) {
							try {
								t.out.writeObject(message);
							} catch (Exception e) {
								System.err.println("New User Error");
							}
						}

				}

			}
			
			public void run(){
					
				try {
					in = new ObjectInputStream(connection.getInputStream());
					out = new ObjectOutputStream(connection.getOutputStream());
					connection.setTcpNoDelay(true);	
				}
				catch(Exception e) {
					System.out.println("Streams not open");
				}
				
				updateClients(new Message(count,true));
					
				 while(true) {
					    try {
					    	Message data = (Message) in.readObject();
					    	callback.accept(data);
							updateClients(data);
						}
					    catch(Exception e) {
							e.printStackTrace();
							Message discon = new Message(count, false);
					    	callback.accept(discon);
					    	updateClients(discon);
					    	clients.remove(this);
					    	break;
					    }
					}
				}//end of run
			
			
		}//end of client thread
}


	
	

	
