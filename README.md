# shut_the_box
Relatively basic swing implementation of the pub game "Shut the Box." Written for RIT CSCI-251.

Part 1 was to write a TCP client that communicated with strings using a defined protocol. The files from that are in a separate directory under src/part1  

Part 2 was to implement both the server and the client using a different communication protocol (network and/or API), so I decided to pursue using TCP with Objects.

Both parts were written in Java for Java 7.

---------------------------------------------------------------

The server program must be run by typing this command line:

java PubDiceServer host port

    <host> is the host name or IP address of the server.
    <port> is the port number of the server. 

---------------------------------------------------------------

The client program must be run by typing this command line:

java PubDice host port playername

    <host> is the host name or IP address of the server.
    <port> is the port number of the server.
    <playername> is the name of the player. The player name must not include any whitespace. 

