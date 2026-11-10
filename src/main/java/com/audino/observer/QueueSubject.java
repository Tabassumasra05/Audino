package com.audino.observer;

/**
 * Subject interface in the Observer pattern managing queue event subscriptions.
 *
 * // OOM: Observer Pattern – subject contract for registering, removing, and broadcasting to observers.
 */
public interface QueueSubject {

    void registerObserver(QueueObserver observer);

    void removeObserver(QueueObserver observer);

    void notifyObservers();
}
