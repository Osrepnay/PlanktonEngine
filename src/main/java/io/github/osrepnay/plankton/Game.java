package io.github.osrepnay.plankton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Game implements Cloneable {
	private long[][] piecePositions = new long[2][6];
	private HashMap<Integer, Integer> squareToColor = new HashMap<>();
	private HashMap<Integer, Integer> squareToPiece = new HashMap<>();
	@SuppressWarnings("unchecked")
	private List<PieceMove>[] pieceMoves = (ArrayList<PieceMove>[])new ArrayList[64];
	private boolean[] castleAvailable = new boolean[] {true, true, true, true};
	private MoveGen moveGen = new MoveGen();

	public Game() {
		resetGame();
	}

	public PrevMoveGameState makeMove(PieceMove move, int color, int piece) {
		squareToColor.remove(move.start);
		squareToPiece.remove(move.start);
		int opponentColor = color ^ 1;
		int capturePiece = -1;
		if(squareToColor.containsKey(move.end)) {
			capturePiece = squareToPiece.get(move.end);
			piecePositions[opponentColor][capturePiece] &= ~(1L << move.end);
			squareToColor.remove(move.end);
			squareToPiece.remove(move.end);
		}
		squareToColor.put(move.end, color);
		squareToPiece.put(move.end, piece);
		piecePositions[color][piece] &= ~(1L << move.start);
		piecePositions[color][piece] |= 1L << move.end;
		boolean[] prevCastleAvailable = Arrays.copyOf(castleAvailable, castleAvailable.length);
		if(piece == 5) {
			castleAvailable = new boolean[4];
			if(move.special == SpecialMove.CASTLE_KINGSIDE) {
				//kingside
				piecePositions[color][3] &= ~(1L << (move.end + 1));
				piecePositions[color][3] |= 1L << (move.end - 1);
				squareToColor.remove(move.end + 1);
				squareToPiece.remove(move.end + 1);
				squareToColor.put(move.end - 1, color);
				squareToPiece.put(move.end - 1, 3);
			} else if(move.special == SpecialMove.CASTLE_QUEENSIDE) {
				//queenside
				piecePositions[color][3] &= ~(1L << (move.end - 2));
				piecePositions[color][3] |= 1L << (move.end + 1);
				squareToColor.remove(move.end - 2);
				squareToPiece.remove(move.end - 2);
				squareToColor.put(move.end + 1, color);
				squareToPiece.put(move.end + 1, 3);
			}
		} else if(piece == 3) {
			switch(move.start) {
				case 0:
					castleAvailable[1] = false;
					break;
				case 7:
					castleAvailable[0] = false;
					break;
				case 56:
					castleAvailable[3] = false;
					break;
				case 63:
					castleAvailable[2] = false;
					break;
			}
		} else if(piece == 0) {
			switch(move.special) {
				case PROMOTION_KNIGHT:
					piecePositions[color][piece] &= ~(1L << move.end);
					piecePositions[color][1] |= 1L << move.end;
					squareToPiece.remove(move.end);
					squareToPiece.put(move.end, 1);
					break;
				case PROMOTION_BISHOP:
					piecePositions[color][piece] &= ~(1L << move.end);
					piecePositions[color][2] |= 1L << move.end;
					squareToPiece.remove(move.end);
					squareToPiece.put(move.end, 2);
					break;
				case PROMOTION_ROOK:
					piecePositions[color][piece] &= ~(1L << move.end);
					piecePositions[color][3] |= 1L << move.end;
					squareToPiece.remove(move.end);
					squareToPiece.put(move.end, 3);
					break;
				case PROMOTION_QUEEN:
					piecePositions[color][piece] &= ~(1L << move.end);
					piecePositions[color][4] |= 1L << move.end;
					squareToPiece.remove(move.end);
					squareToPiece.put(move.end, 4);
					break;
				case EN_PASSANT:
					capturePiece = 0;
					if(color == 0) {
						if(move.end - move.start == 7) {
							piecePositions[opponentColor][0] &= ~(1L << (move.start - 1));
							squareToColor.remove(move.start - 1);
							squareToPiece.remove(move.start - 1);
						} else {
							piecePositions[opponentColor][0] &= ~(1L << (move.start + 1));
							squareToColor.remove(move.start + 1);
							squareToPiece.remove(move.start + 1);
						}
					} else {
						if(move.start - move.end == 7) {
							piecePositions[opponentColor][0] &= ~(1L << (move.start + 1));
							squareToColor.remove(move.start + 1);
							squareToPiece.remove(move.start + 1);
						} else {
							piecePositions[opponentColor][0] &= ~(1L << (move.start - 1));
							squareToColor.remove(move.start - 1);
							squareToPiece.remove(move.start - 1);
						}
					}
					break;
			}
		}
		PrevMoveGameState prevMoveState = new PrevMoveGameState(capturePiece, prevCastleAvailable);
		setMoves();
		return prevMoveState;
	}

	public void unMakeMove(PieceMove move, int color, int piece, PrevMoveGameState prevMoveState) {
		squareToColor.remove(move.end);
		squareToPiece.remove(move.end);
		int opponentColor = color ^ 1;
		if(prevMoveState.getCapturePiece() != -1) {
			piecePositions[opponentColor][prevMoveState.getCapturePiece()] |= 1L << move.end;
			squareToColor.put(move.end, opponentColor);
			squareToPiece.put(move.end, prevMoveState.getCapturePiece());
		}
		squareToColor.put(move.start, color);
		squareToPiece.put(move.start, piece);
		piecePositions[color][piece] |= 1L << move.start;
		piecePositions[color][piece] &= ~(1L << move.end);
		castleAvailable = prevMoveState.getCastleAvailable();
		if(piece == 5) {
			if(move.special == SpecialMove.CASTLE_KINGSIDE) {
				//kingside
				piecePositions[color][3] |= 1L << (move.end + 1);
				piecePositions[color][3] &= ~(1L << (move.end - 1));
				squareToColor.remove(move.end - 1);
				squareToPiece.remove(move.end - 1);
				squareToColor.put(move.end + 1, color);
				squareToPiece.put(move.end + 1, 3);
			} else if(move.special == SpecialMove.CASTLE_QUEENSIDE) {
				//queenside
				piecePositions[color][3] |= 1L << (move.end - 2);
				piecePositions[color][3] &= ~(1L << (move.end + 1));
				squareToColor.remove(move.end + 1);
				squareToPiece.remove(move.end + 1);
				squareToColor.put(move.end - 2, color);
				squareToPiece.put(move.end - 2, 3);
			}
		} else if(piece == 0) {
			switch(move.special) {
				case PROMOTION_KNIGHT:
					piecePositions[color][piece] |= 1L << move.start;
					piecePositions[color][1] &= ~(1L << move.end);
					squareToPiece.remove(move.start);
					squareToPiece.put(move.start, 0);
					break;
				case PROMOTION_BISHOP:
					piecePositions[color][piece] |= 1L << move.start;
					piecePositions[color][2] &= ~(1L << move.end);
					squareToPiece.remove(move.start);
					squareToPiece.put(move.start, 0);
					break;
				case PROMOTION_ROOK:
					piecePositions[color][piece] |= 1L << move.start;
					piecePositions[color][3] &= ~(1L << move.end);
					squareToPiece.remove(move.start);
					squareToPiece.put(move.start, 0);
					break;
				case PROMOTION_QUEEN:
					piecePositions[color][piece] |= 1L << move.start;
					piecePositions[color][4] &= ~(1L << move.end);
					squareToPiece.remove(move.start);
					squareToPiece.put(move.start, 0);
					break;
				case EN_PASSANT:
					if(color == 0) {
						if(move.end - move.start == 7) {
							piecePositions[opponentColor][0] |= 1L << (move.start - 1);
							squareToColor.put(move.start - 1, opponentColor);
							squareToPiece.put(move.start - 1, 0);
						} else {
							piecePositions[opponentColor][0] |= 1L << (move.start + 1);
							squareToColor.put(move.start + 1, opponentColor);
							squareToPiece.put(move.start + 1, 0);
						}
					} else {
						if(move.start - move.end == 7) {
							piecePositions[opponentColor][0] |= 1L << (move.start + 1);
							squareToColor.put(move.start + 1, opponentColor);
							squareToPiece.put(move.start + 1, 0);
						} else {
							piecePositions[opponentColor][0] |= 1L << (move.start - 1);
							squareToColor.put(move.start - 1, opponentColor);
							squareToPiece.put(move.start - 1, 0);
						}
					}
					break;
			}
		}
		setMoves();
	}

	public void createPiece(int color, int piece, int[] position) {
		piecePositions[color][piece] |= 1L << (position[0] + position[1] * 8);
		squareToColor.put(position[0] + position[1] * 8, color);
		squareToPiece.put(position[0] + position[1] * 8, piece);
	}

	public void deletePiece(int color, int piece, int[] position) {
		piecePositions[color][piece] &= ~(1L << (position[0] + position[1] * 8));
		squareToColor.remove(position[0] + position[1] * 8);
		squareToPiece.remove(position[0] + position[1] * 8);
	}

	public void setMoves() {
		long blockers = 0L;
		for(long[] piecePosition : piecePositions) {
			for(long piece : piecePosition) {
				blockers |= piece;
			}
		}
		for(int i = 0; i < 64; i++) {
			pieceMoves[i] = new ArrayList<>();
		}
		for(int color = 0; color < piecePositions.length; color++) {
			for(int piece = 0; piece < piecePositions[color].length; piece++) {
				for(int position = 0; position < 64; position++) {
					if(((piecePositions[color][piece] >> position) & 1) != 0) {
						pieceMoves[position] = moveGen.genMove(position, blockers, castleAvailable, color, piece);
					}
				}
			}
		}
	}

	public void resetGame() {
		for(long[] piecePosition : piecePositions) {
			Arrays.fill(piecePosition, 0L);
		}
		squareToColor = new HashMap<>();
		squareToPiece = new HashMap<>();

		for(int i = 0; i < 8; i++) {
			createPiece(0, 0, new int[] {i, 1});
			createPiece(1, 0, new int[] {i, 6});
		}
		createPiece(0, 1, new int[] {1, 0});
		createPiece(0, 1, new int[] {6, 0});
		createPiece(0, 2, new int[] {2, 0});
		createPiece(0, 2, new int[] {5, 0});
		createPiece(0, 3, new int[] {0, 0});
		createPiece(0, 3, new int[] {7, 0});
		createPiece(0, 4, new int[] {3, 0});
		createPiece(0, 5, new int[] {4, 0});

		createPiece(1, 1, new int[] {1, 7});
		createPiece(1, 1, new int[] {6, 7});
		createPiece(1, 2, new int[] {2, 7});
		createPiece(1, 2, new int[] {5, 7});
		createPiece(1, 3, new int[] {0, 7});
		createPiece(1, 3, new int[] {7, 7});
		createPiece(1, 4, new int[] {3, 7});
		createPiece(1, 5, new int[] {4, 7});
		castleAvailable = new boolean[] {true, true, true, true};
		setMoves();
	}

	public void blankGame() {
		for(long[] piecePosition : piecePositions) {
			Arrays.fill(piecePosition, 0L);
		}
		squareToColor = new HashMap<>();
		squareToPiece = new HashMap<>();
		castleAvailable = new boolean[4];
		setMoves();
	}

	public long[] piecePositionsFromColor(int color) {
		return Arrays.copyOf(piecePositions[color], piecePositions[color].length);
	}

	public long piecePositions(int color, int piece) {
		return piecePositions[color][piece];
	}

	public int colorOfSquare(int square) {
		return squareToColor.get(square);
	}

	public int pieceOfSquare(int square) {
		return squareToPiece.get(square);
	}

	public boolean pieceExists(int square) {
		return squareToColor.containsKey(square);
	}

	public List<PieceMove> pieceMovesFromSquare(int square) {
		List<PieceMove> copy = new ArrayList<>();
		for(PieceMove pieceMove : pieceMoves[square]) {
			copy.add(new PieceMove(pieceMove.start, pieceMove.end, pieceMove.special));
		}
		return copy;
	}

	public PieceMove pieceMove(int square, int moveIdx) {
		return pieceMoves[square].get(moveIdx);
	}

	public boolean castleAvailable(int idx) {
		return castleAvailable(idx);
	}

	public void setCastleAvailable(int idx, boolean value) {
		castleAvailable[idx] = value;
	}

    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        Game gameClone = new Game();
        gameClone.piecePositions = new long[2][6];
        for(int i = 0; i < piecePositions.length; i++) {
            gameClone.piecePositions[i] = piecePositions[i].clone();
        }
        gameClone.squareToColor = (HashMap<Integer, Integer>)squareToColor.clone();
        gameClone.squareToPiece = (HashMap<Integer, Integer>)squareToPiece.clone();
        gameClone.pieceMoves = pieceMoves.clone();
        gameClone.castleAvailable = castleAvailable.clone();
        gameClone.moveGen = moveGen;
        return (Object)gameClone;
    }

	@Override
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(!(o instanceof Game)) {
			return false;
		}
		Game game = (Game)o;
		return Arrays.equals(piecePositions, game.piecePositions) && Arrays.equals(castleAvailable,
				game.castleAvailable);
	}

	@Override
	public int hashCode() {
		int result = Arrays.hashCode(piecePositions);
		result = 31 * result + Arrays.hashCode(castleAvailable);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder string = new StringBuilder();
		for(int i = 7; i >= 0; i--) {
			for(int j = 0; j < 8; j++) {
				if(pieceExists(j + i * 8)) {
					String appendTo = "- ";
					switch(pieceOfSquare(j + i * 8)) {
						case 0:
							appendTo = "p ";
							break;
						case 1:
							appendTo = "n ";
							break;
						case 2:
							appendTo = "b ";
							break;
						case 3:
							appendTo = "r ";
							break;
						case 4:
							appendTo = "q ";
							break;
						case 5:
							appendTo = "k ";
							break;
					}
					if(colorOfSquare(j + i * 8) == 0) {
						appendTo = appendTo.toUpperCase();
					}
					string.append(appendTo);
				} else {
					string.append("- ");
				}
			}
			string.append("\n");
		}
		return string.toString();
	}

}
