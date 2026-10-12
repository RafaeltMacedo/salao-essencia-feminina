package com.example.cabeleleiro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Fica "de olho" nos agendamentos em tempo real (usando o próprio Firebase
 * Realtime Database, sem precisar de servidor nem custo) e avisa a
 * cabeleireira na hora que uma cliente marca um horário novo.
 *
 * Diferença pra um push de verdade: só funciona enquanto esse serviço
 * estiver rodando (ou seja, enquanto o Android não "matar" o app à força).
 * Não precisa de Cloud Functions nem de cartão de crédito no Firebase.
 */
public class NovoAgendamentoService extends Service {

    private static final String CANAL_MONITORAMENTO = "monitoramento_agendamentos";
    private static final int ID_NOTIFICACAO_SERVICO = 501;

    private DatabaseReference referencia;
    private ChildEventListener listener;
    private long momentoInicioServico;

    @Override
    public void onCreate() {
        super.onCreate();
        momentoInicioServico = System.currentTimeMillis();
        NotificationHelper.criarCanalNotificacao(this);
        criarCanalMonitoramento();
        iniciarComoForeground();
        observarNovosAgendamentos();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY: se o Android precisar liberar memória e matar o serviço,
        // ele tenta recriar sozinho assim que possível.
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (referencia != null && listener != null) {
            referencia.removeEventListener(listener);
        }
    }

    private void criarCanalMonitoramento() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_MONITORAMENTO, "Monitoramento (segundo plano)", NotificationManager.IMPORTANCE_MIN);
            canal.setDescription("Notificação discreta que mantém o aviso de novos agendamentos ativo.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(canal);
            }
        }
    }

    /** Notificação de baixa prioridade, exigida pelo Android pra manter um serviço em primeiro plano. */
    private void iniciarComoForeground() {
        Intent intent = new Intent(this, Login.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notificacao = new NotificationCompat.Builder(this, CANAL_MONITORAMENTO)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Essência Feminina")
                .setContentText("Avisando sobre novos agendamentos em tempo real.")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(ID_NOTIFICACAO_SERVICO, notificacao,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(ID_NOTIFICACAO_SERVICO, notificacao);
        }
    }

    private void observarNovosAgendamentos() {
        referencia = FirebaseDatabase.getInstance().getReference().child("agendamentos");

        listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@androidx.annotation.NonNull DataSnapshot snapshot, String previousChildName) {
                Agendamento agendamento = snapshot.getValue(Agendamento.class);
                if (agendamento == null) return;

                // Só avisa sobre agendamentos criados DEPOIS do serviço ligar —
                // sem isso, ele avisaria sobre todo mundo já cadastrado antes.
                if (agendamento.getCriadoEm() >= momentoInicioServico) {
                    notificarNovoAgendamento(agendamento);
                }
            }

            @Override
            public void onChildChanged(@androidx.annotation.NonNull DataSnapshot snapshot, String previousChildName) {
            }

            @Override
            public void onChildRemoved(@androidx.annotation.NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@androidx.annotation.NonNull DataSnapshot snapshot, String previousChildName) {
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
            }
        };

        referencia.addChildEventListener(listener);
    }

    private void notificarNovoAgendamento(Agendamento agendamento) {
        String titulo = "Novo agendamento!";
        String texto = agendamento.getNomeCliente() + " marcou " + agendamento.getServico()
                + " para " + agendamento.getData() + " às " + agendamento.getHora() + ".";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationHelper.CANAL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return; // sem permissão concedida, não trava — só não mostra
        }

        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }
}
