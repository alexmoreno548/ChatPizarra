package com.uneg.aula8.chat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class MainFragment extends Fragment
{


    private static final int REQUEST_LOGIN = 0;
    private static final String NEW_MESSAGE_EVENT = "nuevo mensaje";
    private static final String NEW_USER = "usuario unido";
    private static final String USER_TYPING = "escribiendo";
    private static final String DISCONNECT_USER = "usuario desconectado";
    private static final String USER_NOT_TYPING = "no escribiendo";
    private static final String SEND_IMAGE = "enviar imagen"; 
    private static final int TYPING_TIMER_LENGTH = 600;

    private RecyclerView mensajesView;
    private EditText inputMensajeView;
    private List<Mensaje> mensajes = new ArrayList<Mensaje>();
    private RecyclerView.Adapter msjAdapter;
    private boolean estaEscribiendo = false;
    private Handler escribiendoHandler = new Handler();
    private String strUserName;
    private Object myFile;

    private static final int ACTIVITY_SELECT_IMAGE = 1020;

    public MainFragment () { super (); }

    @Override
    public void onAttach(Activity activity) 
    {
        super.onAttach(activity);
        System.out.println(activity);
        msjAdapter = new MensajeAdapter (activity, mensajes);
        addEnventsToClient();

        WifiManager wf = (WifiManager)activity.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifo = wf.getConnectionInfo();
        int ip = wifo.getIpAddress();
        String ipA = Formatter.formatIpAddress(ip);
        if (ipA.equals("192.168.1.103"))
            Client.enviarEvento("room", "room2");
        else
            Client.enviarEvento("room", "room1");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        strUserName = "YO"; // Conseguir el UserName del Usuario .. 
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) 
    {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onDestroy() 
    {
        super.onDestroy();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) 
    {
        super.onViewCreated(view, savedInstanceState);

        mensajesView = (RecyclerView) view.findViewById(R.id.messages);
        mensajesView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mensajesView.setAdapter(msjAdapter);

        inputMensajeView = (EditText) view.findViewById(R.id.message_input);
        inputMensajeView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) 
            {
                if (id == R.id.send || id == EditorInfo.IME_NULL) {
                    attemptSend();
                    return true;
                }
                return false;
            }
        });
        inputMensajeView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (null == strUserName) return;
                if (!Client.isConnected()) return;

                if (!estaEscribiendo)
                {
                    estaEscribiendo = true;
                    Client.enviarEvento(USER_TYPING);
                }

                escribiendoHandler.removeCallbacks(onEscribiendoTimeout);
                escribiendoHandler.postDelayed(onEscribiendoTimeout, TYPING_TIMER_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ImageButton sendButton = (ImageButton) view.findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                attemptSend();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (Activity.RESULT_OK != resultCode)
        {
            getActivity ().finish();
            return;
        }

        if (requestCode == ACTIVITY_SELECT_IMAGE && resultCode == Activity.RESULT_OK && null!=data) {
            android.net.Uri mImageUri = data.getData();
            //this.generalOptions = options;
            java.io.InputStream is;
            try
            {
                is = getActivity().getContentResolver().openInputStream(mImageUri);
                java.io.BufferedInputStream bis = new java.io.BufferedInputStream(is);
                Object fl = android.graphics.BitmapFactory.decodeStream(bis);

                repSend();

                addFile(strUserName, fl, true);


                Client.enviarEvento(SEND_IMAGE, encodeFile(mImageUri.getPath()));
            }
            catch (java.io.FileNotFoundException e) {}
        }
        else
        {
            strUserName = data.getStringExtra ("nombre_Usuario");
            int numUsers = data.getIntExtra ("numUsuarios", 1);

            addLog (getResources ().getString (R.string.message_welcome));
            addParticipantesLog (numUsers);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) 
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        int id = item.getItemId();

        switch (id)
        {
            case R.id.action_leave:
                salir();
                return true;
            case R.id.action_seleccionar:
                seleccionar();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void addLog(String msj) 
    {
        mensajes.add(new Mensaje.Builder(Mensaje.TYPE_LOG, false)
                .mensaje(msj).file(null).build());
        msjAdapter.notifyItemInserted(mensajes.size() - 1);
        scrollToBottom();
    }

    private void addParticipantesLog(int numUsers) 
    {
        addLog(getResources().getQuantityString(R.plurals.message_participants, numUsers, numUsers));
    }

    private void addMensaje(String usr, String msj, boolean isLocal) 
    {
        mensajes.add(new Mensaje.Builder(Mensaje.TYPE_MESSAGE, isLocal)
                .usuario(usr).mensaje(msj).file(null).build());
        msjAdapter.notifyItemInserted(mensajes.size() - 1);
        scrollToBottom();
    }

    private void addFile(String usr, Object file, boolean isLocal) 
    {
        mensajes.add(new Mensaje.Builder(Mensaje.TYPE_IMAGE, isLocal)
                .usuario(usr).file(file).build());
        msjAdapter.notifyItemInserted(mensajes.size() - 1);
        scrollToBottom();
    }

    private void addEscribiendo(String usr) 
    {
        mensajes.add(new Mensaje.Builder(Mensaje.TYPE_ACTION, false)
                .usuario(usr).file(null).build());
        msjAdapter.notifyItemInserted(mensajes.size() - 1);
        scrollToBottom();
    }

    private void removeEscribiendo(String usr) 
    {
        for (int i = mensajes.size() - 1; i >= 0; i--) 
        {
            Mensaje msj = mensajes.get(i);
            if (msj.getTipo() == Mensaje.TYPE_ACTION && msj.getUsuario().equals(usr)) {
                mensajes.remove(i);
                msjAdapter.notifyItemRemoved(i);
            }
        }
    }

    private void attemptSend() 
    {
        if (null == strUserName) return;
        if (!Client.isConnected()) return;

        estaEscribiendo = false;

        String msj = inputMensajeView.getText().toString().trim();
        if (TextUtils.isEmpty(msj)) {
            inputMensajeView.requestFocus();
            return;
        }

        inputMensajeView.setText("");

        repSend();

        addMensaje(strUserName, msj, true);


        Client.enviarEvento(NEW_MESSAGE_EVENT, msj);
    }

    private void repSend()
    {
        android.media.MediaPlayer snd = android.media.MediaPlayer.create(getActivity(), R.raw.popsend);
        snd.start();
    }

    private void repGet()
    {
        android.media.MediaPlayer snd = android.media.MediaPlayer.create(getActivity(), R.raw.popget);
        snd.start();
    }

    private void salir() 
    {

        strUserName = null;
    }

    private void seleccionar()
    {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, ACTIVITY_SELECT_IMAGE);
    }



    private String encodeFile(String path)
    {
        java.io.File file = new java.io.File(path);

        byte[] bytes = new byte[0];
        try 
        {
            bytes = loadFile(file);
        } 
        catch (java.io.IOException e) 
        {
            e.printStackTrace();
        }

        return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
    }

    private byte[] loadFile(java.io.File file) throws java.io.IOException
    {
        java.io.InputStream is = new java.io.FileInputStream(file);

        long length = file.length();

        byte[] bytes = new byte[(int)length];

        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }

        if (offset < bytes.length)
        {
            throw new java.io.IOException("Archivo no Leido..??  "+file.getName());
        }

        is.close();
        return bytes;
    }

    private Object decodeImage(String data)
    {
        byte[] b = android.util.Base64.decode(data,android.util.Base64.DEFAULT);
        Object bmp = android.graphics.BitmapFactory.decodeByteArray(b,0,b.length);
        return bmp;
    }

    private void scrollToBottom() 
    {
        mensajesView.scrollToPosition(msjAdapter.getItemCount() - 1);
    }

    private void addEnventsToClient()
    {
        Client.anadirEvento(Socket.EVENT_CONNECT_ERROR, onConnectError);
        Client.anadirEvento(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        Client.anadirEvento(NEW_MESSAGE_EVENT, onNuevoMensaje);
        Client.anadirEvento(NEW_USER, onUsuarioUnido);
        Client.anadirEvento(USER_TYPING, onEscribiendo);
        Client.anadirEvento(DISCONNECT_USER, onUsuarioDesconectado);
        Client.anadirEvento(USER_NOT_TYPING, onNoEscribiendo);
        Client.anadirEvento(SEND_IMAGE, onEnviarImagen);
    }

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity().getApplicationContext(),
                            R.string.error_connect, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onNuevoMensaje = new Emitter.Listener() {
        @Override
        public void call(final Object... args) 
        {
            getActivity().runOnUiThread(new Runnable() 
            {
                @Override
                public void run() 
                {
                    JSONObject data = (JSONObject) args[0];
                    String nombre_Usuario;
                    String mensaje;
                    try 
                    {
                        nombre_Usuario = data.getString("nombre_Usuario");
                        mensaje = data.getString("mensaje");
                    } 
                    catch (JSONException e) 
                    {
                        return;
                    }

                    removeEscribiendo(nombre_Usuario);

                    repGet();

                    addMensaje(nombre_Usuario, mensaje, false);
                }
            });
        }
    };

    private Emitter.Listener onUsuarioUnido = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String nombre_Usuario;
                    int numUsuarios;
                    try {
                        nombre_Usuario = data.getString("nombre_Usuario");
                        numUsuarios = data.getInt("numUsuarios");
                    } catch (JSONException e) {
                        return;
                    }

                    addLog(getResources().getString(R.string.message_user_joined, nombre_Usuario));
                    addParticipantesLog(numUsuarios);
                }
            });
        }
    };

    private Emitter.Listener onUsuarioDesconectado = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String nombre_Usuario;
                    int numUsuarios;
                    try {
                        nombre_Usuario = data.getString("nombre_Usuario");
                        numUsuarios = data.getInt("numUsuarios");
                    } catch (JSONException e) {
                        return;
                    }

                    addLog(getResources().getString(R.string.message_user_left, nombre_Usuario));
                    addParticipantesLog(numUsuarios);
                    removeEscribiendo(nombre_Usuario);
                }
            });
        }
    };

    private Emitter.Listener onEscribiendo = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String nombre_Usuario;
                    try {
                        nombre_Usuario = data.getString("nombre_Usuario");
                    } catch (JSONException e) {
                        return;
                    }
                    addEscribiendo(nombre_Usuario);
                }
            });
        }
    };

    private Emitter.Listener onNoEscribiendo = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String nombre_Usuario;
                    try {
                        nombre_Usuario = data.getString("nombre_Usuario");
                    } catch (JSONException e) {
                        return;
                    }
                    removeEscribiendo(nombre_Usuario);
                }
            });
        }
    };

    private Emitter.Listener onEnviarImagen = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String imgCodificada;
                    String nombre_Usuario;
                    try {

                        nombre_Usuario = data.getString("nombre_Usuario");
                        imgCodificada = data.getString("img_Codificada");

                        repGet();

                        addFile(nombre_Usuario, decodeImage(imgCodificada), false);
                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };

    private Runnable onEscribiendoTimeout = new Runnable() {
        @Override
        public void run() {
            if (!estaEscribiendo) return;

            estaEscribiendo = false;
            Client.enviarEvento(USER_NOT_TYPING);
        }
    };
}

