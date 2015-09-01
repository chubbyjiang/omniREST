package v1_1.service;

import v1_1.service.model.FieldModel;
import v1_1.service.model.ResultModel;
import v1_1.service.model.StatusCode;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * Created by JChubby on 2015/7/16.
 */
@Path("/v1.1/{projectName}/{tableName}")
public class Service {

    @PathParam("tableName")
    String tableName;

    @Context
    UriInfo uriInfo;
    @Context
    HttpServletRequest request;

    //jdbc连接相关
    private String projectName;
    private static String jdbc;
    private static String username;
    private static String password;
    //记录上次访问的项目名，用于对比是否更新配置文件信息
    private static String lastProject;
    //配置文件所在地址
    private static String FILEPATH;
    //获得docs中该项目删除标识位的字段名
    private static String deleteFlag;
    //获得docs中该项目的服务地址，用户返回翻页链接使用
    private static String pagerUrl;

    //读库访问类
    private CacheDb cacheDb = new CacheDb();

    //kafka消息生产者
    private static KafkaProducer kafkaProducer = new KafkaProducer();

    //数据库访问总控
    JDBCHelper jdbcHelper;


    /*
            * 根据条件查询记录集合
            * */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResultModel getAll() {
        initVars();

        //------------------------相关字段初始化---------------------------

        //读取对应库表的JSON文件并转换成Map集合
        File tableFile = new File(FILEPATH + tableName);
        Map<String, Object> tableMap = Util.parseJSON2Map(Util.readToString(tableFile));
        //该库表中的主键名
        String pk = (String) tableMap.get("pk");
        //返回的数据
        ResultModel resultModel = null;
        //请求的参数
        MultivaluedMap<String, String> map = uriInfo.getQueryParameters();
        //拼接的sql字符串
        StringBuilder sql = new StringBuilder();
        //返回的状态码
        StatusCode statusCode = StatusCode.SUCCESS;
        //返回的信息
        String msg = "";
        //返回的翻页链接的参数部分
        String returnUrl = "";
        //判断是否可以执行SQL语句（满足了各方面的条件之后）
        Boolean isCollect = true;
        HashMap<Integer, String> paramsMap = new HashMap<Integer, String>();

        //------------------------查读库信息---------------------------
        sql.append("select result from tb_cache where requestUrl=? and requestTable=? and requestParams=? and requestMethod=?");
        try {
            resultModel = cacheDb.query(sql.toString()
                    , request.getRequestURL().toString().replace("'", "\\'")
                    , tableName.replace("'", "\\'")
                    , map.toString().replace("'", "\\'")
                    , request.getMethod().replace("'", "\\'"));
        } catch (SQLException e) {
            System.out.println("查询读库失败：" + e.getMessage());
            e.printStackTrace();
            System.out.println();
        }
        //如果读库中没有数据则到后台数据库中查询
        if (resultModel == null) {
            resultModel = new ResultModel();
            sql = new StringBuilder();
            //------------------------开始处理请求---------------------------

            //如果存在特殊的规则，那么按照特殊规则来执行
            if (map.containsKey("ruleName")) {
                //获得请求参数中rules的值，为自定义的规则名
                String ruleName = map.get("ruleName").get(0);
                //获得配置文件中对应rule的详细信息
                try {
                    Map rules = Util.getRules(tableMap, ruleName);
                    if (rules == null) {
                        msg = "没有配置规则，请检查配置文件信息";
                        isCollect = false;
                        statusCode = StatusCode.FAILD;
                    } else {
                        List<String> parameters = new ArrayList<String>();
                        //获得配置文件中（POST部分）的字段信息（对应数据库中的所有字段，不包含主键和删除标志位）
                        ArrayList<FieldModel> fields = Util.getfields(tableMap);
                        fields.add(new FieldModel(pk, "", true));
                        if (deleteFlag != null) {
                            fields.add(new FieldModel(deleteFlag, "", true));
                        }

                        //将rule拼合成SQL语句
                        sql.append("select ").append(rules.get("wantFields")).append(" from ").append(rules.get("tables"));
                        //判断是否有join连接
                        Boolean hasJoin = false;
                        if (rules.get("join") != null) {
                            hasJoin = true;
                            sql.append(" ").append(rules.get("join")).append(" where ").append(rules.get("relationFields"));
                        } else {
                            sql.append(" where ").append(rules.get("relationFields"));
                        }
                        //判断是否有内嵌的sql语句存在
                        StringBuilder innerSql = new StringBuilder();
                        Boolean hasInnerSql = false;
                        Boolean hasWhere = false;
                        //拼接内嵌的sql
                        if (rules.get("inner") != null) {
                            hasInnerSql = true;
                            innerSql.append(" in ").append("(").append(rules.get("inner"));
                        }
                        returnUrl += "rules=" + ruleName + "&";

                        //如果请求的参数包含在fields中才进行处理
                        for (FieldModel field : fields) {
                            if (map.containsKey(field.getName())) {
                                String key = field.getName();
                                //分别处理大于、小于、模糊查询、普通查询的请求
                                String value = map.get(key).get(0);
                                String[] values = value.split(" ");
                                if (values.length > 1 && (values[0].contains("gt") || values[0].contains("lt") || values[0].contains("lk") || values[0].contains("ct") || values[0].contains("inner"))) {
                                    if (values[0].equals("gt")) {
                                        parameters.add(String.format(" %s.%s>%s", tableName, key, values[1]));
                                        returnUrl += String.format("%s=gt+%s&", field.getName(), values[1]);
                                    } else if (values[0].equals("lt")) {
                                        parameters.add(String.format(" %s.%s<%s", tableName, key, values[1]));
                                        returnUrl += String.format("%s=lt+%s&", field.getName(), values[1]);
                                    } else if (values[0].equals("lk")) {
                                        //parameters.add(" " + tableName + "." + key + " like '%" + values[1] + "%'");
                                        parameters.add(" " + Util.getVagueSQL(tableName + "." + key, values[1]));
                                        returnUrl += String.format("%s=lk+%s&", key, values[1]);
                                    } else if (values[0].equals("ct")) {
                                        parameters.add(" " + tableName + "." + key + " like '%" + values[1] + "%'");
                                        returnUrl += String.format("%s=ct+%s&", key, values[1]);
                                    } else if (values[0].equals("inner")) {
                                        if (hasInnerSql) {
                                            if (!hasWhere) {
                                                innerSql.append(" where ");
                                                hasWhere = true;
                                            }
                                            innerSql.append(String.format(" %s.%s=%s", tableName, key, values[1]));
                                        }
                                    }
                                } else {
                                    parameters.add(String.format(" %s.%s=%s", tableName, key, value));
                                    returnUrl += String.format("%s=%s&", key, value);
                                }
                            }
                        }
                        if (hasInnerSql) {
                            innerSql.append(")");
                        }
                        sql.append(innerSql);
                        if (parameters.size() != 0) {
                            for (String parameter : parameters) {
                                sql.append(" and ");
                                String s = parameter;
                                sql.append(s);
                            }
                            if (deleteFlag != null) {
                                sql.append(String.format(" and %s.%s=0", tableName, deleteFlag));
                            }
                        } else {
                            if (deleteFlag != null) {
                                sql.append(String.format(" and %s.%s=0", tableName, deleteFlag));
                            }
                        }
                        //如果有join和groupby则拼接到末尾
                        if (hasJoin) {
                            if (rules.get("groupby") != null) {
                                sql.append(" group by ").append(rules.get("groupby"));
                            }
                        }
                    }
                } catch (Exception ex) {
                    msg = "配置文件读取错误:" + ex.getMessage();
                    isCollect = false;
                    statusCode = StatusCode.FAILD;
                }
            }
            //请求参数中没有包含rules，则按默认的查询动作执行
            else {
                if (map.containsKey("getCount")) {
                    sql.append(String.format("select count(*) from %s", tableName));
                } else {
                    sql.append(String.format("select * from %s", tableName));
                }
                List<String> parameters = new ArrayList<String>();
                //or查询使用的参数集合
                List<String> orParameters = new ArrayList<String>();
                //获得配置文件中（POST部分）的字段信息（对应数据库中的所有字段，不包含主键和删除标志位）
                try {
                    ArrayList<FieldModel> fields = Util.getfields(tableMap);
                    fields.add(new FieldModel(pk, "", true));
                    if (deleteFlag != null) {
                        fields.add(new FieldModel(deleteFlag, "", true));
                    }
                    //如果请求的参数包含在fields中才进行处理
                    Integer index = 0;
                    for (FieldModel field : fields) {
                        if (map.containsKey(field.getName())) {
                            String key = field.getName();
                            //分别处理大于、小于、模糊查询、普通查询的请求
                            String tmpFilter = map.get(field.getName()).get(0);
                            String[] tmpFilters = tmpFilter.split(" ");
                            String[] tmpIn = tmpFilter.split(",");
                            if (tmpFilters.length > 1 && (tmpFilters[0].contains("gt") || tmpFilters[0].contains("lt") || tmpFilters[0].contains("lk") || tmpFilters[0].contains("or") || tmpFilters[0].contains("ct"))) {
                                //单独处理or查询
                                if (tmpFilters[0].equals("or")) {
                                    if (tmpFilters.length > 2) {
                                        if (tmpFilters[1].equals("gt")) {
                                            orParameters.add(String.format(" %s>?", key));
                                            index++;
                                            paramsMap.put(index, tmpFilters[2]);
                                            returnUrl += String.format("%s=gt+%s&", key, tmpFilters[2]);
                                        } else if (tmpFilters[1].equals("lt")) {
                                            orParameters.add(String.format(" %s<?", key));
                                            index++;
                                            paramsMap.put(index, tmpFilters[2]);
                                            returnUrl += String.format("%s=lt+%s&", key, tmpFilters[2]);
                                        } else if (tmpFilters[1].equals("lk")) {
                                            //parameters.add(" " + key + " like '%" + tmpFilters[1] + "%'");
                                            orParameters.add(" " + Util.getVagueSQL(key, tmpFilters[2]));
                                            returnUrl += String.format("%s=lk+%s&", key, tmpFilters[2]);
                                        } else if (tmpFilters[1].equals("ct")) {
                                            orParameters.add(" " + key + " like '%" + tmpFilters[2] + "%'");
                                            returnUrl += String.format("%s=ct+%s&", key, tmpFilters[2]);
                                        }
                                    } else if (tmpIn.length > 1) {
                                        String strIn = "(";
                                        for (String s : tmpIn) {
                                            strIn += "?,";
                                            index++;
                                            paramsMap.put(index, s);
                                        }
                                        strIn = strIn.substring(0, strIn.length() - 1) + ")";
                                        orParameters.add(String.format(" %s in %s", key, strIn));
                                        returnUrl += String.format("%s=%s&", key, tmpFilter);
                                    } else {
                                        orParameters.add(String.format(" %s=?", key));
                                        index++;
                                        paramsMap.put(index, tmpFilter);
                                        returnUrl += String.format("%s=%s&", key, tmpFilter);
                                    }

                                }
                                //非or查询的处理方式
                                else if (tmpFilters[0].equals("gt")) {
                                    parameters.add(String.format(" %s>?", key));
                                    index++;
                                    paramsMap.put(index, tmpFilters[1]);
                                    returnUrl += String.format("%s=gt+%s&", key, tmpFilters[1]);
                                } else if (tmpFilters[0].equals("lt")) {
                                    parameters.add(String.format(" %s<?", key));
                                    index++;
                                    paramsMap.put(index, tmpFilters[1]);
                                    returnUrl += String.format("%s=lt+%s&", key, tmpFilters[1]);
                                } else if (tmpFilters[0].equals("lk")) {
                                    //parameters.add(" " + key + " like '%" + tmpFilters[1] + "%'");
                                    parameters.add(" " + Util.getVagueSQL(key, tmpFilters[1]));
                                    returnUrl += String.format("%s=lk+%s&", key, tmpFilters[1]);
                                } else if (tmpFilters[0].equals("ct")) {
                                    parameters.add(" " + key + " like '%" + tmpFilters[1] + "%'");
                                    returnUrl += String.format("%s=ct+%s&", key, tmpFilters[1]);
                                }
                            } else if (tmpIn.length > 1) {
                                String strIn = "(";
                                for (String s : tmpIn) {
                                    strIn += "?,";
                                    index++;
                                    paramsMap.put(index, s);
                                }
                                strIn = strIn.substring(0, strIn.length() - 1) + ")";
                                parameters.add(String.format(" %s in %s", key, strIn));
                                returnUrl += String.format("%s=%s&", key, tmpFilter);
                            } else {
                                parameters.add(String.format(" %s=?", key));
                                index++;
                                paramsMap.put(index, tmpFilter);
                                returnUrl += String.format("%s=%s&", key, tmpFilter);
                            }
                        }
                    }
                } catch (Exception ex) {
                    msg = "配置文件读取错误:" + ex.getMessage();
                    isCollect = false;
                    statusCode = StatusCode.FAILD;
                }
                //拼接完整的sql
                if (parameters.size() != 0) {
                    sql.append(" where ");
                    for (int i = 0; i < parameters.size(); i++) {
                        String s = parameters.get(i);
                        sql.append(s);
                        if (i != (parameters.size() - 1)) {
                            sql.append(" and ");
                        }
                    }
                    if (deleteFlag != null) {
                        sql.append(String.format(" and %s=0", deleteFlag));
                    }
                }
                if (orParameters.size() != 0) {
                    sql.append(" or ");
                    for (int i = 0; i < orParameters.size(); i++) {
                        String s = orParameters.get(i);
                        sql.append(s);
                        if (i != (orParameters.size() - 1)) {
                            sql.append(" or ");
                        }
                    }
                    if (deleteFlag != null) {
                        sql.append(String.format(" and %s=0", deleteFlag));
                    }
                }
                if (parameters.size() == 0 && orParameters.size() == 0) {
                    if (deleteFlag != null) {
                        sql.append(String.format(" where %s=0", deleteFlag));
                    }
                }
            }
            if (isCollect) {
                //翻页连接相关参数
                Integer previewPage = 0;
                Integer nextPage = 0;
                Integer pageIndex = 1;
                Integer pageSize = 10;

                if (map.containsKey("pageIndex")) {
                    pageIndex = Integer.parseInt(map.get("pageIndex").get(0));
                    previewPage = pageIndex - 1;
                    nextPage = pageIndex + 1;
                }
                if (map.containsKey("pageSize")) {
                    pageSize = Integer.parseInt(map.get("pageSize").get(0));
                }

                if (map.containsKey("orderby")) {
                    String field = map.get("orderby").get(0);
                    if (field.contains("-")) {
                        sql.append(String.format(" order by %s desc", field.substring(1, field.length())));
                    } else {
                        sql.append(String.format(" order by %s asc", field));
                    }
                    returnUrl += "orderby=" + field + "&";
                }
                //如果请求参数中带有pageIndex则执行分页操作
                try {
                    if (map.containsKey("pageIndex")) {
                        //判断是否存在上下页
                        String nextSql = sql.toString() + " limit " + (((pageIndex - 1) * pageSize) + 1) + "," + pageSize;
                        ResultSet rs = jdbcHelper.executeResultSet(nextSql, paramsMap.values().toArray());
                        rs.last();
                        if (rs.getRow() > 0) {
                            resultModel.setNext_page(String.format("%s/v1.1/%s/%s?%s", pagerUrl, projectName, tableName, returnUrl + "pageIndex=" + nextPage + "&pageSize=" + pageSize));
                        }
                        if (previewPage > 0) {
                            resultModel.setPre_page(String.format("%s/v1.1/%s/%s?%s", pagerUrl, projectName, tableName, returnUrl + "pageIndex=" + previewPage + "&pageSize=" + pageSize));
                        }
                        ResultSet countRs = jdbcHelper.executeResultSet(sql.toString(), paramsMap.values().toArray());
                        Integer total_count = 0;
                        while (countRs.next()) {
                            total_count++;
                        }
                        Integer total_page = total_count / pageSize;
                        resultModel.setTotal_record(total_count.toString());
                        if (total_count % pageSize > 0) {
                            total_page++;
                        }
                        resultModel.setTotal_page(total_page.toString());

                        sql.append(" limit ").append((pageIndex - 1) * pageSize).append(",").append(pageSize);
                    }
                    ResultSet rs = jdbcHelper.executeResultSet(sql.toString(), paramsMap.values().toArray());
                    ResultSetMetaData md = rs.getMetaData();
                    int columnCount = md.getColumnCount();
                    //将得到的结果存入返回的实体类型中
                    while (rs.next()) {
                        IdentityHashMap<String, String> databaseResults = new IdentityHashMap<String, String>();
                        for (int i = 1; i < columnCount + 1; i++) {
                            if (md.getColumnName(i).equals("count(*)")) {
                                databaseResults.put("countNumber", rs.getString(i));

                            } else {
                                if (rs.getString(i) == null) {
                                    databaseResults.put(md.getColumnName(i), "");
                                } else {
                                    //单独处理TINYINT类型的数据（使用getString会读成Boolean类型的值）
                                    if (md.getColumnTypeName(i).equals("TINYINT")) {
                                        databaseResults.put(md.getColumnName(i), String.valueOf(rs.getInt(i)));
                                    } else {
                                        databaseResults.put(md.getColumnName(i), rs.getString(i));
                                    }
                                }
                            }
                        }
                        resultModel.getContent().add(databaseResults);
                    }
                    msg = "success";
                    statusCode = StatusCode.SUCCESS;
                    rs.last();
                    if (rs.getRow() < 1) {
                        statusCode = StatusCode.FAILD;
                        msg = "no result";
                    }
                } catch (Exception ex) {
                    statusCode = StatusCode.FAILD;
                    msg = "exec query error : " + ex.getMessage();
                } finally {
                    jdbcHelper.close();
                }
            }
            jdbcHelper.close();
            resultModel.setMessage(msg);
            resultModel.setCode(statusCode.toString());
        }
        kafkaProducer.produce(projectName, request.getRequestURI(), tableName, map.toString(), request.getMethod(), sql.toString(), request.getRemoteAddr(), resultModel);
        return resultModel;
    }

