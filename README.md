Money transfer service with Add, Get, Delete, ChangeBalance, TransferMoney operations

One can not delete account if balance is not 0
One can not withdraw to negative balance

Javalin is used as HTTP server.
RocksDb is used as Data Store.

It is possible to create a jar file for launching with "gradlew fatJar".
An only command line argument is a path to config file. If there are no command line arguments, server is
launched with default config file.

API example:

add request:
http://[host]:[port]/accounts/add?account="[serialized json account]"

It returns a json containing account id. This id should be provided for all other server methods.

get request:
http://[host]:[port]/accounts/get?id=[id]

change balance request:
http://[host]:[port]/accounts/changeBalance?id=[accountId]&amount=[amount]

transfer money request:
http://[host]:[port]/accounts/transferMoney?from=[accountId1]&to=[accountId2]&amount=[amount]

delete request:
http://[host]:[port]/accounts/delete?id=[id]