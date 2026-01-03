package me.putindeer.miscolegio.comodin;

import lombok.Getter;
import me.putindeer.miscolegio.Main;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

@Getter
public enum ComodinType {
    EMPUJAR(ComodinTier.C, "Empujar", "Empuja a otro jugador.", Material.STICK),
    COPIAR(ComodinTier.B, "Copiar", "Copia la respuesta del jugador con más vidas.", Material.PAPER),
    CAMBIO_ASIENTO(ComodinTier.B, "Cambio de asiento", "Intercambia 2 jugadores aleatorios.", Material.ENDER_PEARL),
    BANO(ComodinTier.A, "Permiso para el baño", "Evita una pregunta teletransportándote.", Material.WATER_BUCKET),
    SILENCIO(ComodinTier.A, "Silencio en la clase", "Vuelve invisible a todos por 10 segundos.", Material.GLASS_BOTTLE),
    RECUPERATIVA(ComodinTier.S, "Recuperativa", "Otorga una vida adicional.", Material.GOLDEN_APPLE);

    private final ComodinTier tier;
    private final String name;
    private final String description;
    private final Material material;

    ComodinType(ComodinTier tier, String name, String description, Material material) {
        this.tier = tier;
        this.name = name;
        this.description = description;
        this.material = material;
    }

    public ItemStack buildItem() {
        Main plugin = Main.getInstance();
        String name = getTier().getColor() + getName();
        String description = getDescription();
        return switch (this) {
            case EMPUJAR -> plugin.utils.ib(getMaterial()).name(name).lore(description).enchant(Enchantment.KNOCKBACK, 5).hideEnchantments().build();
            default -> plugin.utils.ib(getMaterial()).name(name).lore(description).build();
        };
    }
}
