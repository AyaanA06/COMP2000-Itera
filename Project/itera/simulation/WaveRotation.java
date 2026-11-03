package itera.simulation;

import itera.model.zombie.ZombieType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Decides which zombie type is featured in each wave.
 * Every fifth wave is a boss wave and has no featured type.
 */
public final class WaveRotation {
    public static final int BOSS_WAVE_INTERVAL = 5;

    /** Every special zombie type takes a turn; standard zombies are never featured. */
    private static final List<ZombieType> FEATURED = Arrays.stream(ZombieType.values())
        .filter(type -> type != ZombieType.STANDARD)
        .toList();

    private WaveRotation() { }

    public static boolean isBossWave(int waveNumber) {
        checkWave(waveNumber);
        return waveNumber % BOSS_WAVE_INTERVAL == 0;
    }

    public static Optional<ZombieType> featuredFor(int waveNumber) {
        if (isBossWave(waveNumber)) {
            return Optional.empty();
        }

        // Only count normal waves, so boss waves never swallow a type's turn.
        int normalWavesBefore = (waveNumber - 1) - (waveNumber - 1) / BOSS_WAVE_INTERVAL;

        return Optional.of(FEATURED.get(normalWavesBefore % FEATURED.size()));
    }

    private static void checkWave(int waveNumber) {
        if (waveNumber < 1) throw new IllegalArgumentException("Wave numbers start at 1");
    }
}
