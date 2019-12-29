package behaviours;

import agents.ManagerAgent;
import communication_protocols.FIPAMultipleTargetRequester;
import jade.core.AID;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import utils.ClassifierConfig;
import utils.Configuration;
import weka.core.Instances;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class ManagerBehaviour extends FIPAMultipleTargetRequester {
    private static final int INIT = 0;
    private static final int INIT_DONE = 1;
    private static final int TRAIN = 2;
    private static final int PREDICT = 3;
    private int state;
    private ManagerAgent managerAgent;
    private List<AID> classifierAIDList;
    private ACLMessage reply;

    public ManagerBehaviour(ManagerAgent agent) {
        super(agent);
        this.managerAgent = agent;
        this.state = INIT;
        this.classifierAIDList = new ArrayList<>();
        this.reply = null;
        setupLogger();
    }

    private void setupLogger() {
        FileHandler fh;

        try {
            // This block configure the logger with handler and formatter
            fh = new FileHandler("./logs/ManagerBehaviour.log");
            myLogger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
        myLogger.setUseParentHandlers(false);
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

                                reply = receivedMessage.createReply();
                                reply.setPerformative(ACLMessage.AGREE);
                                myAgent.send(reply);
                            } catch (UnreadableException e) {
                                e.printStackTrace();

                                reply = receivedMessage.createReply();
                                reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                                myAgent.send(reply);
                            }
                            myLogger.info("Agent " + this.myAgent.getLocalName() + " >>> Accepted request to initialize classifiers");
                            managerAgent.createClassifiers();

                            checkClassifiersACK();
                            this.setTargetsAIDs(this.classifierAIDList);
                            managerAgent.readDatasetFile();

                            myLogger.info("Agent " + this.myAgent.getLocalName() + " >>> Init done");

                            reply = receivedMessage.createReply();
                            reply.setPerformative(ACLMessage.INFORM);
                            myAgent.send(reply);
                            state = INIT_DONE;
                            break;
                    }
                }
                break;
            case INIT_DONE:
                receivedMessage = myAgent.blockingReceive();
                if (receivedMessage != null && receivedMessage.getPerformative() == ACLMessage.REQUEST) {
                    String content = receivedMessage.getContent();
                    reply = receivedMessage.createReply();

                    if (content != null) {
                        if (content.equals("T")) {
                            state = TRAIN;
                        } else if (content.equals("P")) {
                            state = PREDICT;
                        }
                        reply.setPerformative(ACLMessage.AGREE);
                        myAgent.send(reply);
                    }
                }
                break;
            case TRAIN:
                managerAgent.setRandomTrainDataForClassifiers();
                sendTrainingDataToClassifiers();

                boolean allOk = receiveAllTargetsMessagesInFipaProtocol();
                if (allOk) {
                    myLogger.info("Agent " + this.myAgent.getLocalName() + " >>> Training done");
                    reply.setPerformative(ACLMessage.INFORM);
                    myAgent.send(reply);
                } else {
                    myLogger.severe("Agent " + this.myAgent.getLocalName() + " >>> Training Failed");
                    reply.setPerformative(ACLMessage.FAILURE);
                    myAgent.send(reply);
                }
                state = INIT_DONE;
                break;
            case PREDICT:
                sendPredictDataToClassifiers();
                allOk = receiveAllTargetsMessagesInFipaProtocol();
                if (allOk) {
                    List<Double> predictions = managerAgent.votePredictions();
                    myLogger.info("Agent " + this.myAgent.getLocalName() + " >>> Predict done");
                    reply.setPerformative(ACLMessage.INFORM);
                    try {
                        reply.setContentObject((ArrayList) predictions);
                        myAgent.send(reply);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    myLogger.severe("Agent " + this.myAgent.getLocalName() + " >>> Predict Failed");
                    reply.setPerformative(ACLMessage.FAILURE);
                    myAgent.send(reply);
                }
                state = INIT_DONE;
                break;
        }

    }

    private void sendPredictDataToClassifiers() {
        Instances testData = managerAgent.getTestData();

        for (AID aClassifierAIDList : classifierAIDList) {
            ClassifierConfig classifierConfig = new ClassifierConfig(
                    managerAgent.getConfiguration().getAlgorithm(),
                    "P",
                    testData);
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(aClassifierAIDList);
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            // We want to receive a reply in 10 secs
            msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));

            try {
                msg.setContentObject(classifierConfig);
                myAgent.send(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendTrainingDataToClassifiers() {
        Instances[] classifiersTrainData = managerAgent.getClassifiersTrainData();

        for (int i = 0; i < classifierAIDList.size(); i++) {
            ClassifierConfig classifierConfig = new ClassifierConfig(
                    managerAgent.getConfiguration().getAlgorithm(),
                    "T",
                    classifiersTrainData[i]);
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(classifierAIDList.get(i));
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            // We want to receive a reply in 10 secs
            msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));

            try {
                msg.setContentObject(classifierConfig);
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
                    myLogger.info("Agent " + this.myAgent.getLocalName() + " >>> Classifier " + classifierAID.getName() + " created");
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void doNotUnderstood() {
        myLogger.info("NOT UNDERSTOOD");
    }

    @Override
    protected void doRefuse() {
        myLogger.info("REFUSE");
    }

    @Override
    protected void doFailure(ACLMessage receivedMessage) {
        myLogger.severe("Agent " + this.myAgent.getLocalName() + " >>> Received Message Failure from " + receivedMessage.getSender().getLocalName());
        switch (state) {
            case INIT:
                myLogger.severe("Failed to initialize the system");
                break;
        }
    }

    @Override
    protected void doInform(ACLMessage receivedMessage) {
        myLogger.info("Agent " + this.myAgent.getLocalName() + " >>> Received Message Inform from " + receivedMessage.getSender().getLocalName());
        switch (state) {
            case INIT:
                state = INIT_DONE;
                break;
            case PREDICT:
                try {
                    managerAgent.setPrediction(
                            receivedMessage.getSender(),
                            (List<Double>) receivedMessage.getContentObject());
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}