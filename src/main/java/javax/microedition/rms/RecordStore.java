package javax.microedition.rms;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RecordStore 的桌面实现：记录保存在用户目录 ~/.aoe-desktop/ 下的小文件里。
 * 格式：int 记录数，然后每条记录 int 长度 + 字节内容。记录 id 从 1 开始。
 * -Daoe.rmsDir=<dir> 可整体重定向落盘目录（批量实验隔离 nfo，
 * 避免每局写回随机图种子污染用户数据）。
 */
public final class RecordStore {
    private static final File STORE_DIR =
            new File(System.getProperty("aoe.rmsDir",
                    System.getProperty("user.home") + "/.aoe-desktop"));

    private final File file;
    private final List<byte[]> records = new ArrayList<>();
    private int nextId = 1;

    private RecordStore(File file) {
        this.file = file;
        this.records.add(null); // id 从 1 开始，0 号位占位
    }

    public static RecordStore openRecordStore(String name, boolean createIfNecessary)
            throws RecordStoreException {
        File f = new File(STORE_DIR, name.replaceAll("[^A-Za-z0-9._-]", "_") + ".rms");
        RecordStore rs = new RecordStore(f);
        if (f.exists()) {
            rs.load();
        } else if (!createIfNecessary) {
            throw new RecordStoreException("record store not found: " + name);
        }
        return rs;
    }

    public static void deleteRecordStore(String name) throws RecordStoreException {
        File f = new File(STORE_DIR, name.replaceAll("[^A-Za-z0-9._-]", "_") + ".rms");
        if (f.exists() && !f.delete()) {
            throw new RecordStoreException("cannot delete: " + name);
        }
    }

    private void load() throws RecordStoreException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            int n = in.readInt();
            for (int i = 0; i < n; ++i) {
                int id = in.readInt();
                int len = in.readInt();
                byte[] data = new byte[len];
                in.readFully(data);
                while (records.size() <= id) {
                    records.add(null);
                }
                records.set(id, data);
                nextId = Math.max(nextId, id + 1);
            }
        } catch (IOException e) {
            throw new RecordStoreException("cannot load " + file + ": " + e);
        }
    }

    private void save() {
        try {
            STORE_DIR.mkdirs();
            try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
                out.writeInt(records.size() - 1);
                for (int i = 1; i < records.size(); ++i) {
                    byte[] data = records.get(i);
                    out.writeInt(i);
                    out.writeInt(data.length);
                    out.write(data);
                }
            }
        } catch (IOException e) {
            // 存档写失败不影响游戏运行
            e.printStackTrace();
        }
    }

    public int getNumRecords() {
        return records.size() - 1;
    }

    public int addRecord(byte[] data, int offset, int numBytes) {
        records.add(Arrays.copyOfRange(data, offset, offset + numBytes));
        save();
        return nextId++;
    }

    public byte[] getRecord(int recordId) throws RecordStoreException {
        if (recordId < 1 || recordId >= records.size() || records.get(recordId) == null) {
            throw new RecordStoreException("no record " + recordId);
        }
        return records.get(recordId);
    }

    public int getRecordSize(int recordId) throws RecordStoreException {
        return getRecord(recordId).length;
    }

    public void setRecord(int recordId, byte[] newData, int offset, int numBytes)
            throws RecordStoreException {
        if (recordId < 1 || recordId >= records.size()) {
            throw new RecordStoreException("no record " + recordId);
        }
        records.set(recordId, Arrays.copyOfRange(newData, offset, offset + numBytes));
        save();
    }

    public void deleteRecord(int recordId) throws RecordStoreException {
        if (recordId < 1 || recordId >= records.size()) {
            throw new RecordStoreException("no record " + recordId);
        }
        records.set(recordId, null);
        save();
    }

    public void closeRecordStore() {
        save();
    }
}
