public class ClientMessages {

    public static void processRegister(boolean flag, int ID){
        if (flag){
            System.out.println("Registration succeeded");
            return;
        }else{
            System.out.println("Registration error : Client with ID " + ID + " already exists  or message was tampered");
        }

    }

    public static boolean processSendAmountArgs(String[] args){
        if (args.length != 3) {
            System.out.println("Transaction request error : Must provide destination client id and amount to transfer.\n" +
                    "For more details type \"help\" command");
            return false;
        }

        if (Integer.parseInt(args[2]) <= 0) {
            System.out.println("Transaction request error : Provide a positive amount");
            return false;
        }
        return true;

    }

    public static void processSendAmount(boolean flag){
        if (flag) {
            System.out.println("Transaction request succeeded");
        }
        else{
            System.out.println("Transaction request error : Transaction was not authorized.");
        }

    }

    public static boolean processReceiveAmountArgs(String[] args){
        if (args.length != 2) {
            System.out.println("Transaction confirmation error : Must provide transaction id. For more details\n" +
                    "type \"help\" command");
            return false;
        }
        return true;

    }

    public static void processCheckAccount(Object[] result, int ID){
        if (result != null) {
            System.out.println("### Client " + ID + " ###");
            System.out.print(((Ledger)result[0]).toString(true, false));
            System.out.print(result[1] != null ? ((TransactionList)result[1]).toString(true) : "");
        }
        else { System.out.println("Balance info request error : Client with ID " + ID + " doesn't exists"); }
    }


    public static void processReceiveAmount(boolean flag, String ID){
        if (flag) {
            System.out.println("Transaction confirmation succeeded");
        } else { System.out.println("Transaction confirmation error : Transaction with ID " + ID + "\n" +
                "doesn't exist  or message was tampered"); }
    }

    public static void processAudit(Ledger result, int ID){

        if (result != null) {
            System.out.println("### Client " + ID + " ###");
            System.out.print(result.toString(false, true));
        }
        else { System.out.println("Transaction info request error : Client with ID " + ID + " doesn't exists"); }
    }

    public static void processCrash(int port){
        System.out.println("Server in port " + port + " crashed!");
        System.out.println();
    }

}