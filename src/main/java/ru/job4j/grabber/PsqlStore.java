package ru.job4j.grabber;

import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store {

    private Connection connection;

    public PsqlStore(Properties config) throws SQLException {

        try {
            Class.forName(config.getProperty("driver-class-name"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        connection = DriverManager.getConnection(
                config.getProperty("url"),
                config.getProperty("username"),
                config.getProperty("password")
        );
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement statement =
                     connection.prepareStatement("INSERT INTO posts(name,text,link, created) VALUES (?, ?, ?, ?)"
                             + " ON CONFLICT (link) DO NOTHING",
                             Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getDescription());
            statement.setString(3, post.getLink());
            statement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            statement.execute();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    post.setId(generatedKeys.getInt(1));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Post createPost(ResultSet resultSet) throws SQLException {
        return new Post(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("text"),
                resultSet.getString("link"),
                resultSet.getTimestamp("created").toLocalDateTime()
        );
    }

    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM posts")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    posts.add(createPost(resultSet));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return posts;

    }

    @Override
    public Post findById(int id) {
        Post post = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM posts WHERE id=?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    post = createPost(resultSet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return post;
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    public static void main(String[] args) {
        try (InputStream input = PsqlStore.class.getClassLoader()
                .getResourceAsStream("rabbit.properties")) {
            Properties config = new Properties();
            config.load(input);
            List<Post> posts;
            try (PsqlStore psqlStore = new PsqlStore(config)) {
                HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
                psqlStore.save(new Post(1, "Java Developer", "https://career.habr.com/vacancies/1000091983",
                        "Проекты по разработке сервисов ", habrCareerParse.dateTimeParser.parse("2024-09-09T11:49:24+03:00")));
                psqlStore.save(new Post(2, "Java Developer2", "https://career.habr.com/vacancies/1000146900",
                        "Международная ИТ-компания, специализирующаяся", habrCareerParse.dateTimeParser.parse("2024-09-09T13:32:52+03:00")));
                psqlStore.save(new Post(3, "Java Developer", " https://career.habr.com/vacancies/1000143124",
                        "Международная ИТ-компания, специализирующаяся", habrCareerParse.dateTimeParser.parse("2024-09-06T10:07:20+03:00")));
                System.out.println(psqlStore.findById(2));
                posts = psqlStore.getAll();
            }
            for (Post post : posts) {
                System.out.println(post);
            }

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
