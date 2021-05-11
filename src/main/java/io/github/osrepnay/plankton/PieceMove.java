package io.github.osrepnay.plankton;

import java.util.Objects;

public class PieceMove {
	int start;
	int end;
	SpecialMove special;

	public PieceMove(int start, int end, SpecialMove special) {
		this.start = start;
		this.end = end;
		this.special = special;
	}

	@Override
	public int hashCode() {
		return Objects.hash(start, end, special);
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof PieceMove)) {
			return false;
		}
		PieceMove pieceMove = (PieceMove)obj;
		return pieceMove.start == start && pieceMove.end == end && pieceMove.special == special;
	}

	@Override
	public String toString() {
		return "{Start: " + start + ", End: " + end + ", Special type: " + special + "}";
	}

}
