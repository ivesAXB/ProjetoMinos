package com.mygame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * O "Motor" da nossa IA "Arquiteto".
 * Esta classe gere uma "população" de MapasGenéticos e usa um algoritmo
 * genético (seleção, crossover, mutação) para os "evoluir"
 * e criar labirintos cada vez melhores.
 */
public class AlgoritmoGenetico {

    // --- 1. Parâmetros da Evolução ---
    
    private final int tamanhoPopulacao; // Quantos mapas em cada geração (ex: 100)
    private final float taxaMutacao;      // Chance de um "gene" (bloco) mudar (ex: 1%)
    private final float taxaCrossover;    // Chance de dois pais "cruzarem" (ex: 70%)
    private final int numElite;           // Quantos "melhores" mapas sobrevivem automaticamente
    
    private final int larguraMapa;
    private final int alturaMapa;
    
    private static final Random rand = new Random();

    // A lista de "alunos" da geração atual
    private List<MapaGenetico> populacao;

    /**
     * Construtor: Prepara o motor de IA.
     */
    public AlgoritmoGenetico(int tamanhoPopulacao, float taxaMutacao, float taxaCrossover, int numElite, int larguraMapa, int alturaMapa) {
        this.tamanhoPopulacao = tamanhoPopulacao;
        this.taxaMutacao = taxaMutacao;
        this.taxaCrossover = taxaCrossover;
        this.numElite = numElite;
        this.larguraMapa = larguraMapa;
        this.alturaMapa = alturaMapa;

        this.populacao = new ArrayList<>(tamanhoPopulacao);
    }

    /**
     * Cria a "Geração 0", a população inicial de mapas 100% aleatórios.
     */
    public void inicializarPopulacao() {
        System.out.println("IA: Criando Geração 0 (mapas aleatórios)...");
        for (int i = 0; i < tamanhoPopulacao; i++) {
            MapaGenetico mapa = new MapaGenetico(larguraMapa, alturaMapa);
            // 40% de chance de ser uma parede (podes ajustar isto!)
            mapa.inicializarAleatoriamente(0.4f); 
            populacao.add(mapa);
        }
    }

    /**
     * O "Professor": Avalia cada mapa na população e dá-lhe uma "nota" (fitness).
     * Este é o "gargalo" (parte mais lenta) do processo.
     */
    public void calcularFitnessPopulacao() {
        
        for (MapaGenetico mapa : populacao) {
            
            // 1. Converte o "DNA" (char[][]) para "Língua" (String[])
            String[] mapaStrings = mapa.paraStringArray();
            
            // 2. Cria um "Professor" (Pathfinder) para este mapa
            Pathfinder professor = new Pathfinder(mapaStrings);
            
            // 3. Pede ao professor para encontrar o melhor S e F
            professor.encontrarInicioEFimMaisLongos();
            
            // 4. Pede o caminho
            List<Node> caminho = professor.encontrarCaminho();

            // 5. Calcula a "Nota" (Fitness)
            if (caminho == null) {
                // Se não há caminho, a nota é ZERO.
                mapa.fitness = 0;
            } else {
                // A nota é o comprimento do caminho! (Regra 2 que definiste)
                // Isto recompensa caminhos longos e complexos.
                mapa.fitness = caminho.size();
                
                // TODO: Adicionar a "Regra 3" (becos sem saída)
                // (mapa.fitness *= (numero de 'O's / caminho.size()) )
            }
        }
        
        // Organiza a população: os melhores (maior fitness) ficam no topo da lista.
        populacao.sort(Comparator.comparingInt((MapaGenetico m) -> m.fitness).reversed());
    }

    /**
     * Retorna o melhor mapa (o "vencedor") da geração atual.
     */
    public MapaGenetico getMelhorMapa() {
        return populacao.get(0); // Como a lista está ordenada, o melhor é o [0]
    }

