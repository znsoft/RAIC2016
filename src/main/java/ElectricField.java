import model.*;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by znsoft on 17.12.2016.
 */

public class ElectricField {

    public ElectricCell[][] battleField;
    Point2D target;
    public int cellSize;
    World world;
    double mapSize;
    public int cells;
    public Point2D positiveP, negativeP, minNegativeP, nearLi, backZero;
    public boolean foundLi;
    Wizard self;

    public ElectricField(int cs, World world, Point2D target, Wizard self) {
        cellSize = cs;
        this.world = world;
        this.target = target;
        this.self = self;
        mapSize = MyStrategy.game.getMapSize();
        cells = (int) mapSize / cellSize;
        battleField = new ElectricCell[cells][cells];
        foundLi = false;
        UpdateField();
    }


    boolean IsInPoint(LivingUnit obj, double x, double y) {
        double r = obj.getDistanceTo(x, y);
        return r <= obj.getRadius() + self.getRadius() * 2;
    }


    boolean IsFieldDetectAt(LivingUnit obj, int xoffset, int yoffset) {
        double x = obj.getX();
        double y = obj.getY();
        int ix = (int) x / cellSize;
        int iy = (int) y / cellSize;
        ix += xoffset;
        iy += yoffset;
        if (ix < 0 || ix >= cells || iy < 0 || iy >= cells) return false;
        double x1 = (double) ix * cellSize;
        double y1 = (double) iy * cellSize;
        double x2 = x1 + (double) cellSize;
        double y2 = y1 + (double) cellSize;
        //double r = obj.getRadius();
        //double half = (double)cellSize/2.0D;
        return (IsInPoint(obj, x1, y1) || IsInPoint(obj, x2, y2) || IsInPoint(obj, x2, y1) || IsInPoint(obj, x1, y2));
    }

    public <T extends LivingUnit> void RecursiveFill(T obj, int xoffset, int yoffset) {
        if (xoffset != 0 || yoffset != 0) if (!IsFieldDetectAt(obj, xoffset, yoffset)) return;
        ElectricCell c = GetCell(obj, xoffset, yoffset);
        if (c != null && c.getLastObj() == obj) return;
        UpdateCell(obj, xoffset, yoffset);

        RecursiveFill(obj, xoffset - 1, yoffset - 1);  //x00
        RecursiveFill(obj, xoffset - 1, yoffset);      //x.0
        RecursiveFill(obj, xoffset - 1, yoffset + 1);  //x00
        RecursiveFill(obj, xoffset, yoffset - 1);       // 0xx
        RecursiveFill(obj, xoffset + 1, yoffset - 1);   // 00x
        RecursiveFill(obj, xoffset + 1, yoffset);       // 00x
        RecursiveFill(obj, xoffset + 1, yoffset + 1);  //
        RecursiveFill(obj, xoffset, yoffset + 1);      //  0x0
    }


    public ArrayList<LivingUnit> Get8CellsObjects(double x, double y) {
        int ix = (int) x / cellSize;
        int iy = (int) y / cellSize;
        ArrayList<LivingUnit> a = new ArrayList<>();

        GetObjects(ix, iy, a);
         /*   GetObjects(ix + 1, iy + 1, a);
            GetObjects(ix - 1, iy - 1, a);
            GetObjects(ix + 1, iy - 1, a);
            GetObjects(ix - 1, iy + 1, a);
            GetObjects(ix + 1, iy, a);
            GetObjects(ix - 1, iy, a);
            GetObjects(ix, iy - 1, a);
            GetObjects(ix, iy + 1, a);*/
        return a;
    }


    public ArrayList<LivingUnit> GetObjects(int x, int y, ArrayList<LivingUnit> dest) {
        ElectricCell e = GetCell(x, y);
        if (e == null || e.objects == null) return dest;
        for (LivingUnit l : e.objects)
            if (!dest.contains(l)) dest.add(l);
        return dest;
    }



    public ElectricCell GetCell(Point2D pos) {
        if (pos == null) return null;
        int ix = (int) pos.getX() / cellSize;
        int iy = (int) pos.getY() / cellSize;
        return GetCell(ix, iy);
    }


