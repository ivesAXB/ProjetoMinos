package com.mygame;

import java.util.Random;

/**
 * Representa um "Indivíduo" no nosso Algoritmo Genético.
 * Este objeto contém um "genoma" (o mapa do labirinto) e a sua "pontuação" (fitness).
 * É o "Aluno" que o nosso Pathfinder (Professor) vai avaliar.
 */
public class MapaGenetico {

    // --- 1. O Genoma (O DNA) ---
    
    // O mapa em si. 'X' = Parede, 'O' = Caminho
    public char[][] grelha; 
    public final int largura;
    public final int altura;

    // --- 2. A Pontuação ---
    
    // A "nota" que o Pathfinder (Professor) dá a este mapa.
    // Quanto maior, melhor.
    public int fitness = 0;

    // Um gerador de números aleatórios para mutações
    private static final Random rand = new Random();

    /**
     * Construtor Principal: Cria um novo MapaGenetico (um "Aluno").
     * @param largura A largura do mapa a ser gerado.
     * @param altura A altura do mapa a ser gerado.
     */
    public MapaGenetico(int largura, int altura) {
        this.largura = largura;
        this.altura = altura;
        this.grelha = new char[altura][largura]; // [Z][X] ou [Linha][Coluna]
    }

    /**
     * Inicializa a Geração 0.
     * Preenche a grelha com "genes" (X e O) 100% aleatórios.
     * Isto cria o "lixo" inicial a partir do qual vamos evoluir.
     * * @param chanceDeParede A probabilidade (0.0 a 1.0) de um bloco ser 'X'.
     */
    public void inicializarAleatoriamente(float chanceDeParede) {
        for (int z = 0; z < altura; z++) {
            for (int x = 0; x < largura; x++) {
                
                // Força as BORDAS a serem sempre paredes
                if (z == 0 || z == altura - 1 || x == 0 || x == largura - 1) {
                    grelha[z][x] = 'X';
                } 
                // Para o resto, decide aleatoriamente
                else {
                    if (rand.nextFloat() < chanceDeParede) {
                        grelha[z][x] = 'X'; // Parede
                    } else {
                        grelha[z][x] = 'O'; // Caminho
                    }
                }
            }
        }
    }

    /**
     * Um "ajudante" que converte a nossa grelha (char[][]) 
     * para o formato que o Pathfinder entende (String[]).
     * @return O mapa no formato String[].
     */
    public String[] paraStringArray() {
        String[] mapaStrings = new String[altura];
        for (int z = 0; z < altura; z++) {
            mapaStrings[z] = new String(grelha[z]);
        }
        return mapaStrings;
    }
    
    // --- 3. Métodos Genéticos (Crossover e Mutação) ---
    // (Vamos implementar isto no próximo passo)
    
}