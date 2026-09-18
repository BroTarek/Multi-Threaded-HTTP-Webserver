package com.webserver;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadPool {
    private final BlockingQueue<Runnable> taskQueue;
    private final Thread[] workerThreads;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    public ThreadPool(int threadCount, int queueCapacity) {
        this.taskQueue = new ArrayBlockingQueue<>(queueCapacity);
        this.workerThreads = new Thread[threadCount];

        System.out.println("[ThreadPool] Initializing with " + threadCount + " worker threads and queue capacity of " + queueCapacity);

        for (int i = 0; i < threadCount; i++) {
            workerThreads[i] = new WorkerThread("Worker-" + (i + 1));
            workerThreads[i].start();
        }
    }

    /**
     * Producer method: Enqueues a Runnable task into the bounded queue.
     * Blocks if the queue is at full capacity (backpressure management).
     */
    public void submit(Runnable task) throws InterruptedException {
        if (!isRunning.get()) {
            throw new IllegalStateException("ThreadPool has been shut down");
        }
        taskQueue.put(task);
    }

    public void shutdown() {
        isRunning.set(false);
        for (Thread worker : workerThreads) {
            worker.interrupt();
        }
    }

    private class WorkerThread extends Thread {
        public WorkerThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    // Consumer method: Dequeues Runnable task and executes run()
                    Runnable task = taskQueue.take();
                    task.run();
                } catch (InterruptedException e) {
                    // Thread interrupted during shutdown or queue wait
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("[" + getName() + "] Unhandled task exception: " + e.getMessage());
                }
            }
            System.out.println("[" + getName() + "] Worker thread terminated.");
        }
    }
}
