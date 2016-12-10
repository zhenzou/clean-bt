package me.zzhen.bt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Project:CleanBT
 * Create Time: 2016/10/24.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class TreeNode<T> {

    private List<TreeNode<T>> children = new ArrayList<>();
    private TreeNode<T> parent;
    private T value;

    public TreeNode(T value) {
        this.value = value;
    }

    public TreeNode<T> add(T t) {
        TreeNode<T> node = new TreeNode<>(t);
        children.add(node);
        node.parent = this;
        return node;
    }

    public Optional<TreeNode<T>> get(T key) {
        return children.stream().filter(item -> item.getValue().equals(key)).findFirst();
    }

    public TreeNode<T> getOrAdd(T t) {
        Optional<TreeNode<T>> treeNode = get(t);
        if (treeNode.isPresent()) {
            return treeNode.get();
        } else {
            return add(t);
        }
    }

    public List<TreeNode<T>> getChildren() {
        return children;
    }

    public void setChildren(List<TreeNode<T>> children) {
        this.children = children;
    }

    public TreeNode remove(T key) {
        TreeNode<T> node = new TreeNode<>(key);
        children.remove(node);
        node.parent = null;
        return node;
    }

    /**
     * 根节点没有parent节点
     *
     * @return
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * 没有子节点即叶子节点
     *
     * @return
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }


    public static <T> void printTree(TreeNode<T> tree) {
        int len = tree.getChildren().size();
        if (tree.getChildren().size() == 0) {
            System.out.println(tree.getValue());
        }
        for (int i = 0; i < len; i++) {
            printTree(tree.getChildren().get(i));
        }
        System.out.println(tree.getValue());
    }
}
