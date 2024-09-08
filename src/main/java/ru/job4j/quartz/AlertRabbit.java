package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

public class AlertRabbit implements AutoCloseable {

    private Connection connection;

    public AlertRabbit() {
        init();
    }

    public AlertRabbit(Connection connection) {
        this.connection = connection;
    }

    private void init() {
        try (InputStream input = AlertRabbit.class.getClassLoader()
                .getResourceAsStream("rabbit.properties")) {
            Properties config = new Properties();
            config.load(input);
            Class.forName(config.getProperty("driver-class-name"));
            connection = DriverManager.getConnection(
                    config.getProperty("url"),
                    config.getProperty("username"),
                    config.getProperty("password")
            );

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    public static void main(String[] args) {
        try (AlertRabbit alertRabbit = new AlertRabbit()) {
            try {
                Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
                scheduler.start();
                JobDataMap data = new JobDataMap();
                data.put("alertRabbit", alertRabbit);
                JobDetail job = newJob(Rabbit.class)
                        .usingJobData(data)
                        .build();
                SimpleScheduleBuilder times = simpleSchedule()
                        .withIntervalInSeconds(10)
                        .repeatForever();
                Trigger trigger = newTrigger()
                        .startNow()
                        .withSchedule(times)
                        .build();
                scheduler.scheduleJob(job, trigger);
                Thread.sleep(10000);
                scheduler.shutdown();
            } catch (SchedulerException se) {
                se.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Rabbit implements Job {
        private LocalDateTime created = LocalDateTime.now();

        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
            AlertRabbit alertRabbit = (AlertRabbit) context.getJobDetail().getJobDataMap().get("alertRabbit");
            try (PreparedStatement statement =
                         alertRabbit.connection.prepareStatement("INSERT INTO rabbit(created_date) VALUES (?)")) {
                statement.setTimestamp(1, Timestamp.valueOf(created));
                statement.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}