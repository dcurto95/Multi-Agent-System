package behaviours;

import agents.ClassifierAgent;
import jade.core.behaviours.CyclicBehaviour;

public class ClassifierBehaviour extends CyclicBehaviour {

    private ClassifierAgent classifierAgent;

    public ClassifierBehaviour (ClassifierAgent agent) {
            super(agent);
            this.classifierAgent = agent;
    }

    public void action(){
        System.out.println("Agent " + this.myAgent.getLocalName() + " >>> is here!");
    }

}
