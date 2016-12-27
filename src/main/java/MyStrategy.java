import model.*;

import javax.swing.*;
import java.util.*;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.awt.Color;
import java.awt.Graphics;

//import javax.swing.JFrame;
//import javax.swing.JPanel;

public final class MyStrategy implements Strategy {
    public static final double WAYPOINT_RADIUS = 150.0D;
    public static final boolean DEBUG = true;
    public static final boolean DEBUGWINDOW = true;
    public static final boolean DEBUGNEXTPREV = true;
    public static final boolean DEBUGSIMULATE = false;
    public static final boolean DEBUG_FIELDLI_SIM = false;
    public static final boolean DEBUGSIMULATEOPTIMAL = false;
    public static final boolean DEBUGSIMULATEENEMY = false;
    public static final boolean DEBUG_FIELDLI = false;
    public static final boolean DEBUG_ZEROFIELD = false;
    public static final boolean DEBUG_POSITIVEFIELD = false;
    public static final int WINDOWSIZE = 800;


    public static final boolean USEPOTENTIALFIELD = true;

    public static final double GET_COVER_LOW_HP_FACTOR = 0.6;
    public static final double NORMAL_HP_FACTOR = 0.8;
    public static final double LOW_HP_FACTOR = 0.3D;
    public static final double ENEMY_LOW_HP_FACTOR = 0.25D;
    public static final double VERY_LOW_HP_FACTOR = 0.2D;

    public static final double MAXDISTANCETOBONUS = 1000.0D;

    public static final double VIEW_DISTANCE = 13.0D;
    public static final double BACK_VIEW_DISTANCE = 43.0D;
    public static final int BONUSTIME = 350;
    public static final int LIALGOLEN= 140;
    public static int CELLSIZE = 60;
    public static final double SMOOTHFIELD = 47.8D;

    public static final int SIMLOAD = 20;
    public static final double OFFSETANGLE = 0.355D; //угол между направлениями симуляции
    public double SIMSPEED = 5.0D;             //длина шага симуляции за итерацию
    public static final int SIMITERATIONS = 200; //шаги
    public static final int SIMVECTORS = 7; //количество векторов симуляции

    /**
     * Ключевые точки для каждой линии, позволяющие упростить управление перемещением волшебника.
     * <p>
     * Если всё хорошо, двигаемся к следующей точке и атакуем противников.
     * Если осталось мало жизненной энергии, отступаем к предыдущей точке.
     */
    private final Map<LaneType, Point2D[]> waypointsByLane = new EnumMap<>(LaneType.class);

    private Random random;
    private boolean myrandomBool;
    private int myrandomLen = 0;

    private LaneType lane;
    private Point2D[] waypoints;
    private Point2D[] enemyBuildings;
    ElectricField field;
    private MySim GSim;
    public Wizard self;
    public World world;
    static public Game game;
    private Move move;
    private Socket visSocket = null;
    private PrintWriter out;
    int skill = 0;
    Point2D targetPoint;
    boolean isTurn;
    Double targetAngle;
    Point2D[] bonuses;
    int bonustime = 0;
    Wizard attackWizard;
    Point2D myStartPos;

    Point2D returnFromBonus = null;

    /**
     * Основной метод стратегии, осуществляющий управление волшебником.
     * Вызывается каждый тик для каждого волшебника.
     *
     * @param self  Волшебник, которым данный метод будет осуществлять управление.
     * @param world Текущее состояние мира.
     * @param game  Различные игровые константы.
     * @param move  Результатом работы метода является изменение полей данного объекта.
     */
    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        initializeTick(self, world, game, move);
        initializeStrategy(self, game);

        if (DEBUG && visSocket == null) InitVisualiser();

        targetPoint = null;
        isTurn = false;
        targetAngle = null;
        Boolean isRetreat = new Boolean(false);
        Boolean isNewField = new Boolean(true);

        boolean isMoveNormal = MyMove(isRetreat, isNewField);

        targetPoint = GetSimulatedOptimal(targetPoint, SIMITERATIONS, !isMoveNormal, isRetreat, isNewField);

        AwayFromShot(self, world);
        _goTo();

