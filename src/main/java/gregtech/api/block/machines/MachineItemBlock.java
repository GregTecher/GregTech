package gregtech.api.block.machines;

import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.TieredMetaTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class MachineItemBlock extends ItemBlock {

    public MachineItemBlock(BlockMachine block) {
        super(block);
        setHasSubtypes(true);
    }

    public static MetaTileEntity getMetaTileEntity(ItemStack itemStack) {
        return GregTechAPI.META_TILE_ENTITY_REGISTRY.getObjectById(itemStack.getItemDamage());
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(stack);
        return metaTileEntity == null ? "unnamed" : metaTileEntity.getMetaName();
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(stack);
        //prevent rendering glitch before meta tile entity sync to client, but after block placement
        //set opaque property on the placing on block, instead during set of meta tile entity
        return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ,
            newState.withProperty(BlockMachine.OPAQUE, metaTileEntity != null && metaTileEntity.isOpaqueCube()));
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        MetaTileEntity metaTileEntity = getMetaTileEntity(stack);
        if(metaTileEntity == null) return;

        //item specific tooltip like: gregtech.machine.lathe.lv.tooltip
        String tooltipLocale = metaTileEntity.getMetaName() + ".tooltip";
        if (I18n.hasKey(tooltipLocale)) {
            String[] lines = I18n.format(tooltipLocale).split("/n");
            tooltip.addAll(Arrays.asList(lines));
        }

        //tier less tooltip for a electric machine like: gregtech.machine.lathe.tooltip
        if (metaTileEntity instanceof TieredMetaTileEntity) {
            String tierlessTooltipLocale = ((TieredMetaTileEntity) metaTileEntity).getTierlessTooltipKey();
            //only add tierless tooltip if it's key is not equal to normal tooltip key (i.e if machine name has dot in it's name)
            //case when it's not true would be any machine extending from TieredMetaTileEntity but having only one tier
            if (!tooltipLocale.equals(tierlessTooltipLocale) && I18n.hasKey(tierlessTooltipLocale)) {
                String[] lines = I18n.format(tierlessTooltipLocale).split("/n");
                tooltip.addAll(Arrays.asList(lines));
            }
        }
        metaTileEntity.addInformation(stack, worldIn, tooltip, flagIn.isAdvanced());

        if(flagIn.isAdvanced()) {
            tooltip.add(String.format("MetaTileEntity Id: %s", metaTileEntity.metaTileEntityId));
        }
    }
}
