package io.github.osrepnay.plankton;

import java.util.ArrayList;
import java.util.List;

public class MoveGen {

	long[][] rays = new long[64][8];
	long[] bishopMasks = new long[64];
	long[] rookMasks = new long[64];
	long[] bishopTable = new long[65536];
	long[] rookTable = new long[262144];
	long[] knightMoves = new long[64];
	long[] kingMoves = new long[64];

	public MoveGen() {
		for(int i = 0; i < knightMoves.length; i++) {
			int[] possibleKnightMoves = new int[8];
			possibleKnightMoves[0] = i + 15;
			possibleKnightMoves[1] = i + 17;
			possibleKnightMoves[2] = i + 10;
			possibleKnightMoves[3] = i - 6;
			possibleKnightMoves[4] = i - 15;
			possibleKnightMoves[5] = i - 17;
			possibleKnightMoves[6] = i - 10;
			possibleKnightMoves[7] = i + 6;
			for(int possibleKnightMove : possibleKnightMoves) {
				if(possibleKnightMove >= 0 && possibleKnightMove < 64) {
					if(!((i - 1) % 8 == 0 && (possibleKnightMove + 1) % 8 == 0) &&
							!(i % 8 == 0 && ((possibleKnightMove + 2) % 8 == 0 || (possibleKnightMove + 1) % 8 == 0)) &&
							!((i + 2) % 8 == 0 && possibleKnightMove % 8 == 0) &&
							!((i + 1) % 8 == 0 && ((possibleKnightMove - 1) % 8 == 0 || possibleKnightMove % 8 == 0))) {
						knightMoves[i] |= 1L << possibleKnightMove;
					}
				}
			}
		}
		for(int i = 0; i < kingMoves.length; i++) {
			int[] possibleKingMoves = new int[8];
			possibleKingMoves[0] = i + 8;
			possibleKingMoves[1] = i + 9;
			possibleKingMoves[2] = i + 1;
			possibleKingMoves[3] = i - 7;
			possibleKingMoves[4] = i - 8;
			possibleKingMoves[5] = i - 9;
			possibleKingMoves[6] = i - 1;
			possibleKingMoves[7] = i + 7;
			for(int possibleKingMove : possibleKingMoves) {
				if(possibleKingMove >= 0 && possibleKingMove < 64) {
					if(!(i % 8 == 0 && (possibleKingMove + 1) % 8 == 0) &&
							!((i + 1) % 8 == 0 && possibleKingMove % 8 == 0)) {
						kingMoves[i] |= 1L << possibleKingMove;
					}
				}
			}
		}
		for(int i = 0; i < rays.length; i++) {
			for(int j = 0; j < rays[i].length; j++) {
				rays[i][j] = genRay(i, j);
				if(j % 2 == 0) {
					long edge_mask = 0;
					switch(j) {
						case 0:
							edge_mask = ~0xff00000000000000L;
							break;
						case 2:
							edge_mask = ~0x8080808080808080L;
							break;
						case 4:
							edge_mask = ~0xffL;
							break;
						case 6:
							edge_mask = ~0x101010101010101L;
							break;
					}
					rookMasks[i] |= rays[i][j] & edge_mask;
				} else {
					bishopMasks[i] |= rays[i][j] & 35604928818740736L;
				}
			}
		}
		for(int square = 0; square < 64; square++) {
			for(int bishopBlockersIdx = 0;
				bishopBlockersIdx < (1L << Magics.BISHOP_INDICES[square]); bishopBlockersIdx++) {
				long blockers = blockerFromIdx(bishopBlockersIdx, bishopMasks[square]);
				long key = (blockers * Magics.BISHOP_MAGICS[square]) >>> (64 - Magics.BISHOP_INDICES[square]);
				bishopTable[square * 512 + (int)key] = genBishopClassical(rays, square, blockers);
			}
			for(int rookBlockersIdx = 0; rookBlockersIdx < (1L << Magics.ROOK_INDICES[square]); rookBlockersIdx++) {
				long blockers = blockerFromIdx(rookBlockersIdx, rookMasks[square]);
				long key = (blockers * Magics.ROOK_MAGICS[square]) >>> (64 - Magics.ROOK_INDICES[square]);
				rookTable[square * 4096 + (int)key] = genRookClassical(rays, square, blockers);
			}
		}
	}

