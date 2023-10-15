package io.frezcirno.mymcmod;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class RedstoneCableBlock extends PipeBlock {
    // 属性：
    // 每个方向是否连接其他的导线
    // 信号强度
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    // 体积
    // 8x8x8 block in center
    protected static final VoxelShape CORE_AABB = Block.box(4.0D, 4.0D, 4.0D, 12.0D, 12.0D, 12.0D);
    // 6x6x4 block in north face
    protected static final VoxelShape NORTH_SIDE_AABB = Block.box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 4.0D);
    protected static final VoxelShape SOUTH_SIDE_AABB = NORTH_SIDE_AABB.move(0.0D, 0.0D, 12.0D);
    protected static final VoxelShape WEST_SIDE_AABB = Block.box(0.0D, 5.0D, 5.0D, 4.0D, 11.0D, 11.0D);
    protected static final VoxelShape EAST_SIDE_AABB = WEST_SIDE_AABB.move(12.0D, 0.0D, 0.0D);
    protected static final VoxelShape UP_SIDE_AABB = Block.box(5.0D, 12.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    protected static final VoxelShape DOWN_SIDE_AABB = UP_SIDE_AABB.move(0.0D, -12.0D, 0.0D);

    // shape cache
    protected static final VoxelShape[] SHAPES = makeShapeCache();

    private boolean shouldSignal = true;

    private static VoxelShape[] makeShapeCache() {
        VoxelShape[] shapes = new VoxelShape[64];
        for (int i = 0; i < 64; i++) {
            shapes[i] = makeShape(i);
        }
        return shapes;
    }

    private static final Vec3[] COLORS = Util.make(new Vec3[16], (vec3s) -> {
        for(int i = 0; i <= 15; ++i) {
            float f = (float)i / 15.0F;
            float f1 = f * 0.6F + (f > 0.0F ? 0.4F : 0.3F);
            float f2 = Mth.clamp(f * f * 0.7F - 0.5F, 0.0F, 1.0F);
            float f3 = Mth.clamp(f * f * 0.6F - 0.7F, 0.0F, 1.0F);
            vec3s[i] = new Vec3((double)f1, (double)f2, (double)f3);
        }
    });
    private static final float PARTICLE_DENSITY = 0.2F;

    public RedstoneCableBlock(Properties properties) {
        super(0.5F, properties);
        this.registerDefaultState(this.stateDefinition
                .any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(EAST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(POWER, 0)
        );
    }

    private static VoxelShape makeShape(int sideEnc) {
        VoxelShape shape = Shapes.empty();

        if ((sideEnc & 1) != 0) {
            shape = Shapes.or(shape, NORTH_SIDE_AABB);
        }
        if ((sideEnc & 2) != 0) {
            shape = Shapes.or(shape, SOUTH_SIDE_AABB);
        }
        if ((sideEnc & 4) != 0) {
            shape = Shapes.or(shape, WEST_SIDE_AABB);
        }
        if ((sideEnc & 8) != 0) {
            shape = Shapes.or(shape, EAST_SIDE_AABB);
        }
        if ((sideEnc & 16) != 0) {
            shape = Shapes.or(shape, UP_SIDE_AABB);
        }
        if ((sideEnc & 32) != 0) {
            shape = Shapes.or(shape, DOWN_SIDE_AABB);
        }

        return Shapes.or(shape, CORE_AABB);
    }

    private static VoxelShape getCachedShape(boolean north, boolean south, boolean west, boolean east, boolean up, boolean down) {
        int sideEnc = 0;
        if (north) { sideEnc |= 1; }
        if (south) { sideEnc |= 2; }
        if (west) { sideEnc |= 4; }
        if (east) { sideEnc |= 8; }
        if (up) { sideEnc |= 16; }
        if (down) { sideEnc |= 32; }
        return SHAPES[sideEnc];
    }

    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return getCachedShape(
                state.getValue(NORTH),
                state.getValue(SOUTH),
                state.getValue(WEST),
                state.getValue(EAST),
                state.getValue(UP),
                state.getValue(DOWN)
        );
    }

    public int getDirectSignal(BlockState state, BlockGetter getter, BlockPos pos, Direction inDir) {
        return !this.shouldSignal ? 0 : state.getSignal(getter, pos, inDir);
    }

    // request signal strength from neighbor
    public int getSignal(BlockState state, BlockGetter getter, BlockPos pos, Direction inDir) {
        if (!this.shouldSignal) {
            return 0;
        }

        int i = state.getValue(POWER);
        if (i == 0) {
            return 0;
        }

        return !this.updateConnectingState(getter, state, pos).getValue(PROPERTY_BY_DIRECTION.get(inDir.getOpposite())) ? 0 : i;
    }

    public BlockState getStateForPlacement(BlockPlaceContext placeContext) {
        return this.updateConnectingState(placeContext.getLevel(), this.defaultBlockState(), placeContext.getClickedPos());
    }

    public BlockState updateConnectingState(BlockGetter getter, BlockState state, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (!state.getValue(PROPERTY_BY_DIRECTION.get(direction))) {
                boolean connState = this.getConnectingSide(getter, pos, direction);
                state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connState);
            }
        }

        return state;
    }

    private boolean getConnectingSide(BlockGetter getter, BlockPos pos, Direction neibDir) {
        BlockPos neibPos = pos.relative(neibDir);
        BlockState neibState = getter.getBlockState(neibPos);

        return RedstoneCableBlock.shouldConnectTo(neibState, neibDir);
    }

    // a neighbor block changed
    public BlockState updateShape(BlockState state, Direction neibDir, BlockState neibState, LevelAccessor accessor, BlockPos pos, BlockPos neibPos) {
        boolean flag = RedstoneCableBlock.shouldConnectTo(neibState, neibDir);
        return state.setValue(PROPERTY_BY_DIRECTION.get(neibDir), flag);
    }

    protected static boolean shouldConnectTo(BlockState state, @Nullable Direction dir) {
        if (state.is(ModMain.REDSTONE_CABLE_BLOCK.get())) {
            return true;
        } else if (state.is(Blocks.REDSTONE_WIRE)) {
            return true;
        } else if (state.is(Blocks.REPEATER)) {
            Direction direction = state.getValue(RepeaterBlock.FACING);
            return direction == dir || direction.getOpposite() == dir;
        } else if (state.is(Blocks.OBSERVER)) {
            return dir == state.getValue(ObserverBlock.FACING);
        } else {
            return state.isSignalSource() && dir != null;
        }
    }

    // add property to block state
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateBuilder) {
        stateBuilder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, POWER);
    }

    // cannot walk
