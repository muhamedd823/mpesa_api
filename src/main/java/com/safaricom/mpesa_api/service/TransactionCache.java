package com.safaricom.mpesa_api.service;

import com.safaricom.mpesa_api.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Component
public class TransactionCache {

    private static final int MAX_SIZE = 500;

    private final LinkedList<Transaction> cache = new LinkedList<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void add(Transaction transaction) {
        lock.writeLock().lock();
        try {
            cache.addFirst(transaction);
            if (cache.size() > MAX_SIZE) {
                cache.removeLast();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addAll(List<Transaction> transactions) {
        lock.writeLock().lock();
        try {
            for (Transaction t : transactions) {
                cache.addFirst(t);
            }
            while (cache.size() > MAX_SIZE) {
                cache.removeLast();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Transaction> getRecent(int limit) {
        lock.readLock().lock();
        try {
            return cache.stream().limit(limit).collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Transaction> getAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(cache);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
