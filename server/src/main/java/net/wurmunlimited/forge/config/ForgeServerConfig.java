package net.wurmunlimited.forge.config;

import java.util.Properties;

public class ForgeServerConfig extends ForgeConfig {

    public static ForgeConfig init(Properties properties) {
        if(instance==null) {
            instance = new ForgeServerConfig();
            instance.configure(properties);
        }
        return instance;
    }

    public static ForgeServerConfig getInstance() {
        if(!(instance instanceof ForgeServerConfig)) {
            throw new RuntimeException("Configurations error, wrong type of configuration class requested.");
        }
        return (ForgeServerConfig)instance;
    }

    private ForgeServerConfig() {

    }

    @Override
    public void configure(Properties properties) {
        super.configure(properties);
    }
}