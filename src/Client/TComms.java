package Client;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class responsible for communicating with the load balancer and servers
 */
public class TComms extends Thread implements IRegisterMessage, ISender {

    private final int selfPort;
    private final int outPort;
    private int sent = 0;
    private final ReentrantLock sendlock = new ReentrantLock();
    //private TGUI gui; #TODO: GUI CLASS


    TComms(int selfPort, int outPort) {
        this.selfPort = selfPort;
        this.outPort = outPort;

    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(selfPort);
            while (true) {
                new TCommsReader(serverSocket.accept(),selfPort,this).start();
            }
        } catch (IOException e) {}
    }

    @Override
    public void sendRequest(int precision, int deadline){
        try {
            Socket ext = new Socket("localhost",outPort);
            PrintWriter out = new PrintWriter(ext.getOutputStream());

            sendlock.lock();
            String msg = String.format("%d|%d|00|01|%d|00|%d", selfPort,
                    1000*selfPort+sent,
                    precision,
                    deadline);
            sent++;
            sendlock.unlock();

            out.println(msg);
            out.close();
            ext.close();
        } catch (IOException e) {}
    }

    @Override
    public void registerDecline(int ID, int server){
        //gui.registerDecline(ID,server);#TODO: GUI CLASS
    }
    @Override
    public void registerResponse(int ID,int server, String response){
        //gui.registerResponse(ID,response);#TODO: GUI CLASS
    }
}
