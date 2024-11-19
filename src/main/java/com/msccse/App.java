package com.msccse;

import java.util.Random;
import java.util.concurrent.Semaphore;

public class App {
    // Maximum bus capacity
    private static final int BUS_CAPACITY = 50;
    // Shared variables tracking riders
    private static int waitingRiders = 0;
    private static int boardedRiders = 0;
    private static boolean boarding = false;

    // Synchronization variables
    private static Semaphore mutex = new Semaphore(1);
    private static Semaphore busArrived = new Semaphore(0);
    private static Semaphore allBoarded = new Semaphore(0);

    // Bus thread class
    static class Bus extends Thread {
        public void run() {
            try {
                while (true) {
                    // Acquire mutex to check waiting riders
//                    System.out.println("Bus arriving");
                    mutex.acquire();

                    if (waitingRiders == 0) {
                        // No riders, bus departs immediately
                        System.out.println("Bus arrived with no riders. Departing immediately.");
                        mutex.release();
                    } else {
                        // Prepare for boarding
                        boarding = true;
                        int ridersToBoard = Math.min(waitingRiders, BUS_CAPACITY);
                        System.out.println("Bus arrived. Riders waiting: " + waitingRiders +
                                ", Boarding: " + ridersToBoard);

                        // Signal riders to board
                        busArrived.release(ridersToBoard);

                        // Wait for all riders to board
                        allBoarded.acquire();

                        // Reset boarding and counters
                        boarding = false;
                        waitingRiders -= ridersToBoard;
                        boardedRiders = 0;

                        System.out.println("Bus departing. Remaining waiting riders: " + waitingRiders);

                        // Release mutex
                        mutex.release();
                    }

                    // Simulate time between bus arrivals (exponential distribution)
                    Thread.sleep(generateExponentialDelay(10000)); // 1000 milliseconds = 1 second
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
                // Remove the inner while(true) loop
//                System.out.println("Rider arriving");
                mutex.acquire();

                // Check if bus is currently boarding
                if (boarding) {
                    // If bus is boarding, this rider must wait for next bus
                    waitingRiders++;
                    mutex.release();
                } else {
                    waitingRiders++;
                    mutex.release();

                    // Wait for bus to arrive
                    busArrived.acquire();

                    // Board the bus
                    System.out.println("Rider boarding");
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
    // Generate exponential delay
    private static long generateExponentialDelay(long mean) {
        Random random = new Random();
        return (long) (-Math.log(1 - random.nextDouble()) * mean);
    }

    public static void main(String[] args) {
        // Start threads
        Bus busThread = new Bus();
        busThread.start();

        // Continuously create rider threads
        while (true) {
            Rider riderThread = new Rider();
            riderThread.start();

            try {
                // Simulate time between rider arrivals (exponential distribution)
                Thread.sleep(generateExponentialDelay(1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}