package me.zzhen.bt;

import me.zzhen.bt.decoder.*;
import me.zzhen.bt.utils.Utils;

import java.io.*;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * /**
 * Project:CleanBT
 *
 * @author zzhen zzzhen1994@gmail.com
 *         Create Time: 2016/10/17.
 *         Version :
 *         Description:
 */
public class TorrentFile {

    //公共字段
    public static final String ANNOUNCE = "announce";               //必选, tracker 服务器的地址
    public static final String NODES = "nodes";               //必选, tracker 服务器的地址
    public static final String ANNOUNCE_LIST = "announce-list";     //list, 可选, 可选的 tracker 服务器地址
    public static final String CREATION_DATE = "creation date";     //必选, 文件创建时间
    public static final String COMMENT = "comment";                 //可选, bt 文件注释
    public static final String CREATED_BY = "created by";           //可选， 文件创建者。
    public static final String ENCODING = "encoding";               //文件编码
    //info
    public static final String INFO = "info";                       //必选, 每一数据块的长度
    public static final String PIECE_LENGTH = "piece length";       //必选, 每一数据块的长度
    public static final String PIECES = "pieces";                   //必选, 所有数据块的 SHA1 校验值
    public static final String PUBLISHER = "publisher";             //可选, 发布者
    public static final String PUBLISHER_UTF8 = "publisher.utf-8";  //可选, 发布者的 UTF-8 编码
    public static final String PUBLISHER_URL = "publisher-url";     //可选, 发布者的 URL
    public static final String PUBLISHER_URL_UTF8 = "publisher-url.utf-8";//可选, 发布者的 URL 的 UTF-8 编码
    //单文件
    public static final String NAME = "name";                       //必选, 推荐的文件名称  多文件-必选, 推荐的文件夹名称
    public static final String NAME_UTF8 = "name.utf8";             //可选, 推荐的文件名称的 UTF-8 编码
    public static final String LENGTH = "length";                   //必选， 文件的长度单位是字节
    //多文件
    public static final String FILES = "files";                     //必选, 文件列表，每个文件列表下面是包括每一个文件的信息，文件信息是个字典。
    public static final String PATH = "path";                       //必选， 文件名称，包含文件夹在内
    public static final String PATH_UTF8 = "path.utf8";
    public static final String FILEHASH = "filehash";               //可选， 文件 hash。
    public static final String ED2K = "ed2k";


    public static List<Node> mValues;

    public static TorrentFile fromString(String bytes) throws IOException, DecoderException {
        Decoder decoder = new Decoder(bytes.getBytes());
        TorrentFile ret = new TorrentFile();
        ret.setFileName("String");
        decoder.setHandler(new TorrentFileHandler(ret));
        decoder.parse();
        return ret;
    }

    public static TorrentFile fromStream(InputStream input) throws IOException, DecoderException {
        Decoder decoder = new Decoder(input);
        TorrentFile ret = new TorrentFile();
        ret.setFileName("String");
        decoder.setHandler(new TorrentFileHandler(ret));
        decoder.parse();
        return ret;
    }

    public static TorrentFile fromFile(File file) throws IOException, DecoderException {
        Decoder decoder = new Decoder(file);
        TorrentFile ret = new TorrentFile();
        ret.setFileName(file.getName());
        decoder.setHandler(new TorrentFileHandler(ret));
        decoder.parse();
        return ret;
    }


    private String fileName;
    private Node announce;
    private Node nodes;
    private ListNode announces;
    private Node creationData;
    private Node comment;
    private Node createdBy;
    private Node encoding;
    private DictionaryNode info;

