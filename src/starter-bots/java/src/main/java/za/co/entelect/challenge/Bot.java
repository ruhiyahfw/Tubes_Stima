package za.co.entelect.challenge;

import javafx.geometry.Pos;
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

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    public Command run() {

        Worm enemyWorm = getFirstWormInRange();
        if (enemyWorm != null) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            return new ShootCommand(direction);
        }
        /*
        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        int cellIdx = random.nextInt(surroundingBlocks.size());

        Cell block = surroundingBlocks.get(cellIdx);
        if (block.type == CellType.AIR) {
            return new MoveCommand(block.x, block.y);
        } else if (block.type == CellType.DIRT) {
            return new DigCommand(block.x, block.y);
        }

        return new DoNothingCommand(); */
        //Cek apakah ada cell yang bisa dilakukan digging disekitar kita, return arah tempat digging/null jika tidak ada
        Position digPosition = getDiggingPosition();
        if(digPosition != null){
            debug();
            System.out.println(currentWorm.position.x +  " " + currentWorm.position.y);
            return new DigCommand(digPosition.x, digPosition.y);
        }
        //move mendekati musuh
        Direction moveDirection = getMovingDirection(currentWorm.position);
        return new MoveCommand(currentWorm.position.x + moveDirection.x, currentWorm.position.y + moveDirection.y);
    }

    private void debug(){
        for(int i=0;i<33;i++){
            for(int j=0;j<33;j++){
                System.out.print(gameState.map[i][j].type + "{" + String.valueOf(i) + " " + String.valueOf(j) + "} ");
            }
            System.out.println("");
        }
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

    private Position getEnemyMinimumPos(int currX, int currY){
        int dist = 1000000000;
        int xMove=0, yMove=0;
        for(Worm enemy : opponent.worms){
            int enemyPosX = enemy.position.x;
            int enemyPosY = enemy.position.y;
            int distTmp = euclideanDistance(currX, currY, enemyPosX, enemyPosY);
            if(distTmp < dist){
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

    private boolean isDirtCell(int x, int y){
        //System.out.println(gameState.map[x][y].type + " " + CellType.DIRT);
        return (gameState.map[y][x].type == CellType.DIRT);
    }

    private Position getDiggingPosition(){
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
                    if(isDirtCell(newX, newY)){
                        Position digPos = new Position();
                        digPos.x = newX;
                        digPos.y = newY;
                        //iterasi posisi semua musuh
                        //ambil posisi digging yang memiliki jarak terkecil
                        int dist = 1000000000;
                        for(Worm enemy : opponent.worms){
                            int enemyPosX = enemy.position.x;
                            int enemyPosY = enemy.position.y;
                            int distTmp = euclideanDistance(currX, currY, enemyPosX, enemyPosY);
                            if(distTmp < dist){
                                dist = distTmp;
                            }
                        }
                        if(dist < shortestDist){
                            moveFinal.x = digPos.x;
                            moveFinal.y = digPos.y;
                            dist = shortestDist;
                        }
                    }
                }
            }
        }
        System.out.println(isValidCoordinate(moveFinal.x, moveFinal.y) + " tes");
        if(moveFinal.x == 0 && moveFinal.y == 0) return null;
        return moveFinal;
    }

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

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
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
}