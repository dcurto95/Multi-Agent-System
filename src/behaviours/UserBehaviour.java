package behaviours;

import agents.UserAgent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

import java.io.IOException;
import java.util.Date;

public class UserBehaviour extends CyclicBehaviour {
    public static final int INIT = 0;
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
        managerAgent = getManagerAID();

        switch (state) {
            case INIT:
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

                    AchieveREInitiator aux = new AchieveREInitiator(userAgent, msg) {
                        @Override
                        protected void handleAgree(ACLMessage inform) {
                            System.out.println("Agent " + inform.getSender().getName() + " successfully performed the requested action");
                        }

                        @Override
                        protected void handleInform(ACLMessage inform) {
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

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
        }
    }

    private AID getManagerAID() {
        AID managerAID = null;
        AMSAgentDescription[] agents;

        SearchConstraints c = new SearchConstraints();
        c.setMaxResults((long) -1);

        do {
            try {
                agents = AMSService.search(this.myAgent, new AMSAgentDescription(), c);

                if (agents.length > 0) {
                    managerAID = agents[0].getName();
                }
            } catch (FIPAException e) {
                e.printStackTrace();
                break;
            }
        } while (managerAID != null);
        return managerAID;
    }
}
