package io.github.osrepnay.plankton;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class PositionalTest {

	private Plankton engine = new Plankton();
	private Game game = new Game();

	@Test
	public void testCheckmates() {
		//basic rook back-rank
		game.blankGame();
		game.createPiece(1, 5, new int[] {0, 0});
		game.createPiece(0, 5, new int[] {0, 2});
		game.createPiece(0, 3, new int[] {7, 1});
		game.setMoves();
		double[] bestMove = engine.bestMove(game, 0, 1);
		Assert.assertArrayEquals(new double[] {15, 7, 10000, -1}, bestMove, 0);
	}

	@Test
	public void testStalemates() {
		//choice between material and stalemate or keep playing
		game.blankGame();
		game.createPiece(1, 5, new int[] {0, 0});
		game.createPiece(1, 2, new int[] {2, 1});
		game.createPiece(0, 4, new int[] {3, 1});
		game.createPiece(0, 5, new int[] {7, 7});
		game.setMoves();
		double[] bestMove = engine.bestMove(game, 0, 1);
		//assertArrayNotEquals?
		Assert.assertFalse(Arrays.equals(new int[] {(int)bestMove[0], (int)bestMove[1]}, new int[] {11, 10}));
	}

	@Test
	public void testMaterialGain() {
		//queen fork
		game.blankGame();
		game.createPiece(1, 5, new int[] {0, 0});
		game.createPiece(1, 4, new int[] {0, 2});
		game.createPiece(0, 5, new int[] {7, 7});
		game.createPiece(0, 1, new int[] {4, 0});
		game.setMoves();
		double[] bestMove = engine.bestMove(game, 0, 3);
		Assert.assertArrayEquals(new double[] {4, 10}, Arrays.copyOfRange(bestMove, 0, 2), 0);
	}

}
