#dbcp-conn-log

##Quick start
This is a small project/hack that uses AspectJ to track Tomcat's DBCP connections fate. It just prints in the stdout a line each time a connection is requested from the pool and when the connection is closed (returned to the pool). The outpout looks like this:
```
+++ getConnection(52d02201): MyDAOSQL.getConnection(69) > MyDAOSQL.getCustomerByName(568) > ...
--- retConnection(52d02201): MyDAOSQL.getCustomerByName(568) > CustomerController.getCustomer(67) > ...
+++ getConnection(52d02201): MyDAOSQL.getConnection(69) > MyDAOSQL.getBasket(568) > ...
--- retConnection(52d02201): MyDAOSQL.getBasket(568) > CustomerController.getBasket(67) > ...
```

In the sample above, all connections have matching getConnection and retConnection entries, which means that all is workign well. It is usually very easy to spot a connection that is not closing by the lack of a matching **retConnection**. The brief call trace tells you which class/method is responsible for opening a connection.

**Log format**
```
+++|--- get|retConnection(CONNECTION_ID):  ClassName.Method(line) > ClassName.Method(line)
```
The connection id is that of the wrappe that DBCP uses and not the underlying JDBC, so it changes after every request. The wrapped connection may be the same.

###Requirements
* This is project is tested with **apache tomcat 8** only.
* It just weaves two loggin methods around the getConnection and connection.close methods of the DBCP

###How to use it
```
# You will need a tomcat 8 (binary installation) somewhere in your system
# Backup tomcat's original lib/tomcat-dbcp.jar somewhere, this tool replaces it with the logged version

mvn package
cp target/dbcp-conn-log-1.0-SNAPSHOT.jar PATH_TO_TOMCAT_8/lib/tomcat-dbcp.jar

# Restart tomcat
```
You will now be able to see the connections

##More details
###The problem**
Tomcat's DBCP is great but I doesn't come with much logging. If you work in a large application and somewhere, some code leaves a a connection open, your connection pool will run out of connections (removing abandoned is not always efficient). This code may even be from a 3rd party plugin which makes things even more hard to debug.

I needed a very simple way to tell where in my code a connection is opened (acutally borrowed from the pool) and if and where in the code this same connection is closed (actually returned to the pool). My first idea to compile my own DBCP failed as tomcat uses its own version and I wasnot in the mood to compile my own tomcat just for this.

###The solution
Use AspectJ to weave sipmle logging around the methods taht gets and return a connection to the pool. Log an identifier to match connections and a call trace to see where in the code this happens.

###How it is done
* There are two wrapper methods that add logging
* The pom.xml is fetching tomcat's dbcp jar from maven repo
* AspectJ plugin is weaving the loging around the target methonds and repackes the tomcat-dbcp.ar
* Replace original tomcat-dbcp.jar with the one provided

###Configuration
There is very little cofiguration for this software and it is done via environment variables

| VARIABLE         | Values        | Default | Description  |
| ---------------- |---------------| ----- |----- |
| DBCPLOG_OFF      | false/true    | false |Turn off logging. Default is false, which means logging is active |
| NO_PACKAGE_NAMES | true/false | true |Do not print package names in the stack trace, only ClassSimpleName.Method(line) |
| EXC_DBCP_PACKAGE | true/false | true |Do not print any DBCP internal calls. Recommended |
| MAX_TRACE        | ingeger    | 5 |The number of calls to print. You usually don't need more than 5 |

###Building
The only thing you need to do is run **mvn package**. Maven aspectj plugin will do the rest.

###Deploying
You will need to replace tomcat's lig/tomcat-dbcp.jar with the one this tool builds

###Modifying
The code is very very short. If you want to change the logging format or e.g. use somethig like slf4j just modify it

##Conclusion
This does not look like a tool worth publishing on github but for me it ended a 3 day & 3 night hunt for a runaway JDBC connection. Using this hack, I found a third party plugin that was leaving one connnection open in some specific circumstances.

If there is another out of the box way of doing this and my whole effort was pointless, please let me know!





