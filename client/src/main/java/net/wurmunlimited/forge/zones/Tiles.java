package net.wurmunlimited.forge.zones;

import com.wurmonline.client.game.CaveDataBuffer;
import com.wurmonline.client.game.World;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.TilePicker;
import com.wurmonline.client.renderer.cave.CaveWallPicker;
import com.wurmonline.mesh.FieldData;
import com.wurmonline.mesh.FoliageAge;
import com.wurmonline.mesh.GrassData;
import com.wurmonline.mesh.Tiles.*;
import com.wurmonline.shared.util.ItemTypeUtilites;
import net.wurmunlimited.forge.config.ForgeClientConfig;

import static com.wurmonline.mesh.Tiles.*;

public class Tiles {
    private static final int TILE_MAIN = 0;
    private static final int NORTH_BORDER = 1;
    private static final int WEST_BORDER = 2;
    private static final int TILE_CORNER = 3;
    static final int TILE_PICK_SECTIONS = 4;
    private static final float PICK_WIDTH = 0.2f;

    public static String tilePickerName(final TilePicker picker,final World world,final int x,final int y,final int section,final int distance) {
        final boolean isDev = world.getServerConnection().isDev();
        String suffix = null;
        final Tile type = world.getNearTerrainBuffer().getTileType(x,y);
        final byte data = world.getNearTerrainBuffer().getData(x,y);
        final InventoryMetaItem active = world.getHud().getSourceItem();
        String customSuffix = null;
        boolean isSlopeShown = false;
        if(active!=null && ItemTypeUtilites.doesShowSlopes(active.getTypeBits())) {
            isSlopeShown = true;
            customSuffix = tilePickerSlopeSuffix(picker,world,x,y,section,distance,false);
        } else if(type==Tile.TILE_FIELD || type==Tile.TILE_FIELD2) {
            customSuffix = tilePickerFieldSuffix(picker,world,section,distance,type,data);
        } else if(type.isTree()) {
            customSuffix = tilePickerTreeSuffix(picker,world,section,distance,type,data);
        } else if(type.isBush()) {
            customSuffix = tilePickerTreeSuffix(picker,world,section,distance,type,data);
        } else if(type==Tile.TILE_GRASS) {
            customSuffix = tilePickerGrassSuffix(picker,world,section,distance,data);
        }
        if(customSuffix!=null) {
            suffix = customSuffix;
            if(!isSlopeShown && world.getServerConnection().isDev()) {
                final String suffix2 = tilePickerSlopeSuffix(picker,world,x,y,section,distance,true);
                if(suffix2!=null) {
                    suffix = suffix+" "+suffix2;
                }
            }
        } else if(isDev) {
            suffix = tilePickerSlopeSuffix(picker,world,x,y,section,distance,true);
        }
        if(suffix==null) suffix = "";
        if(section==TILE_MAIN) return type.getTileName(data)+suffix;
        if(section==TILE_CORNER) return "Tile corner"+suffix;
        return "Tile border"+suffix;
    }

    private static String tilePickerSlopeSuffix(final TilePicker picker,final World world,final int x,final int y,final int section,final int distance,final boolean isDev) {
        if(isDev || (distance-1)*15<world.getPlayer().getSkillSet().getSkillValue("digging")) {
            if(section==TILE_MAIN) {
                final float h00 = world.getNearTerrainBuffer().getHeight(x,y);
                final float h2 = world.getNearTerrainBuffer().getHeight(x,y+1);
                final float h3 = world.getNearTerrainBuffer().getHeight(x+1,y);
                final float h4 = world.getNearTerrainBuffer().getHeight(x+1,y+1);
                if(h00==h2 && h00==h3 && h00==h4) return " (flat)";
            } else {
                final float hHere = world.getNearTerrainBuffer().getHeight(x,y);
                float hOther;
                boolean atBottom;
                if(section==NORTH_BORDER) {
                    final float playerX = world.getPlayerPosX()/4.0f;
                    hOther = world.getNearTerrainBuffer().getHeight(x+1,y);
                    atBottom = (playerX<x+0.5f^hHere>hOther);
                } else {
                    final float playerY = world.getPlayerPosY()/4.0f;
                    hOther = world.getNearTerrainBuffer().getHeight(x,y+1);
                    atBottom = (playerY<y+0.5f^hHere>hOther);
                }
                final float slopeDelta = (hHere-hOther)*10.0f;
                final int slope = Math.round(Math.abs(slopeDelta));
                if(slope==0) return " (flat)";
                return " ("+slope+" slope "+(atBottom? "up" : "down")+")";
            }
        }
        return null;
    }