//    public boolean isPathfindable(BlockState p_51719_, BlockGetter p_51720_, BlockPos p_51721_, PathComputationType p_51722_) {
//        return false;
//    }

//    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource source) {
//        int i = state.getValue(POWER);
//        if (i != 0) {
//            for(Direction direction : Direction.Plane.HORIZONTAL) {
//                RedstoneSide redstoneside = state.getValue(PROPERTY_BY_DIRECTION.get(direction));
//                switch (redstoneside) {
//                    case UP:
//                        this.spawnParticlesAlongLine(level, source, pos, COLORS[i], direction, Direction.UP, -0.5F, 0.5F);
//                    case SIDE:
//                        this.spawnParticlesAlongLine(level, source, pos, COLORS[i], Direction.DOWN, direction, 0.0F, 0.5F);
//                        break;
//                    case NONE:
//                    default:
//                        this.spawnParticlesAlongLine(level, source, pos, COLORS[i], Direction.DOWN, direction, 0.0F, 0.3F);
//                }
//            }
//
//        }
//    }

    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState neibState, boolean b) {
        // is not the same block
        if (!neibState.is(state.getBlock()) && !level.isClientSide) {
            // update power strength immediately
            this.updatePowerStrength(level, pos, state);

            for(Direction direction : Direction.values()) {
                level.updateNeighborsAt(pos.relative(direction), this);
            }

            this.updateNeighborsOfNeighboringWires(level, pos);
        }
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState state1, boolean b) {
        if (!b && !state.is(state1.getBlock())) {
            // remove self
            super.onRemove(state, level, pos, state1, b);

            if (!level.isClientSide) {
                // update neighbors
                for(Direction direction : Direction.values()) {
                    level.updateNeighborsAt(pos.relative(direction), this);
                }


                this.updatePowerStrength(level, pos, state);
                this.updateNeighborsOfNeighboringWires(level, pos);
            }
        }
    }

    private void updateNeighborsOfNeighboringWires(Level level, BlockPos pos) {
        // check 4 horizontal directions
        for(Direction direction : Direction.Plane.HORIZONTAL) {
            this.checkCornerChangeAt(level, pos.relative(direction));
        }

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pos.relative(direction);
            if (level.getBlockState(blockpos).isRedstoneConductor(level, blockpos)) {
                this.checkCornerChangeAt(level, blockpos.above());
            } else {
                this.checkCornerChangeAt(level, blockpos.below());
            }
        }

    }

    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos pos1, boolean b) {
        if (!level.isClientSide) {
            this.updatePowerStrength(level, pos, state);
        }
    }

    // update self power strength, and notify neighbors if changed
    private void updatePowerStrength(Level level, BlockPos pos, BlockState state) {
        int strength = this.calculateTargetStrength(level, pos);

        // power strength changed
        if (state.getValue(POWER) != strength) {
            if (level.getBlockState(pos) == state) {
                level.setBlock(pos, state.setValue(POWER, strength), 2);
            }

            for (Direction direction : Direction.values()) {
                level.updateNeighborsAt(pos.relative(direction), this);
            }
        }
    }

    // calculate the power strength of the position
    private int calculateTargetStrength(Level level, BlockPos pos) {
        this.shouldSignal = false;
        // get the best(largest) neighbor signal strength
        int bestNeighborSignal = level.getBestNeighborSignal(pos);
        this.shouldSignal = true;

        // get the largest signal strength of the neighbor red stone cables
        int bestNeighborCableSignal = 0;
        if (bestNeighborSignal < 15) {
            for(Direction direction : Direction.values()) {
                BlockPos neibPos = pos.relative(direction);
                BlockState neibState = level.getBlockState(neibPos);
                bestNeighborCableSignal = Math.max(bestNeighborCableSignal, getWireSignal(neibState));
//
//                BlockPos abovePos = pos.above();
//                if (neibState.isRedstoneConductor(level, neibPos) && !level.getBlockState(abovePos).isRedstoneConductor(level, abovePos)) {
//                    bestNeighborCableSignal = Math.max(bestNeighborCableSignal, getWireSignal(level.getBlockState(neibPos.above())));
//                } else if (!neibState.isRedstoneConductor(level, neibPos)) {
//                    bestNeighborCableSignal = Math.max(bestNeighborCableSignal, getWireSignal(level.getBlockState(neibPos.below())));
//                }
            }
        }

        return Math.max(bestNeighborSignal, bestNeighborCableSignal - 1);
    }

    private static int getWireSignal(BlockState state) {
        return state.is(ModMain.REDSTONE_CABLE_BLOCK.get()) ? state.getValue(POWER) : 0;
    }

    // update power strength when neighbor block changed
    private void checkCornerChangeAt(Level level, BlockPos pos) {
        // the pos is a redstone cable block
        if (level.getBlockState(pos).is(this)) {
            // update its power strength
            level.updateNeighborsAt(pos, this);

            // update its neighbors
            for(Direction direction : Direction.values()) {
                level.updateNeighborsAt(pos.relative(direction), this);
            }
        }
    }
}
