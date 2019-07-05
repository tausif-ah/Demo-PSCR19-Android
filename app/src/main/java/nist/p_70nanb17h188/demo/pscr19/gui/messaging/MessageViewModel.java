package nist.p_70nanb17h188.demo.pscr19.gui.messaging;

import java.util.ArrayList;

import nist.p_70nanb17h188.demo.pscr19.logic.Tuple2;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.Message;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingName;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNameType;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;

class MessageViewModel {
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
            if (mn == null)
                if (appName == null) {
                    appName = name.toString();
                    type = null;
                } else {
                    type = null;
                }
            else {
                appName = mn.getAppName();
                type = mn.getType();
            }
        }

        public Name getName() {
            return name;
        }

        public String getAppName() {
            return appName;
        }

        public MessagingNameType getType() {
            return type;
        }
    }

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

    public NameAttribute getSenderAttribute() {
        return senderAttribute;
    }

    public NameAttribute getReceiverAttribute() {
        return receiverAttribute;
    }

    public NameAttribute[] getNameCarryAttributes() {
        return nameCarryAttributes;
    }

    public boolean isPlayed() {
        return played;
    }

    public boolean isPlaying() {
        return playing;
    }

    void bindValues() {
        senderAttribute.bindValue();
        receiverAttribute.bindValue();
        for (NameAttribute nameCarryAttribute : nameCarryAttributes) {
            nameCarryAttribute.bindValue();
        }
    }

    private Tuple2<String, MessagingNameType> bindValue(MessagingNamespace namespace, Name name, Tuple2<String, MessagingNameType> origValue) {
        MessagingName mn = namespace.getName(name);
        if (mn == null)
            if (origValue == null)
                return new Tuple2<>(name.toString(), null);
            else
                return new Tuple2<>(origValue.getV1(), null);
        return new Tuple2<>(mn.getAppName(), mn.getType());
    }


}
