package com.example.cabeleleiro;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Agenda um lembrete local (sem precisar de servidor) 24h antes do horário
 * marcado. Usa WorkManager, que sobrevive até a reinicialização do celular.
 */
public class NotificationHelper {

    public static final String CANAL_ID = "lembretes_agendamento";

    /** Cria o canal de notificação (obrigatório a partir do Android 8). Chamar uma vez ao abrir o app. */
    public static void criarCanalNotificacao(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID, "Lembretes de Agendamento", NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription("Avisa a cliente um dia antes do horário marcado.");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(canal);
            }
        }
    }

    /** Mostra uma notificação imediata confirmando que o agendamento foi feito. */
    public static void notificarConfirmacaoImediata(Context context, String data, String hora, String servico) {
        criarCanalNotificacao(context);

        String titulo = "Agendamento confirmado!";
        String texto = "Seu horário para " + data + " às " + hora
                + (servico != null && !servico.isEmpty() ? " (" + servico + ")" : "") + " foi marcado com sucesso.";

        androidx.core.app.NotificationCompat.Builder builder =
                new androidx.core.app.NotificationCompat.Builder(context, CANAL_ID)
                        .setSmallIcon(android.R.drawable.ic_popup_reminder)
                        .setContentTitle(titulo)
                        .setContentText(texto)
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return; // sem permissão concedida, não trava — só não mostra
        }

        androidx.core.app.NotificationManagerCompat.from(context)
                .notify((int) System.currentTimeMillis(), builder.build());
    }

    /**
     * Agenda (ou reagenda, se já existir um pra esse mesmo agendamento) um
     * lembrete 24h antes do horário marcado.
     */
    public static void agendarLembrete(Context context, String agendamentoId, String data, String hora,
                                        String servico) {
        if (agendamentoId == null || data == null || hora == null) return;

        long momentoAgendamento = converterParaEpoch(data, hora);
        if (momentoAgendamento <= 0) return;

        long momentoLembrete = momentoAgendamento - TimeUnit.HOURS.toMillis(24);
        long atraso = momentoLembrete - System.currentTimeMillis();

        if (atraso <= 0) {
            // Já está a menos de 24h do horário — não faz sentido mandar um lembrete "de véspera".
            return;
        }

        Data dadosEntrada = new Data.Builder()
                .putString("servico", servico)
                .putString("hora", hora)
                .build();

        OneTimeWorkRequest pedido = new OneTimeWorkRequest.Builder(LembreteWorker.class)
                .setInitialDelay(atraso, TimeUnit.MILLISECONDS)
                .setInputData(dadosEntrada)
                .build();

        // ExistingWorkPolicy.REPLACE garante que, se a cliente editar o agendamento
        // (mudar o horário), o lembrete antigo é substituído pelo novo automaticamente.
        WorkManager.getInstance(context)
                .enqueueUniqueWork(nomeUnico(agendamentoId), ExistingWorkPolicy.REPLACE, pedido);
    }

    /** Cancela o lembrete de um agendamento (chamado ao excluir/cancelar um agendamento). */
    public static void cancelarLembrete(Context context, String agendamentoId) {
        if (agendamentoId == null) return;
        WorkManager.getInstance(context).cancelUniqueWork(nomeUnico(agendamentoId));
    }

    private static String nomeUnico(String agendamentoId) {
        return "lembrete_" + agendamentoId;
    }

    private static long converterParaEpoch(String data, String hora) {
        try {
            SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date dataHora = formato.parse(data + " " + hora);
            return dataHora != null ? dataHora.getTime() : -1;
        } catch (ParseException e) {
            return -1;
        }
    }
}
