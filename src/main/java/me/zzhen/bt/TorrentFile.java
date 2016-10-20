package me.zzhen.bt;

import me.zzhen.bt.decoder.*;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by zzhen on 2016/10/17.
 */
public class TorrentFile {

    //公共字段
    public static final String ANNOUNCE = "announce";//必选, tracker 服务器的地址
    public static final String ANNOUNCE_LIST = "announce-list";//list, 可选, 可选的 tracker 服务器地址
    public static final String CREATION_DATE = "creation date";//必选, 文件创建时间
    public static final String COMMENT = "comment";//可选, bt 文件注释
    public static final String CREATED_BY = "created by";//可选， 文件创建者。
    public static final String ENCODING = "encoding";//文件编码
    //mInfo
    public static final String INFO = "info";//必选, 每一数据块的长度
    public static final String PIECE_LENGTH = "piece length";//必选, 每一数据块的长度
    public static final String PIECES = "pieces";//必选, 所有数据块的 SHA1 校验值
    public static final String PUBLISHER = "publisher";//可选, 发布者
    public static final String PUBLISHER_UTF8 = "publisher.utf-8";//可选, 发布者的 UTF-8 编码
    public static final String PUBLISHER_URL = "publisher-url";//可选, 发布者的 URL
    public static final String PUBLISHER_URL_UTF8 = "publisher-url.utf-8";//可选, 发布者的 URL 的 UTF-8 编码
    //单文件
    public static final String NAME = "name";//必选, 推荐的文件名称  多文件-必选, 推荐的文件夹名称
    public static final String NAME_UTF8 = "name.utf8";//可选, 推荐的文件名称的 UTF-8 编码
    public static final String LENGTH = "length";//必选， 文件的长度单位是字节
    //多文件
    public static final String FILES = "files";//必选, 文件列表，每个文件列表下面是包括每一个文件的信息，文件信息是个字典。
    public static final String PATH = "path";//必选， 文件名称，包含文件夹在内
    public static final String PATH_UTF8 = "path.utf8";
    public static final String FILEHASH = "filehash";//可选， 文件 hash。
    public static final String ED2K = "ed2k";


    public static List<Node> mValues;

    public static TorrentFile fromString(String bytes) throws IOException {
        Decoder decoder = new Decoder(bytes.getBytes());
        TorrentFile ret = new TorrentFile();
        decoder.setHandler(new TorrentFileHandler(ret));
        decoder.parse();
//        List<Node> value = decoder.getValue();
//        DictionaryNode map = (DictionaryNode) value.get(0);
//        ret.setAnnounce(map.getNode(ANNOUNCE).decode());
        return ret;
    }

    public static TorrentFile fromFile(File file) throws IOException {
        Decoder decoder = new Decoder(file);
        TorrentFile ret = new TorrentFile();
        decoder.setHandler(new TorrentFileHandler(ret));
        decoder.parse();
//        List<Node> value = decoder.getValue();
//        DictionaryNode map = (DictionaryNode) value.get(0);
//        ret.setAnnounce(map.getNode(ANNOUNCE).decode());
        return ret;
    }


    private StringNode mAnnounce;
    private ListNode mAnnounceList;
    private IntNode mCreationDate;
    private StringNode mComment;
    private StringNode mCreatedBy;
    private StringNode mEncoding;
    private DictionaryNode mInfo;


    public String getAnnounce() {
        return mAnnounce.decode();
    }

    public void setAnnounce(String announce) {
        this.mAnnounce = new StringNode(announce.getBytes());
    }

    public List<String> getAnnounceList() {
        List<Node> nodes = mAnnounceList.getValue();
        return nodes.stream().map(Node::decode).collect(Collectors.toList());
    }

    public void setAnnounceList(List<String> announceList) {
        List<Node> list = announceList.stream().map(str -> new StringNode(str.getBytes())).collect(Collectors.toList());
        mAnnounceList = new ListNode(list);
    }

    public String getEncoding() {
        return mEncoding.decode();
    }

    public void setEncoidng(String encoidng) {
        mEncoding = new StringNode(encoidng.getBytes());
    }

    public Date getCreationDate() {
        return Date.from(Instant.ofEpochSecond(Long.parseLong(mCreationDate.decode())));
    }

    public void setCreationData(Date date) {
        mCreationDate = new IntNode(String.valueOf(date.getTime()));
    }

    public void setCreationData(Node date) {
        mCreationDate = (IntNode) date;
    }

    public String getComment() {
        return mComment.decode();
    }

    public void setComment(String comment) {
        mComment = new StringNode(comment.getBytes());
    }

    public String getCreatedBy() {
        return mCreatedBy.decode();
    }

    public void setCreatedBy(String created) {
        mCreatedBy = new StringNode(created.getBytes());
    }

    public Map<String, Node> getInfo() {
        return mInfo.getValue();
    }

    public void setInfo(Map<String, Node> info) {
        this.mInfo = new DictionaryNode(info);
    }

    public int getInfoPieceLength() {
        return Integer.parseInt(String.valueOf(mInfo.getNode(PIECE_LENGTH)));
    }

    public void setInfoPieceLength(int len) {
        mInfo.addNode(PIECE_LENGTH, new IntNode(len + ""));
    }

    public String getInfoPieces() {
        return mInfo.getNode(PIECES).decode();
    }

