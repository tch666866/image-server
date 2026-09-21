import java.util.Scanner;
import java.util.Random;
public class bblb02 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random rand = new Random();
        //int x = Math.random();
        double a = 0 ,b = 0 , c = 0 ;
        while(true) {
            double x = (int) (Math.random() * 90 + 10);
            System.out.println(x);
            double y = (int) (Math.random() * 90 + 10);
            System.out.println(y);
            double num = rand.nextInt(2);
            char w;
            double shu;
            if (num == 0) {
                w = '+';
                shu = x + y;
            } else {
                w = '-';
                if (x < y) {
                    double temp = x;
                    x = y;
                    y = temp;
                }
                shu = x - y;
            }
            System.out.println("计算" + x + w + y + "=?");
            int f = scanner.nextInt();
            if (f == shu) {
                System.out.println("正确");
                b++;
            } else {
                System.out.println("错误");
                c++;
            }
            a++;
            System.out.println("是否继续答题（Y/N）");
            char SNK = scanner.next().charAt(0);
            if (SNK=='N'){
                System.out.println("结束");
                break;
            }
        }
        System.out.println("总答题数："+a );
        System.out.println("总正确数："+b );
        double o = (100*b)/(100*a) ;
        System.out.println("准确率："+ o );
    }
}