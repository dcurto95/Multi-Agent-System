package communication_protocols;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for defining FIPA communication protocol with multiple targets
 */
public abstract class FIPAMultipleTargetRequester extends CyclicBehaviour {
    private static final String AGENT_S_RECEIVED_MESSAGE_AGREE_FROM_S = "Agent %s >>> Received Message Agree from %s";
    private static final String PERFORMATIVE_NOT_UNDERSTOOD = "PERFORMATIVE NOT UNDERSTOOD";
    private static final String AGENT_S_UNEXPECTED_MESSAGE_NULL = "Agent %s >>> Unexpected Message [ null ]";
    private static final String AGENT_S_S_SUCCESSFULLY_PERFORMED_THE_REQUESTED_ACTION = "Agent %s >>> %s successfully performed the requested action";
    protected Agent agent;
    protected Logger myLogger = Logger.getMyLogger(getClass().getName());
    private HashMap<AID, Integer> targetLastMessageMap;

    public FIPAMultipleTargetRequester(Agent agent) {
        super(agent);
        this.agent = agent;
        targetLastMessageMap = new HashMap<>();

    }

    /**
     * Creates the initial states for all targets
     *
     * @param targetsAIDs List of Target AIDs
     */
    public void setTargetsAIDs(List<AID> targetsAIDs) {
        targetsAIDs.forEach(aid -> targetLastMessageMap.put(aid, null));
    }

    /**
     * Receives all type of ACLMessage controlling their senders.
     * Finishes when all targets are in a final state: INFORM, FAILURE, REFUSE or NOT_UNDERSTOOD
     */
    protected boolean receiveAllTargetsMessagesInFipaProtocol() {
        do {
            ACLMessage receivedMessage = agent.blockingReceive();
            if (receivedMessage != null) {
                AID sender = receivedMessage.getSender();
                switch (receivedMessage.getPerformative()) {
                    case ACLMessage.AGREE:
                        myLogger.info(String.format(
                                AGENT_S_RECEIVED_MESSAGE_AGREE_FROM_S,
                                this.agent.getLocalName(),
                                sender.getLocalName()));

                        targetLastMessageMap.put(sender, ACLMessage.AGREE);
                        break;
                    case ACLMessage.INFORM:
                        if (targetLastMessageMap.get(sender) == ACLMessage.AGREE) {
                            targetLastMessageMap.put(sender, ACLMessage.INFORM);
                            doInform(receivedMessage);
                            myLogger.info(String.format(
                                    AGENT_S_S_SUCCESSFULLY_PERFORMED_THE_REQUESTED_ACTION,
                                    this.agent.getLocalName(),
                                    sender.getLocalName()));
                        }
                        break;
                    case ACLMessage.FAILURE:
                        if (targetLastMessageMap.get(sender) == ACLMessage.AGREE) {
                            targetLastMessageMap.put(sender, ACLMessage.FAILURE);
                            doFailure(receivedMessage);
                        }
                        break;
                    case ACLMessage.REFUSE:
                        targetLastMessageMap.put(sender, ACLMessage.REFUSE);
                        doRefuse();
                        break;
                    case ACLMessage.NOT_UNDERSTOOD:
                        targetLastMessageMap.put(sender, ACLMessage.NOT_UNDERSTOOD);
                        doNotUnderstood();
                        break;
                    default:
                        myLogger.severe(PERFORMATIVE_NOT_UNDERSTOOD);
                }
            } else {
                myLogger.severe(String.format(AGENT_S_UNEXPECTED_MESSAGE_NULL, this.agent.getLocalName()));
            }
        } while (!allTargetsFinished());

        return allTargetsOkay();
    }

    /**
     * Checks if all targets finished correctly their task
     *
     * @return True if all targets finished correctly their task, false otherwise.
     */
    private boolean allTargetsOkay() {
        for (Map.Entry<AID, Integer> entry : targetLastMessageMap.entrySet()) {
            if (entry.getValue() != ACLMessage.INFORM) return false;
        }
        return true;
    }

    /**
     * Checks if all targets are in a final state
     *
     * @return True if all targets are in a final state, False otherwise
     */
    private boolean allTargetsFinished() {
        int finishedTargets = 0;
        for (Map.Entry<AID, Integer> entry : targetLastMessageMap.entrySet()) {
            if (isLastState(entry.getValue())) finishedTargets++;
        }
        return finishedTargets == targetLastMessageMap.size();
    }

    /**
     * Checks if the given state is final
     *
     * @param state Integer representing the last ACLMessage type received
     * @return True if the state is final, False otherwise
     */
    private boolean isLastState(Integer state) {
        if (state != null) {
            return state == ACLMessage.INFORM ||
                    state == ACLMessage.FAILURE ||
                    state == ACLMessage.REFUSE ||
                    state == ACLMessage.NOT_UNDERSTOOD;
        } else {
            return false;
        }
    }

    protected abstract void doNotUnderstood();

    protected abstract void doRefuse();

    protected abstract void doFailure(ACLMessage receivedMessage);

    protected abstract void doInform(ACLMessage receivedMessage);
}
