package com.example.cabeleleiro;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Recebe os avisos push do Firebase Cloud Messaging.
 *
 * Com o app fechado ou em segundo plano, o próprio Android mostra o aviso
 * (esta classe nem chega a rodar). Quando o app está aberto na tela, o Android
 * não mostra sozinho, então o onMessageReceived cria a notificação manualmente.
 */
public class PushService extends FirebaseMessagingService {

    /** O Firebase gerou (ou renovou) o "endereço" deste aparelho. */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        salvarToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage mensagem) {
        super.onMessageReceived(mensagem);

        RemoteMessage.Notification notificacao = mensagem.getNotification();
        if (notificacao == null) {
            return;
        }

        NotificationHelper.criarCanalNotificacao(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return; // sem permissão de notificação, não mostra (e não trava)
        }

        Intent abrir = new Intent(this, Login.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, abrir, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationHelper.CANAL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(notificacao.getTitle())
                .setContentText(notificacao.getBody())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }

    /** Guarda o token deste aparelho no perfil de quem está logada (usuarios/{uid}/fcmToken). */
    public static void salvarToken(String token) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null || token == null) {
            return;
        }
        FirebaseDatabase.getInstance().getReference("usuarios")
                .child(usuario.getUid())
                .child("fcmToken")
                .setValue(token);
    }
}
