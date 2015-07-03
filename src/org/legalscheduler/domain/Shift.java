package org.legalscheduler.domain;

import java.io.Serializable;
import java.text.SimpleDateFormat;

public class Shift implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMM yyyy");
    
    private int shiftId;
    private boolean backup;
    private ShiftDate shiftDate;
    private int slotNumber;
    public int getShiftId() {
        return shiftId;
    }
    public void setShiftId(int shiftId) {
        this.shiftId = shiftId;
    }
    public boolean isBackup() {
        return backup;
    }
    public void setBackup(boolean backup) {
        this.backup = backup;
    }
    public ShiftDate getShiftDate() {
        return shiftDate;
    }
    public void setShiftDate(ShiftDate date) {
        this.shiftDate = date;
    }
    public int getSlotNumber() {
        return slotNumber;
    }
    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }
    public String toString() {
        
        String desc = DATE_FORMAT.format(shiftDate.getDate()) + " slot #" + slotNumber;
        if (backup) {
            desc += " (backup)";
        }
        return desc;
     
    }
}
