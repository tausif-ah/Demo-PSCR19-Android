package nist.p_70nanb17h188.demo.pscr19.logic.app.messaging;

import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.Objects;

import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;

public class MessagingName {

    @NonNull
    private final Name name;
    @NonNull
    private String appName;
    @NonNull
    private final MessagingNameType type;

    @NonNull
    final HashSet<MessagingName> parents = new HashSet<>();
    @NonNull
    final HashSet<MessagingName> children = new HashSet<>();

    MessagingName(@NonNull Name name, @NonNull String appName, @NonNull MessagingNameType type) {
        this.name = name;
        this.appName = appName;
        this.type = type;
    }

    @NonNull
    public String getAppName() {
        return appName;
    }

    @NonNull
    public Name getName() {
        return name;
    }

    @NonNull
    public MessagingNameType getType() {
        return type;
    }

    void setAppName(@NonNull String appName) {
        this.appName = appName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MessagingName that = (MessagingName) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s(%s,%s)", appName, name, type);
    }
}
