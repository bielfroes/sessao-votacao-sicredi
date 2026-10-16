package com.froes.assembly.util.database;

import java.sql.SQLException;

public final class DatabaseConstraintUtils {

    public static final String UNIQUE_VIOLATION = "23505";

    private DatabaseConstraintUtils() {
    }

    public static boolean isUniqueViolation(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException
                    && UNIQUE_VIOLATION.equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
