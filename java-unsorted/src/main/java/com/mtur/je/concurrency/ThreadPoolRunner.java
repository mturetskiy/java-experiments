package com.mtur.je.concurrency;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class ThreadPoolRunner {

    private LoadingCache<Integer, String> cache = CacheBuilder.newBuilder().build(new CacheLoader<Integer, String>() {
        @Override
        public String load(Integer key) throws Exception {
            return loadCacheValue(key);
        }
    });

    public void startApp() throws ExecutionException, InterruptedException {
        log.info("Running thread experimental app");

        List<Integer> elements = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            elements.add(1);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<?> future = executorService.submit(() -> {
            log.info("Running thread pool task for elements");
            elements.parallelStream().forEach(element -> {
                work(element);
            });
            log.info("Done thread pool task for elements");
        });

        future.get();

        executorService.shutdown();

        log.info("Done with thread pool task.");
    }

    private void work(int element) {
        log.info("Processing element: {}", element);

        String value = cache.getUnchecked(element);


        log.info("Done element: {}. Value: {}", element, value);
    }

    private String loadCacheValue(int key) throws ExecutionException, InterruptedException {
        log.info("Loading cache value for key: {}", key);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<Long> task1 = executorService.submit(() -> dummyWork(30, key));
        Future<Long> task2 = executorService.submit(() -> dummyWork(40, key));

        Long res1 = task1.get();
        Long res2 = task2.get();

        executorService.shutdown();

        String result = res1 + ":" + res2;

        log.info("Done loading cache value for key: {}. Calculated value: {}", key, result);
        return result;
    }

    private long dummyWork(int k, int element) {
        long start = System.nanoTime();
        long n = 100000000L * k;
        long curr = 1;
        long currSum = 1;
        long prevSum = 0;

        while (curr < n) {
            long next = prevSum + currSum;
            prevSum = currSum;
            currSum = next;
            curr++;
        }

         log.info("[{}/{}] Calculated result for: {} is: {}. Time: {} ms", element, k, n, currSum, (float)((System.nanoTime() - start)/ 1000_000.0) );
        return currSum;
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        long start = System.nanoTime();
        new ThreadPoolRunner().startApp();
        log.info("Done with main thread in {} ms", (System.nanoTime() - start) / 1000_000);
    }
}