## HDSCoin
College project for the course of Highly Dependable Systems 2018/19 at Instituto Superior Técnico. HDSCoin is essentially virtual currency, fully distributed and prepared to handle byzantine faults, inspired by cryptocurrencies. Users create addresses that start with a certain balance, and can send and receive coins between themselves. Everything is stored on a server side ledger that servers maintain and verify its integrity. It runs a distributed server-client architecture, where the servers are replicated and implement a Byzantine Memory Register to avoid faults.<br/>
Below is the demo that was written for the final evaluation.

## Demo
### Requirements
This project was built using Java 8 and Maven 4.0.
### Setup
For this demo you will need to open 4 terminal windows: 1 server + 3 clients

First terminal window - Server:
1. Navigate to HDSCoin
2. Run the following command:   ``$> mvn install``
3. Navigate to HDSCoin/Server/
4. This window will be your Server
5. To start the server run: ``$> mvn exec:java@first-execution``

Second terminal window - Client 1:
1. Navigate to HDSCoin/Client/
2. To start the first client run: ``$> mvn exec:java@first-execution``
3. This window will be your client 1

Third terminal window - Client 2:
1. Navigate to HDSCoin/Client/
2. To start the second client run: ``$> mvn exec:java@second-execution``
3. This window will be your client 2

Forth terminal window - Client 3:
1. Navigate to HDSCoin/Client/
2. To start the third client run: ``$> mvn exec:java@third-execution``
3. This window will be your client 3

### Use Case
First, using your file explorer, navigate to HDSCoin/Client/src/main/data and noticed that were create 6 files
corresponding to the public and private key of all clients;

Now let's register the clients in our system

In client 1's terminal window type:

``$> register``

Now using your file explorer, navigate to HDSCoin/Server/src/main/data and notice
that 4 files were created: a file called "1", which is the ledger of client 1; 
"clientAccounts.ser", which is the serialization of the list containing the registered clients in the system
; "clientMap.ser", which is the serialization of the map of public key and client number
and finally the file "clientNonces.ser", which is the serialization of the map of clients and which nonce were used by
the client.

Repeat the previous command in client 2's and client 3's terminal window.

Now let's try to make some transactions

Client 1
- ``$> sendamount 2 10`` .This will send 10 coins to client 2.
- ``$> sendamount 3 15`` .This will send 15 coins to client 3.
- ``$> audit`` .Now this command will show 2 completed transactions of client 1.
- ``$> checkaccount``.Now check that client 1's balance is 75;

Client 2
- ```$> checkaccount``` .Now you will see a pending transaction from client 1.
- ``$> receiveamount <transcation-id>`` .Run this command to accept transaction.
- ``$> checkaccount`` .This will show a balance of 110.
- ``$> audit`` .This will show 1 completed transaction.

Client 3
- ``$> checkaccount`` .Now you will see a pending transaction from client 1.
- ``$> receiveamount <transcation-id>`` .Run this command to accept transaction.
- ``$> checkaccount`` .This will show a balance of 125.
- ``$> audit`` .This will show 1 completed transaction.

Repeat the above steps with more transactions if you which.

Now let's see how persistence work in our project

1. Type exit in all terminal windows open.
2. Re-run the mvn commands to start both server and clients.
3. For example in client 1's window run: ``$> register`` .Notice that the message 
"Registration error : Client with ID 1 already exists  or message was tampered" appears. In this case the client was
already registered in the system. The message can also appear in case of an attack.
4. Run: ``$> audit`` and check that the transactions are the same and have the same ID as before turning off the system.
5. Now, if you wish, you can try to do more transactions and see that the system works like before

If you want to start over with fresh clients, navigate to HDSCoin/Server/src/main/data and 
to HDSCoin/Client/src/main/data and erase all files in both folders.

We hope you enjoy our project and feel free to test more stuff.