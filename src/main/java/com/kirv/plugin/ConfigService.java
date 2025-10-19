package com.kirv.plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigService {
    private Integer serverSidePort = 5000;
    private String configFileName = "code_agent_cnfg.env";
    private Path configFilePath;

    public ConfigService() {
        String userHomeDirectory = System.getProperty("user.home");
        configFilePath = Paths.get(userHomeDirectory, configFileName);
    }

    public void loadConfigFile() {
        Properties properties = new Properties();

        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath.toFile()))) {
            properties.load(reader);

            // Read PORT parameter with default value 5000
            String portStr = properties.getProperty("HTTP_PORT", "5000");
            try {
                serverSidePort = Integer.parseInt(portStr.trim());
            } catch (NumberFormatException e) {
                // If PORT value is invalid, use default 5000
                serverSidePort = 5000;
                System.err.println("Invalid HTTP_PORT value in config file, using default: 5000");
            }

        } catch (IOException e) {
            // Config file not found or cannot be read - use default port
            serverSidePort = 5000;
        }
    }

    public void createConfigFromDefault() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("META-INF/code_agent_cnfg.example")) {
            if (input == null) {
                System.err.println("Cant create config file: resources META-INF/code_agent_cnfg.example is empty");
                return;
            }
            String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            content = "# path to config file: " + configFilePath.toAbsolutePath() + "\n\n" + content;

            try {
                Writer writer = new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(configFilePath.toFile()), "utf-8"
                        )
                );
                writer.write(content);
                writer.close();
            }
            catch (IOException e) {
                System.err.println("Cant create config file: " + configFilePath.toAbsolutePath());
            }
        }
        catch (IOException e) {
            System.err.println("Cant create config file: resources META-INF/code_agent_cnfg.example is empty");
        }
    }

    public Integer getPort() {
        return serverSidePort;
    }

    public Path getConfigFilePath() {
        return configFilePath;
    }
}
