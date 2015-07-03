package org.legalscheduler.solver;

import org.legalscheduler.domain.ShiftAssignment;
import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import org.optaplanner.core.impl.heuristic.selector.move.generic.SwapMove;
import org.optaplanner.core.impl.score.director.ScoreDirector;

public class DayAndShiftTypeSwapMoveFilter implements SelectionFilter<SwapMove> {
    public boolean accept(ScoreDirector scoreDirector, SwapMove move) {
        ShiftAssignment leftShiftAssignment = (ShiftAssignment) move.getLeftEntity();
        ShiftAssignment rightShiftAssignment = (ShiftAssignment) move.getRightEntity();
        boolean isSameDay = leftShiftAssignment.getShift().getShiftDate().getShiftNumber() 
                == rightShiftAssignment.getShift().getShiftDate().getShiftNumber();
        boolean isSameShiftType = 
                (leftShiftAssignment.getShift().isBackup() == rightShiftAssignment.getShift().isBackup());
        return (isSameDay == false) && isSameShiftType;
    }

}
