import java.util.Random;

/**
 * MathQuiz —— 口算练习核心类
 *
 * 本类算法逻辑与 bblb02.java 完全一致（随机两位数加减法、大数减小数），
 * 只是把它从"控制台程序"提炼成"可复用的类"，供网页后端调用。
 * bblb02.java 原文件未做任何修改。
 */
public class MathQuiz {

    private Random rand = new Random();

    // 当前这道题的数据
    private double x;        // 第一个数（10~99）
    private double y;        // 第二个数（10~99）
    private char op;         // 运算符：'+' 或 '-'
    private double answer;   // 正确答案

    // 统计信息
    private int total = 0;       // 总答题数
    private int correct = 0;     // 正确数
    private int wrong = 0;       // 错误数

    /**
     * 生成一道新题（与 bblb02 的随机规则一致）：
     *  - 两个数都是 10~99 的两位数
     *  - 随机选择加法或减法
     *  - 减法保证大数减小数（结果不为负数）
     */
    public void generateQuestion() {
        // 随机生成两个两位数：Math.random() 返回 0.0~1.0，*90 + 10 得到 10~99
        x = (int) (Math.random() * 90 + 10);
        y = (int) (Math.random() * 90 + 10);

        // 随机决定加减法：rand.nextInt(2) 返回 0 或 1
        int num = rand.nextInt(2);
        if (num == 0) {
            op = '+';
            answer = x + y;
        } else {
            op = '-';
            // 减法时保证 x >= y，避免出现负数
            if (x < y) {
                double temp = x;
                x = y;
                y = temp;
            }
            answer = x - y;
        }
    }

    /**
     * 判断用户答案是否正确，并累计统计。
     * @param userAnswer 用户输入的答案
     * @return 正确返回 true，错误返回 false
     */
    public boolean checkAnswer(double userAnswer) {
        total++;
        boolean ok = (userAnswer == answer);
        if (ok) {
            correct++;
        } else {
            wrong++;
        }
        return ok;
    }

    // ===== getters：供页面显示和统计 =====

    public double getX() { return x; }

    public double getY() { return y; }

    public char getOp() { return op; }

    public double getAnswer() { return answer; }

    public int getTotal() { return total; }

    public int getCorrect() { return correct; }

    public int getWrong() { return wrong; }

    /** 准确率 = 正确数 / 总答题数（百分比，保留整数） */
    public double getRate() {
        if (total == 0) return 0;
        return (100.0 * correct) / (100.0 * total);
    }
}
