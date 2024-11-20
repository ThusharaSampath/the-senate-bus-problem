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
    private static int boardedRiders = 0;
    private static boolean boarding = false;

    // Synchronization variables
    private static Semaphore mutex = new Semaphore(1);
    private static Semaphore allowBoarding = new Semaphore(0);
    private static Semaphore allBoarded = new Semaphore(0);

    // Bus thread class
    static class Bus extends Thread {
        public void run() {
            try {
                while (true) {
                    System.out.println("Bus (" + Thread.currentThread().getId() + ") arriving");
                    mutex.acquire();

                    if (waitingRiders == 0) {
                        // No riders, bus departs immediately
                        depart(0);
                        mutex.release();
                    } else {
                        boarding = true;
                        int ridersToBoard = Math.min(waitingRiders, BUS_CAPACITY);
                        System.out.println("Bus arrived. Riders waiting: " + waitingRiders +
                                ", Boarding: " + ridersToBoard);

                        // Signal riders to board
                        allowBoarding.release(ridersToBoard);

                        // Wait for all riders to board
                        allBoarded.acquire();
                        depart(ridersToBoard);

                        // Reset boarding and counters
                        boarding = false;
                        waitingRiders -= ridersToBoard;
                        boardedRiders = 0;

                        mutex.release();
                    }
                    // Simulate time between bus arrivals
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
            System.out.println("Riders (" + Thread.currentThread().getId() + ") arriving");
            try {


                // Check if bus is currently boarding
                if (boarding) {
                    mutex.acquire();
                    // If bus is boarding, this rider must wait for next bus
                    waitingRiders++;
                    System.out.println("Bus (" + Thread.currentThread().getId() + ") is boarding. Rider (" + Thread.currentThread().getId() + ") waiting for the next bus.");
                    mutex.release();
                } else {
                    mutex.acquire();
                    waitingRiders++;
                    mutex.release();

                    // Wait for bus to arrive and allow boarding
                    allowBoarding.acquire();

                    // Board the bus
                    boardBus();
                    boardedRiders++;

                    // Check if all boarded
                    if (boardedRiders == Math.min(waitingRiders, BUS_CAPACITY)) {
                        allBoarded.release();
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Helpers
    private static long generateExponentialDelay(long mean) {
        Random random = new Random();
        return (long) (-Math.log(1 - random.nextDouble()) * mean);
    }

    private static void depart(int ridersCount) {
        if (ridersCount == 0) {
            System.out.println("Bus (" + Thread.currentThread().getId() + ") is departing immediately.");
        } else {
            System.out.println("Bus (" + Thread.currentThread().getId() + ") is departing with " + ridersCount + " riders.");
        }
    }

    private static void boardBus() {
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
