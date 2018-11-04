package config;


import org.cfg4j.provider.ConfigurationProvider;
import org.cfg4j.provider.ConfigurationProviderBuilder;
import org.cfg4j.source.ConfigurationSource;
import org.cfg4j.source.context.environment.Environment;
import org.cfg4j.source.context.propertiesprovider.YamlBasedPropertiesProvider;
import org.cfg4j.source.empty.EmptyConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ConfigKeeper {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigKeeper.class);

    private final HttpSettings httpSettings;
    private final StoreSettings storeSettings;



    public ConfigKeeper(String filename) {
        ConfigurationProvider provider = fileSourceProvider(filename);
        httpSettings = provider.bind("httpserver", HttpSettings.class);
        storeSettings = provider.bind("store", StoreSettings.class);
    }

    public HttpSettings getHttpSettings() {
        return httpSettings;
    }

    public StoreSettings getStoreSettings() {
        return storeSettings;
    }

    private static ConfigurationProvider fileSourceProvider(final String filename) {
        ConfigurationSource source = new EmptyConfigurationSource() {
            private Properties properties = new YamlBasedPropertiesProvider()
                    .getProperties(ClassLoader.class.getResourceAsStream("/" + filename));

            @Override
            public Properties getConfiguration(Environment environment) {
                return properties;
            }
        };
        return new ConfigurationProviderBuilder()
                .withConfigurationSource(source)
                .build();
    }
}
