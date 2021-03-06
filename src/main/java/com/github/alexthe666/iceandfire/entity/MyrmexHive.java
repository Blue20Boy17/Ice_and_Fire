package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.structures.WorldGenMyrmexHive;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.VillageDoorInfo;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;

public class MyrmexHive {
    private World world;
    private final List<VillageDoorInfo> villageDoorInfoList = Lists.<VillageDoorInfo>newArrayList();
    private final List<BlockPos> foodRooms = Lists.<BlockPos>newArrayList();
    private final List<BlockPos> babyRooms = Lists.<BlockPos>newArrayList();
    private final List<BlockPos> miscRooms = Lists.<BlockPos>newArrayList();
    private final List<BlockPos> allRooms = Lists.<BlockPos>newArrayList();
    private final Map<BlockPos, EnumFacing> entrances = Maps.<BlockPos, EnumFacing>newHashMap();
    private final Map<BlockPos, EnumFacing> entranceBottoms = Maps.<BlockPos, EnumFacing>newHashMap();
    private BlockPos centerHelper = BlockPos.ORIGIN;
    private BlockPos center = BlockPos.ORIGIN;
    private int villageRadius;
    private int lastAddDoorTimestamp;
    private int tickCounter;
    private int numMyrmex;
    private int noBreedTicks;
    private final Map<UUID, Integer> playerReputation = Maps.<UUID, Integer>newHashMap();
    private final List<HiveAggressor> villageAgressors = Lists.<HiveAggressor>newArrayList();
    private int numIronGolems;
    public UUID hiveUUID;

    public MyrmexHive(){
        this.hiveUUID = UUID.randomUUID();
    }

    public MyrmexHive(World worldIn) {
        this.world = worldIn;
        this.hiveUUID = UUID.randomUUID();
    }

    public MyrmexHive(World worldIn, BlockPos center, int radius) {
        this.world = worldIn;
        this.center = center;
        this.villageRadius = radius;
        this.hiveUUID = UUID.randomUUID();
    }

    public void setWorld(World worldIn) {
        this.world = worldIn;
    }

    public void tick(int tickCounterIn, World world) {
        this.tickCounter++;
        this.removeDeadAndOldAgressors();
        if (tickCounter % 20 == 0) {
            this.updateNumMyrmex(world);
        }
    }

    private void updateNumMyrmex(World world) {
        List<EntityMyrmexBase> list = world.<EntityMyrmexBase>getEntitiesWithinAABB(EntityMyrmexBase.class, new AxisAlignedBB((double) (this.center.getX() - this.villageRadius), (double) (this.center.getY() - 4), (double) (this.center.getZ() - this.villageRadius), (double) (this.center.getX() + this.villageRadius), (double) (this.center.getY() + 4), (double) (this.center.getZ() + this.villageRadius)));
        this.numMyrmex = list.size();

        if (this.numMyrmex == 0) {
            this.playerReputation.clear();
        }
    }

    public BlockPos getCenter() {
        return this.center;
    }

    public BlockPos getCenterGround() {
        return getGroundedPos(this.world, this.center);
    }

    public static BlockPos getGroundedPos(World world, BlockPos pos) {
        BlockPos current = pos;
        while(world.isAirBlock(current.down())){
            current = current.down();
        }
        return current;
    }

    public int getVillageRadius() {
        return this.villageRadius;
    }

    public int getNumMyrmex() {
        return this.numMyrmex;
    }

    public boolean isBlockPosWithinSqVillageRadius(BlockPos pos) {
        return this.center.distanceSq(pos) < (double) (this.villageRadius * this.villageRadius);
    }

    public boolean isAnnihilated() {
        return false;
    }

    public void addOrRenewAgressor(EntityLivingBase entitylivingbaseIn, int agressiveLevel) {
        for (HiveAggressor hive$villageaggressor : this.villageAgressors) {
            if (hive$villageaggressor.agressor == entitylivingbaseIn) {
                hive$villageaggressor.agressionTime = this.tickCounter;
                return;
            }
        }

        this.villageAgressors.add(new HiveAggressor(entitylivingbaseIn, this.tickCounter, agressiveLevel));
    }

