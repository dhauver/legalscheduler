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
            Map<Integer, ShiftDate> columnDates = initShiftDates(schedule, headerRow);
            initShifts(schedule);
            schedule.setEmployees(new ArrayList<Employee>());
            int rowNumber = 2;
            String[] employeeRow = csvReader.readNext();
            while (employeeRow != null) {
                addEmployee(schedule, employeeRow, rowNumber, columnDates);
                employeeRow = csvReader.readNext();
            }
            csvReader.close();
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while trying to read the contents of the file", e);
        }
        validateShiftTotals(schedule);
        return schedule;
    }
    
    private Map<Integer, ShiftDate> initShiftDates(Schedule schedule, String[] headerRow) {
        Map<Integer, ShiftDate> columnDates = new HashMap<Integer, ShiftDate>();
        List<ShiftDate> shiftDates = new ArrayList<ShiftDate>();
        int shiftNumber = 0;
        int weekNumber = -1;
        int previousWeek = -1;
        int previousYear = -1;
        for (int i = 4; i < headerRow.length; ++i) {
            String value = headerRow[i];
            try {
                Date date = SchedulerApplication.INPUT_DATE_FORMAT.parse(value);
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
                columnDates.put(i, shiftDate);
                shiftDates.add(shiftDate);
            } catch (ParseException e) {
                throw new RuntimeException("The header value in column " + (i + 1) + " is not a valid date: " + value + ". Values must be formatted to look like: " + SchedulerApplication.INPUT_DATE_FORMAT.format(new Date()));
            }
        }
        schedule.setShiftDates(shiftDates);
        return columnDates;
    }
    
    private void initShifts(Schedule schedule) {
        int shiftIndex = 0;
        List<Shift> allShifts = new ArrayList<Shift>();
        for (ShiftDate shiftDate : schedule.getShiftDates()) {
            shiftDate.setShifts(new ArrayList<Shift>());
            shiftIndex = addShift(shiftDate, 1, false, shiftIndex);
            shiftIndex = addShift(shiftDate, 2, false, shiftIndex);
            shiftIndex = addShift(shiftDate, 3, true, shiftIndex);
            allShifts.addAll(shiftDate.getShifts());
        }
        schedule.setShifts(allShifts);
        
        List<ShiftAssignment> allShiftAssignments = new ArrayList<ShiftAssignment>();
        for (Shift shift : allShifts) {
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
    
    private void addEmployee(Schedule schedule, String[] employeeRow, int rowNumber, Map<Integer,ShiftDate> columnDates) {
        if (employeeRow[0].toLowerCase().equals("total") || employeeRow[0].toLowerCase().equals("total:")) {
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
        employee.setAvailableDates(new ArrayList<ShiftDate>());
        employee.setAvailableIfNeededDates(new ArrayList<ShiftDate>());
        for (int i = 4; i < employeeRow.length; ++i) {
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
    
}
