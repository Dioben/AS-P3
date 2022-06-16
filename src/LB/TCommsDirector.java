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
        if (port!=-1){
            try{
                Socket redirect = new Socket("localhost",port);
                PrintWriter out = new PrintWriter(redirect.getOutputStream(), true);
                out.println(inputLine);
                redirect.close();
                out.close();
                String[] split = inputLine.split("\\|");
                src.reportDispatchToMonitor(split[1],port);
            } catch (Exception e) {}
        }else{
            try{
                Socket cancel = new Socket("localhost",port); // TODO: get client port
                PrintWriter out = new PrintWriter(cancel.getOutputStream(), true);
                String[] data = inputLine.split("\\|");
                data[2] = "-1";
                data[3] = "03";
                out.println(String.join("|",data));
                out.close();
                cancel.close();
                src.reportCancelToMonitor(data[1]);
            } catch (Exception e) {}
        }



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
    return port;

    }

}
