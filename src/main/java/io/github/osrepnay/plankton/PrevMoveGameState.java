package io.github.osrepnay.plankton;

import java.util.Arrays;
import java.util.Objects;

/**
 * Stores some stuff that {@link Game#unMakeMove} uses to restore the previous position
 */
public class PrevMoveGameState {

	private int capturePiece;
	private boolean[] castleAvailable;

	public PrevMoveGameState(int capturePiece, boolean[] castleAvailable) {
		this.capturePiece = capturePiece;
		this.castleAvailable = Arrays.copyOf(castleAvailable, castleAvailable.length);
	}

	public int getCapturePiece() {
		return capturePiece;
	}

	public boolean[] getCastleAvailable() {
		return castleAvailable;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null || getClass() != o.getClass())
			return false;
		PrevMoveGameState that = (PrevMoveGameState)o;
		return capturePiece == that.capturePiece && Arrays.equals(castleAvailable, that.castleAvailable);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(capturePiece);
		result = 31 * result + Arrays.hashCode(castleAvailable);
		return result;
	}

	@Override
	public String toString() {
		return "Piece captured: " + capturePiece + ", Castles available: " + Arrays.toString(castleAvailable);
	}

}
