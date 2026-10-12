package com.example.cabeleleiro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Cadastre_se extends AppCompatActivity implements View.OnClickListener {
    Button btCadGravar;
    EditText txtCadNome, txtCadTelefone, txtCadEmail;
    EditText txtCadSenha, txtCadConfSenha;

    private FirebaseAuth mAuth;
    private final UsuarioController usuarioController = new UsuarioController();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastre_se);

        mAuth = FirebaseAuth.getInstance();

        btCadGravar = findViewById(R.id.btCadGravar);
        txtCadNome  = findViewById(R.id.txtCadNome);
        txtCadTelefone = findViewById(R.id.txtCadTelefone);
        txtCadEmail = findViewById(R.id.txtCadEmail);
        txtCadSenha = findViewById(R.id.txtCadSenha);
        txtCadConfSenha = findViewById(R.id.txtCadConfSenha);

        btCadGravar.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (!ValidaDados()) return;

        btCadGravar.setEnabled(false);

        String nome = txtCadNome.getText().toString().trim();
        String telefone = txtCadTelefone.getText().toString().trim();
        String email = txtCadEmail.getText().toString().trim();
        String senha = txtCadSenha.getText().toString().trim();
        // Cadastro público sempre cria conta de CLIENTE. A conta de cabeleireira
        // não pode ser criada por autocadastro — precisa ser definida manualmente
        // pela própria dona do salão (via Firebase Console), evitando que
        // qualquer pessoa vire "administradora" do app só escolhendo uma opção.
        String tipo = Usuario.TIPO_CLIENTE;

        // CADASTRO REAL NA NUVEM: Cria o usuário no Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            String erro = task.getException() != null ? task.getException().getMessage() : "Erro desconhecido";
                            Toast.makeText(Cadastre_se.this, "Erro ao cadastrar: " + erro, Toast.LENGTH_LONG).show();
                            btCadGravar.setEnabled(true);
                            return;
                        }

                        FirebaseUser usuarioAuth = mAuth.getCurrentUser();
                        if (usuarioAuth == null) {
                            Toast.makeText(Cadastre_se.this, "Erro inesperado ao criar conta.", Toast.LENGTH_LONG).show();
                            btCadGravar.setEnabled(true);
                            return;
                        }

                        // Depois de criar o login, salva o perfil (nome, telefone, tipo) no banco
                        Usuario usuario = new Usuario(nome, telefone, email, tipo);
                        usuarioController.salvarPerfil(usuarioAuth.getUid(), usuario, new UsuarioController.OnResultadoListener() {
                            @Override
                            public void onSucesso() {
                                Toast.makeText(Cadastre_se.this, "Cadastro realizado com sucesso!", Toast.LENGTH_LONG).show();
                                finish();
                            }

                            @Override
                            public void onErro(String erro) {
                                Toast.makeText(Cadastre_se.this, "Conta criada, mas houve um erro ao salvar o perfil: " + erro, Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }
                });
    }

    public boolean ValidaDados() {
        String msg = "";
        if (txtCadNome.getText().length()==0){
            msg = "Preencha o campo Nome Completo corretamente!";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return false;
        }
        if (txtCadTelefone.getText().length()==0){
            msg = "Preencha o campo Telefone corretamente!";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return false;
        }
        if (txtCadEmail.getText().length()==0){
            msg = "Preencha o campo E-mail corretamente!";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return false;
        }
        if (txtCadSenha.getText().length()==0){
            msg = "Preencha o campo Senha corretamente!";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return false;
        }
        if (txtCadConfSenha.getText().length()==0){
            msg = "Preencha o campo Confirma Senha corretamente!";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return false;
        }

        // Regra do Firebase: A senha precisa ter pelo menos 6 caracteres
        if (txtCadSenha.getText().length() < 6) {
            msg = "A senha deve ter no mínimo 6 caracteres!";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return false;
        }

        if (!txtCadSenha.getText().toString().equals(txtCadConfSenha.getText().toString())){
            msg = "O campo e Senha e Confirma Senha não estão iguais!";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }
}
