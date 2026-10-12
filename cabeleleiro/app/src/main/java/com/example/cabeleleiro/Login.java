package com.example.cabeleleiro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity implements View.OnClickListener {
    Button btEntrar;
    EditText txtLogEmail, txtLogSenha;
    TextView txtCadastroLink, txtEsqueciSenha;

    private FirebaseAuth mAuth;
    private final UsuarioController usuarioController = new UsuarioController();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        btEntrar = findViewById(R.id.btEntrar);
        txtLogEmail = findViewById(R.id.txtLogEmail);
        txtLogSenha = findViewById(R.id.txtLogSenha);
        txtCadastroLink = findViewById(R.id.txtCadastroLink);
        txtEsqueciSenha = findViewById(R.id.txtEsqueciSenha);

        btEntrar.setOnClickListener(this);
        txtCadastroLink.setOnClickListener(this);
        txtEsqueciSenha.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Se já existe uma sessão válida, pula o Login direto para a tela certa
        FirebaseUser usuarioAtual = mAuth.getCurrentUser();
        if (usuarioAtual != null) {
            redirecionarConformePerfil(usuarioAtual.getUid());
        }
    }

    @Override
    public void onClick(View v) {
        String msg = "";

        if (v.getId() == R.id.btEntrar) {
            String emailInserido = txtLogEmail.getText().toString().trim();
            String senhaInserida = txtLogSenha.getText().toString().trim();

            if (emailInserido.isEmpty()) {
                msg = "O Campo E-mail deve ser preenchido!";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                return;
            }

            if (senhaInserida.isEmpty()) {
                msg = "O Campo SENHA deve ser preenchido!";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                return;
            }

            btEntrar.setEnabled(false);

            // LOGIN NA NUVEM: Conecta ao Firebase para validar as credenciais
            mAuth.signInWithEmailAndPassword(emailInserido, senhaInserida)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                                redirecionarConformePerfil(mAuth.getCurrentUser().getUid());
                            } else {
                                btEntrar.setEnabled(true);
                                Toast.makeText(Login.this, "Erro ao entrar: E-mail/Senha incorretos ou não cadastrados.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

        } else if (v.getId() == R.id.txtCadastroLink) {
            Intent telaCadastre_se = new Intent(this, Cadastre_se.class);
            startActivity(telaCadastre_se);

        } else if (v.getId() == R.id.txtEsqueciSenha) {
            abrirDialogRecuperarSenha();
        }
    }

    /** Mostra um diálogo simples pedindo o e-mail e envia o link de redefinição pelo Firebase. */
    private void abrirDialogRecuperarSenha() {
        EditText campoEmail = new EditText(this);
        campoEmail.setHint("Digite seu e-mail cadastrado");
        campoEmail.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        campoEmail.setText(txtLogEmail.getText().toString().trim()); // já preenche se a pessoa tiver digitado

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        campoEmail.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Recuperar senha")
                .setMessage("Informe o e-mail usado no cadastro. Vamos enviar um link pra você criar uma senha nova.")
                .setView(campoEmail)
                .setPositiveButton("Enviar", (dialog, which) -> {
                    String email = campoEmail.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(Login.this, "Digite um e-mail válido.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(Login.this,
                                    "E-mail enviado! Verifique sua caixa de entrada (e o spam) pra redefinir a senha.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(Login.this,
                                    "Não foi possível enviar: verifique se o e-mail está correto e cadastrado.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** Busca o tipo de perfil (cliente/cabeleireira) e abre a tela correspondente. */
    private void redirecionarConformePerfil(String uid) {
        usuarioController.buscarPerfil(uid, new UsuarioController.OnUsuarioListener() {
            @Override
            public void onSucesso(Usuario usuario) {
                Intent tela;
                if (usuario.isCabeleireira()) {
                    tela = new Intent(Login.this, MenuCabeleireira.class);
                } else {
                    tela = new Intent(Login.this, MenuCliente.class);
                }
                tela.putExtra("nomeUsuario", usuario.getNome());
                startActivity(tela);
                finish();
            }

            @Override
            public void onErro(String erro) {
                btEntrar.setEnabled(true);
                Toast.makeText(Login.this, "Não foi possível carregar seu perfil: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }
}
