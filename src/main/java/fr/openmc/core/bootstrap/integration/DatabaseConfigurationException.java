package fr.openmc.core.bootstrap.integration;

/**
 * Signale une configuration de base de donnees invalide ou une base injoignable.
 * Le message est destine a l'administrateur du serveur, il doit rester lisible sans stacktrace.
 */
public class DatabaseConfigurationException extends RuntimeException {
    public DatabaseConfigurationException(String message) {
        super(message);
    }

    public DatabaseConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