    /*
    * 根据ID查询单个记录
    * */
    @GET
    @Path("/{pkey}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResultModel getSingle(@PathParam("pkey") String pkey) {
        initVars();
        //请求的参数
        MultivaluedMap<String, String> map = uriInfo.getQueryParameters();
        //读取对应库表的JSON文件并转换成Map集合
        File tableFile = new File(FILEPATH + tableName);
        Map<String, Object> tableMap = Util.parseJSON2Map(Util.readToString(tableFile));
        //该库表中的主键名
        String pk = (String) tableMap.get("pk");
        //返回的结果集
        ResultModel resultModel = null;
        //返回的信息
        String msg = "";
        //返回的状态码
        StatusCode statusCode = StatusCode.SUCCESS;
        //判断是否有满足所有要求，可以执行SQL
        Boolean isCollect = true;
        //拼接sql
        StringBuilder sql = new StringBuilder();

        //------------------------查读库信息---------------------------
        sql.append("select result from tb_cache where requestUrl=? and requestTable=? and requestParams=? and requestMethod=?");
        try {
            resultModel = cacheDb.query(sql.toString()
                    , request.getRequestURL().toString().replace("'", "\\'")
                    , tableName.replace("'", "\\'")
                    , map.toString().replace("'", "\\'")
                    , request.getMethod().replace("'", "\\'"));
        } catch (SQLException e) {
            System.out.println("查询读库失败：" + e.getMessage());
            e.printStackTrace();
            System.out.println();
        }
        //如果读库中没有数据则到后台数据库中查询
        if (resultModel == null) {
            sql = new StringBuilder();
            resultModel = new ResultModel();

            //如果存在特殊的规则，那么按照特殊规则来执行
            if (map.containsKey("ruleName")) {
                //获得请求参数中rules的值，是一个整数，对应配置文件中第几个rule
                String ruleName = map.get("ruleName").get(0);
                try {
                    //获得配置文件中对应rule的详细信息
                    Map rules = Util.getRules(tableMap, ruleName);
                    if (rules == null) {
                        isCollect = false;
                        msg = "没有找到规则，请检查配置文件";
                        statusCode = StatusCode.FAILD;
                    } else {
                        //将rule拼合成SQL语句
                        sql.append("select ").append(rules.get("wantFields")).append(" from ").append(rules.get("tables")).append(" where ").append(rules.get("relationFields"));
                        sql.append(String.format(" and %s=?", pk));
                    }
                } catch (Exception ex) {
                    isCollect = false;
                    msg = "配置文件读取错误:" + ex.getMessage();
                    statusCode = StatusCode.FAILD;
                }
            }
            //执行正常查询的操作
            else {
                if (deleteFlag != null) {
                    sql.append(String.format("select * from %s where %s=0 and %s=?", tableName, deleteFlag, pk));
                } else {
                    sql.append(String.format("select * from %s where %s=?", tableName, pk));
                }
            }
            if (isCollect) {
                try {
                    /*stmt = conn.prepareStatement(sql.toString());
                    stmt.setString(1, pkey);
                    rs = stmt.executeQuery();*/
                    ResultSet rs = jdbcHelper.executeResultSet(sql.toString(), new Object[]{pkey});
                    ResultSetMetaData md = rs.getMetaData();
                    int columnCount = md.getColumnCount();
                    while (rs.next()) {
                        IdentityHashMap<String, String> databaseResults = new IdentityHashMap<String, String>();
                        for (int i = 1; i < columnCount + 1; i++) {
                            if (rs.getString(i) == null) {
                                databaseResults.put(md.getColumnName(i), "");
                            } else {
                                if (md.getColumnTypeName(i).equals("TINYINT")) {
                                    databaseResults.put(md.getColumnName(i), String.valueOf(rs.getInt(i)));
                                } else {
                                    databaseResults.put(md.getColumnName(i), rs.getString(i));
                                }
                            }
                        }
                        resultModel.getContent().add(databaseResults);
                    }
                    resultModel.setMessage("success");
                    statusCode = StatusCode.SUCCESS;
                    rs.last();
                    if (rs.getRow() < 1) {
                        statusCode = StatusCode.FAILD;
                        msg = "no result";
                    }
                } catch (Exception ex) {
                    statusCode = StatusCode.FAILD;
                    msg = "exec query error :" + ex.getMessage();
                } finally {
                    jdbcHelper.close();
                }
            }
            jdbcHelper.close();
            resultModel.setMessage(msg);
            resultModel.setCode(statusCode.toString());
        }
        kafkaProducer.produce(projectName, request.getRequestURI(), tableName, map.toString(), request.getMethod(), sql.toString(), request.getRemoteAddr(), resultModel);
        return resultModel;
    }

