import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class SubwaySystem {
    // 存储地铁网络图：站点 -> 相邻站点列表（包含线路和距离）
    private final Map<String, List<Edge>> graph = new HashMap<>();
    // 存储站点所属的线路：站点 -> 线路集合
    private final Map<String, Set<String>> stationLines = new HashMap<>();

    // 内部类：表示两个站点之间的边
    static class Edge {
        String target;      // 相邻站点名称
        String line;        // 所属线路
        double distance;    // 距离（公里）

        public Edge(String target, String line, double distance) {
            this.target = target;
            this.line = line;
            this.distance = distance;
        }
    }

    // 内部类：表示中转站信息
    static class TransferStation {
        String station;
        Set<String> lines;

        public TransferStation(String station, Set<String> lines) {
            this.station = station;
            this.lines = lines;
        }

        @Override
        public String toString() {
            return "<" + station + ", " + lines + ">";
        }
    }

    // 内部类：表示附近站点信息
    static class NearbyStation {
        String station;
        String line;
        int distance;  // 站点间隔数

        public NearbyStation(String station, String line, int distance) {
            this.station = station;
            this.line = line;
            this.distance = distance;
        }

        @Override
        public String toString() {
            return "<" + station + ", " + line + ", " + distance + ">";
        }
    }

    // 从文件加载地铁网络数据
    public void loadNetwork(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length < 4) continue;

                String lineName = parts[0];
                String stationA = parts[1];
                String stationB = parts[2];
                double distance = Double.parseDouble(parts[3]);

                // 更新站点-线路映射
                stationLines.computeIfAbsent(stationA, k -> new HashSet<>()).add(lineName);
                stationLines.computeIfAbsent(stationB, k -> new HashSet<>()).add(lineName);

                // 添加双向边
                addEdge(stationA, stationB, lineName, distance);
                addEdge(stationB, stationA, lineName, distance);
            }
        }
    }

    // 添加边到图中
    private void addEdge(String from, String to, String line, double distance) {
        graph.computeIfAbsent(from, k -> new ArrayList<>())
             .add(new Edge(to, line, distance));
    }

    // 功能1: 获取所有中转站
    public List<TransferStation> getTransferStations() {
        return stationLines.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2)
                .map(entry -> new TransferStation(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    // 功能2: 获取距离小于n的所有站点
    public List<NearbyStation> getNearbyStations(String start, int maxDistance) {
        List<NearbyStation> result = new ArrayList<>();
        // BFS队列：站点, 距离
        Queue<Map.Entry<String, Integer>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        queue.offer(new AbstractMap.SimpleEntry<>(start, 0));
        visited.add(start);

        while (!queue.isEmpty()) {
            Map.Entry<String, Integer> current = queue.poll();
            String station = current.getKey();
            int distance = current.getValue();
            
            if (distance > 0 && distance <= maxDistance) {
                for (String line : stationLines.get(station)) {
                    result.add(new NearbyStation(station, line, distance));
                }
            }
            
            if (distance < maxDistance) {
                for (Edge edge : graph.getOrDefault(station, Collections.emptyList())) {
                    if (!visited.contains(edge.target)) {
                        visited.add(edge.target);
                        queue.offer(new AbstractMap.SimpleEntry<>(edge.target, distance + 1));
                    }
                }
            }
        }
        return result;
    }

    // 功能3: 获取所有路径（DFS）
    public List<List<String>> getAllPaths(String start, String end) {
        List<List<String>> paths = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        dfs(start, end, new ArrayList<>(), visited, paths);
        return paths;
    }

    private void dfs(String current, String end, 
                    List<String> path, 
                    Set<String> visited, 
                    List<List<String>> paths) {
        path.add(current);
        visited.add(current);
        
        if (current.equals(end)) {
            paths.add(new ArrayList<>(path));
        } else {
            for (Edge edge : graph.getOrDefault(current, Collections.emptyList())) {
                if (!visited.contains(edge.target)) {
                    dfs(edge.target, end, path, visited, paths);
                }
            }
        }
        
        path.remove(path.size() - 1);
        visited.remove(current);
    }

    // 功能4: 获取最短路径（BFS）
    public List<String> getShortestPath(String start, String end) {
        // 存储前驱节点
        Map<String, String> predecessors = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        queue.offer(start);
        visited.add(start);
        predecessors.put(start, null);
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            
            if (current.equals(end)) break;
            
            for (Edge edge : graph.getOrDefault(current, Collections.emptyList())) {
                if (!visited.contains(edge.target)) {
                    visited.add(edge.target);
                    predecessors.put(edge.target, current);
                    queue.offer(edge.target);
                }
            }
        }
        
        // 回溯构建路径
        List<String> path = new ArrayList<>();
        String current = end;
        while (current != null) {
            path.add(0, current);
            current = predecessors.get(current);
        }
        
        return path;
    }

    // 功能5: 格式化输出路径
    public void printFormattedPath(List<String> path) {
        if (path.isEmpty()) return;
        
        String currentLine = findCommonLine(path.get(0), path.get(1));
        String startSegment = path.get(0);
        
        for (int i = 1; i < path.size(); i++) {
            String nextLine = (i < path.size() - 1) ? 
                findCommonLine(path.get(i), path.get(i + 1)) : null;
            
            if (nextLine == null || !nextLine.equals(currentLine)) {
                System.out.println("乘坐" + currentLine + "从" + startSegment + "到" + path.get(i));
                if (nextLine != null) {
                    currentLine = nextLine;
                    startSegment = path.get(i);
                }
            }
        }
    }

    // 查找两个相邻站点的共同线路
    private String findCommonLine(String station1, String station2) {
        for (Edge edge : graph.get(station1)) {
            if (edge.target.equals(station2)) {
                return edge.line;
            }
        }
        return null;
    }

    // 功能6: 计算普通票价
    public double calculateFare(List<String> path) {
        double totalDistance = calculateTotalDistance(path);
        return calculateFareByDistance(totalDistance);
    }

    // 计算路径总距离
    private double calculateTotalDistance(List<String> path) {
        double total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            for (Edge edge : graph.get(path.get(i))) {
                if (edge.target.equals(path.get(i + 1))) {
                    total += edge.distance;
                    break;
                }
            }
        }
        return total;
    }

    // 根据距离计算票价（武汉地铁规则）
    private double calculateFareByDistance(double distance) {
        if (distance <= 4) return 2;
        if (distance <= 8) return 3;
        if (distance <= 12) return 4;
        if (distance <= 18) return 5;
        if (distance <= 24) return 6;
        if (distance <= 32) return 7;
        if (distance <= 40) return 8;
        if (distance <= 50) return 9;
        return 9 + Math.ceil((distance - 50) / 20);
    }

    // 功能7: 计算优惠票价
    public double calculateDiscountedFare(List<String> path, String cardType) {
        if (cardType.equals("WHT")) { // 武汉通
            return calculateFare(path) * 0.9;
        } else if (cardType.startsWith("Day")) { // 日票
            return 0;
        }
        return calculateFare(path);
    }

    public static void main(String[] args) {
        SubwaySystem system = new SubwaySystem();
        try {
            // 加载地铁网络数据
            system.loadNetwork("subway.txt");
            
            // 测试功能1：中转站
            System.out.println("===== 中转站 =====");
            system.getTransferStations().forEach(System.out::println);
            
            // 测试功能2：附近站点
            System.out.println("\n===== 附近站点 =====");
            system.getNearbyStations("华中科技大学站", 1).forEach(System.out::println);
            
            // 测试功能3：所有路径
            System.out.println("\n===== 所有路径 =====");
            List<List<String>> allPaths = system.getAllPaths("站点A", "站点C");
            for (int i = 0; i < Math.min(3, allPaths.size()); i++) {
                System.out.println("路径" + (i+1) + ": " + allPaths.get(i));
            }
            
            // 测试功能4：最短路径
            System.out.println("\n===== 最短路径 =====");
            List<String> shortestPath = system.getShortestPath("华中科技大学站", "光谷广场站");
            System.out.println("最短路径: " + shortestPath);
            
            // 测试功能5：格式化输出路径
            System.out.println("\n===== 路径导航 =====");
            system.printFormattedPath(shortestPath);
            
            // 测试功能6：票价计算
            System.out.println("\n===== 票价计算 =====");
            double fare = system.calculateFare(shortestPath);
            System.out.printf("普通票价: %.2f元%n", fare);
            System.out.printf("武汉通票价: %.2f元%n", system.calculateDiscountedFare(shortestPath, "WHT"));
            System.out.println("日票票价: 0.00元");
            
        } catch (IOException e) {
            System.err.println("加载地铁数据失败: " + e.getMessage());
        }
    }
}