    public ElectricCell GetCell(double x, double y) {
        int ix = (int) x / cellSize;
        int iy = (int) y / cellSize;
        return GetCell(ix, iy);
    }


    public ElectricCell GetCell(int ix, int iy) {
        int cells = battleField.length;
        if (ix < 0 || ix >= cells || iy < 0 || iy >= cells) return null;
        return battleField[ix][iy];
    }

    public <T extends LivingUnit> ElectricCell GetCell(T obj) {
        return GetCell(obj.getX(), obj.getY());
    }

    public <T extends LivingUnit> ElectricCell GetCell(T obj, int xoffset, int yoffset) {
        double x = obj.getX();
        double y = obj.getY();
        int ix = (int) x / cellSize;
        int iy = (int) y / cellSize;
        ix += xoffset;
        iy += yoffset;
        if (ix < 0 || ix >= cells || iy < 0 || iy >= cells) return null;

        return GetCell(ix, iy);
    }

    public void SetCell(double x, double y, ElectricCell cell) {
        int ix = (int) x / cellSize;
        int iy = (int) y / cellSize;
        SetCell(ix, iy, cell);
    }



    public void SetCell(int ix, int iy, ElectricCell cell) {
        int cells = battleField.length;
        if (ix < 0 || ix >= cells || iy < 0 || iy >= cells) return;
        battleField[ix][iy] = cell;


    }


    public <T extends LivingUnit> void SetCell(T obj, ElectricCell cell, int xoffset, int yoffset) {
        if (cell == null) return;
        if (obj == null) return;
        double x = obj.getX();
        double y = obj.getY();
        int ix = (int) x / cellSize;
        int iy = (int) y / cellSize;
        ix += xoffset;
        iy += yoffset;
        if (ix < 0 || ix >= cells || iy < 0 || iy >= cells) return;
        SetCell(ix, iy, cell);
    }

    public <T extends LivingUnit> void SetCell(T obj, ElectricCell cell) {
        SetCell(obj.getX(), obj.getY(), cell);
    }


    public <T extends LivingUnit> void UpdateCell(T obj, int xoffset, int yoffset) {


        ElectricCell cell = GetCell(obj, xoffset, yoffset);
        if (cell == null)
            cell = new ElectricCell(obj);
        else
            cell.AddElectricCell(obj);
        SetCell(obj, cell, xoffset, yoffset);
    }


    public <T extends LivingUnit> void UpdateCell(T obj) {
        RecursiveFill(obj, 0, 0);
    }

    public void UpdateField() {


        for (Building b : world.getBuildings()) {
            UpdateCell(b);
        }

        for (Tree t : world.getTrees()) {
            UpdateCell(t);
        }

        if (target != null) {
            int x = (int) target.getX() / cellSize;
            int y = (int) target.getY() / cellSize;
            int tx = (int) self.getX() / cellSize;
            int ty = (int) self.getY() / cellSize;
            foundLi = CalcAStar(x,y, tx, ty);
        }


           /* for (Point2D b : enemyBuildings) {
                ElectricCell e = GetCell(b);
                if (e != null && e.getLastObj() != null) continue;
                e = new ElectricCell(-self.getVisionRange());
                SetCell(b.getX(), b.getY(), e);

            }*/



        for (Wizard w : world.getWizards()) {
            if (w.isMe()) continue;
            UpdateCell(w);

        }
        for (Minion m : world.getMinions()) {
            UpdateCell(m);
        }

        CalcField();


    }