    private static String tilePickerFieldSuffix(final TilePicker picker,final World world,final int section,final int distance,final Tile type,final byte data) {
        if(section==TILE_MAIN) {
            final float skill = world.getPlayer().getSkillSet().getSkillValue("farming");
            if(distance*15<skill) {
                String suffix = (FieldData.getAge(data)>0)? FieldData.getTypeName(type,data) : "Seeds";
                if((distance+1)*15<skill || distance==0)
                    suffix = suffix+", "+FieldData.getAgeName(data);
                if(!FieldData.isTended(data)) suffix += ", untended";
                return " ("+suffix+")";
            }
        }
        return null;
    }

    private static String tilePickerGrassSuffix(final TilePicker picker,final World world,final int section,final int distance,final byte data) {
        if(section==TILE_MAIN) {
            final float skill = world.getPlayer().getSkillSet().getSkillValue("gardening");
            if(distance*15<skill && GrassData.getFlowerType(data)>0) {
                final String suffix = GrassData.getFlowerTypeName(data);
                return " ("+suffix+")";
            }
        }
        return null;
    }

    private static String tilePickerTreeSuffix(final TilePicker picker,final World world,final int section,final int distance,final Tile type,final byte data) {
        if(section==TILE_MAIN) {
            final float skill = world.getPlayer().getSkillSet().getSkillValue("forestry");
            if((distance+1)*15<skill) {
                final FoliageAge fage = FoliageAge.getFoliageAge(data);
                String suffix = fage.getAgeName();
                if(type==Tile.TILE_BUSH_LINGONBERRY) suffix = suffix.replace(", sprouting","");
                if(fage.getAgeId()>FoliageAge.YOUNG_FOUR.getAgeId() && fage.getAgeId()<FoliageAge.OVERAGED.getAgeId() &&
                   type.usesNewData() && type.isNormal() && (data&0x8)>0) suffix += ", harvestable";
                return " ("+suffix+")";
            }
        }
        return null;
    }

