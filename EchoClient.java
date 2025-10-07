import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class EchoClient {
    public static void main(String arg[])
    {
        String hostip="127.0.0.1";
        int portn=9909;
        String userinput=null;
        try(//try with resources
            Socket cs=new Socket(hostip,portn);
            BufferedReader console=new BufferedReader(new InputStreamReader(System.in));
            PrintWriter clientout=new PrintWriter(cs.getOutputStream(),true);
            BufferedReader clientin=new BufferedReader((new InputStreamReader(cs.getInputStream()))); 
        )
        {
        System.out.println("connected to server at:"+hostip+":"+portn);
        System.out.println("Enter a message. Type 'exit' to quit");
        while((userinput=console.readLine())!=null)
        {
            userinput=userinput.trim();
            clientout.println(userinput);
            if ("exit".equalsIgnoreCase(userinput))
            {
                System.out.println("Exiting client");
                break;
            }
            String serverResponse=clientin.readLine();
            if (serverResponse==null)
            {
                System.out.println("Server closed the connection");
            }
            System.out.println("Serversaid:"+serverResponse);
            
        }
    }
        catch(IOException e)
        {
            System.out.println("IO error occured in client");
        }
        
    }
    
}
