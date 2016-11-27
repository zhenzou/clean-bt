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

    private List<TreeNode<T>> mChildren = new ArrayList<>();

    private T value;

    public TreeNode(T value) {
        this.value = value;
    }

    public void add(T t) {
        mChildren.add(new TreeNode<>(t));
    }

    public Optional<TreeNode<T>> get(T key) {
        return mChildren.stream().filter(item -> item.getValue().equals(key)).findFirst();
    }

    public TreeNode<T> getOrAdd(T t) {
        Optional<TreeNode<T>> treeNode = get(t);
        if (treeNode.isPresent()) {
            return treeNode.get();
        } else {
            TreeNode<T> node = new TreeNode<>(t);
            mChildren.add(node);
            return node;
        }
    }

    public List<TreeNode<T>> getChildren() {
        return mChildren;
    }

    public void setChildren(List<TreeNode<T>> children) {
        mChildren = children;
    }

    public TreeNode remove(T key) {

        TreeNode<T> node = new TreeNode<T>(key);
        mChildren.remove(node);
        return node;
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
