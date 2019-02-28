package gregtech.api.util;

import net.minecraft.util.NonNullList;

import java.util.List;

public class NonNullListReloaded<E> extends NonNullList<E> {

    public NonNullListReloaded(List<E> elements, E fill) {
        super(elements, fill);
    }
}
