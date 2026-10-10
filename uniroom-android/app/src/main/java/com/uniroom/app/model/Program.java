package com.uniroom.app.model;

/** The two programs (courses) the app supports. */
public enum Program {
    BSIT("BS-IT", "BSIT Faculty", new String[]{"BSIT-A1"}),
    BSMT("BS-MedTech", "BSMEDTECH Faculty", new String[]{"BSMT-A1", "BSMT-A2"});

    /** Short label shown to students, e.g. "BS-IT". */
    public final String label;
    /** Label of the faculty portal for this program, e.g. "BSIT Faculty". */
    public final String portalLabel;
    /** Sections that belong to this program. */
    public final String[] sections;

    Program(String label, String portalLabel, String[] sections) {
        this.label = label;
        this.portalLabel = portalLabel;
        this.sections = sections;
    }
}
