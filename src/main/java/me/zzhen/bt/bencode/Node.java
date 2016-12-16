package me.zzhen.bt.bencode;

/**
 * /**
 * Project:CleanBT
 *
 * @author zzhen zzzhen1994@gmail.com
 *         Create Time: 2016/10/16.
 *         Version :
 *         Description:
 */
public interface Node {

    /**
     * @return 返回最基本的byte[] 不用受编码影响
     */
    byte[] encode();

    /**
     * 好像没什么用，看看以后有什么用
     *
     * @return 返回数据内容 还是byte[]类型
     */
    byte[] decode();
}
