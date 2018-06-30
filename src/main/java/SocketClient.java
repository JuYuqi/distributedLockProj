import net.sf.json.JSONObject;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class SocketClient {
    Socket socket = null;
    BufferedReader reader;
    BufferedWriter writer;

    public SocketClient(String address, Integer port) throws IOException {
        connectToServer(address, port);
    }

    private boolean connectToServer(String address, Integer port) throws IOException {
        socket = new Socket(address, port);
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();
        reader = new BufferedReader(new InputStreamReader(inputStream));
        writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        String response = getResponse();
        System.out.println("your clientId: " + response);
        return true;
    }

    public void request(String operation) throws IOException {
        writer.write(operation);
        writer.flush();
    }

    private boolean tryLock(String lockName) throws IOException {
        writer.write("tryLock " + lockName);
        writer.flush();
        return Boolean.parseBoolean(getResponse());
    }

    private boolean tryUnLock(String lockName) throws IOException {
        writer.write("tryUnLock " + lockName);
        writer.flush();
        return Boolean.parseBoolean(getResponse());
    }

    private boolean ownLock(String lockName) throws IOException {
        writer.write("ownLock " + lockName);
        writer.flush();
        return Boolean.parseBoolean(getResponse());
    }

    private Map<String, Integer> getLockMap() throws IOException {
        writer.write("getLockMap");
        writer.flush();
        return JSONObject.fromObject(getResponse());
    }

    public String getResponse() throws IOException {
        char[] rBuffer = new char[1024];
        reader.read(rBuffer);
        return new String(rBuffer).trim();
    }

    public static void main(String[] args) {
        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        options.addOption("h", "host", true, "host of server");
        options.addOption("p", "port", true, "port of server");
        try {
            CommandLine commandLine = parser.parse(options, args);
            String host = "127.0.0.1";
            Integer port = 8088;
            if (commandLine.hasOption('h')){
                host = commandLine.getOptionValue('h');
            }
            if (commandLine.hasOption('p')){
                port = Integer.parseInt(commandLine.getOptionValue('p').trim());
            }
            SocketClient socketClient = new SocketClient(host, port);
            BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
            String operation;
            while (true){
                operation = bf.readLine();
                socketClient.writer.write(operation);
                socketClient.writer.flush();
                System.out.println(socketClient.getResponse());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