    @Nullable
    public EntityLivingBase findNearestVillageAggressor(EntityLivingBase entitylivingbaseIn) {
        double d0 = Double.MAX_VALUE;
        int previousAgressionLevel = 0;
        HiveAggressor hive$villageaggressor = null;
        for (int i = 0; i < this.villageAgressors.size(); ++i) {
            HiveAggressor hive$villageaggressor1 = this.villageAgressors.get(i);
            double d1 = hive$villageaggressor1.agressor.getDistanceSq(entitylivingbaseIn);
            int agressionLevel = hive$villageaggressor1.agressionLevel;

            if (d1 <= d0 || agressionLevel > previousAgressionLevel) {
                hive$villageaggressor = hive$villageaggressor1;
                d0 = d1;
            }
            previousAgressionLevel = agressionLevel;
        }

        return hive$villageaggressor == null ? null : hive$villageaggressor.agressor;
    }

    public EntityPlayer getNearestTargetPlayer(EntityLivingBase villageDefender, World world) {
        double d0 = Double.MAX_VALUE;
        EntityPlayer entityplayer = null;

        for (UUID s : this.playerReputation.keySet()) {
            if (this.isPlayerReputationTooLow(s)) {
                EntityPlayer entityplayer1 = world.getPlayerEntityByUUID(s);

                if (entityplayer1 != null) {
                    double d1 = entityplayer1.getDistanceSq(villageDefender);

                    if (d1 <= d0) {
                        entityplayer = entityplayer1;
                        d0 = d1;
                    }
                }
            }
        }

        return entityplayer;
    }

    private void removeDeadAndOldAgressors() {
        Iterator<HiveAggressor> iterator = this.villageAgressors.iterator();

        while (iterator.hasNext()) {
            HiveAggressor hive$villageaggressor = iterator.next();

            if (!hive$villageaggressor.agressor.isEntityAlive() || Math.abs(this.tickCounter - hive$villageaggressor.agressionTime) > 300) {
                iterator.remove();
            }
        }
    }

    public int getPlayerReputation(UUID playerName) {
        Integer integer = this.playerReputation.get(playerName);
        return integer == null ? 0 : integer.intValue();
    }

    private UUID findUUID(String name) {
        if (this.world == null || this.world.getMinecraftServer() == null)
            return EntityPlayer.getOfflineUUID(name);
        GameProfile profile = this.world.getMinecraftServer().getPlayerProfileCache().getGameProfileForUsername(name);
        return profile == null ? EntityPlayer.getOfflineUUID(name) : profile.getId();
    }

    public int modifyPlayerReputation(UUID playerName, int reputation) {
        int i = this.getPlayerReputation(playerName);
        int j = MathHelper.clamp(i + reputation, -30, 10);
        this.playerReputation.put(playerName, Integer.valueOf(j));
        return j;
    }

    public boolean isPlayerReputationTooLow(UUID uuid) {
        return this.getPlayerReputation(uuid) <= -15;
    }

