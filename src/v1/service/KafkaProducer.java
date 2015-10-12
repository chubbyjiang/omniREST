package v1.service;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import v1.service.model.ResultModel;

import java.util.Properties;

/**
 * Created by jchubby on 15/8/6.
 */
public class KafkaProducer {

    private static Producer<String, String> producer;
    public final static String TOPIC = "omni";

    public KafkaProducer() {
        Properties props = new Properties();
        //此处配置的是kafka的端口
        props.put("metadata.broker.list", "amazontest:9092");
        //配置value的序列化类
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        //配置key的序列化类
        props.put("key.serializer.class", "kafka.serializer.StringEncoder");
        props.put("request.required.acks", "-1");
        producer = new Producer<String, String>(new ProducerConfig(props));
    }

    public void produce(String projectName, String requestUrl, String requestTable, String requestParams, String requestMethod, String sql, String remoteAddr, String resultCode, String timespan) {
        String msg = String.format(
                "{" +
                        "\"projectName\":\"%s\"" +
                        ",\"requestUrl\":\"%s\"" +
                        ",\"requestTable\":\"%s\"" +
                        ",\"requestParams\":\"%s\"" +
                        ",\"requestMethod\":\"%s\"" +
                        ",\"sql\":\"%s\"" +
                        ",\"remoteAddr\":\"%s\"" +
                        ",\"resultCode\":\"%s\"" +
                        ",\"timespan\":\"%s\"" +
                        "}"
                , projectName
                , requestUrl
                , requestTable
                , requestParams
                , requestMethod
                , sql
                , remoteAddr
                , resultCode
                , timespan);
        producer.send(new KeyedMessage<String, String>(TOPIC, msg));
    }
}
