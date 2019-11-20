package behaviours;

import agents.UserAgent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.AMSService;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.util.Logger;

import java.io.IOException;
import java.util.Date;

public class ShitBehaviour extends CyclicBehaviour {
    private static final int INIT = 0;
    private static final int INIT_DONE = 1;
    private int state;
    private AID managerAgent;
    private UserAgent userAgent;

    public ShitBehaviour(UserAgent agent) {
        super(agent);
        this.userAgent = agent;
        this.state = INIT;
    }

    @Override
    public void action() {
        System.out.println("IEAPAPA");
        managerAgent = getManagerAID();

        switch (state) {
            case INIT:
                System.out.println(INIT);
                /*
                 *
                 * */
                try {

                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(managerAgent);
                    msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                    // We want to receive a reply in 10 secs
                    msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
                    msg.setPerformative(ACLMessage.REQUEST);
                    msg.setContentObject(userAgent.getConfiguration());
                    //myAgent.send(msg);

                    AchieveREInitiator aux = new AchieveREInitiator(userAgent, msg) {
                        @Override
                        protected void handleAgree(ACLMessage inform) {
                            System.out.println("Agree received");
                            ACLMessage msg = myAgent.blockingReceive();
                            if (msg != null) {
                                switch (msg.getPerformative()) {
                                    case ACLMessage.INFORM:
                                        //TODO: This is log, will be removed/changed in the future
                                        System.out.println("Agent " + this.myAgent.getLocalName() + " - Received Message Inform from " + msg.getSender().getLocalName());
                                        System.out.println("Correct initialization.");
                                        state = INIT_DONE;
                                        break;
                                    case ACLMessage.FAILURE:
                                        //TODO: This is log, will be removed/changed in the future
                                        System.out.println("Agent " + this.myAgent.getLocalName() + " - Received Message Failure from " + msg.getSender().getLocalName());
                                        System.out.println("Failed to initialize the system.");
                                        break;
                                }
                            } else {
                                System.out.println("Agent " + this.myAgent.getLocalName() + " - Unexpected request [" + null + "] received from " + inform.getSender().getLocalName());
                            }
                            System.out.println("Agent " + inform.getSender().getName() + " successfully performed the requested action");
                        }

                        @Override
                        protected void handleInform(ACLMessage inform) {
                            System.out.println("INSIDE HANDLE INFORM!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            System.out.println("Agent " + inform.getSender().getName() + " successfully performed the requested action");
                        }

                        @Override
                        protected void handleRefuse(ACLMessage refuse) {
                            System.out.println("Agent " + refuse.getSender().getName() + " refused to perform the requested action");
                        }

                        @Override
                        protected void handleFailure(ACLMessage failure) {
                            if (failure.getSender().equals(myAgent.getAMS())) {
                                // FAILURE notification from the JADE runtime: the receiver
                                // does not exist
                                System.out.println("Responder does not exist");
                            } else if (failure.getReplyByDate() == null) {
                                System.out.println("Timeout expired: missing responses");
                            } else {
                                System.out.println("Agent " + failure.getSender().getName() + " failed to perform the requested action");
                            }
                        }
                    };
                    myAgent.addBehaviour(aux);
                    //while (state == INIT) ;

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
        }
    }

    private void sendRefuse(ACLMessage msg, ACLMessage reply) {
        System.out.println("Agent " + this.myAgent.getLocalName() + " - Unexpected request [" + null + "] received from " + msg.getSender().getLocalName());
        reply.setPerformative(ACLMessage.REFUSE);
        reply.setContent("Comunicacion restringida");
        this.myAgent.send(reply);
    }

    private void sendACK(ACLMessage message, ACLMessage reply) {
        System.out.println("Agent " + this.myAgent.getLocalName() + " - Received Message Request from " + message.getSender().getLocalName());
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent("ACK");
        this.myAgent.send(reply);
    }

    private AID getManagerAID() {
        AID managerAID = null;
        AMSAgentDescription[] agents;

        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("PingAgent");
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
