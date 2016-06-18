package com.uneg.aula8.chat;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;



public class Client extends Application
{
    //Nombres de Eventos
    final static String GUARDAR_USUARIO = "registrar_usuario";

    final static int ESPERA_ENTRE_CONEXIONES = 1000;
    final static int INTENTOS_CONEXION = 20;
    final static String PUERTO = "3000";

    static private String URL_SOCKET;

    static Socket socket;

    public static void start()
    {
        getUrlSocket();
        anadirEventosSocket();
    }

    public static boolean isConnected()
    {
        return socket.connected();
    }

    public static void anadirEvento(String nombreEvento, Emitter.Listener listener)
    {
        socket.on(nombreEvento, listener);
    }


    public static void enviarEvento(String nombreEvento, JSONObject objetoJSON)
    {
        socket.emit(nombreEvento,objetoJSON);
    }
    public static void enviarEvento(String nombreEvento, String data)
    {
        socket.emit(nombreEvento,data);
    }
    public static void enviarEvento(String nombreEvento)
    {
        socket.emit(nombreEvento);
    }

    public static boolean guardarUsuario(String nombre, String apellido, String cedula, String password, String imgPath)
    {
        JSONObject usuario = new JSONObject();
        if(isConnected() && !imgPath.isEmpty())
        {
            try
            {
                usuario.put("nombre", nombre);
                usuario.put("apellido", apellido);
                usuario.put("cedula", cedula);
                usuario.put("password", password);
                usuario.put("avatar",encodeImage(imgPath));
                //socket.emit(GUARDAR_USUARIO, usuario);
                return true;
            } catch (JSONException e)
            {
                return false;
            }
        }
        else
            return  false;
    }

    public static void sendFile(String filePath)
    {
        String encodedFile = encodeFile(filePath);
        JSONObject obj = new JSONObject();
        try
        {
            obj.put("archivo",encodedFile);
            obj.put("nombre",filePath.substring(filePath.lastIndexOf('/')+1));
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        enviarEvento("enviar_archivo",obj);

    }

    private static void getUrlSocket()
    {
        String numero = "100";
        String prueba;
        IO.Options opts = new IO.Options();
        opts.forceNew = true;
        opts.reconnection = true;

        for (int i=0;i<=INTENTOS_CONEXION ; i++)
        {

            if(i>10)
                numero = "1"+i;
            else
                numero = "10"+i;

            prueba = "http://192.168.1."+numero+":"+PUERTO;

            try
            {
                socket = IO.socket(prueba, opts);
                socket.connect();
                esperar(ESPERA_ENTRE_CONEXIONES);
                if(socket.connected())
                {
                    URL_SOCKET = prueba;
                    break;
                }
            }
            catch (URISyntaxException e)
            {

            }

        }
    }


    private static void anadirEventosSocket()
    {

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener()
        {
            @Override
            public void call(Object... args)
            {
                System.out.println("Conectado");
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener()
        {
            @Override
            public void call(Object... args) {
                System.out.println("Desconectado");
            }
        });



    }

    private static void esperar(int ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException e)
        {

        }
    }

    private static String encodeImage(String path)
    {
        File imagefile = new File(path);
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(imagefile);
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
        Bitmap bm = BitmapFactory.decodeStream(fis);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG,100,baos);
        byte[] b = baos.toByteArray();
        String encImage = Base64.encodeToString(b, Base64.DEFAULT);
        //Base64.de
        return encImage;
    }

    private static String encodeFile(String path)
    {
        File file = new File(path);

        byte[] bytes = new byte[0];
        try {
            bytes = loadFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private static byte[] loadFile(File file) throws IOException
    {
        InputStream is = new FileInputStream(file);

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
            throw new IOException("Could not completely read file "+file.getName());
        }

        is.close();
        return bytes;
    }

    private static Bitmap decodeImage(String data)
    {
        byte[] b = Base64.decode(data,Base64.DEFAULT);
        Bitmap bmp = BitmapFactory.decodeByteArray(b,0,b.length);
        return bmp;
    }
}
