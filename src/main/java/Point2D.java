import model.CircularUnit;
import model.Unit;

/**
 * Created by znsoft on 17.12.2016.
 */
public class Point2D {
    private final double x;
    private final double y;
    public boolean flag;

    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
        flag = false;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngleTo(double x, double y) {
        double absoluteAngleTo = StrictMath.atan2(y - this.y, x - this.x);
        double relativeAngleTo = absoluteAngleTo;

        while (relativeAngleTo > StrictMath.PI) {
            relativeAngleTo -= 2.0D * StrictMath.PI;
        }

        while (relativeAngleTo < -StrictMath.PI) {
            relativeAngleTo += 2.0D * StrictMath.PI;
        }

        return relativeAngleTo;
    }

    public Point2D GetOffsetCorrectLine(Point2D dst, CircularUnit obj) {

        double dx = dst.getX() - this.getX();
        double dy = dst.getY() - this.getY();
        double x = obj.getX() + obj.getSpeedX();
        double y = obj.getY() + obj.getSpeedY();
        if (StrictMath.abs(dx) > StrictMath.abs(dy)) {
            y = this.getY() + (x - this.getX()) * (dy) / (dx);
        } else {
            x = this.getX() + (y - this.getY()) * (dx) / (dy);
        }
        x += (dx > 0.0D ? -obj.getRadius() : obj.getRadius()) / 3;
        y += (dy > 0.0D ? -obj.getRadius() : obj.getRadius()) / 3;
        return new Point2D(obj.getX() - x + obj.getSpeedX(), obj.getY() - y + obj.getSpeedY());
    }

    public Point2D GetOffsetLine(Point2D dst, CircularUnit obj, double selfRadius) {

        double dx = dst.getX() - this.getX();
        double dy = dst.getY() - this.getY();
        double x = obj.getX() + obj.getSpeedX();//,obj.getX() + obj.getRadius(), obj.getX() - obj.getRadius()};
        double y = obj.getY() + obj.getSpeedY();
        if (StrictMath.abs(dx) > StrictMath.abs(dy)) {
            y = this.getY() + (x - this.getX()) * (dy) / (dx);
        } else {
            x = this.getX() + (y - this.getY()) * (dx) / (dy);
        }

        double r = selfRadius + obj.getRadius();

        double x1 = StrictMath.min(this.getX() - selfRadius, dst.getX() - r);
        double y1 = StrictMath.min(this.getY() - selfRadius, dst.getY() - r);
        double x2 = StrictMath.max(this.getX() + selfRadius, dst.getX() + r);
        double y2 = StrictMath.max(this.getY() + selfRadius, dst.getY() + r);
        if (x < x1 || x > x2 || y < y1 || y > y2) return null;

        return new Point2D(obj.getX() - x + obj.getSpeedX(), obj.getY() - y + obj.getSpeedY());
    }


    public double getDistanceTo(double x, double y) {
        return StrictMath.hypot(this.x - x, this.y - y);
    }

    public double getDistanceTo(Point2D point) {
        return getDistanceTo(point.x, point.y);
    }

    public double getDistanceTo(Unit unit) {
        return getDistanceTo(unit.getX(), unit.getY());
    }
}

