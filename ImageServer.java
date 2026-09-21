import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

/**
 * 图片画廊 + 口算练习 服务器（部署版）
 *
 * 支持两种运行方式：
 *  1. 本地IDEA运行：从项目目录读取 index.html / quiz.html / images/
 *  2. 打包jar部署：从classpath读取资源文件
 *  3. 端口自动读取环境变量 PORT（云平台），否则默认9090
 */
public class ImageServer {

    // 端口：优先读环境变量（云平台），否则用9090
    private static final int PORT = Integer.parseInt(
        System.getenv().getOrDefault("PORT", "9090")
    );

    // 口算：题目ID -> 正确答案（存服务端，防止前端直接看答案）
    private static final Map<Integer, Double> QUIZ_ANSWERS = new ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicInteger QUIZ_ID = new java.util.concurrent.atomic.AtomicInteger(0);
    private static final MathQuiz QUIZ = new MathQuiz();

    // 全局统计
    private static final int[] QUIZ_STATS = new int[2];

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", ImageServer::handleIndex);
        server.createContext("/images/", ImageServer::handleImage);
        server.createContext("/quiz", ImageServer::handleQuiz);
        server.createContext("/api/quiz/new", ImageServer::handleQuizNew);
        server.createContext("/api/quiz/check", ImageServer::handleQuizCheck);
        server.setExecutor(null);
        server.start();
        System.out.println("=================================");
        System.out.println("  服务器已启动！端口: " + PORT);
        System.out.println("  图片画廊: http://localhost:" + PORT);
        System.out.println("  口算练习: http://localhost:" + PORT + "/quiz");
        System.out.println("=================================");
    }

    /** 通用：读取资源文件（优先文件系统，其次classpath） */
    private static byte[] readResource(String path) throws IOException {
        // 先从文件系统读（本地开发时）
        java.io.File f = new java.io.File(path);
        if (f.exists() && f.isFile()) {
            return java.nio.file.Files.readAllBytes(f.toPath());
        }
        // 再从classpath读（打包成jar后）
        try (InputStream is = ImageServer.class.getClassLoader().getResourceAsStream(path)) {
            if (is != null) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] tmp = new byte[4096];
                int n;
                while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
                return buf.toByteArray();
            }
        }
        throw new IOException("找不到资源: " + path);
    }

    private static void handleIndex(HttpExchange exchange) throws IOException {
        byte[] response = readResource("index.html");
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private static void handleImage(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String fileName = path.substring("/images/".length());

        try {
            byte[] data = readResource("images/" + fileName);
            exchange.getResponseHeaders().set("Content-Type", "image/jpeg");
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        } catch (IOException e) {
            String err = "404: " + fileName;
            exchange.sendResponseHeaders(404, err.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(err.getBytes());
            }
        }
    }

    /** 口算练习页面 */
    private static void handleQuiz(HttpExchange exchange) throws IOException {
        byte[] response = readResource("quiz.html");
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    /** 接口：出一道新题 */
    private static void handleQuizNew(HttpExchange exchange) throws IOException {
        QUIZ.generateQuestion();
        int id = QUIZ_ID.incrementAndGet();
        QUIZ_ANSWERS.put(id, QUIZ.getAnswer());

        String json = String.format(
            "{\"id\":%d,\"x\":%.0f,\"op\":\"%c\",\"y\":%.0f}",
            id, QUIZ.getX(), QUIZ.getOp(), QUIZ.getY());

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    /** 接口：判题 */
    private static void handleQuizCheck(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String json;
        try {
            int id = Integer.parseInt(params.getOrDefault("id", "0"));
            double userAnswer = Double.parseDouble(params.getOrDefault("answer", "0"));
            Double correctAnswer = QUIZ_ANSWERS.remove(id);
            if (correctAnswer == null) {
                json = "{\"error\":\"题目不存在\"}";
            } else {
                boolean ok = (userAnswer == correctAnswer);
                if (ok) QUIZ_STATS[0]++; else QUIZ_STATS[1]++;
                int total = QUIZ_STATS[0] + QUIZ_STATS[1];
                double rate = total == 0 ? 0 : (100.0 * QUIZ_STATS[0]) / total;
                json = String.format(
                    "{\"correct\":%b,\"total\":%d,\"correctCount\":%d,\"wrongCount\":%d,\"rate\":%.1f}",
                    ok, total, QUIZ_STATS[0], QUIZ_STATS[1], rate);
            }
        } catch (NumberFormatException e) {
            json = "{\"error\":\"参数错误\"}";
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    /** 解析 URL 查询参数 */
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                map.put(key, val);
            }
        }
        return map;
    }

    /** 口算核心类（与 bblb02.java 算法一致） */
    static class MathQuiz {
        private Random rand = new Random();
        private double x, y, answer;
        private char op;

        public void generateQuestion() {
            x = (int) (Math.random() * 90 + 10);
            y = (int) (Math.random() * 90 + 10);
            if (rand.nextInt(2) == 0) {
                op = '+';
                answer = x + y;
            } else {
                op = '-';
                if (x < y) { double t = x; x = y; y = t; }
                answer = x - y;
            }
        }
        public double getX() { return x; }
        public double getY() { return y; }
        public char getOp() { return op; }
        public double getAnswer() { return answer; }
    }
}
