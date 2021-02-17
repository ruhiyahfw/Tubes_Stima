package za.co.entelect.challenge.command;

import za.co.entelect.challenge.entities.Position;

public class BananaBombsCommand implements Command{
    private Position TargetPosition;

    public BananaBombsCommand(Position TargetPosition) {
        this.TargetPosition = TargetPosition;
    }

    @Override
    public String render() {
        return String.format("banana %d %d", TargetPosition.x, TargetPosition.y);
    }
}