    /**
     * DELETE请求，根据主键删除记录
     */
    @DELETE
    @Path("/{pkey}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResultModel delete(@PathParam("pkey") String pkey) {
        initVars();
        String msg = "";
        //读取对应库表的JSON文件并转换成Map集合
        File tableFile = new File(FILEPATH + tableName);
        Map<String, Object> tableMap = Util.parseJSON2Map(Util.readToString(tableFile));
        //该库表中的主键名
        String pk = (String) tableMap.get("pk");
        ResultModel resultModel = new ResultModel();
        StatusCode statusCode;
        StringBuilder sql = new StringBuilder();
        //数据库是否使用删除标志位、检查配置文件中是否使用删除的标志位，分别进行处理
        if (deleteFlag == null || (tableMap.get("useDelFlag") != null && !((Boolean) tableMap.get("useDelFlag")))) {
            sql.append(String.format("delete from %s where %s=?", tableName, pk));
        } else {
            sql.append(String.format("update %s set %s=1 where %s=?", tableName, deleteFlag, pk));
        }
        jdbcHelper.executeUpdate(sql.toString(), pkey);
        msg = "success";
        statusCode = StatusCode.SUCCESS;
        jdbcHelper.close();
        resultModel.setMessage(msg);
        resultModel.setContent(null);
        resultModel.setCode(statusCode.toString());
        kafkaProducer.produce(projectName, request.getRequestURI(), tableName, "", request.getMethod(), sql.toString(), request.getRemoteAddr(), resultModel);
        return resultModel;
    }

