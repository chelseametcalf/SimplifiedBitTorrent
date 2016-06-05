import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author chelsea metcalf
 */
public class Client extends Thread {

    static ArrayList<String> fileNeededList = new ArrayList<String>();
    static List<File> downloadedList = new ArrayList<File>();
    static ArrayList<String> uploadList = new ArrayList<String>();
    static List<File> uploadFileList = new ArrayList<File>();
    static String hostName = "";
    static String downloadNeighbor = "";
    static String uploadNeighbor = "";
    static int serverPortNumber = 0;
    static int uploadPortNumber = 0;
    static int downloadPortNumber = 0;
    static int myPortNumber = 0;
    static String mode = "";

    public Client(String HostName, int PortNumber, String Mode) {
        hostName = HostName;
        serverPortNumber = PortNumber;
        mode = Mode;
    }
    
    public Client(int PortNumber, String Mode) {
        serverPortNumber = PortNumber;
        mode = Mode;
    }
    
    @Override
    public void run() {
        if (mode.equals("D")) {
            try {
                //System.out.println("DO DOWNLOAD");
                sendFileList();
            } catch (IOException | InterruptedException ex) {
                //ex.printStackTrace();
            }
        }
        else if (mode.equals("L")) {
            try {
                waitForFiles();
            } catch (IOException | InterruptedException ex) {
                //ex.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length != 7) {
            System.err.println("Usage: java Client <host name> <host name> <host name> <port number>");
            System.exit(1);
        }

        String serverName = args[0];
        uploadNeighbor = args[1];
        downloadNeighbor = args[2];
        serverPortNumber = Integer.parseInt(args[3]);
        uploadPortNumber = Integer.parseInt(args[4]);
        downloadPortNumber = Integer.parseInt(args[5]);
        myPortNumber = Integer.parseInt(args[6]);
        
        // Initialize - get files from server
        initialPull(serverName, serverPortNumber);

        try {
            Thread.sleep(5000);
            new Client(myPortNumber, "L").start();
            Thread.sleep(5000);
            new Client(downloadNeighbor, downloadPortNumber, "D").start();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        /*Scanner scanner = new Scanner(System.in);
        System.out.print("Press 'q' to quit:\t");
        String input = scanner.nextLine();
        if (input.equals("q")) {
            System.exit(0);
            //new Client(downloadNeighbor, portNumber, "D").start();
        }*/
    }
    
    public static void waitForFiles() throws IOException, InterruptedException {
        try {
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            ServerSocket serverSocket = null;
            Socket sock = null;
            try {
                serverSocket = new ServerSocket(myPortNumber);
                System.out.println("Waiting on port " + myPortNumber);
                try {
                        sock = serverSocket.accept();
                        System.out.println("Accepted connection: " + sock);
                        receiveFILES(sock);
                        /*while (true) {
                            DataInputStream dis = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
                            try {
                                // get number of files being received
                                int numOfFiles = dis.readInt();
                                // read all the files
                                for (int i = 0; i < numOfFiles; i++) {
                                    String filename = dis.readUTF();
                                    long size = dis.readLong();
                                    saveFile(filename, size, dis);
                                }
                            }
                            catch (EOFException e) {
                                break;
                            }
                        }*/
                        /*Thread.sleep(2000);
                        List<File> flist = new ArrayList<File>();
                        flist.add(new File("test.txt"));
                        Socket sendSocket = null;
                        //portNumber++;
                        System.out.println("ADDED TEST.TXT TO SEND ON " + portNumber + "    " + downloadNeighbor);
                        sendSocket = new Socket(downloadNeighbor, portNumber);
                        sendFILES(flist, sendSocket);*/
                    }
                finally {
                    if (bis != null) bis.close();
                    if (sock != null) sock.close();
                }
            }
            finally {
                if (serverSocket != null) serverSocket.close();
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public static void sendFileList() throws IOException, InterruptedException {
        List<File> flist = new ArrayList<File>();
        // convertArrayToFile(arrayList, fileName);
        // pass in files needed array, uploadFileList.txt
        // that's how uploadFileList is created
        flist.add(new File("uploadFileList.txt"));
        boolean connected = false;
        // TO DO
        while (connected == false) {
            try {
                Socket sock = null;
                sock = new Socket(downloadNeighbor, downloadPortNumber);
                connected = true;
                sendFILES(flist, sock);
            }
            catch (Exception e) {
                System.out.println("Trying to download connection... NOT FOUND on port number " + downloadPortNumber);
                Thread.sleep(5000);
                //e.printStackTrace();
            }
        }
    }
    
    public static void initialPull(String hostName, int portNumber) throws IOException {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        Socket sock = null;
        
        boolean connected = false;
        try {
            while (!connected) {
                try {
                    sock = new Socket(hostName, portNumber);
                    connected = true;
                    System.out.println("Connecting...");
                    fileNeededList = convertFileToArray("fileNameList.txt");
                    receiveFILES(sock);
                }
                catch (Exception e) {
                    System.out.println("Connection failed");
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        finally {
          if (fos != null) fos.close();
          if (bos != null) bos.close();
          if (sock != null) sock.close();
        }
    }
    
    public static void receiveFILES(Socket socket) throws IOException, InterruptedException {
        while (true) {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            try {
                // get number of files being received
                int numOfFiles = dis.readInt();
                // read all the files
                for (int i = 0; i < numOfFiles; i++) {
                    String filename = dis.readUTF();
                    long size = dis.readLong();
                    //byte[] buff = new byte[(int)size];
                    File file = new File(filename);
                    //System.out.println("Added: " + filename);
                    if (file.getName().equals("fileNameList.txt")) {
                        saveFile(filename, size, dis);
                    }
                    else if (file.getName().equals("uploadFileList.txt")) {
                        saveFile(filename, size, dis);
                        //sendFileList();
                        uploadList.clear();
                        uploadList = convertFileToArray(filename);
                        for (int j = 0; j < uploadList.size(); j++) {
                            for (int k = 0; k < downloadedList.size(); k++) {
                                if (uploadList.get(j).equals(downloadedList.get(k).getName())) {
                                    System.out.println("uploadFileList in receiveFiles: " + "FileChunks/" + downloadedList.get(k).getName());
                                    uploadFileList.add(new File("FileChunks/" + downloadedList.get(k).getName()));
                                }
                            }
                        }
                        try {
                            Socket sock = null;
                            sock = new Socket(downloadNeighbor, downloadPortNumber);
                            sendFILES(uploadFileList, sock);
                        }
                        catch (Exception e) {
                            System.out.println("Trying to download connection... NOT FOUND on port number " + downloadPortNumber);
                            Thread.sleep(5000);
                            e.printStackTrace();
                        }
                    }
                    else {
                        downloadedList.add(file);
                        fileNeededList.remove(file.getName());
                        saveFile("FileChunks/"+filename, size, dis);
                    }
                }
            }
            catch (EOFException e) {
                break;
            }
        }
    }
    
    public static void saveFile(String filename, long size, DataInputStream dis) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        long total = 0;
        int count = 0;
        byte[] buff = new byte[(int)size];
        while ((total < size) && ((count = dis.read(buff, 0, (int) Math.min(buff.length, size - total))) > 0)) {
            fos.write(buff, 0, count);
            total += count;
        }
        fos.close();
        System.out.println("Received File: " + filename + " (" + size + " bytes)");
    }
    
    public static void sendFILES(List<File> files, Socket socket) throws IOException {
        FileInputStream fis = null;
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        long fileSize;
        dos.writeInt(files.size());

        // send every file in list
        for (File file : files) {
            int bytesRead = 0;
            fis = new FileInputStream(file);
            // send filename                        
            dos.writeUTF(file.getName());
            // send filesize (bytes)
            dos.writeLong(fileSize = file.length());
            //System.out.println("File Size: " + fileSize);
            // send file 
            byte[] buff = new byte [(int)file.length()];
            try {
                while ((bytesRead = fis.read(buff)) != -1) {
                    dos.write(buff, 0, bytesRead);
                    System.out.println("Sending file: " + file.getName() + " (" + buff.length + " bytes) on port number " + socket.getPort());
                }
                dos.flush();
            } catch (IOException e) {
                System.out.println("IO Error!!");
            }
            // close the filestream
            fis.close();
        }
        System.out.println("Finished sending files");
        dos.close();
        socket.close();
    }
    
    public static void mergeFiles(List<File> files, File into) throws IOException {
        try (BufferedOutputStream mergingStream = new BufferedOutputStream(new FileOutputStream(into))) {
            String fi = "";
            for (File f : files) {
                fi = "FileChunks/" + f.getName();
                File joinedFile = new File(fi);
                //System.out.println(joinedFile);
                Files.copy(joinedFile.toPath(), mergingStream);
            }
        }
    }
    
    public static ArrayList<String> convertFileToArray(String fileName) throws FileNotFoundException {
        Scanner s = new Scanner(new File(fileName));
        ArrayList<String> fList = new ArrayList<String>();
        while (s.hasNextLine()) {
            fList.add(s.nextLine());
        }
        s.close();
        return fList;
    }
}