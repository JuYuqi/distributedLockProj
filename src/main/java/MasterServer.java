import org.apache.commons.cli.*;

public class MasterServer {
    public static void main(String[] args) {
        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        options.addOption("c", "clientPort", true, "port of client");
        options.addOption("f", "followPort", true, "port of follow");
        Integer clientPort = 8088;
        Integer followPort = 8089;
        try {
            CommandLine commandLine = parser.parse(options, args);
            if (commandLine.hasOption('c')){
                clientPort = Integer.parseInt(commandLine.getOptionValue('c').trim());
            }
            if (commandLine.hasOption('f')){
                followPort = Integer.parseInt(commandLine.getOptionValue('f').trim());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        SocketServer server = new SocketServer();
        server.startClientService(clientPort);
        server.startFollowerService(followPort);
    }
}
