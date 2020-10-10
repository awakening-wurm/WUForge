package net.wurmunlimited.forge.config;

import java.nio.file.Path;
import java.util.Properties;

public class ForgeServerConfig extends ForgeConfig {

    public static ForgeServerConfig init(Path baseDir,Properties properties) {
        if(ForgeConfig.instance==null) {
            ForgeConfig.instance = new ForgeServerConfig(baseDir);
            ForgeConfig.instance.configure(properties);
        }
        return (ForgeServerConfig)ForgeConfig.instance;
    }

    public static ForgeServerConfig getInstance() {
        if(!(ForgeConfig.instance instanceof ForgeServerConfig)) {
            throw new RuntimeException("Configurations error, wrong type of configuration class requested.");
        }
        return (ForgeServerConfig)ForgeConfig.instance;
    }

    private ForgeServerConfig(Path baseDir) {
        super(baseDir);
    }

    @Override
    public void configure(Properties properties) {
        super.configure(properties);
    }
}
