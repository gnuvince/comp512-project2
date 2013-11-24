BINDIR=make-bin

SERVERHOST=0.0.0.0
SERVERPORT=5005

CARHOST=0.0.0.0
CARPORT=5006

FLIGHTHOST=0.0.0.0
FLIGHTPORT=5007

HOTELHOST=0.0.0.0
HOTELPORT=5008

all: compile jarfile 

compile: 
	mkdir -p /tmp/Group5
	find src -name '*.java' -print0 | xargs -0 javac -g -d $(BINDIR) -cp $(BINDIR)

lockcode:
	mkdir -p $(BINDIR)
	javac -d $(BINDIR) -cp $(BINDIR) src/LockManager/*.java

servercode:
	mkdir -p $(BINDIR)
	javac -g -d $(BINDIR) -cp $(BINDIR) src/servercode/ResImpl/TransactionAbortedException.java
	javac -g -d $(BINDIR) -cp $(BINDIR) src/servercode/ResImpl/InvalidTransactionException.java
	javac -g -d $(BINDIR) -cp $(BINDIR) src/servercode/ResImpl/RMItem.java
	javac -g -d $(BINDIR) -cp $(BINDIR) src/servercode/ResImpl/ReservedItem.java
	javac -g -d $(BINDIR) -cp $(BINDIR) src/servercode/ResImpl/ReservableItem.java
	javac -g -d $(BINDIR) -cp $(BINDIR) src/servercode/ResInterface/*.java
	javac -g -d $(BINDIR) -cp $(BINDIR) src/servercode/ResImpl/*.java

clientcode:
	mkdir -p $(BINDIR)	
	javac -g -d $(BINDIR) -cp $(BINDIR) src/clientcode/*.java

carcode:
	mkdir -p $(BINDIR)
	javac -g -d $(BINDIR) -cp $(BINDIR) src/carcode/ResImpl/*.java
	
flightcode:
	mkdir -p $(BINDIR)
	javac -g -d $(BINDIR) -cp $(BINDIR) src/flightcode/ResImpl/*.java

hotelcode:
	mkdir -p $(BINDIR)
	javac -g -d $(BINDIR) -cp $(BINDIR) src/hotelcode/ResImpl/*.java

runcar: compile
	CLASSPATH=$(BINDIR):ResInterface.jar rmiregistry $(CARPORT) &
	# Debug 
	java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,address=8003,suspend=n -cp $(BINDIR) -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(BINDIR)/ResInterface.jar carcode.ResImpl.CarManagerImpl $(CARPORT)
	# NON-Debug
	#java -cp $(BINDIR) -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(BINDIR)/ResInterface.jar carcode.ResImpl.CarManagerImpl $(CARPORT)
	
recovercar: 
	# Debug 
	java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,address=8003,suspend=n -cp $(BINDIR) -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(BINDIR)/ResInterface.jar carcode.ResImpl.CarManagerImpl $(CARPORT) $(SERVERHOST) $(SERVERPORT)

runflight:
	CLASSPATH=$(BINDIR):ResInterface.jar rmiregistry $(FLIGHTPORT) &
	# DEBUG MODE
	java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,address=8007,suspend=n -cp $(BINDIR) -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(BINDIR)/ResInterface.jar flightcode.ResImpl.FlightManagerImpl $(FLIGHTPORT)	
	# NON-Debug
	#java -cp $(BINDIR) -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(BINDIR)/ResInterface.jar flightcode.ResImpl.FlightManagerImpl $(FLIGHTPORT)

runhotel:
	CLASSPATH=$(BINDIR):ResInterface.jar rmiregistry $(HOTELPORT) &
	# Debug
	java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,address=8004,suspend=n -cp $(BINDIR) -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(BINDIR)/ResInterface.jar hotelcode.ResImpl.HotelManagerImpl $(HOTELPORT)	
	# NON-Debug
	#java -cp $(BINDIR) -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(BINDIR)/ResInterface.jar hotelcode.ResImpl.HotelManagerImpl $(HOTELPORT)
	
runserver:
	CLASSPATH=$(BINDIR):ResInterface.jar rmiregistry $(SERVERPORT) &
	# Debug mode	
	java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n -cp $(BINDIR) -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(BINDIR)/ResInterface.jar servercode.ResImpl.ResourceManagerImpl $(SERVERPORT) $(CARHOST) $(CARPORT) $(FLIGHTHOST) $(FLIGHTPORT) $(HOTELHOST) $(HOTELPORT) 
	# Non-Debug
	#java -cp $(BINDIR) -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(BINDIR)/ResInterface.jar servercode.ResImpl.ResourceManagerImpl $(SERVERPORT) $(CARHOST) $(CARPORT) $(FLIGHTHOST) $(FLIGHTPORT) $(HOTELHOST) $(HOTELPORT)
	
runclient:	
	# Debug mode
	#java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,address=8001,suspend=n -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(CURDIR) -cp $(BINDIR) clientcode.client $(SERVERHOST) $(SERVERPORT)
	# Non-Debug 
	java -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(CURDIR) -cp $(BINDIR) clientcode.client $(SERVERHOST) $(SERVERPORT)
	

automatedclient:	
	# Debug mode
	#java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,address=8001,suspend=n -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(CURDIR) -cp $(BINDIR) clientcode.client $(SERVERHOST) $(SERVERPORT)
	# Non-Debug 
	java -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(CURDIR) -cp $(BINDIR) clientcode.AutomatedClient $(SERVERHOST) $(SERVERPORT) 
	

populate:
	java -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(CURDIR) -cp $(BINDIR) clientcode.AutomatedClient $(SERVERHOST) $(SERVERPORT) pop


jarfile: compile
	jar cvf ResInterface.jar -C $(BINDIR) servercode/ResInterface/ItemManager.class -C $(BINDIR) servercode/ResInterface/ResourceManager.class

policy:
	@echo "grant codeBase \"file:$(BINDIR)\" {" > java.policy
	@echo "    permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy
	
clean:
	rm -rf $(BINDIR)/*
	rm -f ResInterface.jar
