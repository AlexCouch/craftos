package programs;

import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;

public class TestProgram extends Program{
    @NotNull
    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isCLI() {
        return false;
    }

    @Override
    public void init() {

    }

    @NotNull
    @Override
    public ProgramFunction getFunction() {
        return os -> false;
    }

    @Override
    public void deserialize(@NotNull NBTTagCompound nbt) {

    }

    @NotNull
    @Override
    public NBTTagCompound serialize() {
        return new NBTTagCompound();
    }
}
