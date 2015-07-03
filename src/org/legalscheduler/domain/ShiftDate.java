package org.legalscheduler.domain;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class ShiftDate implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Date date;
    private int shiftNumber;
    private int weekNumber;
    private List<Shift> shifts;
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
    public int getShiftNumber() {
        return shiftNumber;
    }
    public void setShiftNumber(int shiftNumber) {
        this.shiftNumber = shiftNumber;
    }
    public int getWeekNumber() {
        return weekNumber;
    }
    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }
    public List<Shift> getShifts() {
        return shifts;
    }
    public void setShifts(List<Shift> shifts) {
        this.shifts = shifts;
    }
}
