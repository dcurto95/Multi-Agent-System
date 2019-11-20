package agents;

import behaviours.ManagerBehaviour;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.Logger;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;
import utils.Configuration;

public class ManagerAgent extends Agent {

    private Configuration configuration;
    private Logger myLogger = Logger.getMyLogger(getClass().getName());

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void setup() {
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("agents.ManagerAgent");
        sd.setName(getName());
        sd.setOwnership("IMAS");
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            ManagerBehaviour managerBehaviour = new ManagerBehaviour(this);
            addBehaviour(managerBehaviour);
        } catch (FIPAException e) {
            e.printStackTrace();
            myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - Cannot register with DF", e);
            doDelete();
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

    public Boolean createClassifiers() {
        AgentContainer ac = this.getContainerController();
        try {
            for (int i = 0; i < this.configuration.getClassifiers(); i++){
                ac.createNewAgent("Classifier_" + i, "agents.ClassifierAgent", new Object[0]).start();
            }
        } catch (StaleProxyException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
