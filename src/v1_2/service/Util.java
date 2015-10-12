package v1_2.service;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.collections.map.ListOrderedMap;
import v1_2.service.model.FieldModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by JChubby on 2015/7/30.
 */
public class Util {

    public static String getStringRes(Map jsonMap, String... keys) {
        Map innerMap = jsonMap;
        ArrayList innerList = new ArrayList();
        String res = "no result";
        for (int i = 0; i < keys.length; i++) {
            if (innerMap.get(keys[i]) != null) {
                if (innerMap.get(keys[i]) instanceof ArrayList) {
                    innerList = (ArrayList) innerMap.get(keys[i]);
                    innerMap = (Map) innerList.get(0);
                } else if (innerMap.get(keys[i]) instanceof Map) {
                    innerMap = (Map) innerMap.get(keys[i]);
                } else {
                    res = (String) innerMap.get(keys[i]);
                    break;
                }
            } else {
                break;
            }
        }
        return res;
    }

    public static ArrayList<String> getDefaultFields(Map<String, Object> tableMap, String... keys) {
        ArrayList list = getArrayList(tableMap, keys);
        ArrayList<String> fields = new ArrayList<String>();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof Map) {
                Map m = (Map) list.get(i);
                if ((Boolean) m.get("required")) {
                    fields.add((String) m.get("name"));
                }
            }
        }
        return fields;
    }

    /**
     * 读取指定file文件，输出其全部内容
     *
     * @param file 指定的文件
     * @return String
     */
    public static String readToString(File file) {
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(filecontent, Charset.forName("utf-8"));
    }

    /**
     * 将JSON字符串转换成List集合
     *
     * @param jsonStr 要进行转换的JSON字符串
     * @return List<Map<String, Object>>
     */
    public static List<Map<String, Object>> parseJSON2List(String jsonStr) {
        JSONArray jsonArr = JSONArray.fromObject(jsonStr);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Iterator<JSONObject> it = jsonArr.iterator();
        while (it.hasNext()) {
            JSONObject json2 = it.next();
            list.add(parseJSON2Map(json2.toString()));
        }
        return list;
    }

    /**
     * 将JSON字符串转换成Map集合
     *
     * @param jsonStr 要进行转换的JSON字符串
     * @return Map<String, Object>
     */
    public static Map<String, Object> parseJSON2Map(String jsonStr) {
        ListOrderedMap map = new ListOrderedMap();
        JSONObject json = JSONObject.fromObject(jsonStr);
        for (Object k : json.keySet()) {
            Object v = json.get(k);
            if (v instanceof JSONArray) {
                List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                Iterator<JSONObject> it = ((JSONArray) v).iterator();
                while (it.hasNext()) {
                    JSONObject json2 = it.next();
                    list.add(parseJSON2Map(json2.toString()));
                }
                map.put(k.toString(), list);
            } else {
                map.put(k.toString(), v);
            }
        }
        return map;
    }

    /**
     * 从指定的配置文件（已传唤成Map对象）中，根据keys参数路径得到对应的集合
     *
     * @param jsonMap 转换成Map对象的对应表的配置文件
     * @param keys    路径参数数组
     */
    public static ArrayList getArrayList(Map jsonMap, String... keys) {
        Map innerMap = jsonMap;
        ArrayList innerList = null;
        //从路径参数0开始进行数据搜寻，目标是找出keys最后一个值对应的数据集合
        for (int i = 0; i < keys.length; i++) {
            //最外层搜索，如果存在则继续搜索
            if (innerMap.get(keys[i]) != null) {
                //如果得到的是一个ArrayList，取该集合的第一个数据继续搜索
                if (innerMap.get(keys[i]) instanceof ArrayList) {
                    innerList = (ArrayList) innerMap.get(keys[i]);
                    innerMap = (Map) innerList.get(0);
                }
                //如果得到的是一个Map，则将其赋值给innerMap
                else if (innerMap.get(keys[i]) instanceof Map) {
                    innerMap = (Map) innerMap.get(keys[i]);
                }
            } else {
                break;
            }
        }
        //当keys循环完毕之后，innerList中的数据就是所需要的数据集合
        return innerList;
    }

    /**
     * 从指定的配置文件（已传唤成Map对象）中提取出库表的字段信息
     *
     * @param tableMap 转换成Map对象的对应表的配置文件
     * @return ArrayList<FieldModel>
     */
    public static ArrayList<FieldModel> getfields(Map<String, Object> tableMap) {
        //获取的是第一个POST请求中配置的parameters，其中各个参数的设置和数据库字段信息保持一致
        ArrayList list = getArrayList(tableMap, "apis", "operations", "parameters");
        ArrayList<FieldModel> fields = new ArrayList<FieldModel>();
        //一个parameter中包含字段的各个属性描述，这里只取其字段名
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof Map) {
                Map m = (Map) list.get(i);
                fields.add(new FieldModel((String) m.get("name"), (String) m.get("dataType"), (Boolean) m.get("required")));
            }
        }
        return fields;
    }


    /**
     * 从指定的配置文件（已转换为Map对象）中提取出指定的rule
     *
     * @param tableMap 转换成Map对象的对应表的配置文件
     * @param ruleName 指定的规则的名字
     * @return Map
     */
    public static Map getRules(Map<String, Object> tableMap, String ruleName) {
        //从配置文件中提取出apis转成ArrayList
        Object rules = tableMap.get("rules");
        ArrayList ruleList;
        Map ruleMap = null;
        if (rules instanceof ArrayList) {
            ruleList = (ArrayList) rules;
            for (Object rule : ruleList) {
                Map m = (Map) rule;
                if (m.get("ruleName").equals(ruleName)) {
                    ruleMap = m;
                    break;
                }
            }
        } else {
            ruleMap = (Map) rules;
        }
        return ruleMap;
    }

    /**
     * 本操作类根据传入的要查询的字段  objectName 及 待传入的模糊参数 返回一个模糊的拼接SQL语句字符串
     *
     * @param objectName 要比较摸模糊字段（及要比较的对象名或者字段名）
     *                   例如查询用户的姓名：user.name --这就是 objectName
     * @param vague      待拼接的模糊字符串
     * @return 返回拼接后的模糊字符串
     * @author 刘胜
     */
    public static String getVagueSQL(String objectName, String vague) {
        String vagueSql;//返回的模糊SQL语句
        int vagueLength = vague.length();//获取模糊字段的长度
        if (vagueLength == 0) {
            return "";
        }
        int arrVagueLength = 1;//默认模糊查询的数组长度为1
        for (int i = 0; i < vagueLength; i++) {
            arrVagueLength = arrVagueLength * 2;//模糊数组的长度（统计为2的n次方-1）
        }
        arrVagueLength = arrVagueLength - 1;
        String[] arrVague = new String[arrVagueLength];//定义保存模糊字段的数组
        int k = 1;//用户寻找模糊数组的下标

        String cacheVague = "";
        arrVague[0] = vague;//给模糊数组的第一个赋值为 vague
        int v = 0;//字符串间接截取
        while (v < vagueLength) {
            String vagueTemp = vague;
            if (v >= 1) {
                vagueTemp = vague.substring(0, v - 1) + vague.substring(v);
            }
            int i = 0;//定义总循环的条件字段
            while (i <= vagueTemp.length()) {
                int j = 0;
                while (j <= vagueTemp.length()) {
                    boolean isExist = false;//定义当前截取的字符串是否已经存在，默认不存在
                    if (i == j) {
                    } else if (i > j) {
                        cacheVague = vagueTemp.substring(j, i);
                    } else {
                        cacheVague = vagueTemp.substring(i, j);
                    }
                    //判断截取字符串是否已经存在
                    for (int m = 0; m < k; m++) {
                        if (cacheVague.equals(arrVague[m])) {
                            isExist = true;
                            break;
                        }
                    }
                    if (!isExist && !cacheVague.equals("") && cacheVague != null) {
                        arrVague[k++] = cacheVague;
                    }
                    j++;
                }
                i++;
            }
            v++;
        }
        //进行字符串从长到短的排序
        shellSort(arrVague, k);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < k; i++) {

            if (i == k - 1) {
                sb.append(objectName + " like '%" + arrVague[i] + "%'");
            } else {
                sb.append(objectName + " like '%" + arrVague[i] + "%' or ");
            }
        }
        vagueSql = new String(sb);
        return vagueSql;
    }

    //希尔排序算法(对字符串的长度从大到小排序)
    public static void shellSort(String[] array, int len) {
        int d = len;
        while (d > 1) {
            d = (d + 1) / 2;
            for (int i = 0; i < len - d; i++) {
                if (array[i + d].length() > array[i].length()) {
                    String temp = array[i + d];
                    array[i + d] = array[i];
                    array[i] = temp;
                }
            }
        }
    }
}
