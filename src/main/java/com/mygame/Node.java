package com.mygame;

import java.util.Objects;

/**
 * Representa um único "bloco" ou "nó" no nosso labirinto.
 * É a "célula cerebral" para o nosso algoritmo de pathfinding (A*).
 */
public class Node {

    // --- 1. Identidade do Nó ---
    
    // A posição deste nó no "mapa" (não no mundo 3D)
    public int x;
    public int z; // (Usamos Z em vez de Y, para manter a consistência com o 3D)
    
    // Este nó é uma parede (intransponível) ou um caminho (transponível)?
    public boolean isWall;

    // --- 2. Variáveis do Algoritmo A* ---
    
    // "G cost": O custo (distância) para vir do "Início" até este nó.
    public int gCost;
    
    // "H cost": A "Heurística" (o nosso "chute") do custo deste nó até ao "Fim".
    public int hCost;
    
    // "F cost": O custo total (G + H). Este é o valor que o A* usa para decidir
    // qual nó é o mais promissor para explorar a seguir.
    public int fCost;

    // "Pai" (Parent): O nó de onde viemos para chegar até este.
    // (Usamos isto no final para "refazer" o caminho, de trás para a frente).
    public Node parent;

    /**
     * Construtor: Cria um novo Nó.
     * @param x A posição X deste nó no mapa.
     * @param z A posição Z deste nó no mapa.
     * @param isWall "true" se for uma parede ('X'), "false" se for um caminho ('O', 'S', 'F').
     */
    public Node(int x, int z, boolean isWall) {
        this.x = x;
        this.z = z;
        this.isWall = isWall;
        
        // Os custos e o "pai" são calculados depois pelo algoritmo A*
        this.gCost = 0;
        this.hCost = 0;
        this.fCost = 0;
        this.parent = null;
    }

    /**
     * Calcula e atualiza o F-Cost (Custo Total) deste nó.
     */
    public void calculateFCost() {
        this.fCost = this.gCost + this.hCost;
    }

    // --- 3. Métodos Essenciais (Para o Java saber comparar Nós) ---

    /**
     * Permite ao Java saber se dois objetos "Node" são o mesmo.
     * (Precisamos disto para listas, como "lista.contains(esteNo)")
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        // Dois nós são "iguais" se tiverem o mesmo X e o mesmo Z.
        Node node = (Node) obj;
        return x == node.x && z == node.z;
    }
    

    /**
     * Um "código hash" único baseado na posição X e Z.
     * É um "parceiro" do .equals() e é necessário para listas.
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}