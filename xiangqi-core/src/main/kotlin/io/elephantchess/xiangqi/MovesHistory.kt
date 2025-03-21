package io.elephantchess.xiangqi

import io.elephantchess.utils.SequenceUtils.allCombinationsOfMax
import io.elephantchess.utils.SequenceUtils.longestConsecutiveSequence
import io.elephantchess.xiangqi.AbstractPieceType.CHARIOT
import io.elephantchess.xiangqi.Color.RED
import io.elephantchess.xiangqi.Position.Companion.parsePositionFromUci

data class MovesHistory(val history: List<HistoricalMove>) {

    /**
     * The key of the map is the number of attackers involved in the check sequence
     */
    fun findSequencesOfConsecutiveChecks(color: Color, maxAttackers: Int = 3): Map<Int, SequenceOfChecks> {
        return detectConsecutiveChecks(color, maxAttackers)
            .groupBy { sequence -> sequence.attackers.size }
            .map { (nbrOfAttackers, sequences) ->
                nbrOfAttackers to sequences.maxBy { it.moves.size }
            }
            .toMap()
    }

    fun detectConsecutiveChecks(color: Color, maxAttackers: Int): List<SequenceOfChecks> {
        fun filterOutSubSets(sequences: List<SequenceOfChecks>): List<SequenceOfChecks> {
            return sequences.filterNot { sequence -> sequences.any { sequence.isSubSetOf(it) } }
        }

        val sequences = allCombinationsOfMax(findAllCheckingPieces(color), maxAttackers)
            .flatMap { pieces ->
                val moves = longestConsecutiveChecks(pieces)

//                println("pieces: $pieces")
//                if (pieces == setOf(PhysicalPiece(PieceType(CHARIOT, RED), parsePositionFromUci("a0")))) {
//                    println("moves: ${moves.size}")
//                    println("areAllPiecesInvolved: ${areAllPiecesInvolved(pieces, moves)}")
//                }

                if (moves.size > 1 && areAllPiecesInvolved(pieces, moves)) {
                    listOf(SequenceOfChecks(color, pieces, moves))
                } else {
                    emptyList()
                }
            }

        return filterOutSubSets(sequences)
    }

    fun findAllCheckingPieces(color: Color): List<PhysicalPiece> {
        return history
            .flatMap { move -> move.attackMap.keys }
            .map { pieceAtPosition -> pieceAtPosition.physicalPiece }
            .filter { piece -> piece.color() == color }
            .distinct()
    }

    fun longestConsecutiveChecks(attackingPieces: Collection<PhysicalPiece>): List<HistoricalMove> {
        val checkingMoves = history
            .filter { move -> move.isCheckingOpponent }
            .filter { move -> attackingPieces.any { move.isAttackingGeneral(it) } }

        return longestConsecutiveSequence(checkingMoves) { previous, current -> previous.index + 2 == current.index }
    }

    companion object {

        /**
         * Each piece is attacking the general at least once in the moves
         */
        fun areAllPiecesInvolved(pieces: Collection<PhysicalPiece>, moves: Collection<HistoricalMove>): Boolean {
            return pieces.all { piece -> moves.any { move -> move.isAttackingGeneral(piece) } }
        }

    }

}
