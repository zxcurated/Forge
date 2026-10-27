package forge.util.io;

import arc.func.Cons;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@SuppressWarnings("unused")
public class AtomicFiles {
    
    private AtomicFiles() {} // can't be instanced
    
    @FunctionalInterface
    public interface ReturnCons<T, R> {
        R apply(T raf) throws IOException;
    }
    
    @FunctionalInterface
    public interface NoReturnCons<T> {
        void apply(T raf) throws IOException;
    }
    
    public static void write(File file, NoReturnCons<RandomAccessFile> readerConsumer) {
        write(file, readerConsumer, e -> { throw new RuntimeException(); });
    }
    
    public static void write(File file, NoReturnCons<RandomAccessFile> readerConsumer, Cons<IOException> exceptionConsumer) {
        File temp = new File(file + ".temp");
        try {
            prepare(temp);
            try (var raf = new RandomAccessFile(temp, "rw")) {
                readerConsumer.apply(raf);
            }
            move(temp, file);
        } catch (IOException e) {
            exceptionConsumer.get(e);
        }
    }
    
    public static void read(File file, NoReturnCons<RandomAccessFile> consumer) throws IOException {
        read(
            file,
            false,
            raf -> { consumer.apply(raf); return null; },
            null
        );
    }
    
    public static <R> R read(File file, ReturnCons<RandomAccessFile, R> consumer) throws IOException {
        return read(
            file,
            false,
            consumer,
            null
        );
    }
    
    public static void read(File file, boolean ignoreNotExist, NoReturnCons<RandomAccessFile> consumer) throws IOException {
        read(
            file,
            ignoreNotExist,
            raf -> { consumer.apply(raf); return null; },
            null
        );
    }
    
    public static void read(File file, NoReturnCons<RandomAccessFile> consumer, Cons<IOException> exceptionConsumer) throws IOException {
        read(
            file,
            false,
            raf -> { consumer.apply(raf); return null; },
            exceptionConsumer
        );
    }
    
    public static <R> R read(File file, boolean ignoreNotExist, ReturnCons<RandomAccessFile, R> consumer) throws IOException {
        return read(
            file,
            ignoreNotExist,
            consumer,
            null
        );
    }
    
    public static <R> R read(File file, ReturnCons<RandomAccessFile, R> consumer, Cons<IOException> exceptionConsumer) throws IOException {
        return read(
            file,
            false,
            consumer,
            exceptionConsumer
        );
    }
    
    public static void read(File file, boolean ignoreNotExist, NoReturnCons<RandomAccessFile> consumer, Cons<IOException> exceptionConsumer) throws IOException {
        read(
            file,
            ignoreNotExist,
            raf -> { consumer.apply(raf); return null; },
            exceptionConsumer
        );
    }
    
    public static <R> R read(File file, boolean ignoreNotExist, ReturnCons<RandomAccessFile, R> readerConsumer, Cons<IOException> exceptionConsumer) throws IOException {
        // if (not exists temp)
        if (Files.notExists(Path.of(file + ".temp"))) {
            // if (exists file)
            if (Files.exists(file.toPath())) {
                try (var raf = new RandomAccessFile(file, "r")) {
                    return readerConsumer.apply(raf);
                } catch (IOException e) {
                    throw error(e, exceptionConsumer);
                }
            } else if (!ignoreNotExist) {
                throw error(new FileNotFoundException(file.toString()), exceptionConsumer);
            } else return null;
        } else {
            throw error(new FileAlreadyExistsException("Existing template of file '" + file + "'"), exceptionConsumer);
        }
    }
    
    private static IOException error(IOException exception, Cons<IOException> exceptionConsumer) {
        if (exceptionConsumer != null) {
            exceptionConsumer.get(exception);
        }
        return exception;
    }
    
    private static void prepare(File file) throws IOException {
        if (!Files.deleteIfExists(file.toPath()))
            Files.createDirectories(file.getParentFile().toPath());
    }
    
    private static void move(File from, File into) throws IOException {
        Files.move(from.toPath(), into.toPath(), ATOMIC_MOVE, REPLACE_EXISTING);
    }
}
