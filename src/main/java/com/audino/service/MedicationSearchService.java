package com.audino.service;

import com.audino.dao.MedicationDAO;
import com.audino.model.Medication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * High-performance medication search and typo-autocorrect service.
 * Employs multi-token prefix/substring matching, Levenshtein edit distance,
 * and character-trigram cosine similarity.
 */
public class MedicationSearchService {

    private static final Logger logger = LoggerFactory.getLogger(MedicationSearchService.class);

    private final MedicationDAO medicationDAO;
    private final List<Medication> index = new ArrayList<>();

    public MedicationSearchService(MedicationDAO medicationDAO) {
        this.medicationDAO = Objects.requireNonNull(medicationDAO, "MedicationDAO must not be null");
        reloadIndex();
    }

    /**
     * Refreshes the in-memory search index from the database.
     */
    public synchronized void reloadIndex() {
        index.clear();
        index.addAll(medicationDAO.findAll());
        logger.info("MedicationSearchService indexed {} formulary entries.", index.size());
    }

    /**
     * Search result holding matching medications and an optional typo autocorrect suggestion.
     */
    public record SearchResponse(List<Medication> results, Optional<String> didYouMean) {}

    /**
     * Searches formulary medications by query string with live prefix/substring matching.
     * If no exact/prefix matches are found, triggers the typo autocorrect pipeline.
     *
     * @param query Search string
     * @param limit Maximum results to return
     * @return SearchResponse with results and optional Did You Mean recommendation
     */
    public SearchResponse search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return new SearchResponse(Collections.emptyList(), Optional.empty());
        }

        String cleanQuery = query.trim().toLowerCase();
        List<ScoredMedication> scored = new ArrayList<>();

        for (Medication med : index) {
            int score = computeMatchScore(med, cleanQuery);
            if (score > 0) {
                scored.add(new ScoredMedication(med, score));
            }
        }

        scored.sort(Comparator.comparingInt(ScoredMedication::score).reversed());

        int max = limit > 0 ? Math.min(limit, scored.size()) : scored.size();
        List<Medication> results = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            results.add(scored.get(i).medication());
        }

        // If no results or very low quality, run typo autocorrect
        Optional<String> didYouMean = Optional.empty();
        if (results.isEmpty() && cleanQuery.length() >= 3) {
            didYouMean = findAutocorrectSuggestion(cleanQuery);
        }

        return new SearchResponse(results, didYouMean);
    }

    private int computeMatchScore(Medication med, String q) {
        String brand = med.getBrandName().toLowerCase();
        String generic = med.getGenericName().toLowerCase();
        String rxnorm = med.getRxNormCode() != null ? med.getRxNormCode().toLowerCase() : "";

        // Exact match
        if (brand.equals(q) || generic.equals(q) || rxnorm.equals(q)) {
            return 100;
        }
        // Prefix match
        if (brand.startsWith(q) || generic.startsWith(q)) {
            return 80;
        }
        // Substring match
        if (brand.contains(q) || generic.contains(q)) {
            return 50;
        }
        // Ingredients match
        for (String ing : med.getIngredients()) {
            if (ing.toLowerCase().startsWith(q)) {
                return 40;
            }
            if (ing.toLowerCase().contains(q)) {
                return 30;
            }
        }
        return 0;
    }

    /**
     * Computes the closest formulary medication name using combined Levenshtein distance
     * and character-trigram cosine similarity.
     *
     * @param input Misspelled query string
     * @return Best matching medication name if threshold exceeded
     */
    public Optional<String> findAutocorrectSuggestion(String input) {
        if (input == null || input.length() < 3) return Optional.empty();

        String bestMatch = null;
        double highestSimilarity = 0.0;

        for (Medication med : index) {
            // Test generic name and tokens
            String genLower = med.getGenericName().toLowerCase();
            double simGeneric = computeCombinedSimilarity(input, genLower);
            for (String part : genLower.split("\\s+")) {
                if (part.length() >= 3) {
                    simGeneric = Math.max(simGeneric, computeCombinedSimilarity(input, part));
                }
            }
            if (simGeneric > highestSimilarity) {
                highestSimilarity = simGeneric;
                bestMatch = med.getGenericName();
            }

            // Test brand name and tokens (e.g. "brufen" in "Brufen 400")
            String brandLower = med.getBrandName().toLowerCase();
            double simBrand = computeCombinedSimilarity(input, brandLower);
            for (String part : brandLower.split("\\s+")) {
                if (part.length() >= 3) {
                    simBrand = Math.max(simBrand, computeCombinedSimilarity(input, part));
                }
            }
            if (simBrand > highestSimilarity) {
                highestSimilarity = simBrand;
                bestMatch = med.getBrandName();
            }
        }

        // Threshold of 0.55 similarity
        if (highestSimilarity >= 0.55 && bestMatch != null && !bestMatch.equalsIgnoreCase(input)) {
            return Optional.of(bestMatch);
        }

        return Optional.empty();
    }

    /**
     * Combines Levenshtein edit distance and Trigram cosine similarity.
     */
    private double computeCombinedSimilarity(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;

        // 1. Normalized Levenshtein similarity (1 - dist / maxLen)
        int dist = computeLevenshteinDistance(s1, s2);
        double levSim = 1.0 - ((double) dist / maxLen);

        // 2. Character-trigram cosine similarity
        double triSim = computeTrigramSimilarity(s1, s2);

        // Weighted hybrid score
        return (levSim * 0.45) + (triSim * 0.55);
    }

    /**
     * Standard dynamic programming Levenshtein distance calculation.
     */
    public static int computeLevenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    /**
     * Character-trigram cosine similarity.
     */
    public static double computeTrigramSimilarity(String s1, String s2) {
        if (s1.length() < 3 || s2.length() < 3) {
            // Fallback for short words: bigram
            return s1.contains(s2) || s2.contains(s1) ? 0.6 : 0.0;
        }

        Set<String> tri1 = extractTrigrams(s1);
        Set<String> tri2 = extractTrigrams(s2);

        int intersection = 0;
        for (String t : tri1) {
            if (tri2.contains(t)) {
                intersection++;
            }
        }

        double denom = Math.sqrt(tri1.size()) * Math.sqrt(tri2.size());
        return denom > 0 ? (intersection / denom) : 0.0;
    }

    private static Set<String> extractTrigrams(String str) {
        Set<String> set = new HashSet<>();
        for (int i = 0; i <= str.length() - 3; i++) {
            set.add(str.substring(i, i + 3));
        }
        return set;
    }

    private record ScoredMedication(Medication medication, int score) {}
}
