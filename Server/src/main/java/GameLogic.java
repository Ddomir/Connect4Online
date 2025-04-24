public class GameLogic {
    private static final int ROWS = 6;
    private static final int COLS = 7;
    private final char[][] board = new char[ROWS][COLS];
    private char currentPlayer = 'R';

    public GameLogic() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                board[row][col] = ' ';
            }
        }
    }

    public boolean makeMove(int col) {
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][col] == ' ') {
                board[row][col] = currentPlayer;
                return true;
            }
        }
        return false;
    }

    public char checkWinner() {
        // Horizontal
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS - 3; col++) {
                if (board[row][col] != ' ' && board[row][col] == board[row][col + 1] &&
                        board[row][col] == board[row][col + 2] && board[row][col] == board[row][col + 3]) {
                    return board[row][col];
                }
            }
        }
        // Vertical
        for (int row = 0; row < ROWS - 3; row++) {
            for (int col = 0; col < COLS; col++) {
                if (board[row][col] != ' ' && board[row][col] == board[row + 1][col] &&
                        board[row][col] == board[row + 2][col] && board[row][col] == board[row + 3][col]) {
                    return board[row][col];
                }
            }
        }
        // Diagonal (positive slope)
        for (int row = 0; row < ROWS - 3; row++) {
            for (int col = 0; col < COLS - 3; col++) {
                if (board[row][col] != ' ' && board[row][col] == board[row + 1][col + 1] &&
                        board[row][col] == board[row + 2][col + 2] && board[row][col] == board[row + 3][col + 3]) {
                    return board[row][col];
                }
            }
        }
        // Diagonal (negative slope)
        for (int row = 3; row < ROWS; row++) {
            for (int col = 0; col < COLS - 3; col++) {
                if (board[row][col] != ' ' && board[row][col] == board[row - 1][col + 1] &&
                        board[row][col] == board[row - 2][col + 2] && board[row][col] == board[row - 3][col + 3]) {
                    return board[row][col];
                }
            }
        }
        return ' ';
    }

    public boolean isDraw() {
        for (int col = 0; col < COLS; col++) {
            if (board[0][col] == ' ') return false;
        }
        return true;
    }

    public char getCurrentPlayer() { return currentPlayer; }
    public void switchPlayer() { currentPlayer = (currentPlayer == 'R') ? 'Y' : 'R'; }
}