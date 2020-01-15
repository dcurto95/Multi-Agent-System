package agents;

import behaviours.ManagerBehaviour;
import jade.content.lang.sl.SLCodec;
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
    private Map<Integer, List<Double>> classifierPredictions;

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

    private Instances[] createTrainingSubset(int[] trainingSettings, Instances trainData) throws Exception {
        Instances[] trainingSets = new Instances[trainingSettings.length];

        trainData.randomize(new java.util.Random());

        int dataLength = trainData.numInstances();
        int lastInstance = 0;
        for (int i = 0; i < trainingSettings.length; i++) {
            int trainSize = trainingSettings[i];

            Instances classifierData;
            if (trainSize > dataLength){
                throw new Exception("The training size is bigger than the dataset");
            }
            if ((lastInstance + trainSize) < dataLength) {
                classifierData = new Instances(trainData, lastInstance, trainSize);
                lastInstance += trainSize;
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
            data.randomize(new java.util.Random());
            int trainSize = data.numInstances() - configuration.getClassifyInstances();
            setTrainData(new Instances(data, 0, trainSize));
            setTestData(new Instances(data, trainSize, configuration.getClassifyInstances()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setRandomTrainDataForClassifiers() throws Exception {
        setClassifiersTrainData(createTrainingSubset(configuration.getTrainingSettings(), trainData));
    }

    public void setPrediction(int AIDIndex, List<Double> predictions) {
        classifierPredictions.put(AIDIndex, predictions);
    }

    private List<Double> votePredictionsPlurality() {
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

    private List<Double> votePredictionsProportional() {
        List<Double> finalPrediction = new ArrayList<>();
        List<Integer> AIDIndexes = new ArrayList<>(classifierPredictions.keySet());

        for (int i = 0; i < classifierPredictions.get(AIDIndexes.get(0)).size(); i++) { // For each instance
            Map<Double, Integer> options = new HashMap<>();

            int maxVotes = 0;
            Double maxVoted = null;
            for (Integer AIDIndex : AIDIndexes) { // For each agent
                Double agentVote = classifierPredictions.get(AIDIndex).get(i);
                int trainingSize = configuration.getTrainingSettings()[AIDIndex];

                if (!options.containsKey(agentVote)) {
                    options.put(agentVote, 0);
                }

                int oldCount = options.get(agentVote);
                int newCount = oldCount + trainingSize;
                options.put(agentVote, newCount);

                if (newCount > maxVotes) {
                    maxVotes = newCount;
                    maxVoted = agentVote;
                }
            }

            finalPrediction.add(maxVoted);
        }
        return finalPrediction;
    }

    private List<Double> votePredictionsEnhanced() {
        List<Double> finalPrediction = new ArrayList<>();
        List<Integer> AIDIndexes = new ArrayList<>(classifierPredictions.keySet());

        int[] trainingSizes = configuration.getTrainingSettings();
        int maxTrainingSize = Collections.max(Arrays.stream(trainingSizes).boxed().collect(Collectors.toList()));
        Map<Integer, Integer> effectiveNumberOfVotes = new HashMap<>();
        for (int i = 0; i < trainingSizes.length; i++) {
            if (trainingSizes[i] > 0.5 * maxTrainingSize) {
                effectiveNumberOfVotes.put(i, trainingSizes[i]);
            } else {
                effectiveNumberOfVotes.put(i, (int) Math.ceil(0.5 * trainingSizes[i]));
            }
        }

        for (int i = 0; i < classifierPredictions.get(AIDIndexes.get(0)).size(); i++) { // For each instance
            Map<Double, Integer> votesPerOption = new HashMap<>();
            Map<Double, Integer> votersPerOption = new HashMap<>();

            int maxVotes = 0;
            List<Double> maxVoted = new ArrayList<>();
            for (Integer AIDIndex : AIDIndexes) { // For each agent
                Double agentVote = classifierPredictions.get(AIDIndex).get(i);
                int numberOfVotes = effectiveNumberOfVotes.get(AIDIndex);

                if (!votesPerOption.containsKey(agentVote)) {
                    votesPerOption.put(agentVote, 0);
                    votersPerOption.put(agentVote, 0);
                }

                int oldCount = votesPerOption.get(agentVote);
                int newCount = oldCount + numberOfVotes;
                votesPerOption.put(agentVote, newCount);

                int newNumberVoters = votersPerOption.get(agentVote) + 1;
                votersPerOption.put(agentVote, newNumberVoters);

                if (newCount > maxVotes) {
                    maxVotes = newCount;
                    maxVoted = new ArrayList<>(List.of(agentVote));
                } else if (newCount == maxVotes) {
                    maxVoted.add(agentVote);
                }
            }

            if (maxVoted.size() == 1) {
                finalPrediction.add(maxVoted.get(0));
            } else if (maxVoted.size() > 1) { // TIE
                List<Double> maxVoted2 = new ArrayList<>();
                int maxVoters = 0;
                for (Double vote : maxVoted) {
                    int numberVoters = votersPerOption.get(vote);
                    if (numberVoters > maxVoters) {
                        maxVoters = numberVoters;
                        maxVoted2 = new ArrayList<>(List.of(vote));
                    } else if (numberVoters == maxVoters) {
                        maxVoted2.add(vote);
                    }
                }
                if (maxVoted2.size() == 1) {
                    finalPrediction.add(maxVoted2.get(0));
                } else { // TIE
                    finalPrediction.add(maxVoted2.get(new Random().nextInt(maxVoted2.size())));
                }
            }
        }
        return finalPrediction;
    }

    public String getResults() {
        String result = "";
        result = result.concat("TRUTH:\n[");
        for (int i = 0; i < testData.numInstances(); i++) {
            if (i != 0) result = result.concat(", ");
            result = result.concat(String.valueOf(testData.instance(i).classValue()));
        }

        result = result.concat("]\nPREDICTIONS:\n");
//        List<Double> predictions = votePredictionsPlurality();
        List<Double> predictions = votePredictionsEnhanced();
        result = result.concat(predictions.toString() + "\n");

        result = result.concat("\nACCURACY:\n");
        int correct = 0;
        for (int i = 0; i < testData.numInstances(); i++) {
            if (testData.instance(i).classValue() == predictions.get(i)) correct++;
        }
        result = result.concat((((float) correct / testData.numInstances()) * 100) + "%\n");

        return result;
    }

    public void resetClassifierPredictions() {
        classifierPredictions = new HashMap<>();
    }
}
