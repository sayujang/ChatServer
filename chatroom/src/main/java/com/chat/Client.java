
package com.chat;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private String notif=" *** ";
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private String serverAddr, username;
    private Socket socket; //socket object for client
    private int port;
    private volatile boolean keepListening=false;
    private Thread listenerThread;
    public String getUsername()
    {
        return username;
    }
    public void setUsername(String username)
    {
        this.username=username;
    }
    Client(String serverAddr, int port, String username)
    {
        this.serverAddr=serverAddr;
        this.port=port;
        this.username=username;
    }
    private void display(String msg) {

		System.out.println(msg);
		
	}
    public boolean start()
    {
        try{
            socket=new Socket(serverAddr,port); //trys to connect to server at serversocket.accept()
        }
        catch(Exception e)
        {
            display("Error Connecting to server:"+e.getMessage());
            return false;
        }
        String msg="Connection accepted "+ socket.getInetAddress()+":"+socket.getPort();
        display(msg);
        try{
            
            sOutput=new ObjectOutputStream(socket.getOutputStream());//creates a stream header byte and sends to the tcp send buffer//is a non blocking type
            sOutput.flush();//sends it immediately from the buffer.
            sInput=new ObjectInputStream(socket.getInputStream());//blocks while waiting for header byte from output stream of serrver
        }
        catch(IOException e)
        {
            display("Exception creating new i/o streams:"+e.getMessage());
            // disconnect();
            return false;
        }
        keepListening=true;
        listenerThread=new ListenFromServer();//uses main thread and listener thread to parallely listen to
        // server while the client is sending messages through sInput.readObject()(blocking operation) in sendMessage
        // and so that user can type in the console(scan is also blocking) while simultaneously receive message from server 
        //the only time the client sends a msg in string ie username all others will be in chatmessage
        listenerThread.start();
        try{sOutput.writeObject(username);
        }catch(IOException e)
        {
            display("Exception doing login: "+ e.getMessage());
            disconnect();//frees up resources and closes socket which then allows the thread to exit
            return false;
        }
        return true;
    }
    boolean sendMessage(ChatMessage msg)
    {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {

            display("Exception while writing to server:"+ e.getMessage());
            return false;
        }
        return true;
    }
    private void disconnect()
    {
        keepListening=false;
        try
            {if (sInput !=null) sInput.close();}
            catch(Exception e){}
            try{if (sOutput !=null) sOutput.close();}
            catch(Exception e ){}
            try{
            if(socket!=null) socket.close();}
            catch(Exception e){}
        System.out.println("Connection to server lost");
    }
    public static void main(String arg[])
    {
        int portnumber=7450;
        String serverAddress="localhost";
        String username="Anonymous";
        Scanner scan=new Scanner(System.in);
        System.out.println("Enter username:");
        username=scan.nextLine();
        Client client =new Client(serverAddress, portnumber, username);
        if(!client.start()) return;
        System.out.println("\nHello.! Welcome to the chatroom.");
		System.out.println("Instructions:");
		System.out.println("1. Simply type the message to send broadcast to all active clients");
		System.out.println("2. Type '@username<space>yourmessage' without quotes to send message to desired client");
		System.out.println("3. Type 'WHOISIN' without quotes to see list of active clients");
		System.out.println("4. Type 'LOGOUT' without quotes to logoff from server");
        while(true)
        {
            System.out.print(">");
            String msg=scan.nextLine();//collects user message
            if (msg.equalsIgnoreCase("LOGOUT"))
            {
                if (!client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""))) break;
                break;
            }
            else if(msg.equalsIgnoreCase("WHOISIN"))
            {
                if (!client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""))) break;
                
            }
            else
            {
                if (!client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg))) break;
            }
        }
        scan.close();
        client.disconnect();
    }

    class ListenFromServer extends Thread{
        public void run()
        {

            //runs until the client gets disconnected or clients exits through lOGOUT message
            while(keepListening){
            try {
                String msg=(String)sInput.readObject();
                System.out.println(msg);
                System.out.print(">");
            } catch (ClassNotFoundException e) {e.printStackTrace();
            } catch (IOException e) {
                display(notif+"Server has closed the connection:" +e.getMessage()+notif);
                disconnect();
                break;//for extra safety(optional)
            }
        }
        }
    }

    
    
}