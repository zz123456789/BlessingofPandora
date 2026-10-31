package com.blessingofpandora;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICurio;

/** A missing player slot needs a zero base, without changing any global or existing slot definition. */
public final class BlessingSlotRegistration
{
    private static final ResourceLocation TAG_VALIDATOR = ResourceLocation.parse("curios:tag");
    private static final ResourceLocation DEFAULT_CHARM_ICON = ResourceLocation.parse("curios:slot/empty_charm_slot");
    // These signatures were added after the supported 5.4.5 compile API. Bind them only once;
    // exact calls below avoid reflective invocation, argument arrays and primitive boxing.
    private static final MethodHandle SIDED_SLOT = sidedSlotHandle();
    private static final MethodHandle VALIDATORS = validatorsHandle();
    private static final MethodHandle CLIENT_MANAGER = clientManagerHandle();

    private BlessingSlotRegistration() {}

    /** The caller caches the result until Curios replaces its resource-derived source map. */
    public static Map<String, ISlotType> withMissingPlayerCharm(
            EntityType<?> entity, Map<String, ISlotType> slots, Object manager)
    {
        if (entity != EntityType.PLAYER || slots.containsKey("charm")) return slots;
        Map<String, ISlotType> result = new HashMap<>(slots);
        result.put("charm", new ZeroBaseCharm(globalCharm(manager)));
        return Map.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static ISlotType globalCharm(Object manager)
    {
        try
        {
            if (SIDED_SLOT != null)
            {
                boolean client = CLIENT_MANAGER != null && manager != null
                        && (Object) CLIENT_MANAGER.invokeExact() == manager;
                return ((Optional<ISlotType>) SIDED_SLOT.invokeExact(client)).orElse(null);
            }
            return CuriosApi.getSlot("charm").orElse(null);
        }
        catch (Throwable exception)
        {
            throw new IllegalStateException("Cannot read the Curios charm template", exception);
        }
    }

    private static MethodHandle sidedSlotHandle()
    {
        try
        {
            MethodHandle method = MethodHandles.publicLookup().findStatic(CuriosApi.class, "getSlot",
                    MethodType.methodType(Optional.class, String.class, boolean.class));
            return MethodHandles.insertArguments(method, 0, "charm");
        }
        catch (NoSuchMethodException ignored) { return null; }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Cannot bind the Curios slot API", exception); }
    }

    private static MethodHandle validatorsHandle()
    {
        try
        {
            return MethodHandles.publicLookup().findVirtual(ISlotType.class, "getValidators",
                    MethodType.methodType(Set.class));
        }
        catch (NoSuchMethodException ignored) { return null; }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Cannot bind the Curios validator API", exception); }
    }

    private static MethodHandle clientManagerHandle()
    {
        try
        {
            Class<?> type = Class.forName("top.theillusivec4.curios.common.data.CuriosEntityManager");
            return MethodHandles.publicLookup().findStaticGetter(type, "CLIENT", type)
                    .asType(MethodType.methodType(Object.class));
        }
        catch (ClassNotFoundException | NoSuchFieldException ignored) { return null; }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Cannot bind the Curios client manager", exception); }
    }

    private static final class ZeroBaseCharm implements ISlotType
    {
        private final ISlotType template;
        private final Set<ResourceLocation> validators;

        @SuppressWarnings("unchecked")
        private ZeroBaseCharm(ISlotType template)
        {
            this.template = template;
            try
            {
                this.validators = template != null && VALIDATORS != null
                        ? Set.copyOf((Set<ResourceLocation>) VALIDATORS.invokeExact(template)) : Set.of(TAG_VALIDATOR);
            }
            catch (Throwable exception)
            {
                throw new IllegalStateException("Cannot read the Curios charm validators", exception);
            }
        }

        @Override public String getIdentifier() { return "charm"; }
        @Override public int getSize() { return 0; }
        @Override public ResourceLocation getIcon()
        {
            return template == null ? DEFAULT_CHARM_ICON : template.getIcon();
        }
        @Override public int getOrder() { return template == null ? 200 : template.getOrder(); }
        @Override public boolean useNativeGui() { return template == null || template.useNativeGui(); }
        @Override public boolean hasCosmetic() { return template != null && template.hasCosmetic(); }
        @Override public boolean canToggleRendering() { return template == null || template.canToggleRendering(); }
        @Override public ICurio.DropRule getDropRule()
        {
            return template == null ? ICurio.DropRule.DEFAULT : template.getDropRule();
        }
        @Override public int getPriority() { return template == null ? 0 : template.getPriority(); }
        @Override public boolean isVisible() { return template == null || template.isVisible(); }
        // No @Override: 5.4.5 lacks this method; newer Curios uses it for actual item validation.
        public Set<ResourceLocation> getValidators() { return validators; }
        @Override public int compareTo(ISlotType other)
        {
            int order = Integer.compare(getOrder(), other.getOrder());
            return order == 0 ? getIdentifier().compareTo(other.getIdentifier()) : order;
        }
    }
}
