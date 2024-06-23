package example.example.example.forge;

import example.example.example.Example;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Example.MOD_ID)
public class ExampleForge {
    public ExampleForge() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Example.init();
        eventBus.register(this);
    }
}