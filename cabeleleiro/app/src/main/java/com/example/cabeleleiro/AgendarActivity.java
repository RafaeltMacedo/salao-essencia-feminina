package com.example.cabeleleiro;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tela única para criar OU editar um agendamento.
 * A pessoa escolhe entre os próximos 7 dias (os que o salão realmente
 * funciona) e vê só os horários ainda livres naquele dia.
 */
public class AgendarActivity extends AppCompatActivity {

    private static final int HORA_ABERTURA = 10;   // 10:00
    private static final int HORA_FECHAMENTO = 19;  // 19:00 — nenhum atendimento pode passar disso
    private static final int QTD_DIAS = 7;

    // Paleta da identidade visual (preto e dourado)
    private static final String COR_CARD = "#1C1C1C";
    private static final String COR_BORDA = "#3A3A3A";
    private static final String COR_DOURADO = "#C9A227";
    private static final String COR_FECHADO = "#111111";
    private static final int COR_TEXTO_SECUNDARIO = 0xFFB0A896;
    private static final int COR_DOURADO_INT = 0xFFC9A227;
    private static final int COR_PRETO_INT = 0xFF000000;
    private static final int COR_FECHADO_TEXTO = 0xFF555555;

    private TextView txtTitulo, txtResumoData, txtVazioHorarios;
    private EditText nomeCliente, telefoneCliente;
    private Spinner spnServicos;
    private android.widget.Button btSalvar;
    private LinearLayout containerDias;
    private GridLayout containerHorarios;
    private List<Servico> listaServicos = new ArrayList<>();
    private ArrayAdapter<String> adapterServicos;

    private final ServicoController servicoController = new ServicoController();
    private final ConfiguracaoController configuracaoController = new ConfiguracaoController();
    private final BancoController bancoController = new BancoController(this);

    private final List<String[]> dias = new ArrayList<>(); // cada item: {labelDiaSemana, numeroDia, dataChave, chaveDiaSemana, aberto}
    private final List<String> horariosPossiveis = new ArrayList<>();
    private Map<String, Boolean> configDiasFuncionamento = new HashMap<>();

    private View diaSelecionadoView;
    private View horaSelecionadaView;
    private String dataSelecionada;
    private String horaSelecionada;

    private String agendamentoId; // null = criando novo; preenchido = editando
    private String dataOriginalEdicao, horaOriginalEdicao; // usados só no modo edição
    private int duracaoOriginalEdicao = 40;
    private String clienteUidEdicao; // dono original do agendamento sendo editado

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agendar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.AgendarActivity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtTitulo = findViewById(R.id.txtTitulo);
        txtResumoData = findViewById(R.id.txtResumoData);
        txtVazioHorarios = findViewById(R.id.txtVazioHorarios);
        nomeCliente = findViewById(R.id.txtNome);
        telefoneCliente = findViewById(R.id.txtTelefone);
        spnServicos = findViewById(R.id.spnServicos);
        btSalvar = findViewById(R.id.btSalvar);
        containerDias = findViewById(R.id.containerDias);
        containerHorarios = findViewById(R.id.containerHorarios);

        agendamentoId = getIntent().getStringExtra("agendamentoId");

        carregarServicos();
        gerarHorariosPossiveis();
        carregarDiasFuncionamentoEMontarTela();

        spnServicos.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                // A duração muda conforme o serviço, então a grade de horários precisa ser recalculada
                if (dataSelecionada != null) {
                    carregarHorariosDoDia();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        btSalvar.setOnClickListener(v -> salvar());
    }

    /** Carrega os serviços cadastrados no Firebase pela cabeleireira (tela "Serviços e Preços"). */
    private void carregarServicos() {
        adapterServicos = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        adapterServicos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnServicos.setAdapter(adapterServicos);

        servicoController.semearPadraoSeVazio(new ServicoController.OnResultadoListener() {
            @Override
            public void onSucesso(String mensagem) {
                buscarEExibirServicos();
            }

            @Override
            public void onErro(String erro) {
                buscarEExibirServicos(); // tenta listar mesmo assim
            }
        });
    }

