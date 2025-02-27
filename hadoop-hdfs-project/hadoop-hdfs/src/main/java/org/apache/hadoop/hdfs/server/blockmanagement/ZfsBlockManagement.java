package org.apache.hadoop.hdfs.server.blockmanagement;

import jni.DnodeAttributes;
import jni.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZfsBlockManagement {

    public static final Logger LOG = LoggerFactory.getLogger(ZfsBlockManagement.class.getName());

    // What datanode storage "failure" contributed to the block failure
    // "failure" does not mean data storage failure, it just mean that block cannot be r/w/found for some reason
    public Map<Long, List<ZfsFailureTuple>> blockFailureSources = new ConcurrentHashMap<>();

    public ZfsBlockManagement() {
        this.blockFailureSources = new ConcurrentHashMap<>();
    }

    // We can check whether the map contains blockId
    // If so, the block failure is caused by
    public boolean isZfsFailure(BlockInfo blockInfo) {
        return blockFailureSources.containsKey(blockInfo.getBlockId());
    }

    public List<ZfsFailureTuple> getFailureTuple(BlockInfo blockInfo) {
        return blockFailureSources.get(blockInfo.getBlockId());
    }

    // This is for R_min
    public List<ZfsFailureTuple> getDataNodeZfsFailureTuples(DatanodeDescriptor datanode) {
        // This needs to talk to ZFS through custom JDK/JNI
        // TODO: Dummy implementation

        return null;
    }

    public static void writeToZfs(OutputStream os) {
        // Do nothing.
    }

    /**
     * Returns failed stripes on data node. This is for R_fco.
     *
     * @param datanode the data node to look for.
     * @return list of bloc info.
     */
    public static List<ZfsFailureTuple> getDataNodeZfsFailedStripes(DatanodeDescriptor datanode) {

        // Randomly select a block sitting on the datanode
        // Map<DatanodeStorage, BlockListAsLongs> report = datanode.getFSDataset().getBlockReports(blockPoolId);
        // long[] blocks = new ArrayList<>(report.entrySet()).get(0).getValue().getBlockListAsLongs();
        List<Long> failedBlocks = new ArrayList<>();

        datanode.getBlockIterator().forEachRemaining(blockInfo -> {
            failedBlocks.add(blockInfo.getBlockId());
            LOG.info("BlockInfo {} on datanode {}", blockInfo, datanode.getHostName());
        });
        
        // Always mark the first one as failed
        // TODO: return the first block on the datanode as failed, 2+1, so the second data chunk failed
        return Collections.singletonList(new ZfsFailureTuple(
                failedBlocks.get(0), Collections.singletonList(1)));
    }

}
