import javafx.util.Pair;
import java.io.File;
import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ClientApplication {

    private static Client client;
    private static ClientCommunicationManager comMan;
    private static ClientSecurityManager secMan = ClientSecurityManager.getInstance();
    private static int clientNum;
    private static final String keystorePath =
            (new File(System.getProperty("user.dir"))).getParent() +
                    File.separator + "Security/src/main/data/";
    protected static List<Integer> serverPorts = new ArrayList<>();
    private static List<RemoteServerInterface> servers = new ArrayList<>(4);

    public static void main(String args[]) {

        try{
            clientNum = Integer.parseInt(args[0]);
            secMan.loadKeyPair(clientNum, keystorePath, "client");

            serverPorts.add(Integer.parseInt(args[1]));
            serverPorts.add(Integer.parseInt(args[2]));
            serverPorts.add(Integer.parseInt(args[3]));
            serverPorts.add(Integer.parseInt(args[4]));


            for (int i = 0; i < serverPorts.size(); i++){
                String remoteServerName = "rmi://localhost:" + serverPorts.get(i) + "/HDSCoin-Server";

                // In case a server crashes, client can still make requests to other servers
                try {
                    Registry registry = LocateRegistry.getRegistry(serverPorts.get(i));
                    servers.add((RemoteServerInterface) registry.lookup(remoteServerName));
                }catch(ConnectException e){
                    serverPorts.remove(i);
                }


            }
            ClientApplication.comMan = ClientCommunicationManager.getInstance(ClientApplication.servers);
            ClientApplication.client = new Client(secMan.getPublicKey(), comMan, secMan);

            displayGUI(serverPorts);

        }catch (NumberFormatException e){
            System.out.println("Enter a numeric server port");
            System.exit(1);
        }catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

    }

    private static void displayGUI(List<Integer> serverPorts) throws GeneralSecurityException, IOException{

        printHelpCommandOutput();
        for (int registryPort : serverPorts){
            System.out.println("Client " + Integer.toString(clientNum) + " connected with server on port " + registryPort);

        }
        System.out.println("Type in a command...");
        processCommands();

    }

    private static void printHelpCommandOutput() {

        System.out.println("+--------------------------------------------------------------------------+");
        System.out.println("Application Commands\n");
        System.out.println("\tREGISTER\n\t\tRegisters client in the system with 100 HDS Coins\n");
        System.out.println("\tSENDAMOUNT <client_id> <amount>\n\t\tSends number of coins specified in <amount> to <client_id>\n");
        System.out.println("\tCHECKACCOUNT <client_id>\n\t\tShows balance of <client_id>. Defaults to current client\n");
        System.out.println("\tRECEIVEAMOUNT <transaction_id>\n\t\tAccepts transaction with <transaction_id>\n");
        System.out.println("\tAUDIT <client_id>\n\t\tReturns history of transactions of <client_id>. Defaults to current\n\t\t" +
                "client\n");
        System.out.println("\tHELP\n\t\tShows current list of available commands\n");
        System.out.println("\tEXIT\n\t\tExits the program\n");
        System.out.println("+--------------------------------------------------------------------------+\n");

    }

    private static void processCommands(){

        Scanner scan = new Scanner(System.in);
        while (true) {
            String command = scan.nextLine();
            if (command.toLowerCase().equals("exit")) { return; }
            if (command.toLowerCase().equals("help")) { printHelpCommandOutput(); continue; }

            String[] args = command.trim().split("\\s+");
            switch (args[0].toLowerCase()) {

                case "register":
                    ClientApplication.processRegister();
                    break;

                case "sendamount":
                    ClientApplication.processSendAmount(args);
                    break;

                case "checkaccount":
                    ClientApplication.processCheckAccount(args);
                    break;

                case "receiveamount":
                    ClientApplication.processReceiveAmount(args);
                    break;

                case "audit":
                    ClientApplication.processAudit(args);
                    break;

                default:
                    System.out.println("Invalid command. Try again");
                    break;
            }
        }
    }

    private static void processRegister() {

        try {
            boolean flag = ClientApplication.client.register(clientNum);
            ClientMessages.processRegister(flag, clientNum);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void processSendAmount(String[] args) {

        if (!ClientMessages.processSendAmountArgs(args)) return;
        try {
            boolean flag = false;
            PublicKey pk = secMan.getClientPubKey(Integer.parseInt(args[1]));
            if (pk != null) {
                flag = ClientApplication.client.sendAmount(new Pair<>(pk, Integer.parseInt(args[1])),
                        clientNum, Integer.parseInt(args[2]));
            }
            ClientMessages.processSendAmount(flag);
        } catch (NumberFormatException e) {
            System.out.println("Transaction request error : Enter a numeric client id and a numeric amount");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void processCheckAccount(String[] args) {

        Object[] result;
        if (args.length == 1){
            try {
                result = ClientApplication.client.checkAccount();
                ClientMessages.processCheckAccount(result,clientNum);
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else if (args.length == 2) {
            try {
                PublicKey pk = secMan.getClientPubKey(Integer.parseInt(args[1]));
                if (pk != null) {
                    result = ClientApplication.client.checkAccount(pk);
                } else {
                    result = null;
                }
                ClientMessages.processCheckAccount(result,Integer.parseInt(args[1]));
            }catch (NumberFormatException e){
                System.out.println("Balance info request error : Enter a numeric client id");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    private static void processReceiveAmount(String[] args) {

        if (!ClientMessages.processReceiveAmountArgs(args)) return;
        try {
            boolean flag = ClientApplication.client.receiveAmount(args[1]);
            ClientMessages.processReceiveAmount(flag, args[1]);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processAudit(String[] args) {

        Ledger result;
        if (args.length == 1){
            try {
                result = ClientApplication.client.audit();
                ClientMessages.processAudit(result, clientNum);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (args.length == 2) {
            try {
                PublicKey pk = secMan.getClientPubKey(Integer.parseInt(args[1]));
                if (pk != null) {
                    result = ClientApplication.client.audit(pk);
                } else {
                    result = null;
                }
                ClientMessages.processAudit(result, Integer.parseInt(args[1]));
            }catch (NumberFormatException e){
                System.out.println("Balance info request error : Enter a numeric client id");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}