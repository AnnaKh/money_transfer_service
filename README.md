# Description

A money transfer service which supports basic account management (add, get, delete), changing account balance and money transfers between accounts.

# Build

To create a jar file, "gradlew fatJar" should be executed.
To launch an existing jar, "java -jar money_transfer_service-0.1.jar" should be executed.
An only command line argument is path to a config file. If there are no command line arguments, server is
launched with a default config file.

# Main design considerations

### Technologies
For the sake of the test task, lightweight technologies are chosen:
* HTTP server: [Javalin](https://javalin.io/)
* Data store: [RocksDb](https://rocksdb.org/)

However, in a real system, more full-fledged transactional databases should be considered.

### HTTP request processing

Since each request requires some relatively time-consuming operations (access to the database, waiting on locks, etc), we add requests to a queue and execute them asynchronously on a separate thread pool. Once a request is executed, the HTTP server receives a notification via a CompletableFuture object.

### Locks

Synchronization for concurrent access to accounts is achieved with the aid of explicit locks in Java code: when a request execution starts, it at first acquires locks for accounts involved. In order to avoid deadlocks, TransferMoney method always acquires lock in order ascending by account id.

Alternatively, other approaches could be implemented:
* To use database transactions and to rely on their atomicity - i.e. on database locks instead of Java locks. It is a possible approach; however, it might have performance issues.
* To use a graph of dependencies of operations - i.e. to execute an operation after previous operations with same accounts have finished. It is also a possible approach; however, it might result in a bit more complicated code.

# API

### Add account
```sh
$ http://[host]:[port]/accounts/add?account="[serialized json account]"
```
A json containing account id is returned. This id should be provided to all other server methods.

### Get account
```sh
$ http://[host]:[port]/accounts/get?id=[id]
```
A json containing account information is returned.

### Delete account
```sh
http://[host]:[port]/accounts/delete?id=[id]
```
An attempt to delete an account with positive balance will result in an error.

### Change balance
```sh
http://[host]:[port]/accounts/changeBalance?id=[accountId]&amount=[amount]
```

### Transfer
```sh
http://[host]:[port]/accounts/transferMoney?from=[accountId1]&to=[accountId2]&amount=[amount]
```
An attempt to tranfer an amount of money which is greater than an account balance will result in an error.