    /**
     * Read this village's data from NBT.
     */
    public void readVillageDataFromNBT(NBTTagCompound compound) {
        this.numMyrmex = compound.getInteger("PopSize");
        this.villageRadius = compound.getInteger("Radius");
        this.numIronGolems = compound.getInteger("Golems");
        this.lastAddDoorTimestamp = compound.getInteger("Stable");
        this.tickCounter = compound.getInteger("Tick");
        this.noBreedTicks = compound.getInteger("MTick");
        this.center = new BlockPos(compound.getInteger("CX"), compound.getInteger("CY"), compound.getInteger("CZ"));
        this.centerHelper = new BlockPos(compound.getInteger("ACX"), compound.getInteger("ACY"), compound.getInteger("ACZ"));
        NBTTagList nbttaglist = compound.getTagList("Doors", 10);
        for (int i = 0; i < nbttaglist.tagCount(); ++i) {
            NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
            VillageDoorInfo villagedoorinfo = new VillageDoorInfo(new BlockPos(nbttagcompound.getInteger("X"), nbttagcompound.getInteger("Y"), nbttagcompound.getInteger("Z")), nbttagcompound.getInteger("IDX"), nbttagcompound.getInteger("IDZ"), nbttagcompound.getInteger("TS"));
            this.villageDoorInfoList.add(villagedoorinfo);
        }

        NBTTagList foodRoomList = compound.getTagList("FoodRooms", 10);
        this.foodRooms.clear();
        for (int i = 0; i < foodRoomList.tagCount(); ++i) {
            NBTTagCompound nbttagcompound = foodRoomList.getCompoundTagAt(i);
            this.foodRooms.add(new BlockPos(nbttagcompound.getInteger("X"), nbttagcompound.getInteger("Y"), nbttagcompound.getInteger("Z")));
        }
        NBTTagList babyRoomList = compound.getTagList("BabyRooms", 10);
        this.babyRooms.clear();
        for (int i = 0; i < babyRoomList.tagCount(); ++i) {
            NBTTagCompound nbttagcompound = babyRoomList.getCompoundTagAt(i);
            this.babyRooms.add(new BlockPos(nbttagcompound.getInteger("X"), nbttagcompound.getInteger("Y"), nbttagcompound.getInteger("Z")));
        }
        NBTTagList miscRoomList = compound.getTagList("MiscRooms", 10);
        this.miscRooms.clear();
        for (int i = 0; i < miscRoomList.tagCount(); ++i) {
            NBTTagCompound nbttagcompound = miscRoomList.getCompoundTagAt(i);
            this.miscRooms.add(new BlockPos(nbttagcompound.getInteger("X"), nbttagcompound.getInteger("Y"), nbttagcompound.getInteger("Z")));
        }
        NBTTagList entrancesList = compound.getTagList("Entrances", 10);
        this.entrances.clear();
        for (int i = 0; i < entrancesList.tagCount(); ++i) {
            NBTTagCompound nbttagcompound = entrancesList.getCompoundTagAt(i);
            this.entrances.put(new BlockPos(nbttagcompound.getInteger("X"), nbttagcompound.getInteger("Y"), nbttagcompound.getInteger("Z")), EnumFacing.getHorizontal(nbttagcompound.getInteger("Facing")));
        }

        NBTTagList entranceBottomsList = compound.getTagList("EntranceBottoms", 10);
        this.entranceBottoms.clear();
        for (int i = 0; i < entranceBottomsList.tagCount(); ++i) {
            NBTTagCompound nbttagcompound = entranceBottomsList.getCompoundTagAt(i);
            this.entranceBottoms.put(new BlockPos(nbttagcompound.getInteger("X"), nbttagcompound.getInteger("Y"), nbttagcompound.getInteger("Z")), EnumFacing.getHorizontal(nbttagcompound.getInteger("Facing")));
        }
        hiveUUID = compound.getUniqueId("HiveUUID");
        if(hiveUUID == null){
            hiveUUID = UUID.randomUUID();
        }
        NBTTagList nbttaglist1 = compound.getTagList("Players", 10);
        for (int j = 0; j < nbttaglist1.tagCount(); ++j) {
            NBTTagCompound nbttagcompound1 = nbttaglist1.getCompoundTagAt(j);

            if (nbttagcompound1.hasKey("UUID")) {
                this.playerReputation.put(UUID.fromString(nbttagcompound1.getString("UUID")), Integer.valueOf(nbttagcompound1.getInteger("S")));
            } else {
                //World is never set here, so this will always be offline UUIDs, sadly there is no way to convert this.
                this.playerReputation.put(findUUID(nbttagcompound1.getString("Name")), Integer.valueOf(nbttagcompound1.getInteger("S")));
            }
        }
    }

