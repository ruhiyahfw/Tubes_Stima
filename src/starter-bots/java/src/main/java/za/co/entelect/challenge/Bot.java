package za.co.entelect.challenge;

//import javafx.geometry.Pos;
import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.io.Console;
import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;
    private MyPlayer player;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
        this.player = gameState.myPlayer;
    }

    public Command run() {
        //mencari adakah yang bisa melakukan bananabomb
        Worm Bananaenemy = getFirstWormInRange2(player.worms[1].bananaBombs.range, player.worms[1]);
        if (Bananaenemy != null && canBananaBom(player.worms[1], Bananaenemy) && !kenaBananaSendiri(Bananaenemy.position)) {
            Position position = Bananaenemy.position;
            if (player.remainingWormSelections > 0 && currentWorm.id != 2) {
                String perintah = String.format("banana %d %d", position.x, position.y);
                return new SelectCommand(2, perintah);
            } else if (currentWorm.id == 2) {
                return new BananaBombsCommand(position);
            }
        }

        //mencari adakah yg bisa melakukan snowball
        Worm snowEnemy = getFirstWormInRange2(player.worms[2].snowballs.range, player.worms[2]);
        if (snowEnemy != null && canSnowBall(player.worms[2], snowEnemy) && !kenaSnowballSendiri(snowEnemy.position)) {
            Position position = snowEnemy.position;
            if (player.remainingWormSelections > 0 && currentWorm.id != 3) {
                String perintah = String.format("snowball %d %d", position.x, position.y);
                return new SelectCommand(3, perintah);
            } else if (currentWorm.id == 3) {
                return new SnowBallsCommand(position);
            }
        }

        //mencari apakah ada yg bisa dishoot
        // kita lihat apa bisa tanpa select dulu
        Worm enemyWorm = getFirstWormInRange2(currentWorm.weapon.range, currentWorm);
        if (enemyWorm != null) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            if (!tertembakPenghalang(currentWorm, direction, enemyWorm)) {
                return new ShootCommand(direction);
            }
        }
        // kita lihat worm lain yang bisa menembak (pakai perintah select)
        if (player.remainingWormSelections > 0) {
            for (MyWorm wormkita : player.worms) {
                if (wormkita != currentWorm) {
                    enemyWorm = getFirstWormInRange2(wormkita.weapon.range, wormkita);
                    if (enemyWorm != null) {
                        Direction direction = resolveDirection(wormkita.position, enemyWorm.position);
                        if (!tertembakPenghalang(wormkita, direction, enemyWorm)) {
                            String perintah = String.format("shoot %s", direction.name());
                            return new SelectCommand(wormkita.id, perintah);
                        }
                    }
                }
            }
        }

        //Cek apakah ada cell yang bisa dilakukan digging disekitar kita, return arah tempat digging/null jika tidak ada
        Position digPosition = getDiggingPosition();
        if (digPosition != null) {
            System.out.println(currentWorm.position.x + " " + currentWorm.position.y);
            return new DigCommand(digPosition.x, digPosition.y);
        }
        //move mendekati musuh
        Direction moveDirection = getMovingDirection(currentWorm.position);
        return new MoveCommand(currentWorm.position.x + moveDirection.x, currentWorm.position.y + moveDirection.y);
        /*
        Position movPos = getMovingPosition();
        if (movPos != null) {
            return new MoveCommand(movPos.x, movPos.y);
        }

        return new DoNothingCommand(); */

    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    private boolean canBananaBom(MyWorm wormkita, Worm enemy) {
        return wormkita.bananaBombs.count > 0
                && euclideanDistance(wormkita.position.x, wormkita.position.y, enemy.position.x, enemy.position.y) <= wormkita.bananaBombs.range
                && euclideanDistance(wormkita.position.x, wormkita.position.y, enemy.position.x, enemy.position.y) > wormkita.bananaBombs.damageRadius * 0.75;
    }

    private boolean canSnowBall(MyWorm wormkita, Worm enemy) {
        return wormkita.snowballs.count > 0
                && enemy.roundsUntilUnfrozen == 0
                && wormkita.roundsUntilUnfrozen == 0
                && euclideanDistance(wormkita.position.x, wormkita.position.y, enemy.position.x, enemy.position.y) <= wormkita.snowballs.range
                && euclideanDistance(wormkita.position.x, wormkita.position.y, enemy.position.x, enemy.position.y) > wormkita.snowballs.freezeRadius * Math.sqrt(2);
    }

    private Worm getFirstWormInRange2(int rangeSenjata, MyWorm wormkita) {

        Set<String> cells = constructFireDirectionLines2(rangeSenjata, wormkita)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition)) {
                return enemyWorm;
            }
        }

        return null;
    }

    private List<List<Cell>> constructFireDirectionLines2(int range, MyWorm wormkita) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = wormkita.position.x + (directionMultiplier * direction.x);
                int coordinateY = wormkita.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(wormkita.position.x, wormkita.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private boolean tertembakPenghalang(MyWorm wormkita, Direction direction, Worm enemyWorm) {
        boolean tertembak = false;
        int directionmultiplier = 1;
        int coordinateX;
        int coordinateY;
        do {
            coordinateX = wormkita.position.x + (directionmultiplier * direction.x);
            coordinateY = wormkita.position.y + (directionmultiplier * direction.y);
            for (MyWorm cek : player.worms) {
                if (coordinateX == cek.position.x && coordinateY == cek.position.y) {
                    tertembak = true;
                    break;
                }
            }
            Cell cell = gameState.map[coordinateX][coordinateY];
            if (cell.type == CellType.DIRT || cell.type == CellType.DEEP_SPACE) {
                tertembak = true;
            }
            directionmultiplier++;
        } while (coordinateX != enemyWorm.position.x && coordinateY != enemyWorm.position.y && !tertembak && directionmultiplier < wormkita.weapon.range);
        if (!tertembak && directionmultiplier == wormkita.weapon.range && !(coordinateX == enemyWorm.position.x && coordinateY == enemyWorm.position.y)) {
            tertembak = true; // untuk cek out of bound
        }
        return tertembak;
    }

    private boolean kenaBananaSendiri(Position posisitembak) {
        boolean tertembak = false;
        for (Direction direction : Direction.values()) {
            int directionMultiplier = 1;
            do {
                int coordinateX = posisitembak.x + (directionMultiplier * direction.x);
                int coordinateY = posisitembak.y + (directionMultiplier * direction.y);
                for (MyWorm cek : player.worms) {
                    if (coordinateX == cek.position.x && coordinateY == cek.position.y) {
                        tertembak = true;
                        break;
                    }
                }
                directionMultiplier++;
            } while (directionMultiplier <= 2 && !tertembak);
            if (tertembak) {
                break;
            }
        }
        return tertembak;
    }

    private boolean kenaSnowballSendiri(Position posisitembak) {
        for (Direction direction : Direction.values()) {
            int coordinateX = posisitembak.x + direction.x;
            int coordinateY = posisitembak.y + direction.y;
            for (MyWorm cek : player.worms) {
                if (coordinateX == cek.position.x && coordinateY == cek.position.y) {
                    return true;
                }
            }
        }
        return false;
    }

    private void debug(){
        for(int i=0;i<33;i++){
            for(int j=0;j<33;j++){
                System.out.print(gameState.map[i][j].type + "{" + String.valueOf(i) + " " + String.valueOf(j) + "} ");
            }
            System.out.println("");
        }
    }


    private boolean isOccupied(int x, int y){
        for(int i=0;i<gameState.myPlayer.worms.length;i++){
            Worm tmp = gameState.myPlayer.worms[i];
            if(tmp.position.x == x && tmp.position.y == y) return true;
        }
        for(int i=0;i<opponent.worms.length;i++){
            Worm tmp = opponent.worms[i];
            if(tmp.position.x == x && tmp.position.y == y) return true;
        }
        return false;
    }

    private boolean isAir(int x, int y){
        return (gameState.map[y][x].type == CellType.AIR);
    }


    private Position getMovingPosition(){
        int currX = currentWorm.position.x, currY = currentWorm.position.y;
        int deltaPos[] = {1, 0, -1};

        Position moveFinal = new Position();
        moveFinal.x = 0;
        moveFinal.y = 0;
        int shortestDist = 1000000000;
        for(int deltaX : deltaPos){
            for(int deltaY : deltaPos){
                //jika misalkan ada > 1 posisi untuk digging, ambil yang jarak
                //euclideannya terhadap musuh minimum
                int newX = currX+deltaX, newY = currY+deltaY;
                if(isValidCoordinate(newX, newY)){
                    if(isAir(newX, newY) && !isOccupied(newX, newY)){
                        Position movPos = new Position();
                        movPos.x = newX;
                        movPos.y = newY;
                        //iterasi posisi semua musuh
                        //ambil posisi digging yang memiliki
                        // rata-rata jarak terkecil antara musuh-player
                        int dist = 1000000000;
                        for(Worm enemy : opponent.worms){
                            int enemyPosX = enemy.position.x;
                            int enemyPosY = enemy.position.y;
                            int distTmp = euclideanDistance(currX, currY, enemyPosX, enemyPosY);
                            if(distTmp < dist){
                                dist = distTmp;
                            }
                        }

                        if(dist< shortestDist ){
                            moveFinal.x = movPos.x;
                            moveFinal.y = movPos.y;
                            dist = shortestDist;
                        }
                    }
                }
            }
        }
        //System.out.println(isValidCoordinate(moveFinal.x, moveFinal.y) + " tes");
        if(moveFinal.x == 0 && moveFinal.y == 0) return null;
        return moveFinal;
    }

    private Direction getMovingDirection(Position curr){
        //hitung semua jarak euclidean dari titik sekarang ke semua musuh, cari minimum
        int dist = 1000000000;
        int currX = curr.x, currY = curr.y;
        Position minEnemyPos = new Position();
        minEnemyPos.x = 0;
        minEnemyPos.y = 0;
        for(Worm enemy : opponent.worms){
            int enemyPosX = enemy.position.x;
            int enemyPosY = enemy.position.y;
            int distTmp = euclideanDistance(currX, currY, enemyPosX, enemyPosY);
            if(distTmp < dist){
                dist = distTmp;
                minEnemyPos.x = enemyPosX;
                minEnemyPos.y = enemyPosY;
            }
        }
        Direction move = resolveDirection(curr, minEnemyPos);
        return move;
    }

    private Position getEnemyMinimumPos(int currX, int currY) {
        int dist = 1000000000;
        int xMove = 0, yMove = 0;
        for (Worm enemy : opponent.worms) {
            int enemyPosX = enemy.position.x;
            int enemyPosY = enemy.position.y;
            int distTmp = euclideanDistance(currX, currY, enemyPosX, enemyPosY);
            if (distTmp < dist) {
                dist = distTmp;
                xMove = enemyPosX;
                yMove = enemyPosY;
            }
        }
        Position finalMove = new Position();
        finalMove.x = xMove;
        finalMove.y = yMove;
        return finalMove;
    }

    private boolean isDirtCell(int x, int y) {
        //System.out.println(gameState.map[x][y].type + " " + CellType.DIRT);
        return (gameState.map[y][x].type == CellType.DIRT);
    }

    private Position getDiggingPosition() {
        int currX = currentWorm.position.x, currY = currentWorm.position.y;
        int deltaPos[] = {1, 0, -1};

        Position moveFinal = new Position();
        moveFinal.x = 0;
        moveFinal.y = 0;
        int shortestDist = 1000000000;
        for (int deltaX : deltaPos) {
            for (int deltaY : deltaPos) {
                //jika misalkan ada > 1 posisi untuk digging, ambil yang jarak
                //euclideannya terhadap musuh minimum
                int newX = currX + deltaX, newY = currY + deltaY;
                if (isValidCoordinate(newX, newY)) {
                    if (isDirtCell(newX, newY)) {
                        Position digPos = new Position();
                        digPos.x = newX;
                        digPos.y = newY;
                        //iterasi posisi semua musuh
                        //ambil posisi digging yang memiliki jarak terkecil
                        int dist = 1000000000;
                        for (Worm enemy : opponent.worms) {
                            int enemyPosX = enemy.position.x;
                            int enemyPosY = enemy.position.y;
                            int distTmp = euclideanDistance(currX, currY, enemyPosX, enemyPosY);
                            if (distTmp < dist) {
                                dist = distTmp;
                            }
                        }
                        if (dist < shortestDist) {
                            moveFinal.x = digPos.x;
                            moveFinal.y = digPos.y;
                            dist = shortestDist;
                        }
                    }
                }
            }
        }
        //System.out.println(isValidCoordinate(moveFinal.x, moveFinal.y) + " tes");
        if (moveFinal.x == 0 && moveFinal.y == 0) return null;
        return moveFinal;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }
    /*
    private Worm getFirstWormInRange() {

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition)) {
                return enemyWorm;
            }
        }

        return null;
    }

    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }
    */
}