    private void CalcField() {
        if(!MyStrategy.USEPOTENTIALFIELD)return;
        int maxy = (int) self.getVisionRange() * 2 / cellSize;
        int startX = (int) (self.getX() - self.getVisionRange()) / cellSize;
        int startY = (int) (self.getY() - self.getVisionRange()) / cellSize;
        int maxx = maxy;



        double half = cellSize / 2;
        double cellDiv = 1.0D - MyStrategy.SMOOTHFIELD / cellSize;
        double d, posdist = self.getVisionRange(), negdist = self.getVisionRange();
        double pos = 1.0D, neg = -1.0D, negP = -100.0D;
        double nearLiDist = Double.MAX_VALUE;
        Point2D p;
        positiveP = null;
        negativeP = null;
        minNegativeP = null;
        nearLi = null;
        backZero = null;

        int maxIterations = (int) self.getVisionRange() / MyStrategy.CELLSIZE +5;
        startX = 0;
        startY = 0;
        maxx = this.cells;
        maxy = this.cells;

        for (int i = 0; i < maxIterations; i++)
            for (int y = 0; y <= maxy; y++)
                for (int x = 0; x <= maxx; x++) {
                    int x1 = x + startX;
                    int y1 = y + startY;
                    ElectricCell e = GetCell(x1, y1);
                    //if (e != null && e.getLastObj() != null) continue;
                    //Получение суммы сил
                    e = GetSummField(x1, y1, e, cellDiv);
                    //if(!foundLi)e.path = 0;
                    SetCell(x1, y1, e);


                    if (i == maxIterations && e == null) continue;
                    p = new Point2D(x1 * cellSize + half, y1 * cellSize + half);
                    d = p.getDistanceTo(self);

                    if (e.path > 0 && d < nearLiDist) {
                        nearLi = p;
                        nearLiDist = d;
                    }


                    double vol = e.electricity;
                    if (vol > 0.0D) {


                    }


                    if (vol == 0.0D) continue;
                    //if (d > self.getVisionRange()) continue;
                    if (vol < neg && negdist > d) {
                        negdist = d;
                        negativeP = p;
                        neg = vol;
                    }
                    if (vol > pos && posdist > d) {
                        posdist = d;
                        positiveP = p;
                        pos = vol;
                    }

                    if (vol < -1.0D && vol > negP && negdist > d) {
                        negdist = d;
                        minNegativeP = p;
                        negP = vol;
                    }


                }
    }

    ElectricCell GetSummField(int x, int y, ElectricCell e, double cellDiv) {
        if (e == null) e = new ElectricCell(0.0D);

        double el = (e.electricity) + (GetElectricityCell(x, y + 1)) + GetElectricityCell(x, y - 1) + GetElectricityCell(x + 1, y) + GetElectricityCell(x - 1, y);

        e.electricity = el * cellDiv;

        return e;

    }

    double GetElectricityCell(int x, int y) {
        ElectricCell e = GetCell(x, y);
        if (e == null) return 0.0D;
        return e.electricity;
    }




    ElectricCell GetWithCoords(int x,int y){
        ElectricCell e = GetCell(x,y);
        if(e==null)e = new ElectricCell();
        e.x = x;
        e.y = y;
        e.ifBlocked = (e.getLastObj()!=null)||(x<0||y<0||x>cells||y>cells);
        SetCell(x,y,e);
        return e;
    }



