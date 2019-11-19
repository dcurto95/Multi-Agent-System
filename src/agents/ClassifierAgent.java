package agents;

import behaviours.ClassifierBehaviour;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.Logger;

public class ClassifierAgent extends Agent {
    private Logger myLogger = Logger.getMyLogger(getClass().getName());

    @Override
    protected void setup() {
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("agents.ClassifierAgent");
        sd.setName(getName());
        sd.setOwnership("IMAS");
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            ClassifierBehaviour classifierBehaviour = new ClassifierBehaviour(this);
            addBehaviour(classifierBehaviour);
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
}
