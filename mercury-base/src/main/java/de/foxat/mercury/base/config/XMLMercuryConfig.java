package de.foxat.mercury.base.config;

import de.foxat.mercury.api.config.DiscordInstance;
import de.foxat.mercury.api.config.MercuryConfig;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XMLMercuryConfig implements MercuryConfig {

    public static final String FILE_LOCATION = "config/mercury-system.xml";

    private static XMLMercuryConfig instance;

    private final Document document;

    // system properties

    private String systemName;
    private String mainInstanceId;
    private boolean lazyLoaded;

    // config properties

    private boolean isGlobalInstancesDisabled;
    private boolean homeGuild;
    private String homeGuildId;

    // other

    DiscordInstance rootInstance;
    List<DiscordInstance> discordInstances;

    private XMLMercuryConfig() throws IOException, ParserConfigurationException, SAXException {
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(FILE_LOCATION));
        document.getDocumentElement().normalize();

        parseDocument();
    }

    private void parseDocument() throws NullPointerException, ClassCastException {
        parseGlobalConfigs();
        parseInstances();
    }

    private void parseGlobalConfigs() throws NullPointerException, ClassCastException {
        // system properties
        Element system = (Element) document.getElementsByTagName("system").item(0);
        systemName = system.getElementsByTagName("name").item(0).getTextContent();
        mainInstanceId = system.getElementsByTagName("mainInstance").item(0).getTextContent();
        lazyLoaded = Boolean.parseBoolean(system.getElementsByTagName("lazyModules").item(0).getTextContent());

        // other (module) properties
        Element config = (Element) document.getElementsByTagName("config").item(0);
        homeGuild = Boolean.parseBoolean(config.getElementsByTagName("hasHomeGuild").item(0).getTextContent());
        if (homeGuild) {
            homeGuildId = config.getElementsByTagName("homeGuild").item(0).getTextContent();
        }

        NodeList disableGlobalInstances = config.getElementsByTagName("disableGlobalInstances");
        if (disableGlobalInstances.getLength() > 0) {
            this.isGlobalInstancesDisabled = Boolean.parseBoolean(disableGlobalInstances.item(0).getTextContent());
        }
    }

    private void parseInstances() throws NullPointerException, ClassCastException {
        Element instances = (Element) document.getElementsByTagName("instances").item(0);
        Element root = (Element) instances.getElementsByTagName("root").item(0);
        rootInstance = new DiscordInstance(
                "root",
                root.getElementsByTagName("id").item(0).getTextContent(),
                root.getElementsByTagName("token").item(0).getTextContent()
        );

        discordInstances = new ArrayList<>();
        discordInstances.add(rootInstance);
        NodeList instanceNodeList = instances.getElementsByTagName("instance");

        for (int i = 0; i < instanceNodeList.getLength(); i++) {
            Element currentInstance = (Element) instanceNodeList.item(i);
            discordInstances.add(new DiscordInstance(
                    currentInstance.getElementsByTagName("name").item(0).getTextContent(),
                    currentInstance.getElementsByTagName("id").item(0).getTextContent(),
                    currentInstance.getElementsByTagName("token").item(0).getTextContent()
            ));
        }
    }

    public static XMLMercuryConfig getInstance() {
        if (instance == null) {
            try {
                instance = new XMLMercuryConfig();
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

        return instance;
    }

    /* --- interface getter methods --- */

    @Override
    public String getSystemName() {
        return systemName;
    }

    @Override
    public String getMainInstanceId() {
        return mainInstanceId;
    }

    @Override
    public boolean isGlobalInstancesDisabled() {
        return isGlobalInstancesDisabled;
    }

    @Override
    public boolean isLazyLoaded() {
        return lazyLoaded;
    }

    @Override
    public boolean hasHomeGuild() {
        return homeGuild;
    }

    @Override
    public String getHomeGuildId() {
        return homeGuildId;
    }

    @Override
    public DiscordInstance getRootInstance() {
        return rootInstance;
    }

    @Override
    public List<DiscordInstance> getInstances() {
        if (isGlobalInstancesDisabled()) {
            return List.of();
        }

        return discordInstances;
    }
}
