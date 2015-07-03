package org.legalscheduler.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.legalscheduler.domain.Employee;
import org.legalscheduler.domain.Schedule;
import org.legalscheduler.domain.Shift;
import org.legalscheduler.domain.ShiftAssignment;
import org.optaplanner.core.impl.phase.custom.CustomPhaseCommand;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduleSolutionInitializer implements CustomPhaseCommand {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    public void changeWorkingSolution(ScoreDirector scoreDirector) {
        Schedule Schedule = (Schedule) scoreDirector.getWorkingSolution();
        initializeShiftAssignmentList(scoreDirector, Schedule);
    }

    public void initializeShiftAssignmentList(ScoreDirector scoreDirector, Schedule schedule) {
        // GOALS: 
        // - assign each employee no more times than their maximum number of available days
        // - don't assign employees to dates on which they are already working
        
        // This will break a lot of the other constraints, but we'll let the solver figure
        // those out by swapping employees around.
        
        
        List<ShiftAssignment> shiftAssignmentList = createShiftAssignmentList(schedule);
        List<Employee> employeesForPrimaryShifts = getEmployeesToAssign(schedule, false);
        List<Employee> employeesForBackupShifts = getEmployeesToAssign(schedule, true);
        
        Map<Employee, List<Integer>> employeeAssignmentDates = new HashMap<Employee, List<Integer>>();
        for (Employee employee : schedule.getEmployees()) {
            employeeAssignmentDates.put(employee, new ArrayList<Integer>());
        }
        
        initializeList(employeesForPrimaryShifts, shiftAssignmentList, false, employeeAssignmentDates, scoreDirector);
        initializeList(employeesForBackupShifts, shiftAssignmentList, true, employeeAssignmentDates, scoreDirector);
        
        schedule.setShiftAssignments(shiftAssignmentList);
    }
    
    private void initializeList(List<Employee> employeesForShift, 
            List<ShiftAssignment> shiftAssignmentList, 
            boolean isBackup, 
            Map<Employee, List<Integer>> employeeAssignmentDates, 
            ScoreDirector scoreDirector) {
        
        for (Employee employee : employeesForShift) {
            List<Integer> daysForEmployee = employeeAssignmentDates.get(employee);
            boolean added = false;
            // Try to add employees on days which they aren't working
            for (ShiftAssignment assignment : shiftAssignmentList) {
                if (assignment.getShift().isBackup() == isBackup 
                        && assignment.getEmployee() == null) {
                    int dayIndex = assignment.getShift().getShiftDate().getShiftNumber();
                    if (daysForEmployee.contains(dayIndex) == false) {
                        scoreDirector.beforeEntityAdded(assignment);
                        assignment.setEmployee(employee);
                        scoreDirector.afterEntityAdded(assignment);
                        daysForEmployee.add(dayIndex);
                        added = true;
                        break;
                    }
                }
            }
            // For the last few employees in the list, there might not be
            // enough slots left. Add them to whatever shift isn't assigned, and
            // let the scheduler sort it out.
            if (added == false) {
                for (ShiftAssignment assignment : shiftAssignmentList) {
                    if (assignment.getShift().isBackup() == isBackup 
                            && assignment.getEmployee() == null) {
                        int dayIndex = assignment.getShift().getShiftDate().getShiftNumber();
                        scoreDirector.beforeEntityAdded(assignment);
                        assignment.setEmployee(employee);
                        scoreDirector.afterEntityAdded(assignment);
                        daysForEmployee.add(dayIndex);
                        added = true;
                        break;
                    }
                }
            }
        }
    }

    private List<ShiftAssignment> createShiftAssignmentList(Schedule schedule) {
        List<ShiftAssignment> shiftAssignmentList = new ArrayList<ShiftAssignment>();
        for (Shift shift : schedule.getShifts()) {
            ShiftAssignment assignment = new ShiftAssignment(); 
            assignment.setShift(shift);
            shiftAssignmentList.add(assignment);
        }
        System.out.println("Shift assignment list has " + shiftAssignmentList.size() + " entries");
        return shiftAssignmentList;
    }
    
    private List<Employee> getEmployeesToAssign(Schedule schedule, boolean isBackup) {
        if (isBackup) {
            System.out.println("Creating backup staffing list");
        } else {
            System.out.println("Creating primary staffing list");
        }
        List<Employee> employeesToAssign = new ArrayList<Employee>();
        for (Employee employee : schedule.getEmployees()) {
            int numAssignments = isBackup ? employee.getNumberOfBackupShifts() 
                    : employee.getNumberOfPrimaryShifts();
            for (int i = 1; i <= numAssignments; ++i) {
                System.out.println("Adding " + employee.getName());
                employeesToAssign.add(employee);
            }
        }
        System.out.println("List size is " + employeesToAssign.size());
        return employeesToAssign;
    }
}
