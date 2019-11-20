package behaviours;

import agents.ManagerAgent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import utils.Configuration;

import java.io.Serializable;

public class ManagerBehaviour extends CyclicBehaviour {
    private static final int INIT = 0;
    private static final int INIT_DONE = 1;
    private int state;
    //private AID userAID;
    private ManagerAgent managerAgent;

    public ManagerBehaviour(ManagerAgent agent) {
        super(agent);
        this.managerAgent = agent;
        this.state = INIT;
    }

    @Override
    public void action() {
        switch (state) {
            case INIT:
                //userAID = getUserAID();

                ACLMessage receivedMessage = myAgent.blockingReceive();
                if (receivedMessage != null) {
                    switch (receivedMessage.getPerformative()) {
                        case ACLMessage.REQUEST:
                            Serializable content;
                            try {
                                content = receivedMessage.getContentObject();
                                if (content != null) {
                                    managerAgent.setConfiguration((Configuration) content);
                                }

                                ACLMessage reply = receivedMessage.createReply();
                                reply.setPerformative(ACLMessage.AGREE);
                                myAgent.send(reply);
                            } catch (UnreadableException e) {
                                e.printStackTrace();

                                ACLMessage reply = receivedMessage.createReply();
                                reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                                myAgent.send(reply);
                            }
                            System.out.println("Agent " + this.myAgent.getLocalName() + " >>> Accepted request to initialize classifiers");
                            Boolean done = managerAgent.createClassifiers();

                            //TODO: Check cases
                            ACLMessage fromClassifier;
                            for (int i = 0; i < this.managerAgent.getConfiguration().getClassifiers(); i++){
                                fromClassifier = myAgent.blockingReceive();
                                if (fromClassifier.getPerformative() == ACLMessage.INFORM && fromClassifier.getContent() != null){
                                    System.out.println("Agent " + this.myAgent.getLocalName() + " >>> Classifier " + fromClassifier.getContent() + " created");
                                }
                            }
                        ACLMessage reply = receivedMessage.createReply();

                        //reply.addReceiver(userAID);
                        if (done) {
                            System.out.println("Agent " + this.myAgent.getLocalName() + " >>> Init done");
                            reply.setPerformative(ACLMessage.INFORM);
                            myAgent.send(reply);
                            state = INIT_DONE;
                        } else {
                            reply.setPerformative(ACLMessage.FAILURE);
                            myAgent.send(reply);
                        }
                        break;
                    }
                }
                break;
            case INIT_DONE:
                try {
                    Thread.sleep(10000000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
    /*
    private AID getUserAID() {
        AID userAID = null;

        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("agents.UserAgent");
        dfd.addServices(sd);

        SearchConstraints c = new SearchConstraints();
        c.setMaxResults((long) -1);

        do {
            try {
                DFAgentDescription[] result = DFService.search(this.myAgent, dfd, c);
                if (result.length > 0) {
                    userAID = result[0].getName();
                }
            } catch (FIPAException e) {
                e.printStackTrace();
                break;
            }
        } while (userAID == null);
        System.out.println("Found agent " + userAID.getName());
        return userAID;
    }*/
}