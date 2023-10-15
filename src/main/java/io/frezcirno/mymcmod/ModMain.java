package io.frezcirno.mymcmod;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ModMain.MODID)
public class ModMain {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "mymcmod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> MOD_BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> MOD_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> MOD_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final CreativeModeTab MY_MOD_TAB = new CreativeModeTab("my_mod_tab") {
        @Override
        public @NotNull ItemStack makeIcon() {
            return new ItemStack(Blocks.BOOKSHELF);
        }
    };

    // 注册方块
    public static RegistryObject<Block> REDSTONE_CABLE_BLOCK = MOD_BLOCKS.register("redstone_cable", () -> new RedstoneCableBlock(BlockBehaviour.Properties.of(Material.STONE).instabreak()));
    public static RegistryObject<Item> REDSTONE_CABLE = MOD_ITEMS.register("redstone_cable", () -> new ItemNameBlockItem(REDSTONE_CABLE_BLOCK.get(), new Item.Properties().tab(MY_MOD_TAB)));
    public static final RegistryObject<Item> SUPER_SNOWBALL = MOD_ITEMS.register("super_snowball", () -> new SuperSnowballItem(new Item.Properties().stacksTo(6).tab(MY_MOD_TAB), 1));
    public static final RegistryObject<EntityType<SuperSnowballEntity>> SUPER_SNOWBALL_ENTITY_TYPE = MOD_ENTITY_TYPES.register(
            "super_snowball",
            () -> EntityType
                    .Builder
                    .<SuperSnowballEntity>of(SuperSnowballEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("super_snowball")
    );


    public ModMain() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register new blocks and items
        MOD_BLOCKS.register(modBus);
        MOD_ITEMS.register(modBus);
        MOD_ENTITY_TYPES.register(modBus);

        // Register other auto methods of this object
        modBus.register(this);
    }

    @SubscribeEvent
    public void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
    }

    @SubscribeEvent
    public void onRegister(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS,
                helper -> {
//                    REDSTONE_CABLE_BLOCK = new RedstoneCableBlock(BlockBehaviour.Properties.of(Material.STONE).instabreak());
//                    helper.register("redstone_cable", REDSTONE_CABLE_BLOCK);
                }
        );
        event.register(ForgeRegistries.Keys.ITEMS,
                helper -> {
//                    helper.register("redstone_cable", new ItemNameBlockItem(REDSTONE_CABLE_BLOCK, new Item.Properties().tab(MY_MOD_TAB)));
//                    helper.register("super_snowball", new SuperSnowballItem(new Item.Properties().stacksTo(6).tab(MY_MOD_TAB), 1));
                }
        );
        event.register(ForgeRegistries.Keys.ENTITY_TYPES,
                helper -> {
//                    SUPER_SNOWBALL_ENTITY_TYPE = EntityType
//                            .Builder
//                            .<SuperSnowballEntity>of(SuperSnowballEntity::new, MobCategory.MISC)
//                            .sized(0.5F, 0.5F)
//                            .clientTrackingRange(4)
//                            .updateInterval(10)
//                            .build("super_snowball");
//                    helper.register("super_snowball", SUPER_SNOWBALL_ENTITY_TYPE);
                }
        );
    }

    @SubscribeEvent
    public void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SUPER_SNOWBALL_ENTITY_TYPE.get(), ThrownItemRenderer::new);
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
