package v1_1.service;

import v1.service.model.FieldModel;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jchubby on 15/9/10.
 */
public class ServiceUtil {
    public static void rule(StringBuilder sql, Map rules) {
        sql.append("select ").append(rules.get("wantFields")).append(" from ").append(rules.get("tables"));

    }

    public static void join(StringBuilder sql, Map rules, MultivaluedMap<String, String> map) {
        Boolean hasJoin = false;
        String relationFields = rules.get("relationFields").toString();
        for (String param : map.keySet()) {
            if (relationFields.contains("$" + param)) {
                relationFields = relationFields.replace("$" + param, map.get(param).get(0));
            }
        }
        if (rules.get("join") != null) {
            hasJoin = true;
            sql.append(" ").append(rules.get("join")).append(" where ").append(relationFields);
        } else {
            sql.append(" where ").append(relationFields);
        }
    }

    public static void inner(Map rules, String returnUrl, String ruleName) {
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
    }

    public static void or(String[] values, List<String> orParameters, String returnUrl, Boolean hasInnerSql, Boolean hasWhere, StringBuilder innerSql, FieldModel field, String key, String value) {
        if (values.length > 2) {
            if (values[1].equals("gt")) {
                orParameters.add(String.format(" %s>%s", key, values[2]));
                returnUrl += String.format("%s=gt+%s&", field.getName(), values[2]);
            } else if (values[1].equals("lt")) {
                orParameters.add(String.format(" %s<%s", key, values[2]));
                returnUrl += String.format("%s=lt+%s&", field.getName(), values[2]);
            } else if (values[1].equals("nq")) {
                orParameters.add(String.format(" %s<>%s", key, values[2]));
                returnUrl += String.format("%s=nq+%s&", field.getName(), values[2]);
            } else if (values[1].equals("lk")) {
                orParameters.add(" " + Util.getVagueSQL(key, values[2]));
                returnUrl += String.format("%s=lk+%s&", key, values[1]);
            } else if (values[1].equals("ct")) {
                orParameters.add(" " + key + " like '%" + values[2] + "%'");
                returnUrl += String.format("%s=ct+%s&", key, values[2]);
            } else if (values[1].equals("inner")) {
                if (hasInnerSql) {
                    if (!hasWhere) {
                        innerSql.append(" where ");
                        hasWhere = true;
                    }
                    innerSql.append(String.format(" %s=%s", key, values[1]));
                }
            }
        } else {
            orParameters.add(String.format(" %s=%s", key, values[1]));
            returnUrl += String.format("%s=%s&", key, value);
        }
    }

    public static void notor(String[] values, List<String> parameters, String returnUrl, Boolean hasInnerSql, Boolean hasWhere, StringBuilder innerSql, FieldModel field, String key, String value) {
        if (values[0].equals("gt")) {
            parameters.add(String.format(" %s>%s", key, values[1]));
            returnUrl += String.format("%s=gt+%s&", field.getName(), values[1]);
        } else if (values[0].equals("lt")) {
            parameters.add(String.format(" %s<%s", key, values[1]));
            returnUrl += String.format("%s=lt+%s&", field.getName(), values[1]);
        } else if (values[0].equals("nq")) {
            parameters.add(String.format(" %s<>%s", key, values[1]));
            returnUrl += String.format("%s=nq+%s&", field.getName(), values[1]);
        } else if (values[0].equals("lk")) {
            parameters.add(" " + Util.getVagueSQL(key, values[1]));
            returnUrl += String.format("%s=lk+%s&", key, values[1]);
        } else if (values[0].equals("ct")) {
            parameters.add(" " + key + " like '%" + values[1] + "%'");
            returnUrl += String.format("%s=ct+%s&", key, values[1]);
        } else if (values[0].equals("inner")) {
            if (hasInnerSql) {
                if (!hasWhere) {
                    innerSql.append(" where ");
                    hasWhere = true;
                }
                innerSql.append(String.format(" %s=%s", key, values[1]));
            }
        }
    }
}
