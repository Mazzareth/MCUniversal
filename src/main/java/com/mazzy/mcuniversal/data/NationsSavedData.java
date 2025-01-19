package com.mazzy.mcuniversal.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NationsSavedData extends SavedData {

    private static final String DATA_NAME = "mcuniversal_nations";

    public static class NationEntry {
        private final UUID nationId;
        private String name;
        private BlockPos capital;
        private final List<String> members;

        public NationEntry(UUID nationId, String name, BlockPos capital, List<String> members) {
            this.nationId = nationId;
            this.name = name;
            this.capital = capital;
            this.members = new ArrayList<>(members);
        }

        public UUID getNationId() {
            return nationId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BlockPos getCapital() {
            return capital;
        }

        public void setCapital(BlockPos capital) {
            this.capital = capital;
        }

        public List<String> getMembers() {
            return members;
        }

        public void addMember(String memberId) {
            if (!members.contains(memberId)) {
                members.add(memberId);
            }
        }

        public void removeMember(String memberId) {
            members.remove(memberId);
        }
    }

    private final List<NationEntry> nations = new ArrayList<>();

    public static NationsSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                NationsSavedData::load,
                NationsSavedData::new,
                DATA_NAME
        );
    }

    public NationsSavedData() {
    }

    public UUID createNation(String name, BlockPos capital, List<String> members) {
        UUID newNationId = UUID.randomUUID();
        NationEntry entry = new NationEntry(newNationId, name, capital, members);
        nations.add(entry);
        setDirty();
        return newNationId;
    }

    public NationEntry getNationById(UUID nationId) {
        for (NationEntry entry : nations) {
            if (entry.getNationId().equals(nationId)) {
                return entry;
            }
        }
        return null;
    }

    public boolean removeNation(UUID nationId) {
        for (int i = 0; i < nations.size(); i++) {
            if (nations.get(i).getNationId().equals(nationId)) {
                nations.remove(i);
                setDirty();
                return true;
            }
        }
        return false;
    }

    public List<NationEntry> getAllNations() {
        return nations;
    }

    /**
     * Finds the nation that contains the given player's UUID in its member list.
     * Returns null if the player is not in any nation.
     */
    public NationEntry getNationByMember(UUID playerUUID) {
        String playerUuidString = playerUUID.toString();
        for (NationEntry entry : nations) {
            if (entry.getMembers().contains(playerUuidString)) {
                return entry;
            }
        }
        return null;
    }

    public static NationsSavedData load(CompoundTag compound) {
        NationsSavedData data = new NationsSavedData();
        ListTag nationsListTag = compound.getList("nations", Tag.TAG_COMPOUND);

        for (Tag nationTag : nationsListTag) {
            if (nationTag instanceof CompoundTag nationCompound) {
                UUID nationId = nationCompound.hasUUID("nationId")
                        ? nationCompound.getUUID("nationId")
                        : UUID.randomUUID();

                String name = nationCompound.getString("name");
                int x = nationCompound.getInt("capitalX");
                int y = nationCompound.getInt("capitalY");
                int z = nationCompound.getInt("capitalZ");
                BlockPos capitalPos = new BlockPos(x, y, z);

                List<String> members = new ArrayList<>();
                if (nationCompound.contains("members", Tag.TAG_LIST)) {
                    ListTag membersList = nationCompound.getList("members", Tag.TAG_STRING);
                    for (Tag memberTag : membersList) {
                        if (memberTag instanceof StringTag stringMember) {
                            members.add(stringMember.getAsString());
                        }
                    }
                }

                data.nations.add(new NationEntry(nationId, name, capitalPos, members));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        ListTag nationsListTag = new ListTag();

        for (NationEntry entry : nations) {
            CompoundTag nationCompound = new CompoundTag();
            nationCompound.putUUID("nationId", entry.getNationId());
            nationCompound.putString("name", entry.getName());
            BlockPos capital = entry.getCapital();
            nationCompound.putInt("capitalX", capital.getX());
            nationCompound.putInt("capitalY", capital.getY());
            nationCompound.putInt("capitalZ", capital.getZ());

            ListTag membersList = new ListTag();
            for (String member : entry.getMembers()) {
                membersList.add(StringTag.valueOf(member));
            }
            nationCompound.put("members", membersList);

            nationsListTag.add(nationCompound);
        }

        compound.put("nations", nationsListTag);
        return compound;
    }
}