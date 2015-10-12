package v1_2.service.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by JChubby on 2015/7/17.
 */
@XmlRootElement
public enum StatusCode {

    /*SUCCESS(200), //  - �ɹ�������

    BAD_REQUEST(400),//    - �����ʽ����

    NOT_FOUND(404);//  - �������Դ������*/
    SUCCESS(0),
    FAILD(-1);

    private int code;

    private StatusCode(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return String.valueOf(this.code);
    }
}