    private void buscarEExibirServicos() {
        servicoController.listarServicos(new ServicoController.OnServicosListener() {
            @Override
            public void onSucesso(List<Servico> lista) {
                listaServicos.clear();
                listaServicos.addAll(lista);

                List<String> nomesExibidos = new ArrayList<>();
                for (Servico servico : listaServicos) {
                    nomesExibidos.add(servico.getTextoExibicao());
                }

                adapterServicos.clear();
                adapterServicos.addAll(nomesExibidos);
                adapterServicos.notifyDataSetChanged();

                if (listaServicos.isEmpty()) {
                    Toast.makeText(AgendarActivity.this,
                            "Nenhum serviço cadastrado ainda. Peça para a cabeleireira cadastrar em 'Serviços e Preços'.",
                            Toast.LENGTH_LONG).show();
                } else {
                    aplicarSelecaoServicoEmEdicao();
                    if (dataSelecionada != null) {
                        carregarHorariosDoDia(); // recalcula a grade agora que já sabemos a duração certa
                    }
                }
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(AgendarActivity.this, "Erro ao carregar serviços: " + erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Em modo edição, seleciona no spinner o serviço que já estava salvo nesse agendamento. */
    private void aplicarSelecaoServicoEmEdicao() {
        if (agendamentoId == null) return;
        String servicoAtual = getIntent().getStringExtra("servico");
        if (servicoAtual == null) return;

        for (int i = 0; i < listaServicos.size(); i++) {
            if (listaServicos.get(i).getTextoExibicao().equals(servicoAtual)) {
                spnServicos.setSelection(i);
                break;
            }
        }
    }

    /** Duração (em minutos) do serviço selecionado agora no spinner, ou 40 min como padrão de segurança. */
    private int obterDuracaoServicoSelecionado() {
        int posicao = spnServicos.getSelectedItemPosition();
        if (posicao >= 0 && posicao < listaServicos.size()) {
            return listaServicos.get(posicao).getDuracaoMinutos();
        }
        return 40;
    }

    /** Gera as marcas candidatas de início, a cada 20 minutos (a granularidade dos blocos de reserva). */
    private void gerarHorariosPossiveis() {
        horariosPossiveis.clear();
        int minutosTotais = HORA_ABERTURA * 60;
        int limiteMinutos = HORA_FECHAMENTO * 60;
        while (minutosTotais < limiteMinutos) {
            horariosPossiveis.add(BancoController.paraTexto(minutosTotais));
            minutosTotais += BancoController.GRANULARIDADE_MINUTOS;
        }
    }

    /** Busca quais dias da semana o salão funciona, depois monta os chips de dia. */
    private void carregarDiasFuncionamentoEMontarTela() {
        configuracaoController.buscarDias(new ConfiguracaoController.OnDiasListener() {
            @Override
            public void onSucesso(Map<String, Boolean> diasConfig) {
                configDiasFuncionamento = diasConfig;
                gerarBotoesDeDias();
                continuarAposMontarDias();
            }

            @Override
            public void onErro(String erro) {
                // Se der erro, assume todos os dias abertos, pra não travar a tela
                configDiasFuncionamento = new HashMap<>();
                gerarBotoesDeDias();
                continuarAposMontarDias();
            }
        });
    }

    private void continuarAposMontarDias() {
        if (agendamentoId != null) {
            preencherModoEdicao();
        } else {
            selecionarPrimeiroDiaAberto();
        }
    }

    /** Cria os "chips" de dia (SEX / HOJE, SAB / 12, ...) para os próximos 7 dias. */
    private void gerarBotoesDeDias() {
        dias.clear();
        containerDias.removeAllViews();

        SimpleDateFormat formatoDiaSemana = new SimpleDateFormat("EEE", new Locale("pt", "BR"));
        SimpleDateFormat formatoDataChave = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        Calendar calendario = Calendar.getInstance();

        for (int i = 0; i < QTD_DIAS; i++) {
            String diaSemanaLabel = i == 0 ? "HOJE" : formatoDiaSemana.format(calendario.getTime()).toUpperCase(Locale.getDefault()).replace(".", "");
            int numeroDia = calendario.get(Calendar.DAY_OF_MONTH);
            String dataChave = formatoDataChave.format(calendario.getTime());

            // Calendar.DAY_OF_WEEK: 1=domingo ... 7=sábado — bate com ConfiguracaoController.CHAVES_DIAS
            int indiceDiaSemana = calendario.get(Calendar.DAY_OF_WEEK) - 1;
            String chaveDiaSemana = ConfiguracaoController.CHAVES_DIAS[indiceDiaSemana];
            boolean aberto = !Boolean.FALSE.equals(configDiasFuncionamento.get(chaveDiaSemana));

            dias.add(new String[]{diaSemanaLabel, String.valueOf(numeroDia), dataChave, chaveDiaSemana, String.valueOf(aberto)});

            View chip = criarChipDia(diaSemanaLabel, numeroDia, i, aberto);
            containerDias.addView(chip);

            calendario.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private View criarChipDia(String diaSemana, int numeroDia, int indice, boolean aberto) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER);
        int paddingH = dpParaPx(14);
        int paddingV = dpParaPx(10);
        chip.setPadding(paddingH, paddingV, paddingH, paddingV);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, dpParaPx(10), 0);
        chip.setLayoutParams(params);

        TextView txtDiaSemana = new TextView(this);
        txtDiaSemana.setTextSize(11);
        txtDiaSemana.setGravity(Gravity.CENTER);

        TextView txtNumero = new TextView(this);
        txtNumero.setText(String.valueOf(numeroDia));
        txtNumero.setTextSize(20);
        txtNumero.setTypeface(null, android.graphics.Typeface.BOLD);
        txtNumero.setGravity(Gravity.CENTER);

        if (aberto) {
            chip.setBackground(criarFundoArredondado(COR_CARD, COR_BORDA));
            txtDiaSemana.setText(diaSemana);
            txtDiaSemana.setTextColor(COR_TEXTO_SECUNDARIO);
            txtNumero.setTextColor(COR_DOURADO_INT);
            chip.setOnClickListener(v -> selecionarDia(indice));
        } else {
            // Dia fechado: visual apagado, não clicável
            chip.setBackground(criarFundoArredondado(COR_FECHADO, COR_FECHADO));
            txtDiaSemana.setText("FECHADO");
            txtDiaSemana.setTextColor(COR_FECHADO_TEXTO);
            txtNumero.setTextColor(COR_FECHADO_TEXTO);
            chip.setOnClickListener(v -> Toast.makeText(this, "O salão não funciona nesse dia.", Toast.LENGTH_SHORT).show());
        }

        chip.addView(txtDiaSemana);
        chip.addView(txtNumero);

        return chip;
    }

    /** Seleciona automaticamente o primeiro dia aberto (geralmente hoje) ao abrir a tela pra criar. */
    private void selecionarPrimeiroDiaAberto() {
        for (int i = 0; i < dias.size(); i++) {
            if (Boolean.parseBoolean(dias.get(i)[4])) {
                selecionarDia(i);
                return;
            }
        }
        // Nenhum dia aberto nos próximos 7 dias — situação rara, mas trata sem travar
        txtResumoData.setText("O salão está fechado nos próximos dias.");
        txtVazioHorarios.setVisibility(View.VISIBLE);
    }

    private void selecionarDia(int indice) {
        String[] diaInfo = dias.get(indice);
        if (!Boolean.parseBoolean(diaInfo[4])) return; // segurança extra: não deixa selecionar dia fechado

        dataSelecionada = diaInfo[2];
        horaSelecionada = null;

        // Realça visualmente o chip escolhido e volta o anterior ao normal
        if (diaSelecionadoView != null) {
            diaSelecionadoView.setBackground(criarFundoArredondado(COR_CARD, COR_BORDA));
            aplicarCorTextos((LinearLayout) diaSelecionadoView, COR_TEXTO_SECUNDARIO, COR_DOURADO_INT);
        }
        View chipEscolhido = containerDias.getChildAt(indice);
        chipEscolhido.setBackground(criarFundoArredondado(COR_DOURADO, COR_DOURADO));
        aplicarCorTextos((LinearLayout) chipEscolhido, COR_PRETO_INT, COR_PRETO_INT);
        diaSelecionadoView = chipEscolhido;

        atualizarResumoData();
        carregarHorariosDoDia();
    }

    private void aplicarCorTextos(LinearLayout chip, int corDiaSemana, int corNumero) {
        ((TextView) chip.getChildAt(0)).setTextColor(corDiaSemana);
        ((TextView) chip.getChildAt(1)).setTextColor(corNumero);
    }

    /**
     * Busca no Firebase quais horários já estão ocupados no dia escolhido — sem
     * expor nome/telefone de ninguém, só a lista de horários já reservados.
     */
    private void carregarHorariosDoDia() {
        containerHorarios.removeAllViews();
        txtVazioHorarios.setVisibility(View.GONE);

        bancoController.listarHorariosOcupados(dataSelecionada, new BancoController.OnHorariosOcupadosListener() {
            @Override
            public void onSucesso(Set<String> ocupados) {
                // Se estivermos editando ESTE agendamento neste MESMO dia, os blocos que ele
                // mesmo já ocupava não contam como "ocupados" (ela pode mantê-lo ou realocar).
                if (agendamentoId != null && dataSelecionada.equals(dataOriginalEdicao)) {
                    for (String bloco : BancoController.gerarBlocos(horaOriginalEdicao, duracaoOriginalEdicao)) {
                        ocupados.remove(bloco);
                    }
                }
                montarGradeHorarios(ocupados);
            }

            @Override
            public void onErro(String erro) {
                Toast.makeText(AgendarActivity.this, "Erro ao carregar horários: " + erro, Toast.LENGTH_LONG).show();
                montarGradeHorarios(new HashSet<>());
            }
        });
    }

    private void montarGradeHorarios(Set<String> ocupados) {
        containerHorarios.removeAllViews();
        horaSelecionadaView = null;

        boolean existeAlgumLivre = false;
        boolean ehHoje = dataSelecionadaEhHoje();
        int minutosAgora = ehHoje ? minutosAtuais() : -1;
        int duracaoServico = obterDuracaoServicoSelecionado();
        int limiteFechamento = HORA_FECHAMENTO * 60;

        for (String hora : horariosPossiveis) {
            if (ehHoje && BancoController.paraMinutos(hora) <= minutosAgora) continue; // já passou, some da grade

            List<String> blocosNecessarios = BancoController.gerarBlocos(hora, duracaoServico);
            String ultimoBloco = blocosNecessarios.get(blocosNecessarios.size() - 1);
            boolean caberiaAntesDoFechamento = BancoController.paraMinutos(ultimoBloco) + BancoController.GRANULARIDADE_MINUTOS <= limiteFechamento;
            if (!caberiaAntesDoFechamento) continue; // o serviço não terminaria a tempo, nem mostra

            boolean algumBlocoOcupado = false;
            for (String bloco : blocosNecessarios) {
                if (ocupados.contains(bloco)) {
                    algumBlocoOcupado = true;
                    break;
                }
            }
            if (algumBlocoOcupado) continue; // não cabe inteiro sem esbarrar em outro agendamento

            existeAlgumLivre = true;
            View slot = criarSlotHorario(hora);
            containerHorarios.addView(slot);

            if (hora.equals(horaSelecionada)) {
                marcarHorarioSelecionado(slot);
            }
        }

        txtVazioHorarios.setVisibility(existeAlgumLivre ? View.GONE : View.VISIBLE);
    }

    /** Compara a data escolhida com a data de hoje (mesmo formato dd/MM/yyyy). */
    private boolean dataSelecionadaEhHoje() {
        if (dataSelecionada == null) return false;
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String hojeFormatado = formato.format(Calendar.getInstance().getTime());
        return dataSelecionada.equals(hojeFormatado);
    }

    private int minutosAtuais() {
        Calendar agora = Calendar.getInstance();
        return agora.get(Calendar.HOUR_OF_DAY) * 60 + agora.get(Calendar.MINUTE);
    }

    private View criarSlotHorario(String hora) {
        TextView slot = new TextView(this);
        slot.setText(hora);
        slot.setTextColor(COR_DOURADO_INT);
        slot.setTextSize(15);
        slot.setGravity(Gravity.CENTER);
        slot.setBackground(criarFundoArredondado(COR_CARD, COR_BORDA));

        int margem = dpParaPx(6);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dpParaPx(48);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(margem, margem, margem, margem);
        slot.setLayoutParams(params);

        slot.setOnClickListener(v -> {
            horaSelecionada = hora;
            if (horaSelecionadaView != null) {
                horaSelecionadaView.setBackground(criarFundoArredondado(COR_CARD, COR_BORDA));
                ((TextView) horaSelecionadaView).setTextColor(COR_DOURADO_INT);
            }
            marcarHorarioSelecionado(slot);
            atualizarResumoData();
        });

        return slot;
    }

    private void marcarHorarioSelecionado(View slot) {
        slot.setBackground(criarFundoArredondado(COR_DOURADO, COR_DOURADO));
        ((TextView) slot).setTextColor(COR_PRETO_INT);
        horaSelecionadaView = slot;
    }

    private void atualizarResumoData() {
        if (dataSelecionada == null) {
            txtResumoData.setText("Escolha um dia e horário acima");
            return;
        }
        String resumo = dataSelecionada + (horaSelecionada != null ? " às " + horaSelecionada : " -- selecione um horário");
        txtResumoData.setText(resumo);
    }

    /** Pré-preenche os campos quando a tela é aberta em modo edição. */
    private void preencherModoEdicao() {
        txtTitulo.setText("Editar Agendamento");
        btSalvar.setText("Salvar Alterações");

        nomeCliente.setText(getIntent().getStringExtra("nomeCliente"));
        telefoneCliente.setText(getIntent().getStringExtra("telefoneCliente"));
        // O serviço em si é selecionado em aplicarSelecaoServicoEmEdicao(),
        // chamado depois que a lista de serviços termina de carregar do Firebase.

        dataOriginalEdicao = getIntent().getStringExtra("data");
        horaOriginalEdicao = getIntent().getStringExtra("hora");
        duracaoOriginalEdicao = getIntent().getIntExtra("duracaoMinutos", 40);
        clienteUidEdicao = getIntent().getStringExtra("clienteUid");
        horaSelecionada = horaOriginalEdicao;

        // Tenta encontrar essa data entre os 7 dias mostrados; se achar, seleciona automaticamente
        int indiceEncontrado = -1;
        for (int i = 0; i < dias.size(); i++) {
            if (dias.get(i)[2].equals(dataOriginalEdicao)) {
                indiceEncontrado = i;
                break;
            }
        }

        if (indiceEncontrado >= 0 && Boolean.parseBoolean(dias.get(indiceEncontrado)[4])) {
            selecionarDia(indiceEncontrado);
        } else {
            // Data original está fora da janela de 7 dias (ou caiu num dia fechado) —
            // a pessoa escolhe um novo dia/horário
            dataSelecionada = dataOriginalEdicao;
            Toast.makeText(this, "Escolha um novo dia e horário para reagendar.", Toast.LENGTH_LONG).show();
            selecionarPrimeiroDiaAberto();
        }
    }

    private void salvar() {
        String nome = nomeCliente.getText().toString().trim();
        String telefone = telefoneCliente.getText().toString().trim();
        String servico = spnServicos.getSelectedItem() != null ? spnServicos.getSelectedItem().toString() : "";

        if (nome.isEmpty()) {
            Toast.makeText(this, "Preencha o nome antes de salvar!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dataSelecionada == null || horaSelecionada == null) {
            Toast.makeText(this, "Escolha um dia e um horário disponível!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser usuarioAtual = FirebaseAuth.getInstance().getCurrentUser();
        String uidAtual = usuarioAtual != null ? usuarioAtual.getUid() : "";
        int duracaoSelecionada = obterDuracaoServicoSelecionado();

        btSalvar.setEnabled(false);

        if (agendamentoId != null) {
            bancoController.atualizarDados(agendamentoId, nome, telefone, servico,
                    dataOriginalEdicao, horaOriginalEdicao, duracaoOriginalEdicao,
                    dataSelecionada, horaSelecionada, duracaoSelecionada, clienteUidEdicao,
                    new BancoController.OnResultadoListener() {
                        @Override
                        public void onSucesso(String mensagem, String idAgendamento) {
                            Toast.makeText(AgendarActivity.this, mensagem, Toast.LENGTH_LONG).show();
                            NotificationHelper.agendarLembrete(AgendarActivity.this, idAgendamento,
                                    dataSelecionada, horaSelecionada, servico);
                            finish();
                        }

                        @Override
                        public void onErro(String erro) {
                            btSalvar.setEnabled(true);
                            Toast.makeText(AgendarActivity.this, erro, Toast.LENGTH_LONG).show();
                            carregarHorariosDoDia(); // recarrega, caso o horário tenha sido ocupado nesse meio tempo
                        }
                    });
        } else {
            bancoController.insereDados(uidAtual, nome, telefone, servico, dataSelecionada, horaSelecionada,
                    duracaoSelecionada,
                    new BancoController.OnResultadoListener() {
                        @Override
                        public void onSucesso(String mensagem, String idAgendamento) {
                            Toast.makeText(AgendarActivity.this, mensagem, Toast.LENGTH_LONG).show();
                            NotificationHelper.notificarConfirmacaoImediata(AgendarActivity.this,
                                    dataSelecionada, horaSelecionada, servico);
                            NotificationHelper.agendarLembrete(AgendarActivity.this, idAgendamento,
                                    dataSelecionada, horaSelecionada, servico);
                            limpar();
                            btSalvar.setEnabled(true);
                        }

                        @Override
                        public void onErro(String erro) {
                            btSalvar.setEnabled(true);
                            Toast.makeText(AgendarActivity.this, erro, Toast.LENGTH_LONG).show();
                            carregarHorariosDoDia(); // o horário pode ter sido ocupado por outra pessoa nesse meio tempo
                        }
                    });
        }
    }

    private void limpar() {
        nomeCliente.setText("");
        telefoneCliente.setText("");
        spnServicos.setSelection(0);
        horaSelecionada = null;
        carregarHorariosDoDia();
        atualizarResumoData();
    }

    /** Cria um fundo retangular arredondado (usado nos chips de dia e horário). */
    private GradientDrawable criarFundoArredondado(String corFundo, String corBorda) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(android.graphics.Color.parseColor(corFundo));
        drawable.setStroke(dpParaPx(1), android.graphics.Color.parseColor(corBorda));
        drawable.setCornerRadius(dpParaPx(12));
        return drawable;
    }

    private int dpParaPx(int dp) {
        float densidade = getResources().getDisplayMetrics().density;
        return Math.round(dp * densidade);
    }
}
