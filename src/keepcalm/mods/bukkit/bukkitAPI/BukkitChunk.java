package keepcalm.mods.bukkit.bukkitAPI;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import keepcalm.mods.bukkit.bukkitAPI.block.BukkitBlock;
import keepcalm.mods.bukkit.bukkitAPI.entity.BukkitEntity;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.ChunkPosition;
import net.minecraft.src.EmptyChunk;
import net.minecraft.src.ExtendedBlockStorage;
import net.minecraft.src.WorldChunkManager;
import net.minecraft.src.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
//import net.minecraft.src.Chunk;
//import org.bukkit.craftbukkit.block.BukkitBlock;

public class BukkitChunk implements Chunk {
    private WeakReference<net.minecraft.src.Chunk> weakChunk;
    private final WorldServer worldServer;
    private final int x;
    private final int z;
    private static final byte[] emptyData = new byte[2048];
    private static final short[] emptyBlockIDs = new short[4096];
    private static final byte[] emptySkyLight = new byte[2048];

    public BukkitChunk(net.minecraft.src.Chunk chunk) {
        if (!(chunk instanceof EmptyChunk)) {
            this.weakChunk = new WeakReference<net.minecraft.src.Chunk>(chunk);
        }

        worldServer = (WorldServer) getHandle().worldObj;
        x = getHandle().xPosition;
        z = getHandle().zPosition;
    }

    public World getWorld() {
        return Bukkit.getServer().getWorld(worldServer.getWorldInfo().getWorldName());
    }

    public BukkitWorld getBukkitWorld() {
        return (BukkitWorld) getWorld();
    }

    public net.minecraft.src.Chunk getHandle() {
        net.minecraft.src.Chunk c = weakChunk.get();

        if (c == null) {
            c = worldServer.getChunkFromChunkCoords(x, z);

            if (!(c instanceof EmptyChunk)) {
                weakChunk = new WeakReference<net.minecraft.src.Chunk>(c);
            }
        }

        return c;
    }

