package itera.simulation;

import itera.model.zombie.*;
import java.util.List;
import java.util.Random;

/** ZombieType keeps ordinary zombie creation in one place. */
public final class ZombieSpawns {
    private static final List<ZombieType> TYPES = List.of(ZombieType.values());

    private ZombieSpawns() { }

    public static int typeCount() {
        return TYPES.size();
    }

    public static Zombie atType(int type, int x, int y) {
        return TYPES.get(Math.floorMod(type, TYPES.size())).create(x, y);
    }

    public static Zombie random(Random random, int x, int y) {
        return atType(random.nextInt(TYPES.size()), x, y);
    }
}
