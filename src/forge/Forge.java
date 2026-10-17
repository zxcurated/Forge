package forge;

import arc.files.Fi;
import forge.util.io.BinReader;
import forge.util.io.BinWriter;
import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.mod.Mods.LoadedMod;

public class Forge extends Mod {
    public static final String LAST_VERSION_FILE = "lastVersion.version";
    public static final String LAST_VERSION_ERROR = "error";
    
    public static final LoadedMod mod = Vars.mods.getMod(Forge.class);
    public static final Fi dir = Vars.dataDirectory.child("forge/");
    public static final String version = mod.meta.version;
    public static final String lastVersion = getLastVersion(); // last RUNNED version
    
    public static Fi fi(String name) {
        return dir.child(name);
    }

    private static String getLastVersion() {
        Fi file = fi(LAST_VERSION_FILE);
        String lastVersion = readLastVersion(file);
        if (!lastVersion.equals(version)) writeLastVersion(file);
        return lastVersion;
    }
    
    private static String readLastVersion(Fi file) {
        return file.exists()
            ? new BinReader(file).readString()
            : LAST_VERSION_ERROR;
    }
    
    private static void writeLastVersion(Fi file) {
        try (var writer = new BinWriter(file)) {
            writer.writeString(version);
        }
    }
}
