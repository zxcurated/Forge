package forge;

import arc.files.Fi;
import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.mod.Mods.LoadedMod;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Forge extends Mod {
    public static final String LAST_VERSION_FILE = "lastVersion.version";
    public static final String LAST_VERSION_ERROR = "error";
    
    public static final LoadedMod mod = Vars.mods.getMod("forge");
    public static final Fi dir = Vars.dataDirectory.child("forge/");
    public static final String dirPath = dir.absolutePath();
    public static final String version = mod.meta.version;
    public static final String lastVersion = getLastVersion(); // last RUNNED version
    
    public static Fi fi(String name) {
        return dir.child(name);
    }
    
    public static File file(String name) {
        return new File(dirPath + name);
    }

    private static String getLastVersion() {
        File file = file(LAST_VERSION_FILE);
        String lastVersion = readLastVersion(file);
        if (!lastVersion.equals(version)) writeLastVersion(file);
        return lastVersion;
    }
    
    private static String readLastVersion(File file) {
        try (var raf = new RandomAccessFile(file, "r")){
            return raf.readUTF();
        } catch (IOException e) {
            return LAST_VERSION_ERROR;
        }
    }
    
    private static void writeLastVersion(File file) {
        try (var writer = new RandomAccessFile(file, "rw")) {
            writer.writeUTF(version);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
