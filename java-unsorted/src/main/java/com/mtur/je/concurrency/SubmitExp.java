package com.mtur.je.concurrency;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

@Slf4j
public class SubmitExp {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        SubmitExp invokeAllExp = new SubmitExp();
        invokeAllExp.startApp();
        log.info("Finished.");
    }

    public void startApp() throws InterruptedException, ExecutionException {
        log.info("Starting");

        int tasksCount = 100;
        int poolSize = 3;
//        ExecutorService pool = new ThreadPoolExecutor(0, poolSize,
//                30L, TimeUnit.SECONDS, new SynchronousQueue<>());
        ThreadPoolExecutor pool = new ThreadPoolExecutor(poolSize, poolSize,
                5L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        pool.allowCoreThreadTimeOut(true);

        log.info("Sleeping ");
        Thread.sleep(10_000);

        List<Callable<String>> callables = new ArrayList<>(tasksCount);
        for (int i = 0; i <  tasksCount; i++) {
            callables.add(new MyTask(i));
        }

        try {
            log.info("Submitting all tasks");
            List<Future<String>> futures = new ArrayList<>(callables.size());
            for (Callable<String> callable : callables) {
                futures.add(pool.submit(callable));
            }


            log.info("All tasks are submitted. Waiting ..");

            List<String> results = new ArrayList<>(callables.size());
            for (Future<String> future : futures) {
                String res = future.get();
                results.add(res);
            }

            log.info("------------------------------------------------");
            log.info("Results: \n{}", results);
        } finally {
//            pool.shutdown();
        }

        log.info("Sleeping ");
        Thread.sleep(100_000);
    }

    @AllArgsConstructor
    static class MyTask implements Callable<String> {
        private int id;
        @Override
        public String call() throws Exception {
            log.info("[{}] Start task", id);
            Thread.sleep(new Random().nextInt(10) * 100);
            log.info("[{}] Done task", id);
            if (id == 33) {
                throw new RuntimeException("My ex");
            }
            return "Result-" + id;
        }
    }


}