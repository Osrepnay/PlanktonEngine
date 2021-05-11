package io.github.osrepnay.plankton;

import java.util.ArrayList;
import java.util.List;

public class BitboardUtility {

	private BitboardUtility() {}

	public static int scanDown(long board) {
		return 63 - Long.numberOfLeadingZeros(board);
	}

	public static int scanUp(long board) {
		return Long.numberOfTrailingZeros(board);
	}

	public static List<PieceMove> bitboardToPieceMoves(int start, long board) {
		List<PieceMove> pieceMoves = new ArrayList<>();
		for(int i = 0; i < 64; i++) {
			if(((board >> i) & 1L) != 0) {
				pieceMoves.add(new PieceMove(start, i, SpecialMove.NONE));
			}
		}
		return pieceMoves;
	}

	public static long pieceMovesToBitboard(List<PieceMove> pieceMoves) {
		long board = 0L;
		for(PieceMove square : pieceMoves) {
			board |= 1L << square.end;
		}
		return board;
	}

	public static String toString(long board) {
		StringBuilder string = new StringBuilder();
		for(int i = 7; i >= 0; i--) {
			for(int j = 0; j < 8; j++) {
				string.append(((board >> (j + i * 8) & 1) != 0) ? "0 " : "- ");
			}
			string.append("\n");
		}
		return string.toString();
	}

}
