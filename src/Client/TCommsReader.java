package Client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Class responsible for reading socket input
 */
public class TCommsReader extends Thread{
    private final Socket comms;
    private final IRegisterMessage parent;
    private final  int ID;

    /**
     *
     * @param accept Communication socket
     * @param ID Communication numeric ID
     * @param registry Interface that messages are reported to
     */
    public TCommsReader(Socket accept,int ID, IRegisterMessage registry) {
    comms = accept;
    parent = registry;
    this.ID = ID;
    }

    /**
     * Reads a single message, updates registry and exits
     */
    @Override
    public void run() {
        try{
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(comms.getInputStream()));
            String inputLine = in.readLine();
            String[] content = inputLine.split("\\|");
            int clientID = Integer.parseInt(content[0]);
            if (clientID!=ID)
                throw new RuntimeException("Got response meant for someone else");
            int requestID = Integer.parseInt(content[1]);
            int server = Integer.parseInt(content[2]);

            String code = content[3];
            if (code.equals("02")){
                int precision = Integer.parseInt(content[4]);
                String result = content[5];
                if (result.length()!= 2+precision){
                    throw new RuntimeException("Result "+result + " does not have "+precision+" decimal places");
                }
                parent.registerResponse(requestID,server,result);
            }
            else if(code.equals("03")){
                parent.registerDecline(requestID,server);
            }
            else{
                throw new RuntimeException("Invalid message code");
            }
            in.close();
            comms.close();
        }catch (Exception e){}
    }
}
