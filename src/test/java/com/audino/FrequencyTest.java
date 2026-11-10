package com.audino;

import com.audino.model.Frequency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Test: Frequency Enum Invariants")
class FrequencyTest {

    @Test
    @DisplayName("Frequency enum correctly defines daily multipliers and scheduled phases")
    void testFrequencyProperties() {
        assertThat(Frequency.ONCE_DAILY.getTimesPerDay()).isEqualTo(1);
        assertThat(Frequency.ONCE_DAILY.getScheduledPhases()).contains(Frequency.DosePhase.MORNING);

        assertThat(Frequency.TWICE_DAILY.getTimesPerDay()).isEqualTo(2);
        assertThat(Frequency.TWICE_DAILY.getScheduledPhases()).contains(Frequency.DosePhase.MORNING, Frequency.DosePhase.NIGHT);

        assertThat(Frequency.THRICE_DAILY.getTimesPerDay()).isEqualTo(3);
        assertThat(Frequency.THRICE_DAILY.getScheduledPhases()).contains(Frequency.DosePhase.MORNING, Frequency.DosePhase.AFTERNOON, Frequency.DosePhase.NIGHT);

        assertThat(Frequency.FOUR_TIMES_DAILY.getTimesPerDay()).isEqualTo(4);
        assertThat(Frequency.FOUR_TIMES_DAILY.getScheduledPhases()).hasSize(4);

        assertThat(Frequency.BEDTIME.getTimesPerDay()).isEqualTo(1);
        assertThat(Frequency.BEDTIME.getScheduledPhases()).contains(Frequency.DosePhase.NIGHT);

        assertThat(Frequency.SOS.getTimesPerDay()).isEqualTo(1);
        assertThat(Frequency.SOS.getScheduledPhases()).contains(Frequency.DosePhase.AS_NEEDED);
    }
}
