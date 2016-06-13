package data;

import java.io.Serializable;

//서버와 안드로이드 간에 OBJECT형 객체로 통신함.
public class OBJECT implements Serializable {
    private static final long serialVersionUID = 1L;

    private short Message = -1;
    private Object[] object;

    public OBJECT(short Message, Object... obj) {
        this.Message = Message;
        this.object = obj;
    }

    public short getMessage() {
        return Message;
    }

    public void setMessage(short Message) {
        this.Message = Message;
    }

    public Object getObject(int position){
        return this.object[position];
    }

    public void setObject(Object[] object) {
        this.object = object;
    }
}