    private TorrentFile() {

    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getAnnounce() {
        return announce.toString();
    }

    public void setAnnounce(String announce) {
        this.announce = new StringNode(announce.getBytes());
    }

    public void setAnnounce(StringNode announce) {
        this.announce = announce;
    }

    public List<String> getAnnounceList() {
        List<Node> nodes = announces.getValue();
        return nodes.stream().map(Node::toString).collect(Collectors.toList());
    }

    public void setAnnounceList(List<String> announceList) {
        List<Node> list = announceList.stream().map(str -> new StringNode(str.getBytes())).collect(Collectors.toList());
        announces = new ListNode(list);
    }

    public void setAnnounceList(ListNode announceList) {

        announces = announceList;
    }

    public String getEncoding() {
        return encoding.toString();
    }

    public void setEncoidng(String encoidng) {
        encoding = new StringNode(encoidng.getBytes());
    }

    public void setEncoidng(StringNode encoidng) {
        encoding = encoidng;
    }

    public Date getCreationDate() {
        String time = creationData.toString();
        if (time.length() > 10) {
            return Date.from(Instant.ofEpochMilli(Long.parseLong(creationData.toString())));
        } else {
            return Date.from(Instant.ofEpochSecond(Long.parseLong(creationData.toString())));
        }
    }

    public void setCreationData(Date date) {
        long time;
        if (date == null) {
            time = Instant.now().getEpochSecond();
        } else {
            time = date.getTime();
        }
        creationData = new IntNode(String.valueOf(time));
    }

    public Node getNodes() {
        return nodes;
    }

    public void setNodes(Node nodes) {
        this.nodes = nodes;
    }

    public void setCreationData(Node date) {
        creationData = (IntNode) date;
    }

    public String getComment() {
        return comment.toString();
    }

    public void setComment(String comment) {
        this.comment = new StringNode(comment.getBytes());
    }

    public void setComment(StringNode comment) {
        this.comment = comment;
    }

    public String getCreatedBy() {
        return createdBy.toString();
    }

    public void setCreatedBy(StringNode created) {
        createdBy = created;
    }

    public Node getInfo() {
        return info;
    }

    public void setInfo(Map<String, Node> info) {
        this.info = new DictionaryNode(info);
    }

    public void setInfo(DictionaryNode info) {
        this.info = info;
    }

    public long getInfoPieceLength() {
        return Long.parseLong(String.valueOf(info.getNode(PIECE_LENGTH)));
    }

    public void setInfoPieceLength(int len) {
        info.addNode(PIECE_LENGTH, new IntNode(len + ""));
    }

    public String getInfoPieces() {
        return info.getNode(PIECES).toString();
    }

    public void setInfoPieces(String value) {
        info.addNode(PIECES, new StringNode(value.getBytes()));
    }

    public String getInfoPublisher() {
        return String.valueOf(info.getNode(PUBLISHER));
    }

    public void setInfoPublisher(String value) {
        info.addNode(PUBLISHER, new StringNode(value.getBytes()));
    }

    public String getInfoPublisherUtf8() {
        return String.valueOf(info.getNode(PUBLISHER_UTF8));
    }

    public void setInfoPublisherUtf8(String value) {
        info.addNode(PUBLISHER_UTF8, new StringNode(value.getBytes()));
    }

    public String getInfoPublisherUrl() {
        return String.valueOf(info.getNode(PUBLISHER_URL));
    }

    public void setInfoPublisherUrl(String value) {
        info.addNode(PUBLISHER_URL, new StringNode(value.getBytes()));
    }

    public String getInfoPublisherUrlUtf8() {
        return String.valueOf(info.getNode(PUBLISHER_URL_UTF8));
    }

    public void setInfoPublisherUrlUtf8(String value) {
        info.addNode(PUBLISHER_URL_UTF8, new StringNode(value.getBytes()));
    }

    //目录
    public Node getInfoName() {
        return info.getNode(NAME);
    }

    public void settInfoName(Node name) {
        info.addNode(NAME, name);
    }

    public Node getInfoNameUtf8() {
        return info.getNode(NAME);
    }

    //多个文件
    public Node getInfoFiles() {
        return info.getNode(FILES);
    }

    public void setInfoFiles(Node node) {
        info.addNode(FILES, node);
    }

    public long getInfoLength() {
        return Long.parseLong(info.getNode(LENGTH).toString());
    }

    public String getInfoFilesPath() {
        return info.getNode(PATH).toString();
    }

    public void setInfoFilesPath(String value) {
        info.addNode(PATH, new StringNode(value.getBytes()));
    }

    public String getInfoHash() {
        byte[] encode = new byte[0];
        encode = getInfo().encode();
        return Utils.toHex(Utils.SHA_1(encode));
    }

    public String getMagnet() {
        return "magnet:?xt=urn:btih:" + getInfoHash();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (announce != null) {
            sb.append(announce.toString());
        }
        if (nodes != null) {
            sb.append(nodes.toString());
        }
        if (announces != null) {
            sb.append(announces.toString());
        }
        if (encoding != null) {
            sb.append(encoding.toString());
        }
        if (comment != null) {
            sb.append(comment.toString());
        }
        if (creationData != null) {
            sb.append(creationData.toString());
        }
        if (createdBy != null) {
            sb.append(createdBy.toString());
        }
        if (info != null) {
            sb.append(info.toString());
        }
        return sb.toString();
    }

    public byte[] encode() {
        DictionaryNode bt = new DictionaryNode();

        if (announce != null) {
            bt.addNode(ANNOUNCE, announce);
        }
        if (nodes != null) {
            bt.addNode(NODES, nodes);
        }
        if (announces != null) {
            bt.addNode(ANNOUNCE_LIST, announces);
        }
        if (encoding != null) {
            bt.addNode(ENCODING, encoding);
        }
        if (comment != null) {
            bt.addNode(COMMENT, comment);
        }
        if (creationData != null) {
            bt.addNode(CREATION_DATE, creationData);
        }
        if (createdBy != null) {
            bt.addNode(CREATED_BY, createdBy);
        }
        if (info != null) {
            bt.addNode(INFO, info);
        }
        return bt.encode();
    }

    /**
     * @param file 没有检查是否为空
     * @throws IOException
     */
    public void save(File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(encode());
            out.flush();
            out.close();
        } catch (IOException e) {
            throw e;
        }
    }

