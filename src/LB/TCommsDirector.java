package LB;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCommsDirector extends Thread{
    private final Socket comms;
    private final IMonitorInfoContact src;

    public TCommsDirector(Socket accept, IMonitorInfoContact provider) {
        comms = accept;
        this.src = provider;
    }

    @Override
    public void run() {
        String inputLine = null;
        try{
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(comms.getInputStream()));
            inputLine = in.readLine();
            in.close();
            comms.close();

        }catch (Exception e){}
        String statuses = src.requestStatusFromMonitor(inputLine);
        int port = getOptimalServer(statuses);

        try{
            Socket redirect = new Socket("localhost",port);
            PrintWriter out = new PrintWriter(redirect.getOutputStream());
            out.println(inputLine);
            redirect.close();
            out.close();
            String[] split = inputLine.split("\\|");
            src.reportDispatchToMonitor(split[1],port);
        } catch (Exception e) {}


    }

    private int getOptimalServer(String statuses) {
    String[] input = statuses.split("\\|");
    int best = 21; //max is 20 anyway
    int port= -1;
    for (int i = 0;i<input.length/2;i++){
        int localLoad = Integer.parseInt(input[i*2+1]);
        if (localLoad<best){
            best = localLoad;
            port = Integer.parseInt(input[i*2]);
        }
    }
    if (port==-1){
        throw new RuntimeException("No servers available or all servers above max load");
    }
    return port;

    }

}