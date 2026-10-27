package forge;

import arc.files.Fi;
import arc.util.Log;
import forge.util.io.AtomicFiles;
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
    
    public static final String version = mod.meta.version;
    public static final String lastVersion = getLastVersion(); // last RUNNED version
    
    public static final boolean updated = !version.equals(lastVersion);
    
    static {
        if (updated) writeVersion();
    }
    
    public static Fi fi(String name) {
        return dir.child(name);
    }
    
    public static File file(String name) {
        return fi(name).file();
    }

    private static String getLastVersion() {
        try {
            return readLastVersion();
        } catch (IOException e) {
            return LAST_VERSION_ERROR;
        }
    }
    
    private static String readLastVersion() throws IOException {
        return AtomicFiles.read(file(LAST_VERSION_FILE), RandomAccessFile::readUTF, Log::err);
    }
    
    private static void writeVersion() {
        AtomicFiles.write(file(LAST_VERSION_FILE), raf -> raf.writeUTF(version));
    }
}
