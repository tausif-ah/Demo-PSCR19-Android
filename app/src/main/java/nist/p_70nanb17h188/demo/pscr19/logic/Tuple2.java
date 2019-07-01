package nist.p_70nanb17h188.demo.pscr19.logic;

import java.util.Objects;

public class Tuple2<T1, T2> {
    private final T1 v1;
    private final T2 v2;

    public Tuple2(T1 v1, T2 v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    public T1 getV1() {
        return v1;
    }

    public T2 getV2() {
        return v2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple2<?, ?> tuple2 = (Tuple2<?, ?>) o;
        return Objects.equals(v1, tuple2.v1) &&
                Objects.equals(v2, tuple2.v2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(v1, v2);
    }
}
