package org.legalscheduler.domain;

import java.io.Serializable;
import java.util.List;

public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private AttorneyType attorneyType;
    private int numberOfPrimaryShifts;
    private int numberOfBackupShifts;
    private List<ShiftDate> availableDates;
    private List<ShiftDate> availableIfNeededDates;
    private int weight = 1;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public AttorneyType getAttorneyType() {
        return attorneyType;
    }
    public void setAttorneyType(AttorneyType attorneyType) {
        this.attorneyType = attorneyType;
    }
    public int getNumberOfPrimaryShifts() {
        return numberOfPrimaryShifts;
    }
    public void setNumberOfPrimaryShifts(int numberOfPrimaryShifts) {
        this.numberOfPrimaryShifts = numberOfPrimaryShifts;
    }
    public int getNumberOfBackupShifts() {
        return numberOfBackupShifts;
    }
    public void setNumberOfBackupShifts(int numberOfBackupShifts) {
        this.numberOfBackupShifts = numberOfBackupShifts;
    }
    public List<ShiftDate> getAvailableDates() {
        return availableDates;
    }
    public void setAvailableDates(List<ShiftDate> availableDates) {
        this.availableDates = availableDates;
    }
    public List<ShiftDate> getAvailableIfNeededDates() {
        return availableIfNeededDates;
    }
    public void setAvailableIfNeededDates(List<ShiftDate> availableIfNeededDates) {
        this.availableIfNeededDates = availableIfNeededDates;
    }
    public int getWeight() {
        return weight;
    }
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
}
