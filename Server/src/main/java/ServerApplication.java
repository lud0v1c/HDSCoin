import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.GeneralSecurityException;
import java.util.Scanner;

public class ServerApplication {

    protected static final String appFolderPath = System.getProperty("user.dir") + File.separator;
    private static Server server;
    private static ServerSecurityManager secMan = ServerSecurityManager.getInstance();
    private static ServerCommunicationManager comMan = ServerCommunicationManager.getInstance();
    private static final String keystorePath =
            (new File(System.getProperty("user.dir"))).getParent() +
                    File.separator + "Security/src/main/data/";

    public static void main(String args[]){

        try{
            secMan.loadKeyPair(Integer.parseInt(args[0]),keystorePath, "server");

            int registryPort = Integer.parseInt(args[0]);
            String remoteServerName = "rmi://localhost:" + registryPort + "/HDSCoin-Server";
            ServerApplication.server = new Server(secMan, comMan, registryPort);
            Registry registry = LocateRegistry.createRegistry(registryPort);
            registry.rebind(remoteServerName, ServerApplication.server);

            displayGUI(registryPort);

        }catch (NumberFormatException e){
            System.out.println("Enter a numeric server port");
            System.exit(1);
        }catch (RemoteException e){
            System.out.println("Error connecting with registry");
            System.exit(1);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void displayGUI(int registryPort){

        printHelpCommandOutput();
        System.out.println("Server is operating on port " + registryPort);
        System.out.println("Type in a command...");
        processCommands();

    }

    private static void printHelpCommandOutput(){

        System.out.println("+--------------------------------------------------------------------------+");
        System.out.println("Application Commands\n");
        System.out.println("\tHELP\n\t\tShows current list of available commands\n");
        System.out.println("\t[" + server.getByzantineStatus(0) + "] BYZ_ORD\n\t\tTurns byzantine behaviour - random transaction order - on or off\n");
        System.out.println("\t[" + server.getByzantineStatus(1) + "] BYZ_REM\n\t\tTurns byzantine behaviour - remove last transaction - on or off\n");
        System.out.println("\t[" + server.getByzantineStatus(2) + "] BYZ_SIG\n\t\tTurns byzantine behaviour - fake signatures - on or off\n");
        System.out.println("\t[" + server.getByzantineStatus(3) + "] BYZ_PEN\n\t\tTurns byzantine behaviour - wrong pending transactions - on or off\n");
        System.out.println("\tEXIT\n\t\tExits the program\n");
        System.out.println("+--------------------------------------------------------------------------+\n");

    }

    private static void processCommands() {

        Scanner scan = new Scanner(System.in);
        while (true) {
            String command = scan.nextLine();
            if (command.toLowerCase().equals("exit")) { System.exit(0); }
            if (command.toLowerCase().equals("byz_ord")) { server.setByzantine(0); printHelpCommandOutput(); continue;}
            if (command.toLowerCase().equals("byz_rem")) { server.setByzantine(1); printHelpCommandOutput(); continue;}
            if (command.toLowerCase().equals("byz_sig")) { server.setByzantine(2); printHelpCommandOutput(); continue;}
            if (command.toLowerCase().equals("byz_pen")) { server.setByzantine(3); printHelpCommandOutput(); continue;}
            if (command.toLowerCase().equals("help")) { printHelpCommandOutput(); continue; }
            System.out.println("Invalid command. Try again");
        }
    }

}