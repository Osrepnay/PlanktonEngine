package io.github.osrepnay.plankton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;

public class UCIInterface {

	private volatile static Plankton engine = new Plankton();
	private static Game game = new Game();

	public static void main(String[] args) throws IOException {
		System.out.println("Plankton");
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String[] input = reader.readLine().split(" ");
		int color = 0;
		boolean debug = false;
		inputLoop:
		while(true) {
			switch(input[0]) {
				case "uci":
					System.out.println("id name Plankton");
					System.out.println("id author Nosrep");
					System.out.println("uciok");
					break;
				case "isready":
					System.out.println("readyok");
					break;
				case "ucinewgame":
					game.resetGame();
					break;
				case "position":
					game.resetGame();
					int offset = 3;
					if(input.length <= 2) {
						color = 0;
					} else {
						color = input.length % 2 == 0 ? 1 : 0;
					}
					if(input[1].equals("fen")) {
						if(input.length <= 8) {
							color = input[3].equals("w") ? 0 : 1;
						}
						parseFEN(game, String.join(" ", Arrays.copyOfRange(input, 2, input.length)));
						offset = 9;
					}
					if(debug) {
						System.out.printf("info string color %s\n", color);
					}
					for(int i = offset; i < input.length; i++) {
						int startPos = (input[i].charAt(0) - 'a') + (Character.getNumericValue(
								input[i].charAt(1)) - 1) * 8;
						int move =
								(input[i].charAt(2) - 'a') + (Character.getNumericValue(input[i].charAt(3)) - 1) * 8;
						int moveColor = i % 2 == 0 ? 1 : 0;
						int piece = game.pieceOfSquare(startPos);
						SpecialMove special = SpecialMove.NONE;
						if(piece == 5) {
							if(startPos - move == 2) {
								special = SpecialMove.CASTLE_QUEENSIDE;
							} else if(startPos - move == -2) {
								special = SpecialMove.CASTLE_KINGSIDE;
							}
						} else if(piece == 0) {
							if((move >= 0 && move < 8) || (move >= 56 && move < 64)) {
								switch(input[i].charAt(4)) {
									case 'n':
										special = SpecialMove.PROMOTION_KNIGHT;
										break;
									case 'b':
										special = SpecialMove.PROMOTION_BISHOP;
										break;
									case 'r':
										special = SpecialMove.PROMOTION_ROOK;
										break;
									case 'q':
										special = SpecialMove.PROMOTION_QUEEN;
										break;
								}
							} else {
								if(Math.abs(startPos - move) != 8 && Math.abs(
										startPos - move) != 16 && !game.pieceExists(move)) {
									special = SpecialMove.EN_PASSANT;
								}
							}
						}
						game.makeMove(new PieceMove(startPos, move, special), moveColor, piece);
						if(debug) {
							System.out.println(new PieceMove(startPos, move, special));
						}
						color = moveColor ^ 1;
					}
					game.setMoves();
					break;
				case "go":
					long[] times = new long[2];
					long moveTime = -1;
					boolean infinite = false;
					int depth = -1;
					for(int i = 1; i < input.length; i += 2) {
						switch(input[i]) {
							case "wtime":
								times[0] = Integer.parseInt(input[i + 1]);
								break;
							case "btime":
								times[1] = Integer.parseInt(input[i + 1]);
								break;
							case "movetime":
								moveTime = Integer.parseInt(input[i + 1]);
								break;
							case "infinite":
								infinite = true;
								break;
							case "depth":
								depth = Integer.parseInt(input[i + 1]);
						}
					}
					long startTime = System.currentTimeMillis();
					long time = moveTime == -1 ? (color == 0 ? times[0] / 30 : times[1] / 30) : moveTime;
					time += 1000;
					//Prevent taking too long on long time controls
					if(time > 15000) {
						time = 15000;
					}
					double[] bestMove = new double[4];
					engine.keepSearching = true;
					if(depth != -1) {
						bestMove = engine.bestMove(game, color, depth);
						if(debug) {
							System.out.println("info time " + (System.currentTimeMillis() - startTime));
						}
					} else {
						if(!infinite) {
							Thread waitThread = new Thread(new TellEngineStop(time));
							waitThread.start();
						}
						for(int i = 1; keepGoing(i, startTime, time, infinite, depth); i++) {
							double[] bestMoveTemp = engine.bestMove(game, color, i);
							if(bestMoveTemp[0] != -1) {
								bestMove = bestMoveTemp;
							} else {
								if(debug) {
									System.out.println("info time " + (System.currentTimeMillis() - startTime));
								}
								break;
							}
						}
					}
					int[] startPos = new int[] {(int)(bestMove[0] % 8), (int)(bestMove[0] / 8)};
					int[] endPos = new int[] {(int)(bestMove[1] % 8), (int)(bestMove[1] / 8)};
					String printString =
							"bestmove " + (char)(startPos[0] + 'a') + (startPos[1] + 1) + (char)(endPos[0] + 'a') +
									(endPos[1] + 1);
					if(bestMove[3] != -1) {
						switch((int)bestMove[3]) {
							case 1:
								printString += 'n';
								break;
							case 2:
								printString += 'b';
								break;
							case 3:
								printString += 'r';
								break;
							case 4:
								printString += 'q';
								break;
						}
					}
					System.out.println(printString);
					break;
				case "debug":
					if(input[1].equals("on")) {
						debug = true;
					} else if(input[1].equals("off")) {
						debug = false;
					}
					break;
				case "setoption":
					break;
				case "quit":
					break inputLoop;
				default:
					System.out.printf("Invalid command: %s\n", input[0]);
			}
			input = reader.readLine().split(" ");
		}
		reader.close();
	}

