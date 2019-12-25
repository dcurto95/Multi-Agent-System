package behaviours;

import agents.ManagerAgent;
import communication_protocols.FIPAMultipleTargetRequester;
import jade.core.AID;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import utils.Configuration;
import weka.core.Instances;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ManagerBehaviour extends FIPAMultipleTargetRequester {
    private static final int INIT = 0;
    private static final int INIT_DONE = 1;
    private int state;
    private ManagerAgent managerAgent;
    private List<AID> classifierAIDList;

    public ManagerBehaviour(ManagerAgent agent) {
        super(agent);
        this.managerAgent = agent;
        this.state = INIT;
        this.classifierAIDList = new ArrayList<>();
    }

    @Override
    public void action() {
        ACLMessage receivedMessage = null;
        switch (state) {
            case INIT:
                receivedMessage = myAgent.blockingReceive();
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
                            managerAgent.createClassifiers();

                            checkClassifiersACK();
                            this.setTargetsAIDs(this.classifierAIDList);
                            managerAgent.readDatasetFile();

                            ACLMessage reply = receivedMessage.createReply();
                            boolean done = true;
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
                receivedMessage = myAgent.blockingReceive();
                if (receivedMessage != null) {
                    switch (receivedMessage.getPerformative()) {
                        case ACLMessage.REQUEST:
                            String content = receivedMessage.getContent();
                            if (content != null) {
                                if (content.equals("T")) {
                                    sendTrainingDataToClassifiers();
                                    receiveAllTargetsMessagesInFipaProtocol();
                                } else if (content.equals("P")) {
                                    managerAgent.predictClassifiers();
                                }
                            }

                    }
                }
                try {
                    Thread.sleep(10000000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void sendTrainingDataToClassifiers() {
        Instances[] classifiersTrainData = managerAgent.getClassifiersTrainData();

        for (int i = 0; i < classifierAIDList.size(); i++) {
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(classifierAIDList.get(i));
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            // We want to receive a reply in 10 secs
            msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));

            try {
                msg.setContentObject(classifiersTrainData[i]);
                myAgent.send(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkClassifiersACK() {
        ACLMessage fromClassifier;
        for (int i = 0; i < this.managerAgent.getConfiguration().getClassifiers(); i++) {
            fromClassifier = myAgent.blockingReceive();
            if (fromClassifier.getPerformative() == ACLMessage.INFORM && fromClassifier.getContent() != null) {
                try {
                    AID classifierAID = (AID) fromClassifier.getContentObject();
                    this.classifierAIDList.add(classifierAID);
                    System.out.println("Agent " + this.myAgent.getLocalName() + " >>> Classifier " + classifierAID.getName() + " created");
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void doNotUnderstood() {
        System.out.println("NOT UNDERSTOOD");
    }

    @Override
    protected void doRefuse() {
        System.out.println("REFUSE");
    }

    @Override
    protected void doFailure(ACLMessage receivedMessage) {
        //TODO: This is log, will be removed/changed in the future
        System.out.println("Agent " + this.myAgent.getLocalName() + " >>> Received Message Failure from " + receivedMessage.getSender().getLocalName());
        System.out.println("Failed to initialize the system");
    }

    @Override
    protected void doInform(ACLMessage receivedMessage) {
        //TODO: This is log, will be removed/changed in the future
        System.out.println("Agent " + this.myAgent.getLocalName() + " >>> Received Message Inform from " + receivedMessage.getSender().getLocalName());
        if (state == INIT) {
            state = INIT_DONE;
        } else {
            System.out.println(receivedMessage.getContent());
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
                DFAgentDescription[] result = DFService.search(this.agent, dfd, c);
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