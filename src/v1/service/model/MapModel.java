package v1.service.model;



import net.sf.json.JSONArray;

import java.util.IdentityHashMap;

/**
 * Created by JChubby on 2015/7/20.
 */
public class MapModel {
    public MapModel() {
    }

    private IdentityHashMap<String,String> result = new IdentityHashMap<String,String>();


    public MapModel(IdentityHashMap<String, String> result) {
        this.result = result;
    }

    public IdentityHashMap<String, String> getResult() {
        return result;
    }

    public void setResult(IdentityHashMap<String, String> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return JSONArray.fromObject(this).get(0).toString();
    }
}

