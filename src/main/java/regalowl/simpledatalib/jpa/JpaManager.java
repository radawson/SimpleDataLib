package regalowl.simpledatalib.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import regalowl.simpledatalib.SimpleDataLib;

/**
 * Optional JPA/Hibernate integration manager for SimpleDataLib.
 * This provides EntityManagerFactory management for plugins that want to use JPA.
 * 
 * Note: This is an optional feature. Plugins can continue using the JDBC-based
 * SQLManager for database operations without requiring JPA.
 */
public class JpaManager {
    
    private SimpleDataLib sdl;
    private EntityManagerFactory emf;
    private boolean enabled;
    
    /**
     * Creates a new JpaManager instance.
     * 
     * @param sdl The SimpleDataLib instance
     */
    public JpaManager(SimpleDataLib sdl) {
        this.sdl = sdl;
        this.enabled = false;
    }
    
    /**
     * Initializes JPA with the provided DataSource.
     * This creates an EntityManagerFactory configured to use the SimpleDataLib DataSource.
     * 
     * @param dataSource The DataSource from SQLManager
     * @param persistenceUnitName The persistence unit name (optional, defaults to "SimpleDataLib")
     * @return true if initialization was successful
     */
    public boolean initialize(DataSource dataSource, String persistenceUnitName) {
        if (enabled) {
            return true;
        }
        
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("jakarta.persistence.nonJtaDataSource", dataSource);
            properties.put("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
            properties.put("hibernate.hikari.dataSource", dataSource);
            
            // Use Hibernate dialect based on database type
            if (sdl.getSQLManager().useMySQL()) {
                properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
            } else {
                properties.put("hibernate.dialect", "org.hibernate.dialect.SQLiteDialect");
            }
            
            emf = Persistence.createEntityManagerFactory(
                persistenceUnitName != null ? persistenceUnitName : "SimpleDataLib",
                properties
            );
            enabled = true;
            return true;
        } catch (Exception e) {
            sdl.getErrorWriter().writeError(e, "Failed to initialize JPA");
            return false;
        }
    }
    
    /**
     * Creates a new EntityManager from the factory.
     * 
     * @return A new EntityManager instance, or null if JPA is not enabled
     */
    public EntityManager createEntityManager() {
        if (!enabled || emf == null) {
            return null;
        }
        return emf.createEntityManager();
    }
    
    /**
     * Gets the EntityManagerFactory.
     * 
     * @return The EntityManagerFactory, or null if JPA is not enabled
     */
    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }
    
    /**
     * Checks if JPA is enabled and initialized.
     * 
     * @return true if JPA is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Shuts down the EntityManagerFactory.
     * Call this when your plugin is disabling to properly clean up resources.
     */
    public void shutdown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            emf = null;
        }
        enabled = false;
    }
}

