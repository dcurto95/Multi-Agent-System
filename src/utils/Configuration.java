package utils;

import java.io.Serializable;

public class Configuration implements Serializable {
    private String title;
    private String algorithm;
    private int classifiers;
    private int[] trainingSettings;
    private int classifyInstances;
    private String file;

    public Configuration(String title, String algorithm, int classifiers, int[] trainingSettings, int classifyInstances, String file) {
        this.title = title;
        this.algorithm = algorithm;
        this.classifiers = classifiers;
        this.trainingSettings = trainingSettings;
        this.classifyInstances = classifyInstances;
        this.file = file;
    }

    public Configuration() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getClassifiers() {
        return classifiers;
    }

    public void setClassifiers(int classifiers) {
        this.classifiers = classifiers;
    }

    public int[] getTrainingSettings() {
        return trainingSettings;
    }

    public void setTrainingSettings(int[] trainingSettings) {
        this.trainingSettings = trainingSettings;
    }

    public int getClassifyInstances() {
        return classifyInstances;
    }

    public void setClassifyInstances(int classifyInstances) {
        this.classifyInstances = classifyInstances;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
