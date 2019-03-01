package gregtech.integration.theoneprobe.provider;

import gregtech.api.capability.impl.EnergyContainerBatteryBuffer;
import gregtech.common.metatileentities.electric.MetaTileEntityBatteryBuffer;
import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.TextStyleClass;

public class BatteryBufferInfoProvider extends MetaTileEntityHolderInfoProvider<MetaTileEntityBatteryBuffer> {

    public BatteryBufferInfoProvider() {
        super(MetaTileEntityBatteryBuffer.class);
    }

    @Override
    protected void addProbeInfo(MetaTileEntityBatteryBuffer metaTileEntity, IProbeInfo probeInfo) {
        EnergyContainerBatteryBuffer container = (EnergyContainerBatteryBuffer) metaTileEntity.getEnergyContainer();
        IProbeInfo horizontalPane = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER));
        horizontalPane.text(TextStyleClass.INFO + "{*gregtech.top.amperage*} ");
        horizontalPane.progress(container.getAmperageOut(), container.getAmperageMax(), probeInfo.defaultProgressStyle()
            .suffix(" / " + container.getAmperageMax() + " A")
            .borderColor(0x00000000)
            .backgroundColor(0x00000000)
            .filledColor(0xFFFFE000)
            .alternateFilledColor(0xFFEED000));
    }

    @Override
    public String getID() {
        return "gregtech:battery_buffer_provider";
    }
}
