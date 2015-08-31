package v1.service.model;

import net.sf.json.JSONArray;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * Created by JChubby on 2015/7/20.
 */

@XmlRootElement
public class ResultModel {
    private String code;
    private String message;
    private ArrayList<MapModel> content = new ArrayList<MapModel>();
    private String previewPage;
    private String nextPage;

    public ResultModel() {
    }

    public ResultModel(String code, String message, ArrayList<MapModel> content, String previewPage, String nextPage) {
        this.code = code;
        this.message = message;
        this.content = content;
        this.previewPage = previewPage;
        this.nextPage = nextPage;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ArrayList<MapModel> getContent() {
        return content;
    }

    public void setContent(ArrayList<MapModel> content) {
        this.content = content;
    }

    public String getPreviewPage() {
        return previewPage;
    }

    public void setPreviewPage(String previewPage) {
        this.previewPage = previewPage;
    }

    public String getNextPage() {
        return nextPage;
    }

    public void setNextPage(String nextPage) {
        this.nextPage = nextPage;
    }

    @Override
    public String toString() {
        /*String json = JSONArray.fromObject(this).toString();
        json = json.substring(1, json.length() - 1);
        return json;*/
        return JSONArray.fromObject(this).get(0).toString();
    }
}
