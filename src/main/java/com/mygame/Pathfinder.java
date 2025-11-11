package com.mygame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Esta classe é o "cérebro" da nossa IA.
 * Ela contém o algoritmo A* (A-Star) para encontrar o caminho mais curto
 * num labirinto, representado por uma grelha de Nós (Nodes).
 */
public class Pathfinder {
    
    private final List<Node> nosDeCaminho; // Lista de todos os 'O'
    // A grelha 2D de Nós que representa o nosso labirinto
    private final Node[][] grelha;
    
    // O tamanho do nosso mapa
    private final int larguraMapa;
    private final int alturaMapa;

    // Os pontos de início e fim que encontramos ao ler o mapa
    private Node noInicio;
    private Node noFim;

    /**
     * Construtor do Pathfinder.
     * Ele "lê" o mapa (String[]) e converte-o numa Grelha de Nós (Node[][]).
     * @param mapaLabirinto O mapa 2D (array de Strings) vindo do Main.java.
     */
    public Pathfinder(String[] mapaLabirinto) {
        
        this.alturaMapa = mapaLabirinto.length;
        this.larguraMapa = mapaLabirinto[0].length();
        this.grelha = new Node[larguraMapa][alturaMapa];

        // 1. Loop por todo o mapa (String[])
        for (int z = 0; z < alturaMapa; z++) {
            String linha = mapaLabirinto[z];
            for (int x = 0; x < larguraMapa; x++) {
                
                char caractere = linha.charAt(x);
                
                // 2. Decide se este bloco é uma parede
                boolean isWall = (caractere == 'X');
                
                // 3. Cria um novo Nó e guarda-o na nossa grelha
                Node node = new Node(x, z, isWall);
                this.grelha[x][z] = node;

                
            }
        }
        this.nosDeCaminho = encontrarTodosOsCaminhos();
    }

    /**
     * Método PÚBLICO principal.
     * Tenta encontrar o caminho do 'S' ao 'F' que foram definidos no construtor.
     * @return Uma Lista de Nós (o caminho), ou "null" se não houver caminho.
     */

    /**
     * O "coração" do algoritmo A-Star.
     * @param inicio O Nó de partida (S).
     * @param fim O Nó de destino (F).
     * @return A lista de Nós (caminho) ou "null".
     */
    private List<Node> calcularCaminhoAStar(Node inicio, Node fim) {
        
        // A "Lista Aberta": Nós que descobrimos, mas ainda não visitámos.
        List<Node> listaAberta = new ArrayList<>();
        
        // A "Lista Fechada": Nós que já visitámos e analisámos.
        List<Node> listaFechada = new ArrayList<>();

        // 1. Adiciona o nó inicial à Lista Aberta para começarmos
        listaAberta.add(inicio);

        // Loop principal: continua enquanto houver nós para explorar
        while (!listaAberta.isEmpty()) {

            // --- A. Encontra o Nó mais promissor ---
            // (O nó na Lista Aberta com o F-Cost mais baixo)
            Node noAtual = listaAberta.get(0);
            for (int i = 1; i < listaAberta.size(); i++) {
                if (listaAberta.get(i).fCost < noAtual.fCost || 
                   (listaAberta.get(i).fCost == noAtual.fCost && listaAberta.get(i).hCost < noAtual.hCost)) {
                    noAtual = listaAberta.get(i);
                }
            }

            // --- B. Move o Nó da Aberta para a Fechada ---
            // (Já o visitámos e analisámos, não precisamos de o ver de novo)
            listaAberta.remove(noAtual);
            listaFechada.add(noAtual);

            // --- C. ENCONTRÁMOS O FIM! ---
            // Se o nó atual é o nó de destino, terminámos!
            if (noAtual.equals(fim)) {
                // Encontrámos o caminho. Agora, "refaz" o caminho
                // seguindo os "pais" de trás para a frente (do Fim ao Início).
                return refazerCaminho(fim);
            }

            // --- D. Explora os Vizinhos do Nó Atual ---
            for (Node vizinho : getVizinhos(noAtual)) {
                
                // Se o vizinho for uma parede ou já estiver na Lista Fechada, ignora-o.
                if (vizinho.isWall || listaFechada.contains(vizinho)) {
                    continue;
                }

                // --- E. Calcula o "G-Cost" (Custo do Início) ---
                // Calcula o custo para chegar a *este* vizinho através do *nó atual*.
                int novoGCostParaVizinho = noAtual.gCost + calcularDistancia(noAtual, vizinho);

                // Se este caminho (pelo 'noAtual') for melhor do que algum que já tínhamos...
                // ...ou se este vizinho nunca foi explorado (não está na Lista Aberta)...
                if (novoGCostParaVizinho < vizinho.gCost || !listaAberta.contains(vizinho)) {
                    
                    // Atualiza os custos do vizinho
                    vizinho.gCost = novoGCostParaVizinho;
                    vizinho.hCost = calcularDistancia(vizinho, fim);
                    vizinho.calculateFCost(); // fCost = gCost + hCost
                    
                    // Define o "pai" deste vizinho como sendo o "nó atual"
                    // (Isto é como deixar "migalhas de pão")
                    vizinho.parent = noAtual;

                    // Adiciona o vizinho à Lista Aberta para ser explorado
                    if (!listaAberta.contains(vizinho)) {
                        listaAberta.add(vizinho);
                    }
                }
            }
        }

        // Se a Lista Aberta ficar vazia e nunca encontrámos o Fim,
        // significa que NÃO HÁ CAMINHO.
        return null; 
    }

