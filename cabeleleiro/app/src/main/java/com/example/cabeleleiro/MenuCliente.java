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

public class MenuCliente extends AppCompatActivity {

    TextView txtSaudacao;
    Button btSair;
    LinearLayout containerOpcoes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_cliente);

        NotificationHelper.criarCanalNotificacao(this);
        pedirPermissaoDeNotificacaoSeNecessario();

        txtSaudacao = findViewById(R.id.txtSaudacao);
        btSair = findViewById(R.id.btSair);
        containerOpcoes = findViewById(R.id.containerOpcoes);

        String nome = getIntent().getStringExtra("nomeUsuario");
        if (nome != null && !nome.trim().isEmpty()) {
            txtSaudacao.setText("Olá, " + nome.trim() + "!");
        }

        adicionarOpcao("📅", "Agendar Horário", "Escolha o serviço e o melhor horário pra você.",
                () -> startActivity(new Intent(MenuCliente.this, AgendarActivity.class)));

        adicionarOpcao("✂️", "Serviços", "Veja todos os serviços que oferecemos.",
                () -> startActivity(new Intent(MenuCliente.this, VerServicosActivity.class)));

        adicionarOpcao("💇‍♀️", "Profissional", "Conheça quem vai cuidar do seu visual.",
                () -> startActivity(new Intent(MenuCliente.this, ProfissionaisActivity.class)));

        adicionarOpcao("🗓️", "Meus Agendamentos", "Acompanhe seus horários agendados.", () -> {
            Intent tela = new Intent(MenuCliente.this, ListaAgendamentosActivity.class);
            tela.putExtra("modo", "cliente");
            startActivity(tela);
        });

        adicionarOpcao("👤", "Perfil", "Edite seus dados pessoais.",
                () -> startActivity(new Intent(MenuCliente.this, PerfilActivity.class)));

        btSair.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent tela = new Intent(MenuCliente.this, Login.class);
            tela.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(tela);
            finish();
        });
    }

    /** Cria e adiciona um card de opção (ícone + título + descrição) no menu. */
    private void adicionarOpcao(String emoji, String titulo, String descricao, Runnable aoClicar) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_menu_opcao, containerOpcoes, false);

        ((TextView) card.findViewById(R.id.txtIconeOpcao)).setText(emoji);
        ((TextView) card.findViewById(R.id.txtTituloOpcao)).setText(titulo);
        ((TextView) card.findViewById(R.id.txtDescricaoOpcao)).setText(descricao);
        card.setOnClickListener(v -> aoClicar.run());

        containerOpcoes.addView(card);
    }

    /** A partir do Android 13, é preciso pedir permissão explícita pra mostrar notificações. */
    private void pedirPermissaoDeNotificacaoSeNecessario() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }
}
