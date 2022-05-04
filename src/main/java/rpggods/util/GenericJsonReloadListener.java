package rpggods.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import rpggods.RPGGods;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;

public class GenericJsonReloadListener<T> extends SimpleJsonResourceReloadListener {

    private final Codec<T> codec;
    private final Consumer<GenericJsonReloadListener<T>> syncOnReload;
    private final Class<T> objClass;

    protected Map<ResourceLocation, Optional<T>> OBJECTS = new HashMap<>();

    public GenericJsonReloadListener(final String folderIn, final Class<T> oClass, final Codec<T> oCodec,
                                     Consumer<GenericJsonReloadListener<T>> syncOnReloadConsumer) {
        super(new GsonBuilder().create(), folderIn);
        objClass = oClass;
        codec = oCodec;
        syncOnReload = syncOnReloadConsumer;
    }

    /**
     * Adds an object to the map
     *
     * @param id  the resource location id
     * @param obj the object, or null
     **/
    public void put(final ResourceLocation id, @Nullable final T obj) {
        OBJECTS.put(id, Optional.ofNullable(obj));
    }

    /**
     * @param id a ResourceLocation name to retrieve
     * @return an Optional containing the object if found, otherwise empty
     **/
    public Optional<T> get(final ResourceLocation id) {
        return OBJECTS.getOrDefault(id, Optional.empty());
    }

    /**
     * @return a collection of all objects
     **/
    public Collection<Optional<T>> getValues() {
        return OBJECTS.values();
    }

    /**
     * @return a set of all keys that are present in the object map
     **/
    public Set<ResourceLocation> getKeys() {
        return OBJECTS.keySet();
    }

    /**
     * @return a collection of all object entries
     **/
    public Set<Entry<ResourceLocation, Optional<T>>> getEntries() {
        return OBJECTS.entrySet();
    }

    public DataResult<Tag> writeObject(final T obj) {
        // write Object T to NBT
        return codec.encodeStart(NbtOps.INSTANCE, obj);
    }

    public DataResult<T> jsonToObject(final JsonElement json) {
        // read Object T from json
        return codec.parse(JsonOps.INSTANCE, json);
    }

    public DataResult<T> readObject(final Tag nbt) {
        // read Object T from nbt
        return codec.parse(NbtOps.INSTANCE, nbt);
    }

    public void syncOnReload() {
        syncOnReload.accept(this);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager manager, ProfilerFiller profile) {
        // build the maps
        OBJECTS.clear();
        RPGGods.LOGGER.debug("Parsing Reloadable JSON map of type " + objClass.getName());
        jsons.forEach((key, input) -> OBJECTS.put(key, jsonToObject(input).resultOrPartial(
                error -> RPGGods.LOGGER.error("Failed to read JSON object for type" + objClass.getName() + "\n" + error))));
        // print size of the map for debugging purposes
        RPGGods.LOGGER.debug("Found " + OBJECTS.size() + " entries");
        boolean isServer = true;
        try {
            LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);
        } catch (Exception e) {
            isServer = false;
        }
        // if we're on the server, send syncing packets
        if (isServer == true) {
            syncOnReload();
        }
    }
}
