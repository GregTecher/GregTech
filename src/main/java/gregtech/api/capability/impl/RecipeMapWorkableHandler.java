package gregtech.api.capability.impl;

import gregtech.api.GTValues;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.IWorkable;
import gregtech.api.metatileentity.MTETrait;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.util.GTUtility;
import gregtech.api.util.XSTR;
import gregtech.common.ConfigHolder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class RecipeMapWorkableHandler extends MTETrait implements IWorkable {

    public final RecipeMap<?> recipeMap;
    protected Recipe previousRecipe;

    protected List<ItemStack> lastFailedItem;
    protected List<FluidStack> lastFailedFluid;

    protected int progressTime;
    protected int maxProgressTime;
    protected int recipeEUt;
    protected List<FluidStack> fluidOutputs;
    protected NonNullList<ItemStack> itemOutputs;
    protected final Random random = new XSTR();

    private boolean isActive;
    private boolean workingEnabled = true;
    private boolean hasNotEnoughEnergy;
    private boolean wasActiveAndNeedsUpdate;

    public RecipeMapWorkableHandler(MetaTileEntity tileEntity, RecipeMap<?> recipeMap) {
        super(tileEntity);
        this.recipeMap = recipeMap;
    }

    protected abstract long getEnergyStored();

    protected abstract long getEnergyCapacity();

    protected abstract boolean drawEnergy(int recipeEUt);

    protected abstract long getMaxVoltage();

    protected IItemHandlerModifiable getInputInventory() {
        return metaTileEntity.getImportItems();
    }

    protected IItemHandlerModifiable getOutputInventory() {
        return metaTileEntity.getExportItems();
    }

    protected IMultipleTankHandler getInputTank() {
        return metaTileEntity.getImportFluids();
    }

    protected IMultipleTankHandler getOutputTank() {
        return metaTileEntity.getExportFluids();
    }

    @Override
    public String getName() {
        return "RecipeMapWorkable";
    }

    @Override
    public Capability<?> getImplementingCapability() {
        return GregtechCapabilities.CAPABILITY_WORKABLE;
    }

    @Override
    public void update() {
        if (getMetaTileEntity().getWorld().isRemote)
            return;
        if (progressTime > 0 && workingEnabled) {
            boolean drawEnergy = drawEnergy(recipeEUt);
            if (drawEnergy || (recipeEUt < 0 && ignoreTooMuchEnergy())) {
                if (++progressTime >= maxProgressTime) {
                    completeRecipe();
                }
            } else {
                if (recipeEUt > 0) {
                    //only set hasNotEnoughEnergy if this recipe is consuming recipe
                    //generators always have enough energy
                    this.hasNotEnoughEnergy = true;
                    //if current progress value is greater than 2, decrement it by 2
                    if (progressTime >= 2) {
                        if (ConfigHolder.insufficientEnergySupplyWipesRecipeProgress) {
                            this.progressTime = 1;
                        } else {
                            this.progressTime = Math.max(1, progressTime - 2);
                        }
                    }
                }
            }
        }

        boolean isHit = true;
        if (progressTime == 0 && workingEnabled) {
            long maxVoltage = getMaxVoltage();
            Recipe currentRecipe = null;
            IItemHandlerModifiable importInventory = getInputInventory();
            IMultipleTankHandler importFluids = getInputTank();
            if (previousRecipe != null && previousRecipe.matches(false, importInventory, importFluids)) {
                //if previous recipe still matches inputs, try to use it
                currentRecipe = previousRecipe;
            } else {
                // firstly, check if match the failed recipe
                if (!isLastFailedRecipeMatch(importInventory, importFluids)) {
                    isHit = false;
                    //else, try searching new recipe for given inputs
                    currentRecipe = findRecipe(maxVoltage, importInventory, importFluids);
                    //if we found recipe that can be buffered, buffer it
                    if (currentRecipe != null && currentRecipe.canBeBuffered()) {
                        this.previousRecipe = currentRecipe;
                    }
                }
            }
            if (currentRecipe != null && setupAndConsumeRecipeInputs(currentRecipe)) {
                setupRecipe(currentRecipe);
            }

            if (currentRecipe == null && !isHit) {
                List<ItemStack> itemStacks = GTUtility.itemHandlerToList(importInventory);
                lastFailedItem = new ArrayList<>(itemStacks.size());
                for (ItemStack item : itemStacks) {
                    lastFailedItem.add(item.copy());
                }
                List<FluidStack> fluidStacks = GTUtility.fluidHandlerToList(importFluids);
                lastFailedFluid = new ArrayList<>(fluidStacks.size());
                for (FluidStack fluid : fluidStacks) {
                    lastFailedFluid.add(fluid == null ? null : fluid.copy());
                }
            }
        }

        if (wasActiveAndNeedsUpdate) {
            this.wasActiveAndNeedsUpdate = false;
            setActive(false);
        }
    }

    protected Recipe findRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs) {
        return recipeMap.findRecipe(maxVoltage, inputs, fluidInputs);
    }

    protected boolean setupAndConsumeRecipeInputs(Recipe recipe) {
        int[] resultOverclock = calculateOverclock(recipe.getEUt(), getMaxVoltage(), recipeMap.getAmperage(), recipe.getDuration(), false);
        int totalEUt = resultOverclock[0] * resultOverclock[1];
        IItemHandlerModifiable importInventory = getInputInventory();
        IItemHandlerModifiable exportInventory = getOutputInventory();
        IMultipleTankHandler importFluids = getInputTank();
        IMultipleTankHandler exportFluids = getOutputTank();
        return (totalEUt >= 0 ? getEnergyStored() >= (totalEUt > getEnergyCapacity() / 2 ? resultOverclock[0] : totalEUt) :
            (ignoreTooMuchEnergy() || getEnergyStored() - resultOverclock[0] <= getEnergyCapacity())) &&
            (!recipe.needsEmptyOutput() || MetaTileEntity.isItemHandlerEmpty(exportInventory)) &&
            MetaTileEntity.addItemsToItemHandler(exportInventory, true, recipe.getOutputs()) &&
            MetaTileEntity.addFluidsToFluidHandler(exportFluids, true, recipe.getFluidOutputs()) &&
            recipe.matches(true, importInventory, importFluids);
    }

    protected boolean ignoreTooMuchEnergy() {
        return false;
    }

    protected int[] calculateOverclock(int EUt, long voltage, long amperage, int duration, boolean consumeInputs) {
        boolean negativeEU = EUt < 0;
        int tier = getOverclockingTier(voltage);
        if (GTValues.V[tier] <= EUt || tier == 0)
            return new int[]{EUt, duration};
        if (negativeEU)
            EUt = -EUt;
        if (EUt <= 16) {
            int multiplier = tier - 1;
            int resultEUt = EUt * (1 << multiplier) * (1 << multiplier);
            int resultDuration = duration / (1 << multiplier);
            return new int[]{negativeEU ? -resultEUt : resultEUt, resultDuration};
        } else {
            int resultEUt = EUt;
            double resultDuration = duration;
            double durationMultiplier = negativeEU ? 3.80 : 2.0;
            //do not overclock further if duration is already too small
            while (resultDuration >= durationMultiplier && resultEUt <= GTValues.V[tier - 1] * amperage) {
                resultEUt *= 4;
                resultDuration /= durationMultiplier;
            }
            return new int[]{negativeEU ? -resultEUt : resultEUt, (int) Math.floor(resultDuration)};
        }
    }

    protected int getOverclockingTier(long voltage) {
        return GTUtility.getTierByVoltage(voltage);
    }

    protected void setupRecipe(Recipe recipe) {
        int[] resultOverclock = calculateOverclock(recipe.getEUt(), getMaxVoltage(), recipeMap.getAmperage(), recipe.getDuration(), true);
        this.progressTime = 1;
        setMaxProgress(resultOverclock[1]);
        this.recipeEUt = resultOverclock[0];
        this.fluidOutputs = GTUtility.copyFluidList(recipe.getFluidOutputs());
        int byproductChanceMultiplier = 1;
        int tier = GTUtility.getTierByVoltage(getMaxVoltage());
        int recipeTier = GTUtility.getTierByVoltage(recipe.getEUt());
        if (tier > GTValues.LV && tier > recipeTier) {
            byproductChanceMultiplier = 1 << (tier - recipeTier);
        }
        this.itemOutputs = GTUtility.copyStackList(recipe.getResultItemOutputs(random, byproductChanceMultiplier));
        if (this.wasActiveAndNeedsUpdate) {
            this.wasActiveAndNeedsUpdate = false;
        } else {
            this.setActive(true);
        }
    }

    protected void completeRecipe() {
        MetaTileEntity.addItemsToItemHandler(getOutputInventory(), false, itemOutputs);
        MetaTileEntity.addFluidsToFluidHandler(getOutputTank(), false, fluidOutputs);
        this.progressTime = 0;
        setMaxProgress(0);
        this.recipeEUt = 0;
        this.fluidOutputs = null;
        this.itemOutputs = null;
        this.hasNotEnoughEnergy = false;
        this.wasActiveAndNeedsUpdate = true;
    }

    public double getProgressPercent() {
        return getMaxProgress() == 0 ? 0.0 : getProgress() / (getMaxProgress() * 1.0);
    }

    public int getTicksTimeLeft() {
        return maxProgressTime == 0 ? 0 : (maxProgressTime - progressTime);
    }

    @Override
    public int getProgress() {
        return progressTime;
    }

    @Override
    public int getMaxProgress() {
        return maxProgressTime;
    }

    public int getRecipeEUt() {
        return recipeEUt;
    }

    public void setMaxProgress(int maxProgress) {
        this.maxProgressTime = maxProgress;
        if (!metaTileEntity.getWorld().isRemote) {
            metaTileEntity.markDirty();
        }
    }

    @Override
    public void setActive(boolean active) {
        this.isActive = active;
        if (!metaTileEntity.getWorld().isRemote) {
            metaTileEntity.markDirty();
            writeCustomData(1, buf -> buf.writeBoolean(active));
        }
    }

    @Override
    public void increaseProgress(int progress) {
        if (!metaTileEntity.getWorld().isRemote) {
            this.progressTime = Math.min(progressTime + progress, maxProgressTime);
            metaTileEntity.markDirty();
        }
    }

    @Override
    public boolean hasWorkToDo() {
        return progressTime > 0;
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled) {
        this.workingEnabled = workingEnabled;
        if (!metaTileEntity.getWorld().isRemote) {
            metaTileEntity.markDirty();
        }
    }

    public boolean isHasNotEnoughEnergy() {
        return hasNotEnoughEnergy;
    }

    @Override
    public boolean isWorkingEnabled() {
        return workingEnabled;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        if (dataId == 1) {
            this.isActive = buf.readBoolean();
            getMetaTileEntity().getHolder().scheduleChunkForRenderUpdate();
        }
    }

    @Override
    public void writeInitialData(PacketBuffer buf) {
        buf.writeBoolean(this.isActive);
    }

    @Override
    public void receiveInitialData(PacketBuffer buf) {
        this.isActive = buf.readBoolean();
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setBoolean("WorkEnabled", this.workingEnabled);
        if (progressTime > 0) {
            compound.setInteger("Progress", progressTime);
            compound.setInteger("MaxProgress", maxProgressTime);
            compound.setInteger("RecipeEUt", this.recipeEUt);
            NBTTagList itemOutputsList = new NBTTagList();
            for (ItemStack itemOutput : itemOutputs) {
                itemOutputsList.appendTag(itemOutput.writeToNBT(new NBTTagCompound()));
            }
            NBTTagList fluidOutputsList = new NBTTagList();
            for (FluidStack fluidOutput : fluidOutputs) {
                fluidOutputsList.appendTag(fluidOutput.writeToNBT(new NBTTagCompound()));
            }
            compound.setTag("ItemOutputs", itemOutputsList);
            compound.setTag("FluidOutputs", fluidOutputsList);
        }
        return compound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        this.workingEnabled = compound.getBoolean("WorkEnabled");
        this.progressTime = compound.getInteger("Progress");
        this.isActive = false;
        if (progressTime > 0) {
            this.isActive = true;
            this.maxProgressTime = compound.getInteger("MaxProgress");
            this.recipeEUt = compound.getInteger("RecipeEUt");
            NBTTagList itemOutputsList = compound.getTagList("ItemOutputs", Constants.NBT.TAG_COMPOUND);
            this.itemOutputs = NonNullList.create();
            for (int i = 0; i < itemOutputsList.tagCount(); i++) {
                this.itemOutputs.add(new ItemStack(itemOutputsList.getCompoundTagAt(i)));
            }
            NBTTagList fluidOutputsList = compound.getTagList("FluidOutputs", Constants.NBT.TAG_COMPOUND);
            this.fluidOutputs = new ArrayList<>();
            for (int i = 0; i < fluidOutputsList.tagCount(); i++) {
                this.fluidOutputs.add(FluidStack.loadFluidStackFromNBT(fluidOutputsList.getCompoundTagAt(i)));
            }
        }
    }

    /**
     * check if match the last failed recipe
     *
     * @param importInventory
     * @param importFluids
     * @return
     */
    private boolean isLastFailedRecipeMatch(IItemHandlerModifiable importInventory, IMultipleTankHandler importFluids) {
        // Because it will be called frequently, I will not use lambda & stream
        // actually, the "items" object can be reuse
        if (lastFailedFluid == null || lastFailedItem == null) {
            return false;
        }
        List<ItemStack> items = GTUtility.itemHandlerToList(importInventory);
        List<FluidStack> fluids = GTUtility.fluidHandlerToList(importFluids);

        if (items.size() != lastFailedItem.size() || fluids.size() != lastFailedFluid.size()) {
            return false;
        }

        for (int i = 0; i < lastFailedItem.size(); i++) {
            ItemStack failedItem = lastFailedItem.get(i);
            ItemStack item = items.get(i);
            if ((item.getCount() != failedItem.getCount())
                || (!(ItemStack.areItemStacksEqual(item, failedItem) && ItemStack.areItemStackTagsEqual(item, failedItem)))) {
                return false;
            }
        }
        for (int i = 0; i < lastFailedFluid.size(); i++) {
            FluidStack failedFluid = lastFailedFluid.get(i);
            FluidStack fluid = fluids.get(i);
            if (fluid == null && failedFluid == null) {
                continue;
            }
            if (fluid == null || failedFluid == null) {
                return false;
            }
            if ((fluid.amount != failedFluid.amount)
                || (!fluid.isFluidEqual(failedFluid))) {
                return false;
            }
        }
        return true;
    }

}