package za.co.entelect.challenge.command;

import za.co.entelect.challenge.entities.Position;

public class SnowBallsCommand implements Command {
    private Position TargetPosition;

    public SnowBallsCommand(Position TargetPosition) {
        this.TargetPosition = TargetPosition;
    }

    @Override
    public String render() { return String.format("snowball %d %d", TargetPosition.x, TargetPosition.y);}
}
