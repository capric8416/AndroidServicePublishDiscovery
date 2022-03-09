package com.upuphone.service.publish.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class Publish implements Runnable {
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

            while (running) {
                String message = "Publish at " + new java.util.Date();
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, multicastGroup, port);

                multicastSock.send(packet);  // Send datagram packet to multicast sock group
                System.out.println("Send datagram packet `" + message + "` to multicast sock group 224.2.2.4:2240");

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
