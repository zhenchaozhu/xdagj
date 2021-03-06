/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2030 The XdagJ Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.xdag.event;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.xdag.utils.exception.UnreachableException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PubSub {

    private final String name;

    private final LinkedBlockingQueue<PubSubEvent> queue;

    /** [event] => [list of subscribers] */
    private final ConcurrentHashMap<Class<? extends PubSubEvent>, ConcurrentLinkedQueue<PubSubSubscriber>> subscribers;
    private final AtomicBoolean isRunning;
    private Thread eventProcessingThread;

    protected PubSub(String name) {
        this.name = name;
        queue = new LinkedBlockingQueue<>();
        subscribers = new ConcurrentHashMap<>();
        isRunning = new AtomicBoolean(false);
    }

    /**
     * Add an event to {@link this#queue}.
     *
     * @param event
     *            the event to be subscribed.
     * @return whether the event is successfully added.
     */
    public boolean publish(PubSubEvent event) {
        // Do not accept any event if this pubsub instance hasn't been started in order
        // to avoid memory garbage.
        if (!isRunning.get()) {
            return false;
        }

        return queue.add(event);
    }

    /**
     * Subscribe to an event.
     *
     * @param subscriber
     *            the subscriber.
     * @param eventClss
     *            the event classes to be subscribed.
     */
    @SafeVarargs
    public final void subscribe(
            PubSubSubscriber subscriber, Class<? extends PubSubEvent>... eventClss) {
        for (Class<? extends PubSubEvent> eventCls : eventClss) {
            subscribers.computeIfAbsent(eventCls, k -> new ConcurrentLinkedQueue<>()).add(subscriber);
        }
    }

    /**
     * Unsubscribe an event.
     *
     * @param subscriber
     *            the subscriber.
     * @param event
     *            the event to be unsubscribed.
     * @return whether the event is successfully unsubscribed.
     */
    public boolean unsubscribe(PubSubSubscriber subscriber, Class<? extends PubSubEvent> event) {
        ConcurrentLinkedQueue<?> q = subscribers.get(event);
        if (q != null) {
            return q.remove(subscriber);
        } else {
            return false;
        }
    }

    /**
     * Unsubscribe from all events.
     *
     * @param subscriber
     *            the subscriber.
     */
    public void unsubscribeAll(final PubSubSubscriber subscriber) {
        subscribers.values().forEach(q -> q.remove(subscriber));
    }

    /**
     * Start the {@link this#eventProcessingThread}.
     *
     * @throws UnreachableException
     *             this method should only be called for once, otherwise an
     *             exception will be thrown.
     */
    public synchronized void start() {
        if (!isRunning.compareAndSet(false, true)) {
            throw new UnreachableException("PubSub service can be started for only once");
        }

        eventProcessingThread = new Thread(new EventProcessor(), "event-processor-" + name);
        eventProcessingThread.start();
        log.info("PubSub service started");
    }

    /** Stop the {@link this#eventProcessingThread}. */
    public synchronized void stop() {
        eventProcessingThread.interrupt();
        try {
            eventProcessingThread.join(10_000L);
        } catch (InterruptedException e) {
            log.error("Interrupted while joining the PubSub processing thread");
        }

        isRunning.set(false);
        log.info("PubSub service stopped");
    }

    /**
     * This thread will be continuously polling for new events until PubSub is
     * stopped.
     */
    private class EventProcessor implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                PubSubEvent event;
                try {
                    event = queue.take();
                } catch (InterruptedException e) {
                    return;
                }

                ConcurrentLinkedQueue<PubSubSubscriber> q = subscribers.get(event.getClass());
                if (q != null) {
                    for (PubSubSubscriber subscriber : q) {
                        try {
                            subscriber.onPubSubEvent(event);
                        } catch (Exception e) {
                            log.error("Event processing error", e);
                        }
                    }
                }
            }
        }
    }
}
