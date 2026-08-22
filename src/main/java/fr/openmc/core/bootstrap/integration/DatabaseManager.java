package fr.openmc.core.bootstrap.integration;

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.logger.Level;
import com.j256.ormlite.logger.LocalLogBackend;
import com.j256.ormlite.support.ConnectionSource;
import fr.openmc.core.OMCPlugin;
import fr.openmc.core.bootstrap.features.Feature;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.nio.channels.ConnectionPendingException;
import java.sql.SQLException;

/**
 * Gere la connexion base de donnees et l'initialisation des features persistantes.
 */
public class DatabaseManager {
    @Getter
    private static ConnectionSource connectionSource;

    private static final String CONFIG_HELP = """
            OpenMC a besoin d'une base MySQL/MariaDB pour demarrer.
            Renseignez la section "database" de plugins/OpenMC/config.yml :
              database:
                url: "jdbc:mysql://<adresse>:<port>/<base>"   (ex: jdbc:mysql://127.0.0.1:3306/openmc)
                username: "<utilisateur>"
                password: "<mot de passe>"
            La base indiquee doit exister et l'utilisateur doit avoir les droits dessus.""";

    /**
     * Initialise le driver, la connexion pool et les features de type DB.
     *
     * @throws RuntimeException Si le driver ou la connexion DB échoue
     */
    public static void init() {
        try {
            if (OMCPlugin.isUnitTestVersion()) {
                Class.forName("org.h2.Driver");
            } else {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
        } catch (ClassNotFoundException e) {
            OMCLogger.error("Database driver not found. Please ensure the MySQL or H2 driver is included in the classpath.");
            throw new RuntimeException(e);
        }

        // ormlite
        try {
            FileConfiguration config = OMCPlugin.getConfigs();
            String databaseUrl = config.getString("database.url");
            String username = config.getString("database.username");
            String password = config.getString("database.password");

            if (!OMCPlugin.isUnitTestVersion()) checkConfiguration(databaseUrl, username);

            connectionSource = new JdbcPooledConnectionSource(databaseUrl, username, password);
            if (!OMCPlugin.isUnitTestVersion()) checkConnection(databaseUrl, username);

            OMCPlugin.getInstance().REGISTRY_HOOKS
                    .forEach(h -> {
                        try {
                            h.startDB(connectionSource);
                        } catch (SQLException e) {
                            OMCLogger.error("Failed to initialize the database connection.", e);
                            throw new RuntimeException(e);
                        } catch (ConnectionPendingException e) {
                            OMCLogger.error("Database connection is pending. Please check your database configuration.");
                            throw new RuntimeException(e);
                        } catch (NoClassDefFoundError e) {
                            OMCLogger.errorFormatted("Plugin has failed to start feature because {} does not exist.",
                                    e.getMessage());
                        }
                    });

            OMCPlugin.getInstance().REGISTRY_FEATURE
                    .forEach(f -> {
                        try {
                            Feature feature = f.create();
                            feature.startDB(connectionSource);
                        } catch (SQLException e) {
                            OMCLogger.error("Failed to initialize the database connection.", e);
                            throw new RuntimeException(e);
                        } catch (ConnectionPendingException e) {
                            OMCLogger.error("Database connection is pending. Please check your database configuration.");
                            throw new RuntimeException(e);
                        } catch (NoClassDefFoundError e) {
                            OMCLogger.errorFormatted("Plugin has failed to start feature because {} does not exist.",
                                    e.getMessage());
                        }
                    });
        } catch (SQLException e) {
            throw new DatabaseConfigurationException(
                    "Connexion a la base impossible : " + e.getMessage() + "\n" + CONFIG_HELP, e);
        } catch (ConnectionPendingException e) {
            OMCLogger.error("Database connection is pending. Please check your database configuration.");
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifie que la configuration DB est exploitable avant toute tentative de connexion.
     *
     * @param databaseUrl URL JDBC configuree
     * @param username Utilisateur configure
     * @throws DatabaseConfigurationException Si l'URL ou l'utilisateur est absent ou encore a sa valeur d'exemple
     */
    private static void checkConfiguration(String databaseUrl, String username) {
        if (databaseUrl == null || databaseUrl.isBlank())
            throw new DatabaseConfigurationException("\"database.url\" n'est pas renseigne.\n" + CONFIG_HELP);

        if (!databaseUrl.startsWith("jdbc:"))
            throw new DatabaseConfigurationException(
                    "\"database.url\" (" + databaseUrl + ") n'est pas une URL JDBC.\n" + CONFIG_HELP);

        if (databaseUrl.contains("host:port") || databaseUrl.contains("<"))
            throw new DatabaseConfigurationException(
                    "\"database.url\" (" + databaseUrl + ") contient encore une valeur d'exemple.\n" + CONFIG_HELP);

        if (username == null || username.isBlank())
            throw new DatabaseConfigurationException("\"database.username\" n'est pas renseigne.\n" + CONFIG_HELP);
    }

    /**
     * Verifie que la base configuree est joignable, la connexion ORMLite etant paresseuse.
     *
     * @param databaseUrl URL JDBC configuree
     * @param username Utilisateur configure
     * @throws DatabaseConfigurationException Si la base est injoignable ou refuse la connexion
     */
    private static void checkConnection(String databaseUrl, String username) {
        try {
            connectionSource.releaseConnection(connectionSource.getReadWriteConnection(null));
        } catch (SQLException e) {
            throw new DatabaseConfigurationException(
                    "Connexion impossible a " + databaseUrl + " avec l'utilisateur \"" + username + "\" : "
                            + e.getMessage() + "\n" + CONFIG_HELP, e);
        }
    }

    /**
     * Filtre les logs OrmLite trop verbeux lors du demarrage.
     */
    public static class ShutUpOrmLite extends LocalLogBackend {
        private final String classLabel;

        /**
         * Crée un filtre de logs OrmLite pour une classe donnée.
         *
         * @param classLabel Label de classe ORMLite
         */
        public ShutUpOrmLite(String classLabel) {
            super(classLabel);
            this.classLabel = classLabel;
        }

        /**
         * Indique si un niveau est autorisé.
         *
         * @param level Niveau ORMLite
         * @return True si le niveau est autorisé
         */
        @Override
        public boolean isLevelEnabled(Level level) {
            return Level.INFO.isEnabled(level);
        }

        /**
         * Log un message ORMLite si non filtré.
         *
         * @param level Niveau du log
         * @param msg Message
         */
        @Override
        public void log(Level level, String msg) {
            if (classLabel.contains("com.j256.ormlite.table.TableUtils") || msg.contains("DaoManager created dao for class class"))
                return;

            super.log(level, msg);
        }

        /**
         * Log un message ORMLite avec exception si non filtré.
         *
         * @param level Niveau du log
         * @param msg Message
         * @param throwable Exception associée
         */
        @Override
        public void log(Level level, String msg, Throwable throwable) {
            if (classLabel.contains("com.j256.ormlite.table.TableUtils") || msg.contains("DaoManager created dao for class class"))
                return;

            super.log(level, msg, throwable);
        }
    }
}
