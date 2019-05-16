package com.vate;

import clover.org.apache.commons.lang.StringUtils;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.UnsupportedEncodingException;

@Component
public class EncryptAndDecryptUtil {

    private String encryptKey;

    /**
     * 加密数据
     * @param content
     * @return
     */
    public String encryptContentForRqpanda(String content){
        SymmetricCrypto des = new SymmetricCrypto(SymmetricAlgorithm.DESede,getKey());
        return des.encryptHex(content);
    }

    /**
     * 解密数据
     * @param content
     * @return
     */
    public String decryptContentForRqpanda(String content){
        SymmetricCrypto des = new SymmetricCrypto(SymmetricAlgorithm.DESede,getKey());
        return des.decryptStr(content);
    }

    public byte[] getKey(){
        byte[] key = null;
        if (StringUtils.isNotBlank(encryptKey)){
            try {
                key = encryptKey.getBytes("utf8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }else {
            throw new NullPointerException("获取加密秘钥失败！秘钥字符串为null!");
        }
        return key;
    }



    public void setEncryptKey(String encryptKey) {
        this.encryptKey = encryptKey;
    }

    public String getEncryptKey() {
        return encryptKey;
    }
}
