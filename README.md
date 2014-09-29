BitCoinGeneration
=================

This is used to generate BitCoins using SHA-256 and according to user requirements. Server and Client share workload to do this task.

The File Project1.scala contains code to generate Bitcoins by applying SHA-256 to randomly generated strings.

File CommonClasses.scala contains code for all common classes used by Server and Client in the application.

Server.scala contains code for generating bitcoins and in addition when a client requests some work by pinging Server, Server sends
some inputs so that Client can generate Bitcoins.

Client.scala contains code for generating bitcoins by sending TCP request to Server and asking for work. Once Server sends inputs, client
will generate bitcoins and send them back to server which Server finally prints out.

