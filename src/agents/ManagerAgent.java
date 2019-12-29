package agents;

import behaviours.ManagerBehaviour;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.util.Logger;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import utils.Configuration;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

public class ManagerAgent extends Agent {

    private Configuration configuration;
    private Logger myLogger = Logger.getMyLogger(getClass().getName());
    private Instances[] classifiersTrainData;
    private Instances trainData;
    private Instances testData;
    private List<AgentController> classifierAgents;
    private Map<AID, List<Double>> classifierPredictions;
    private Map<Integer, List<Double>> classifierPredictions2;

    public Instances[] getClassifiersTrainData() {
        return classifiersTrainData;
    }

    public void setClassifiersTrainData(Instances[] classifiersTrainData) {
        this.classifiersTrainData = classifiersTrainData;
    }

    public Instances getTrainData() {
        return trainData;
    }

    public void setTrainData(Instances trainData) {
        this.trainData = trainData;
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
        setupLogger();
        classifierAgents = new ArrayList<>();
        classifierPredictions = new HashMap<>();
        classifierPredictions2 = new HashMap<>();

        //Register the SL content language
        getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL);

        //Register the mobility ontology
        getContentManager().registerOntology(JADEManagementOntology.getInstance());

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

    private void setupLogger() {
        FileHandler fh;

        try {
            // This block configure the logger with handler and formatter
            fh = new FileHandler("./logs/ManagerAgent.log");
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

    public void createClassifiers() {
        AgentContainer ac = this.getContainerController();
        try {
            for (int i = 0; i < this.configuration.getClassifiers(); i++) {
                AgentController aux = ac.createNewAgent("Classifier_" + i, "agents.ClassifierAgent", null);
                aux.start();
                classifierAgents.add(aux);
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

            trainData.randomize(new java.util.Random());
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
                    new BufferedReader(new FileReader("./data/" + configuration.getFile()));
            ArffReader arff = new ArffReader(reader);

            Instances data = arff.getData();
            data.setClassIndex(data.numAttributes() - 1);
            int trainSize = data.numInstances() - configuration.getClassifyInstances();
            Instances trainData = new Instances(data, 0, trainSize);
            setTrainData(trainData);
            setTestData(new Instances(data, trainSize, configuration.getClassifyInstances()));
            //TODO: Take out
            System.out.println("TRUTH:");
            for (int i = 0; i < testData.numInstances(); i++) {
                System.out.println(testData.instance(i).classValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setRandomTrainDataForClassifiers() {
        setClassifiersTrainData(createTrainingSubset(configuration.getTrainingSettings(), trainData));
    }


    public void setPrediction(AID sender, List<Double> predictions) {
        classifierPredictions.put(sender, predictions);
    }

    public void setPrediction2(int i, List<Double> predictions){
        classifierPredictions2.put(i, predictions);
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

    public List<Double> votePredictions2() {
        //TODO Improve
        List<Double> finalPrediction = new ArrayList<>();
        List<Integer> ids = new ArrayList<>(classifierPredictions2.keySet());

        for (int i = 0; i < classifierPredictions2.get(ids.get(0)).size(); i++) { // For each instance
            Map<Double, Integer> options = new HashMap<>();

            int maxVotes = 0;
            Double maxVoted = null;
            for (Integer id : ids) { // For each agent
                Double agentVote = classifierPredictions2.get(id).get(i);
                int trainingSize = configuration.getTrainingSettings()[id];

                if (!options.containsKey(agentVote)) {
                    options.put(agentVote, 0);
                }

                int oldCount = options.get(agentVote);
                int newCount = oldCount + trainingSize;
                options.put(agentVote, newCount);
                System.out.println("Agent amb " + trainingSize + " vota " + agentVote);

                if (newCount > maxVotes) {
                    maxVotes = newCount;
                    maxVoted = agentVote;
                }
            }

            finalPrediction.add(maxVoted);
        }
        return finalPrediction;
    }
}
