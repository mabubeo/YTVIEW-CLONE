package windstudy.com.ytwind.eventbus;

import java.util.ArrayList;

public class ActionEvent<T> {
    int type;
    T data;

    public ActionEvent(int type, T data) {
        this.type = type;
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
