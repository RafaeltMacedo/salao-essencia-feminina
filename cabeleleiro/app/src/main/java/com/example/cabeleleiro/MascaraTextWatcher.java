package com.example.cabeleleiro;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Aplica uma máscara simples enquanto o usuário digita apenas números,
 * inserindo separadores nas posições certas (ex: 12092026 -> 12/09/2026).
 * Evita que a pessoa precise digitar "/" ou ":" manualmente — e evita
 * que esqueça e mande a data toda junta, como "11092026".
 */
public class MascaraTextWatcher implements TextWatcher {

    /** Máscara pronta para datas: DD/MM/AAAA */
    public static void aplicarMascaraData(EditText campo) {
        campo.addTextChangedListener(new MascaraTextWatcher(campo, new int[]{2, 4}, '/', 8));
    }

    /** Máscara pronta para horários: HH:MM */
    public static void aplicarMascaraHora(EditText campo) {
        campo.addTextChangedListener(new MascaraTextWatcher(campo, new int[]{2}, ':', 4));
    }

    private final EditText campo;
    private final int[] posicoesSeparador; // posições (em dígitos) onde entra o separador
    private final char separador;
    private final int maxDigitos;
    private boolean autoAjustando = false;

    private MascaraTextWatcher(EditText campo, int[] posicoesSeparador, char separador, int maxDigitos) {
        this.campo = campo;
        this.posicoesSeparador = posicoesSeparador;
        this.separador = separador;
        this.maxDigitos = maxDigitos;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable texto) {
        if (autoAjustando) {
            return;
        }
        autoAjustando = true;

        // Pega só os dígitos que o usuário digitou, ignorando o que já era separador
        StringBuilder apenasDigitos = new StringBuilder();
        for (char c : texto.toString().toCharArray()) {
            if (Character.isDigit(c) && apenasDigitos.length() < maxDigitos) {
                apenasDigitos.append(c);
            }
        }

        // Remonta o texto já com os separadores nas posições certas
        StringBuilder textoFormatado = new StringBuilder();
        for (int i = 0; i < apenasDigitos.length(); i++) {
            textoFormatado.append(apenasDigitos.charAt(i));
            for (int posicao : posicoesSeparador) {
                if (i + 1 == posicao && i + 1 != apenasDigitos.length()) {
                    textoFormatado.append(separador);
                }
            }
        }

        campo.setText(textoFormatado.toString());
        campo.setSelection(textoFormatado.length());

        autoAjustando = false;
    }
}
