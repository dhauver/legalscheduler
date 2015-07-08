package org.legalscheduler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.legalscheduler.domain.Schedule;
import org.legalscheduler.domain.Shift;
import org.legalscheduler.domain.ShiftAssignment;
import org.legalscheduler.domain.ShiftDate;

import au.com.bytecode.opencsv.CSVWriter;

public class ScheduleExporter {
    public void export(Schedule schedule, File file) {
        try {
            
            FileWriter fileWriter = new FileWriter(file);
            CSVWriter csvWriter = new CSVWriter(fileWriter);
            // Figure out the maximum number of primary and backup shifts that occur on
            // any date
            int maxPrimaryShifts = schedule.getMaxNumberOfPrimaryShifts();
            int maxBackupShifts = schedule.getMaxNumberOfBackupShifts();
            List<String> headerRow = new ArrayList<String>();
            headerRow.add("Date");
            for (int i = 1; i <= maxPrimaryShifts; ++i) {
                headerRow.add("Attorney #" + i);
            }
            for (int i = 1; i <= maxBackupShifts; ++i) {
                headerRow.add("Backup Attorney #" + i);
            }
            csvWriter.writeNext(headerRow.toArray(new String[0]));
            
            for (ShiftDate shiftDate : schedule.getShiftDates()) {
                List<String> row = new ArrayList<String>();
                row.add(SchedulerApplication.OUTPUT_DATE_FORMAT.format(shiftDate.getDate()));
                // ScheduleLoader initialized this list so that all primary shifts come before
                // backup shifts.
                boolean backupShiftSeen = false;
                int numPrimaryShiftsSeen = 0;
                int numBackupShiftsSeen = 0;
                for (Shift shift : shiftDate.getShifts()) {
                    if (shift.isBackup()) {
                        // If this is the first backup shift seen on this date, 
                        // add empty columns if the number of primary shifts is less than the max
                        if (backupShiftSeen == false) {
                            for (int i = numPrimaryShiftsSeen; i < maxPrimaryShifts; ++i) {
                                row.add("");
                            }
                        }
                        backupShiftSeen = true;
                        ++numBackupShiftsSeen;
                    } else {
                        ++numPrimaryShiftsSeen;
                    }
                    ShiftAssignment assignment = getAssignment(schedule, shift);
                    if (assignment.getEmployee() == null) {
                        row.add("Unassigned");
                    } else {
                        row.add(assignment.getEmployee().getName());
                    }
                }
                // Add empty columns if the number of primary shifts is less than the max
                for (int i = numBackupShiftsSeen; i < maxBackupShifts; ++i) {
                    row.add("");
                }
                csvWriter.writeNext(row.toArray(new String[0]));
            }
            
            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while attempting to save the results to " 
                    + file.getAbsolutePath(), e);
        }
    }
    
    private ShiftAssignment getAssignment(Schedule schedule, Shift shift) {
        for (ShiftAssignment assignment : schedule.getShiftAssignments()) {
            if (assignment.getShift() == shift) {
                return assignment;
            }
        }
        return null;
    }
}
