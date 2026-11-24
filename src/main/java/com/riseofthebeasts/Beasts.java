package com.riseofthebeasts;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.common.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

@Mod(Beasts.ID)
public class Beasts {
    public static final String ID = "riseofthebeasts";
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, ID);
    static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ID);
    static final RegistryObject<EntityType<Windegow>> WIND = ENTITIES.register("windegow", () ->
        EntityType.Builder.of(Windegow::new, MobCategory.MONSTER).sized(1.3f, 4.4f).clientTrackingRange(8).build("windegow"));
    static final RegistryObject<EntityType<Giant>> KRAKEN = giant("kraken");
    static final RegistryObject<EntityType<Giant>> LEVI = giant("leviathan");
    static final RegistryObject<EntityType<Giant>> SERPENT = giant("jormungandr");
    static final RegistryObject<EntityType<BeastShot>> SHOT = ENTITIES.register("beast_shot", () ->
        EntityType.Builder.<BeastShot>of(BeastShot::new, MobCategory.MISC).sized(.45f, .45f).clientTrackingRange(8).updateInterval(1).build("beast_shot"));
    static final RegistryObject<Cavern> CAVERN = FEATURES.register("abyss", () -> new Cavern(NoneFeatureConfiguration.CODEC));
    static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,ID);
    static final RegistryObject<net.minecraft.sounds.SoundEvent> SCREECH=SOUNDS.register("leviathan_screech",
        ()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id("leviathan_screech")));
    static final RegistryObject<MobEffect> STUN=EFFECTS.register("stunned",ScreechStun::new);
    static final RegistryObject<MobEffect> VENOM = EFFECTS.register("serpent_venom", Venom::new);
    static final RegistryObject<Item> SOUNDING = ITEMS.register("sounding_shell", () -> new SoundingShell(new Item.Properties().stacksTo(1)));
    static final RegistryObject<Item> KRAKEN_EGG = egg("kraken", KRAKEN, 0x913e49, 0xf2bd80);
    static final RegistryObject<Item> WIND_EGG = egg("windegow", WIND, 0x563627, 0xd5cab0);
    static final RegistryObject<Item> LEVI_EGG = egg("leviathan", LEVI, 0x182e58, 0xf08b36);
    static final RegistryObject<Item> SERPENT_EGG = egg("jormungandr", SERPENT, 0x403948, 0xd76626);
    private static RegistryObject<EntityType<Giant>> giant(String id) {
        return ENTITIES.register(id, () -> EntityType.Builder.<Giant>of(Giant::new, MobCategory.CREATURE)
            .sized(9, 7).clientTrackingRange(16).updateInterval(2).build(id));
    }
    private static RegistryObject<Item> egg(String id, RegistryObject<? extends EntityType<? extends Mob>> type, int base, int spots) {
        return ITEMS.register(id+"_spawn_egg", () -> new ForgeSpawnEggItem(type, base, spots, new Item.Properties()));
    }
    static ResourceLocation id(String name) { return new ResourceLocation(ID, name); }
    public Beasts() {
        KrakenView.register();
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus); ITEMS.register(bus); FEATURES.register(bus); EFFECTS.register(bus); SOUNDS.register(bus);
        bus.addListener(this::attributes); bus.addListener(this::tabs); bus.addListener(this::placements);
        MinecraftForge.EVENT_BUS.register(Encounters.class);
        MinecraftForge.EVENT_BUS.register(Stomach.class); MinecraftForge.EVENT_BUS.register(ScreechStun.class);
    }
    private void attributes(EntityAttributeCreationEvent event) {
        event.put(WIND.get(), Windegow.attributes().build());
        for (var type : java.util.List.of(LEVI, SERPENT, KRAKEN)) event.put(type.get(), Mob.createMobAttributes().build());
    }
    private void placements(SpawnPlacementRegisterEvent event) {
        event.register(WIND.get(), SpawnPlacements.Type.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Windegow::canSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
    private void tabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(WIND_EGG); event.accept(LEVI_EGG); event.accept(SERPENT_EGG); event.accept(KRAKEN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(SOUNDING);
    }
}
