package org.legalscheduler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.legalscheduler.domain.AttorneyType;
import org.legalscheduler.domain.Employee;
import org.legalscheduler.domain.Schedule;
import org.legalscheduler.domain.Shift;
import org.legalscheduler.domain.ShiftAssignment;
import org.legalscheduler.domain.ShiftDate;

import au.com.bytecode.opencsv.CSVReader;

public class ScheduleLoader {
    
    public Schedule loadSchedule(File csvFile) {
        Schedule schedule = new Schedule();
        CSVReader csvReader = null;
        try {
            Reader fileReader = new FileReader(csvFile);
            csvReader = new CSVReader(fileReader);
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while trying to open the file", e);
        }
        try {
            String[] headerRow = csvReader.readNext();
            String[] primaryShiftsRow = csvReader.readNext();
            String[] backupShiftsRow = csvReader.readNext();
            boolean weightColumnPresent = isWeightColumnPresent(headerRow);
            Map<Integer, ShiftDate> columnDates = initShiftDates(schedule, headerRow, primaryShiftsRow, backupShiftsRow, weightColumnPresent);
            initShifts(schedule);
            schedule.setEmployees(new ArrayList<Employee>());
            int rowNumber = 4;
            String[] employeeRow = csvReader.readNext();
            while (employeeRow != null) {
                addEmployee(schedule, employeeRow, rowNumber, columnDates, weightColumnPresent);
                employeeRow = csvReader.readNext();
            }
            csvReader.close();
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while trying to read the contents of the file", e);
        }
        validateShiftTotals(schedule);
        return schedule;
    }
    
    private Map<Integer, ShiftDate> initShiftDates(Schedule schedule, 
            String[] headerRow, String[] primaryShiftsRow,
            String[] backupShiftsRow, boolean weightColumnPresent) {
        Map<Integer, ShiftDate> columnDates = new HashMap<Integer, ShiftDate>();
        List<ShiftDate> shiftDates = new ArrayList<ShiftDate>();
        int shiftNumber = 0;
        int weekNumber = -1;
        int previousWeek = -1;
        int previousYear = -1;
        List<Shift> allShifts = new ArrayList<Shift>();
        int shiftIndex = 0;
        for (int i = getFirstDateColumnIndex(weightColumnPresent); i < headerRow.length; ++i) {
            String value = headerRow[i];
            Date date = null;
            int numPrimaryShifts = 0;
            int numBackupShifts = 0;
            // The right-most column might also be used to tally up the total number of backup
            // and primary shifts, in order to cross-check with the target numbers for each
            // employee
            if (isTotal(value)) {
                break;
            }
            // This ignores any trailing empty columns that might exist to the far right.
            if (StringUtils.stripToNull(value) == null && i > 4) {
                break;
            }
            try {
                date = SchedulerApplication.INPUT_DATE_FORMAT.parse(value);
            } catch (ParseException e) {
                throw new RuntimeException("The header value in column " + (i + 1) + " is not a valid date: " + value + ". Values must be formatted to look like: " + SchedulerApplication.INPUT_DATE_FORMAT.format(new Date()));
            }
            try {
                numPrimaryShifts = Integer.parseInt(primaryShiftsRow[i]);
            } catch (NumberFormatException e) {
                throw new RuntimeException("The number of primary shifts in column " + (i + 1) + " for " + value + " is not a valid integer: " + primaryShiftsRow[i]);
            }
            try {
                numBackupShifts = Integer.parseInt(backupShiftsRow[i]);
            } catch (NumberFormatException e) {
                throw new RuntimeException("The number of backup shifts in column " + (i + 1) + " for " + value + " is not a valid integer: " + backupShiftsRow[i]);
            }
            
            
            ShiftDate shiftDate = new ShiftDate();
            shiftDate.setDate(date);
            shiftDate.setShiftNumber(shiftNumber);
            
            ++ shiftNumber;

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int curWeek = cal.get(Calendar.WEEK_OF_YEAR);
            int curYear = cal.get(Calendar.YEAR);
            if (curYear == previousYear && curWeek == previousWeek) {
                shiftDate.setWeekNumber(weekNumber);
            } else {
                ++ weekNumber;
                shiftDate.setWeekNumber(weekNumber);
                previousWeek = curWeek;
                previousYear = curYear;
            }
            for (int j = 1; j <= numPrimaryShifts; ++j) {
                shiftIndex = addShift(shiftDate, j, false, shiftIndex);
            }
            for (int j = 1; j <= numBackupShifts; ++j) {
                shiftIndex = addShift(shiftDate, j, true, shiftIndex);
            }
            allShifts.addAll(shiftDate.getShifts());
            columnDates.put(i, shiftDate);
            shiftDates.add(shiftDate);
        }
        schedule.setShiftDates(shiftDates);
        schedule.setShifts(allShifts);
        return columnDates;
    }
    
    private void initShifts(Schedule schedule) {
        List<ShiftAssignment> allShiftAssignments = new ArrayList<ShiftAssignment>();
        for (Shift shift : schedule.getShifts()) {
            ShiftAssignment assignment = new ShiftAssignment();
            assignment.setShift(shift);
            allShiftAssignments.add(assignment);
        }
        schedule.setShiftAssignments(allShiftAssignments);
    }
    
