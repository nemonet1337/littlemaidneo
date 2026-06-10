package work.nemonet.littlemaidneo.client.screen.component;

public record RangeChecker(int x, int y, int width, int height) {

    public boolean check(double x, double y) {
        return checkFromWidth(x, y, this.x, this.y, width, height);
    }

    public static boolean checkFromWidth(double x, double y, double baseX, double baseY, double width, double height) {
        return check(x, y, baseX, baseY, baseX + width, baseY + height);
    }

    public static boolean check(double x, double y, double minX, double minY, double maxX, double maxY) {
        return minX <= x && x < maxX && minY <= y && y < maxY;
    }
}
