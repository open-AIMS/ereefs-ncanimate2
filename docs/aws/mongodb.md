# MongoDB Guide

This guide describes how to connect to a MongoDB
and how to request the database with basic queries.

## Connection to the MongoDB EC2 instance

1. Get the IP address of the MongoDB server.

2. Connect to the server using SSH:

    ```$ ssh -o ServerAliveInterval=60 server-user@XXX.XXX.XXX.XXX```

    The `-o ServerAliveInterval=60` argument is used to keep the SSH connection active.

    If you are using a SSH, you can provide it to the SSH command using the `-i` argument:

    ```$ ssh -i ssh-key.pem -o ServerAliveInterval=60 server-user@XXX.XXX.XXX.XXX```

    **NOTE**: Replace `server-user` with a valid user on the server
        and `XXX.XXX.XXX.XXX` with the MongoDB server IP.

3. Connect to the MongoDB docker image

    ```$ docker exec -it mongodb bash```

4. Connect to the database using the appropriate credential:
    ```
    $ mongo -u db-user -p
    MongoDB shell version v4.2.3
    Enter password: **************
    ```

    **NOTE**: Replace `db-user` with a valid MongoDB user.

5. Connect to the eReefs database
    ```
    > use ereefs
    switched to db ereefs
    ```
6. Request the database with MongoDB queries.


## MongoDB Queries

MongoDB documentation can be found online:
https://docs.mongodb.com/manual/reference/method/

### Examples

Display generic help:
```
> help
```

List the database collections:
```
> show collections
```

**NOTE**: A collection is the MongoDB equivalent of a SQL table.

Display help for a collection:
```
> db.metadata.help()
```

Show a document matching a given ID:
```
> db.metadata.find( { "_id": "products_ncanimate_test" } ).pretty()
```

List all documents matching a regex.

Example, list all document `_id` where the `_id` starts with `products_ncanimate_`:
```
> db.metadata.find( { "_id": /products_ncanimate_.*/ }, { "_id": 1 } )
```

List all the unique values for a given attribute.

Example: list all `definitionId`:
```
> db.metadata.distinct( "definitionId" )
```
