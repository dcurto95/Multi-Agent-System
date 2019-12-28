package utils;

import weka.core.Instances;

import java.io.Serializable;

public class ClassifierConfig implements Serializable {
    private String algorithm;
    private String action;
    private Instances data;

    public ClassifierConfig(String algorithm, String action, Instances data) {
        this.algorithm = algorithm;
        this.action = action;
        this.data = data;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public Instances getData() {
        return data;
    }

    public String getAction() {
        return action;
    }
}
