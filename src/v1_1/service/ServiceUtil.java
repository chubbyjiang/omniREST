package v1_1.service;

import v1_1.service.model.FieldModel;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jchubby on 15/9/10.
 */
public class ServiceUtil {
    public static ServiceModel rule(ServiceModel serviceModel) {
        StringBuilder sql = serviceModel.getSql();
        Map rules = serviceModel.getRules();
        sql.append("select ").append(rules.get("wantFields")).append(" from ").append(rules.get("tables"));
        serviceModel.setSql(sql);
        return serviceModel;
    }

    public static ServiceModel join(ServiceModel serviceModel) {
        StringBuilder sql = serviceModel.getSql();
        Map rules = serviceModel.getRules();
        MultivaluedMap<String, String> map = serviceModel.getMap();
        Boolean hasJoin = serviceModel.getHasJoin();
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
        serviceModel.setSql(sql);
        serviceModel.setHasJoin(hasJoin);
        return serviceModel;
    }

    public static ServiceModel inner(ServiceModel serviceModel) {
        StringBuilder sql = serviceModel.getSql();
        Map rules = serviceModel.getRules();
        //判断是否有内嵌的sql语句存在
        StringBuilder innerSql = serviceModel.getInnerSql();
        Boolean hasInnerSql = serviceModel.getHasInnerSql();
        Boolean hasWhere = serviceModel.getHasWhere();
        String returnUrl = serviceModel.getReturnUrl();
        String ruleName = serviceModel.getRuleName();
        //拼接内嵌的sql
        if (rules.get("inner") != null) {
            hasInnerSql = true;
            innerSql.append(" in ").append("(").append(rules.get("inner"));
        }
        returnUrl += "rules=" + ruleName + "&";
        serviceModel.setSql(sql);
        serviceModel.setInnerSql(innerSql);
        serviceModel.setHasInnerSql(hasInnerSql);
        serviceModel.setHasWhere(hasWhere);
        serviceModel.setHasWhere(hasWhere);
        serviceModel.setReturnUrl(returnUrl);
        return serviceModel;
    }

    public static ServiceModel or(ServiceModel serviceModel) {
        String[] values = serviceModel.getValues();
        List<String> orParameters = serviceModel.getOrParameters();
        String returnUrl = serviceModel.getReturnUrl();
        Boolean hasInnerSql = serviceModel.getHasInnerSql();
        Boolean hasWhere = serviceModel.getHasWhere();
        StringBuilder innerSql = serviceModel.getInnerSql();
        FieldModel field = serviceModel.getField();
        String key = serviceModel.getKey();
        String value = serviceModel.getValue();
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
        serviceModel.setInnerSql(innerSql);
        serviceModel.setHasWhere(hasWhere);
        serviceModel.setReturnUrl(returnUrl);
        return serviceModel;
    }

    public static ServiceModel notor(ServiceModel serviceModel) {
        String[] values = serviceModel.getValues();
        List<String> parameters = serviceModel.getParameters();
        String returnUrl = serviceModel.getReturnUrl();
        Boolean hasInnerSql = serviceModel.getHasInnerSql();
        Boolean hasWhere = serviceModel.getHasWhere();
        StringBuilder innerSql = serviceModel.getInnerSql();
        FieldModel field = serviceModel.getField();
        String key = serviceModel.getKey();
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
        serviceModel.setReturnUrl(returnUrl);
        serviceModel.setHasWhere(hasWhere);
        serviceModel.setInnerSql(innerSql);
        return serviceModel;
    }
}
