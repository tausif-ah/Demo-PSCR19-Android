package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.app.Application;
import android.widget.Toast;

import nist.p_70nanb17h188.demo.pscr19.logic.Device;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

final class LinkLayer_Impl {


    /**
     * Singleton pattern, prevent the class to be instantiated by the others.
     */
    LinkLayer_Impl(Application application) {
        TCPConnectionManager instance = TCPConnectionManager.init();
        if (instance == null)
            Toast.makeText(application.getApplicationContext(), "Failed in creating TCPConnectionManager!", Toast.LENGTH_LONG).show();
        WifiLinkManager.init(application);
        Log.d("LinkLayer_Impl", "%s initialized", Device.getName());
    }

    public boolean sendData(NeighborID id, byte[] data, int start, int len) {
        // Do not forget to flush stream after each send, when in TCP
        return false;
    }

    /**
     * Example function on forwarding a data to all the handlers.
     *
     * @param id   neighbor id.
     * @param data data received.
     */
    private void onDataReceived(NeighborID id, byte[] data) {
//        Intent intent = new Intent();
//        intent.putExtra

//        for (DataReceivedHandler dataHandler : dataHandlers) {
//            byte[] toForward = new byte[data.length];
//            // make a copy and send, so that the users have the freedom to update the content.
//            System.arraycopy(data, 0, toForward, 0, data.length);
//            dataHandler.dataReceived(id, toForward);
//        }
//        dataHandlers.forEach(h -> {
//            byte[] toForward = new byte[data.length];
//            // make a copy and send, so that the users have the freedom to update the content.
//            System.arraycopy(data, 0, toForward, 0, data.length);
//            h.dataReceived(id, toForward);
//        });
    }
}