    public static class TorrentFileHandler implements EventHandler {

        private TorrentFile mTorrentFile;

        public TorrentFileHandler(TorrentFile torrent) {
            mTorrentFile = torrent;
        }

        @Override
        public Node handleDictionaryNode(String key, Node value) {
            switch (key) {
                case ANNOUNCE:
                    mTorrentFile.setAnnounce((StringNode) value);
                    break;
                case NODES:
                    mTorrentFile.setNodes(value);
                    break;
                case ANNOUNCE_LIST:
                    mTorrentFile.setAnnounceList(((ListNode) value));
                    break;
                case CREATION_DATE:
                    mTorrentFile.setCreationData(value);
                    break;
                case COMMENT:
                    mTorrentFile.setComment((StringNode) value);
                    break;
                case CREATED_BY:
                    mTorrentFile.setCreatedBy((StringNode) value);
                    break;
                case ENCODING:
                    mTorrentFile.setEncoidng((StringNode) value);
                    break;
                case INFO:
                    mTorrentFile.setInfo(((DictionaryNode) value));
                    break;
                case PIECE_LENGTH:
                    break;
                case PIECES:
                    break;
                case PUBLISHER:
                    break;
                case PUBLISHER_UTF8:
                    break;
                case PUBLISHER_URL:
                    break;
                case PUBLISHER_URL_UTF8:
                    break;
                case NAME:
                    break;
                case NAME_UTF8:
                    break;
                case LENGTH:
                    break;
                case FILES:
                    break;
                case PATH:
                    break;
                case PATH_UTF8:
                    break;
                case FILEHASH:
                    break;
                case ED2K:
                    break;
            }
            return value;
        }
    }


    public static void main(String[] args) {
        try {
            TorrentFile torrentFile = TorrentFile.fromFile(new File("D:/The.Big.Bang.Theory.torrent"));
            System.out.println(torrentFile.getMagnet());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
