package io.github.axiraa.cinematicchat.client;
import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class ConfigManager implements AutoCloseable {
    public static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private final Path directory; private final Consumer<String> warning;
    private final ExecutorService io=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Cinematic Chat config");t.setDaemon(true);return t;});
    public ConfigManager(Path directory,Consumer<String> warning){this.directory=directory;this.warning=warning;}
    public Path directory(){return directory;}
    public CinematicConfig load(){var c=read("cinematic_chat.json",CinematicConfig.class,CinematicConfig::new);c.validate();return c;}
    public CompletableFuture<CinematicConfig> loadAsync(){return CompletableFuture.supplyAsync(this::load,io);}
    public <T> CompletableFuture<T> readAsync(String name,Class<T> type,java.util.function.Supplier<T> defaults){return CompletableFuture.supplyAsync(()->read(name,type,defaults),io);}
    public <T> T read(String name,Class<T> type,java.util.function.Supplier<T> defaults) {
        Path file=directory.resolve(name);if(!Files.exists(file))return defaults.get();
        try { if(Files.size(file)>2*1024*1024)throw new IOException("Configuration too large");
            var data=JsonParser.parseString(Files.readString(file,StandardCharsets.UTF_8));if(!data.isJsonObject())throw new JsonParseException("Configuration must be an object");
            String key=type==CinematicConfig.class?"configVersion":"version";int latest=type==CinematicConfig.class?2:1;
            var version=data.getAsJsonObject().get(key);if(version!=null){double v=version.getAsDouble();if(!Double.isFinite(v)||v!=Math.rint(v)||v<1||v>latest)throw new JsonParseException("Unsupported schema version: "+version);}
            T result=JSON.fromJson(data,type);if(result==null)throw new JsonParseException("Empty config");return result; }
        catch(IOException|RuntimeException ex) { warning.accept("Cannot read "+name+": "+ex.getMessage());
            try { Files.move(file,file.resolveSibling(name+".corrupt-"+System.currentTimeMillis())); } catch(IOException backup){warning.accept("Cannot back up "+name+": "+backup.getMessage());}return defaults.get(); }
    }
    public <T> T copy(T value,Class<T> type){return JSON.fromJson(JSON.toJson(value),type);}
    public CompletableFuture<Void> save(String name,Object value) {
        String json=JSON.toJson(value);return CompletableFuture.runAsync(()->{
            try { Files.createDirectories(directory);Path file=directory.resolve(name),temp=Files.createTempFile(directory,"cchat-",".tmp");
                try { Files.writeString(temp,json,StandardCharsets.UTF_8);try {Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ex){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);} }
                finally {Files.deleteIfExists(temp);} }
            catch(IOException ex){warning.accept("Cannot save "+name+": "+ex.getMessage());throw new CompletionException(ex);}
        },io);
    }
    public void close(){io.shutdown();try{io.awaitTermination(2,TimeUnit.SECONDS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}}
}
