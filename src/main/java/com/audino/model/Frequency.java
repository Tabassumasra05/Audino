package com.audino.model;

import java.util.List;

/**
 * Medication administration frequency and schedule metadata.
 * Encapsulates times-per-day multiplier and daily chronological phase mappings.
 */
public enum Frequency {
    ONCE_DAILY("Once daily (1-0-0)", 1, List.of(DosePhase.MORNING)),
    TWICE_DAILY("Twice daily (1-0-1)", 2, List.of(DosePhase.MORNING, DosePhase.NIGHT)),
    THRICE_DAILY("Thrice daily (1-1-1)", 3, List.of(DosePhase.MORNING, DosePhase.AFTERNOON, DosePhase.NIGHT)),
    FOUR_TIMES_DAILY("Four times daily (1-1-1-1)", 4, List.of(DosePhase.MORNING, DosePhase.AFTERNOON, DosePhase.EVENING, DosePhase.NIGHT)),
    BEDTIME("At bedtime (0-0-1)", 1, List.of(DosePhase.NIGHT)),
    SOS("SOS / As needed", 1, List.of(DosePhase.AS_NEEDED));

    private final String label;
    private final int timesPerDay;
    private final List<DosePhase> scheduledPhases;

    Frequency(String label, int timesPerDay, List<DosePhase> scheduledPhases) {
        this.label = label;
        this.timesPerDay = timesPerDay;
        this.scheduledPhases = scheduledPhases;
    }

    public String getLabel() {
        return label;
    }

    public int getTimesPerDay() {
        return timesPerDay;
    }

    public List<DosePhase> getScheduledPhases() {
        return scheduledPhases;
    }

    /**
     * Chronological phases of the day for the pill schedule grid.
     */
    public enum DosePhase {
        MORNING("Morning", "08:00 AM", "fa-sun"),
        AFTERNOON("Afternoon", "01:00 PM", "fa-cloud-sun"),
        EVENING("Evening", "06:00 PM", "fa-coffee"),
        NIGHT("Night", "09:00 PM", "fa-moon"),
        AS_NEEDED("As Needed", "SOS", "fa-bolt");

        private final String title;
        private final String defaultTime;
        private final String iconLiteral;

        DosePhase(String title, String defaultTime, String iconLiteral) {
            this.title = title;
            this.defaultTime = defaultTime;
            this.iconLiteral = iconLiteral;
        }

        public String getTitle() {
            return title;
        }

        public String getDefaultTime() {
            return defaultTime;
        }

        public String getIconLiteral() {
            return iconLiteral;
        }
    }
}
