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

public class UserBehaviour extends FIPARequester {
    private static final int INIT = 0;
    private static final int INIT_DONE = 1;
    private static final int TRAIN = 2;
    private static final int PREDICT = 3;
    private int state;
    private AID managerAgent;
    private UserAgent userAgent;

    public UserBehaviour(UserAgent agent) {
        super(agent);
        this.userAgent = agent;
        this.state = INIT;
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

                        switch (userInput) {
                            case "T":
                                correctInput = true;
                                state = TRAIN;
                                System.out.println("Training");
                                break;
                            case "P":
                                state = PREDICT;
                                correctInput = true;
                                System.out.println("Predicting");
                                break;
                            default:
                                correctInput = false;
                                System.out.println("Wrong input, only T or P accepted");
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
        System.out.println("NOT UNDERSTOOD");
    }

    @Override
    protected void doRefuse() {
        System.out.println("REFUSE");
    }

    @Override
    protected void doFailure(ACLMessage receivedMessage) {
        //TODO: This is log, will be removed/changed in the future
        System.out.println("Agent " + this.agent.getLocalName() + " >>> Received Message Failure from " + receivedMessage.getSender().getLocalName());
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
        //TODO: This is log, will be removed/changed in the future
        System.out.println("Agent " + this.agent.getLocalName() + " >>> Received Message Inform from " + receivedMessage.getSender().getLocalName());
        switch (state) {
            case INIT:
                state = INIT_DONE;
                break;
            case TRAIN:
                System.out.println("Correctly trained the data.");
                break;
            case PREDICT:
                //TODO get content and print prediction
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
        System.out.println("Agent " + this.agent.getLocalName() + " >>> Found agent " + managerAID.getLocalName());
        return managerAID;
    }
}