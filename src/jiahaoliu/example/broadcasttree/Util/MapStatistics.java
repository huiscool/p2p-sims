package jiahaoliu.example.broadcasttree.Util;

import java.util.Map;

public class MapStatistics {
    public static String getStatistics(Map<Integer, Integer> map) {
        int sum = map.values().stream().mapToInt(x -> x).sum();
        StringBuilder sb = new StringBuilder();
        double average = 0;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            sb.append(entry.getKey() + " : " + (entry.getValue() * 1.0 / sum) * 100 + "%").append("\r\n");
            average += entry.getKey() * entry.getValue();
        }
        sb.append("average steps is : " + average / sum);
        return sb.toString();
    }
}
