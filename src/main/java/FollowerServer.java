import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class FollowerServer {
    public static void main(String[] args) {
        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        options.addOption("c", "clientPort", true, "port of client");
        options.addOption("p", "port", true, "port of master");
        options.addOption("h", "host", true, "host of master");
        Integer clientPort = 8087;
        String host = "127.0.0.1";
        Integer port = 8088;
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args);
            if (commandLine.hasOption('c')){
                clientPort = Integer.parseInt(commandLine.getOptionValue('c').trim());
            }
            if (commandLine.hasOption('h')){
                host = commandLine.getOptionValue('h');
            }
            if (commandLine.hasOption('p')){
                port = Integer.parseInt(commandLine.getOptionValue('p').trim());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        SocketServer server = new SocketServer();
        server.startClientService(clientPort);
        server.connectToMaster(host, port);
    }
}