    public static String caveWallPickerName(final CaveWallPicker picker,final World world,final int x,final int y,final String name,final int distance) {
        boolean isDev = world.getServerConnection().isDev();
        float mining = world.getPlayer().getSkillSet().getSkillValue("mining");
        int wallSide = picker.getWallId();
        if(wallSide==CAVE_SIDE_FLOOR || wallSide==CAVE_SIDE_ROOF) {
            final int playerX = world.getPlayerCurrentTileX();
            final int playerY = world.getPlayerCurrentTileY();
            final int distX = Math.abs(playerX-x);
            final int distY = Math.abs(playerY-y);
            int dist = Math.max(distX,distY);
            if(dist>0) --dist;
            if(isDev || (dist+1)*15<mining) {
                final InventoryMetaItem active = world.getHud().getSourceItem();
                if((active!=null && active.getBaseName().contains("pickaxe")) || isDev) {
                    boolean isFlat = false;
                    if(wallSide==CAVE_SIDE_ROOF) {
                        final short h00 = world.getCaveBuffer().getRawCeiling(x,y);
                        final short h2 = world.getCaveBuffer().getRawCeiling(x,y+1);
                        final short h3 = world.getCaveBuffer().getRawCeiling(x+1,y);
                        final short h4 = world.getCaveBuffer().getRawCeiling(x+1,y+1);
                        isFlat = (h00==h2 && h00==h3 && h00==h4);
                    } else {
                        final short h00 = world.getCaveBuffer().getRawFloor(x,y);
                        final short h2 = world.getCaveBuffer().getRawFloor(x,y+1);
                        final short h3 = world.getCaveBuffer().getRawFloor(x+1,y);
                        final short h4 = world.getCaveBuffer().getRawFloor(x+1,y+1);
                        isFlat = (h00==h2 && h00==h3 && h00==h4);
                    }
                    if(isFlat) return name+" (flat)";
                }
            }
        } else {
            if(wallSide==CAVE_SIDE_CORNER) {
                if(ForgeClientConfig.showExtraTooltips) {
                    if(isDev || (distance+1)*15<mining) {
                        short floorHeight = world.getCaveBuffer().getRawFloor(x,y);
                        short ceilingHeight = world.getCaveBuffer().getRawCeiling(x,y);
                        return "Tile corner ("+(ceilingHeight-floorHeight)+")";
                    }
                }
                return "Tile corner";
            }
            if(wallSide==CAVE_SIDE_BORDER_NORTH || wallSide==CAVE_SIDE_BORDER_WEST || wallSide==CAVE_SIDE_BORDER_SOUTH || wallSide==CAVE_SIDE_BORDER_EAST) {
                String customSuffix = "";
                final InventoryMetaItem active2 = world.getHud().getSourceItem();
                if(world.getServerConnection().isDev())
                    customSuffix = caveWallPickerSlopeSuffix(picker,world,distance,true);
                else if(active2!=null && (active2.getBaseName().contains("pickaxe") || active2.getBaseName().contains("concrete") ||
                                          active2.getBaseName().contains("trowel") || active2.getBaseName().contains("mallet") ||
                                          active2.getBaseName().contains("hammer"))) {
                    if((distance+1)*15<mining) {
                        customSuffix = caveWallPickerSlopeSuffix(picker,world,distance,false);
                    }
                }
                return "Tile border"+customSuffix;
            }
        }
        return name;
    }

    private static String caveWallPickerSlopeSuffix(final CaveWallPicker picker,final World world,final int distance,final boolean isDev) {
        if(isDev || (distance+1)*15<world.getPlayer().getSkillSet().getSkillValue("mining")) {
            if(ForgeClientConfig.showExtraTooltips) {
                String floor = caveWallPickerSlopeSuffix(picker,world,false);
                String ceiling = caveWallPickerSlopeSuffix(picker,world,true);
                if(floor.equals(ceiling)) return " ("+floor+")";
                return " ("+floor+", "+ceiling+")";
            }
            return " ("+caveWallPickerSlopeSuffix(picker,world,false)+")";
        }
        return "";
    }

    private static String caveWallPickerSlopeSuffix(final CaveWallPicker picker,final World world,final boolean ceiling) {
        CaveDataBuffer cdb = world.getCaveBuffer();
        int wallSide = picker.getWallId();
        int xOrigin = picker.getXNeighbor();
        int yOrigin = picker.getYNeighbor();
        if(wallSide==CAVE_SIDE_BORDER_SOUTH) ++yOrigin;
        else if(wallSide==CAVE_SIDE_BORDER_EAST) ++xOrigin;
        final float hHere = ceiling? cdb.getAdjustedCeiling(xOrigin,yOrigin) : cdb.getAdjustedFloor(xOrigin,yOrigin);
        float hOther;
        boolean atBottom;
        if(wallSide==CAVE_SIDE_BORDER_NORTH || wallSide==CAVE_SIDE_BORDER_SOUTH) {
            final float playerX = world.getPlayerPosX()/4.0f;
            hOther = ceiling? cdb.getAdjustedCeiling(xOrigin+1,yOrigin) : cdb.getAdjustedFloor(xOrigin+1,yOrigin);
            atBottom = (playerX<xOrigin+0.5f^hHere>hOther);
        } else {
            final float playerY = world.getPlayerPosY()/4.0f;
            hOther = ceiling? cdb.getAdjustedCeiling(xOrigin,yOrigin+1) : cdb.getAdjustedFloor(xOrigin,yOrigin+1);
            atBottom = (playerY<yOrigin+0.5f^hHere>hOther);
        }
        final float slopeDelta = (hHere-hOther)*10.0f;
        final int slope = Math.round(Math.abs(slopeDelta));
        if(slope==0) return "flat";
        return slope+" slope "+(atBottom? "up" : "down");
    }
}
