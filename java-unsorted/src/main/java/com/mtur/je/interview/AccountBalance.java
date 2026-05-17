package com.mtur.je.interview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountBalance {
    public static class Account {
        private long balance;

        public synchronized void applyTransaction(long txnAmount) {
            this.balance += txnAmount;
        }

        public long getBalance() {
            return this.balance;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Account account = new Account();

        List<Long> transactions = Collections.synchronizedList(loadTransactions());

        Thread client1 = new Thread(() -> {
            while (true) {
                Long amount = extractFirstAmount(transactions);
                if (amount != null) {
                    account.applyTransaction(amount);
                } else {
                    break;
                }
            }
//            while(!transactions.isEmpty()) {
//                Long amount = transactions.remove(0);
//                account.applyTransaction(amount);
//            }
        });

        Thread client2 = new Thread(() -> {
            while (true) {
                Long amount = extractFirstAmount(transactions);
                if (amount != null) {
                    account.applyTransaction(amount);
                } else {
                    break;
                }
            }
//            while(!transactions.isEmpty()) {
//                Long amount = transactions.remove(0);
//                account.applyTransaction(amount);
//            }
        });

        client1.start();
        client2.start();

        client1.join();
        client2.join();

        System.out.println("Final balance is: " + account.getBalance());

    }

    private static Long extractFirstAmount(List<Long> transactions) {
        synchronized (transactions) {
            if (!transactions.isEmpty()) {
                return transactions.remove(0);
            }
        }

        return null;
    }

    private static List<Long> loadTransactions() {
        List<Long> transactions = new ArrayList<>();
        transactions.add(100L);
        for (long amount = 1; amount <= 1000; amount++) {
            transactions.add(amount);
            transactions.add(-amount);
        }
        return transactions;
    }


}
