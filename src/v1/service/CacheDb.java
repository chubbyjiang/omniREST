package v1.service;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import v1.service.model.MapModel;
import v1.service.model.ResultModel;

import java.sql.*;
import java.util.ArrayList;

/**
 * Created by jchubby on 15/8/10.
 */
public class CacheDb {

    //数据库链接需要的jdbc类
    /*private Connection conn;
    private PreparedStatement stmt;
    private ResultSet rs;*/

    JDBCHelper jdbcHelper = new JDBCHelper("jdbc:mysql://117.29.168.34:3307/omniREST?characterEncoding=utf8", "root", "root");

    /**
     * 打开数据库连接
     */
    /*private void connect() {
        try {
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
            conn = DriverManager.getConnection("jdbc:mysql://117.29.168.34:3307/omniREST?characterEncoding=utf8", "root", "root");
            conn.setAutoCommit(true);
            //stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
        } catch (Exception e) {
            System.out.println("database connect error: " + e.getMessage());
        }
    }*/
    public ResultModel query(String sql, String... params) {
        //connect();
        ResultModel resultModel = null;
        try {
            /*stmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setString(i + 1, params[i]);
            }
            rs = stmt.executeQuery();*/
            ResultSet rs = jdbcHelper.executeResultSet(sql, params);
            rs.last();
            if (rs.getRow() > 0) {
                resultModel = new ResultModel();

                rs.first();
                JSONObject jsonObject = JSONObject.fromObject(rs.getString("result"));
                resultModel.setCode((String) jsonObject.get("code"));
                resultModel.setMessage((String) jsonObject.get("message"));

                JSONArray jsonArray = jsonObject.getJSONArray("content");
                ArrayList<MapModel> mapModels = new ArrayList<MapModel>();
                for (int i = 0; i < jsonArray.size(); i++) {
                    MapModel m = (MapModel) JSONObject.toBean(jsonArray.getJSONObject(i), MapModel.class);
                    mapModels.add(m);
                }
                resultModel.setContent(mapModels);

                resultModel.setPreviewPage((String) jsonObject.get("previewPage"));
                resultModel.setNextPage((String) jsonObject.get("nextPage"));
            }
        } catch (Exception e) {
            System.out.println("database connect error: " + e.getMessage());
        }
        finally {
            jdbcHelper.close();
        }
        /*if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }*/
        jdbcHelper.close();
        return resultModel;
    }
}
