package behaviours;

import agents.UserAgent;
import communication_protocols.FIPARequester;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class UserBehaviour extends FIPARequester {
    private static final int INIT = 0;
    private static final int INIT_DONE = 1;
    private static final int TRAIN = 2;
    private static final int PREDICT = 3;
    public static final String WRONG_INPUT_ONLY_INIT_T_OR_P_ACCEPTED = "Wrong input, only INIT, T or P accepted";
    private int state;
    private AID managerAgent;
    private UserAgent userAgent;

    public UserBehaviour(UserAgent agent) {
        super(agent);
        this.userAgent = agent;
        this.state = INIT;
        setupLogger();
    }

    private void setupLogger() {
        FileHandler fh;

        try {
            // This block configure the logger with handler and formatter
            fh = new FileHandler("./logs/UserBehaviour.log");
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
        String userInput = null;

        switch (state) {
            case INIT:
                managerAgent = getManagerAID();

                try {

                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(managerAgent);
                    msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                    // We want to receive a reply in 10 secs
                    msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));

                    msg.setContentObject(userAgent.getConfiguration());
                    agent.send(msg);
                    receiveMessageInFipaProtocol();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case INIT_DONE:
                boolean correctInput = true;
                do {
                    try {
                        userInput = userAgent.readUserInput();
                        String[] splitedInput = userInput.split("\\s+");
                        if (splitedInput.length <= 2) {
                            switch (splitedInput[0].toUpperCase()) {
                                case "INIT":
                                    if (splitedInput.length == 2) {
                                        correctInput = true;
                                        state = TRAIN;
                                        System.out.println("Initializing the system...");
                                    }
                                case "T":
                                    correctInput = true;
                                    state = TRAIN;
                                    System.out.println("Training...");
                                    break;
                                case "P":
                                    state = PREDICT;
                                    correctInput = true;
                                    System.out.println("Predicting...");
                                    break;
                                default:
                                    correctInput = false;
                                    System.out.println(WRONG_INPUT_ONLY_INIT_T_OR_P_ACCEPTED);
                            }
                        } else {
                            correctInput = false;
                            System.out.println(WRONG_INPUT_ONLY_INIT_T_OR_P_ACCEPTED);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } while (!correctInput);

            case TRAIN:
            case PREDICT:
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(managerAgent);
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                // We want to receive a reply in 10 secs
                msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
                msg.setContent(userInput);
                agent.send(msg);

                receiveMessageInFipaProtocol();
                state = INIT_DONE;
                break;
        }
    }

    @Override
    protected void doNotUnderstood() {
        myLogger.severe("NOT UNDERSTOOD");
    }

    @Override
    protected void doRefuse() {
        myLogger.severe("REFUSE");
    }

    @Override
    protected void doFailure(ACLMessage receivedMessage) {
        myLogger.severe("Agent " + this.agent.getLocalName() + " >>> Received Message Failure from " + receivedMessage.getSender().getLocalName());
        switch (state) {
            case INIT:
                System.out.println("Failed to initialize the system");
                break;
            case TRAIN:
                System.out.println("Failed to train the model.");
                break;
            case PREDICT:
                System.out.println("The model hasn't been trained yet.");
                break;
        }
    }

    @Override
    protected void doInform(ACLMessage receivedMessage) {
        myLogger.info("Agent " + this.agent.getLocalName() + " >>> Received Message Inform from " + receivedMessage.getSender().getLocalName());
        switch (state) {
            case INIT:
                state = INIT_DONE;
                break;
            case TRAIN:
                System.out.println("Correctly trained the data.");
                break;
            case PREDICT:
                try {
                    System.out.println("Prediction finished.");
                    System.out.println(receivedMessage.getContentObject().toString());
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                break;
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
                DFAgentDescription[] result = DFService.search(this.agent, dfd, c);
                if (result.length > 0) {
                    managerAID = result[0].getName();
                }
            } catch (FIPAException e) {
                e.printStackTrace();
                break;
            }
        } while (managerAID == null);
        myLogger.info("Agent " + this.agent.getLocalName() + " >>> Found agent " + managerAID.getLocalName());
        return managerAID;
    }
}