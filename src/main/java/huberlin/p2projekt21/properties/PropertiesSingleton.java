package huberlin.p2projekt21.properties;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Singleton class used to wrap application property access
 */
public class PropertiesSingleton {
    private static PropertiesSingleton INSTANCE;
    private final static String FILE_NAME = "application.properties";
    private Properties properties;

    /**
     * @return Unique instance of this class
     * @throws IOException
     */
    public static PropertiesSingleton getInstance() throws IOException {
        if (INSTANCE == null) {
            INSTANCE = new PropertiesSingleton();
        }
        return INSTANCE;
    }

    public PropertiesSingleton() throws IOException {
        this.properties = new Properties();

        if (Files.exists(Paths.get(FILE_NAME))) {
            this.properties.load(new FileInputStream(FILE_NAME));
        } else {
            this.properties.load(getClass().getClassLoader().getResourceAsStream(FILE_NAME));
        }
    }

    /**
     * This method returns the value of a given key
     *
     * @param key Stored value for this key
     * @return String value if the key-value pair is set, null otherwise
     */
    public String get(String key) {
        return properties.getProperty(key);
    }

    /**
     * This method sets the key-value pair
     *
     * @param key
     * @param value
     */
    public void set(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * This method removes the associated value
     *
     * @param key Key of to be removed key-value pair
     */
    public void remove(String key) {
        properties.remove(key);
    }

    /**
     * This method persists the application properties to the local machine
     *
     * @throws IOException
     */
    public void store() throws IOException {
        properties.store(new FileOutputStream(FILE_NAME), null);
        properties.load(new FileInputStream(FILE_NAME));
    }
}