        SkillUp();
        if (DEBUGNEXTPREV) DebugLine(firePlace, "0.27");
        if (DEBUG && visSocket != null && DEBUGWINDOW) {
            frame.repaint(100, 0, 0, WINDOWSIZE, WINDOWSIZE);
            frame.invalidate();
        }
        if (DEBUGNEXTPREV) DebugLine(getPreviousWaypoint(self), "3");
        if (DEBUGNEXTPREV) DebugLine(getNextWaypoint(self), "2");
        if (DEBUGNEXTPREV) DebugLine(returnFromBonus, "4");
    }

    private void SkillUp() {
        //Move move = ;
        for (SkillType s : fireball) {
            if (isSkillEnable(s))
                continue;
            move.setSkillToLearn(s);//.values()[skill++]);
            break;
        }
    }

    SkillType frostball[] = new SkillType[]{

            SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
            SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
            SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
            SkillType.MAGICAL_DAMAGE_BONUS_AURA_2,
            SkillType.FROST_BOLT,
            SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,  //усиление посоха
            SkillType.STAFF_DAMAGE_BONUS_AURA_1,
            SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
            SkillType.STAFF_DAMAGE_BONUS_AURA_2,
            SkillType.FIREBALL,
            SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
            SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
            SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
            SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
            SkillType.SHIELD,
            SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
            SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
            SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2

    };

    SkillType fireball[] = new SkillType[]{
            SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,  //усиление посоха
            SkillType.STAFF_DAMAGE_BONUS_AURA_1,
            SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
            SkillType.STAFF_DAMAGE_BONUS_AURA_2,
            SkillType.FIREBALL,
            SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
            SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
            SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
            SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
            SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
            SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
            SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
            SkillType.SHIELD,
            SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
            SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
            SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
            SkillType.MAGICAL_DAMAGE_BONUS_AURA_2,
            SkillType.FROST_BOLT

    };

    boolean isWeStop() {
        return (StrictMath.abs(self.getSpeedX()) < 0.1D && StrictMath.abs(self.getSpeedY()) < 0.1D);
    }

    private void AwayFromShot(Wizard self, World world) {
        for (Projectile shot : world.getProjectiles()) {
            if (shot.getOwnerUnitId() == self.getId()) continue;
            double dist = shot.getDistanceTo(self);// - self.getRadius();
            if (dist > self.getVisionRange()) continue;
            double angleTo = StrictMath.abs(shot.getAngleTo(self));
            if (angleTo > game.getStaffSector()) continue;
            double angle = shot.getAngle();
            if (dist < self.getRadius() + 5) Attack(shot);
            double x = shot.getX() + dist * StrictMath.cos(angle);
            double y = shot.getY() + dist * StrictMath.sin(angle);
            double dx = x - self.getX();
            double dy = y - self.getY();
            if (StrictMath.hypot(dx, dy) < 0.1D) {
                dx = StrictMath.sin(angle);
                dy = StrictMath.cos(angle);
            }

            Point2D away = new Point2D(self.getX() - dx * dist, self.getY() - dy * dist);
            double ang = self.getAngleTo(away.getX(), away.getY());

            if (StrictMath.abs(ang) < game.getStaffSector() * 2)
                goTo(away);
            else goBack(away);

            if (DEBUGNEXTPREV) DebugLine(shot.getX(), shot.getY(), x, y, "2.1");
            if (DEBUGNEXTPREV) DebugLine(targetPoint, "-10");
            //   return true;
        }
    }



    private boolean MyMove(Boolean isRetreat, Boolean isNewField) {
        isRetreat = false;
        //randMove(game, move);
        move.setStrafeSpeed(random.nextBoolean() ? game.getWizardStrafeSpeed() : -game.getWizardStrafeSpeed());
        GoToCover();

        // if ((int) (self.getRemainingActionCooldownTicks() / 6) > 0 || self.getRemainingCooldownTicksByAction()[2] > 0)
        //   goBack(getPreviousWaypoint());
        LivingUnit nearestTarget = getNearestTarget(self);
        LivingUnit optimalTarget = getOptimalTarget(self);


        if (GoToBonusPlace(optimalTarget)) return true;

        boolean followLowTarget = (optimalTarget != null && optimalTarget instanceof Wizard && optimalTarget.getLife() < optimalTarget.getMaxLife() * VERY_LOW_HP_FACTOR);


        Boolean x = GetAwayFromNearest(nearestTarget, followLowTarget);
        if (x != null) return x;

        boolean a = AttackTarget(optimalTarget, !followLowTarget);

        // Если осталось мало жизненной энергии, отступаем к предыдущей ключевой точке на линии.
        if (IfWeLowHP()) {
            isRetreat = true;
            return false;
        }

        if (GetAwayFromBuildGuard()) return true;

        if (GoToFollowWizard(optimalTarget, followLowTarget)) return true;
       /* if (firePlace != null && self.getRemainingActionCooldownTicks() > 0) {
            GoToCover();
            return true;
        }
        if (self.getRemainingActionCooldownTicks() > 10)
            goBack(GetBack());
*/
        if (UsePotentialField()){
            //isNewField = false;
            return true;
        }

        if (a) return true;//we in cast range

        IfWeStoped();

/*        if (optimalTarget != null && optimalTarget instanceof Wizard && optimalTarget.getLife() <= optimalTarget.getMaxLife() * ENEMY_LOW_HP_FACTOR) {
            goTo(new Point2D(optimalTarget.getX(), optimalTarget.getY()));
            return true;
        }
*/
        if (GoReturnFromBonus()) return true;

        if (nearestTarget == null && optimalTarget == null) goTo(getNextWaypoint(self));
        else goBack(getNextWaypoint(self));
        return true;
    }


    //if (UsePotentialField(nearestTarget, optimalTarget)) return true;
    private boolean UsePotentialField() {
        if (!USEPOTENTIALFIELD) return false;
        if (firePlace == null && field != null && field.negativeP != null) {
//            if (firePlace == null && field != null && self.getRemainingActionCooldownTicks() > 1 && field.negativeP != null) {

//            if (firePlace == null && nearestTarget != null && optimalTarget != null && field != null && self.getRemainingActionCooldownTicks() > 5 && field.positiveP != null && field.negativeP != null) {
            double pos = 0.0D, neg = 0.0D;
            ElectricCell e = field.GetCell(field.positiveP);
            // if (DEBUG_POSITIVEFIELD) field.DrawBox(field.negativeP, "0");
            if (e != null) pos = e.electricity;
            e = field.GetCell(field.negativeP);
            if (e != null) neg = e.electricity;

            Point2D point = field.positiveP;//field.GetElectricPoint(pos / 2);

            if (pos < 1.0D && neg < -1.0D) point = GetBack();
            //if (DEBUG_POSITIVEFIELD) field.DrawBox(point, "0");

            if (StrictMath.abs(neg) > pos) {
                goBack(point);
                return true;
            }
        }
        return false;
    }

    private boolean GoReturnFromBonus() {
        //Wizard self = ;
        if (returnFromBonus != null && bonustime == 0) {
            goTo(returnFromBonus);

            if (returnFromBonus.getDistanceTo(self) <= WAYPOINT_RADIUS * 2) {
                returnFromBonus = null;
            }
            return true;
        }
        return false;
    }

    private boolean GoToFollowWizard(LivingUnit optimalTarget, boolean followLowTarget) {
        if (followLowTarget && optimalTarget != null) {
            goTo(new Point2D(optimalTarget.getX(), optimalTarget.getY()));
            return true;
        }
        return false;
    }

    private boolean GetAwayFromBuildGuard() {
        Point2D noBuild = GetSavePointNearBuilding();
        if (noBuild != null) {
            goBack(noBuild);
            if (DEBUGNEXTPREV) DebugLine(noBuild, "0");
            return true;
        }
        return false;
    }

    private Boolean GetAwayFromNearest(LivingUnit nearestTarget, boolean followLowTarget) {
        //Wizard self = ; Move move = ;
        if (nearestTarget != null) {
            double distance = self.getDistanceTo(nearestTarget);
            double angle = self.getAngleTo(nearestTarget);
            // ... то поворачиваемся к цели.

            if (distance < self.getRadius() + nearestTarget.getRadius() + 4) {
                move.setTurn(angle);
                Attack(nearestTarget);
                goBack(getPreviousWaypoint(self));
                //goBack(GetBack());
                return false;
            }


            if (distance <= (followLowTarget ? 200 - self.getLife() : 350)) {
                AttackTarget(nearestTarget, false);
                goBack(getPreviousWaypoint(self));
                //goBack(GetBack());
                return true;
            }

        }
        return null;
    }

    private boolean GoToBonusPlace(LivingUnit optimalTarget) {
        // Wizard self = ; Game game = ;
        int tick = world.getTickIndex();
        if ((tick % game.getBonusAppearanceIntervalTicks()) == (game.getBonusAppearanceIntervalTicks() - BONUSTIME / 2)) {
            bonustime = BONUSTIME;
            returnFromBonus = new Point2D(self.getX(), self.getY());
            //returnFromBonus = GetBack();
        }
        Bonus bonus = GetBonus();

        if (bonus != null)
            if (bonus.getDistanceTo(self) < self.getVisionRange()) {
               // if (bonustime == 0) returnFromBonus = new Point2D(self.getX(), self.getY());
                bonustime = 3;
            }

        if (bonustime > 0) {
            Point2D nearestBonus = getNearestBonus(self);
            if (bonus != null) {
                nearestBonus = new Point2D(bonus.getX(), bonus.getY());
                bonustime = 3;
            } else bonustime--;


            if (nearestBonus != null)
                if (nearestBonus.getDistanceTo(self) > self.getRadius()) goBack(nearestBonus);
                else {
                    bonustime = 0;
                    //returnFromBonus = null;
                }

            if (nearestBonus == null) {
                //returnFromBonus = null;
                bonustime = 0;
            }
            if (AttackTarget(optimalTarget, false)) goBack(nearestBonus);
            else goTo(nearestBonus);

            if (nearestBonus != null) return true;
        }
        return false;
    }

    private void GoToCover() {
        // Wizard self = ;
        if (firePlace != null) {
            goTo(firePlace);
            double dist = firePlace.getDistanceTo(self);
            double r = self.getRadius();
            if (dist < r / 3 || dist > self.getCastRange())
                firePlace = null;
            if (self.getRemainingActionCooldownTicks() == 0) firePlace = null;

        }
    }


    Point2D GetBack() {
        Point2D back;
        if (USEPOTENTIALFIELD && field != null) {
            back = field.FindZeroField();
            if (back != null) return back;
        }


        back = getPreviousWaypoint(self);
        double dx = self.getX() - back.getX();
        double dy = self.getY() - back.getY();
        back = new Point2D(self.getX() - dx, self.getY() - dy);
        // DebugLine(back, "0.57");

        List<LivingUnit> targets = new ArrayList<>();
        targets.addAll(Arrays.asList(world.getWizards()));
        targets.addAll(Arrays.asList(world.getMinions()));

        for (LivingUnit target : targets) {

            if (target.getFaction() == this.self.getFaction() || target.getFaction() == Faction.NEUTRAL)
                continue;
            double dist = self.getDistanceTo(target);
            if (dist > this.self.getVisionRange() || dist == 0.0D) continue;
            dist = dist / (self.getRadius() + target.getRadius() + 4);
            dx = dx + (target.getX() - self.getX()) / dist;
            dy = dy + (target.getY() - self.getY()) / dist;
        }
        back = new Point2D(self.getX() - dx, self.getY() - dy);
        DebugLine(back, "0.67");
        return back;//getPreviousWaypoint(self);
    }


    private boolean IfWeLowHP() {

        if (self.getLife() < self.getMaxLife() * LOW_HP_FACTOR) {


            // if (firePlace!=null)return false;

            //goBack(getPreviousWaypoint(self));
            goBack(GetBack());
            return true;
        }
        return false;
    }

    private void IfWeStoped() {
        if (isWeStop()) {
            Tree t = GetNearTree();
            if (t != null) {
                double angle = self.getAngleTo(t);
                move.setTurn(angle);
                if (StrictMath.abs(angle) < game.getStaffSector() / 2.0D) {
                    move.setAction(ActionType.STAFF);
                    //    goBack(getPreviousWaypoint(self));
                }
            }


        }
    }

    final double SPEEDMULTIPLE = 2.0D;

    private boolean AttackTarget(LivingUnit optimalTarget, boolean isCovering) {
        //null = next command  true = return true  false =
        // Если видим противника ...
        if (optimalTarget != null) {
            Point2D targetPoint = new Point2D(optimalTarget.getX() + optimalTarget.getSpeedX() * SPEEDMULTIPLE, optimalTarget.getY() + optimalTarget.getSpeedY() * SPEEDMULTIPLE);
            //Point2D selfPoint = new Point2D(self.getX(),self.getY());

            double distance = self.getDistanceTo(optimalTarget) - game.getMagicMissileRadius();
            double angle = self.getAngleTo(targetPoint.getX(), targetPoint.getY());
            // ... то поворачиваемся к цели.
            // ... и он в пределах досягаемости наших заклинаний, ...
            if (self.getRemainingActionCooldownTicks() < 4) {
                move.setTurn(angle);
            }
            if (distance <= self.getCastRange()) {

                // Если цель перед нами, ...
                if (StrictMath.abs(angle) < game.getStaffSector() / 2.0D) {
                    // ... то атакуем.
                    move.setCastAngle(angle);
                    //move.setMinCastDistance(distance - optimalTarget.getRadius() + game.getMagicMissileRadius());
                    move.setMinCastDistance(distance + game.getMagicMissileRadius());
                    //if (isCovering && TreeOnLine(self, new Point2D(optimalTarget.getX(), optimalTarget.getY())) != null)return false;
                    if (!Attack(optimalTarget)) {
                        //  if(field != null)goBack(field.GetNearZeroPoint()); else
                        if (self.getLife() < self.getMaxLife() * GET_COVER_LOW_HP_FACTOR && firePlace == null)
                            goBack(GetBack());
                        // if (isCovering && optimalTarget instanceof Wizard && self.getLife() > self.getMaxLife() * LOW_HP_FACTOR)
                        //GoToCoverPlace(optimalTarget)
//                            if (self.getRemainingActionCooldownTicks() > 0) {
                        //                              if (GoToCoverPlace(optimalTarget)) return true;
                        //                        } else {
                        //                         // if (GoToUnCoverPlace(optimalTarget)) return true;
                        //                    }
                    } else {
//&& (self.getLife() < self.getMaxLife() * GET_COVER_LOW_HP_FACTOR)
                        if (isCovering && optimalTarget instanceof Wizard) {
                            GoToCoverPlace(optimalTarget);


                        }
                    }

                }
                return true;
            }
        }
        return false;
    }


    Point2D GetSavePointNearBuilding() {
        Building build = null;
        double dist = Double.MAX_VALUE;
        //Получим точку предположительной башни
        Point2D buildPoint = null;
        for (Point2D preBuild : enemyBuildings) {
            double l = preBuild.getDistanceTo(self);
            if (l < dist) {
                dist = l;
                buildPoint = preBuild;
            }
        }
        double buildDist = Double.MAX_VALUE;
        double attackRange = self.getVisionRange();
        for (Building b : world.getBuildings()) {
            if (b.getFaction() == self.getFaction()) continue;
            double l = b.getDistanceTo(self);
            if (l < buildDist) {
                build = b;
                buildDist = l;
            }
        }

        double remainingActionCooldownTicks = 0;


        if (build != null) {
            dist = buildDist;
            if (buildPoint.getDistanceTo(build) < 200.0D) buildPoint.flag = true;
            remainingActionCooldownTicks = (double) build.getRemainingActionCooldownTicks();
            buildPoint = new Point2D(build.getX(), build.getY());
        } else if (buildPoint.flag) return null;
        if (buildPoint == null) return null;
        dist -= (self.getRadius() + SIMSPEED * SIMLOAD);
        if (dist > (double) (attackRange - remainingActionCooldownTicks * game.getWizardBackwardSpeed())) return null;

        double angle = buildPoint.getAngleTo(self.getX(), self.getY());
        return new Point2D(buildPoint.getX() + 2 * dist * StrictMath.cos(angle), buildPoint.getY() + 2 * dist * StrictMath.sin(angle));
    }

    boolean GetAwayFromBuildings() {
        Point2D b = GetSavePointNearBuilding();
        if (b == null) return false;
        DebugLine(b, "0");
        goBack(b);
        return true;
    }


    boolean isSkillEnable(SkillType skill) {
        if (!game.isSkillsEnabled()) return false;
        SkillType skills[] = self.getSkills();
        for (SkillType s : skills) {
            if (s == skill) return true;
        }
        return false;
    }


    private boolean Attack(CircularUnit target) {
        int r[] = self.getRemainingCooldownTicksByAction();
        ActionType a = ActionType.NONE;

        double distance = self.getDistanceTo(target);

        if (distance <= game.getStaffRange()) a = ActionType.STAFF;

        if (r[ActionType.MAGIC_MISSILE.ordinal()] == 0 && self.getMana() >= game.getMagicMissileManacost() && r[ActionType.MAGIC_MISSILE.ordinal()] == 0)
            a = ActionType.MAGIC_MISSILE;
        if (isSkillEnable(SkillType.FROST_BOLT) && self.getMana() >= game.getFrostBoltManacost() && r[ActionType.FROST_BOLT.ordinal()] == 0)
            a = ActionType.FROST_BOLT;
        if (isSkillEnable(SkillType.FIREBALL) && distance >= game.getStaffRange() + self.getRadius() * 2 && self.getMana() >= game.getFireballManacost() && r[ActionType.FIREBALL.ordinal()] == 0)
            a = ActionType.FIREBALL;
        if (target instanceof Tree) a = ActionType.STAFF;

        if (target instanceof Projectile) a = ActionType.SHIELD;


        move.setAction(a);
        return (a != ActionType.NONE);

    }

    Point2D getNearestBonus(CircularUnit self) {
        double nearestBonusDistance = MAXDISTANCETOBONUS;
        Point2D bonus = null;
        for (Point2D b : bonuses) {
            double dist = self.getDistanceTo(b.getX(), b.getY());
            int treecount = 0;
            for (Tree tree : world.getTrees()) {
                treecount += OnMyLine(self, b, tree) ? 1 : 0;
            }
            if (dist < nearestBonusDistance && treecount < 3) {
                nearestBonusDistance = dist;
                bonus = b;
            }

        }
        return bonus;

    }


    Bonus GetBonus() {
        for (Bonus b : world.getBonuses()) {
            return b;
        }
        return null;
    }

    Tree GetNearTree() {
        Tree tree = null;

        double dist = WAYPOINT_RADIUS + self.getRadius();
        for (Tree t : world.getTrees()) {
            double dis = self.getDistanceTo(t);
            if (dis < dist) {
                dist = dis;
                tree = t;
            }
        }
        if (tree == null) return null;
        if (dist <= self.getRadius() + tree.getRadius() + 4) return tree;
        return null;
    }


    public final static double OFFSETANGLEVIEWENEMYSIM = 0.3;
    public final static int ENEMYSIMULATIONVECTORS = 9;
    public final static int ENEMYSIMLOAD = 5;
    Point2D firePlace;
    Point2D uncoverPlace;

    boolean GoToCoverPlace(LivingUnit target) {
       /* int tick = world.getTickIndex();///(int)SIMSPEED;
        if (firePlace != null && tick % ENEMYSIMLOAD == 0) {
            if (firePlace.getDistanceTo(target) < self.getCastRange() + self.getRadius()) {
                firePlace = null;
                return false;
            }
            goTo(firePlace);
            if (DEBUGSIMULATEENEMY) DebugLine(firePlace, "0");

            return true;
        }
*/
        if (firePlace == null)
            firePlace = FindOptimalFirePlace(target);

        if (firePlace == null) {
            double dist = (random.nextBoolean() ? 1 : -1) * self.getRadius() * 4 + game.getMagicMissileRadius() * 2 + 1;
            firePlace = new Point2D(self.getX() - dist * StrictMath.cos(self.getAngle() + StrictMath.PI / 2), self.getY() - dist * StrictMath.sin(self.getAngle() + StrictMath.PI / 2));
        }


        if (firePlace != null) {
            // field = new ElectricField(CELLSIZE, world, firePlace);
            goTo(firePlace);
            GSim = (new MySim(world,self, targetPoint, field, 10)).Simulate(40, targetPoint, 0);
            if (GSim.iter == 40) firePlace = null;
            return true;
        }
        return false;
    }

    boolean GoToUnCoverPlace(LivingUnit target) {
        int tick = world.getTickIndex();///(int)SIMSPEED;
        if (uncoverPlace != null && tick % ENEMYSIMLOAD == 0) {
            goBack(uncoverPlace);
            if (DEBUGSIMULATEENEMY) DebugLine(uncoverPlace, "0");

            return true;
        }

        uncoverPlace = FindOptimalUncoverPlace(firePlace, target);
        if (uncoverPlace != null) {
            goBack(uncoverPlace);
            return true;
        }
        return false;
    }


    Point2D FindOptimalUncoverPlace(Point2D coverPlace, LivingUnit target) {
        if (coverPlace == null || target == null) return null;
        if (!(target instanceof Wizard)) return null;
        Tree tree = TreeOnLine(self, new Point2D(target.getX(), target.getY()));
        if (tree == null) return null;
        Point2D offset = new Point2D(target.getX(), target.getY()).GetOffsetCorrectLine(new Point2D(self.getX(), self.getY()), tree);
        if (offset == null) return null;

        double dist = target.getDistanceTo(tree);
        double absoluteAngleTo = StrictMath.atan2(offset.getY(), offset.getX());//  + self.getAngle();
        double angle2 = absoluteAngleTo + StrictMath.PI;
        double r = tree.getRadius() * VIEW_DISTANCE * BACK_VIEW_DISTANCE / (dist + 1.0D);
        //DebugLine(o.getX(), o.getY(), o.getX() - r * StrictMath.cos(absoluteAngleTo), o.getY() - r * StrictMath.sin(absoluteAngleTo), "0.0");
        List<Point2D> points = new ArrayList<>();
        points.add(new Point2D(tree.getX() - r * StrictMath.cos(absoluteAngleTo), tree.getY() - r * StrictMath.sin(absoluteAngleTo)));
        points.add(new Point2D(tree.getX() - r * StrictMath.cos(angle2), tree.getY() - r * StrictMath.sin(angle2)));
        Point2D result = null;
        for (Point2D p : points) {
            double d = p.getDistanceTo(self);
            if (TreeOnLine((Wizard) target, p) == null && dist > d) {
                dist = d;
                result = p;
            }
        }

        return result;
    }


    Point2D FindOptimalFirePlace(LivingUnit fromTarget) {
        List<Point2D> points = new ArrayList<>();
        double r = self.getCastRange();
        Double[] angles = new Double[ENEMYSIMULATIONVECTORS];
        double nachalo = -(double) ((int) (ENEMYSIMULATIONVECTORS / 2) + 1) * OFFSETANGLEVIEWENEMYSIM;
        for (int i = 0; i <= angles.length - 1; i++)
            angles[i] = nachalo += OFFSETANGLE;
        if (firePlace != null) points.add(firePlace);
        int wizCount = 0;
        for (Wizard w : world.getWizards()) {

            if (w.getFaction() == self.getFaction()) continue;
            double dist = w.getDistanceTo(self);
            if (dist > self.getVisionRange()) continue;
            wizCount++;
            //if (dist < r) continue;
            double startAngle = w.getAngleTo(self);
            List<Point2D> tmPoints = new ArrayList<>();

            for (Point2D target : points)
                if (TreeOnLine(w, target) == null) tmPoints.add(target);

            points.removeAll(tmPoints);

            tmPoints = new ArrayList<>();
            for (Double angle : angles) {
                Point2D target = new Point2D(w.getX() - r * StrictMath.cos(angle + startAngle), w.getY() - r * StrictMath.sin(angle + startAngle));
                if (DEBUGSIMULATEENEMY) DebugLine(w.getX(), w.getY(), target.getX(), target.getY(), "0");
                if (target.getDistanceTo(self) > r) continue;
                if (TreeOnLine(w, target) == null) continue;
                tmPoints.add(target);

            }
            points.addAll(tmPoints);
        }
        if (wizCount < 2) return null;

        double minDist = Double.MAX_VALUE;
        Point2D bestPoint = null;
        for (Point2D target : points) {
            double l = target.getDistanceTo(self);
            if (l < minDist) {
                minDist = l;
                bestPoint = target;
            }

        }


        return bestPoint;
    }


    public class MyPaint extends javax.swing.JPanel {

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (field == null) return;
            DrawField(g);

        }


        public void DrawField(Graphics g) {

            int wb = (int) (g.getClipBounds().getWidth() / field.cells);
            int hb = (int) (g.getClipBounds().getHeight() / field.cells);


            for (int y = 0; y <= field.cells; y++)
                for (int x = 0; x <= field.cells; x++) {
                    ElectricCell e = field.GetCell(x, y);
                    if (e == null) continue;
                    //if(e.getLastObj()!=null)continue;

                    g.setColor(Color.LIGHT_GRAY);
                    if(e.road||e.x>0||e.y>0)g.fillRect(x * wb, y * hb, wb, hb);


                    g.setColor(Color.BLACK);
                    if (e.getLastObj() != null ) {
                        g.drawRect(x * wb, y * hb, wb, hb);
                        if(e.electricity==0.0D)continue;

                    }

                    if (StrictMath.abs(e.electricity) < 1.0D) continue;

                    int r = e.electricity < 0 ? 255 : (int) StrictMath.abs(e.electricity);
                    int gr = (int) StrictMath.abs(100 - e.electricity);
                    int b = (int) StrictMath.abs(e.electricity * 30);
                    g.setColor(new Color(r % 255, gr % 255, b % 255));

                    g.fillRect(x * wb, y * hb, wb, hb);

                }

if(!field.foundLi)return;

            Point2D slf = new Point2D(self.getX(), self.getY());
            for(int i = LIALGOLEN;i>0;i--){
                slf = field.GetNextPoint(slf,2);
                if(slf==null)break;
                g.setColor(Color.RED);
                g.fillRect((int)slf.getX()/field.cellSize * wb, (int)slf.getY()/field.cellSize * hb, wb, hb);

            }

            ElectricCell e = field.GetCell(field.target);
            for(int i = LIALGOLEN;i>0;i--) {
                if(e==null)break;
                g.setColor(Color.GREEN);
                g.fillRect(e.x * wb, e.y * hb, wb, hb);
                e = e.parent;
            }

        }


    }

    javax.swing.JFrame frame;

    void InitLocalVisualWindow() {
        javax.swing.JPanel paintTester = new MyPaint();
        frame = new javax.swing.JFrame();

        int cells = WINDOWSIZE;
        frame.setSize(cells, cells);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.add(paintTester);
        // frame.pack();
        frame.setVisible(true);


    }


    void InitVisualiser() {
        if (!DEBUG) return;
        try {

            visSocket = new Socket("127.0.0.1", 8901);
            visSocket.setSendBufferSize(1000);
            out = new PrintWriter(visSocket.getOutputStream(), true);
            if (DEBUGWINDOW) InitLocalVisualWindow();

        } catch (Exception e) {

        }
    }


    void DebugLine(String lines) {
        if (!DEBUG || visSocket == null || out == null || lines.isEmpty()) return;
        if (!visSocket.isConnected()) return;

        out.println(lines);
        out.flush();
        try {
            out.wait();
        } catch (Exception e) {
        }
        //try {out = new PrintWriter(visSocket.getOutputStream(), true);        } catch (Exception e){}


        try {

            Thread.yield();//Thread.sleep(1);
        } catch (Exception e) {
        }
    }

    void DebugLine(double x, double y, double x1, double y1, String color) {
        String lines = Double.toString(x) + "," + Double.toString(y) + "," + Double.toString(x1) + "," + Double.toString(y1) + "," + color;
        DebugLine(lines);
    }

    void DebugLine(double x1, double y1, String color) {
        String lines = Double.toString(self.getX()) + "," + Double.toString(self.getY()) + "," + Double.toString(x1) + "," + Double.toString(y1) + "," + color;
        DebugLine(lines);
    }

    void DebugLine(LivingUnit l, String color) {
        DebugLine(l.getX(), l.getY(), color);
    }

    void DebugLine(Point2D l, String color) {
        if (l == null) return;
        DebugLine(l.getX(), l.getY(), color);
    }

    void DebugBox(double x1, double y1, double x2, double y2, String color) {
        //DebugLine(x1, y1, x2, y2, color);
        DebugLine(x1, y1, x1, y2, color);
        DebugLine(x1, y1, x2, y1, color);
        //DebugLine(x1, y2, x2, y2, color);
        //DebugLine(x2, y1, x2, y2, color);
    }


    /**
     * Инциализируем стратегию.
     * <p>
     * Для этих целей обычно можно использовать конструктор, однако в данном случае мы хотим инициализировать генератор
     * случайных чисел значением, полученным от симулятора игры.
     */
    private void initializeStrategy(Wizard self, Game game) {
        if (random == null) {
            random = new Random(game.getRandomSeed());

            double mapSize = game.getMapSize();
            //CELLSIZE = (int) self.getRadius();//*2;
            Object[] b = Arrays.stream(world.getBuildings()).filter(x -> x.getFaction() == self.getFaction()).map(x -> new Point2D(mapSize - x.getX(), mapSize - x.getY())).toArray();
            enemyBuildings = Arrays.asList(b).toArray(new Point2D[b.length]);

            myStartPos = new Point2D(self.getX(), self.getY());

            bonuses = new Point2D[]{
                    new Point2D(mapSize * 0.3D, mapSize * 0.3D),
                    new Point2D(mapSize * 0.7D, mapSize * 0.7D)};

            waypointsByLane.put(LaneType.MIDDLE, new Point2D[]{
                    new Point2D(100.0D, mapSize - 100.0D),
                    //new Point2D(300.0D, mapSize - 300.0D),
                    //new Point2D(800.0D, mapSize - 800.0D),
                   // new Point2D(2400.0D, 2400.0D),
                    new Point2D(mapSize - 600.0D, 600.0D)
            });

            waypointsByLane.put(LaneType.TOP, new Point2D[]{
                    new Point2D(100.0D, mapSize - 100.0D),
                    new Point2D(100.0D, mapSize - 400.0D),
                    new Point2D(200.0D, mapSize - 800.0D),
                    new Point2D(200.0D, mapSize * 0.75D),
                    new Point2D(200.0D, mapSize * 0.5D),
                    new Point2D(200.0D, mapSize * 0.25D),
                    new Point2D(400.0D, 400.0D),
                    new Point2D(mapSize * 0.25D, 200.0D),
                    new Point2D(mapSize * 0.5D, 200.0D),
                    new Point2D(mapSize * 0.75D, 200.0D),
                    new Point2D(mapSize - 200.0D, 200.0D)

            });

            waypointsByLane.put(LaneType.BOTTOM, new Point2D[]{
                    new Point2D(100.0D, mapSize - 100.0D),
                    new Point2D(400.0D, mapSize - 100.0D),
                    new Point2D(800.0D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.25D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.5D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.75D, mapSize - 200.0D),
                    new Point2D(mapSize - 400.0D, mapSize - 400.0D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.75D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.5D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.25D),
                    new Point2D(mapSize - 200.0D, 200.0D)
            });

            switch ((int) self.getId()) {
                case 1:
                case 2:
                case 6:
                case 7:
                    lane = LaneType.TOP;
                    break;
                case 3:
                case 8:
                    lane = LaneType.MIDDLE;
                    break;
                case 4:
                case 5:
                case 9:
                case 10:
                    lane = LaneType.BOTTOM;
                    break;
                default:
            }
            //lane = LaneType.MIDDLE;
            waypoints = waypointsByLane.get(lane);

        }
    }

    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним.
     */
    private void initializeTick(Wizard self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        MyStrategy.game = game;
        this.move = move;
    }

    /**
     * Данный метод предполагает, что все ключевые точки на линии упорядочены по уменьшению дистанции до последней
     * ключевой точки. Перебирая их по порядку, находим первую попавшуюся точку, которая находится ближе к последней
     * точке на линии, чем волшебник. Это и будет следующей ключевой точкой.
     * <p>
     * Дополнительно проверяем, не находится ли волшебник достаточно близко к какой-либо из ключевых точек. Если это
     * так, то мы сразу возвращаем следующую ключевую точку.
     */
    private Point2D getNextWaypoint(CircularUnit self) {
        int lastWaypointIndex = waypoints.length - 1;
        Point2D lastWaypoint = waypoints[lastWaypointIndex];

        for (int waypointIndex = 0; waypointIndex < lastWaypointIndex; ++waypointIndex) {
            Point2D waypoint = waypoints[waypointIndex];

            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
                return waypoints[waypointIndex + 1];
            }

            if (lastWaypoint.getDistanceTo(waypoint) < lastWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return lastWaypoint;
    }

    /**
     * Действие данного метода абсолютно идентично действию метода {@code getNextWaypoint}, если перевернуть массив
     * {@code waypoints}.
     */
    private Point2D getPreviousWaypoint(CircularUnit self) {
        Point2D firstWaypoint = waypoints[0];

        for (int waypointIndex = waypoints.length - 1; waypointIndex > 0; --waypointIndex) {
            Point2D waypoint = waypoints[waypointIndex];

            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
                return waypoints[waypointIndex - 1];
            }

            if (firstWaypoint.getDistanceTo(waypoint) < firstWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return firstWaypoint;
    }

    /**
     * Простейший способ перемещения волшебника.
     */
    /*
    private void goTo(Point2D point) {
        double angle = self.getAngleTo(point.getX(), point.getY());

        move.setTurn(angle);

        if (StrictMath.abs(angle) < game.getStaffSector() / 4.0D) {
            move.setSpeed(game.getWizardForwardSpeed());
        }
    }
*/
    private void goTo(Point2D point) {
        if (point == null) return;
        targetPoint = point;
        isTurn = true;
    }

    private void goBack(Point2D point) {
        if (point == null) return;
        targetPoint = point;
        //if()isTurn = false;
    }


    private void _goTo() {
        if (targetPoint == null) return;
        targetAngle = _goBack(targetPoint);
        if (isTurn)
            move.setTurn(targetAngle);
    }


    private double _goBack(Point2D point) {
        if (point == null) return self.getAngle();
        double angle = self.getAngleTo(point.getX(), point.getY());
        double fwd = game.getWizardForwardSpeed() * StrictMath.cos(angle);
        double strf = game.getWizardStrafeSpeed() * StrictMath.sin(angle);
        move.setSpeed(fwd);
        move.setStrafeSpeed(strf);
        return angle;
    }

    private Tree TreeOnLine(Wizard self, Point2D target) {
        for (Tree tree : world.getTrees()) {
            // if(tree.getDistanceTo(self)>self.getCastRange())continue;
            if (OnMyLine(self, target, tree)) return tree;
        }
        return null;
    }


    private boolean OnMyLine(CircularUnit src, Point2D dst, CircularUnit obj) {
        Point2D offset = (new Point2D(src.getX(), src.getY())).GetOffsetLine(dst, obj, self.getRadius() / 2);
        if (offset == null) return false;

        double h = StrictMath.hypot(offset.getX(), offset.getY());
        return h < (obj.getRadius() + src.getRadius() * 1.2D);
    }


    /**
     * Находим ближайшую цель для атаки, независимо от её типа и других характеристик.
     */
    private LivingUnit getNearestTarget(CircularUnit self) {
        List<LivingUnit> targets = new ArrayList<>();
        targets.addAll(Arrays.asList(world.getBuildings()));
        targets.addAll(Arrays.asList(world.getWizards()));
        targets.addAll(Arrays.asList(world.getMinions()));

        LivingUnit nearestTarget = null;
        double nearestTargetDistance = this.self.getVisionRange() + this.self.getRadius();

        for (LivingUnit target : targets) {

            if (target.getFaction() == self.getFaction() || (target.getFaction() == Faction.NEUTRAL && target.getLife() == target.getMaxLife()))
                continue;
            double distance = self.getDistanceTo(target);


            if (distance < nearestTargetDistance) {
                nearestTarget = target;
                nearestTargetDistance = distance;
            }
        }

        return nearestTarget;
    }


    /**
     * Находим ближайшую цель для атаки, независимо от её типа и других характеристик.
     */
    private LivingUnit getOptimalTarget(CircularUnit self) {
        List<LivingUnit> targets = new ArrayList<>();
        targets.addAll(Arrays.asList(world.getBuildings()));
        //targets.addAll(Arrays.asList(world.getWizards()));
        targets.addAll(Arrays.asList(world.getMinions()));

        LivingUnit nearestTarget = null;
        double nearestTargetDistance = Double.MAX_VALUE;
        double hp = Double.MAX_VALUE;

        for (LivingUnit target : world.getWizards()) {
            if (target.getFaction() == self.getFaction())
                continue;
            double distance = self.getDistanceTo(target);
            double l = (double) target.getLife() / (double) target.getMaxLife();

            if (distance > this.self.getCastRange() + self.getRadius() && !(distance <= this.self.getVisionRange() + self.getRadius() * 3 && l < ENEMY_LOW_HP_FACTOR))
                //if (distance > this.self.getCastRange()+self.getRadius() && !(distance <= this.self.getVisionRange() + self.getRadius() * 3 && l < ENEMY_LOW_HP_FACTOR))
                continue;
            // if(TreeOnLine(this.self,new Point2D(target.getX(),target.getY()))!=null)continue;

            if (distance < nearestTargetDistance || ((distance - nearestTargetDistance) < (self.getRadius() * 4) && hp > l)) {
                nearestTarget = target;
                nearestTargetDistance = distance;
                hp = (double) nearestTarget.getLife() / (double) nearestTarget.getMaxLife();
            }


        }
        if (nearestTarget != null) return nearestTarget;


        for (LivingUnit target : targets) {
            if ((target.getFaction() == Faction.NEUTRAL) || target.getFaction() == self.getFaction())
                continue;
            double distance = self.getDistanceTo(target);

            if (distance > this.self.getCastRange()) continue;
            double l = (double) target.getLife() / (double) target.getMaxLife();

            if (distance < nearestTargetDistance || ((distance - nearestTargetDistance) < (self.getRadius() * 2) && hp > l)) {
                nearestTarget = target;
                nearestTargetDistance = distance;
                hp = (double) nearestTarget.getLife() / (double) nearestTarget.getMaxLife();
            }
        }

        if (nearestTarget != null) return nearestTarget;

        //if(game.getFriendlyFireDamageFactor()>0.1D) for (Minion target : world.getMinions()             )            if(target.getLife()<12)return target;

        return nearestTarget;
    }


    public void ResetSimulation() {
        if (GSim == null) return;
        if (GSim.target.getDistanceTo(targetPoint) < CELLSIZE) return;
        GSim = null;


    }

    public Point2D GetSimulatedOptimal(Point2D targetPoint, int Iterations, boolean reset, boolean isRetreat, boolean isNewField) {
        if (targetPoint == null) return null;
        if (targetPoint.getDistanceTo(self) < SIMSPEED * 1.5) return null;
        //if(reset)ResetSimulation();
        int tick = world.getTickIndex();///(int)SIMSPEED;

        if (GSim != null && (tick % SIMLOAD) != 0 && GSim.path.size() > 0) {
            if (DEBUGSIMULATEOPTIMAL) {
                Point2D mp = GSim.path.get(0);//firstStep;
                int i = 0;
                for (Point2D p : GSim.path) {
                    DebugLine(mp.getX(), mp.getY(), p.getX(), p.getY(), String.valueOf((double) i / 20.0D));
                    i++;
                    mp = p;
                }

                //return GSim.firstStep;
            }
            Point2D step = GetNextStepSim(GSim, SIMSPEED);

            if (step != null) return step;
            isNewField = false;
        }
        MySim[] s = new MySim[SIMVECTORS + 1];


        if (isNewField) field = new ElectricField(CELLSIZE, world, targetPoint, self);
        UsePotentialField();


        s[SIMVECTORS] = (new MySim(world,self, targetPoint, field, (double)CELLSIZE, true)).Simulate(Iterations, targetPoint, 0);
        if (s[SIMVECTORS].mypoint != null) Iterations = s[SIMVECTORS].iter;

        if (DEBUG_FIELDLI_SIM && s[SIMVECTORS].mypoint != null) {

            Point2D mp = s[SIMVECTORS].path.get(0);//firstStep;
            int i = 0;
            for (Point2D p : s[SIMVECTORS].path) {
                DebugLine(mp.getX(), mp.getY(), p.getX(), p.getY(), "0.0");
                i++;
                mp = p;
            }
        }


        if (s[SIMVECTORS] != null && s[SIMVECTORS].mypoint != null) {
            GSim = s[SIMVECTORS];
            return GetNextStepSim(GSim, SIMSPEED);
        }


        int centr = (SIMVECTORS / 2);
        s[centr] = (new MySim(world,self, targetPoint, field, SIMSPEED)).Simulate(Iterations, targetPoint, 0);

        if (s[centr] != null && s[centr].mypoint != null) {
            GSim = s[centr];
            return GetNextStepSim(GSim, SIMSPEED);
        }


        GSim = getSimulatedPath(targetPoint, Iterations, s);


        Point2D t = GetNextStepSim(GSim, SIMSPEED);

        if (t == null)
            GSim = (new MySim(world,self, targetPoint, field, SIMSPEED)).SetNewTarget(StrictMath.PI).Simulate(Iterations, targetPoint, 10);
        if (GSim != null&&GSim.mypoint == null && field.nearLi != null) {
            s[centr] = (new MySim(world,self, field.nearLi, field, SIMSPEED)).Simulate(Iterations, field.nearLi, 0);
            GSim = getSimulatedPath(field.nearLi, Iterations, s);
        }
        if (GSim == null || GSim.mypoint == null) return targetPoint;

        return GetNextStepSim(GSim, SIMSPEED);
        //return opt.target;
    }

    private MySim getSimulatedPath(Point2D targetPoint, int Iterations, MySim[] s) {
        int centr = (SIMVECTORS / 2);
        double nachalo = -(double) (centr + 1) * OFFSETANGLE;
        //s[centr] = (new MySim(self, targetPoint, field, SIMSPEED)).Simulate(Iterations, targetPoint, 0);

        //Iterations = s[centr].iter;


        for (int i = 0; i <= s.length - 2; i++) {
            if (i == centr) {
                nachalo += OFFSETANGLE;
                continue;
            }
            s[i] = (new MySim(world,self, targetPoint, field, SIMSPEED)).SetNewTarget(nachalo += OFFSETANGLE).Simulate(Iterations, targetPoint, Iterations - Iterations / 3);

            Iterations = s[i].iter;
        }


        double mindist = Double.MAX_VALUE;
        MySim opt = null;
        int it = Iterations;
        int enemys = Integer.MAX_VALUE;
        for (MySim r : s) {
            if (r.mypoint == null) continue;
            int i = r.iter;
            if (r.enemysCount > enemys) continue;
            double dist = targetPoint.getDistanceTo(r.mypoint);
            if ((dist <= mindist && i <= it) || i < it) {
                opt = r;
                it = i;
                mindist = dist;
                enemys = r.enemysCount;
            }

        }
        return opt;
    }

    Point2D GetNextStepSim(MySim opt, double speed) {
        if (opt == null) return targetPoint;
        int numStep = opt.currStep;
        if (numStep >= opt.path.size() - 1) return targetPoint;
        Point2D step = opt.path.get(numStep);
        if (step.getDistanceTo(self) < speed) opt.currStep += 2;
        return step;
    }

}



