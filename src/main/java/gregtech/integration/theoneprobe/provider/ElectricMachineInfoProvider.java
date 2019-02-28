package gregtech.integration.theoneprobe.provider;

import gregtech.api.capability.impl.RecipeMapWorkableHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.WorkableTieredMetaTileEntity;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import mcjty.theoneprobe.api.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ElectricMachineInfoProvider implements IProbeInfoProvider {

    @Override
    public String getID() {
        return "gregtech:electric_machine_provider";
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        if(blockState.getBlock().hasTileEntity(blockState)) {
            TileEntity tileEntity = world.getTileEntity(data.getPos());
            if (tileEntity instanceof MetaTileEntityHolder) {
                MetaTileEntity metaTileEntity = ((MetaTileEntityHolder) tileEntity).getMetaTileEntity();

                RecipeMapWorkableHandler handler = null;
                if (metaTileEntity instanceof RecipeMapMultiblockController) {
                    handler = ((RecipeMapMultiblockController) metaTileEntity).getRecipeMapWorkable();
                }

                if (metaTileEntity instanceof WorkableTieredMetaTileEntity) {
                    handler = ((WorkableTieredMetaTileEntity) metaTileEntity).getWorkable();
                }
                if (handler != null && handler.getRecipeEUt() > 0) {
                    probeInfo.horizontal().text(TextStyleClass.INFO.toString() + handler.getRecipeEUt() + " eu/t");
                }
            }
        }
    }
}
