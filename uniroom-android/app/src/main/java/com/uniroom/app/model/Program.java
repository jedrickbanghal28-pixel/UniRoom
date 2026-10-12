package com.uniroom.app.model;

/**
 * The two programs (courses) the app supports. "code" is the department code
 * in the database. The sections of each program come from the database.
 */
public enum Program {
    BSIT("BSIT", "BS-IT", "BSIT Faculty"),
    BSMT("BSMT", "BS-MedTech", "BSMEDTECH Faculty");

    /** Department code in the database, e.g. "BSIT". */
    public final String code;
    /** Short label shown to students, e.g. "BS-IT". */
    public final String label;
    /** Label of the faculty portal for this program, e.g. "BSIT Faculty". */
    public final String portalLabel;

    Program(String code, String label, String portalLabel) {
        this.code = code;
        this.label = label;
        this.portalLabel = portalLabel;
    }

    /** "BSIT" → BSIT, "BSMT" / "BSMEDTECH" → BSMT (the section name is used if the code is missing). */
    public static Program of(String code, String section) {
        String c = ((code == null ? "" : code) + " " + (section == null ? "" : section)).toUpperCase();
        if (c.contains("BSMT") || c.contains("MEDTECH") || c.contains("MED")) return BSMT;
        return BSIT;
    }
}
