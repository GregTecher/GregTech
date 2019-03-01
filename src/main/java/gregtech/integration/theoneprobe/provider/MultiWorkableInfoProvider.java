package gregtech.integration.theoneprobe.provider;

import gregtech.api.capability.impl.RecipeMapWorkableHandler;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.TextStyleClass;

public class MultiWorkableInfoProvider extends MetaTileEntityHolderInfoProvider<RecipeMapMultiblockController> {

    public MultiWorkableInfoProvider() {
        super(RecipeMapMultiblockController.class);
    }

    @Override
    public String getID() {
        return "gregtech:multi_workable_provider";
    }

    @Override
    protected void addProbeInfo(RecipeMapMultiblockController metaTileEntity, IProbeInfo probeInfo) {
        RecipeMapWorkableHandler handler = metaTileEntity.getRecipeMapWorkable();

        if (handler != null && handler.getRecipeEUt() > 0) {
            probeInfo.horizontal().text(TextStyleClass.INFO.toString() + handler.getRecipeEUt() + " eu/t");
        }
    }
}
