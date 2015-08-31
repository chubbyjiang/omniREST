package v1_1.service;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.dbcp.BasicDataSource;
import v1.service.*;
import v1_1.service.model.ResultModel;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.IdentityHashMap;

/**
 * Created by jchubby on 15/8/10.
 */
public class CacheDb {
    JDBCHelper jdbcHelper = new JDBCHelper("jdbc:mysql://117.29.168.34:3307/omniREST?characterEncoding=utf8", "root", "root");


    public ResultModel query(String sql, String... params) throws SQLException {
        ResultModel resultModel = null;
        ResultSet rs = jdbcHelper.executeResultSet(sql, params);
        rs.last();
        if (rs.getRow() > 0) {
            resultModel = new ResultModel();

            rs.first();
            JSONObject jsonObject = JSONObject.fromObject(rs.getString("result"));
            resultModel.setCode((String) jsonObject.get("code"));
            resultModel.setMessage((String) jsonObject.get("message"));

            JSONArray jsonArray = jsonObject.getJSONArray("content");
            ArrayList<IdentityHashMap<String, String>> mapModels = new ArrayList<IdentityHashMap<String, String>>();
            for (int i = 0; i < jsonArray.size(); i++) {
                IdentityHashMap<String, String> m = (IdentityHashMap<String, String>) JSONObject.toBean(jsonArray.getJSONObject(i), IdentityHashMap.class);
                mapModels.add(m);
            }
            resultModel.setContent(mapModels);

            resultModel.setPre_page((String) jsonObject.get("previewPage"));
            resultModel.setNext_page((String) jsonObject.get("nextPage"));
        }
        jdbcHelper.close();
        return resultModel;
    }
}