	public List<PieceMove> genMove(int position, long blockers, boolean[] castleAvailable, int color, int piece) {
		switch(piece) {
			case 0:
				return genPawn(position, blockers, color);
			case 1:
				return BitboardUtility.bitboardToPieceMoves(position, knightMoves[position]);
			case 2:
				return genBishop(position, blockers);
			case 3:
				return genRook(position, blockers);
			case 4:
				List<PieceMove> bishopMoves = genBishop(position, blockers);
				List<PieceMove> rookMoves = genRook(position, blockers);
				bishopMoves.addAll(rookMoves);
				return bishopMoves;
			case 5:
				return genKing(position, blockers, castleAvailable, color);
			default:
				throw new IllegalArgumentException(String.format("Invalid Piece %s", piece));
		}
	}

	public List<PieceMove> genPawn(int position, long blockers, int color) {
		List<PieceMove> pieceMoves = new ArrayList<>();
		int posChange = -((color * 2 - 1) * 8);
		//List of possible promotions if is promotion, SpecialMove.NONE if not
		SpecialMove[] promotions;
		if(position + posChange >= 56 || position + posChange < 8) {
			promotions = new SpecialMove[] {SpecialMove.PROMOTION_KNIGHT, SpecialMove.PROMOTION_BISHOP,
					SpecialMove.PROMOTION_ROOK, SpecialMove.PROMOTION_QUEEN};
		} else {
			promotions = new SpecialMove[] {SpecialMove.NONE};
		}
		if(((blockers >> (position + posChange)) & 1) == 0) {
			for(SpecialMove promotion : promotions) {
				pieceMoves.add(new PieceMove(position, position + posChange, promotion));
			}
			if(color == 0) {
				//if still on starting position
				if(position >= 8 && position < 16 && ((blockers >> (position + 2 * posChange)) & 1) == 0) {
					for(SpecialMove promotion : promotions) {
						pieceMoves.add(new PieceMove(position, position + 2 * posChange, promotion));
					}
				}
			} else {
				//if still on starting position
				if(position >= 48 && position < 56 && ((blockers >> (position + 2 * posChange)) & 1) == 0) {
					for(SpecialMove promotion : promotions) {
						pieceMoves.add(new PieceMove(position, position + 2 * posChange, promotion));
					}
				}
			}
		}
		if(position + posChange + 1 >= 0 && position + posChange + 1 < 64 &&
				((blockers >> (position + posChange + 1)) & 1) != 0 && (position + 1) % 8 != 0) {
			for(SpecialMove promotion : promotions) {
				pieceMoves.add(new PieceMove(position, position + posChange + 1, promotion));
			}
		}
		if(position + posChange - 1 >= 0 && position + posChange - 1 < 64 &&
				((blockers >> (position + posChange - 1)) & 1) != 0 && position % 8 != 0) {
			for(SpecialMove promotion : promotions) {
				pieceMoves.add(new PieceMove(position, position + posChange - 1, promotion));
			}
		}
		return pieceMoves;
	}

	public List<PieceMove> genBishop(int position, long blockers) {
		long maskedBlockers = blockers & bishopMasks[position];
		long key = (maskedBlockers * Magics.BISHOP_MAGICS[position]) >>> (64 - Magics.BISHOP_INDICES[position]);
		return BitboardUtility.bitboardToPieceMoves(position, bishopTable[position * 512 + (int)key]);
	}

	public List<PieceMove> genRook(int position, long blockers) {
		long maskedBlockers = blockers & rookMasks[position];
		long key = (maskedBlockers * Magics.ROOK_MAGICS[position]) >>> (64 - Magics.ROOK_INDICES[position]);
		return BitboardUtility.bitboardToPieceMoves(position, rookTable[position * 4096 + (int)key]);
	}