    /**
     * Write this village's data to NBT.
     */
    public void writeVillageDataToNBT(NBTTagCompound compound) {
        compound.setInteger("PopSize", this.numMyrmex);
        compound.setInteger("Radius", this.villageRadius);
        compound.setInteger("Golems", this.numIronGolems);
        compound.setInteger("Stable", this.lastAddDoorTimestamp);
        compound.setInteger("Tick", this.tickCounter);
        compound.setInteger("MTick", this.noBreedTicks);
        compound.setInteger("CX", this.center.getX());
        compound.setInteger("CY", this.center.getY());
        compound.setInteger("CZ", this.center.getZ());
        compound.setInteger("ACX", this.centerHelper.getX());
        compound.setInteger("ACY", this.centerHelper.getY());
        compound.setInteger("ACZ", this.centerHelper.getZ());
        NBTTagList nbttaglist = new NBTTagList();
        for (VillageDoorInfo villagedoorinfo : this.villageDoorInfoList) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.setInteger("X", villagedoorinfo.getDoorBlockPos().getX());
            nbttagcompound.setInteger("Y", villagedoorinfo.getDoorBlockPos().getY());
            nbttagcompound.setInteger("Z", villagedoorinfo.getDoorBlockPos().getZ());
            nbttagcompound.setInteger("IDX", villagedoorinfo.getInsideOffsetX());
            nbttagcompound.setInteger("IDZ", villagedoorinfo.getInsideOffsetZ());
            nbttagcompound.setInteger("TS", villagedoorinfo.getLastActivityTimestamp());
            nbttaglist.appendTag(nbttagcompound);
        }

