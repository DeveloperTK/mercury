package de.foxat.mercury.mm;

import de.foxat.mercury.api.Mercury;
import de.foxat.mercury.api.config.DiscordInstance;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class XMLRadioConfiguration {

    private final MercuryRadio parent;
    private final Logger logger;
    private final Document document;

    private String homeGuildId;
    private Map<String, RadioInstance> radios;

    private XMLRadioConfiguration(File configFile, MercuryRadio mercuryRadio) throws IOException,
            ParserConfigurationException, SAXException {

        this.parent = mercuryRadio;
        this.logger = mercuryRadio.getLogger();
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(configFile);
        document.getDocumentElement().normalize();

        parseDocument();
    }

    private void parseDocument() throws NullPointerException, ClassCastException {
        parseGlobalConfigs();
        parseInstances();
    }

    private void parseGlobalConfigs() throws NullPointerException, ClassCastException {
        homeGuildId = document.getElementsByTagName("homeGuild").item(0).getTextContent();
    }

    private void parseInstances() throws NullPointerException, ClassCastException {
        Element instances = (Element) document.getElementsByTagName("instances").item(0);

        radios = new HashMap<>();
        NodeList instanceNodeList = instances.getElementsByTagName("instance");

        logger.info("Loading instances...");

        for (int i = 0; i < instanceNodeList.getLength(); i++) {
            Element currentInstance = (Element) instanceNodeList.item(i);
            String name = currentInstance.getElementsByTagName("name").item(0).getTextContent();

            DiscordInstance discord = new DiscordInstance(
                    name,
                    currentInstance.getElementsByTagName("id").item(0).getTextContent(),
                    currentInstance.getElementsByTagName("token").item(0).getTextContent()
            );

            NodeList volumeNodes = currentInstance.getElementsByTagName("volume");
            int volume = 100;
            if (volumeNodes.getLength() > 0) {
                try {
                    volume = Integer.parseInt(volumeNodes.item(0).getTextContent());
                } catch (NumberFormatException exception) {
                    logger.error("Cannot parse volume " + volumeNodes.item(0).getTextContent(), exception);
                }
            }

            try {
                radios.putIfAbsent(name, new RadioInstance(
                        logger,
                        homeGuildId,
                        discord,
                        currentInstance.getElementsByTagName("channel").item(0).getTextContent(),
                        currentInstance.getElementsByTagName("playlist").item(0).getTextContent(),
                        volume
                ));
            } catch (IllegalArgumentException exception) {
                logger.error(String.format("Could not instantiate radio \"%s\"", name), exception);
            }
        }

        logger.info("Loaded all radios");
    }

    public static XMLRadioConfiguration create(File configFile, MercuryRadio mercuryRadio) {
        try {
            return new XMLRadioConfiguration(configFile, mercuryRadio);
        } catch (FileNotFoundException exception) {
            throw new IllegalStateException("Config file not found!", exception);
        } catch (IllegalArgumentException | ParserConfigurationException | SAXException exception) {
            throw new IllegalStateException("Invalid config, please check the format!", exception);
        } catch (NullPointerException | ClassCastException exception) {
            throw new IllegalStateException("Missing required config field!", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Unexpected IOException while reading config", exception);
        }
    }

    public String getHomeGuildId() {
        return homeGuildId;
    }

    public Map<String, RadioInstance> getRadios() {
        return radios;
    }
}