    /**
     * Executa UM ciclo de evolução (Geração N -> Geração N+1).
     */
    public void evoluirProximaGeracao() {
        
        // 1. Cria a lista para a "próxima geração"
        List<MapaGenetico> novaPopulacao = new ArrayList<>(tamanhoPopulacao);

        // 2. Elitismo: Os "melhores" (numElite) sobrevivem automaticamente.
        // Isto garante que o nosso melhor mapa nunca "piora".
        for (int i = 0; i < numElite; i++) {
            novaPopulacao.add(populacao.get(i));
        }

        // 3. Preenche o resto da população com "filhos"
        while (novaPopulacao.size() < tamanhoPopulacao) {
            
            // 3a. Seleciona dois "pais" (os com melhor fitness têm mais chance)
            MapaGenetico pai1 = selecionarPai();
            MapaGenetico pai2 = selecionarPai();

            // 3b. "Crossover" (Reprodução)
            MapaGenetico filho;
            if (rand.nextFloat() < taxaCrossover) {
                filho = crossover(pai1, pai2);
            } else {
                // Se não houver crossover, o filho é um clone do Pai 1
                filho = pai1; 
            }

            // 3c. "Mutação"
            mutacao(filho);

            // 3d. Adiciona o novo filho à próxima geração
            novaPopulacao.add(filho);
        }
        
        // 4. Substitui a população antiga pela nova
        this.populacao = novaPopulacao;
    }


    // --- 4. Métodos Genéticos (Seleção, Crossover, Mutação) ---

    /**
     * Seleção por "Torneio": Pega em 5 mapas aleatórios e retorna o melhor deles.
     * Isto dá aos mapas "médios" uma chance de se reproduzirem,
     * o que é bom para a diversidade genética.
     */
    private MapaGenetico selecionarPai() {
        int tamanhoTorneio = 5;
        MapaGenetico melhorPai = null;
        
        for (int i = 0; i < tamanhoTorneio; i++) {
            int index = rand.nextInt(tamanhoPopulacao);
            MapaGenetico candidato = populacao.get(index);
            
            if (melhorPai == null || candidato.fitness > melhorPai.fitness) {
                melhorPai = candidato;
            }
        }
        return melhorPai;
    }

    /**
     * Crossover (Reprodução): Cria um "filho" a partir de dois "pais".
     * Usa o método "Single-Point Crossover".
     */
    /**
     * Crossover (Reprodução): Cria um "filho" a partir de dois "pais".
     * (Versão CORRIGIDA com 'larguraMapa' e 'alturaMapa')
     */
    private MapaGenetico crossover(MapaGenetico pai1, MapaGenetico pai2) {
        MapaGenetico filho = new MapaGenetico(larguraMapa, alturaMapa);

        // Escolhe um "ponto de corte" aleatório no DNA
        // CORRIGIDO: Usa 'alturaMapa'
        int pontoCorteZ = rand.nextInt(alturaMapa); 
        
        // CORRIGIDO: Usa 'alturaMapa'
        for (int z = 0; z < alturaMapa; z++) { 
            // CORRIGIDO: Usa 'larguraMapa'
            for (int x = 0; x < larguraMapa; x++) { 
                
                // Parte 1 (DNA do Pai 1)
                if (z < pontoCorteZ) {
                    filho.grelha[z][x] = pai1.grelha[z][x];
                }
                // Parte 2 (DNA do Pai 2)
                else {
                    filho.grelha[z][x] = pai2.grelha[z][x];
                }
            }
        }
        return filho;
    }

    /**
     * Mutação: Percorre *cada* "gene" (bloco) do mapa de um "filho"
     * e dá-lhe uma pequena chance (taxaMutacao) de "mudar"
     * (de 'X' para 'O' ou de 'O' para 'X').
     */
    /**
     * Mutação: Percorre *cada* "gene" (bloco) do mapa de um "filho"
     * e dá-lhe uma pequena chance (taxaMutacao) de "mudar".
     * (Versão CORRIGIDA com 'larguraMapa' e 'alturaMapa')
     */
    private void mutacao(MapaGenetico mapa) {
        // CORRIGIDO: Usa 'alturaMapa'
        for (int z = 0; z < alturaMapa; z++) { 
            // CORRIGIDO: Usa 'larguraMapa'
            for (int x = 0; x < larguraMapa; x++) { 
                
                // Ignora as bordas (elas DEVEM ser 'X')
                // CORRIGIDO: Usa 'alturaMapa' e 'larguraMapa'
                if (z == 0 || z == alturaMapa - 1 || x == 0 || x == larguraMapa - 1) {
                    continue; 
                }

                // Se o "dado" (0.0 a 1.0) for menor que a taxa de mutação...
                if (rand.nextFloat() < taxaMutacao) {
                    // ... inverte o gene!
                    if (mapa.grelha[z][x] == 'X') {
                        mapa.grelha[z][x] = 'O';
                    } else {
                        mapa.grelha[z][x] = 'X';
                    }
                }
            }
        }
    }
}