    /**
     * Refaz o caminho a partir do nó final, seguindo os "pais" (parent).
     * @param noFim O nó de destino que foi encontrado.
     * @return A lista de Nós (o caminho) na ordem correta (Início -> Fim).
     */
    private List<Node> refazerCaminho(Node noFim) {
        List<Node> caminho = new ArrayList<>();
        Node noAtual = noFim;

        // Loop "de trás para a frente" (Fim -> Pai -> Pai...)
        while (noAtual != null) {
            caminho.add(noAtual);
            noAtual = noAtual.parent;
        }
        
        // A lista está ao contrário (Fim -> Início). Vamos invertê-la.
        Collections.reverse(caminho);
        return caminho;
    }

    /**
     * Encontra e retorna os 4 vizinhos (Norte, Sul, Leste, Oeste) de um nó.
     * @param node O nó central.
     * @return Uma lista de Nós vizinhos válidos.
     */
    private List<Node> getVizinhos(Node node) {
        List<Node> vizinhos = new ArrayList<>();
        
        // Coordenadas dos 4 vizinhos
        int[] dx = {0, 0, 1, -1}; // (Norte, Sul, Leste, Oeste - X)
        int[] dz = {1, -1, 0, 0}; // (Norte, Sul, Leste, Oeste - Z)

        for (int i = 0; i < 4; i++) {
            int checkX = node.x + dx[i];
            int checkZ = node.z + dz[i];

            // Verifica se o vizinho está DENTRO dos limites do mapa
            if (checkX >= 0 && checkX < larguraMapa && checkZ >= 0 && checkZ < alturaMapa) {
                // Se estiver, adiciona o Nó da nossa grelha
                vizinhos.add(grelha[checkX][checkZ]);
            }
        }
        return vizinhos;
    }
    
    /**
     * Corre o algoritmo BFS (Busca em Largura) a partir de um único nó.
     * Encontra a distância desse nó para todos os outros nós acessíveis.
     * @param noInicio O Nó de onde a "inundação" começa.
     * @return Um "Mapa de Distâncias" (um `List<QueueNode>`) de todos os nós encontrados.
     */
    private List<QueueNode> executarBFS(Node noInicio) {
        
        // Lista de nós encontrados e as suas distâncias
        List<QueueNode> nosEncontrados = new ArrayList<>();
        
        // Fila para o algoritmo: "Quem vamos visitar a seguir?"
        List<QueueNode> fila = new ArrayList<>();
        
        // "Visitados": Uma lista para não visitarmos o mesmo nó duas vezes
        List<Node> visitados = new ArrayList<>();

        // 1. Começa o processo
        QueueNode noRaiz = new QueueNode(noInicio, 0); // Distância 0
        fila.add(noRaiz);
        visitados.add(noInicio);
        nosEncontrados.add(noRaiz);

        // 2. Loop principal do BFS (enquanto a fila não estiver vazia)
        int indexFila = 0;
        while (indexFila < fila.size()) {
            QueueNode atual = fila.get(indexFila++); // Pega o próximo da fila

            // 3. Olha para os 4 vizinhos
            for (Node vizinho : getVizinhos(atual.node)) {
                
                // Se o vizinho for válido (não é parede E não foi visitado)...
                if (!vizinho.isWall && !visitados.contains(vizinho)) {
                    
                    // 4. Marca-o como visitado e adiciona-o à fila
                    visitados.add(vizinho);
                    QueueNode noVizinho = new QueueNode(vizinho, atual.distancia + 1);
                    fila.add(noVizinho);
                    nosEncontrados.add(noVizinho);
                }
            }
        }
        
        // 5. Retorna a lista de todos os nós que a "inundação" alcançou
        return nosEncontrados;
    }

