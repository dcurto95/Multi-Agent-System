package agents;

import behaviours.ClassifierBehaviour;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.Logger;
import utils.ClassifierConfig;
import weka.classifiers.Classifier;
import weka.classifiers.functions.RBFNetwork;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.core.Instances;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class ClassifierAgent extends Agent {
    private Logger myLogger = Logger.getMyLogger(getClass().getName());
    private Classifier classifier;

    @Override
    protected void setup() {
        classifier = null;
        setupLogger();
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("agents.ClassifierAgent");
        sd.setName(getName());
        sd.setOwnership("IMAS");
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            ClassifierBehaviour classifierBehaviour = new ClassifierBehaviour(this);
            addBehaviour(classifierBehaviour);
        } catch (FIPAException e) {
            e.printStackTrace();
            myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - Cannot register with DF", e);
            doDelete();
        }
    }

    private void setupLogger() {
        FileHandler fh;

        try {
            // This block configure the logger with handler and formatter
            fh = new FileHandler("./logs/ClassifierAgent" + getLocalName() + ".log");
            myLogger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
        myLogger.setUseParentHandlers(false);
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            myLogger.info("[" + getLocalName() + "]: L'AGENT S'HA ELIMINAT DEL DF");
        } catch (FIPAException e) {
            myLogger.severe("[" + getLocalName() + "]: NO S'HA POGUT ELIMINAR");
            e.printStackTrace();
        }
    }

    public boolean train(ClassifierConfig classifierConfig) {
        switch (classifierConfig.getAlgorithm()) {
            case "J48":
                classifier = new J48();
                break;
            case "IBk":
                classifier = new IBk();
                break;
            case "RBFNetwork":
                classifier = new RBFNetwork();
                break;
            default:
                return false;
        }
        try {
            classifier.buildClassifier(classifierConfig.getData());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public List<Double> predict(ClassifierConfig classifierConfig) {
        if (classifier == null) return new ArrayList<>();

        Instances test = classifierConfig.getData();

        List<Double> predictions = new ArrayList<>();
        for (int i = 0; i < test.numInstances(); i++) {
            try {
                double pred = classifier.classifyInstance(test.instance(i));
                predictions.add(pred);
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        }
        return predictions;
    }


}
