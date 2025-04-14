package hu.egyetem.vizilabdapp.data.model;

public class Team {
    private String name;
    private String logoUrl;

    public Team() {
        // Üres konstruktor szükséges a Firebase-hez
    }

    public Team(String name, String logoUrl) {
        this.name = name;
        this.logoUrl = logoUrl;
    }

    public String getName() {
        return name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }
}