    /**
     * Calcula o "custo" (distância) entre dois nós.
     * Usa a "Distância de Manhattan" (movimento apenas em grelha, não diagonal).
     * @param a Nó A
     * @param b Nó B
     * @return A distância (custo).
     */
    private int calcularDistancia(Node a, Node b) {
        // Custo do movimento: 10 para horizontal/vertical.
        // (Usamos 10 em vez de 1 para evitar problemas com floats).
        int distZ = Math.abs(a.z - b.z);
        int distX = Math.abs(a.x - b.x);
        return 10 * (distX + distZ);
    }
    
    /**
     * "Varre" a grelha e retorna uma lista de todos os Nós que SÃO caminhos
     * (não-paredes).
     * @return Uma lista de Nós transponíveis.
     */
    private List<Node> encontrarTodosOsCaminhos() {
        List<Node> caminhos = new ArrayList<>();
        for (int z = 0; z < alturaMapa; z++) {
            for (int x = 0; x < larguraMapa; x++) {
                if (!grelha[x][z].isWall) {
                    caminhos.add(grelha[x][z]);
                }
            }
        }
        return caminhos;
    }

    /**
     * O "Cérebro" Otimizado!
     * Encontra o caminho mais longo usando BFS (muito mais rápido).
     */
    public void encontrarInicioEFimMaisLongos() {
        System.out.println("Pathfinder: A calcular o caminho mais longo (Método Rápido BFS)...");

        // 1. Pega no primeiro caminho 'O' que encontrarmos (ponto de partida)
        if (nosDeCaminho.isEmpty()) {
            System.err.println("Pathfinder: O mapa não tem caminhos ('O')!");
            return;
        }
        Node pontoDePartida = nosDeCaminho.get(0); // Pega no primeiro 'O'

        // 2. Corre o BFS UMA SÓ VEZ a partir desse ponto.
        // Isto "inunda" o labirinto e encontra a distância para todos os outros nós.
        List<QueueNode> todosNos = executarBFS(pontoDePartida);
        
        // 3. Encontra o nó com a "distância" mais longa
        QueueNode noMaisLonge = todosNos.get(0);
        for (QueueNode qn : todosNos) {
            if (qn.distancia > noMaisLonge.distancia) {
                noMaisLonge = qn;
            }
        }
        
        // 4. Agora, o nó MAIS LONGE do início é o nosso "Fim"
        this.noFim = noMaisLonge.node;

        // 5. Agora, corremos o BFS UMA SEGUNDA VEZ, a partir do "Fim".
        List<QueueNode> todosNosDoFim = executarBFS(this.noFim);

        // 6. Encontra o nó mais longe DO FIM.
        QueueNode noMaisLongeDoFim = todosNosDoFim.get(0);
        for (QueueNode qn : todosNosDoFim) {
            if (qn.distancia > noMaisLongeDoFim.distancia) {
                noMaisLongeDoFim = qn;
            }
        }

        // 7. Esse nó (o mais longe do Fim) é o nosso "Início"!
        this.noInicio = noMaisLongeDoFim.node;
        
        System.out.println("Pathfinder: Caminho mais longo encontrado!");
    }

    /**
     * O "encontrarCaminho" original, mas agora garantimos que os nós foram definidos.
     * @return 
     */
    public List<Node> encontrarCaminho() {
        // Verifica se o 'encontrarInicioEFimMaisLongos' já foi chamado
        if (noInicio == null || noFim == null) {
            System.err.println("Pathfinder: Tens de chamar 'encontrarInicioEFimMaisLongos()' PRIMEIRO!");
            return null;
        }
        // Calcula o caminho A* entre os nós que encontrámos
        return calcularCaminhoAStar(noInicio, noFim);
    }
    
    /**
     * "Getter" público para expor o Nó de Início (S) para outras classes.
     * @return O Nó de Início.
     */
    public Node getNoInicio() {
        return noInicio;
    }

    /**
     * "Getter" público para expor o Nó de Fim (F) para outras classes.
     * @return O Nó de Fim.
     */
    public Node getNoFim() {
        return noFim;
    }
    /**
     * "Ajudante" interno para o algoritmo BFS.
     * Guarda um Nó e a sua distância do início.
     */
    private static class QueueNode {
        Node node;
        int distancia;

        QueueNode(Node node, int distancia) {
            this.node = node;
            this.distancia = distancia;
        }
    }
}