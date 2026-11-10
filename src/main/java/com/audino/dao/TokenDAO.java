package com.audino.dao;

import com.audino.model.Token;
import com.audino.model.TokenStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for outpatient queue tokens.
 */
public interface TokenDAO {

    Optional<Token> findById(Long id);

    Optional<Token> findByTokenNoAndDate(String tokenNo, LocalDate date);

    List<Token> findTodayTokens();

    List<Token> findTodayTokensByDoctor(String doctorUsername);

    Optional<Token> findNextWaitingToken(String doctorUsername, LocalDate date);

    Token save(Token token);

    void updateStatus(Long tokenId, TokenStatus newStatus);

    void assignDoctor(Long tokenId, String doctorUsername);

    String getNextTokenNo(LocalDate date);

    int countTodayTokensByStatus(TokenStatus status);
}
