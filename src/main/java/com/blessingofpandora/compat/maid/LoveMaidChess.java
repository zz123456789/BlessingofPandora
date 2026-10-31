package com.blessingofpandora.compat.maid;

import com.blessingofpandora.compat.love.LoveProgressEvents;
import com.github.tartaricacid.touhoulittlemaid.api.block.IBoardGameEntityBlock;
import com.github.tartaricacid.touhoulittlemaid.api.game.gomoku.Point;
import com.github.tartaricacid.touhoulittlemaid.api.game.gomoku.Statue;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidGomokuAI;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntitySit;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityCChess;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityGomoku;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityWChess;
import com.github.tartaricacid.touhoulittlemaid.util.CChessUtil;
import com.github.tartaricacid.touhoulittlemaid.util.WChessUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Optional Maid API adapter. Called only by the mod's authoritative player-win branches. */
public final class LoveMaidChess
{
    private static final String CREDITED = "blessingofpandora:love_chess_credited";
    private LoveMaidChess() {}

    /**
     * The upstream block checks the winning player's ownership before applying
     * these three favorability types. Validate the actual board as well: a generic
     * favorability change, replayed advancement or client "maidLost" flag is not a win.
     */
    public static void onFavorability(EntityMaid maid, Type type)
    {
        if (type != Type.GOMOKU_WIN && type != Type.CCHESS_WIN && type != Type.WCHESS_WIN) return;
        if (!(maid.level() instanceof ServerLevel) || !maid.isAlive()
                || !(maid.getOwner() instanceof ServerPlayer player)
                || player.level() != maid.level() || !maid.isOwnedBy(player)) return;
        String game = winningGame(maid, type);
        if (game == null || !(maid.getVehicle() instanceof EntitySit seat)) return;
        BlockEntity board = maid.level().getBlockEntity(seat.getAssociatedBlockPos());
        if (board == null || board.getPersistentData().getBoolean(CREDITED)) return;
        // The upstream chess packet can replay a completed mate position. Persist a
        // round receipt on the real board so reloading a chunk cannot replay it either.
        board.getPersistentData().putBoolean(CREDITED, true);
        board.setChanged();
        LoveProgressEvents.onChessWin(player, maid.getUUID(), game);
    }

    public static void onNewRound(BlockEntity board)
    {
        if (board.getLevel() != null && board.getLevel().isClientSide) return;
        board.getPersistentData().remove(CREDITED);
        board.setChanged();
    }

    /** No world scan: resolve the exact occupied seat and its associated board. */
    public static String winningGame(EntityMaid maid, Type type)
    {
        if (!(maid.level() instanceof ServerLevel level)
                || !(maid.getVehicle() instanceof EntitySit seat)
                || !seat.isAlive() || seat.getFirstPassenger() != maid
                || seat.getAssociatedBlockPos() == null || !level.isLoaded(seat.getAssociatedBlockPos())) return null;
        var blockEntity = level.getBlockEntity(seat.getAssociatedBlockPos());
        if (!(blockEntity instanceof IBoardGameEntityBlock board)
                || !seat.getUUID().equals(board.getSitId())) return null;

        if (type == Type.GOMOKU_WIN && blockEntity instanceof TileEntityGomoku gomoku)
        {
            // In BlockGomoku.use the new stone is committed before the win callback;
            // setStatue(WIN) occurs afterward. Do not require the still-old status.
            Point last = gomoku.getLatestChessPoint();
            if (gomoku.isPlayerTurn() && last != null
                    && last.type == Point.BLACK && last.x >= 0 && last.x < 15 && last.y >= 0 && last.y < 15
                    && MaidGomokuAI.getStatue(gomoku.getChessData(), last) == Statue.WIN) return "gomoku";
        }
        else if (type == Type.CCHESS_WIN && blockEntity instanceof TileEntityCChess chess)
        {
            if (chess.isCheckmate() && CChessUtil.isMaid(chess.getChessData())
                    && chess.getChessData().isMate()) return "xiangqi";
        }
        else if (type == Type.WCHESS_WIN && blockEntity instanceof TileEntityWChess chess)
        {
            if (chess.isCheckmate() && WChessUtil.isMaid(chess.getChessData())
                    && chess.getChessData().isMate()) return "chess";
        }
        return null;
    }
}
