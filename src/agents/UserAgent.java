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
import java.io.IOException;

public class UserAgent extends Agent {

    private Configuration configuration;
    private Logger myLogger = Logger.getMyLogger(getClass().getName());

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void setup() {

        Configuration myConfig = new Configuration();
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

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
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
}