        NBTTagList foodRoomList = new NBTTagList();
        for (BlockPos pos : this.foodRooms) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.setInteger("X", pos.getX());
            nbttagcompound.setInteger("Y", pos.getY());
            nbttagcompound.setInteger("Z", pos.getZ());
            foodRoomList.appendTag(nbttagcompound);
        }
        compound.setTag("FoodRooms", foodRoomList);
        NBTTagList babyRoomList = new NBTTagList();
        for (BlockPos pos : this.babyRooms) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.setInteger("X", pos.getX());
            nbttagcompound.setInteger("Y", pos.getY());
            nbttagcompound.setInteger("Z", pos.getZ());
            babyRoomList.appendTag(nbttagcompound);
        }
        compound.setTag("BabyRooms", babyRoomList);
        NBTTagList miscRoomList = new NBTTagList();
        for (BlockPos pos : this.miscRooms) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.setInteger("X", pos.getX());
            nbttagcompound.setInteger("Y", pos.getY());
            nbttagcompound.setInteger("Z", pos.getZ());
            miscRoomList.appendTag(miscRoomList);
        }
        compound.setTag("MiscRooms", babyRoomList);
        NBTTagList entrancesList = new NBTTagList();
        for (Map.Entry<BlockPos, EnumFacing> entry : this.entrances.entrySet()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.setInteger("X", entry.getKey().getX());
            nbttagcompound.setInteger("Y", entry.getKey().getY());
            nbttagcompound.setInteger("Z", entry.getKey().getZ());
            nbttagcompound.setInteger("Facing", entry.getValue().getHorizontalIndex());
            entrancesList.appendTag(nbttagcompound);
        }
        compound.setTag("Entrances", entrancesList);

        NBTTagList entranceBottomsList = new NBTTagList();
        for (Map.Entry<BlockPos, EnumFacing> entry : this.entranceBottoms.entrySet()) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            nbttagcompound.setInteger("X", entry.getKey().getX());
            nbttagcompound.setInteger("Y", entry.getKey().getY());
            nbttagcompound.setInteger("Z", entry.getKey().getZ());
            nbttagcompound.setInteger("Facing", entry.getValue().getHorizontalIndex());
            entranceBottomsList.appendTag(nbttagcompound);
        }
        compound.setTag("EntranceBottoms", entranceBottomsList);
        compound.setUniqueId("HiveUUID", this.hiveUUID);
        compound.setTag("Doors", nbttaglist);
        NBTTagList nbttaglist1 = new NBTTagList();

        for (UUID s : this.playerReputation.keySet()) {
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();

            try {
                {
                    nbttagcompound1.setString("UUID", s.toString());
                    nbttagcompound1.setInteger("S", ((Integer) this.playerReputation.get(s)).intValue());
                    nbttaglist1.appendTag(nbttagcompound1);
                }
            } catch (RuntimeException var9) {
                ;
            }
        }

        compound.setTag("Players", nbttaglist1);
    }

    public void addRoom(BlockPos center, WorldGenMyrmexHive.RoomType roomType){
        if(roomType == WorldGenMyrmexHive.RoomType.FOOD){
            this.foodRooms.add(center);
        }else if(roomType == WorldGenMyrmexHive.RoomType.NURSERY){
            this.babyRooms.add(center);
        }else{
            this.miscRooms.add(center);
        }
    }

    public List<BlockPos> getRooms(WorldGenMyrmexHive.RoomType roomType){
        if(roomType == WorldGenMyrmexHive.RoomType.FOOD){
            return foodRooms;
        }else if(roomType == WorldGenMyrmexHive.RoomType.NURSERY){
            return babyRooms;
        }else{
            return miscRooms;
        }
    }

    public List<BlockPos> getAllRooms(){
        allRooms.clear();
        //allRooms.add(center);
        allRooms.addAll(foodRooms);
        allRooms.addAll(babyRooms);
        allRooms.addAll(miscRooms);
        return allRooms;
    }

    public BlockPos getRandomRoom(Random random, BlockPos returnPos){
        List<BlockPos> rooms = getAllRooms();
        return rooms.isEmpty() ? returnPos : rooms.get(random.nextInt(Math.max(rooms.size() - 1, 1)));
    }
    public BlockPos getRandomRoom(WorldGenMyrmexHive.RoomType roomType, Random random, BlockPos returnPos){
        List<BlockPos> rooms = getRooms(roomType);
        return rooms.isEmpty() ? returnPos : rooms.get(random.nextInt(Math.max(rooms.size() - 1, 1)));
    }

    public BlockPos getClosestEntranceToEntity(Entity entity, Random random, boolean randomize){
        Map.Entry<BlockPos, EnumFacing> closest = getClosestEntrance(entity);
        if(closest != null) {
            if (randomize) {
                BlockPos pos = closest.getKey().offset(closest.getValue(), random.nextInt(7) + 7).up(4);
                return pos.add(10 - random.nextInt(20), 0, 10 - random.nextInt(20));
            } else {
                return closest.getKey();
            }
        }
        return entity.getPosition();
    }

    public BlockPos getClosestEntranceBottomToEntity(Entity entity, Random random){
        Map.Entry<BlockPos, EnumFacing> closest = null;
        for (Map.Entry<BlockPos, EnumFacing> entry : this.entranceBottoms.entrySet()) {
            if(closest == null || closest.getKey().distanceSq(entity.posX, entity.posY, entity.posZ) > entry.getKey().distanceSq(entity.posX, entity.posY, entity.posZ)){
                closest = entry;
            }
        }
        return closest != null ? closest.getKey() : entity.getPosition();
    }


    public Map<BlockPos, EnumFacing> getEntrances(){
        return entrances;
    }

    public Map<BlockPos, EnumFacing> getEntranceBottoms(){
        return entranceBottoms;
    }

    private Map.Entry<BlockPos, EnumFacing> getClosestEntrance(Entity entity){
        Map.Entry<BlockPos, EnumFacing> closest = null;
        for (Map.Entry<BlockPos, EnumFacing> entry : this.entrances.entrySet()) {
            if(closest == null || closest.getKey().distanceSq(entity.posX, entity.posY, entity.posZ) > entry.getKey().distanceSq(entity.posX, entity.posY, entity.posZ)){
                closest = entry;
            }
        }
        return closest;
    }

    public void setDefaultPlayerReputation(int defaultReputation) {
        for (UUID s : this.playerReputation.keySet()) {
            this.modifyPlayerReputation(s, defaultReputation);
        }
    }

    public boolean repopulate() {
        int roomCount = this.allRooms.size();
        return this.numMyrmex < Math.min(80, roomCount * 4);
    }

    class HiveAggressor {
        public EntityLivingBase agressor;
        public int agressionTime;
        public int agressionLevel;

        HiveAggressor(EntityLivingBase agressorIn, int agressionTimeIn, int agressionLevel) {
            this.agressor = agressorIn;
            this.agressionTime = agressionTimeIn;
            this.agressionLevel = agressionLevel;
        }
    }

    public String toString(){
        return "MyrmexHive(x=" + this.center.getX() + ",y=" + this.center.getY() + ",z="  + this.center.getZ() + "), population=" + this.getNumMyrmex() + "\nUUID: " + hiveUUID;
    }
}