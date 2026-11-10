package com.audino;

import com.audino.dao.MedicationDAO;
import com.audino.model.CapsuleMedication;
import com.audino.model.Medication;
import com.audino.model.TabletMedication;
import com.audino.service.MedicationSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Test: Medication Search & Typo Autocorrect")
class MedicationSearchTest {

    private MedicationSearchService searchService;

    @BeforeEach
    void setUp() {
        MockMedicationDAO mockDAO = new MockMedicationDAO();
        mockDAO.add(new TabletMedication(1L, "Paracetamol", "Dolo 650", "3222", "650mg", List.of("Paracetamol"), new BigDecimal("30.00")));
        mockDAO.add(new CapsuleMedication(2L, "Amoxicillin", "Mox 500", "723", "500mg", List.of("Amoxicillin"), new BigDecimal("75.00")));
        mockDAO.add(new TabletMedication(3L, "Ibuprofen", "Brufen 400", "5640", "400mg", List.of("Ibuprofen"), new BigDecimal("25.00")));
        mockDAO.add(new TabletMedication(4L, "Warfarin", "Warf 5", "11289", "5mg", List.of("Warfarin"), new BigDecimal("85.00")));
        mockDAO.add(new TabletMedication(5L, "Atorvastatin", "Atorva 10", "83367", "10mg", List.of("Atorvastatin"), new BigDecimal("68.00")));

        searchService = new MedicationSearchService(mockDAO);
    }

    @Test
    @DisplayName("Search by brand name prefix returns matching medication")
    void testSearchByBrandName() {
        var resp = searchService.search("Dolo", 5);
        assertThat(resp.results()).isNotEmpty();
        assertThat(resp.results().get(0).getBrandName()).isEqualTo("Dolo 650");
    }

    @Test
    @DisplayName("Search by generic name prefix returns matching medication")
    void testSearchByGenericName() {
        var resp = searchService.search("Amox", 5);
        assertThat(resp.results()).isNotEmpty();
        assertThat(resp.results().get(0).getGenericName()).isEqualTo("Amoxicillin");
    }

    @Test
    @DisplayName("Search by RxNorm code returns exact match")
    void testSearchByRxNormCode() {
        var resp = searchService.search("11289", 5);
        assertThat(resp.results()).isNotEmpty();
        assertThat(resp.results().get(0).getGenericName()).isEqualTo("Warfarin");
    }

    @Test
    @DisplayName("Typo autocorrect: 'amoxcilin' correctly suggests 'Amoxicillin'")
    void testAutocorrectAmoxicillin() {
        var resp = searchService.search("amoxcilin", 5);

        // Exact results empty due to misspelling
        assertThat(resp.results()).isEmpty();

        // Autocorrect pipeline provides suggestion
        assertThat(resp.didYouMean()).isPresent();
        assertThat(resp.didYouMean().get()).isEqualTo("Amoxicillin");
    }

    @Test
    @DisplayName("Typo autocorrect: 'paracetmol' correctly suggests 'Paracetamol'")
    void testAutocorrectParacetamol() {
        var resp = searchService.search("paracetmol", 5);

        assertThat(resp.didYouMean()).isPresent();
        assertThat(resp.didYouMean().get()).isEqualTo("Paracetamol");
    }

    @Test
    @DisplayName("Typo autocorrect: 'brufn' correctly suggests 'Brufen 400'")
    void testAutocorrectBrufen() {
        var resp = searchService.search("brufn", 5);

        assertThat(resp.didYouMean()).isPresent();
        assertThat(resp.didYouMean().get()).isEqualTo("Brufen 400");
    }

    private static class MockMedicationDAO implements MedicationDAO {
        private final List<Medication> list = new ArrayList<>();
        void add(Medication m) { list.add(m); }
        @Override public Optional<Medication> findById(Long id) { return list.stream().filter(m -> m.getId().equals(id)).findFirst(); }
        @Override public List<Medication> findAll() { return new ArrayList<>(list); }
        @Override public List<Medication> search(String q) { return list; }
        @Override public Medication save(Medication m) { list.add(m); return m; }
        @Override public void update(Medication m) {}
        @Override public void updatePrice(Long id, BigDecimal p) {}
        @Override public void delete(Long id) {}
        @Override public List<String> findIngredientsByMedicationId(Long id) { return List.of(); }
    }
}