    boolean CalcAStar(int x, int y, int goalx, int goaly){

        LinkedList<ElectricCell> openList = new LinkedList<>();
        LinkedList<ElectricCell> closedList = new LinkedList<>();
        LinkedList<ElectricCell> tmpList = new LinkedList<>();

        boolean found = false;
        boolean noroute = false;
        ElectricCell start = GetWithCoords(x,y);
        ElectricCell finish = GetWithCoords(goalx,goaly);

        openList.push(start);

        int i = MyStrategy.LIALGOLEN;

        while (!found && !noroute && i>0) {
            i--;
            //a) Ищем в открытом списке клетку с наименьшей стоимостью F. Делаем ее текущей клеткой.
            ElectricCell min = openList.getFirst();
            for (ElectricCell cell : openList) {
                // тут я специально тестировал, при < или <= выбираются разные пути,
                // но суммарная стоимость G у них совершенно одинакова. Забавно, но так и должно быть.
                if (cell.F < min.F) min = cell;
            }

            //b) Помещаем ее в закрытый список. (И удаляем с открытого)
            closedList.push(min);
            openList.remove(min);
            //System.out.println(openList);

            //c) Для каждой из соседних 8-ми клеток ...
            tmpList.clear();
            tmpList.add(GetWithCoords(min.x,     min.y - 1));
            tmpList.add(GetWithCoords(min.x + 1, min.y));
            tmpList.add(GetWithCoords(min.x,     min.y + 1));
            tmpList.add(GetWithCoords(min.x - 1, min.y));
            tmpList.add(GetWithCoords(min.x - 1, min.y - 1));
            tmpList.add(GetWithCoords(min.x + 1, min.y + 1));
            tmpList.add(GetWithCoords(min.x - 1 ,min.y + 1));
            tmpList.add(GetWithCoords(min.x + 1, min.y - 1));

            for (ElectricCell neightbour : tmpList) {
                //Если клетка непроходимая или она находится в закрытом списке, игнорируем ее. В противном случае делаем следующее.
                if (neightbour == null || neightbour.ifBlocked || closedList.contains(neightbour)) continue;

                //Если клетка еще не в открытом списке, то добавляем ее туда. Делаем текущую клетку родительской для это клетки. Расчитываем стоимости F, G и H клетки.
                if (!openList.contains(neightbour)) {
                    openList.add(neightbour);
                    neightbour.parent = min;
                    neightbour.H = neightbour.Mandist(finish);
                    neightbour.G = start.price(min);
                    neightbour.F = neightbour.H + neightbour.G;
                    continue;
                }

                // Если клетка уже в открытом списке, то проверяем, не дешевле ли будет путь через эту клетку. Для сравнения используем стоимость G.
                if (neightbour.G + neightbour.price(min) < min.G) {
                    // Более низкая стоимость G указывает на то, что путь будет дешевле. Эсли это так, то меняем родителя клетки на текущую клетку и пересчитываем для нее стоимости G и F.
                    neightbour.parent = min; // вот тут я честно хз, надо ли min.parent или нет
                    neightbour.H = neightbour.Mandist(finish);
                    neightbour.G = start.price(min);
                    neightbour.F = neightbour.H + neightbour.G;
                }

                // Если вы сортируете открытый список по стоимости F, то вам надо отсортировать свесь список в соответствии с изменениями.
            }

            //d) Останавливаемся если:
            //Добавили целевую клетку в открытый список, в этом случае путь найден.
            //Или открытый список пуст и мы не дошли до целевой клетки. В этом случае путь отсутствует.

            if (openList.contains(finish)) {
                found = true;
                finish.parent = min;
            }

            if (openList.isEmpty()) {
                noroute = true;
                return false;
            }
        }

        //3) Сохраняем путь. Двигаясь назад от целевой точки, проходя от каждой точки к ее родителю до тех пор, пока не дойдем до стартовой точки. Это и будет наш путь.
        if (!noroute&&found) {
            ElectricCell rd = finish;//.parent;
            int path = 0;
            //finish.path = path;
            while (rd!=null&&!rd.equals(start)) {
                path++;
                rd.road = true;
                rd.path = path;
                SetCell(rd.x,rd.y,rd);
                if(rd.equals(rd.parent))return false;
                rd = rd.parent;
                //if (rd == null) break;
            }

        } else {
            return false;
        }

        return !noroute && found;


    }



    public Point2D FindZeroField() {
        if (negativeP == null) return null;
        int maxy = (int) self.getVisionRange() * 3 / cellSize;
        int startX = (int) (self.getX() - self.getVisionRange() * 2) / cellSize;
        int startY = (int) (self.getY() - self.getVisionRange()) / cellSize;
        int maxx = maxy;
        double half = cellSize / 2;
        double zero = Double.MAX_VALUE;
        double dist = 0.0D;
        backZero = null;
        for (int y = 0; y <= maxy; y++)
            for (int x = 0; x <= maxx; x++) {
                int x1 = x + startX;
                int y1 = y + startY;
                ElectricCell e = GetCell(x1, y1);
                if (e == null) continue;
                if (e != null && e.getLastObj() != null) continue;
                double vol = e.electricity;
                if (vol < 0) continue;
                if (vol > zero) continue;
                Point2D p = new Point2D(x1 * cellSize + half, y1 * cellSize + half);
                double d = p.getDistanceTo(self);
                double de = p.getDistanceTo(negativeP);
                if (dist > de || de < d) continue;
                backZero = p;
                dist = de;
                zero = vol;

            }

        return backZero;
    }


