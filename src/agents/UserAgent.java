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

public class UserAgent extends Agent {

    private Configuration configuration;
    private Logger myLogger = Logger.getMyLogger(getClass().getName());

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    protected void setup() {

        Configuration myConfig;
        try {
            myConfig = XmlParser.parseConfigFile("configuration.xml");
            this.configuration = myConfig;

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

        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("[" + getLocalName() + "]: L'AGENT S'HA ELIMINAT DEL DF");
        } catch (FIPAException e) {
            System.err.println("[" + getLocalName() + "]: NO S'HA POGUT ELIMINAR");
            e.printStackTrace();
        }
    }

    public String readUserInput() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        // Read and process lines from console
        String input;
        while (br.ready()) {
            br.readLine();
        }
        System.out.println("Select train (T) or predict (P):");
        input = br.readLine();
        return input.toUpperCase();
    }
}
