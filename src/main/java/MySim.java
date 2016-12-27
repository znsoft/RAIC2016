import model.*;

import java.util.ArrayList;

/**
 * Created by znsoft on 17.12.2016.
 */

public class MySim { //класс симулятор движения в одном из направлений к цели
    public boolean useLiAlgorithm = false;
    public Point2D mypoint;
    public Point2D ordtarget, target;
    public double speed;
    public ArrayList<Point2D> path;
    public int currStep;
    public int enemysCount;
    public ElectricField electricField;
    Wizard self;
    World world;
    //public LivingUnit near;


    public MySim(World world, Wizard self, Point2D target, ElectricField field, double speed) {
        this.target = target;
        ordtarget = target;
        this.self = self;
        this.world = world;
        mypoint = new Point2D(self.getX(), self.getY());
        this.speed = speed;
        path = new ArrayList<Point2D>();
        currStep = 0;
        enemysCount = 0;
        electricField = field;
        useLiAlgorithm = false;
    }

    public MySim(World world, Wizard self, Point2D target, ElectricField field, double speed, boolean useLi) {
        this.target = target;
        ordtarget = target;
        this.self = self;
        this.world = world;
        mypoint = new Point2D(self.getX(), self.getY());
        this.speed = speed;
        path = new ArrayList<Point2D>();
        currStep = 0;
        enemysCount = 0;
        electricField = field;
        useLiAlgorithm = useLi;
    }



    public int iter;
    double SPEEDMULTIPLE = 2.0D;

    public MySim Simulate(int iterations, Point2D centralTarget, int central) {

        iter = 0;
        SPEEDMULTIPLE = (double) iter + 1.0D;
        double mapSize = MyStrategy.game.getMapSize() - self.getRadius();
        while (iter < iterations) {
            if (iter == central && centralTarget != null) ordtarget = centralTarget;
            target = ordtarget;

            if (useLiAlgorithm) if (electricField == null) {
                iter = iterations;
                mypoint = null;
                break;
            } else {
                target = electricField.GetNextPoint(mypoint, 3);
                if (target == null) {
                    iter = iterations;
                    mypoint = null;
                    break;
                }

            }

            if (go(target)) break;
            if (mypoint.getX() < self.getRadius() || mypoint.getX() > mapSize || mypoint.getY() < self.getRadius() || mypoint.getY() > mapSize) {
                iter = iterations;
                mypoint = null;
                break;
            }
            if (StrictMath.hypot(centralTarget.getX() - mypoint.getX(), centralTarget.getY() - mypoint.getY()) <= speed)
                break;
            if (!correctMoveBounds(target)) {
                iter = iterations;
                mypoint = null;
                break;
            }
            //if(iter>iterations-2)firstStep = mypoint;

        }
        return this;
    }

    private boolean go(Point2D point) {
        iter++;
        if (point == null) return false;
        double angle = mypoint.getAngleTo(point.getX(), point.getY());
        double x = speed * StrictMath.cos(angle);
        double y = speed * StrictMath.sin(angle);
        x += mypoint.getX();
        y += mypoint.getY();


        //if (DEBUGSIMULATE) DebugLine(mypoint.getX(), mypoint.getY(), x, y, String.valueOf((double) iter / 20.0D));
        //DebugLine( x,  y, "1.0");
        mypoint = new Point2D(x, y);
        // if (x < self.getRadius() || x > mapSize || y < self.getRadius() || y > mapSize) return false;
        path.add(mypoint);
        if (x == target.getX() && y == target.getY()) return true;
        return false;
    }


    public MySim SetNewTarget(double angleOffset) {
        if (target == null) return this;
        double x = target.getX();
        double y = target.getY();
        double angle = mypoint.getAngleTo(x, y);
        double r = mypoint.getDistanceTo(x, y);
        angle += angleOffset;
        target = new Point2D(self.getX() + r * StrictMath.cos(angle), self.getY() + r * StrictMath.sin(angle));
        ordtarget = target;
        return this;
    }


    public boolean correctMoveBounds(Point2D targetPoint) {

        double x, y;
        if (targetPoint == null) return false;
        x = targetPoint.getX();
        y = targetPoint.getY();

        double distance = Double.MAX_VALUE;
        if (electricField == null) {
            distance = getBounce(world.getTrees(), x, y, distance);
            distance = getBounce(world.getBuildings(), x, y, distance);
            distance = getBounce(world.getMinions(), x, y, distance);
            distance = getBounce(world.getWizards(), x, y, distance);
        } else {

            ArrayList<LivingUnit> e = electricField.Get8CellsObjects(mypoint.getX(), mypoint.getY());
            //ElectricCell e = electricField.GetCell(mypoint.getX(),mypoint.getY());
            if (e == null || e.size() == 0) {
                //go(targetPoint);
            } else
                distance = getBounce(e.toArray(new LivingUnit[e.size()]), x, y, distance);

        }
        if (distance > 0) return true;
        return false;

    }

    private double getBounce(LivingUnit[] units, double x, double y, double distance) {
        for (LivingUnit o : units) {
            if (o instanceof Wizard && ((Wizard) o).isMe()) continue;
            if (self.getDistanceTo(o) > self.getVisionRange()) continue;
            //if (mypoint.getDistanceTo(self) > self.getVisionRange()) return -1.0D;
            if (mypoint.getDistanceTo(o) <= (o.getRadius() + self.getRadius())) return -1.0D;
            double dist = GetDistanceOnWay(x, y, o);
            if (distance > dist) {
                if (o.getFaction() == Faction.RENEGADES) enemysCount++;
                distance = dist;
                if(!useLiAlgorithm)correctBound(x, y, o);
            }
        }
        return distance;
    }


    private double GetDistanceOnWay(double x, double y, LivingUnit o) {
        double dist = mypoint.getDistanceTo(o);
        if (dist > self.getRadius() * MyStrategy.VIEW_DISTANCE) return Double.MAX_VALUE;
        if (!OnMyLine(mypoint, new Point2D(x, y), o)) return Double.MAX_VALUE;
        return dist;
    }


    private void correctBound(double x, double y, LivingUnit o) {
        double dist = mypoint.getDistanceTo(o);

        Point2D offset = mypoint.GetOffsetCorrectLine(new Point2D(x, y), o);
        if (offset == null) return;
        double absoluteAngleTo = StrictMath.atan2(offset.getY(), offset.getX());//  + self.getAngle();
        double r = o.getRadius() * MyStrategy.VIEW_DISTANCE * MyStrategy.BACK_VIEW_DISTANCE / (dist + 1.0D);// отталкивание от объекта в сторону воизбежаение столкновений
        go(new Point2D(o.getX() - r * StrictMath.cos(absoluteAngleTo) + o.getSpeedX() * SPEEDMULTIPLE, o.getY() - r * StrictMath.sin(absoluteAngleTo) + o.getSpeedY() * SPEEDMULTIPLE));
        //target = (new Point2D(o.getX() - r * StrictMath.cos(absoluteAngleTo), o.getY() - r * StrictMath.sin(absoluteAngleTo)));

    }


    private boolean OnMyLine(Point2D src, Point2D dst, CircularUnit obj) {
        Point2D offset = src.GetOffsetLine(dst, obj, self.getRadius());
        if (offset == null) return false;

        double h = StrictMath.hypot(offset.getX(), offset.getY());
//            return h < (obj.getRadius() + self.getRadius() * 1.2D);
        return h < (obj.getRadius() + self.getRadius() + 5);
    }


}

