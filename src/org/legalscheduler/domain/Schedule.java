package org.legalscheduler.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.Solution;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.impl.score.buildin.hardsoft.HardSoftScoreDefinition;
import org.optaplanner.persistence.xstream.impl.score.XStreamScoreConverter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;

@PlanningSolution
@XStreamAlias("Schedule")
public class Schedule implements Solution<HardSoftScore> {
    // facts
    private List<Employee> employees;
    private List<Shift> shifts;
    private List<ShiftDate> shiftDates;
    
    @XStreamConverter(value = XStreamScoreConverter.class, types = {HardSoftScoreDefinition.class})
    private HardSoftScore score;
    
    @ValueRangeProvider(id = "employees")
    public List<Employee> getEmployees() {
        return employees;
    }
    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
    }
    public List<Shift> getShifts() {
        return shifts;
    }
    public void setShifts(List<Shift> shifts) {
        this.shifts = shifts;
    }
    public List<ShiftDate> getShiftDates() {
        return shiftDates;
    }
    public void setShiftDates(List<ShiftDate> shiftDates) {
        this.shiftDates = shiftDates;
    }


    // planning entities
    private List<ShiftAssignment> shiftAssignments;

    @PlanningEntityCollectionProperty
    public List<ShiftAssignment> getShiftAssignments() {
        return shiftAssignments;
    }
    public void setShiftAssignments(List<ShiftAssignment> shiftAssignments) {
        this.shiftAssignments = shiftAssignments;
    }
    
    @Override
    public Collection<? extends Object> getProblemFacts() {
        List<Object> facts = new ArrayList<Object>();
        facts.addAll(employees);
        facts.addAll(shifts);
        facts.addAll(shiftDates);
        return facts;
    }
    @Override
    public HardSoftScore getScore() {
        return score;
    }
    @Override
    public void setScore(HardSoftScore score) {
        this.score = score;
    }
    
}
