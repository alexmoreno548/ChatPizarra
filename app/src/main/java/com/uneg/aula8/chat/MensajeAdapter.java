package com.uneg.aula8.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;


public class MensajeAdapter extends RecyclerView.Adapter<MensajeAdapter.ViewHolder> {

    private List<Mensaje> mensajes;
    private int[] usr_Colores;

    public MensajeAdapter(Context context, List<Mensaje> msjs) {
        mensajes = msjs;
        usr_Colores = context.getResources().getIntArray(R.array.username_colors);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = -1;
        switch (viewType) {
            case Mensaje.TYPE_MESSAGE:
                layout = R.layout.item_message;
                break;
            case Mensaje.TYPE_LOG:
                layout = R.layout.item_log;
                break;
            case Mensaje.TYPE_ACTION:
                layout = R.layout.item_action;
                break;
            case Mensaje.TYPE_IMAGE:
                layout = R.layout.item_image;
                break;
        }
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int posicion) {
        Mensaje mensaje = mensajes.get(posicion);

        viewHolder.setNombreUsuario(mensaje.getUsuario(), mensaje.getIsLocal());
        if (mensaje.getFile() == null)
            viewHolder.setMensaje(mensaje.getMensaje());
        else
            viewHolder.setFile(mensaje.getFile());
    }

    @Override
    public int getItemCount() {
        return mensajes.size();
    }

    @Override
    public int getItemViewType(int posicion) {
        return mensajes.get(posicion).getTipo();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView usrView;
        private TextView msjView;
        private Object fileView;

        public ViewHolder(View itemView) {
            super(itemView);

            usrView = (TextView) itemView.findViewById(R.id.username);
            msjView = (TextView) itemView.findViewById(R.id.message);
            fileView = itemView.findViewById(R.id.image_view);
        }

        public void setFile(Object obj)
        {
            ImageView im = (ImageView)fileView;
            im.setImageBitmap((Bitmap) obj);
        }

        public void setNombreUsuario(String nombre_Usuario, boolean isLocal) {
            if (null == usrView) return;

            usrView.setText(isLocal?"yo":nombre_Usuario);
            usrView.setTextColor(getUsrColor(nombre_Usuario));
        }

        public void setMensaje(String mensaje) {
            if (null == msjView) return;
            msjView.setText(mensaje);
        }

        private int getUsrColor(String nombre_Usuario) {
            int hash = 7;
            for (int i = 0, len = nombre_Usuario.length(); i < len; i++) {
                hash = nombre_Usuario.codePointAt(i) + (hash << 5) - hash;
            }
            int idx = Math.abs(hash % usr_Colores.length);
            return usr_Colores[idx];
        }
    }
}
