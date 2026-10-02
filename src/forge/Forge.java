package forge;

import arc.files.Fi;
import forge.util.io.BinReader;
import mindustry.Vars;
import mindustry.mod.Mods.LoadedMod;

public class Forge {
    public static final LoadedMod mod;

    public static final Fi dir;
    
    public static final String version;
    public static final String lastVersion;

    static {
        Fi mods = Vars.modDirectory;
        mod = Vars.mods.getMod(ForgeLoader.class);
        version = mod.meta.version;
        dir = mods.child("Forge/");
        lastVersion = new BinReader(fi("lastVersion.version")).readString();
    }

    public static Fi fi(String name){
        return dir.child(name);
    }
}