	public static long genBishopClassical(long[][] rays, int position, long blockers) {
		long board = 0L;
		for(int i = 0; i < 4; i++) {
			long maskedBlockers = blockers & rays[position][i * 2 + 1];
			int firstBlockerPosition = -1;
			long moves;
			if(maskedBlockers == 0) {
				moves = rays[position][i * 2 + 1];
			} else {
				switch(i * 2 + 1) {
					case 1:
					case 7:
						firstBlockerPosition = BitboardUtility.scanUp(maskedBlockers);
						break;
					case 3:
					case 5:
						firstBlockerPosition = BitboardUtility.scanDown(maskedBlockers);
						break;
				}
				long blockerRay = rays[firstBlockerPosition][i * 2 + 1];
				moves = rays[position][i * 2 + 1] & ~blockerRay;
			}
			board |= moves;
		}
		return board;
	}

	public static long genRookClassical(long[][] rays, int position, long blockers) {
		long board = 0L;
		for(int i = 0; i < 4; i++) {
			long maskedBlockers = blockers & rays[position][i * 2];
			int firstBlockerPosition = -1;
			long moves;
			if(maskedBlockers == 0) {
				moves = rays[position][i * 2];
			} else {
				switch(i * 2) {
					case 0:
					case 2:
						firstBlockerPosition = BitboardUtility.scanUp(maskedBlockers);
						break;
					case 4:
					case 6:
						firstBlockerPosition = BitboardUtility.scanDown(maskedBlockers);
						break;
				}
				long blockerRay = rays[firstBlockerPosition][i * 2];
				moves = rays[position][i * 2] & ~blockerRay;
			}
			board |= moves;
		}
		return board;
	}

	public List<PieceMove> genKing(int position, long blockers, boolean[] castleAvailable, int color) {
		List<PieceMove> moves = BitboardUtility.bitboardToPieceMoves(position, kingMoves[position]);
		if(castleAvailable[color * 2] && ((blockers >> (position + 1)) & 1) == 0 &&
				((blockers >> (position + 2)) & 1) == 0) {
			moves.add(new PieceMove(position, position + 2, SpecialMove.CASTLE_KINGSIDE));
		}
		if(castleAvailable[color * 2 + 1] && ((blockers >> (position - 1)) & 1) == 0 &&
				((blockers >> (position - 2)) & 1) == 0 && ((blockers >> (position - 3)) & 1) == 0) {
			moves.add(new PieceMove(position, position - 2, SpecialMove.CASTLE_QUEENSIDE));
		}
		return moves;
	}

	private long genRay(int position, int direction) {
		long ray = 0L;
		switch(direction) {
			case 0:
				// up
				for(int i = position; i < 64; i += 8) {
					ray |= 1L << i;
				}
				break;
			case 1:
				// up-right
				for(int i = position; i < 64 && !(i % 8 == 0 && i != position); i += 9) {
					ray |= 1L << i;
				}
				break;
			case 2:
				// right
				for(int i = position; i < 64 && !(i % 8 == 0 && i != position); i++) {
					ray |= 1L << i;
				}
				break;
			case 3:
				// down-right
				for(int i = position; i >= 0 && !(i % 8 == 0 && i != position); i -= 7) {
					ray |= 1L << i;
				}
				break;
			case 4:
				// down
				for(int i = position; i >= 0; i -= 8) {
					ray |= 1L << i;
				}
				break;
			case 5:
				// down-left
				for(int i = position; i >= 0 && !((i + 1) % 8 == 0 && i != position); i -= 9) {
					ray |= 1L << i;
				}
				break;
			case 6:
				// left
				for(int i = position; i >= 0 && !((i + 1) % 8 == 0 && i != position); i--) {
					ray |= 1L << i;
				}
				break;
			case 7:
				// up-left
				for(int i = position; i < 64 && !((i + 1) % 8 == 0 && i != position); i += 7) {
					ray |= 1L << i;
				}
				break;
		}
		ray &= ~(1L << position);
		return ray;
	}

	private long blockerFromIdx(int idx, long mask) {
		long blockers = 0L;
		int bitAt = 0;
		while(mask != 0) {
			int lsb = Long.numberOfTrailingZeros(mask);
			mask &= ~(1L << lsb);
			if((idx & (1 << bitAt)) != 0) {
				blockers |= 1L << lsb;
			}
			bitAt++;
		}
		return blockers;
	}

}
