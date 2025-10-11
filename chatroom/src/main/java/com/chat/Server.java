package com.chat;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;
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
    private DatabaseManager dbManager;

    //initialize ports,data format and array of client threads
    public Server(int port)
    {
        this.port=port;
        sdf=new SimpleDateFormat("HH:mm:ss");
        al=Collections.synchronizedList(new ArrayList<>());
        try{
            dbManager=new DatabaseManager();
        }
        catch(SQLException e)
        {
            System.err.println("failed to connect to database: "+e.getMessage());
            System.err.println("Server will run without persistence");
        }
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
        finally{
            if(dbManager!=null)
            {
                dbManager.close();
            }
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
    private synchronized boolean broadcast(String message, int senderId)
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
                    else{
                        if(dbManager!=null)
                        {
                            try
                            {dbManager.saveMessage(senderId, c1.userId, m[2], isPrivate);
                            }
                            catch(SQLException e)
                            {
                                display("erro saving message to database: "+e.getMessage());
                            }
                        }
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
            if (dbManager!=null)
            {
                try{
                    dbManager.saveMessage(senderId, null, message, isPrivate);
                }
                catch(SQLException e)
                {
                    display("error saving message to database: "+e.getMessage());
                }
            }
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
            int sessionId=-1;
            for(int i=al.size()-1;i>=0;i--)
            {
                ClientThread c=al.get(i);
                if(c.id==id)
                {
                    clientRemoved=c.getUsername();
                    sessionId=c.sessionId;
                    al.remove(i);
                    break;
                }
            }
            if (dbManager!=null && sessionId !=-1)
            {
                try{
                    dbManager.endSession(sessionId);
                }
                catch(SQLException e)
                {
                    display("Error ending session: "+e.getMessage());
                }
            }
            broadcast(notif + clientRemoved + " has left the chat." +notif,-1);
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
        int userId;
        int sessionId;

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
                if (dbManager != null) {
                    try {
                        userId = dbManager.getUserId(username);
                        sessionId = dbManager.createSession(userId);
                        display("User " + username + " logged in with ID: " + userId);
                        
                        // Check if this is a returning user
                        boolean isFirstLogin = dbManager.isFirstLogin(userId);
                        
                        if (isFirstLogin) {
                            // First time user - show recent broadcast history
                            writeMsg("\n=== Welcome to the chatroom! ===\n");
                            writeMsg("Here's the recent chat history:\n\n");
                            
                            List<DatabaseManager.ChatHistoryMessage> history = 
                                dbManager.getRecentMessages(20);
                            
                            if (!history.isEmpty()) {
                                for (DatabaseManager.ChatHistoryMessage msg : history) {
                                    writeMsg(msg.formatMessage() + "\n");
                                }
                            } else {
                                writeMsg("No previous messages. You're the first one here!\n");
                            }
                            writeMsg("\n=== End of History ===\n\n");
                        } else {
                            // Returning user - show messages since last login
                            writeMsg("\n=== Welcome back, " + username + "! ===\n");
                            
                            List<DatabaseManager.ChatHistoryMessage> missedMessages = 
                                dbManager.getMessagesSinceLastLogin(userId);
                            
                            if (!missedMessages.isEmpty()) {
                                writeMsg("You have " + missedMessages.size() + " message(s) since your last visit:\n\n");
                                for (DatabaseManager.ChatHistoryMessage msg : missedMessages) {
                                    writeMsg(msg.formatMessage() + "\n");
                                }
                                writeMsg("\n=== You're all caught up! ===\n\n");
                            } else {
                                writeMsg("No new messages since your last visit.\n\n");
                            }
                        }
                    } catch (SQLException e) {
                        display("Database error for user " + username + ": " + e.getMessage());
                        userId = -1;
                    }
                }
                
                broadcast(notif + username + " has joined the chat." + notif, userId);
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
                    boolean confirmation=broadcast(username+": "+message,userId);
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
                case ChatMessage.WHOISHERE:
                    writeMsg("List of the users connected at current time:" +sdf.format(new Date())+"\n");
                    for(int i=0;i<al.size(); i++)
                    {
                        ClientThread ct=al.get(i);
                        writeMsg((i+1) +") "+ ct.username+ " since "+ ct.date);
                    }
                    break;
                case ChatMessage.MYHISTORY:
                    if (dbManager != null && userId != -1) {
                            try {
                                writeMsg("\n=== Your Personal Chat History (Last 50 messages) ===\n");
                                List<DatabaseManager.ChatHistoryMessage> personalHistory = 
                                    dbManager.getUserRelevantMessages(userId, 50);
                                
                                if (!personalHistory.isEmpty()) {
                                    for (DatabaseManager.ChatHistoryMessage msg : personalHistory) {
                                        writeMsg(msg.formatMessage() + "\n");
                                    }
                                } else {
                                    writeMsg("No chat history found.\n");
                                }
                                writeMsg("=== End of History ===\n\n");
                            } catch (SQLException e) {
                                writeMsg("Error retrieving your history: " + e.getMessage() + "\n");
                            }
                        } else {
                            writeMsg("History feature not available (database not connected)\n");
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
    