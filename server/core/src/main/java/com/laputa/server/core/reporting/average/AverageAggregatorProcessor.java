package com.laputa.server.core.reporting.average;

import com.laputa.server.core.model.auth.User;
import com.laputa.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.laputa.utils.ReportingUtil.read;
import static com.laputa.utils.ReportingUtil.write;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 10.08.15.
 */
public class AverageAggregatorProcessor implements Closeable {

    private static final Logger log = LogManager.getLogger(AverageAggregatorProcessor.class);

    public static final long MINUTE = 1000 * 60;
    public static final long HOUR = 60 * MINUTE;
    public static final long DAY = 24 * HOUR;
    public static final String MINUTE_TEMP_FILENAME = "minute_temp.bin";
    public static final String HOURLY_TEMP_FILENAME = "hourly_temp.bin";
    public static final String DAILY_TEMP_FILENAME = "daily_temp.bin";
    private final String dataFolder;
    private final ConcurrentHashMap<AggregationKey, AggregationValue> minute;
    private final ConcurrentHashMap<AggregationKey, AggregationValue> hourly;
    private final ConcurrentHashMap<AggregationKey, AggregationValue> daily;

    public AverageAggregatorProcessor(String dataFolder) {
        this.dataFolder = dataFolder;

        Path path;

        path = Paths.get(dataFolder, MINUTE_TEMP_FILENAME);
        this.minute = read(path);
        FileUtils.deleteQuietly(path);

        path = Paths.get(dataFolder, HOURLY_TEMP_FILENAME);
        this.hourly = read(path);
        FileUtils.deleteQuietly(path);

        path = Paths.get(dataFolder, DAILY_TEMP_FILENAME);
        this.daily = read(path);
        FileUtils.deleteQuietly(path);
    }

    private static void aggregate(Map<AggregationKey, AggregationValue> map, AggregationKey key, double value) {
        AggregationValue aggregationValue = map.get(key);
        if (aggregationValue == null) {
            final AggregationValue aggregationValueTmp = new AggregationValue();
            aggregationValue = map.putIfAbsent(key, aggregationValueTmp);
            if (aggregationValue == null) {
                aggregationValue = aggregationValueTmp;
            }
        }

        aggregationValue.update(value);
    }

    public void collect(User user, int dashId, int deviceId, char pinType, byte pin, long ts, double val) {
        aggregate(minute, new AggregationKey(user.email, user.appName, dashId, deviceId, pinType, pin, ts / MINUTE), val);
        aggregate(hourly, new AggregationKey(user.email, user.appName, dashId, deviceId, pinType, pin, ts / HOUR), val);
        aggregate(daily, new AggregationKey(user.email, user.appName, dashId, deviceId, pinType, pin, ts / DAY), val);
    }

    public ConcurrentHashMap<AggregationKey, AggregationValue> getMinute() {
        return minute;
    }

    public ConcurrentHashMap<AggregationKey, AggregationValue> getHourly() {
        return hourly;
    }

    public ConcurrentHashMap<AggregationKey, AggregationValue> getDaily() {
        return daily;
    }

    @Override
    public void close() {
        if (minute.size() > 100_000) {
            log.info("Too many minute records ({}). This may cause performance issues on server start. Skipping.", minute.size());
        } else {
            write(Paths.get(dataFolder, MINUTE_TEMP_FILENAME), minute);
        }
        write(Paths.get(dataFolder, HOURLY_TEMP_FILENAME), hourly);
        write(Paths.get(dataFolder, DAILY_TEMP_FILENAME), daily);
    }

}