    public Point2D GetElectricPoint(double electricLevel,Wizard self) {

        int maxy = (int) self.getVisionRange() * 2 / cellSize;
        int startX = (int) (self.getX() - self.getVisionRange()) / cellSize;
        int startY = (int) (self.getY() - self.getVisionRange()) / cellSize;
        double half = cellSize / 2;
        int maxx = maxy;

        double d, posdist = self.getVisionRange();
        double pos = 1.0D;
        Point2D p, posP = null;
        for (int y = 0; y <= maxy; y++)
            for (int x = 0; x <= maxx; x++) {
                int x1 = x + startX;
                int y1 = y + startY;
                ElectricCell e = GetCell(x1, y1);
                if (e != null && e.getLastObj() != null) continue;
                if (e == null) continue;
                double vol = e.electricity;
                if (vol == 0.0D) continue;
                p = new Point2D(x1 * cellSize + half, y1 * cellSize + half);
                d = p.getDistanceTo(self);
                if (d > self.getVisionRange()) continue;

                if (StrictMath.abs(vol - electricLevel) < StrictMath.abs(pos - electricLevel) && posdist > d) {
                    posdist = d;
                    posP = p;
                    pos = vol;
                }


            }
        return posP;

    }


    public Point2D GetNearZeroPoint(Wizard self) {
        int maxy = (int) self.getVisionRange() * 2 / cellSize;
        int startX = (int) (self.getX() - self.getVisionRange()) / cellSize;
        int startY = (int) (self.getY() - self.getVisionRange()) / cellSize;
        double half = cellSize / 2;
        int maxx = maxy;
        int xc, yc;
        double d, dist = self.getCastRange();
        Point2D p, res = null;
        for (int y = 0; y <= maxy; y++)
            for (int x = 0; x <= maxx; x++) {
                int x1 = x + startX;
                int y1 = y + startY;
                ElectricCell e = GetCell(x1, y1);
                if (e != null && e.getLastObj() != null) continue;
                if (e == null) continue;
                if (StrictMath.abs(e.electricity) < (double) cellSize) {
                    p = new Point2D(x1 * cellSize + half, y1 * cellSize + half);
                    d = p.getDistanceTo(self);

                    if (dist > d) {
                        res = p;
                        dist = d;
                    }
                }
            }
        return res;
    }


    public Point2D GetNextPoint(Point2D self, int num) {
        if (target == null||!foundLi) return null;
        int x = (int) self.getX() / cellSize;
        int y = (int) self.getY() / cellSize;
        ElectricCell e = GetCell(x, y);
        if (e == null || e.path == 0) return null;


        int start = e.path;
        int x1, y1;
        ElectricCell p = e.parent;
        if(p==null)




        for (int i = num; i > 0; i--) {
            //DebugCell(x, y, Double.toString((double)i/75));
            x1 = x;
            y1 = y;
            e = GetCell(x + 1, y);
            if (e != null && e.path > 0 && e.path > start) {
                start = e.path;
                x1 = x + 1;
                y1 = y;
            }
            e = GetCell(x - 1, y);
            if (e != null && e.path > 0 && e.path > start) {
                start = e.path;
                x1 = x - 1;
                y1 = y;
            }
            e = GetCell(x, y + 1);
            if (e != null && e.path > 0 && e.path > start) {
                start = e.path;
                x1 = x;
                y1 = y + 1;
            }
            e = GetCell(x, y - 1);
            if (e != null && e.path > 0 && e.path > start) {
                start = e.path;
                x1 = x;
                y1 = y - 1;
            }
            if (x == x1 && y == y1) break;
            x = x1;
            y = y1;

        }else
        {
            x = p.x;
            y = p.y;
        }


        double half = cellSize / 2.0D;
        double x2 = x * cellSize + half;
        double y2 = y * cellSize + half;
        return new Point2D(x2, y2);

    }




}

