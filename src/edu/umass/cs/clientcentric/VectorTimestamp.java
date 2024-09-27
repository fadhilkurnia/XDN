package edu.umass.cs.clientcentric;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RunWith(Enclosed.class)
public class VectorTimestamp {

    private final Map<String, Long> nodeTimestamp;

    public VectorTimestamp(List<String> nodeIds) {
        nodeTimestamp = new ConcurrentHashMap<>();
        for (String nodeId : nodeIds) {
            nodeTimestamp.put(nodeId, 0L);
        }
    }

    public synchronized Set<String> getNodeIds() {
        return this.nodeTimestamp.keySet();
    }

    public synchronized void updateNodeTimestamp(String nodeID, long timestamp) {
        assert this.nodeTimestamp.containsKey(nodeID);
        this.nodeTimestamp.put(nodeID, timestamp);
    }

    public synchronized VectorTimestamp increaseNodeTimestamp(String nodeID) {
        Long tt = this.nodeTimestamp.get(nodeID);
        assert tt != null;
        this.nodeTimestamp.put(nodeID, tt + 1);
        return this;
    }

    public synchronized long getNodeTimestamp(String nodeID) {
        Long tt = this.nodeTimestamp.get(nodeID);
        assert tt != null : "non-existent timestamp is accessed";
        return tt;
    }

    public synchronized boolean isComparableWith(VectorTimestamp other) {
        // timestamps are incomparable if the number of nodes is different
        if (this.nodeTimestamp.size() != other.nodeTimestamp.size()) {
            return false;
        }

        // timestamps are incomparable if the sets of node is different
        if (!this.nodeTimestamp.keySet().equals(other.nodeTimestamp.keySet())) {
            return false;
        }

        return true;
    }

    public synchronized boolean isLessThan(VectorTimestamp other) {
        assert other != null;
        assert this.isComparableWith(other);

        boolean hasAtLeastOneLessTimestamp = false;
        for (String nodeID : this.nodeTimestamp.keySet()) {
            Long myNodeIdTimestamp = this.nodeTimestamp.get(nodeID);
            Long theirNodeIdTimestamp = other.nodeTimestamp.get(nodeID);
            assert myNodeIdTimestamp != null;
            assert theirNodeIdTimestamp != null;
            if (myNodeIdTimestamp > theirNodeIdTimestamp) {
                return false;
            }
            if (myNodeIdTimestamp < theirNodeIdTimestamp) {
                hasAtLeastOneLessTimestamp = true;
            }
        }

        return hasAtLeastOneLessTimestamp;
    }

    public synchronized boolean isEqualTo(VectorTimestamp other) {
        assert other != null;
        assert this.isComparableWith(other);

        for (String nodeID : this.nodeTimestamp.keySet()) {
            Long myNodeIdTimestamp = this.nodeTimestamp.get(nodeID);
            Long theirNodeIdTimestamp = other.nodeTimestamp.get(nodeID);
            assert myNodeIdTimestamp != null;
            assert theirNodeIdTimestamp != null;
            if (!myNodeIdTimestamp.equals(theirNodeIdTimestamp)) {
                return false;
            }
        }

        return true;
    }

    public synchronized boolean isLessThanOrEqualTo(VectorTimestamp other) {
        return this.isLessThan(other) || this.isEqualTo(other);
    }

    public synchronized boolean isGreaterThan(VectorTimestamp other) {
        assert other != null;
        assert this.isComparableWith(other);

        boolean hasAtLeastOneGreaterTimestamp = false;
        for (String nodeID : this.nodeTimestamp.keySet()) {
            Long myNodeIdTimestamp = this.nodeTimestamp.get(nodeID);
            Long theirNodeIdTimestamp = other.nodeTimestamp.get(nodeID);
            assert myNodeIdTimestamp != null;
            assert theirNodeIdTimestamp != null;
            if (myNodeIdTimestamp < theirNodeIdTimestamp) {
                return false;
            }
            if (myNodeIdTimestamp > theirNodeIdTimestamp) {
                hasAtLeastOneGreaterTimestamp = true;
            }
        }

        return hasAtLeastOneGreaterTimestamp;
    }

    public synchronized boolean isZero() {
        for (String nodeID : this.nodeTimestamp.keySet()) {
            long myNodeIdTimestamp = this.nodeTimestamp.get(nodeID);
            if (myNodeIdTimestamp != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VectorTimestamp/");
        int remainingNodes = this.nodeTimestamp.size();
        for (String nodeID : this.nodeTimestamp.keySet()) {
            sb.append(nodeID);
            sb.append(":");
            sb.append(this.nodeTimestamp.get(nodeID));
            if (remainingNodes > 1) {
                remainingNodes -= 1;
                sb.append(".");
            }
        }
        sb.append("/");
        return sb.toString();
    }

    public static VectorTimestamp createFromString(String encodedTimestamp) {
        String validPrefix = "VectorTimestamp/";
        assert encodedTimestamp.startsWith(validPrefix) :
                "Incorrect string format for VectorTimestamp";

        String rawTimestamp = encodedTimestamp.substring(validPrefix.length(),
                encodedTimestamp.length() - 1);
        String[] nodeIdTimestampPairs = rawTimestamp.split("\\.");
        List<String> nodeIDs = new ArrayList<>();
        List<Long> nodeTimestamps = new ArrayList<>();
        for (String pair : nodeIdTimestampPairs) {
            String[] temp = pair.split(":");
            assert temp.length == 2 : "Invalid string format for VectorTimestamp";
            nodeIDs.add(temp[0]);
            nodeTimestamps.add(Long.valueOf(temp[1]));
        }

        VectorTimestamp timestamp = new VectorTimestamp(nodeIDs);
        for (int i = 0; i < nodeTimestamps.size(); ++i) {
            timestamp.updateNodeTimestamp(nodeIDs.get(i), nodeTimestamps.get(i));
        }
        return timestamp;
    }

    public static class VectorTimestampTest {
        @Test
        public void VectorTimestampTest_ToString() {
            VectorTimestamp timestamp = new VectorTimestamp(List.of("AR0", "AR1", "AR2"));
            assert timestamp.toString().equals("VectorTimestamp/AR1:0.AR2:0.AR0:0/") :
                    "Incorrect timestamp format in String: " + timestamp;
        }
        @Test
        public void VectorTimestampTest_FromString() {
            String encoded = "VectorTimestamp/AR1:313.AR2:354.AR0:413/";
            VectorTimestamp timestamp = VectorTimestamp.createFromString(encoded);
            assert timestamp.getNodeTimestamp("AR1") == 313;
            assert timestamp.getNodeTimestamp("AR2") == 354;
            assert timestamp.getNodeTimestamp("AR0") == 413;
        }
    }

}
