package v1_1.service;

import java.sql.*;
import java.util.Map;

public class JDBCHelper {
    private Connection con = null;
    private PreparedStatement stmt = null;
    private ResultSet rs = null;

    public JDBCHelper(String url, String uid, String pwd) {

        try {
            // 加载驱动
            Class.forName("com.mysql.jdbc.Driver");
            // 打开数据库连接
            con = DriverManager.getConnection(url, uid, pwd);

        } catch (ClassNotFoundException e) {
            System.out.println("Error: unable to load driver class!");
            System.out
                    .println("please check if you hava the mysql-connector jar file in your lib dir!");
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public ResultSet executeResultSet(String sql) {
        return executeResultSet(sql, null);
    }

    public ResultSet executeResultSet(String sql, Object[] params) {
        try {
            // 创建一个JDBC声明
            stmt = con.prepareStatement(sql);
            if (null != params && params.length != 0) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }
            rs = stmt.executeQuery();
            return rs;
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public int executeUpdate(String sql, Object[] params) {
        try {
            // 创建一个JDBC声明
            stmt = con.prepareStatement(sql);
            System.out.println(params.length);
            if (params.length != 0) {
                for (int i = 0; i < params.length; i++) {
                    String pp = params[i].toString();
                    if (pp.contains("[") || pp.contains("]")) {
                        pp = pp.substring(1, pp.length() - 1);
                    }
                    stmt.setObject(i + 1, pp);
                }
            }
            return stmt.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public int executeUpdate(String sql, String pkey) {
        try {
            // 创建一个JDBC声明
            stmt = con.prepareStatement(sql);
            stmt.setObject(1, pkey);
            return stmt.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public int executeUpdate(String sql, String pkey, Object[] params) {
        try {
            // 创建一个JDBC声明
            stmt = con.prepareStatement(sql);
            if (params.length != 0) {
                for (int i = 0; i < params.length; i++) {
                    String pp = params[i].toString();
                    if (pp.contains("[") || pp.contains("]")) {
                        pp = pp.substring(1, pp.length() - 1);
                    }
                    stmt.setObject(i + 1, pp);
                }
            }
            stmt.setString(params.length + 1, pkey);
            return stmt.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public void close() {
        try {
            // 应该明确地关闭所有的数据库资源
            if (null != rs)
                rs.close();
            if (null != stmt)
                stmt.close();
            if (null != con)
                con.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
}