	public static void parseFEN(Game game, String fen) {
		game.blankGame();
		String[] fenSections = fen.split(" ");
		String[] boardRows = fenSections[0].split("/");
		HashMap<Character, Integer> pieceToInt = new HashMap<>();
		pieceToInt.put('p', 0);
		pieceToInt.put('n', 1);
		pieceToInt.put('b', 2);
		pieceToInt.put('r', 3);
		pieceToInt.put('q', 4);
		pieceToInt.put('k', 5);
		for(int i = boardRows.length - 1; i >= 0; i--) {
			int offset = 0;
			for(int j = 0; j < 8; j++) {
				int newJ = j - offset;
				if(Character.isDigit(boardRows[i].charAt(j - offset))) {
					j += boardRows[i].charAt(newJ) - '1';
					offset += boardRows[i].charAt(newJ) - '1';
				} else {
					int color = Character.isUpperCase(boardRows[i].charAt(newJ)) ? 0 : 1;
					game.createPiece(color, pieceToInt.get(Character.toLowerCase(boardRows[i].charAt(newJ))),
							new int[] {j, 7 - i});
				}
			}
		}
		if(!fenSections[2].equals("-")) {
			for(int i = 0; i < fenSections[2].length(); i++) {
				switch(fenSections[2].charAt(i)) {
					case 'K':
						game.setCastleAvailable(0, true);
						break;
					case 'k':
						game.setCastleAvailable(2, true);
						break;
					case 'Q':
						game.setCastleAvailable(1, true);
						break;
					case 'q':
						game.setCastleAvailable(3, true);
						break;
				}
			}
		} else {
			for(int i = 0; i < 4; i++) {
				game.setCastleAvailable(i, false);
			}
		}
	}

	private static boolean keepGoing(int i, long startTime, long time, boolean infinite, int depth) {
		if(depth != -1) {
			return i <= depth;
		} else if(!infinite) {
			return System.currentTimeMillis() - startTime < time;
		}
		return true;
	}

	static class TellEngineStop implements Runnable {

		private long waitTime;

		public TellEngineStop(long waitTime) {
			this.waitTime = waitTime;
		}

		@Override
		public void run() {
			try {
				Thread.sleep(waitTime);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
			engine.keepSearching = false;
		}

	}

}
