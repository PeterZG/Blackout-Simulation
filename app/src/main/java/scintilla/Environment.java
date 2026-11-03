package scintilla;

public class Environment {
    private static final String PREFIX = "scintilla:";
    private static volatile String ipAddress = null;
    private static volatile Integer port = null;
    private static volatile Boolean headless = null;
    private static volatile Boolean securable = null;

    public static final synchronized String getIPAddress() {
        return (ipAddress == null && (ipAddress = System.getenv(PREFIX + "ADDRESS")) == null) ? ipAddress = "127.0.0.1"
                : ipAddress;
    }

    public static final synchronized int getPort() {
        if (port != null)
            return port;
        try {
            String configured = System.getenv(PREFIX + "PORT");
            if (configured == null) configured = System.getenv(PREFIX + "port");
            return port = Integer.parseInt(configured);
        } catch (Exception e) {
            return port = 4567;
        }
    }

    public static final synchronized boolean isHeadless() {
        return headless != null ? headless.booleanValue() : (headless = (System.getenv(PREFIX + "HEADLESS") != null));
    }

    public static final synchronized boolean isSecure() {
        return securable != null ? securable.booleanValue() : (securable = (System.getenv(PREFIX + "SECURE") != null));
    }
}