    void breakLink() {
        weakChunk.clear();
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public String toString() {
        return "BukkitChunk{" + "x=" + getX() + "z=" + getZ() + '}';
    }

    public Block getBlock(int x, int y, int z) {
        return new BukkitBlock(this, (getX() << 4) | (x & 0xF), y & 0xFF, (getZ() << 4) | (z & 0xF));
    }

    public Entity[] getEntities() {
        int count = 0, index = 0;
        net.minecraft.src.Chunk chunk = getHandle();

        for (int i = 0; i < 16; i++) {
            count += chunk.entityLists[i].size();
        }

        Entity[] entities = new Entity[count];

        for (int i = 0; i < 16; i++) {
            for (Object obj : chunk.entityLists[i].toArray()) {
                if (!(obj instanceof net.minecraft.src.Entity)) {
                    continue;
                }

                entities[index++] = BukkitEntity.getEntity((BukkitServer) Bukkit.getServer(), (net.minecraft.src.Entity) obj);
            }
        }

        return entities;
    }

    public BlockState[] getTileEntities() {
        int index = 0;
        net.minecraft.src.Chunk chunk = getHandle();
        BlockState[] entities = new BlockState[chunk.chunkTileEntityMap.size()];

        for (Object obj : chunk.chunkTileEntityMap.keySet().toArray()) {
            if (!(obj instanceof ChunkPosition)) {
                continue;
            }

            ChunkPosition position = (ChunkPosition) obj;
            entities[index++] = (BlockState) worldServer.getBlockTileEntity(position.x + (chunk.xPosition << 4), position.y, position.z + (chunk.zPosition << 4));
        }
        return entities;
    }

    public boolean isLoaded() {
        return getWorld().isChunkLoaded(this);
    }

    public boolean load() {
        return getWorld().loadChunk(getX(), getZ(), true);
    }

    public boolean load(boolean generate) {
        return getWorld().loadChunk(getX(), getZ(), generate);
    }

    public boolean unload() {
        return getWorld().unloadChunk(getX(), getZ());
    }

    public boolean unload(boolean save) {
        return getWorld().unloadChunk(getX(), getZ(), save);
    }

    public boolean unload(boolean save, boolean safe) {
        return getWorld().unloadChunk(getX(), getZ(), save, safe);
    }

    public ChunkSnapshot getChunkSnapshot() {
        return getChunkSnapshot(true, false, false);
    }

    public ChunkSnapshot getChunkSnapshot(boolean includeMaxBlockY, boolean includeBiome, boolean includeBiomeTempRain) {
        net.minecraft.src.Chunk chunk = getHandle();

        ExtendedBlockStorage[] cs = chunk.getBlockStorageArray(); /* Get sections */
        short[][] sectionBlockIDs = new short[cs.length][];
        byte[][] sectionBlockData = new byte[cs.length][];
        byte[][] sectionSkyLights = new byte[cs.length][];
        byte[][] sectionEmitLights = new byte[cs.length][];
        boolean[] sectionEmpty = new boolean[cs.length];

        for (int i = 0; i < cs.length; i++) {
            if (cs[i] == null) { /* Section is empty? */
                sectionBlockIDs[i] = emptyBlockIDs;
                sectionBlockData[i] = emptyData;
                sectionSkyLights[i] = emptySkyLight;
                sectionEmitLights[i] = emptyData;
                sectionEmpty[i] = true;
            } else { /* Not empty */
                short[] blockids = new short[4096];
                byte[] baseids = cs[i].getBlockLSBArray();

                /* Copy base IDs */
                for (int j = 0; j < 4096; j++) {
                    blockids[j] = (short) (baseids[j] & 0xFF);
                }

                if (true) { /* If we've got extended IDs */
                    byte[] extids = cs[i].getBlockMSBArray().data;

                    for (int j = 0; j < 2048; j++) {
                        short b = (short) (extids[j] & 0xFF);

                        if (b == 0) {
                            continue;
                        }

                        blockids[j<<1] |= (b & 0x0F) << 8;
                        blockids[(j<<1)+1] |= (b & 0xF0) << 4;
                    }
                }

                sectionBlockIDs[i] = blockids;

                /* Get block data nibbles */
                sectionBlockData[i] = new byte[2048];
                System.arraycopy(cs[i].getBlockMSBArray().data, 0, sectionBlockData[i], 0, 2048); // Should be getData
                sectionSkyLights[i] = new byte[2048];
                System.arraycopy(cs[i].getSkylightArray().data, 0, sectionSkyLights[i], 0, 2048); // Should be getSkyLight
                sectionEmitLights[i] = new byte[2048];
                System.arraycopy(cs[i].getBlocklightArray().data, 0, sectionEmitLights[i], 0, 2048); // Should be getBlockLight
            }
        }

        int[] hmap = null;

        if (includeMaxBlockY) {
            hmap = new int[256]; // Get copy of height map
            System.arraycopy(chunk.heightMap, 0, hmap, 0, 256);
        }

        BiomeGenBase[] biome = null;
        double[] biomeTemp = null;
        double[] biomeRain = null;

        if (includeBiome || includeBiomeTempRain) {
            WorldChunkManager wcm = chunk.worldObj.getWorldChunkManager();

            if (includeBiome) {
                biome = new BiomeGenBase[256];
                for (int i = 0; i < 256; i++) {
                    biome[i] = chunk.getBiomeGenForWorldCoords(i & 0xF, i >> 4, wcm);
                }
            }

            if (includeBiomeTempRain) {
                biomeTemp = new double[256];
                biomeRain = new double[256];
                float[] dat = wcm.getTemperatures((float[]) null, getX() << 4, getZ() << 4, 16, 16);

                for (int i = 0; i < 256; i++) {
                    biomeTemp[i] = dat[i];
                }

                dat = wcm.getRainfall((float[]) null, getX() << 4, getZ() << 4, 16, 16);

                for (int i = 0; i < 256; i++) {
                    biomeRain[i] = dat[i];
                }
            }
        }

        World world = getWorld();
        return new BukkitChunkSnapshot(getX(), getZ(), world.getName(), world.getFullTime(), sectionBlockIDs, sectionBlockData, sectionSkyLights, sectionEmitLights, sectionEmpty, hmap, biome, biomeTemp, biomeRain);
    }

    public static ChunkSnapshot getEmptyChunkSnapshot(int x, int z, BukkitWorld world, boolean includeBiome, boolean includeBiomeTempRain) {
        BiomeGenBase[] biome = null;
        double[] biomeTemp = null;
        double[] biomeRain = null;

        if (includeBiome || includeBiomeTempRain) {
            WorldChunkManager wcm = world.getHandle().getWorldChunkManager();

            if (includeBiome) {
                biome = new BiomeGenBase[256];
                for (int i = 0; i < 256; i++) {
                    biome[i] = world.getHandle().getBiomeGenForCoords((x << 4) + (i & 0xF), (z << 4) + (i >> 4));
                }
            }

            if (includeBiomeTempRain) {
                biomeTemp = new double[256];
                biomeRain = new double[256];
                float[] dat = wcm.getTemperatures((float[]) null, x << 4, z << 4, 16, 16);

                for (int i = 0; i < 256; i++) {
                    biomeTemp[i] = dat[i];
                }

                dat = wcm.getRainfall((float[]) null, x << 4, z << 4, 16, 16);

                for (int i = 0; i < 256; i++) {
                    biomeRain[i] = dat[i];
                }
            }
        }

        /* Fill with empty data */
        int hSection = world.getMaxHeight() >> 4;
        short[][] blockIDs = new short[hSection][];
        byte[][] skyLight = new byte[hSection][];
        byte[][] emitLight = new byte[hSection][];
        byte[][] blockData = new byte[hSection][];
        boolean[] empty = new boolean[hSection];

        for (int i = 0; i < hSection; i++) {
            blockIDs[i] = emptyBlockIDs;
            skyLight[i] = emptySkyLight;
            emitLight[i] = emptyData;
            blockData[i] = emptyData;
            empty[i] = true;
        }

        return new BukkitChunkSnapshot(x, z, world.getName(), world.getFullTime(), blockIDs, blockData, skyLight, emitLight, empty, new int[256], biome, biomeTemp, biomeRain);
    }

    static {
        Arrays.fill(emptySkyLight, (byte) 0xFF);
    }
}