package me.zzhen.bt.dht.meta;

import javax.xml.crypto.Data;

/**
 * Project:CleanBT
 * Create Time: 17-6-18.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public enum MetadataState {
    START,
    TO_SHAKEN,//发送完握手信息
    SHAKEN,// 接收，并并验证握手信息
    TO_EXT_SHAKEN,//发出扩展握手信息
    EXT_SHAKEN,//验证扩展握手信息
    DATA,//正在接收数据
    END;

    public MetadataState next() {
        switch (this) {
            case START:
                return TO_SHAKEN;
            case TO_SHAKEN:
                return SHAKEN;
            case SHAKEN:
                return TO_EXT_SHAKEN;
            case TO_EXT_SHAKEN:
                return EXT_SHAKEN;
            case EXT_SHAKEN:
                return DATA;
            case DATA:
                return END;
            case END:
                return END;
            default:
                throw new IllegalArgumentException("wrong state");
        }
    }
}