    /**
     * POST请求，插入新纪录，根据配置文件中的信息进行校验，确保配置文件中POST请求的配置和数据库字段信息相对应（接受的数据为JSON字符串）
     */
    @POST
    //@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResultModel insert(String jsonStr) {
        initVars();
        ResultModel resultModel = new ResultModel();
        StatusCode statusCode = StatusCode.SUCCESS;
        //读取对应库表的JSON文件并转换成Map集合
        File tableFile = new File(FILEPATH + tableName);
        Map<String, Object> tableMap = Util.parseJSON2Map(Util.readToString(tableFile));
        //该库表中的主键名
        String pk = (String) tableMap.get("pk");
        //数据库字段集合
        ArrayList<FieldModel> fs = Util.getfields(tableMap);
        //校验配置文件中主键是否具有默认值
        Boolean isDefault = true;
        if (tableMap.get("isDefault") != null && !((Boolean) tableMap.get("isDefault"))) {
            isDefault = false;
        }
        //没有默认值的话插入时应带上主键信息
        if (!isDefault) {
            fs.add(new FieldModel(pk, "", true));
        }
        Map<String, Object> map;
        try {
            map = Util.parseJSON2Map(jsonStr);
        } catch (Exception ex) {
            map = new HashMap<String, Object>();
            MultivaluedMap<String, String> mMap = uriInfo.getQueryParameters();
            for (String s : mMap.keySet()) {
                map.put(s, mMap.get(s).get(0));
            }
        }
        //请求参数的键集合
        Iterator keyIt = map.keySet().iterator();
        HashMap<String, Object> tmpMap = new HashMap<String, Object>();
        //循环键集合，如果数据库字段中有包含这个参数信息就加入到tmpMap中
        //此步骤主要是过滤请求参数中的杂乱字段信息，最后到tmpMap中的参数都是和数据库中字段对应的
        while (keyIt.hasNext()) {
            String key = (String) keyIt.next();
            for (FieldModel fieldModel : fs) {
                if (fieldModel.getName().equals(key)) {
                    tmpMap.put(key, map.get(key));
                }
            }
        }
        //判断数据库中所必须的字段是否全部都有
        Boolean isCollect = true;
        //返回的信息
        String msg = "";
        for (FieldModel fieldModel : fs) {
            if (fieldModel.getRequired()) {
                if (!(tmpMap.containsKey(fieldModel.getName()))) {
                    msg += "数据库必要的字段没有满足:" + fieldModel.getName() + " ";
                    statusCode = StatusCode.FAILD;
                    isCollect = false;
                }
            }
        }

