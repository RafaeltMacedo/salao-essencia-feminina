package com.example.cabeleleiro;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Executado pelo WorkManager no momento certo, exibe a notificação de lembrete. */
public class LembreteWorker extends Worker {

    public LembreteWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        NotificationHelper.criarCanalNotificacao(context);

        String servico = getInputData().getString("servico");
        String hora = getInputData().getString("hora");

        String titulo = "Lembrete — Essência Feminina";
        String texto = "Você tem horário marcado amanhã às " + hora
                + (servico != null && !servico.isEmpty() ? " (" + servico + ")" : "") + ".";

        Intent intent = new Intent(context, Login.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CANAL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Sem permissão de notificação concedida — não trava o app, só não mostra o aviso.
            return Result.success();
        }

        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
        return Result.success();
    }
}
