package utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class XmlParser {

    public static Configuration parseConfigFile(String fileName) throws ParserConfigurationException, IOException, SAXException {

        Configuration configFile = new Configuration();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File("./resources/" + fileName));
        document.getDocumentElement().normalize();
        //Here comes the root node
        Element root = document.getDocumentElement();
        configFile.setTitle(root.getElementsByTagName("title").item(0).getTextContent());
        configFile.setAlgorithm(root.getElementsByTagName("algorithm").item(0).getTextContent());
        configFile.setClassifiers(Integer.parseInt(root.getElementsByTagName("classifiers").item(0).getTextContent()));
        List<String> trainingSettingsList = Arrays.asList(root.getElementsByTagName("trainingSettings").item(0).getTextContent().split(","));

        int[] list = new int[trainingSettingsList.size()];
        for (int i = 0; i < trainingSettingsList.size(); i++) {
            list[i] = Integer.parseInt(trainingSettingsList.get(i));
        }
        configFile.setTrainingSettings(list);
        configFile.setClassifyInstances(Integer.parseInt(root.getElementsByTagName("classifyInstances").item(0).getTextContent()));
        configFile.setFile(root.getElementsByTagName("file").item(0).getTextContent());

        return configFile;


    }
}
