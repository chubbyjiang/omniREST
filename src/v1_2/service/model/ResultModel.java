package v1_2.service.model;

import net.sf.json.JSONArray;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.IdentityHashMap;

/**
 * Created by JChubby on 2015/7/20.
 */

@XmlRootElement
public class ResultModel {
    private String code;
    private String message;
    private ArrayList<IdentityHashMap<String,String>> content = new ArrayList<IdentityHashMap<String,String>>();
    private String pre_page;
    private String next_page;
    private String total_page;
    private String total_record;

    public ResultModel() {
    }

    public ResultModel(String code, String message, ArrayList<IdentityHashMap<String, String>> content, String pre_page, String next_page, String total_page, String total_record) {
        this.code = code;
        this.message = message;
        this.content = content;
        this.pre_page = pre_page;
        this.next_page = next_page;
        this.total_page = total_page;
        this.total_record = total_record;
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

    public ArrayList<IdentityHashMap<String, String>> getContent() {
        return content;
    }

    public void setContent(ArrayList<IdentityHashMap<String, String>> content) {
        this.content = content;
    }

    public String getPre_page() {
        return pre_page;
    }

    public void setPre_page(String pre_page) {
        this.pre_page = pre_page;
    }

    public String getNext_page() {
        return next_page;
    }

    public void setNext_page(String next_page) {
        this.next_page = next_page;
    }

    public String getTotal_page() {
        return total_page;
    }

    public void setTotal_page(String total_page) {
        this.total_page = total_page;
    }

    public String getTotal_record() {
        return total_record;
    }

    public void setTotal_record(String total_record) {
        this.total_record = total_record;
    }

    @Override
    public String toString() {
        /*String json = JSONArray.fromObject(this).toString();
        json = json.substring(1, json.length() - 1);
        return json;*/
        return JSONArray.fromObject(this).get(0).toString();
    }
}
