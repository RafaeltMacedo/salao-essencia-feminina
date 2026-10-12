package com.example.cabeleleiro;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class MenuCabeleireira extends AppCompatActivity {

    TextView txtSaudacao;
    Button btSair;
    LinearLayout containerOpcoes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_cabeleireira);

        NotificationHelper.criarCanalNotificacao(this);
        pedirPermissaoDeNotificacaoSeNecessario();
        // O aviso de novo agendamento agora vem por push (Firebase Cloud Messaging),
        // então o serviço antigo, que ficava ligado o tempo todo, não é mais usado.
        stopService(new Intent(this, NovoAgendamentoService.class));
        registrarAparelhoParaAvisos();

        txtSaudacao = findViewById(R.id.txtSaudacao);
        btSair = findViewById(R.id.btSair);
        containerOpcoes = findViewById(R.id.containerOpcoes);

        String nome = getIntent().getStringExtra("nomeUsuario");
        if (nome != null && !nome.trim().isEmpty()) {
            txtSaudacao.setText("Olá, " + nome.trim() + "!");
        }

        adicionarOpcao("📋", "Agenda do Salão", "Veja e gerencie todos os agendamentos.", () -> {
            Intent tela = new Intent(MenuCabeleireira.this, ListaAgendamentosActivity.class);
            tela.putExtra("modo", "cabeleireira");
            startActivity(tela);
        });

        adicionarOpcao("➕", "Agendar Cliente", "Marque um horário de encaixe.",
                () -> startActivity(new Intent(MenuCabeleireira.this, AgendarActivity.class)));

        adicionarOpcao("💇", "Serviços e Preços", "Cadastre e edite os serviços do salão.",
                () -> startActivity(new Intent(MenuCabeleireira.this, GerenciarServicosActivity.class)));

        adicionarOpcao("🗓️", "Dias de Funcionamento", "Escolha em quais dias o salão abre.",
                () -> startActivity(new Intent(MenuCabeleireira.this, DiasFuncionamentoActivity.class)));

        adicionarOpcao("👤", "Perfil", "Edite seus dados pessoais.",
                () -> startActivity(new Intent(MenuCabeleireira.this, PerfilActivity.class)));

        btSair.setOnClickListener(v -> {
            // Ao sair, este aparelho deixa de receber os avisos da cabeleireira
            FirebaseMessaging.getInstance().deleteToken();
            FirebaseAuth.getInstance().signOut();
            Intent tela = new Intent(MenuCabeleireira.this, Login.class);
            tela.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(tela);
            finish();
        });
    }

    /** Pega o "endereço" deste aparelho no Firebase Cloud Messaging e guarda no perfil dela. */
    private void registrarAparelhoParaAvisos() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(tarefa -> {
            if (tarefa.isSuccessful() && tarefa.getResult() != null) {
                PushService.salvarToken(tarefa.getResult());
            }
        });
    }

    /** A partir do Android 13, é preciso pedir permissão explícita pra mostrar notificações. */
    private void pedirPermissaoDeNotificacaoSeNecessario() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private void adicionarOpcao(String emoji, String titulo, String descricao, Runnable aoClicar) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_menu_opcao, containerOpcoes, false);

        ((TextView) card.findViewById(R.id.txtIconeOpcao)).setText(emoji);
        ((TextView) card.findViewById(R.id.txtTituloOpcao)).setText(titulo);
        ((TextView) card.findViewById(R.id.txtDescricaoOpcao)).setText(descricao);
        card.setOnClickListener(v -> aoClicar.run());

        containerOpcoes.addView(card);
    }
}
