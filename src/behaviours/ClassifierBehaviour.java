package behaviours;

import agents.ClassifierAgent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;


public class ClassifierBehaviour extends CyclicBehaviour {

    private static final int INIT = 0;
    private static final int INIT_DONE = 1;
    private ClassifierAgent classifierAgent;
    private int state;

    public ClassifierBehaviour (ClassifierAgent agent) {
            super(agent);
            this.classifierAgent = agent;
            this.state = INIT;
    }

    @Override
    public void action(){
        switch (this.state){
            case INIT:
                System.out.println("Agent " + this.myAgent.getLocalName() + " >>> I'm alive!");
                AID managerAID = getManagerAID();

                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(managerAID);
                msg.setContent(this.myAgent.getLocalName());
                myAgent.send(msg);
                this.state = INIT_DONE;
                break;
            case INIT_DONE:
                try {
                    Thread.sleep(10000000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }

    private AID getManagerAID() {
        AID managerAID = null;

        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("agents.ManagerAgent");
        dfd.addServices(sd);

        SearchConstraints c = new SearchConstraints();
        c.setMaxResults((long) -1);

        do {
            try {
                DFAgentDescription[] result = DFService.search(this.myAgent, dfd, c);
                if (result.length > 0) {
                    managerAID = result[0].getName();
                }
            } catch (FIPAException e) {
                e.printStackTrace();
                break;
            }
        } while (managerAID == null);
        System.out.println("Agent " + this.myAgent.getLocalName() + " >>> Found agent " + managerAID.getLocalName());
        return managerAID;
    }
}
