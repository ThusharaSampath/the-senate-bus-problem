package com.msccse;

import java.util.Random;
import java.util.concurrent.Semaphore;

public class App {
    // Constants
    private static final int BUS_CAPACITY = 50;
    private static long busArrivalMean;
    private static long riderArrivalMean;

    // Shared variables
    private static int waitingRiders = 0;

    // Synchronization variables
    private static Semaphore busStopMutex = new Semaphore(1, true);
    private static Semaphore allowBoarding = new Semaphore(0, true);
    private static Semaphore allBoarded = new Semaphore(0, true);

    static class Bus extends Thread {
        public void run() {
            try {
                while (true) {
                    busStopMutex.acquire();

                    if (waitingRiders == 0) {
                        // No riders, bus departs immediately
                        depart(0);
                        busStopMutex.release();
                    } else {
                        int ridersToBoard = Math.min(waitingRiders, BUS_CAPACITY);
                        System.out.println("Bus arrived. Riders waiting: " + waitingRiders +
                                ", Boarding: " + ridersToBoard);

                        // Signal riders to board
                        allowBoarding.release(ridersToBoard);

                        // Wait for all riders to board
                        allBoarded.acquire();

                        waitingRiders -= ridersToBoard;
                        busStopMutex.release();
                        depart(ridersToBoard);
                    }
                    // Simulate time between rider arrivals
                    Thread.sleep(generateExponentialDelay(busArrivalMean));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Rider thread class
    static class Rider extends Thread {
        public void run() {
            try {
                System.out.println("Riders (" + Thread.currentThread().getId() + ")  waiting to enter bus stop");
                busStopMutex.acquire();
                waitingRiders++;
                busStopMutex.release();
                System.out.println("Riders (" + Thread.currentThread().getId() + ")  entered bus stop");
                allowBoarding.acquire();

                boardBus();
                if (allowBoarding.availablePermits() == 0) {
                    allBoarded.release();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // cleanup threads and release mutex
                busStopMutex.release();
                Thread.currentThread().interrupt();
            }
        }
    }

    // Helpers
    static long generateExponentialDelay(long mean) {
        Random random = new Random();
        return (long) (-Math.log(1 - random.nextDouble()) * mean);
    }

    private static void depart(int ridersCount) {
        if (ridersCount == 0) {
            System.out.println("No Riders, Bus is departing immediately.");
        } else {
            System.out.println(
                    "Bus is departing with " + ridersCount + " riders.");
        }
    }

    static void boardBus() {
        System.out.println("Rider (" + Thread.currentThread().getId() + ") boarding.");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java App <busArrivalMean> <riderArrivalMean>");
            System.exit(1);
        }

        try {
            busArrivalMean = Long.parseLong(args[0]);
            riderArrivalMean = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Both arguments must be valid integers representing milliseconds.");
            System.exit(1);
        }

        Bus busThread = new Bus();
        busThread.start();

        // Continuously create rider threads
        while (true) {
            Rider riderThread = new Rider();
            riderThread.start();

            try {
                // Simulate time between rider arrivals
                Thread.sleep(generateExponentialDelay(riderArrivalMean));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
