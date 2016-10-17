package me.zzhen.bt;

import me.zzhen.bt.decoder.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Created by zzhen on 2016/10/17.
 */
public class TorrentFile {
    /*
    bt 种子文件是使用 bencode 编码的，整个文件就 dictionary，包含以下键。

    info, dictinary, 必选, 表示该bt种子文件的文件信息。
        文件信息包括文件的公共部分
        piece length, integer, 必选, 每一数据块的长度
        pieces, string, 必选, 所有数据块的 SHA1 校验值
        publisher, string, 可选, 发布者
        publisher.utf-8, string, 可选, 发布者的 UTF-8 编码
        publisher-url, string, 可选, 发布者的 URL
        publisher-url.utf-8, string, 可选, 发布者的 URL 的 UTF-8 编码
        如果 bt 种子包含的是单个文件，包含以下内容
        name, string, 必选, 推荐的文件名称
        name.utf-8, string, 可选, 推荐的文件名称的 UTF-8 编码
        length, int, 必选， 文件的长度单位是字节
        如果是多文件，则包含以下部分:
        name, string, 必选, 推荐的文件夹名称
        name.utf-8, string, 可选, 推荐的文件名称的 UTF-8 编码
        files, list, 必选, 文件列表，每个文件列表下面是包括每一个文件的信息，文件信息是个字典。
        文件字典
        length, int, 必选， 文件的长度单位是字节
        path, string, 必选， 文件名称，包含文件夹在内
        path.utf-8, string, 必选， 文件名称 UTF-8 表示，包含文件夹在内
        filehash，string, 可选， 文件 hash。
        ed2k, string, 可选, ed2k 信息。
    announce, string, 必选, tracker 服务器的地址
    announce-list, list, 可选, 可选的 tracker 服务器地址
    creation date， interger， 必选, 文件创建时间
    comment， string, 可选, bt 文件注释
    created by， string， 可选， 文件创建者。
     */


    public static TorrentFile fromFile(String file) throws IOException {
        Decoder decoder = new Decoder(file);
        decoder.parse();
        List<Node> value = decoder.getValue();
        TorrentFile ret = new TorrentFile();
        DictionaryNode map = (DictionaryNode) value.get(0);
        ret.setAnnounce((StringNode) map.getNode("announce"));
        return ret;
    }

    private StringNode announce;
    private ListNode announceList;
    private IntNode creationDate;
    private StringNode comment;
    private StringNode createdBy;
    private DictionaryNode info;

    //*info


    public StringNode getAnnounce() {
        return announce;
    }

    public void setAnnounce(StringNode announce) {
        this.announce = announce;
    }

    public ListNode getAnnounceList() {
        return announceList;
    }

    public void setAnnounceList(ListNode announceList) {
        this.announceList = announceList;
    }

    public IntNode getCreationDate() {
        return creationDate;
    }

    public void setCreationData(IntNode creation_date) {
        this.creationDate = creation_date;
    }

    public StringNode getComment() {
        return comment;
    }

    public void setComment(StringNode comment) {
        this.comment = comment;
    }

    public StringNode getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(StringNode created_by) {
        this.createdBy = created_by;
    }

    public DictionaryNode getInfo() {
        return info;
    }

    public void setInfo(DictionaryNode info) {
        this.info = info;
    }

    public int getInfoPieceLength() {
        return Integer.parseInt(String.valueOf(info.getNode("piece length")));
    }

    public void setInfoPieceLength(int len) {
        info.addNode("piece length", new IntNode(len + ""));
    }

    public String getInfoPieces() {
        return String.valueOf(info.getNode("pieces"));
    }

    public String getInfoPublisher() {
        return String.valueOf(info.getNode("publisher"));
    }

    public String getInfoPublisherUtf8() {
        return String.valueOf(info.getNode("publisher.utf-8"));
    }

    public String getInfoPublisherUrl() {
        return String.valueOf(info.getNode("publisher-url"));
    }

    public String getInfoPublisherUrlUtf8() {
        return String.valueOf(info.getNode("publisher-url.utf-8"));
    }

    public Node getInfoName() {
        return info.getNode("name");
    }

    public Node getInfoNameUtf8() {
        return info.getNode("name.utf-8");
    }

    /**
     * 多个文件
     *
     * @return
     */
    public Node getInfoFiles() {
        return info.getNode("files");
    }

    public int getInfoLength() {
        return Integer.parseInt(info.getNode("length").toString());
    }

    public Node getInfoFilesPath() {
        return info.getNode("files");
    }

}
