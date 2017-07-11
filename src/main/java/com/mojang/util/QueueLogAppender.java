package com.mojang.util;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Plugin(name = "Queue", category = "Core", elementType = "appender", printObject = true)
public class QueueLogAppender extends AbstractAppender {

    private static final Map<String, BlockingQueue<String>> QUEUES = new HashMap<>();
    private static final ReadWriteLock QUEUE_LOCK = new ReentrantReadWriteLock();
    private final BlockingQueue<String> queue;
    
    public QueueLogAppender(String name, Filter filter, Layout<? extends Serializable> layout,
                            boolean ignoreExceptions, BlockingQueue<String> queue) {
        super(name, filter, layout, ignoreExceptions);
        this.queue = queue;
    }
    
    public void append(final LogEvent event) {
        if(queue.size() >= 250) {
            queue.clear();
        }

        queue.add(this.getLayout().toSerializable(event).toString());
    }
    
    @PluginFactory
    public static QueueLogAppender createAppender(@PluginAttribute("name") String name,
                                                  @PluginAttribute("ignoreExceptions") String ignore,
                                                  @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                  @PluginElement("Filters") Filter filter,
                                                  @PluginAttribute("target") String target) {
        boolean ignoreExceptions = Boolean.parseBoolean(ignore);

        if(name == null) {
            LOGGER.error("No name provided for QueueLogAppender");
            return null;
        }

        if(target == null) {
            target = name;
        }

        QUEUE_LOCK.writeLock().lock();
        BlockingQueue<String> queue = QUEUES.computeIfAbsent(target, k -> new LinkedBlockingQueue<>());
        QUEUE_LOCK.writeLock().unlock();

        if(layout == null) {
            layout = PatternLayout.newBuilder().build();
        }

        return new QueueLogAppender(name, filter, layout, ignoreExceptions, queue);
    }

    public static String getNextLogEvent(String queueName) {
        QUEUE_LOCK.readLock().lock();
        BlockingQueue<String> queue = QUEUES.get(queueName);
        QUEUE_LOCK.readLock().unlock();

        if(queue != null) {
            try {
                return queue.take();
            } catch(InterruptedException ignored) {}
        }

        return null;
    }

}
