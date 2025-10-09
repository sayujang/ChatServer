package com.chat;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Server {
    //belongs to Server and incremented when a new thread is created
    private static int uniqueId;
    //final prevents unnecessary reassignments
    private final List<ClientThread> al;
    private SimpleDateFormat sdf;
    //port server listens to
    private int port;
    private boolean keepGoing;
    private String notif =" *** ";

    //initialize ports,data format and array of client threads
    public Server(int port)
    {
        this.port=port;
        sdf=new SimpleDateFormat("HH:mm:ss");
        al=Collections.synchronizedList(new ArrayList<>());
    }

    //start the server
    public void start()
    {
        keepGoing=true;
        try(ServerSocket serversocket=new ServerSocket(port);)
        {
            while(keepGoing)
            {
                display("Server waiting for clients on port" + port + ".");
                Socket socket =serversocket.accept();//
                if(!keepGoing)break;
                ClientThread t=new ClientThread(socket);
                al.add(t);
                t.start();
            }
            try{
                for(ClientThread tc : al)
                {
                    tc.sInput.close();
                    tc.sOutput.close();
                    tc.socket.close();
                }
            }
            catch(IOException e)
            {
                System.out.println("Error closing the server and clients");
            }

        }
        catch(IOException e)
        {
            String msg=sdf.format(new Date())+"Exception on new Serversocket: " +e;
            display(msg);
        }
    }
    protected void stop()
    {
        keepGoing=false;
        try ( Socket a=new Socket("localhost",port);){
           
        }
         catch (Exception e) {
            e.printStackTrace();
        }
    }
    //displays with current time 
    private void display(String msg)
    {
        String message=sdf.format(new Date())+" "+msg;
        System.out.println(message);
    }

    //automatically decides whether to broadcast or send private message
    private synchronized boolean broadcast(String message)
    {
        String time=sdf.format(new Date());
        String[] m=message.split(" ", 3);
        boolean isPrivate=false;
        if (m[1].charAt(0)=='@')
            isPrivate=true;
        
        // Ram: @Sita Hallo
        if (isPrivate==true)
        {
            String receiverName=m[1].substring(1,m[1].length());
            boolean userFound=false;
            String msg=time+" "+ m[0]+m[2]+"\n";

            for(int i=al.size()-1;i>=0;i--)
            {
                ClientThread c1=al.get(i);
                String receiverCheck=c1.getUsername();
                if(receiverName.equals(receiverCheck))
                {
                    if(!c1.writeMsg(msg))
                    {
                        al.remove(i);
                        display("client got disconnected: "+ c1.username +" removed from the list.");

                    }
                    userFound=true;
                    break;
                }
            }
            if(userFound!=true) return false;
            


        }
        else
        {
            String msg=time + " " + message + "\n";
            System.out.print(msg);
            for(int i=al.size()-1; i>=0;i--)
            {
                ClientThread c1=al.get(i);
                if(!c1.writeMsg(msg))
                {
                    al.remove(i);
                    display("Client disconnected: "+ c1.username+ " removed from list.");
                }
            }
        }
        return true;
    }

        synchronized void remove(int id)
        {
            String clientRemoved="";
            for(int i=al.size()-1;i>=0;i--)
            {
                ClientThread c=al.get(i);
                if(c.id==id)
                {
                    clientRemoved=c.getUsername();
                    al.remove(i);
                    break;
                }
            }
            broadcast(notif + clientRemoved + " has left the chat." +notif);
        }
    

    public static void main(String arg[])
    {
        int portNum = 7450;
        Server server=new Server(portNum);
        server.start();


    }
    //one thread per client
    class ClientThread extends Thread{
        Socket socket;//store reference var of socket ie client specific
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;//unique id for each client
        String username;//clients username
        ChatMessage cm;
        String date;// to show joined date of active clients

        //Constructor:handles date register and receiving username in string from client
        ClientThread(Socket socket)
        {
            this.socket=socket;
            id=++uniqueId;
            System.out.println("Thread is trying to create object Input/output streams");
            try{
                //order of creation: reverse of client
                sOutput=new ObjectOutputStream(socket.getOutputStream());//order matters for preventing deadlocks
                sOutput.flush();
                sInput=new ObjectInputStream(socket.getInputStream());
                username=(String)sInput.readObject();
                broadcast(notif + username +" has joined the chat." +notif);
            }
            catch(IOException e)
            {
                display("Exception trying to create object Input/output streams");
            }
            catch(ClassNotFoundException e){}
            date=new Date().toString()+ "\n";

        }
        public String getUsername()
        {
            return username;
        }
        public void setUsername(String username)
        {
            this.username=username;
        }

        //handles ChatMessages: like MESSAGE, WHOISIN, LOGOUT
        public void run()//runs when  new ClientThread.start() in start() method of Server
        {
            boolean keepGoing=true;
            while(keepGoing)
            {
                try
                {cm=(ChatMessage)sInput.readObject();}
                catch(IOException e)
                {
                    display("Exception while reading streams");
                    break;
                }
                catch(ClassNotFoundException e)
                {
                    break;
                }
                String message=cm.getMessage();
                switch(cm.getType()) //handle different type of messages
                {
                    case ChatMessage.MESSAGE: //simple broadcast or chat message
                    boolean confirmation=broadcast(username+": "+message);
                    if (!confirmation)
                    {
                        String msg=notif +"Sorry. No user exists."+notif;
                        writeMsg(msg);
                    }
                    break;
                case ChatMessage.LOGOUT: //client tries to logout
                    display(username + " disconnected with a LOGOUT message.");//print in server console
                    keepGoing=false;
                    break;
                case ChatMessage.WHOISIN:
                    writeMsg("List of the users connected at current time:" +sdf.format(new Date())+"\n");
                    for(int i=0;i<al.size(); i++)
                    {
                        ClientThread ct=al.get(i);
                        writeMsg((i+1) +") "+ ct.username+ " since "+ ct.date);
                    }
                    break;
                    default:
                        break;
                }
            }
            remove(id);
            close();
        }
        //function to write messages to clients
        //string message is sent by server when server gets WHOISIN or when message is not sent
        boolean writeMsg(String msg)
        {
            if(!socket.isConnected())
                {close();
                return false;}
            try{sOutput.writeObject(msg);}//writeObject is a blocking operation
            catch(IOException e){display(notif +"Error sending message to "+username +notif);}
            return true;

        }
        private void close()
        {
            //only close if streams exist 
            try
            {if (sOutput !=null) sOutput.close();}
            catch(Exception e){e.printStackTrace();}
            try{if (sInput !=null) sInput.close();}
            catch(Exception e ){e.printStackTrace();}
            try{
            if(socket!=null) socket.close();}
            catch(Exception e){e.printStackTrace();}
        }





    }
}
    