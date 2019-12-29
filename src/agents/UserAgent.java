package agents;

import behaviours.UserBehaviour;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.Logger;
import org.xml.sax.SAXException;
import utils.Configuration;
import utils.XmlParser;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class UserAgent extends Agent {

    private Configuration configuration;
    private String userInput;
    private Logger myLogger = Logger.getMyLogger(getClass().getName());
    private String configFileName;

    public void setConfigFileName(String configFileName) {
        this.configFileName = configFileName;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    private void setupLogger() {
        FileHandler fh;

        try {
            // This block configure the logger with handler and formatter
            fh = new FileHandler("./logs/UserAgent.log");
            myLogger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
        myLogger.setUseParentHandlers(false);
    }

    @Override
    protected void setup() {
        setupLogger();
        Configuration myConfig;
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("agents.UserAgent");
        sd.setName(getName());
        sd.setOwnership("IMAS");
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            UserBehaviour userBehaviour = new UserBehaviour(this);
            addBehaviour(userBehaviour);
        } catch (FIPAException e) {
            e.printStackTrace();
            myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - Cannot register with DF", e);
            doDelete();
        }
    }

    public void parseConfigFile(String configFile) throws ParserConfigurationException, IOException, SAXException {
        Configuration myConfig;
        myConfig = XmlParser.parseConfigFile(configFile);
        this.configuration = myConfig;
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            myLogger.info("[" + getLocalName() + "]: L'AGENT S'HA ELIMINAT DEL DF");
        } catch (FIPAException e) {
            myLogger.severe("[" + getLocalName() + "]: NO S'HA POGUT ELIMINAR");
            e.printStackTrace();
        }
    }

    public String readUserInput() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        // Read and process lines from console
        String input;
        try {
            while (br.ready()) {
                br.readLine();
            }
            System.out.println("\n\nChoose action:\n - Initialize the system (INIT <config_file>)\n - train (T)\n - predict (P)");
            input = br.readLine();
            userInput = input;
            return input;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public boolean readConfigFile() {
        try {
            parseConfigFile(configFileName);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.err.println("File Not Found: Please enter the correct config file.");
            return false;
        }
        return true;
    }
}
