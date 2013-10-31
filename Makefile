BINDIR=make-bin
CARPORT=5006

all: compile jarfile

compile: servercode clientcode carcode 

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
	java -cp $(BINDIR) carcode.ResImpl.CarManagerImpl localhost $(CARPORT)
	

jarfile: compile
	jar cvf ResInterface.jar -C $(BINDIR) servercode/ResInterface/ItemManager.class -C $(BINDIR) servercode/ResInterface/ResourceManager.class

clean:
	rm -rf $(BINDIR)/*
	rm -f ResInterface.jar
