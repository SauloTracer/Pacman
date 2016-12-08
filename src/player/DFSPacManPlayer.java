package player;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import pacman.*;
import pacman.Location;

/**
 * Use this class for your basic DFS player implementation.
 * @author grenager
 *
 */
public class DFSPacManPlayer implements PacManPlayer, StateEvaluator {

    Move lastMove;
    int hungry;
    
    
  /**
   * Chooses a Move for Pac-Man in the Game.
   * 
   * @param game
   * @return a Move for Pac-Man
   */
    @Override
    public Move chooseMove(Game game) {
        int len, move, mVotes[], hWeights[], chosen;
        double h[][], bestH[];
        State state, nextState;
        Move forbiden = null;
        boolean winning, fear;
        winning = fear = false;
                
        List<Move> legalMoves = game.getLegalPacManMoves();
        
        state = game.getCurrentState();
        
        //Menor distância do próximo disco
        //Maior distância do fantasma mais próximo
        //Maior média de distância de fantasmas
        //Proximidade do objetivo
        
        bestH = new double[] {
            (double)Double.POSITIVE_INFINITY, 
            (double)Double.POSITIVE_INFINITY,
            (double)Double.POSITIVE_INFINITY,
            (double)Double.NEGATIVE_INFINITY,
            (double)Double.NEGATIVE_INFINITY,
            (double)Double.NEGATIVE_INFINITY
        };
        
        hWeights = new int[] {
            5, //Menor distância do próximo disco
            4, //Menor distância média do próximo disco
            3, //Menor distância euclidiana do disco mais próximo
            3, //Maior distância do fantasma mais próximo
            2, //Maior média de distância dos fantasmas
            9  //Menor quantidade de discos
        };
        
        Collection<Location> ghosts = state.getGhostLocations();
        double distance = Location.manhattanDistanceToClosest(state.getPacManLocation(), ghosts);
        if (distance > 5) {
            fear = false;
            hWeights[2] = 2;
        } else {
            fear = true;
            hWeights[2] = 100;
        }
        
        Collection<Location> dots = state.getDotLocations();
        distance = Location.manhattanDistanceToClosest(state.getPacManLocation(), dots);
        hWeights[0] = (distance > 5) ? 10 : 5;

        if(!fear) {
            hWeights[0] += hungry;
            hWeights[1] += hungry;
            hWeights[2] += hungry;
            
            //Impedir de retornar ao último estado a não ser que esteja fugindo de fantasmas.
            //ToDo - Eliminar movimento oposto ao anterior na tentativa de impedir loops
            if (lastMove != null) {
                switch(lastMove) {
                    case DOWN:
                        forbiden = Move.UP;
                        break;
                    case UP:
                        forbiden = Move.DOWN;
                        break;
                    case LEFT:
                        forbiden = Move.RIGHT;
                        break;
                    case RIGHT:
                        forbiden = Move.LEFT;
                        break;
                    default:
                        forbiden = null;
                }

                if(legalMoves.indexOf(forbiden) > -1) {
                    legalMoves.remove(legalMoves.indexOf(forbiden));
                }
            }
        }
        
        len = legalMoves.size();
        h = new double[bestH.length][len];
        mVotes = new int[len];
        for (int i = 0; i < len; i++) mVotes[i] = 0;
                
        for(Move m : legalMoves) {
            move = legalMoves.indexOf(m);
            nextState = Game.getNextState(state, m);
            
            if(Game.isFinal(nextState)) {
                if(Game.isWinning(nextState)) {
                    chosen = move;
                    winning = true;
                    break;
                } else {
                    h[0][move] = (double)Double.POSITIVE_INFINITY;
                    h[1][move] = (double)Double.POSITIVE_INFINITY;
                    h[2][move] = (double)Double.POSITIVE_INFINITY;
                    h[3][move] = (double)Double.NEGATIVE_INFINITY;
                    h[4][move] = (double)Double.NEGATIVE_INFINITY;
                    h[5][move] = (double)Double.NEGATIVE_INFINITY;
                    continue;
                }
            }
            
            //Menor distância do próximo disco - h[0]
            dots = nextState.getDotLocations();
            h[0][move] = Location.manhattanDistanceToClosest(nextState.getPacManLocation(), dots);
            if(h[0][move] < bestH[0]) bestH[0] = h[0][move];
            
            //Menor distância média do próximo disco - h[1]
            //Tenta direcionar o pacman para a direção da maior população de discos
            h[1][move] = 0;
            Location l;
            for (Iterator iterator = dots.iterator(); iterator.hasNext();) {
                l = (Location) iterator.next();
                h[1][move] += Location.manhattanDistance(nextState.getPacManLocation(), l);
            }
            for(int i = 0; i < dots.size(); i++)
            h[1][move] /= dots.size();
            if(h[1][move] > bestH[1]) bestH[1] = h[1][move];
            
            //Menor distância euclidiana do disco mais próximo - h[2]
            h[2][move] = Location.euclideanDistanceToClosest(nextState.getPacManLocation(), dots);
            if(h[2][move] < bestH[2]) bestH[2] = h[2][move];
            
            //Maior distância do fantasma mais próximo - h[3]
            ghosts = nextState.getGhostLocations();
            h[3][move] = Location.manhattanDistanceToClosest(nextState.getPacManLocation(), ghosts);
            if(h[3][move] > bestH[3]) bestH[3] = h[3][move];
            
            //Maior média de distância de fantasmas - h[4]
            h[4][move] = 0;
            List<Location> ghostLocations = nextState.getGhostLocations();
            for(int i = 0; i < 4; i++)
                h[4][move] += Location.manhattanDistance(nextState.getPacManLocation(), ghostLocations.get(i));
            h[4][move] /= 4;
            if(h[4][move] > bestH[4]) bestH[4] = h[4][move];
            
            //Proximidade do objetivo (< n de rings) - h[5]
            h[5][move] = evaluateState(nextState);  
            if(h[5][move] > bestH[5]) bestH[5] = h[5][move];
        }
        
        chosen = 0;
        if (!winning) {
            for(int i = 0; i < bestH.length; i++)
                for(int j = 0; j < len; j++)
                    if(h[i][j] == bestH[i]) mVotes[j]+= hWeights[i];

            for(int i = 1; i < len; i++)
                if(mVotes[i] > mVotes[chosen]) chosen = i;
        }
        
        lastMove = legalMoves.get(chosen);
        
        return lastMove;
    }

  /**
   * Computes an estimate of the value of the State.
   * @param state the State to evaluate.
   * @return an estimate of the value of the State.
   */
  @Override
  public double evaluateState(State state) {
    if (Game.isLosing(state))
      return Double.NEGATIVE_INFINITY;
    return -state.getDotLocations().size(); //Qt mais próximo de 0 melhor
  }

}