        if (map.size() < 1) {
            statusCode = StatusCode.FAILD;
            msg = "params error!";
            isCollect = false;
        }
        //拼接要执行的sql
        String sql = "";
        //发送到kafka具有详细信息的sql
        String kafkaSql = "";
        if (isCollect) {
            //从tmpMap中拼接要执行的sql语句
            String fields = "";
            String valueTmp = "";
            //和kafkaSql配合使用
            String kafkaValueTmp = "";
            for (Object o : tmpMap.keySet()) {
                valueTmp += "?,";
                fields += o + ",";
                kafkaValueTmp += tmpMap.get(o) + ",";
            }
            valueTmp = valueTmp.substring(0, valueTmp.length() - 1);
            fields = fields.substring(0, fields.length() - 1);
            kafkaValueTmp = kafkaValueTmp.substring(0, kafkaValueTmp.length() - 1);
            sql = String.format("insert into %s(%s) values(%s);", tableName, fields, valueTmp);
            kafkaSql = String.format("insert into %s(%s) values(%s);", tableName, fields, kafkaValueTmp);
            int res = 0;
            try {
                res = jdbcHelper.executeUpdate(sql, tmpMap.values().toArray());
                if (res < 1) {
                    statusCode = StatusCode.FAILD;
                    msg = "exec post error! no more msg";
                } else {
                    msg = "success";
                    statusCode = StatusCode.SUCCESS;
                    //根据主键是否有默认值两种情况对返回的主键值进行处理
                    if (isDefault) {
                        /*stmt = conn.prepareStatement("select " + pk + " from " + tableName + " order by " + pk + " desc limit 0,1");
                        rs = stmt.executeQuery();*/
                        ResultSet rs = jdbcHelper.executeResultSet("select " + pk + " from " + tableName + " order by " + pk + " desc limit 0,1");
                        if (rs.next()) {
                            IdentityHashMap<String, String> mapModel = new IdentityHashMap<String, String>();
                            mapModel.put(pk, rs.getString(pk));
                            resultModel.getContent().add(mapModel);
                        }
                    } else {
                        IdentityHashMap<String, String> mapModel = new IdentityHashMap<String, String>();
                        mapModel.put(pk, (String) map.get(pk));
                        resultModel.getContent().add(mapModel);
                    }
                }
                //ps.close();
            } catch (SQLException ex) {
                statusCode = StatusCode.FAILD;
                msg = "exec post error : " + ex.getMessage();
            } finally {
                jdbcHelper.close();
            }
        }
        jdbcHelper.close();
        resultModel.setMessage(msg);
        resultModel.setCode(statusCode.toString());
        kafkaProducer.produce(projectName, request.getRequestURI(), tableName, map.toString(), request.getMethod(), kafkaSql, request.getRemoteAddr(), resultModel);
        return resultModel;
    }

    /**
     * POST请求，插入新纪录，根据配置文件中的信息进行校验，确保配置文件中POST请求的配置和数据库字段信息相对应（接受的数据为标准表单参数）
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public ResultModel insert(MultivaluedMap<String, String> map) {
        initVars();
        ResultModel resultModel = new ResultModel();
        StatusCode statusCode = StatusCode.SUCCESS;
        //读取对应库表的JSON文件并转换成Map集合
        File tableFile = new File(FILEPATH + tableName);
        Map<String, Object> tableMap = Util.parseJSON2Map(Util.readToString(tableFile));
        //该库表中的主键名
        String pk = (String) tableMap.get("pk");
        //数据库字段集合
        ArrayList<FieldModel> fs = Util.getfields(tableMap);
        //校验配置文件中主键是否具有默认值
        Boolean isDefault = true;
        if (tableMap.get("isDefault") != null && !((Boolean) tableMap.get("isDefault"))) {
            isDefault = false;
        }
        //没有默认值的话插入时应带上主键信息
        if (!isDefault) {
            fs.add(new FieldModel(pk, "", true));
        }
        //请求参数的键集合
        Iterator keyIt = map.keySet().iterator();
        HashMap<String, Object> tmpMap = new HashMap<String, Object>();
        //循环键集合，如果数据库字段中有包含这个参数信息就加入到tmpMap中
        //此步骤主要是过滤请求参数中的杂乱字段信息，最后到tmpMap中的参数都是和数据库中字段对应的
        while (keyIt.hasNext()) {
            String key = (String) keyIt.next();
            for (FieldModel fieldModel : fs) {
                if (fieldModel.getName().equals(key)) {
                    tmpMap.put(key, map.get(key).get(0));
                }
            }
        }
        //判断数据库中所必须的字段是否全部都有
        Boolean isCollect = true;
        //返回的信息
        String msg = "";
        for (FieldModel fieldModel : fs) {
            if (fieldModel.getRequired()) {
                if (!(tmpMap.containsKey(fieldModel.getName()))) {
                    msg += "数据库必要的字段没有满足:" + fieldModel.getName() + " ";
                    statusCode = StatusCode.FAILD;
                    isCollect = false;
                }
            }
        }

        if (map.size() < 1) {
            statusCode = StatusCode.FAILD;
            msg = "params error!";
            isCollect = false;
        }
        String sql = "";
        //发送到kafka具有详细信息的sql
        String kafkaSql = "";
        if (isCollect) {
            //从tmpMap中拼接要执行的sql语句
            String fields = "";
            String valueTmp = "";
            //和kafkaSql配合使用
            String kafkaValueTmp = "";
            for (Object o : tmpMap.keySet()) {
                valueTmp += "?,";
                fields += o + ",";
                kafkaValueTmp += tmpMap.get(o) + ",";
            }
            valueTmp = valueTmp.substring(0, valueTmp.length() - 1);
            fields = fields.substring(0, fields.length() - 1);
            kafkaValueTmp = kafkaValueTmp.substring(0, kafkaValueTmp.length() - 1);
            sql = String.format("insert into %s(%s) values(%s);", tableName, fields, valueTmp);
            kafkaSql = String.format("insert into %s(%s) values(%s);", tableName, fields, kafkaValueTmp);
            int res = 0;
            try {
                res = jdbcHelper.executeUpdate(sql, tmpMap.values().toArray());
                if (res < 1) {
                    statusCode = StatusCode.FAILD;
                    msg = "exec post error! no more msg";
                } else {
                    msg = "success";
                    statusCode = StatusCode.SUCCESS;
                    //根据主键是否有默认值两种情况对返回的主键值进行处理
                    if (isDefault) {
                        ResultSet rs = jdbcHelper.executeResultSet("select " + pk + " from " + tableName + " order by " + pk + " desc limit 0,1");
                        if (rs.next()) {
                            IdentityHashMap<String, String> mapModel = new IdentityHashMap<String, String>();
                            mapModel.put(pk, rs.getString(pk));
                            resultModel.getContent().add(mapModel);
                        }
                    } else {
                        IdentityHashMap<String, String> mapModel = new IdentityHashMap<String, String>();
                        mapModel.put(pk, map.get(pk).get(0));
                        resultModel.getContent().add(mapModel);
                    }
                }
            } catch (SQLException ex) {
                statusCode = StatusCode.FAILD;
                msg = "exec post error : " + ex.getMessage();
            } finally {
                jdbcHelper.close();
            }
        }
        jdbcHelper.close();
        resultModel.setMessage(msg);
        resultModel.setCode(statusCode.toString());
        kafkaProducer.produce(projectName, request.getRequestURI(), tableName, map.toString(), request.getMethod(), kafkaSql, request.getRemoteAddr(), resultModel);
        return resultModel;
    }

    /**
     * PUT请求，将提交的参数值更新到数据库中对应的字段（接受的数据为JSON字符串）
     */
    @PUT
    @Path("/{pkey}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResultModel update(@PathParam("pkey") String pkValue, String jsonStr) {
        initVars();
        //读取对应库表的JSON文件并转换成Map集合
        File tableFile = new File(FILEPATH + tableName);
        Map<String, Object> tableMap = Util.parseJSON2Map(Util.readToString(tableFile));
        //该库表中的主键名
        String pk = (String) tableMap.get("pk");
        Map<String, Object> map;
        try {
            map = Util.parseJSON2Map(jsonStr);
        } catch (Exception ex) {
            map = new HashMap<String, Object>();
            MultivaluedMap<String, String> mMap = uriInfo.getQueryParameters();
            for (String s : mMap.keySet()) {
                map.put(s, mMap.get(s).get(0));
            }
        }
        String sql = String.format("update %s set ", tableName);
        //发送到kafka具有详细信息的sql
        String kafkaSql = String.format("update %s set ", tableName);
        ResultModel resultModel = new ResultModel();
        String msg = "";
        StatusCode statusCode = StatusCode.SUCCESS;
        if (map.size() < 1) {
            statusCode = StatusCode.FAILD;
            msg = "params error!";
        } else {
            for (String key : map.keySet()) {
                sql += key + "=?,";
                kafkaSql += key + "=" + map.get(key) + ",";
            }
            sql = sql.substring(0, sql.length() - 1);
            sql += String.format(" where %s=?", pk);

            kafkaSql = kafkaSql.substring(0, kafkaSql.length() - 1);
            kafkaSql += String.format(" where %s=" + pkValue, pk);

            try {
                int res = jdbcHelper.executeUpdate(sql, pkValue, map.values().toArray());
                if (res > 0) {
                    msg = "success";
                    statusCode = StatusCode.SUCCESS;
                } else {
                    msg = "没有此记录！";
                    statusCode = StatusCode.FAILD;
                }
            } catch (Exception ex) {
                msg = "exec put error : " + ex.getMessage();
                statusCode = StatusCode.FAILD;
            } finally {
                jdbcHelper.close();
            }
        }
        jdbcHelper.close();
        resultModel.setMessage(msg);
        resultModel.setCode(statusCode.toString());
        resultModel.setContent(null);
        kafkaProducer.produce(projectName, request.getRequestURI(), tableName, map.toString(), request.getMethod(), kafkaSql, request.getRemoteAddr(), resultModel);
        return resultModel;
    }

    /**
     * PUT请求，将提交的参数值更新到数据库中对应的字段（接受的数据为标准表单参数）
     */
    @PUT
    @Path("/{pkey}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public ResultModel update(@PathParam("pkey") String pkValue, MultivaluedMap<String, String> map) {
        initVars();
        //读取对应库表的JSON文件并转换成Map集合
        File tableFile = new File(FILEPATH + tableName);
        Map<String, Object> tableMap = Util.parseJSON2Map(Util.readToString(tableFile));
        //该库表中的主键名
        String pk = (String) tableMap.get("pk");
        String sql = String.format("update %s set ", tableName);
        //发送到kafka具有详细信息的sql
        String kafkaSql = String.format("update %s set ", tableName);
        ResultModel resultModel = new ResultModel();
        String msg = "";
        StatusCode statusCode = StatusCode.SUCCESS;
        if (map.size() < 1) {
            statusCode = StatusCode.FAILD;
            msg = "params error!";
        } else {
            for (String key : map.keySet()) {
                sql += key + "=?,";
                kafkaSql += key + "=" + map.get(key).get(0) + ",";
            }

            sql = sql.substring(0, sql.length() - 1);
            sql += String.format(" where %s=?", pk);

            kafkaSql = kafkaSql.substring(0, kafkaSql.length() - 1);
            kafkaSql += String.format(" where %s=" + pkValue, pk);

            try {
                int res = jdbcHelper.executeUpdate(sql, pkValue, map.values().toArray());
                if (res > 0) {
                    msg = "success";
                    statusCode = StatusCode.SUCCESS;
                } else {
                    msg = "没有此记录！";
                    statusCode = StatusCode.FAILD;
                }
            } catch (Exception ex) {
                msg = "exec put error : " + ex.getMessage();
                statusCode = StatusCode.FAILD;
            } finally {
                jdbcHelper.close();
            }
        }
        jdbcHelper.close();
        resultModel.setMessage(msg);
        resultModel.setCode(statusCode.toString());
        resultModel.setContent(null);
        kafkaProducer.produce(projectName, request.getRequestURI(), tableName, map.toString(), request.getMethod(), kafkaSql, request.getRemoteAddr(), resultModel);
        return resultModel;
    }

    /**
     * 初始化一些运行时需要的变量，文件读取方式待改进
     */
    private void initVars() {
        //获得当前项目下的classes路径，如本地测试为：/Users/jchubby/Documents/IdeaProjects/omniREST/out/artifacts/omniREST_war_exploded/WEB-INF/classes/
        String path = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        //request.getRequestURI()格式为/projectName/tableName，使用/分割之后，索引为1的值就是所需的项目名
        String[] paths = request.getRequestURI().split("/");
        String version = paths[1].replace(".", "_");
        projectName = paths[2];

        if (!projectName.equals(lastProject) || lastProject.equals("")) {
            synchronized (this) {
                FILEPATH = path + version + "/" + projectName + "/";
                Map<String, Object> docsMap = Util.parseJSON2Map(Util.readToString(new File(FILEPATH + "docs")));
                username = (String) docsMap.get("username");
                password = (String) docsMap.get("password");
                jdbc = (String) docsMap.get("jdbc");
                deleteFlag = (String) docsMap.get("deleteFlag");
                pagerUrl = (String) docsMap.get("pagerUrl");
            }
        }
        lastProject = projectName;
        jdbcHelper = new JDBCHelper(jdbc, username, password);
    }
}