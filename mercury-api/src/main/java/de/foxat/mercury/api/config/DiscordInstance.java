package de.foxat.mercury.api.config;

public class DiscordInstance {
    private final String name;
    private final String id;
    private final String token;

    public DiscordInstance(String name, String id, String token) {
        this.name = name;
        this.id = id;
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "DiscordInstance{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
