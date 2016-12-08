package player;

import java.util.List;
import pacman.*;

public class SauloMachadoPacManPlayer implements PacManPlayer {

    @Override
    public Move chooseMove(Game game) {
        int len, move;
        List<Move> legalMoves = game.getLegalPacManMoves();
        len = legalMoves.size();
        move = (int)(len*Math.random()); //Varia de 0 a legalMoves.size()-1
        return legalMoves.get(move);
    }
    
}
