package forge;

import arc.files.Fi;
import forge.util.io.BinReader;
import forge.util.io.BinWriter;
import mindustry.Vars;
import mindustry.mod.Mods.LoadedMod;

public class Forge {
    public static final LoadedMod mod;
    
    public static final Fi dir;
    
    public static final String version;
    public static final String lastVersion;
    
    static {
        mod = Vars.mods.getMod(ForgeLoader.class);
        version = mod.meta.version;
        dir = Vars.dataDirectory.child("forge/");
        
        Fi lastVersionFile = fi("lastVersion.version");
        if (lastVersionFile.exists()) {
            lastVersion = new BinReader(lastVersionFile).readString();
            if (!lastVersion.equals(version)) {
                var writer = new BinWriter(lastVersionFile);
                writer.writeString(version);
                writer.close();
            }
        } else {
            lastVersion = "error";
            var writer = new BinWriter(lastVersionFile);
            writer.writeString(version);
            writer.close();
        }
    }
    
    public static Fi fi(String name) {
        return dir.child(name);
    }
}
