package server;

import java.sql.*;

public class DbHelper {

    private static String url;

    /** tworzy plik index.db i tabelę, jeśli brak */
    public static void init(String dbPath) throws SQLException {
        url = "jdbc:sqlite:" + dbPath;
        try (Connection  c = DriverManager.getConnection(url);
             Statement   s = c.createStatement()) {

            s.execute("""
                CREATE TABLE IF NOT EXISTS images(
                    id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    path   TEXT    NOT NULL,
                    size   INTEGER NOT NULL,
                    delay  INTEGER NOT NULL
                )
            """);
        }
    }

    /** wstawia pojedynczy rekord */
    public static void insert(String path, int size, long delay) {
        String sql = "INSERT INTO images(path,size,delay) VALUES(?,?,?)";

        try (Connection        c  = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, path);
            ps.setInt   (2, size);
            ps.setLong  (3, delay);
            ps.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
