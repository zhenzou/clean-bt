package me.zzhen.bt.dht.routetable;

/**
 * 前缀树
 * 只有叶子节点的value才不为空
 */
public class TreeNode {

    /**
     * 现在没什么用，以后扩展的时候有用
     */
    public final byte key;
    public final TreeNode parent;

    public TreeNode(byte key, TreeNode parent) {
        this.key = key;
        this.parent = parent;
    }

    Bucket value;
    TreeNode left;//大
    TreeNode right;//小
}
