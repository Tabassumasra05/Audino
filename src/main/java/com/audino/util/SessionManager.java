package com.audino.util;

import com.audino.model.Account;
import com.audino.model.Doctor;

/**
 * Thread-safe Singleton managing the currently authenticated staff session.
 *
 * // OOM: Singleton Pattern – guarantees a single global session state for the running client instance.
 */
public class SessionManager {

    private static volatile SessionManager instance;

    private Account currentAccount;

    private SessionManager() {
        // Private constructor for singleton
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    public synchronized void setCurrentAccount(Account account) {
        this.currentAccount = account;
    }

    public synchronized Account getCurrentAccount() {
        return currentAccount;
    }

    public synchronized boolean isLoggedIn() {
        return currentAccount != null;
    }

    public synchronized Doctor getCurrentDoctor() {
        if (currentAccount instanceof Doctor doctor) {
            return doctor;
        }
        return null;
    }

    public synchronized void logout() {
        this.currentAccount = null;
    }

    public synchronized void clearSession() {
        logout();
    }
}
