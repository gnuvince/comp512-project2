BINDIR=make-bin
SERVERPORT=5005
CARPORT=5006

all: compile jarfile

compile: lockcode servercode clientcode carcode policy

lockcode:
	mkdir -p $(BINDIR)
	javac -d $(BINDIR) -cp $(BINDIR) src/LockManager/*.java

servercode:
	mkdir -p $(BINDIR)
	javac -d $(BINDIR) -cp $(BINDIR) src/servercode/ResImpl/RMItem.java
	javac -d $(BINDIR) -cp $(BINDIR) src/servercode/ResImpl/ReservedItem.java
	javac -d $(BINDIR) -cp $(BINDIR) src/servercode/ResInterface/*.java
	javac -d $(BINDIR) -cp $(BINDIR) src/servercode/ResImpl/*.java

clientcode:
	mkdir -p $(BINDIR)
	javac -d $(BINDIR) -cp $(BINDIR) src/clientcode/*.java

carcode:
	mkdir -p $(BINDIR)
	javac -d $(BINDIR) -cp $(BINDIR) src/carcode/ResImpl/*.java

runcar: compile
	CLASSPATH=$(BINDIR):ResInterface.jar rmiregistry $(CARPORT) &
	java -cp $(BINDIR) -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(BINDIR)/ResInterface.jar carcode.ResImpl.CarManagerImpl localhost $(CARPORT)
	
runserver:
	CLASSPATH=$(BINDIR):ResInterface.jar rmiregistry $(SERVERPORT) &	
	java -cp $(BINDIR) -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(BINDIR)/ResInterface.jar servercode.ResImpl.ResourceManagerImpl localhost localhost localhost $(SERVERPORT)
	
runclient:	
	java -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(CURDIR) -cp $(BINDIR) clientcode.client localhost 5005
	

jarfile: compile
	jar cvf ResInterface.jar -C $(BINDIR) servercode/ResInterface/ItemManager.class -C $(BINDIR) servercode/ResInterface/ResourceManager.class

policy:
	@echo "grant codeBase \"file:$(BINDIR)\" {" > java.policy
	@echo "    permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy
	
clean:
	rm -rf $(BINDIR)/*
	rm -f ResInterface.jar
