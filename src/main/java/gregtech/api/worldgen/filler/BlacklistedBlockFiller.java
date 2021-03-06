package gregtech.api.worldgen.filler;

import com.google.gson.JsonObject;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.minecraft.CraftTweakerMC;
import gregtech.api.GTValues;
import gregtech.api.worldgen.config.OreConfigUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.common.Optional.Method;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@ZenClass("mods.gregtech.ore.filter.BlacklistedBlockFilter")
@ZenRegister
public class BlacklistedBlockFiller extends BlockFiller {

    private Function<IBlockState, IBlockState> blockStateFiller;
    private List<IBlockState> blacklist;

    public BlacklistedBlockFiller(List<IBlockState> blacklist) {
        this.blacklist = blacklist;
    }

    public BlacklistedBlockFiller(List<IBlockState> blacklist, Function<IBlockState, IBlockState> blockStateFiller) {
        this.blacklist = blacklist;
        this.blockStateFiller = blockStateFiller;
    }

    public List<IBlockState> getBlacklist() {
        return blacklist;
    }

    @ZenGetter("blacklist")
    @Method(modid = GTValues.MODID_CT)
    public List<crafttweaker.api.block.IBlockState> ctGetBlacklist() {
        return blacklist.stream()
            .map(CraftTweakerMC::getBlockState)
            .collect(Collectors.toList());
    }

    @Override
    public void loadFromConfig(JsonObject object) {
        this.blockStateFiller = OreConfigUtils.createBlockStateFiller(object.get("value"));
    }

    @Override
    public IBlockState getStateForGeneration(IBlockState currentState, int x, int y, int z) {
        for (IBlockState blockState : blacklist) {
            if (blockState == currentState) {
                return currentState;
            }
        }
        return blockStateFiller.apply(currentState);
    }
}
