From the Project's folder run the following commands to execute the system.

Commands to compile and run in Windows:

javac -classpath lib\jade.jar;lib\weka-stable-3.6.6.jar -d out\production\IMAS_Practice\ src\agents\*.java src\behaviours\*.java src\communication_protocols\*.java src\utils\*.java
java -cp lib\jade.jar;lib\weka-stable-3.6.6.jar;out\production\IMAS_Practice jade.Boot -gui -agents user:agents.UserAgent;manager:agents.ManagerAgent


Commands to compile and run in Linux:

javac -classpath lib/jade.jar;lib/weka-stable-3.6.6.jar -d out/production/IMAS_Practice/ src/agents/*.java src/behaviours/*.java src/communication_protocols/*.java src/utils/*.java
java -cp lib/jade.jar;lib/weka-stable-3.6.6.jar;out/production/IMAS_Practice jade.Boot -gui -agents user:agents.UserAgent;manager:agents.ManagerAgent