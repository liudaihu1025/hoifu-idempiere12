package org.idempiere.optaplanner.util;  
  
public class DependencyValidator {  
    public static boolean validateOptaPlannerDependencies() {  
        try {  
            Class.forName("org.optaplanner.core.api.solver.SolverManager");  
            Class.forName("org.optaplanner.core.api.score.stream.ConstraintProvider");  
            return true;  
        } catch (ClassNotFoundException e) {  
            return false;  
        }  
    }  
}