import model.*;

import java.util.ArrayList;



/**
 * Created by znsoft on 17.12.2016.
 */
public class ElectricCell {

        public boolean isEmpty;
        public int magnet;
        public int path;
        public ArrayList<LivingUnit> objects;
        public double electricity;
        private LivingUnit lastObj;


        public int x = -1;
        public int y = -1;
        public ElectricCell parent;// = this;
        //public boolean blocked = false;
        public boolean isStart = false;
        public boolean isFinish = false;
        public boolean road = false;
        public int F = 0;
        public int G = 0;
        public int H = 0;
        public boolean ifBlocked;

        public int Mandist(ElectricCell finish) {
            return 10 * (Math.abs(this.x - finish.x) + Math.abs(this.y - finish.y));
        }

        public int price(ElectricCell finish) {
            if (this.x == finish.x || this.y == finish.y) {
                return 10;
            } else {
                return 14;
            }
        }


        public void setAsStart() {
            this.isStart = true;
        }


        public void setAsFinish() {
            this.isFinish = true;
        }

        public boolean equals(ElectricCell second) {
            return (this.x == second.x) && (this.y == second.y);
        }

        public ElectricCell() {
            objects = new ArrayList<>();
            lastObj = null;
            electricity = 0;
        }

        public ElectricCell(double e) {
            objects = new ArrayList<>();
            electricity = e;
        }

        public ElectricCell(int e) {
            objects = new ArrayList<>();
            electricity = 0.0D;
            path = e;
        }


        public ElectricCell(Wizard obj) {
            objects = new ArrayList<>();
            electricity = 0;
            AddElectricCell(obj);
        }

        public ElectricCell(Tree obj) {
            objects = new ArrayList<>();
            electricity = 0;
            AddElectricCell(obj);
        }

        public ElectricCell(Minion obj) {
            objects = new ArrayList<>();
            electricity = 0;
            AddElectricCell(obj);
        }

        public ElectricCell(Building obj) {
            objects = new ArrayList<>();
            electricity = 0;
            AddElectricCell(obj);
        }

        public <T extends LivingUnit> ElectricCell(T obj) {
            objects = new ArrayList<>();
            electricity = 0;
            AddElectricCell(obj);
        }


        public void AddElectricCell(Wizard obj) {
            if (objects == null) objects = new ArrayList<>();
            double remainingActionCooldownTicks = (double) obj.getRemainingActionCooldownTicks();
            double attackRange = obj.getCastRange() * (obj.getFaction() == Faction.RENEGADES ? -1 : 1) - remainingActionCooldownTicks;
            electricity += attackRange;
            magnet += attackRange / MyStrategy.CELLSIZE;
            lastObj = obj;
            objects.add(obj);

        }

        public void AddElectricCell(Tree obj) {
            if (objects == null) objects = new ArrayList<>();
            electricity = 0;
            magnet = 0;
            lastObj = obj;
            objects.add(obj);
        }

        public void AddElectricCell(Minion obj) {
            if (objects == null) objects = new ArrayList<>();
            //electricity = 0;
            //double remainingActionCooldownTicks = (double) obj.getRemainingActionCooldownTicks();
            double attackRange;
            if (obj.getType() == MinionType.FETISH_BLOWDART)
                attackRange = MyStrategy.game.getFetishBlowdartAttackRange() - obj.getRemainingActionCooldownTicks();
            else
                attackRange = MyStrategy.game.getStaffRange();
            attackRange *= (obj.getFaction() == Faction.RENEGADES ? -1 : 1);
            this.electricity += attackRange;
            //magnet += (obj.getFaction() == Faction.RENEGADES ? -1 : 1);
            magnet += (obj.getFaction() == Faction.RENEGADES ? -1 : 1) * attackRange / MyStrategy.CELLSIZE;
            lastObj = obj;
            objects.add(obj);

        }

        public void AddElectricCell(Building obj) {
            if (objects == null) objects = new ArrayList<>();
            //double electricity = 0;
            double remainingActionCooldownTicks = (double) obj.getRemainingActionCooldownTicks();
            double attackRange = obj.getAttackRange() + obj.getRadius() - remainingActionCooldownTicks * MyStrategy.game.getWizardStrafeSpeed();
            electricity += (obj.getFaction() == Faction.RENEGADES ? -1 : 1) * attackRange;

            magnet += (obj.getFaction() == Faction.RENEGADES ? -1 : 1) * attackRange / MyStrategy.CELLSIZE;
            lastObj = obj;
            objects.add(obj);
        }


        public <T extends LivingUnit> void AddElectricCell(T obj) {
            if (obj instanceof Wizard) AddElectricCell((Wizard) obj);
            if (obj instanceof Building) AddElectricCell((Building) obj);
            if (obj instanceof Tree) AddElectricCell((Tree) obj);
            if (obj instanceof Minion) AddElectricCell((Minion) obj);
        }

        public LivingUnit getLastObj() {
            return lastObj;
        }

        public ElectricCell AddElectricCell(ElectricCell electricCell, double cellSize, boolean calcPath, double cellDiv)//, boolean isShadow)
        {
            if (electricCell == null) return this;
            //cellSize = 1.0D;
            //double delta = (StrictMath.abs(electricCell.electricity) < cellSize) ? 0.0D : electricCell.electricity - ((electricCell.electricity < 0) ? -cellSize : ((electricCell.electricity > 0) ? cellSize : 0.0D));
            //boolean priority = StrictMath.min(electricity, delta) < 0.0D ? true : false;
            //electricity = electricity==0.0D?delta:(electricity + delta)/2;
            //if()
            //electricity += delta;
            electricity = (electricity + electricCell.electricity) * cellDiv;
            magnet = (magnet + electricCell.magnet) / 2;

            //алгоритм Ли
            if (electricCell.path > 0 && calcPath) {
                if (this.path > 0) {
                    this.path = StrictMath.min(electricCell.path + 1, this.path);
                } else {
                    this.path = electricCell.path + 1;
                }
            }


            return this;
        }

        public ElectricCell CalcPath(ElectricCell electricCell){
            if (electricCell == null) return this;
            //алгоритм Ли
            if (electricCell.path > 0 ) {
                if (this.path > 0) {
                    this.path = StrictMath.min(electricCell.path - 1, this.path);
                } else {
                    this.path = electricCell.path - 1;
                }
            }


            return this;

        }

    }

