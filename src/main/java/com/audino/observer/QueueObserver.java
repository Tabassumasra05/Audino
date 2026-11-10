package com.audino.observer;

import com.audino.model.Token;

import java.util.List;

/**
 * Observer interface for components subscribing to outpatient queue state changes.
 *
 * // OOM: Observer Pattern – allows decentralized UI panels to react to queue changes.
 */
public interface QueueObserver {

    /**
     * Triggered when the outpatient queue state is updated.
     *
     * @param tokens Current snapshot of today's tokens
     */
    void onQueueUpdated(List<Token> tokens);
}
