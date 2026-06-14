package work.nemonet.littlemaidneo.util;

import java.util.Optional;

public interface ProcessDivider<T> {

    default void tick(int count) {
        for (int i = 0; i < count; i++) {
            if (isEnd()) {
                hasResult();
                return;
            }
            if (tick()) {
                return;
            }
        }
    }

    boolean tick();

    default boolean hasResult() {
        return getResult().isPresent();
    }

    Optional<T> getResult();

    boolean isEnd();

}