    public void setInfoPieces(String value) {
        mInfo.addNode(PIECES, new StringNode(value.getBytes()));
    }

    public String getInfoPublisher() {
        return String.valueOf(mInfo.getNode(PUBLISHER));
    }

    public void setInfoPublisher(String value) {
        mInfo.addNode(PUBLISHER, new StringNode(value.getBytes()));
    }

    public String getInfoPublisherUtf8() {
        return String.valueOf(mInfo.getNode(PUBLISHER_UTF8));
    }

    public void setInfoPublisherUtf8(String value) {
        mInfo.addNode(PUBLISHER_UTF8, new StringNode(value.getBytes()));
    }

    public String getInfoPublisherUrl() {
        return String.valueOf(mInfo.getNode(PUBLISHER_URL));
    }

    public void setInfoPublisherUrl(String value) {
        mInfo.addNode(PUBLISHER_URL, new StringNode(value.getBytes()));
    }

    public String getInfoPublisherUrlUtf8() {
        return String.valueOf(mInfo.getNode(PUBLISHER_URL_UTF8));
    }

    public void setInfoPublisherUrlUtf8(String value) {
        mInfo.addNode(PUBLISHER_URL_UTF8, new StringNode(value.getBytes()));
    }

    public Node getInfoName() {
        return mInfo.getNode(NAME);
    }

    public void settInfoName(Node name) {
        mInfo.addNode(NAME, name);
    }

    public Node getInfoNameUtf8() {
        return mInfo.getNode(NAME);
    }


    /**
     * 多个文件
     * <p>
     * length
     *
     * @return
     */
    public Node getInfoFiles() {
        return mInfo.getNode(FILES);
    }

    public void setInfoFiles(Node node) {
        mInfo.addNode(FILES, node);
    }

    public int getInfoLength() {
        return Integer.parseInt(mInfo.getNode(LENGTH).decode());
    }

    public String getInfoFilesPath() {
        return mInfo.getNode(PATH).decode();
    }

    public void setInfoFilesPath(String value) {
        mInfo.addNode(PATH, new StringNode(value.getBytes()));
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
                    mTorrentFile.setAnnounce(value.decode());
                    break;
                case ANNOUNCE_LIST:
                    mTorrentFile.setAnnounceList(((ListNode) value).getValue().stream().map(Node::decode).collect(Collectors.toList()));
                    break;
                case CREATION_DATE:
                    mTorrentFile.setCreationData(value);
                    break;
                case COMMENT:
                    mTorrentFile.setComment(value.decode());
                    break;
                case CREATED_BY:
                    mTorrentFile.setCreatedBy(value.decode());
                    break;
                case ENCODING:
                    mTorrentFile.setEncoidng(value.decode());
                    break;
                case INFO:
                    mTorrentFile.setInfo(((DictionaryNode) value).getValue());
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


    /**
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        DictionaryNode bt = new DictionaryNode();
//        if (mAnnounce != null) {
//            sb.append(mAnnounce.encode());
//        }
//        if (mAnnounceList != null) {
//            sb.append(mAnnounceList.encode());
//        }
//        if (mEncoding != null) {
//            sb.append(mEncoding.encode());
//        }
//        if (mComment != null) {
//            sb.append(mComment.encode());
//        }
//        if (mCreationDate != null) {
//            sb.append(mCreationDate.encode());
//        }
//        if (mCreatedBy != null) {
//            sb.append(mCreatedBy.encode());
//        }
//        if (mInfo != null) {
//            sb.append(mInfo.encode());
//        }
        if (mAnnounce != null) {
            bt.addNode(ANNOUNCE, mAnnounce);
        }
        if (mAnnounceList != null) {
            bt.addNode(ANNOUNCE_LIST, mAnnounceList);
        }
        if (mEncoding != null) {
            bt.addNode(ENCODING, mEncoding);
        }
        if (mComment != null) {
            bt.addNode(COMMENT, mComment);
        }
        if (mCreationDate != null) {
            bt.addNode(CREATION_DATE, mCreationDate);
        }
        if (mCreatedBy != null) {
            bt.addNode(CREATED_BY, mCreatedBy);
        }
        if (mInfo != null) {
            bt.addNode(INFO, mInfo);
        }
//        return bt.encode();
        return "";
    }

    public byte[] encode() {
        DictionaryNode bt = new DictionaryNode();

        if (mAnnounce != null) {
            bt.addNode(ANNOUNCE, mAnnounce);
        }
        if (mAnnounceList != null) {
            bt.addNode(ANNOUNCE_LIST, mAnnounceList);
        }
        if (mEncoding != null) {
            bt.addNode(ENCODING, mEncoding);
        }
        if (mComment != null) {
            bt.addNode(COMMENT, mComment);
        }
        if (mCreationDate != null) {
            bt.addNode(CREATION_DATE, mCreationDate);
        }
        if (mCreatedBy != null) {
            bt.addNode(CREATED_BY, mCreatedBy);
        }
        if (mInfo != null) {
            bt.addNode(INFO, mInfo);
        }
        return bt.encode();
    }

    public static void main(String[] args) {
        try {
            TorrentFile torrentFile = TorrentFile.fromString("D:/Chicago.Med.torrent");
            System.out.println(torrentFile.getAnnounce());
            System.out.println(torrentFile.getInfoFiles());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
