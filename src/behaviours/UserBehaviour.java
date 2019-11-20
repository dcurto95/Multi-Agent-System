package behaviours;

import agents.UserAgent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.util.Date;

public class UserBehaviour extends CyclicBehaviour {
    private static final int INIT = 0;
    private static final int INIT_DONE = 1;
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
        switch (state) {
            case INIT:
                managerAgent = getManagerAID();

                try {

                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(managerAgent);
                    msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                    // We want to receive a reply in 10 secs
                    msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
                    //msg.setPerformative(ACLMessage.REQUEST);
                    msg.setContentObject(userAgent.getConfiguration());
                    myAgent.send(msg);
                    ACLMessage received_message = myAgent.blockingReceive();
                    if (received_message != null) {
                        switch (received_message.getPerformative()) {
                            case ACLMessage.AGREE:
                                System.out.println("Agent " + this.myAgent.getLocalName() + " >>> Received Message Agree from " + received_message.getSender().getLocalName());
                                received_message = myAgent.blockingReceive();
                                if (received_message != null) {
                                    switch (received_message.getPerformative()) {
                                        case ACLMessage.INFORM:
                                            //TODO: This is log, will be removed/changed in the future
                                            System.out.println("Agent " + this.myAgent.getLocalName() + " >>> Received Message Inform from " + received_message.getSender().getLocalName());
                                            System.out.println("Correct initialization");
                                            state = INIT_DONE;
                                            break;
                                        case ACLMessage.FAILURE:
                                            //TODO: This is log, will be removed/changed in the future
                                            System.out.println("Agent " + this.myAgent.getLocalName() + " >>> Received Message Failure from " + received_message.getSender().getLocalName());
                                            System.out.println("Failed to initialize the system");
                                            break;
                                    }
                                } else {
                                    System.out.println("Agent " + this.myAgent.getLocalName() + " >>> Unexpected Message [" + null + "]");
                                }
                                System.out.println("Agent " + received_message.getSender().getLocalName() + " successfully performed the requested action");
                                break;
                            case ACLMessage.REFUSE:
                                System.out.println("REFUSE");
                                break;
                            case ACLMessage.NOT_UNDERSTOOD:
                                System.out.println("NOT UNDERSTOOD");
                                break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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
        System.out.println("Found agent " + managerAID.getName());
        return managerAID;
    }
}