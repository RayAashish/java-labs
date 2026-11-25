package jdbc.models;

import java.util.Objects;

/**
 * Domain entity representing a Laptop (preserved and enhanced from jdbc-with-IO).
 */
public class Laptop {

    private int id;
    private String name;
    private int ram;
    private int ssd;

    public Laptop() {
    }

    public Laptop(String name, int ram, int ssd) {
        this(0, name, ram, ssd);
    }

    public Laptop(int id, String name, int ram, int ssd) {
        this.id = id;
        this.name = name;
        this.ram = ram;
        this.ssd = ssd;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRam() {
        return ram;
    }

    public void setRam(int ram) {
        this.ram = ram;
    }

    public int getSsd() {
        return ssd;
    }

    public void setSsd(int ssd) {
        this.ssd = ssd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Laptop laptop)) return false;
        return id == laptop.id && ram == laptop.ram && ssd == laptop.ssd && Objects.equals(name, laptop.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, ram, ssd);
    }

    @Override
    public String toString() {
        return String.format("Laptop[id=%d, model='%s', RAM=%dGB, SSD=%dGB]", id, name, ram, ssd);
    }
}
