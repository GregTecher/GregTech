package gregtech.integration.theoneprobe.provider;

import gregtech.api.capability.impl.RecipeMapWorkableHandler;
import gregtech.api.metatileentity.WorkableTieredMetaTileEntity;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.TextStyleClass;

public class TieredWorkableInfoProvider extends MetaTileEntityHolderInfoProvider<WorkableTieredMetaTileEntity> {

    public TieredWorkableInfoProvider() {
        super(WorkableTieredMetaTileEntity.class);
    }

    @Override
    public String getID() {
        return "gregtech:tiered_workable_provider";
    }

    @Override
    protected void addProbeInfo(WorkableTieredMetaTileEntity metaTileEntity, IProbeInfo probeInfo) {
        RecipeMapWorkableHandler handler = metaTileEntity.getWorkable();

        if (handler != null && handler.getRecipeEUt() > 0) {
            probeInfo.horizontal().text(TextStyleClass.INFO.toString() + handler.getRecipeEUt() + " eu/t");
        }
    }
}
