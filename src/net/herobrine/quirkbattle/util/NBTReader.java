package net.herobrine.quirkbattle.util;

import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NBTReader {
    private ItemStack stack;
    private net.minecraft.server.v1_8_R3.ItemStack netStack;
    private NBTTagCompound nbtTagCompound;

    public NBTReader(ItemStack stack) {
        this.stack = stack;
        this.netStack = CraftItemStack.asNMSCopy(stack);
        this.nbtTagCompound = netStack.getTag() != null ? netStack.getTag() : new NBTTagCompound();
    }


    public void assign(Supplier<ItemStack> itemStackSupplier) {
        if (itemStackSupplier.get() == null) return;
        this.stack = itemStackSupplier.get();
        this.netStack = CraftItemStack.asNMSCopy(stack);
        this.nbtTagCompound = netStack.getTag() != null ? netStack.getTag() : new NBTTagCompound();
    }

    public void updateTag() {
        this.netStack.setTag(nbtTagCompound);
    }

    public Optional<String> getStringNBT(String id) {
        return getOptionalNBT(id, nbtTagCompound::getString);
    }

    public Optional<Integer> getIntNBT(String id) {
        return getOptionalNBT(id, nbtTagCompound::getInt);
    }

    public Optional<Boolean> getBooleanNBT(String id) {
        return getOptionalNBT(id, nbtTagCompound::getBoolean);
    }

    public Optional<Double> getDoubleNBT(String id) {
        return getOptionalNBT(id, nbtTagCompound::getDouble);
    }

    public Optional<NBTTagList> getListNBT(String id, int index) {
        return Optional.of(nbtTagCompound.getList(id, index));
    }

    public void writeStringNBT(String id, Supplier<String> value) {
        writeNBT(id, value, nbtTagCompound::setString);
    }

    public void writeIntNBT(String id, Supplier<Integer> value) {
        writeNBT(id, value, nbtTagCompound::setInt);
    }

    public void writeBooleanNBT(String id, Supplier<Boolean> value) {
        writeNBT(id, value, nbtTagCompound::setBoolean);
    }

    public void writeDoubleNBT(String id, Supplier<Double> value) {
        writeNBT(id, value, nbtTagCompound::setDouble);
    }

    public void writeListNBT(String id, Supplier<NBTTagList> supplier) {
        writeNBT(id, supplier, nbtTagCompound::set);
    }

    private <T> Optional<T> getOptionalNBT(String id, Function<String, T> getter) {
        return Optional.of(getter.apply(id));
    }

    private <T> void writeNBT(String id, Supplier<T> value, BiConsumer<String, T> setter) {
        setter.accept(id, value.get());
    }

    public ItemStack toBukkit() {
        return CraftItemStack.asBukkitCopy(netStack);
    }

}
