package v1.service.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by JChubby on 2015/7/17.
 */
@XmlRootElement
public class ParameterModel {
    private String name;
    private String value;
    private String type;

    public ParameterModel() {
    }

    public ParameterModel(String name, String value, String type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
