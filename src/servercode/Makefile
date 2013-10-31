policy:
	@echo "grant codeBase \"file:$(CURDIR)/bin\" {" > java.policy
	@echo "    permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

compileserver: policy
	mkdir -p bin
	javac -d bin -cp . ResInterface/ResourceManager.java
	jar cvf ResInterface.jar bin/ResInterface/*.class
	javac -d bin -cp . ResImpl/*.java

runserver: compileserver
	java -cp bin:ResInterface.jar -Djava.security.debug=access,failure -Djava.security.policy=./java.policy -Djava.rmi.server.codebase=file:$(CURDIR) ResImpl.ResourceManagerImpl localhost localhost localhost

clean:
	rm -rf bin/*
	rm -f ResInterface.jar
