package sample.Network;

import javafx.application.Platform;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import sample.Controllers.Chat;
import sample.Controllers.Controller;
import sample.DataInstances.ChatMessages;
import sample.DataInstances.Notifications;
import sample.DataInstances.Screenshare;
import sample.DataInstances.TransferObject;
import tray.animations.AnimationType;
import tray.notification.TrayNotification;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.prefs.Preferences;

public class Server {

    private ServerSocket server;
    private Socket connection;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    private static final String LOCALHOST = "127.0.0.1";
    public static int PORT = 26979;

    private boolean connected;
    private boolean searching = false;
    private boolean isHost = true;   
    
    public boolean isHost() {
        return isHost;
    }


    private TransferObject transferObject;
    private WritableImage image;

    private static Server instance = null;

    private Server(){}

    public static Server getInstance(){
        if(instance == null){
            instance = new Server();
        }
        return instance;
    }

    // ===== SET ROLE =====
    public void setHost(boolean host){
        this.isHost = host;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isSearching() {
        return searching;
    }

    public void setConnection(){
        searching = true;

        new Thread(() -> {
            try {
                System.out.println(isHost ? "HOST started" : "CLIENT started");

                if(isHost){
                    // ===== HOST =====
                    if(server != null){
                        server.close();
                    }
                    server = new ServerSocket(PORT, 100);
                    connection = server.accept();

                } else {
                    // ===== CLIENT (RETRY UNTIL HOST READY) =====
                    boolean connectedFlag = false;
                    while(!connectedFlag){
                        try{
                            connection = new Socket(LOCALHOST, PORT);
                            connectedFlag = true;
                        } catch (IOException e){
                            System.out.println("Waiting for host...");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ignored){}
                        }
                    }
                }

                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());

                connected = true;
                searching = false;

                System.out.println("connected");

                Platform.runLater(() ->
                        Controller.getInstance()
                                .setConnectionButtonText("Connected")
                );

                getInput();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


    public void send(TransferObject ob) {
        try {
            out.writeObject(ob);
            out.reset();
        } catch (IOException e){
            connected = false;
        }
    }

    private void getInput(){
        new Thread(() -> {
            while(connected){
                try{
                    transferObject = (TransferObject) in.readUnshared();

                    if(transferObject.getState() == 1){
                        image = transferObject.getImage();
                        Screenshare.getInstance().setImage(image);

                    }
//                    else if(transferObject.getState() == 2){
//                        Preferences preferences = Preferences.userNodeForPackage(Controller.class);
//                        String destinationFolder = preferences.get(
//                                "downloadDirectory","G:\\JavaFX\\New folder\\"
//                        );
//
//                        File dstfile = new File(
//                                destinationFolder + "\\" + transferObject.getFilename()
//                        );
//
//                        FileOutputStream fos = new FileOutputStream(dstfile);
//                        fos.write(transferObject.getFileData());
//                        fos.close();
//
//                        Platform.runLater(() -> {
//                            TrayNotification tray = new TrayNotification();
//                            tray.setTitle("New File");
//                            tray.setMessage("New file was downloaded");
//                            tray.setRectangleFill(Paint.valueOf("#2A9A84"));
//                            tray.setAnimationType(AnimationType.POPUP);
//                            tray.showAndDismiss(Duration.seconds(2));
//                            Notifications.getInstance()
//                                    .addData("New file was downloaded");
//                        });
//
//                    }
                    else if(transferObject.getState() == 3){
                        String message = transferObject.getString();
                        ChatMessages.getInstance().addToList("peer", message);

                        Platform.runLater(() -> {
                            if(ChatMessages.getInstance().isActive()){
                                Chat chat = Chat.getInstance();
                                if(chat != null){
                                    chat.addToChat("peer", message);
                                }
                            } else {
                                TrayNotification tray = new TrayNotification();
                                tray.setTitle("New Message");
                                tray.setMessage(message);
                                tray.setRectangleFill(Paint.valueOf("#2A9A84"));
                                tray.setAnimationType(AnimationType.POPUP);
                                tray.showAndDismiss(Duration.seconds(2));
                                Notifications.getInstance()
                                        .addData("You have a new message");
                            }
                        });
                    }

                } catch (IOException | ClassNotFoundException e){
                    connected = false;
                }
            }
        }).start();
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
