package nist.p_70nanb17h188.demo.pscr19.gui.messaging;

import java.util.Arrays;

import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.Message;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingName;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNameType;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;

class MessageViewModel {
    private final Message message;
    private NameAttribute senderAttribute, receiverAttribute;
    private NameAttribute[] nameCarryAttributes;
    private boolean played = false;
    private boolean playing = false;

    MessageViewModel(Message message) {
        this.message = message;
        senderAttribute = new NameAttribute(message.getSenderGroup());
        receiverAttribute = new NameAttribute(message.getReceiverGroup());
        nameCarryAttributes = new NameAttribute[message.getCarriedNames().length];
        Name[] carriedNames = message.getCarriedNames();
        for (int i = 0; i < carriedNames.length; i++) {
            nameCarryAttributes[i] = new NameAttribute(carriedNames[i]);
        }
        bindValues();
    }

    public Message getMessage() {
        return message;
    }

    NameAttribute getSenderAttribute() {
        return senderAttribute;
    }

    NameAttribute getReceiverAttribute() {
        return receiverAttribute;
    }

    NameAttribute[] getNameCarryAttributes() {
        return nameCarryAttributes;
    }

    boolean isPlayed() {
        return played;
    }

    void setPlayed(boolean played) {
        this.played = played;
    }

    boolean isPlaying() {
        return playing;
    }

    void setPlaying(boolean playing) {
        this.playing = playing;
    }

    void bindValues() {
        senderAttribute.bindValue();
        receiverAttribute.bindValue();
        for (NameAttribute nameCarryAttribute : nameCarryAttributes) {
            nameCarryAttribute.bindValue();
        }
    }

    static class NameAttribute {
        private final Name name;
        private String appName;
        private MessagingNameType type;

        NameAttribute(Name name) {
            this.name = name;
            bindValue();
        }

        private void bindValue() {
            MessagingName mn = MessagingNamespace.getDefaultInstance().getName(name);
            if (mn == null) if (appName == null) {
                appName = name.toString();
                type = null;
            } else {
                type = null;
            }
            else {
                String[] incidents = MessagingNamespace.getDefaultInstance().getNameIncidents(mn.getName());
                appName = String.format("%s %s", mn.getAppName(), incidents.length == 0 ? "" : Arrays.toString(incidents));
                type = mn.getType();
            }
        }

        Name getName() {
            return name;
        }

        String getAppName() {
            return appName;
        }

        MessagingNameType getType() {
            return type;
        }

    }
}
