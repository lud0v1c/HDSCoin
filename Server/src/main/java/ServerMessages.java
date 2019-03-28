public class ServerMessages {

    public static void processRegisterResult(boolean flag, int ID){
        System.out.println("###");
        if (flag) { System.out.println("> Client " + ID + " registration succeed"); }
        else { System.out.println("> Client " + ID + " registration error"); }
        System.out.println("###");
    }

    public static void processSendAmountResult(boolean flag, int ID){
        System.out.println("###");
        if (flag) { System.out.println("> Client " + ID + " transaction request succeed"); }
        else { System.out.println("> Client " + ID + " transaction request error"); }
        System.out.println("###");
    }

    public static void processCheckAccountResult(boolean flag, int ID){
        System.out.println("###");
        if (flag) { System.out.println("> Client " + ID + " balance info request succeed"); }
        else { System.out.println("> Client " + ID + " balance info request error : " +
                "Client with ID " + ID + " doesn't exist"); }
        System.out.println("###");
    }

    public static void processReceiveAmountResult(boolean flag, int ID){
        System.out.println("###");
        if (flag) { System.out.println("> Client " + ID + " transaction confirmation succeed"); }
        else { System.out.println("> Client " + ID + " transaction confirmation error"); }
        System.out.println("###");
    }

    public static void processAuditResult(boolean flag, int ID){
        System.out.println("###");
        if (flag) { System.out.println("> Client " + ID + " transaction info request succeed"); }
        else { System.out.println("> Client " + ID + " transaction info request error : " +
                "Client with ID " + ID + " doesn't exist"); }
        System.out.println("###");
    }

    public static void printFakeLedger(String command, String ledger, int ID) {
        System.out.println("############################################################################");
        System.out.println("> [" + command + "]  Client " + ID + " got tampered " + ledger);
        System.out.println("############################################################################");
    }

    public static void printFakeTransactionList(String transactionList, int ID) {
        System.out.println("############################################################################");
        System.out.println("> Client " + ID + " got fake " + transactionList);
        System.out.println("############################################################################");
    }

}