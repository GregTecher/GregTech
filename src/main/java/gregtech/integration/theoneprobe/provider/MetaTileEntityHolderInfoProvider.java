package gregtech.integration.theoneprobe.provider;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public abstract class MetaTileEntityHolderInfoProvider<T> implements IProbeInfoProvider {

    private Class<T> klass;

    public MetaTileEntityHolderInfoProvider(Class<T> klass) {
        this.klass = klass;
    }

    protected abstract void addProbeInfo(T metaTileEntity, IProbeInfo probeInfo);

    protected boolean allowDisplaying(T capability) {
        return true;
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        if (blockState.getBlock().hasTileEntity(blockState)) {
            TileEntity tileEntity = world.getTileEntity(data.getPos());
            if (tileEntity instanceof MetaTileEntityHolder) {
                MetaTileEntity metaTileEntity = ((MetaTileEntityHolder) tileEntity).getMetaTileEntity();

                if (klass.isInstance(metaTileEntity)) {
                    addProbeInfo((T) metaTileEntity, probeInfo);
                }
            }
        }
    }

}
