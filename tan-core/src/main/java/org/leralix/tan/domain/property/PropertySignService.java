package org.leralix.tan.domain.property;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.leralix.lib.position.Vector3D;
import org.leralix.tan.TownsAndNations;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.dataclass.PropertyData;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.listeners.interact.events.property.CreatePropertyEvent;
import org.leralix.tan.utils.gameplay.TANCustomNBT;

/**
 * Handles sign display and management for a property. State is kept on PropertyData for Gson compatibility.
 */
public class PropertySignService {

    private final PropertyData property;

    public PropertySignService(PropertyData property) {
        this.property = property;
    }

    public void updateSign() {
        Vector3D signLocation = property.getSignLocation();
        if (signLocation == null) {
            return;
        }
        try {
            World world = Bukkit.getWorld(signLocation.getWorldID());
            if (world == null) {
                return;
            }
            Block signBlock =
                world.getBlockAt(signLocation.getX(), signLocation.getY(), signLocation.getZ());
            if (!(signBlock.getState() instanceof Sign)) {
                TownsAndNations.getPlugin()
                    .getLogger()
                    .warning("Property " + property.getName() + " sign location is not a sign: " + signBlock.getType());
                return;
            }
            Sign sign = (Sign) signBlock.getState();
            String[] lines = updateLines();
            SignSide signSide = sign.getSide(Side.FRONT);
            signSide.line(0, org.leralix.tan.utils.text.ComponentUtil.fromLegacy(lines[0]));
            signSide.line(1, org.leralix.tan.utils.text.ComponentUtil.fromLegacy(lines[1]));
            signSide.line(2, org.leralix.tan.utils.text.ComponentUtil.fromLegacy(lines[2]));
            signSide.line(3, org.leralix.tan.utils.text.ComponentUtil.fromLegacy(lines[3]));
            sign.update();
        } catch (Exception e) {
            TownsAndNations.getPlugin()
                .getLogger()
                .warning("Error updating sign for property " + property.getName() + ": " + e.getMessage());
        }
    }

    private String[] updateLines() {
        String[] lines = new String[4];
        LangType langType = Lang.getServerLang();
        lines[0] = Lang.SIGN_NAME.get(langType, property.getName());
        lines[1] = Lang.SIGN_PLAYER.get(langType, property.getOwner().getName());
        if (property.isForSale()) {
            lines[2] = Lang.SIGN_FOR_SALE.get(langType);
            lines[3] = Lang.SIGN_SALE_PRICE.get(langType, Double.toString(property.getSalePrice()));
        } else if (property.isForRent()) {
            lines[2] = Lang.SIGN_RENT.get(langType);
            lines[3] = Lang.SIGN_RENT_PRICE.get(langType, Double.toString(property.getRentPrice()));
        } else if (property.isRented()) {
            lines[2] = Lang.SIGN_RENTED_BY.get(langType);
            ITanPlayer renter = property.getRenter();
            lines[3] = renter != null ? renter.getNameStored() : "Unknown";
        } else {
            lines[2] = Lang.SIGN_NOT_FOR_SALE.get(langType);
            lines[3] = "";
        }
        return lines;
    }

    public Optional<Block> getSign() {
        Vector3D signLocation = property.getSignLocation();
        World world = Bukkit.getWorld(signLocation.getWorldID());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(
            world.getBlockAt(signLocation.getX(), signLocation.getY(), signLocation.getZ()));
    }

    public void setSignData() {
        Vector3D signLocation = property.getSignLocation();
        Vector3D supportLocation = property.getSupportLocation();
        TANCustomNBT.setBockMetaData(
            signLocation.getLocation().getBlock(), "propertySign", property.getTotalID());
        TANCustomNBT.setBockMetaData(
            supportLocation.getLocation().getBlock(), "propertySign", property.getTotalID());
    }

    public void createPropertySign(Player player, Block block, BlockFace blockFace) {
        Location selectedSignLocation = block.getRelative(blockFace).getLocation();
        selectedSignLocation
            .getBlock()
            .setType(blockFace == BlockFace.UP ? Material.OAK_SIGN : Material.OAK_WALL_SIGN);
        BlockState blockState = selectedSignLocation.getBlock().getState();
        Sign sign = (Sign) blockState;
        if (blockFace != BlockFace.UP) {
            BlockFace direction =
                CreatePropertyEvent.getTopDirection(block.getLocation(), player.getLocation());
            Directional directional = (Directional) sign.getBlockData();
            directional.setFacing(direction);
            sign.setBlockData(directional);
        } else {
            org.bukkit.block.data.type.Sign signData =
                (org.bukkit.block.data.type.Sign) sign.getBlockData();
            BlockFace direction =
                CreatePropertyEvent.getTopDirection(block.getLocation(), player.getLocation());
            signData.setRotation(direction);
            sign.setBlockData(signData);
        }
        sign.update();
        block.setMetadata(
            "propertySign", new FixedMetadataValue(TownsAndNations.getPlugin(), property.getTotalID()));
        sign.getBlock()
            .setMetadata(
                "propertySign", new FixedMetadataValue(TownsAndNations.getPlugin(), property.getTotalID()));
        property.setSignLocation(new Vector3D(selectedSignLocation));
        property.setSupportLocation(new Vector3D(block.getLocation()));
        setSignData();
        updateSign();
    }

    public void removeSign() {
        Vector3D signLocation = property.getSignLocation();
        Vector3D supportLocation = property.getSupportLocation();
        World world = Bukkit.getWorld(signLocation.getWorldID());
        if (world == null) {
            return;
        }
        Block signBlock = signLocation.getLocation().getBlock();
        signBlock.setType(Material.AIR);
        TANCustomNBT.removeBockMetaData(signBlock, "propertySign");
        TANCustomNBT.removeBockMetaData(supportLocation.getLocation().getBlock(), "propertySign");
        world.spawnParticle(Particle.BUBBLE_POP, signBlock.getLocation(), 5);
    }
}
