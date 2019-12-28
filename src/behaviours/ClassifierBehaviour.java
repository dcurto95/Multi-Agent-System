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
import jade.lang.acl.UnreadableException;
import utils.ClassifierConfig;
import weka.core.Instances;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;


public class ClassifierBehaviour extends CyclicBehaviour {

    private static final int INIT = 0;
    private static final int INIT_DONE = 1;
    private ClassifierAgent classifierAgent;
    private int state;

    public ClassifierBehaviour(ClassifierAgent agent) {
        super(agent);
        this.classifierAgent = agent;
        this.state = INIT;
    }

    @Override
    public void action() {
        switch (this.state) {
            case INIT:
                System.out.println("Agent " + this.myAgent.getLocalName() + " >>> I'm alive!");
                AID managerAID = getManagerAID();

                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(managerAID);
                try {
                    msg.setContentObject(this.myAgent.getAID());
                    myAgent.send(msg);
                    this.state = INIT_DONE;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case INIT_DONE:
                ACLMessage receivedMessage = myAgent.blockingReceive();
                if (receivedMessage != null && receivedMessage.getPerformative() == ACLMessage.REQUEST) {
                    Serializable content;
                    try {
                        content = receivedMessage.getContentObject();
                        if (content != null) {
                            ClassifierConfig classifierConfig = (ClassifierConfig) content;

                            ACLMessage reply = receivedMessage.createReply();
                            reply.setPerformative(ACLMessage.AGREE);
                            myAgent.send(reply);

                            if (classifierConfig.getAction().equals("T")) {
                                trainAgent(receivedMessage, classifierConfig);
                            } else {
                                predictAgent(receivedMessage, classifierConfig);
                            }
                        } else {
                            ACLMessage reply = receivedMessage.createReply();
                            reply.setPerformative(ACLMessage.REFUSE);
                            myAgent.send(reply);
                        }

                    } catch (UnreadableException e) {
                        e.printStackTrace();

                        ACLMessage reply = receivedMessage.createReply();
                        reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                        myAgent.send(reply);
                    }
                }
        }
    }

    private void predictAgent(ACLMessage receivedMessage, ClassifierConfig classifierConfig) {
        ACLMessage reply;//TODO: Temporal Log, remove or use Logger
        System.out.println("Agent " + this.myAgent.getLocalName() + " >>> Received Predict Data");

        List<Double> predictions = classifierAgent.predict(classifierConfig);
        if (!predictions.isEmpty()) {
            //INFORM
            reply = receivedMessage.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            try {
                reply.setContentObject((Serializable) predictions);
                myAgent.send(reply);
            } catch (IOException e) {
                e.printStackTrace();
                reply.setPerformative(ACLMessage.FAILURE);
                myAgent.send(reply);
            }
        } else {
            //FAILURE
            reply = receivedMessage.createReply();
            reply.setPerformative(ACLMessage.FAILURE);
            myAgent.send(reply);
        }
    }

    private void trainAgent(ACLMessage receivedMessage, ClassifierConfig classifierConfig) {
        ACLMessage reply;//TODO: Temporal Log, remove or use Logger
        System.out.println("Agent " + this.myAgent.getLocalName() + " >>> Received Training Data");

        if (classifierAgent.train(classifierConfig)) {
            //INFORM
            reply = receivedMessage.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            myAgent.send(reply);
        } else {
            //FAILURE
            reply = receivedMessage.createReply();
            reply.setPerformative(ACLMessage.FAILURE);
            myAgent.send(reply);
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
