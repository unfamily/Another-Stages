package net.unfamily.anotherstages.owner;

import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.unfamily.anotherstages.AnotherStages;

import java.util.function.Supplier;

/**
 * Persistent machine owner UUID (string form) on block entities.
 * Empty string means no owner claimed yet.
 */
public final class AsAttachmentTypes {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, AnotherStages.MOD_ID);

    public static final Supplier<AttachmentType<String>> MACHINE_OWNER = ATTACHMENT_TYPES.register(
            "machine_owner",
            () -> AttachmentType.builder(() -> "")
                    .serialize(Codec.STRING.fieldOf("uuid"), s -> s != null && !s.isEmpty())
                    .build());

    private AsAttachmentTypes() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
