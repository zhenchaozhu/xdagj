package io.xdag.db.store;

import cn.hutool.core.bean.BeanUtil;
import io.xdag.config.Config;
import io.xdag.core.Block;
import io.xdag.core.XdagStats;
import io.xdag.crypto.ECKey;
import io.xdag.crypto.Sha256Hash;
import io.xdag.db.DatabaseFactory;
import io.xdag.db.DatabaseName;
import io.xdag.db.KVSource;
import io.xdag.db.rocksdb.RocksdbFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.math.BigInteger;

import static io.xdag.BlockGenerater.*;
import static org.junit.Assert.*;

public class BlockStoreTest {
    @Rule
    public TemporaryFolder root = new TemporaryFolder();

    Config config = new Config();
    DatabaseFactory factory;
    KVSource<byte[], byte[]> indexSource;
    KVSource<byte[], byte[]> timeSource;
    KVSource<byte[], byte[]> blockSource;

    @Before
    public void setUp() throws Exception {
        config.setStoreDir(root.newFolder().getAbsolutePath());
        config.setStoreBackupDir(root.newFolder().getAbsolutePath());
        factory = new RocksdbFactory(config);
        indexSource = factory.getDB(DatabaseName.INDEX);
        timeSource = factory.getDB(DatabaseName.TIME);
        blockSource = factory.getDB(DatabaseName.BLOCK);
    }

    @Test
    public void testNewBlockStore() {
        BlockStore bs = new BlockStore(indexSource, timeSource, blockSource);
        assertNotNull(bs);
    }

    @Test
    public void testInit() {
        BlockStore bs = new BlockStore(indexSource, timeSource, blockSource);
        bs.init();
    }

    @Test
    public void testReset() {
        BlockStore bs = new BlockStore(indexSource, timeSource, blockSource);
        bs.reset();
    }

    @Test
    public void testSaveXdagStatus() {
        BlockStore bs = new BlockStore(indexSource, timeSource, blockSource);
        bs.init();
        XdagStats stats = new XdagStats();
        byte[] hashlow = Sha256Hash.hashTwice("".getBytes());
        stats.setTopMainChain(hashlow);
        stats.setTopDiff(BigInteger.ONE);
        stats.setNmain(1);
        bs.saveXdagStatus(stats);
        XdagStats storedStats = bs.getXdagStatus();
        assertArrayEquals(stats.getTopMainChain(), storedStats.getTopMainChain());
        assertEquals(stats.getTopDiff(), storedStats.getTopDiff());
        assertEquals(stats.getNmain(), storedStats.getNmain());
    }

    @Test
    public void testSaveBlock() {
        BlockStore bs = new BlockStore(indexSource, timeSource, blockSource);
        bs.init();
        long time = System.currentTimeMillis();
        ECKey key = new ECKey();
        Block block = generateAddressBlock(key, time);
        bs.saveBlock(block);
        Block storedBlock = bs.getBlockByHash(block.getHashLow(), true);
        assertArrayEquals(block.toBytes(), storedBlock.toBytes());
    }
}