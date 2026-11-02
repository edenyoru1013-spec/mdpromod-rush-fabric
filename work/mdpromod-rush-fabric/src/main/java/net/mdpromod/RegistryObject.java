package net.mdpromod;

import java.util.function.Supplier;

/** Small compatibility wrapper for the Forge RegistryObject access pattern. */
final class RegistryObject<T> implements Supplier<T> {
    private final T value;

    RegistryObject(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }
}
