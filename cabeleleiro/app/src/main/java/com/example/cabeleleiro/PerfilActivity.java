package com.example.cabeleleiro;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Tela onde a pessoa (cliente ou cabeleireira) edita seu próprio nome e telefone. */
public class PerfilActivity extends AppCompatActivity {

    private final UsuarioController usuarioController = new UsuarioController();
    private EditText txtNome, txtTelefone, txtEmail;
    private TextView txtTipoUsuario;
    private Button btSalvar;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        txtNome = findViewById(R.id.txtNome);
        txtTelefone = findViewById(R.id.txtTelefone);
        txtEmail = findViewById(R.id.txtEmail);
        txtTipoUsuario = findViewById(R.id.txtTipoUsuario);
        btSalvar = findViewById(R.id.btSalvar);

        FirebaseUser usuarioAtual = FirebaseAuth.getInstance().getCurrentUser();
        if (usuarioAtual == null) {
            finish();
            return;
        }
        uid = usuarioAtual.getUid();

        carregarPerfil();

        btSalvar.setOnClickListener(v -> salvar());
    }

    private void carregarPerfil() {
        usuarioController.buscarPerfil(uid, new UsuarioController.OnUsuarioListener() {
            @Override
            public void onSucesso(Usuario usuario) {
                txtNome.setText(usuario.getNome());
                txtTelefone.setText(usuario.getTelefone());
                txtEmail.setText(usuario.getEmail());
                txtTipoUsuario.setText(usuario.isCabeleireira() ? "Cabeleireira" : "Cliente");
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(PerfilActivity.this, "Erro ao carregar perfil: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void salvar() {
        String nome = txtNome.getText().toString().trim();
        String telefone = txtTelefone.getText().toString().trim();

        if (nome.isEmpty()) {
            Toast.makeText(this, "O nome não pode ficar em branco!", Toast.LENGTH_SHORT).show();
            return;
        }

        btSalvar.setEnabled(false);
        usuarioController.atualizarNomeTelefone(uid, nome, telefone, new UsuarioController.OnResultadoListener() {
            @Override
            public void onSucesso() {
                Toast.makeText(PerfilActivity.this, "Perfil atualizado com sucesso!", Toast.LENGTH_LONG).show();
                btSalvar.setEnabled(true);
            }

            @Override
            public void onErro(String erro) {
                btSalvar.setEnabled(true);
                Toast.makeText(PerfilActivity.this, erro, Toast.LENGTH_LONG).show();
            }
        });
    }
}
