package org.legalscheduler.domain;

import java.io.Serializable;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@PlanningEntity
@XStreamAlias("ShiftAssignment")
public class ShiftAssignment implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Shift shift;
    private Employee employee;
    public Shift getShift() {
        return shift;
    }
    public void setShift(Shift shift) {
        this.shift = shift;
    }
    @PlanningVariable(valueRangeProviderRefs = {"employees"})
    public Employee getEmployee() {
        return employee;
    }
    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
    
    public String toString() {
        String assignee = employee != null ? employee.getName() : " Unassigned";
        return shift.toString() + " " + assignee;
    }
}
