package io.github.osrepnay.plankton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Plankton {

    public volatile boolean keepSearching = true;
    private double[] pieceScores = new double[] {1, 3, 3.25, 5, 9, 10000};

    public double[] bestMove(Game game, int color, int depth) {
        double[] bestMove = new double[] {-1, -1, 0, -1};
        double bestMoveScore = color == 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        List<PieceMove> moves = new ArrayList<>();
        for(int square = 0; square < 64; square++) {
            if(!game.pieceExists(square) || game.colorOfSquare(square) != color) {
                continue;
            }
            int piece = game.pieceOfSquare(square);
            for(PieceMove move : game.pieceMovesFromSquare(square)) {
                if(!validMove(game, move, color, piece)) {
                    continue;
                }
                moves.add(move);
            }
        }
        int numCores = Runtime.getRuntime().availableProcessors();
        Map<Integer, List<Integer>> sectionedMoves = IntStream.rangeClosed(0, moves.size() - 1)
                .boxed()
                .collect(Collectors.groupingBy(x -> x % numCores));
        List<FindScore> threads = new ArrayList<>();
        for(List<Integer> moveGroupIdxs : sectionedMoves.values()) {
            List<PieceMove> moveGroup = moveGroupIdxs.stream()
                    .map(x -> moves.get(x))
                    .collect(Collectors.toList());
            Game gameClone = (Game)game.clone();
            FindScore searchThread = new FindScore(gameClone, moveGroup, color, depth);
            searchThread.start();
            threads.add(searchThread);
        }
        List<double[]> bestMoves = new ArrayList<>();
            for(FindScore thread : threads) {
                try {
                    thread.join();
                } catch(InterruptedException e) {
                    e.printStackTrace();
                    return new double[] {-1, -1, 0, -1};
                }
                bestMoves.add(thread.result);
            }
        for(double[] potentialBestMove : bestMoves) {
            if(color == 0 ? potentialBestMove[2] > bestMoveScore : potentialBestMove[2] < bestMoveScore) {
                bestMove = potentialBestMove;
                bestMoveScore = potentialBestMove[2];
            }
        }
        return bestMove;
    }

    class FindScore extends Thread {

        Game game;
        List<PieceMove> moves;
        int color;
        int depth;
        double[] result = new double[4];

        public FindScore(Game game, List<PieceMove> moves, int color, int depth) {
            this.game = game;
            this.moves = moves;
            this.color = color;
            this.depth = depth;
        }

        @Override
        public void run() {
            double[] bestMove = new double[] {-1, -1, 0, -1};
            double bestScore = color == 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            for(PieceMove move : moves) {
                int piece = game.pieceOfSquare(move.start);
                if(!keepSearching) {
                    result = new double[] {-1, -1, 0, -1};
                    break;
                }
                PrevMoveGameState prevMoveState = game.makeMove(move, color, piece);
                double moveScore = color == 0
                        ? min(game, bestScore, Integer.MAX_VALUE, depth - 1)
                        : max(game, Integer.MIN_VALUE, bestScore, depth - 1);
                game.unMakeMove(move, color, piece, prevMoveState);
                //sign for ran out of time
                if(moveScore == Double.MAX_VALUE) {
                    result = new double[] {-1, -1, 0, -1};
                    break;
                }
                if(color == 0 ? moveScore > bestScore : moveScore < bestScore) {
                    bestScore = moveScore;
                    double promotion = -1;
                    switch(move.special) {
                        case PROMOTION_KNIGHT:
                            promotion = 1;
                            break;
                        case PROMOTION_BISHOP:
                            promotion = 2;
                            break;
                        case PROMOTION_ROOK:
                            promotion = 3;
                            break;
                        case PROMOTION_QUEEN:
                            promotion = 4;
                            break;
                    }
                    bestMove = new double[] {move.start, move.end, bestScore, promotion};
                }
            }
            result = bestMove;
        }

    }

    public double max(Game game, double alpha, double beta, int depth) {
        if(gameOver(game, 0)) {
            return eval(game, 0);
        }
        if(depth <= 0) {
            return qMax(game, alpha, beta);
        }
        for(int square = 0; square < 64; square++) {
            if(!game.pieceExists(square) || game.colorOfSquare(square) != 0) {
                continue;
            }
            int piece = game.pieceOfSquare(square);
            for(PieceMove move : game.pieceMovesFromSquare(square)) {
                if(!keepSearching) {
                    return Double.MAX_VALUE;
                }
                if(!validMove(game, move, 0, piece)) {
                    continue;
                }
                PrevMoveGameState prevMoveState = game.makeMove(move, 0, piece);
                double moveScore = min(game, alpha, beta, depth - 1);
                game.unMakeMove(move, 0, piece, prevMoveState);
                if(moveScore >= beta) {
                    return beta;
                }
                if(moveScore > alpha) {
                    alpha = moveScore;
                }
            }
        }
        return alpha;
    }

    public double min(Game game, double alpha, double beta, int depth) {
        if(gameOver(game, 1)) {
            return eval(game, 1);
        }
        if(depth <= 0) {
            return qMin(game, alpha, beta);
        }
        for(int square = 0; square < 64; square++) {
            if(!game.pieceExists(square) || game.colorOfSquare(square) != 1) {
                continue;
            }
            int piece = game.pieceOfSquare(square);
            for(PieceMove move : game.pieceMovesFromSquare(square)) {
                if(!keepSearching) {
                    return Double.MAX_VALUE;
                }
                if(!validMove(game, move, 1, piece)) {
                    continue;
                }
                PrevMoveGameState prevMoveState = game.makeMove(move, 1, piece);
                double moveScore = max(game, alpha, beta, depth - 1);
                game.unMakeMove(move, 1, piece, prevMoveState);
                if(moveScore <= alpha) {
                    return alpha;
                }
                if(moveScore < beta) {
                    beta = moveScore;
                }
            }
        }
        return beta;
    }

    public double qMax(Game game, double alpha, double beta) {
        double standPat = eval(game, 0);
        if(gameOver(game, 0)) {
            return standPat;
        }
        if(standPat >= beta) {
            return beta;
        }
        if(standPat > alpha) {
            alpha = standPat;
        }
        for(int square = 0; square < 64; square++) {
            if(!game.pieceExists(square) || game.colorOfSquare(square) != 0) {
                continue;
            }
            int piece = game.pieceOfSquare(square);
            for(PieceMove move : game.pieceMovesFromSquare(square)) {
                if(!keepSearching) {
                    return Double.MAX_VALUE;
                }
                if(!game.pieceExists(move.end)) {
                    continue;
                }
                if(!validMove(game, move, 0, piece)) {
                    continue;
                }
                if(see(game, move) < 0) {
                    continue;
                }
                PrevMoveGameState prevMoveState = game.makeMove(move, 0, piece);
                double moveScore = qMin(game, alpha, beta);
                game.unMakeMove(move, 0, piece, prevMoveState);
                if(moveScore >= beta) {
                    return beta;
                }
                if(moveScore > alpha) {
                    alpha = moveScore;
                }
            }
        }
        return alpha;
    }

    public double qMin(Game game, double alpha, double beta) {
        double standPat = eval(game, 1);
        if(gameOver(game, 1)) {
            return standPat;
        }
        if(standPat <= alpha) {
            return alpha;
        }
        if(standPat < beta) {
            beta = standPat;
        }
        for(int square = 0; square < 64; square++) {
            if(!game.pieceExists(square) || game.colorOfSquare(square) != 1) {
                continue;
            }
            int piece = game.pieceOfSquare(square);
            for(PieceMove move : game.pieceMovesFromSquare(square)) {
                if(!keepSearching) {
                    return Double.MAX_VALUE;
                }
                if(!game.pieceExists(move.end)) {
                    continue;
                }
                if(!validMove(game, move, 1, piece)) {
                    continue;
                }
                if(see(game, move) > 0) {
                    continue;
                }
                PrevMoveGameState prevMoveState = game.makeMove(move, 1, piece);
                double moveScore = qMax(game, alpha, beta);
                game.unMakeMove(move, 1, piece, prevMoveState);
                if(moveScore <= alpha) {
                    return alpha;
                }
                if(moveScore < beta) {
                    beta = moveScore;
                }
            }
        }
        return beta;
    }

    public double see(Game game, PieceMove move) {
        int color = game.colorOfSquare(move.start);
        int piece = game.pieceOfSquare(move.start);
        double score = pieceScores[game.pieceOfSquare(move.end)] * (-color * 2 + 1);
        int lowestAttackerSquare = -1;
        PrevMoveGameState prevMoveState = game.makeMove(move, color, piece);
        for(int square = 0; square < 64; square++) {
            if(game.pieceExists(square) && game.colorOfSquare(square) != color) {
                PieceMove capture = new PieceMove(square, move.end, SpecialMove.NONE);
                int moveIndex = game.pieceMovesFromSquare(square).indexOf(capture);
                if(moveIndex == -1) {
                    continue;
                }
                int squareColor = game.colorOfSquare(square);
                int squarePiece = game.pieceOfSquare(square);
                if(validMove(game, capture, squareColor, squarePiece)) {
                    if(lowestAttackerSquare == -1 || squarePiece < game.pieceOfSquare(lowestAttackerSquare)) {
                        lowestAttackerSquare = square;
                        if(squarePiece == 0) {
                            break;
                        }
                    }
                }
            }
        }
        if(lowestAttackerSquare != -1) {
            //TODO store move index
            PieceMove capture = new PieceMove(lowestAttackerSquare, move.end, SpecialMove.NONE);
            score += see(game, capture);
        }
        game.unMakeMove(move, color, piece, prevMoveState);
        return score;
    }

    public double eval(Game game, int color) {
        if(inStalemate(game, color)) {
            return 0;
        }
        if(inCheckmate(game, 0)) {
            return -10000;
        }
        if(inCheckmate(game, 1)) {
            return 10000;
        }
        double score = 0;
        double[] totalMaterial = new double[2];
        for(int piece = 0; piece < game.piecePositionsFromColor(0).length; piece++) {
            double wScore = Long.bitCount(game.piecePositions(0, piece)) * pieceScores[piece];
            double bScore = Long.bitCount(game.piecePositions(1, piece)) * pieceScores[piece];
            score += wScore;
            score -= bScore;
            if(piece != 5) {
                totalMaterial[0] += wScore;
                totalMaterial[1] += bScore;
            }
        }
        int[] moveCount = new int[2];
        for(int square = 0; square < 64; square++) {
            if(game.pieceExists(square)) {
                if(game.colorOfSquare(square) == 0 && totalMaterial[1] >= 10) {
                    score += PSTables.psTables[game.pieceOfSquare(square)][square] / 15;
                } else if(game.colorOfSquare(square) == 1 && totalMaterial[0] >= 10) {
                    score -= PSTables.psTables[game.pieceOfSquare(square)][63 - square] / 15;
                }
                moveCount[game.colorOfSquare(square)] += game.pieceMovesFromSquare(square).size();
            }
        }
        score += moveCount[0] / 100d;
        score -= moveCount[1] / 100d;
        return score;
    }

    public boolean validMove(Game game, PieceMove move, int color, int piece) {
        if(game.pieceExists(move.end) && game.colorOfSquare(move.end) == color) {
            return false;
        }
        if(move.special == SpecialMove.CASTLE_KINGSIDE) {
            //kingside
            if(game.pieceExists(move.start + 1) || game.pieceExists(move.start + 2)) {
                return false;
            }
            PieceMove midCastleStateMove = new PieceMove(move.start, move.start + 1, SpecialMove.NONE);
            PrevMoveGameState midCastleState = game.makeMove(midCastleStateMove, color, piece);
            if(midCastleState.getCapturePiece() != -1 || inCheck(game, color)) {
                game.unMakeMove(midCastleStateMove, color, piece, midCastleState);
                return false;
            }
            game.unMakeMove(midCastleStateMove, color, piece, midCastleState);
        } else if(move.special == SpecialMove.CASTLE_QUEENSIDE) {
            //queenside
            if(game.pieceExists(move.start - 1) ||
                    game.pieceExists(move.start - 2) ||
                    game.pieceExists(move.start - 3)) {
                return false;
            }
            PieceMove midCastleStateMove = new PieceMove(move.start, move.start - 1, SpecialMove.NONE);
            PrevMoveGameState midCastleState = game.makeMove(midCastleStateMove, color, piece);
            if(midCastleState.getCapturePiece() != -1 || inCheck(game, color)) {
                game.unMakeMove(midCastleStateMove, color, piece, midCastleState);
                return false;
            }
            game.unMakeMove(midCastleStateMove, color, piece, midCastleState);
        }
        PrevMoveGameState prevMoveState = game.makeMove(move, color, piece);
        if(inCheck(game, color)) {
            game.unMakeMove(move, color, piece, prevMoveState);
            return false;
        }
        game.unMakeMove(move, color, piece, prevMoveState);
        return true;
    }

    public boolean gameOver(Game game, int color) {
        //Doesn't account for checkmates on the other side, but shouldn't be a problem with the current use case
        return inStalemate(game, color) || inCheckmate(game, color);
    }

    public boolean inCheck(Game game, int color) {
        int opponentColor = color ^ 1;
        for(int square = 0; square < 64; square++) {
            if(game.pieceExists(square) && game.colorOfSquare(square) == opponentColor) {
                if((BitboardUtility.pieceMovesToBitboard(game.pieceMovesFromSquare(square)) &
                        game.piecePositions(color, 5)) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean inCheckmate(Game game, int color) {
        if(!inCheck(game, color)) {
            return false;
        }
        for(int square = 0; square < 64; square++) {
            if(!game.pieceExists(square) || game.colorOfSquare(square) != color) {
                continue;
            }
            int piece = game.pieceOfSquare(square);
            for(PieceMove move : game.pieceMovesFromSquare(square)) {
                if(!validMove(game, move, color, piece)) {
                    continue;
                }
                PrevMoveGameState prevMoveState = game.makeMove(move, color, piece);
                if(!inCheck(game, color)) {
                    game.unMakeMove(move, color, piece, prevMoveState);
                    return false;
                }
                game.unMakeMove(move, color, piece, prevMoveState);
            }
        }
        return true;
    }

    public boolean inStalemate(Game game, int color) {
        if(inCheck(game, color)) {
            return false;
        }
        for(int square = 0; square < 64; square++) {
            if(!game.pieceExists(square) || game.colorOfSquare(square) != color) {
                continue;
            }
            int piece = game.pieceOfSquare(square);
            for(PieceMove move : game.pieceMovesFromSquare(square)) {
                if(!validMove(game, move, color, piece)) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

}
