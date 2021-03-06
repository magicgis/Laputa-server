package com.laputa.server.core.stats.model;

import com.laputa.server.core.BlockingIOProcessor;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 03.05.17.
 */
public class BlockingIOStat {

    private final int messagingActiveTasks;

    private final long messagingExecutedTasks;

    private final int historyActiveTasks;

    private final long historyExecutedTasks;

    private final int dbActiveTasks;

    private final long dbExecutedTasks;


    public BlockingIOStat(BlockingIOProcessor blockingIOProcessor) {
        this(blockingIOProcessor.messagingActiveTasks(), blockingIOProcessor.messagingExecutedTasks(),
             blockingIOProcessor.historyActiveTasks(), blockingIOProcessor.historyExecutedTasks(),
             blockingIOProcessor.dbActiveTasks(), blockingIOProcessor.dbExecutedTasks());
    }

    public BlockingIOStat(int messagingActiveTasks, long messagingExecutedTasks,
                          int historyActiveTasks, long historyExecutedTasks,
                          int dbActiveTasks, long dbExecutedTasks) {
        this.messagingActiveTasks = messagingActiveTasks;
        this.messagingExecutedTasks = messagingExecutedTasks;
        this.historyActiveTasks = historyActiveTasks;
        this.historyExecutedTasks = historyExecutedTasks;
        this.dbActiveTasks = dbActiveTasks;
        this.dbExecutedTasks = dbExecutedTasks;
    }
}
