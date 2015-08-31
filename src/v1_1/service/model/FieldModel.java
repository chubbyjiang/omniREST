package v1_1.service.model;

/**
 * Created by jchubby on 15/8/4.
 */
public class FieldModel {
    private String name;
    private String type;
    private Boolean required;

    public FieldModel() {
    }

    public FieldModel(String name, String type, Boolean required) {
        this.name = name;
        this.type = type;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }
}
