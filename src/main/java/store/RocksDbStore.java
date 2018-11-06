package store;

import config.StoreSettings;
import model.Account;
import model.AccountSerializer;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class RocksDbStore implements Store {

    private static final Logger LOG = LoggerFactory.getLogger(RocksDbStore.class);
    private static final String COLUMN_FAMILY = "accounts";

    private final AccountSerializer accountSerializer;
    private final RocksDB db;
    private final Map<String, ColumnFamilyHandle> handles = new ConcurrentHashMap<>();

    static {
        RocksDB.loadLibrary();
    }


    public RocksDbStore(AccountSerializer accountSerializer, StoreSettings storeSettings) {
        this.accountSerializer = accountSerializer;
        List<ColumnFamilyDescriptor> familyList = createFamilyColumns(storeSettings.path());
        List<ColumnFamilyHandle> columns = new ArrayList<>();
        db = startDb(familyList, columns, storeSettings.path());
        initColumnHandlesMap(familyList, columns);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for ( ColumnFamilyHandle columnFamilyHandle : handles.values()) {
                columnFamilyHandle.close();
            }
        }));
    }

    @Override
    public void put(String accountId, Account account) {
        ColumnFamilyHandle columnFamilyHandle = getHandle(COLUMN_FAMILY);

        try {
            db.put(columnFamilyHandle, new WriteOptions(),
                    accountId.getBytes(),
                    accountSerializer.serialize(account).getBytes());

        } catch (RocksDBException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException("Can not insert data into rocksdb");
        }
    }

    @Override
    public Account get(String accountId) {
        ColumnFamilyHandle columnFamilyHandle = getHandle(COLUMN_FAMILY);
        try {
            byte[] val = db.get(columnFamilyHandle, accountId.getBytes());
            if (val == null || val.length == 0) {
                return null;
            }
            return accountSerializer.deserialize(new String(val));

        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException("Can not get data from RocksDb");
        }
    }

    @Override
    public void delete(String accountId) {
        try {
            db.delete(accountId.getBytes());
        } catch (RocksDBException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException("Can not delete data in RocksDb");
        }
    }

    private ColumnFamilyHandle getHandle(String family) {
        String name = family;
        return handles.computeIfAbsent(name, familyKey -> {
            ColumnFamilyHandle handle = null;
            try {
                byte[] bytes = familyKey.getBytes();
                ColumnFamilyDescriptor descriptor = new ColumnFamilyDescriptor(bytes);
                handle = db.createColumnFamily(descriptor);
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
                throw new RuntimeException("Can not launch RocksDb");
            }
            return handle;
        });
    }

    private static DBOptions createDbOptions() {
        return new DBOptions()
                .setCreateIfMissing(true)
                .setCreateMissingColumnFamilies(true);
    }

    private List<ColumnFamilyDescriptor> createFamilyColumns(String path) {
        List<ColumnFamilyDescriptor> familyList = new ArrayList<>();
        familyList.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));

        try {
            List<byte[]> families = RocksDB.listColumnFamilies(new Options(), path);
            families.forEach(familyName -> familyList.add(new ColumnFamilyDescriptor(familyName)));

        } catch (RocksDBException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException("Can not launch RocksDb");
        }
        return familyList;
    }

    private RocksDB startDb(List<ColumnFamilyDescriptor> familyList, List<ColumnFamilyHandle> columns, String path) {
        DBOptions dbOptions = createDbOptions();
        RocksDB db;
        try {
            db = RocksDB.open(dbOptions, path,
                    familyList,
                    columns);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                dbOptions.close();
                db.close();
            }));
        } catch (RocksDBException e) {

            throw new RuntimeException("Can not launch RocksDb", e);
        }
        return db;

    }

    private void initColumnHandlesMap(List<ColumnFamilyDescriptor> familyList, List<ColumnFamilyHandle> columns) {
        for (int i = 0; i < familyList.size(); i++) {
            String columnFamilyName = new String(familyList.get(i).columnFamilyName());
            handles.put(columnFamilyName, columns.get(i));
        }
    }

}
