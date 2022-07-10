package com.brandon3055.brandonscore.common.utills;

import com.brandon3055.brandonscore.BrandonsCore;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class Teleporter {

    public static class TeleportLocation {
        protected double xCoord;
        protected double yCoord;
        protected double zCoord;
        protected int dimension;
        protected float pitch;
        protected float yaw;
        protected String name;
        protected String dimentionName = "";
        protected boolean writeProtected = false;

        public TeleportLocation() {}

        public TeleportLocation(double x, double y, double z, int dimension) {
            this.xCoord = x;
            this.yCoord = y;
            this.zCoord = z;
            this.dimension = dimension;
            this.pitch = 0;
            this.yaw = 0;
        }

        public TeleportLocation(double x, double y, double z, int dimension, float pitch, float yaw) {
            this.xCoord = x;
            this.yCoord = y;
            this.zCoord = z;
            this.dimension = dimension;
            this.pitch = pitch;
            this.yaw = yaw;
        }

        public TeleportLocation(double x, double y, double z, int dimension, float pitch, float yaw, String name) {
            this.xCoord = x;
            this.yCoord = y;
            this.zCoord = z;
            this.dimension = dimension;
            this.pitch = pitch;
            this.yaw = yaw;
            this.name = name;
        }

        public double getXCoord() {
            return xCoord;
        }

        public double getYCoord() {
            return yCoord;
        }

        public double getZCoord() {
            return zCoord;
        }

        public int getDimension() {
            return dimension;
        }

        public String getDimensionName() {
            return dimentionName;
        }

        public float getPitch() {
            return pitch;
        }

        public float getYaw() {
            return yaw;
        }

        public String getName() {
            return name;
        }

        public boolean getWriteProtected() {
            return writeProtected;
        }

        public void setXCoord(double x) {
            xCoord = x;
        }

        public void setYCoord(double y) {
            yCoord = y;
        }

        public void setZCoord(double z) {
            zCoord = z;
        }

        public void setDimension(int d) {
            dimension = d;
        }

        public void setPitch(float p) {
            pitch = p;
        }

        public void setYaw(float y) {
            yaw = y;
        }

        public void setName(String s) {
            name = s;
        }

        public void setWriteProtected(boolean b) {
            writeProtected = b;
        }

        public void writeToNBT(NBTTagCompound compound) {
            compound.setDouble("X", xCoord);
            compound.setDouble("Y", yCoord);
            compound.setDouble("Z", zCoord);
            compound.setInteger("Dimension", dimension);
            compound.setFloat("Pitch", pitch);
            compound.setFloat("Yaw", yaw);
            compound.setString("Name", name);
            compound.setString("DimentionName", dimentionName);
            compound.setBoolean("WP", writeProtected);
        }

        public void readFromNBT(NBTTagCompound compound) {
            xCoord = compound.getDouble("X");
            yCoord = compound.getDouble("Y");
            zCoord = compound.getDouble("Z");
            dimension = compound.getInteger("Dimension");
            pitch = compound.getFloat("Pitch");
            yaw = compound.getFloat("Yaw");
            name = compound.getString("Name");
            dimentionName = compound.getString("DimentionName");
            writeProtected = compound.getBoolean("WP");
        }

        public void sendEntityToCoords(Entity entity) {
            entity.worldObj.playSoundEffect(
                    entity.posX,
                    entity.posY,
                    entity.posZ,
                    "draconicevolution:portal",
                    0.1F,
                    entity.worldObj.rand.nextFloat() * 0.1F + 0.9F);

            teleportEntity(entity, this);

            entity.worldObj.playSoundEffect(
                    entity.posX,
                    entity.posY,
                    entity.posZ,
                    "draconicevolution:portal",
                    0.1F,
                    entity.worldObj.rand.nextFloat() * 0.1F + 0.9F);
        }

        public void setDimentionName(String dimentionName) {
            this.dimentionName = dimentionName;
        }
    }

    @SuppressWarnings("unchecked")
    private static Entity teleportEntity(Entity entity, TeleportLocation destination) {
        if (entity == null || entity.worldObj.isRemote) return entity;

        World startWorld = entity.worldObj;
        World destinationWorld = BrandonsCore.proxy.getMCServer().worldServerForDimension(destination.dimension);

        if (destinationWorld == null) {
            LogHelper.error("Destination world dose not exist!");
            return entity;
        }

        Entity mount = entity.ridingEntity;
        if (entity.ridingEntity != null) {
            entity.mountEntity(null);
            mount = teleportEntity(mount, destination);
        }

        boolean interDimensional = startWorld.provider.dimensionId != destinationWorld.provider.dimensionId;

        startWorld.updateEntityWithOptionalForce(entity, false); // added

        if ((entity instanceof EntityPlayerMP) && interDimensional) {
            EntityPlayerMP player = (EntityPlayerMP) entity;
            player.closeScreen(); // added
            player.dimension = destination.dimension;
            player.playerNetServerHandler.sendPacket(new S07PacketRespawn(
                    player.dimension,
                    player.worldObj.difficultySetting,
                    destinationWorld.getWorldInfo().getTerrainType(),
                    player.theItemInWorldManager.getGameType()));
            ((WorldServer) startWorld).getPlayerManager().removePlayer(player);

            startWorld.playerEntities.remove(player);
            startWorld.updateAllPlayersSleepingFlag();
            int i = entity.chunkCoordX;
            int j = entity.chunkCoordZ;
            if ((entity.addedToChunk) && (startWorld.getChunkProvider().chunkExists(i, j))) {
                startWorld.getChunkFromChunkCoords(i, j).removeEntity(entity);
                startWorld.getChunkFromChunkCoords(i, j).isModified = true;
            }
            startWorld.loadedEntityList.remove(entity);
            startWorld.onEntityRemoved(entity);
        }

        entity.setLocationAndAngles(
                destination.xCoord, destination.yCoord, destination.zCoord, destination.yaw, destination.pitch);

        ((WorldServer) destinationWorld)
                .theChunkProviderServer.loadChunk((int) destination.xCoord >> 4, (int) destination.zCoord >> 4);

        destinationWorld.theProfiler.startSection("placing");
        if (interDimensional) {
            if (!(entity instanceof EntityPlayer)) {
                NBTTagCompound entityNBT = new NBTTagCompound();
                entity.isDead = false;
                entityNBT.setString("id", EntityList.getEntityString(entity));
                entity.writeToNBT(entityNBT);
                entity.isDead = true;
                entity = EntityList.createEntityFromNBT(entityNBT, destinationWorld);
                if (entity == null) {
                    LogHelper.error("Failed to teleport entity to new location");
                    return null;
                }
                entity.dimension = destinationWorld.provider.dimensionId;
            }
            destinationWorld.spawnEntityInWorld(entity);
            entity.setWorld(destinationWorld);
        }
        entity.setLocationAndAngles(
                destination.xCoord, destination.yCoord, destination.zCoord, destination.yaw, entity.rotationPitch);

        destinationWorld.updateEntityWithOptionalForce(entity, false);
        entity.setLocationAndAngles(
                destination.xCoord, destination.yCoord, destination.zCoord, destination.yaw, entity.rotationPitch);

        if ((entity instanceof EntityPlayerMP)) {
            EntityPlayerMP player = (EntityPlayerMP) entity;
            if (interDimensional) {
                player.mcServer.getConfigurationManager().func_72375_a(player, (WorldServer) destinationWorld);
            }
            player.playerNetServerHandler.setPlayerLocation(
                    destination.xCoord,
                    destination.yCoord,
                    destination.zCoord,
                    player.rotationYaw,
                    player.rotationPitch);
        }

        destinationWorld.updateEntityWithOptionalForce(entity, false);

        if (((entity instanceof EntityPlayerMP)) && interDimensional) {
            EntityPlayerMP player = (EntityPlayerMP) entity;
            player.theItemInWorldManager.setWorld((WorldServer) destinationWorld);
            player.mcServer.getConfigurationManager().updateTimeAndWeatherForPlayer(player, (WorldServer)
                    destinationWorld);
            player.mcServer.getConfigurationManager().syncPlayerInventory(player);

            for (PotionEffect potionEffect : (Iterable<PotionEffect>) player.getActivePotionEffects()) {
                player.playerNetServerHandler.sendPacket(new S1DPacketEntityEffect(player.getEntityId(), potionEffect));
            }

            player.playerNetServerHandler.sendPacket(
                    new S1FPacketSetExperience(player.experience, player.experienceTotal, player.experienceLevel));
            FMLCommonHandler.instance()
                    .firePlayerChangedDimensionEvent(
                            player, startWorld.provider.dimensionId, destinationWorld.provider.dimensionId);
        }
        entity.setLocationAndAngles(
                destination.xCoord, destination.yCoord, destination.zCoord, destination.yaw, entity.rotationPitch);

        if (mount != null) {
            entity.mountEntity(mount);
            if ((entity instanceof EntityPlayerMP)) {
                destinationWorld.updateEntityWithOptionalForce(entity, true);
            }
        }
        destinationWorld.theProfiler.endSection();
        entity.fallDistance = 0;
        return entity;
    }
}
