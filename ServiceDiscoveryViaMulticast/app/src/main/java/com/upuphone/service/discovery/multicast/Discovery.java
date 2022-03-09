package com.upuphone.service.discovery.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;


public class Discovery implements Runnable{
    private Thread task = null;
    private boolean running = false;

    boolean start() {
        if(running) {
            return false;
        }

        task = new Thread(this);
        task.start();

        running = true;

        return true;
    }

    boolean stop() {
        if(running) {
            running = false;
            return true;
        }

        return false;
    }

    @Override
    public void run() {
        String ip = "224.2.2.4";
        int port = 2240;

        try {
            InetAddress multicastGroup = InetAddress.getByName(ip);

            MulticastSocket multicastSock = new MulticastSocket(port);
            multicastSock.joinGroup(multicastGroup);  // Join multicast socket group

            byte[] buffer = new byte[8192];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (running) {
                multicastSock.receive(packet);  // Receive datagram packet from multicast sock group

                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Receive datagram packet `" + message + "` from multicast sock group 224.2.2.4:2240");

                Thread.sleep(1000);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
