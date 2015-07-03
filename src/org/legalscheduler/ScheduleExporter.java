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
            String[] headerRow = new String[] {"Date", "Attorney #1", "Attorney #2", "Backup Attorney"};
            csvWriter.writeNext(headerRow);
            
            for (ShiftDate shiftDate : schedule.getShiftDates()) {
                List<String> row = new ArrayList<String>();
                row.add(SchedulerApplication.OUTPUT_DATE_FORMAT.format(shiftDate.getDate()));
                // ScheduleLoader initialized this list so that the backup list is the final
                // entry. Since the list order isn't changed, we're not bothering to check again.
                for (Shift shift : shiftDate.getShifts()) {
                    ShiftAssignment assignment = getAssignment(schedule, shift);
                    if (assignment.getEmployee() == null) {
                        row.add("Unassigned");
                    } else {
                        row.add(assignment.getEmployee().getName());
                    }
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
