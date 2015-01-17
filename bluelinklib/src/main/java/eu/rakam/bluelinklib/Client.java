package eu.rakam.bluelinklib;

public class Client {

    private final String name;

    public Client(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
