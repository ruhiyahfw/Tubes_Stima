package za.co.entelect.challenge.command;

public class SelectCommand implements Command {
    private int id;
    private String perintah;

    public SelectCommand(int id, String perintah) {
        this.id = id;
        this.perintah = perintah;
    }

    @Override
    public String render() {
        return String.format("select %d;%s", id, perintah);
    }
}
