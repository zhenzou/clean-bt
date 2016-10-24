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

    private T mValue;

    public TreeNode(T value) {
        mValue = value;

    }

    public void add(T t) {
        mChildren.add(new TreeNode<>(t));
    }

    public Optional<TreeNode<T>> get(T key) {
        return mChildren.stream().filter(item -> {
            System.out.println(key.toString());
            System.out.println(item.toString());
            return item.getValue().equals(key);
        }).findFirst();
    }

    public TreeNode<T> getOrAdd(T t) {
        Optional<TreeNode<T>> treeNode = get(t);
        System.out.println(treeNode.isPresent());
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
        return mValue;
    }

    public void setValue(T value) {
        mValue = value;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int len = mChildren.size();
        for (int i = 0; i < len; i++) {
            getString(sb, mChildren.get(i));
        }
        return sb.toString();
    }

    private String getString(StringBuilder sb, TreeNode<T> children) {
        if (children.getChildren().size() == 0) {
            sb.append(children.getValue().toString());
        } else {
            children.getChildren().forEach(node -> {
                sb.append(getString(new StringBuilder(), node));
            });
        }
        return sb.toString();
    }
}
