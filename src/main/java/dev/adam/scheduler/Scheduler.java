package dev.adam.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Scheduler {
    private static final Map<String, BukkitTask> namedTasks = new ConcurrentHashMap<>();
    private static final Map<Plugin, Set<BukkitTask>> pluginTasks = new ConcurrentHashMap<>();
    private static final Map<BukkitTask, TaskInfo> taskInfoMap = new ConcurrentHashMap<>();
        
    public static BukkitTask runLater(Plugin plugin, Runnable task, long delay) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        trackTask(plugin, bukkitTask, "runLater", delay, -1);
        return bukkitTask;
    }
    
    public static BukkitTask runRepeating(Plugin plugin, Runnable task, long delay, long period) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        trackTask(plugin, bukkitTask, "runRepeating", delay, period);
        return bukkitTask;
    }
    
    public static BukkitTask runNow(Plugin plugin, Runnable task) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTask(plugin, task);
        trackTask(plugin, bukkitTask, "runNow", 0, -1);
        return bukkitTask;
    }
        
    public static BukkitTask runAsync(Plugin plugin, Runnable task) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        trackTask(plugin, bukkitTask, "runAsync", 0, -1);
        return bukkitTask;
    }
    
    public static BukkitTask runAsyncLater(Plugin plugin, Runnable task, long delay) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
        trackTask(plugin, bukkitTask, "runAsyncLater", delay, -1);
        return bukkitTask;
    }
    
    public static BukkitTask runAsyncRepeating(Plugin plugin, Runnable task, long delay, long period) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
        trackTask(plugin, bukkitTask, "runAsyncRepeating", delay, period);
        return bukkitTask;
    }
        
    public static BukkitTask runNamedTask(Plugin plugin, String name, Runnable task, long delay) {
        cancelNamedTask(name);
        BukkitTask bukkitTask = runLater(plugin, task, delay);
        namedTasks.put(name, bukkitTask);
        return bukkitTask;
    }
    
    public static BukkitTask runNamedRepeatingTask(Plugin plugin, String name, Runnable task, long delay, long period) {
        cancelNamedTask(name);
        BukkitTask bukkitTask = runRepeating(plugin, task, delay, period);
        namedTasks.put(name, bukkitTask);
        return bukkitTask;
    }
    
    public static boolean cancelNamedTask(String name) {
        BukkitTask task = namedTasks.remove(name);
        if (task != null && !task.isCancelled()) {
            task.cancel();
            return true;
        }
        return false;
    }
    
    public static boolean isNamedTaskRunning(String name) {
        BukkitTask task = namedTasks.get(name);
        return task != null && !task.isCancelled();
    }
    
    public static BukkitTask runIf(Plugin plugin, Supplier<Boolean> condition, Runnable task, long delay) {
        return runLater(plugin, () -> {
            if (condition.get()) {
                task.run();
            }
        }, delay);
    }
    
    public static BukkitTask runUntil(Plugin plugin, Supplier<Boolean> condition, Runnable task, long delay, long period) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (!condition.get()) {
                    this.cancel();
                    return;
                }
                task.run();
            }
        }.runTaskTimer(plugin, delay, period);
    }
    
    public static BukkitTask runWhile(Plugin plugin, Supplier<Boolean> condition, Runnable task, long delay, long period) {
        return runUntil(plugin, condition, task, delay, period);
    }

    public static BukkitTask runWithRetry(Plugin plugin, Runnable task, int maxRetries, long retryDelay) {
        return runWithRetry(plugin, () -> {
            task.run();
            return true; // Assume success if no exception
        }, maxRetries, retryDelay, null);
    }
    
    public static BukkitTask runWithRetry(Plugin plugin, Supplier<Boolean> task, int maxRetries, long retryDelay, Consumer<Integer> onFailure) {
        return new BukkitRunnable() {
            int attempts = 0;
            
            @Override
            public void run() {
                attempts++;
                try {
                    if (task.get()) {
                        this.cancel(); // Success!
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("Task failed on attempt " + attempts + ": " + e.getMessage());
                }
                
                if (attempts >= maxRetries) {
                    this.cancel();
                    if (onFailure != null) {
                        onFailure.accept(attempts);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, retryDelay);
    }
    
    public static void chainTasks(Plugin plugin, TaskChain... chains) {
        if (chains.length == 0) return;
        
        TaskChain first = chains[0];
        BukkitTask task = runLater(plugin, first.task, first.delay);
        
        if (chains.length > 1) {
            TaskChain[] remaining = Arrays.copyOfRange(chains, 1, chains.length);
            runLater(plugin, () -> chainTasks(plugin, remaining), first.delay + 1);
        }
    }
    
    public static class TaskChain {
        final Runnable task;
        final long delay;
        
        public TaskChain(Runnable task, long delay) {
            this.task = task;
            this.delay = delay;
        }
        
        public static TaskChain of(Runnable task, long delay) {
            return new TaskChain(task, delay);
        }
    }
    
    public static BukkitTask runCountdown(Plugin plugin, int seconds, Consumer<Integer> onTick, Runnable onFinish) {
        return new BukkitRunnable() {
            int remaining = seconds;
            
            @Override
            public void run() {
                if (remaining <= 0) {
                    this.cancel();
                    if (onFinish != null) {
                        onFinish.run();
                    }
                    return;
                }
                
                if (onTick != null) {
                    onTick.accept(remaining);
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Every second
    }
    
    public static <T> CompletableFuture<T> supplyAsync(Plugin plugin, Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        runAsync(plugin, () -> {
            try {
                T result = supplier.get();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
        
    private static final Map<String, Long> rateLimits = new ConcurrentHashMap<>();
    
    public static boolean runRateLimited(Plugin plugin, String key, long cooldownTicks, Runnable task) {
        long currentTime = System.currentTimeMillis();
        Long lastRun = rateLimits.get(key);
        
        if (lastRun != null && (currentTime - lastRun) < (cooldownTicks * 50)) { 
            return false; 
        }
        
        rateLimits.put(key, currentTime);
        runNow(plugin, task);
        return true;
    }
        
    public static void cancelTask(BukkitTask task) {
        if (task != null && !task.isCancelled()) {
            task.cancel();
            untrackTask(task);
        }
    }
    
    public static void cancelAllTasks(Plugin plugin) {
        Set<BukkitTask> tasks = pluginTasks.get(plugin);
        if (tasks != null) {
            tasks.forEach(task -> {
                if (!task.isCancelled()) {
                    task.cancel();
                }
            });
            tasks.clear();
        }
        
        namedTasks.entrySet().removeIf(entry -> {
            BukkitTask task = entry.getValue();
            TaskInfo info = taskInfoMap.get(task);
            return info != null && info.plugin.equals(plugin);
        });
    }
    
    public static void cancelTasksById(int... taskIds) {
        for (int id : taskIds) {
            Bukkit.getScheduler().cancelTask(id);
        }
    }
        
    public static class TaskInfo {
        public final Plugin plugin;
        public final String type;
        public final long delay;
        public final long period;
        public final long createdAt;
        
        public TaskInfo(Plugin plugin, String type, long delay, long period) {
            this.plugin = plugin;
            this.type = type;
            this.delay = delay;
            this.period = period;
            this.createdAt = System.currentTimeMillis();
        }
    }
    
    private static void trackTask(Plugin plugin, BukkitTask task, String type, long delay, long period) {
        pluginTasks.computeIfAbsent(plugin, k -> ConcurrentHashMap.newKeySet()).add(task);
        taskInfoMap.put(task, new TaskInfo(plugin, type, delay, period));
    }
    
    private static void untrackTask(BukkitTask task) {
        TaskInfo info = taskInfoMap.remove(task);
        if (info != null) {
            Set<BukkitTask> tasks = pluginTasks.get(info.plugin);
            if (tasks != null) {
                tasks.remove(task);
            }
        }
    }
        
    public static int getActiveTaskCount() {
        return (int) taskInfoMap.keySet().stream()
                .filter(task -> !task.isCancelled())
                .count();
    }
    
    public static int getActiveTaskCount(Plugin plugin) {
        Set<BukkitTask> tasks = pluginTasks.get(plugin);
        if (tasks == null) return 0;
        return (int) tasks.stream()
                .filter(task -> !task.isCancelled())
                .count();
    }
    
    public static Map<String, Integer> getTaskStatsByType() {
        Map<String, Integer> stats = new HashMap<>();
        taskInfoMap.entrySet().stream()
                .filter(entry -> !entry.getKey().isCancelled())
                .forEach(entry -> {
                    String type = entry.getValue().type;
                    stats.put(type, stats.getOrDefault(type, 0) + 1);
                });
        return stats;
    }
    
    public static void printTaskStatistics() {
        System.out.println("=== Scheduler Statistics ===");
        System.out.println("Total active tasks: " + getActiveTaskCount());
        System.out.println("Named tasks: " + namedTasks.size());
        
        Map<String, Integer> typeStats = getTaskStatsByType();
        typeStats.forEach((type, count) -> 
            System.out.println("  " + type + ": " + count)
        );
        
        System.out.println("Tasks by plugin:");
        pluginTasks.forEach((plugin, tasks) -> {
            long activeCount = tasks.stream()
                    .filter(task -> !task.isCancelled())
                    .count();
            if (activeCount > 0) {
                System.out.println("  " + plugin.getName() + ": " + activeCount);
            }
        });
    }
    
    public static long secondsToTicks(double seconds) {
        return (long) (seconds * 20.0);
    }
    
    public static long minutesToTicks(double minutes) {
        return secondsToTicks(minutes * 60.0);
    }
    
    public static double ticksToSeconds(long ticks) {
        return ticks / 20.0;
    }
    
    public static boolean isPrimaryThread() {
        return Bukkit.isPrimaryThread();
    }
    
    public static void ensureMainThread(Plugin plugin, Runnable task) {
        if (isPrimaryThread()) {
            task.run();
        } else {
            runNow(plugin, task);
        }
    }
    
    public static void cleanupFinishedTasks() {
        taskInfoMap.entrySet().removeIf(entry -> entry.getKey().isCancelled());
        
        pluginTasks.values().forEach(tasks -> 
            tasks.removeIf(BukkitTask::isCancelled)
        );
        
        namedTasks.entrySet().removeIf(entry -> entry.getValue().isCancelled());
    }
    
    public static void shutdown(Plugin plugin) {
        cancelAllTasks(plugin);
        pluginTasks.remove(plugin);
        cleanupFinishedTasks();
    }
}