package communication_protocols;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

public abstract class FIPARequester extends CyclicBehaviour {
    private static final String AGENT_S_RECEIVED_MESSAGE_AGREE_FROM_S = "Agent %s >>> Received Message Agree from %s";
    private static final String PERFORMATIVE_NOT_UNDERSTOOD = "PERFORMATIVE NOT UNDERSTOOD";
    private static final String AGENT_S_UNEXPECTED_MESSAGE_NULL = "Agent %s >>> Unexpected Message [ null ]";
    private static final String AGENT_S_S_SUCCESSFULLY_PERFORMED_THE_REQUESTED_ACTION = "Agent %s >>> %s successfully performed the requested action";
    protected Agent agent;
    protected Logger myLogger = Logger.getMyLogger(getClass().getName());

    public FIPARequester(Agent agent) {
        super(agent);
        this.agent = agent;
    }

    protected void receiveMessageInFipaProtocol() {
        ACLMessage receivedMessage = agent.blockingReceive();
        if (receivedMessage != null) {
            switch (receivedMessage.getPerformative()) {
                case ACLMessage.AGREE:
                    myLogger.info(String.format(
                            AGENT_S_RECEIVED_MESSAGE_AGREE_FROM_S,
                            this.agent.getLocalName(),
                            receivedMessage.getSender().getLocalName()));
                    receivedMessage = agent.blockingReceive();
                    if (receivedMessage != null) {
                        switch (receivedMessage.getPerformative()) {
                            case ACLMessage.INFORM:
                                doInform(receivedMessage);
                                myLogger.info(String.format(
                                        AGENT_S_S_SUCCESSFULLY_PERFORMED_THE_REQUESTED_ACTION,
                                        this.agent.getLocalName(),
                                        receivedMessage.getSender().getLocalName()));
                                break;
                            case ACLMessage.FAILURE:
                                doFailure(receivedMessage);
                                break;
                            default:
                                myLogger.severe(PERFORMATIVE_NOT_UNDERSTOOD);
                        }
                    } else {
                        myLogger.severe(String.format(AGENT_S_UNEXPECTED_MESSAGE_NULL, this.agent.getLocalName()));
                    }
                    break;
                case ACLMessage.REFUSE:
                    doRefuse();
                    break;
                case ACLMessage.NOT_UNDERSTOOD:
                    doNotUnderstood();
                    break;
                default:
                    myLogger.severe(PERFORMATIVE_NOT_UNDERSTOOD);
            }
        }
    }

    protected abstract void doNotUnderstood();

    protected abstract void doRefuse();

    protected abstract void doFailure(ACLMessage receivedMessage);

    protected abstract void doInform(ACLMessage receivedMessage);
}
