package behaviours;

import agents.ClassifierAgent;
import jade.core.behaviours.CyclicBehaviour;

public class ClassifierBehaviour extends CyclicBehaviour {

    private ClassifierAgent classifierAgent;

    public ClassifierBehaviour (ClassifierAgent agent) {
            super(agent);
            this.classifierAgent = agent;
    }

    @Override
    public void action(){
        try {
            System.out.println("Agent " + this.myAgent.getLocalName() + " >>> is here!");
            Thread.sleep(10000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
