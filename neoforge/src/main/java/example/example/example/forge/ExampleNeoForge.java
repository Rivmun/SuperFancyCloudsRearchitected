package example.example.example.forge;

import example.example.example.Example;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@SuppressWarnings("unused")
@Mod(Example.MOD_ID)
public class ExampleNeoForge {
    public ExampleNeoForge(IEventBus eventBus) {
        Example.init();
    }
}