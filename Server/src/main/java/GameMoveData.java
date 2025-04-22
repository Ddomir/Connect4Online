import java.io.Serializable;

public class GameMoveData implements Serializable {
    private final int column;
    private final boolean hasWinner;
    private final boolean isDraw;

    public GameMoveData(int column, boolean hasWinner, boolean isDraw) {
        this.column = column;
        this.hasWinner = hasWinner;
        this.isDraw = isDraw;
    }

    public int getColumn() { return column; }
    public boolean hasWinner() { return hasWinner; }
    public boolean isDraw() { return isDraw; }
}