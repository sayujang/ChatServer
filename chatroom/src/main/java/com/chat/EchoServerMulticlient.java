package com.chat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServerMulticlient {
    public static void main(String arg[])
    {
        System.out.println("Server has started on port 9909");
        try(ServerSocket ss=new ServerSocket(9909))
        {
            while(true)
            {
                Socket clientsocket=ss.accept();//is a bloccking call
                System.out.println("Connected with client having :"+clientsocket.getRemoteSocketAddress());
                //handle each client
                new Thread(new ClientHandler(clientsocket)).start();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
    private static class ClientHandler implements Runnable
    {
        private final Socket socket;
        ClientHandler(Socket socket)
        {
            this.socket=socket;
        }
        public void run()
        {
            System.out.println("The handler has started for :"+socket.getRemoteSocketAddress());//gets ip+port of other side of connection
            // try {
            //     socket.setSoTimeout(5000);
            // } catch (SocketException e) {
            //     e.printStackTrace();
            // } 
            try(
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out=new PrintWriter(socket.getOutputStream(),true)//autoflush
            )
            {
                String line;
                while((line=in.readLine())!=null)//readline() waits if client doesn't close and also not send any message ie has null only when client closes connection
                {
                    line=line.trim();//remove whitespace
                    if ("exit".equalsIgnoreCase(line))
                    {
                        out.println("Goodbye");
                        break;
                    }
                    out.println("Echo:" + line);
                    
                }
                System.out.println("Connection with " + socket.getRemoteSocketAddress() + " closing.");
                
            }
            catch(IOException e)
            {
                System.out.println("IO error with client: "+socket.getRemoteSocketAddress());

            }
            finally{
              try {
                socket.close();
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
        }
    }
    
}
