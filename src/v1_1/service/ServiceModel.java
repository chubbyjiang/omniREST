package v1_1.service;

import v1_1.service.model.FieldModel;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jchubby on 15/9/10.
 */
public class ServiceModel {
    private Boolean hasJoin;
    private StringBuilder sql;
    private Map rules;
    private MultivaluedMap<String, String> map;
    private String returnUrl;
    private String ruleName;
    private List<String> orParameters;
    private List<String> parameters;
    private String[] values;
    private Boolean hasInnerSql;
    private Boolean hasWhere;
    private StringBuilder innerSql;
    private FieldModel field;
    private String key;
    private String value;

    public Boolean getHasJoin() {
        return hasJoin;
    }

    public void setHasJoin(Boolean hasJoin) {
        this.hasJoin = hasJoin;
    }

    public StringBuilder getSql() {
        return sql;
    }

    public void setSql(StringBuilder sql) {
        this.sql = sql;
    }

    public Map getRules() {
        return rules;
    }

    public void setRules(Map rules) {
        this.rules = rules;
    }

    public MultivaluedMap<String, String> getMap() {
        return map;
    }

    public void setMap(MultivaluedMap<String, String> map) {
        this.map = map;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public List<String> getOrParameters() {
        return orParameters;
    }

    public void setOrParameters(List<String> orParameters) {
        this.orParameters = orParameters;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public String[] getValues() {
        return values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }

    public Boolean getHasInnerSql() {
        return hasInnerSql;
    }

    public void setHasInnerSql(Boolean hasInnerSql) {
        this.hasInnerSql = hasInnerSql;
    }

    public Boolean getHasWhere() {
        return hasWhere;
    }

    public void setHasWhere(Boolean hasWhere) {
        this.hasWhere = hasWhere;
    }

    public StringBuilder getInnerSql() {
        return innerSql;
    }

    public void setInnerSql(StringBuilder innerSql) {
        this.innerSql = innerSql;
    }

    public FieldModel getField() {
        return field;
    }

    public void setField(FieldModel field) {
        this.field = field;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
