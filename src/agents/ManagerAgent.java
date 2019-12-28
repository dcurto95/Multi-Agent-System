package agents;

import behaviours.ManagerBehaviour;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.Logger;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;
import utils.Configuration;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ManagerAgent extends Agent {

    private Configuration configuration;
    private Logger myLogger = Logger.getMyLogger(getClass().getName());
    private Instances[] classifiersTrainData;
    private Instances testData;
    private Map<AID, List<Double>> classifierPredictions;

    public Instances[] getClassifiersTrainData() {
        return classifiersTrainData;
    }

    public void setClassifiersTrainData(Instances[] classifiersTrainData) {
        this.classifiersTrainData = classifiersTrainData;
    }

    public Instances getTestData() {
        return testData;
    }

    public void setTestData(Instances testData) {
        this.testData = testData;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    @Override
    protected void setup() {
        classifierPredictions = new HashMap<>();
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("agents.ManagerAgent");
        sd.setName(getName());
        sd.setOwnership("IMAS");
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            ManagerBehaviour managerBehaviour = new ManagerBehaviour(this);
            addBehaviour(managerBehaviour);
        } catch (FIPAException e) {
            e.printStackTrace();
            myLogger.log(Logger.SEVERE, "Agent " + getLocalName() + " - Cannot register with DF", e);
            doDelete();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("[" + getLocalName() + "]: L'AGENT S'HA ELIMINAT DEL DF");
        } catch (FIPAException e) {
            System.err.println("[" + getLocalName() + "]: NO S'HA POGUT ELIMINAR");
            e.printStackTrace();
        }
    }

    public void createClassifiers() {
        AgentContainer ac = this.getContainerController();
        try {
            for (int i = 0; i < this.configuration.getClassifiers(); i++) {
                ac.createNewAgent("Classifier_" + i, "agents.ClassifierAgent", new Object[0]).start();
            }
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    private Instances[] createTrainingSubset(int[] trainingSettings, Instances trainData) {
        Instances[] trainingSets = new Instances[trainingSettings.length];

        int dataLength = trainData.numInstances();
        int lastInstance = 0;
        for (int i = 0; i < trainingSettings.length; i++) {
            int trainSize = trainingSettings[i];

            trainData.randomize(new java.util.Random(0));
            Instances classifierData;
            if ((lastInstance + trainSize) < dataLength) {
                classifierData = new Instances(trainData, lastInstance, trainSize);
                lastInstance = trainSize;
            } else {
                int first = lastInstance - ((lastInstance + trainSize) - dataLength);
                classifierData = new Instances(trainData, first, trainSize);
                lastInstance = 0;
            }
            trainingSets[i] = classifierData;
        }

        return trainingSets;
    }

    public void readDatasetFile() {
        try {
            BufferedReader reader =
                    new BufferedReader(new FileReader("./data/iris.arff"));
            ArffReader arff = new ArffReader(reader);

            Instances data = arff.getData();
            data.setClassIndex(data.numAttributes() - 1);
            int trainSize = data.numInstances() - configuration.getClassifyInstances();
            Instances trainData = new Instances(data, 0, trainSize);
            setTestData(new Instances(data, trainSize, configuration.getClassifyInstances()));
            System.out.println("TRUTH:");
            for (int i = 0; i < testData.numInstances(); i++) {
                System.out.println(testData.instance(i).classValue());
            }
            setClassifiersTrainData(createTrainingSubset(configuration.getTrainingSettings(), trainData));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setPrediction(AID sender, List<Double> predictions) {
        classifierPredictions.put(sender, predictions);
    }

    public List<Double> votePredictions() {
        //TODO Improve
        List<Double> finalPrediction = new ArrayList<>();
        List<List<Double>> votes = new ArrayList<>(classifierPredictions.values());

        for (int i = 0; i < votes.get(0).size(); i++) {
            Map<Double, Integer> options = new HashMap<>();

            int maxVote = 0;
            for (List<Double> agentVotes : votes) {
                Double agentVote = agentVotes.get(i);
                if (!options.containsKey(agentVote)) {
                    options.put(agentVote, 0);
                }
                int newVote = options.get(agentVote);
                if (newVote > maxVote) {
                    maxVote = newVote;
                }
                options.put(agentVote, newVote);
            }

            int finalMaxVote = maxVote;
            List<Double> mostVotedOptions = options.entrySet()
                    .stream()
                    .filter(entry -> Objects.equals(entry.getValue(), finalMaxVote))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            finalPrediction.add(mostVotedOptions.get(0));
        }
        return finalPrediction;
    }
}