    private int addShift(ShiftDate shiftDate, int slotNumber, boolean backup, int index) {
        Shift shift = new Shift();
        shift.setShiftId(index);
        shift.setShiftDate(shiftDate);
        shift.setSlotNumber(slotNumber);
        shift.setBackup(backup);
        shiftDate.getShifts().add(shift);
        return (index + 1);
    }
    
    private void addEmployee(Schedule schedule, String[] employeeRow, 
            int rowNumber, Map<Integer,ShiftDate> columnDates,
            boolean weightColumnPresent) {
        if (isTotal(employeeRow[0])) {
            return;
        }
        
        Employee employee = new Employee();
        employee.setName(employeeRow[0]);
        try {
            AttorneyType type = AttorneyType.valueOf(StringUtils.trim(employeeRow[1]));
            employee.setAttorneyType(type);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("An unrecognized attorney type was found on row " + rowNumber + ": " 
                    + employeeRow[1] + ". Values must be one of: " + StringUtils.join(AttorneyType.values(), ", "));
        }
        
        try {
            employee.setNumberOfPrimaryShifts(Integer.parseInt(StringUtils.trim(employeeRow[2])));
        } catch (NumberFormatException e) {
            throw new RuntimeException("The number of primary shifts could not be parsed on row " 
                    + rowNumber + ": " + employeeRow[2]);
        }
        try {
            employee.setNumberOfBackupShifts(Integer.parseInt(StringUtils.trim(employeeRow[3])));
        } catch (NumberFormatException e) {
            throw new RuntimeException("The number of backup shifts could not be parsed on row " 
                    + rowNumber + ": " + employeeRow[3]);
        }
        if (weightColumnPresent) {
            try {
                employee.setWeight(Integer.parseInt(StringUtils.trim(employeeRow[4])));
            } catch (NumberFormatException e) {
                throw new RuntimeException("The employee weight could not be parsed on row " 
                        + rowNumber + ": " + employeeRow[4]);
            }
        }
        employee.setAvailableDates(new ArrayList<ShiftDate>());
        employee.setAvailableIfNeededDates(new ArrayList<ShiftDate>());
        int numberOfDates = schedule.getShiftDates().size();
        int startingColumn = getFirstDateColumnIndex(weightColumnPresent);
        // The right-most column might be used to tally the total number of primary and backup
        // shifts, causing there to be additional columns in each row beyond those that mark
        // availability for each shift date
        int maxColumn = startingColumn + numberOfDates - 1;
        for (int i = startingColumn; i < employeeRow.length && i <= maxColumn ; ++i) {
            String availability = StringUtils.trim(employeeRow[i]).toLowerCase();
            ShiftDate shiftDate = columnDates.get(i);
            if ("yes".equals(availability)) {
                employee.getAvailableDates().add(shiftDate);
            } else if ("ok".equals(availability)) {
                employee.getAvailableIfNeededDates().add(shiftDate);
            }
        }
        schedule.getEmployees().add(employee);
    }
    
    private void validateShiftTotals(Schedule schedule) {
        int numberOfPrimaryShifts = 0;
        int numberOfBackupShifts = 0;
        int numberOfEmployeesForPrimaryShifts = 0;
        int numberOfEmployeesForBackupShifts = 0;
        
        for (Shift shift : schedule.getShifts()) {
            if (shift.isBackup()) {
                ++ numberOfBackupShifts;
            } else {
                ++ numberOfPrimaryShifts;
            }
        }
        
        for (Employee employee : schedule.getEmployees()) {
            numberOfEmployeesForPrimaryShifts += employee.getNumberOfPrimaryShifts();
            numberOfEmployeesForBackupShifts += employee.getNumberOfBackupShifts();
            int totalNumberOfShifts = employee.getNumberOfBackupShifts() + employee.getNumberOfPrimaryShifts();
            int numberOfDatesAvailable = employee.getAvailableDates().size() + employee.getAvailableIfNeededDates().size();
            if (numberOfDatesAvailable < totalNumberOfShifts) {
                throw new RuntimeException(employee.getName() 
                        + " is assigned to work more shifts (" + totalNumberOfShifts
                        + ") than his or her number of available dates (" +numberOfDatesAvailable + ")");
            }
        }
        
        if (numberOfPrimaryShifts != numberOfEmployeesForPrimaryShifts) {
            throw new RuntimeException("The number of primary shift slots that need to be filled (" 
                    + numberOfPrimaryShifts 
                    + ") does not match the the total number of primary shifts to which employees should be assigned ("
                    + numberOfEmployeesForPrimaryShifts + ")");
        }
        if (numberOfBackupShifts != numberOfEmployeesForBackupShifts) {
            throw new RuntimeException("The number of backup shift slots that need to be filled (" 
                    + numberOfBackupShifts 
                    + ") does not match the the total number of backup shifts to which employees should be assigned ("
                    + numberOfEmployeesForBackupShifts + ")");
        }
    }
    
    private boolean isTotal(String value) {
        String valToCheck = StringUtils.stripToEmpty(value).toLowerCase();
        return (valToCheck.equals("total") 
                || valToCheck.startsWith("total:") 
                || valToCheck.startsWith("total "));
    }
    
    private boolean isWeightColumnPresent(String[] headerRow) {
        return headerRow.length > 4 
                && StringUtils.stripToEmpty(headerRow[4]).toLowerCase().startsWith("weight");
    }
    
    private int getFirstDateColumnIndex(boolean weightColumnPresent) {
        return weightColumnPresent ? 5 : 4;
    }
    
}
