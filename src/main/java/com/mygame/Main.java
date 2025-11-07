package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

/**
 * Classe principal do "Projeto Minos".
 * Esta classe inicializa o jogo, o mundo da física e os controlos.
 */
public class Main extends SimpleApplication implements ActionListener, AnalogListener {
    private boolean modoCameraCima = false;
    // --- Variáveis Globais do Jogo ---
    // O "chefe" do mundo da física. Controla a gravidade e as colisões.
    private BulletAppState bulletAppState; 
    // O nosso jogador. É um "controlador" de física, não um objeto visual.
    private CharacterControl player;       
    // "Bandeiras" (flags) para saber se as teclas de movimento estão premidas
    private boolean left = false, right = false, up = false, down = false;
    // Um objeto "ajudante" para cálculos de rotação (evita criar novos a cada frame)
    private final Quaternion tempRotation = new Quaternion();
    private float camVerticalAngle = 0f;
    private final String[] mapaLabirinto = new String[] {
        "XXXXXXXXXXXXXXXXXXXXX",
        "X        X          X",
        "X XXX XX X XXX XX X X",
        "X X   X  X X   X  X X",
        "X XX XXX X XX XXX X X",
        "X  X   X X  X   X X X",
        "XX XX X X XX XX X X X",
        "X   X X    X   X X X",
        "X XXX XXXXXX XXX X X",
        "X   X      X   X   X",
        "XXXXXXXXXXXXXXXXXXXXX"
    };

    
    /**
     * Ponto de entrada do programa.
     * @param args
     */
    public static void main(String[] args) {
        Main app = new Main();
        app.start(); // Inicia a aplicação JME
    }

    /**
     * Este é o método principal de inicialização.
     * É chamado uma vez quando o jogo começa.
     */
    @Override
    public void simpleInitApp() {
        
        // --- 1. Configurar a Física ---
        
        // Inicializa o "mundo" da física
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState); // "Liga" o motor de física ao jogo

        // Ativa a "depuração" (debug)
        // Isto desenha "arames" azuis à volta dos objetos físicos. Útil para ver colisões.
        //bulletAppState.setDebugEnabled(true);

        // --- 2. Configurar Controlos (Rato e Teclado) ---
        
        // Esta é a "combinação mágica" que resolve o problema do rato:
        // 1. Esconde o cursor do Windows.
        // 2. O 'flyCam' (que ainda está ativo) agarra o rato (mouse lock).
        // 3. O 'setupKeys' (abaixo) "rouba" o controlo do 'flyCam'.
        inputManager.setCursorVisible(false);
        setupKeys(); // Chama o nosso método para mapear as teclas

        construirLabirinto();
        
        // --- 5. Criar o Jogador ---
        // Define a forma de colisão do jogador (uma "cápsula")
        // Raio de 0.5f, Altura de 2f
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(0.5f, 2f);

        // Inicializa o nosso "player" (controlador de personagem)
        player = new CharacterControl(capsuleShape, 0.1f); // 0.1f é a altura do degrau

        // Define a física do jogador
        player.setJumpSpeed(20);  // Velocidade do pulo
        player.setFallSpeed(30);  // Velocidade da queda
        player.setGravity(30);    // Força da gravidade (puxa para baixo)

        // Define a posição inicial do jogador
        // (0, 3, 0) -> 3 unidades ACIMA do chão (para ele "cair" no início)
        player.setPhysicsLocation(new Vector3f(4.0f, 3.0f, 4.0f));

        // Adiciona o jogador APENAS ao mundo da física
        // (Não o adicionamos ao rootNode, porque ele é invisível, só a sua física importa)
        bulletAppState.getPhysicsSpace().add(player);
    }
    
    /**
     * Método "ajudante" que constrói UMA parede física e visual.
     * @param x Posição X (centro da parede)
     * @param y Posição Y (centro da parede)
     * @param z Posição Z (centro da parede)
     */
    private void criarParede(float x, float y, float z) {
        // Define as dimensões da parede
        float raioLargura = 2.0f; // Metade da largura (Total = 4.0f)
        float raioAltura = 1.5f;  // Metade da altura (Total = 3.0f)
        float raioEspessura = 2.0f; // Metade da espessura (Total = 4.0f)
        
        // Cria a "forma" visual (uma caixa)
        Box formaParede = new Box(raioLargura, raioAltura, raioEspessura);
        Geometry geoParede = new Geometry("Parede", formaParede);
        
        // Cria o "material" (a cor)
        Material matParede = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matParede.setColor("Color", ColorRGBA.Red); // <-- CORRIGIDO! (Para ser fácil de ver)
        geoParede.setMaterial(matParede);

        // --- A Física ---
        
        // Cria a forma de colisão (a partir da geometria)
        CollisionShape fisicaFormaParede = CollisionShapeFactory.createBoxShape(geoParede);
        
        // Cria o corpo físico (0kg = Estático, não se mexe)
        RigidBodyControl fisicaCorpoParede = new RigidBodyControl(fisicaFormaParede, 0);
        
        // "Cola" a física ao objeto visual
        // (Isto faz com que o visual "salte" para a posição da física)
        geoParede.addControl(fisicaCorpoParede);
        
        // Adiciona ambos aos seus mundos
        bulletAppState.getPhysicsSpace().add(fisicaCorpoParede); // Mundo da Física
        rootNode.attachChild(geoParede); // Mundo Visual
        // Define a posição da parede no MUNDO DA FÍSICA
        fisicaCorpoParede.setPhysicsLocation(new Vector3f(x, y, z));
    }
    
    /**
     * Método ajudante que constroi 1 bloco de chão físico e visual
     * @param x posição em X (centro do bloco)
     * @param z posição Z centro do bloco
     */
    private void criarBlocoChao(float x, float z) {
        float raioLargura = 2.0f;
        float raioAltura = 0.1f;
        float raioEspessura = 2.0f;
        
        Box formaChao = new Box(raioLargura, raioAltura, raioEspessura);
        Geometry geoChao = new Geometry("Chao", formaChao);
        
        // COR do chão..
        Material matChao = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matChao.setColor("Color", ColorRGBA.Green);
        geoChao.setMaterial(matChao);
        
        //Fisica..
        CollisionShape fisicaFormaChao = CollisionShapeFactory.createBoxShape(geoChao);
        RigidBodyControl fisicaCorpoChao = new RigidBodyControl(fisicaFormaChao, 0); //0Kg
        
        //Define a posicao y = 0 para estar no chão
        fisicaCorpoChao.setPhysicsLocation(new Vector3f(x, 0, z)); // y = 0
        
        geoChao.addControl(fisicaCorpoChao);
        
        //add ao mundo
        bulletAppState.getPhysicsSpace().add(fisicaCorpoChao);
        rootNode.attachChild(geoChao);
        
        
    }
    
    private void construirLabirinto(){
        float tamanhoBloco = 4.0f; //O nosso bloco é 4.0f x 4.0f
        float alturaParede = 3.0f; // a nossa altura total é 3.0f
        
        //Loop pela altura do mapa (as linhas do eixo Z)
        for (int z = 0; z < mapaLabirinto.length; z++) {
            String linha = mapaLabirinto[z];
            
            //Loop pela largura do mapa (eixo X)
            for (int x = 0; x < linha.length(); x++) {
                //calcula a posicao que é compartilhada pelo chao e paredes
                float posX = x * tamanhoBloco;
                float posZ = z * tamanhoBloco;
                
                //Criar chao independente do char 'x' ou '  '...
                criarBlocoChao(posX, posZ);
                
                //se o caractere for x constroi parede
                char caractere = linha.charAt(x);
                
                if (caractere == 'X') {
                   float posY = alturaParede / 2; //para a parede ficar no chão
                   
                   //chama a ferramente de paredes
                   criarParede(posX, posY, posZ);
                }
            }
        }
    }
    
    /**
     * Método de inicialização que "mapeia" as teclas e o rato.
     */
    private void setupKeys() {
        // --- Teclado (WASD) ---
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        
        // Diz ao "inputManager" que "esta" classe (this) vai ouvir as ações do teclado
        inputManager.addListener(this, "Left", "Right", "Up", "Down");
        
        // --- Rato (Eixos X e Y) ---
        // Mapeia o movimento do rato (eixo X)
        inputManager.addMapping("LookRight", new MouseAxisTrigger(MouseInput.AXIS_X, false)); // "true" = positivo (direita)
        inputManager.addMapping("LookLeft", new MouseAxisTrigger(MouseInput.AXIS_X, true)); // "false" = negativo (esquerda)

        // Mapeia o movimento do rato (eixo Y)
        inputManager.addMapping("LookUp", new MouseAxisTrigger(MouseInput.AXIS_Y, false)); // "true" = positivo (para cima)
        inputManager.addMapping("LookDown", new MouseAxisTrigger(MouseInput.AXIS_Y, true)); // "false" = negativo (para baixo)

        // Diz ao "inputManager" que "esta" classe (this) vai ouvir os movimentos analógicos
        inputManager.addListener(this, "LookRight", "LookLeft", "LookUp", "LookDown");
        
        // Mapeia a tecla 'F' para a ação "ToggleCamera"
        inputManager.addMapping("ToggleCamera", new KeyTrigger(KeyInput.KEY_F));
        // Diz ao "ouvinte" (this) para "ouvir" esta nova ação
        inputManager.addListener(this, "ToggleCamera");
    }
    
    /**
     * "Ouvinte" do Teclado (ActionListener).
     * Chamado pelo JME quando uma tecla (W,A,S,D) é PREMIDA ou LARGADA.
     * @param name O nome da ação que foi disparada (ex: "Left", "Right").
     * @param isPressed "true" se a tecla foi premida, "false" se foi largada.
     * @param tpf O tempo por frame (não costumamos usar neste método).
     */
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        
        // Usamos a sintaxe "switch expression" (Java 14+)
        // Atualiza as nossas "bandeiras" booleanas
        switch (name) {
            case "Left"  -> left = isPressed;
            case "Right" -> right = isPressed;
            case "Up"    -> up = isPressed;
            case "Down"  -> down = isPressed;
            case "ToggleCamera"-> {
                // "isPressed" garante que só executamos na "descida" da tecla
                if (isPressed) { 
                    // Inverte o valor da "bandeira" (interruptor)
                    modoCameraCima = !modoCameraCima; 
                }
                break; // Não te esqueças do break!
            }
        }
    }
    
    /**
     * "Ouvinte" do Rato (AnalogListener).
     * Chamado a cada frame que o rato se mexe.
     * @param name O nome da ação que foi disparada (ex: "LookRight").
     * @param value A intensidade do movimento (o "quanto" o rato mexeu).
     * @param tpf O tempo por frame (usado para suavizar o movimento).
     */
    @Override
    public void onAnalog(String name, float value, float tpf) {
        // "value" é a intensidade do movimento do rato
        
        float rotationSpeed = 2.0f; // Podes ajustar esta velocidade!
        
        // O JME já aplica o 'tpf' ao 'value' nos AnalogListeners, 
        // mas multiplicar por 'rotationSpeed' ajuda a controlar a sensibilidade.

        // --- Rotação Horizontal (Esquerda/Direita) ---
        // (Isto roda o "corpo" do jogador, o CharacterControl)
        
        switch (name) {
            case "LookRight" ->  { //rotacao horizontal direita
                float rotationAmount = value * rotationSpeed;
                tempRotation.fromAngleAxis(-rotationAmount, Vector3f.UNIT_Y);
                Vector3f oldDirection = player.getViewDirection();
                Vector3f newDirection = tempRotation.mult(oldDirection);
                player.setViewDirection(newDirection);
            }
            case "LookLeft" -> { //esquerda
                float rotationAmount = value * rotationSpeed;
                tempRotation.fromAngleAxis(rotationAmount, Vector3f.UNIT_Y);
                Vector3f oldDirection = player.getViewDirection();
                Vector3f newDirection = tempRotation.mult(oldDirection);
                player.setViewDirection(newDirection);
                break;
            }
            case "LookUp" -> { // cima
                float rotationAmount = value * rotationSpeed * 0.5f;
                camVerticalAngle -= rotationAmount;
                break;
            }
            case "LookDown" -> {
                float rotationAmount = value * rotationSpeed * 0.5f;
                camVerticalAngle += rotationAmount;
                break;
            }
            
        }
        
        float maxAngle = 1.396f; //80 graus
        float minAngle = -1.396f; //-80 graus
        
        if (camVerticalAngle > maxAngle) {
            camVerticalAngle = maxAngle;
        } else if (camVerticalAngle < minAngle) {
            camVerticalAngle = minAngle;
        }
    }

    /**
     * O "Game Loop" principal.
     * Este método é chamado 60 vezes por segundo (ou mais).
     * @param tpf "Time Per Frame" - O tempo que passou desde o último frame.
     */
    @Override
    public void simpleUpdate(float tpf) {

        // --- 1. Lógica de Movimento (Baseado nas Teclas) ---

        // Pega na direção da câmara (para onde estamos a olhar)
        Vector3f camDir = cam.getDirection();
        Vector3f camLeft = cam.getLeft();
        
        // Vetor que guarda a direção final do nosso movimento
        Vector3f walkDirection = new Vector3f(0, 0, 0);

        // Adiciona direções ao "walkDirection" com base nas teclas premidas
        if (up) {
            walkDirection.addLocal(camDir); // Adiciona "em frente"
        }
        if (down) {
            walkDirection.addLocal(camDir.negate()); // Adiciona "para trás"
        }
        if (left) {
            walkDirection.addLocal(camLeft); // Adiciona "para a esquerda"
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate()); // Adiciona "para a direita"
        }

        // Define a direção de "andar" no nosso "player" (controlador de física)
        // .setY(0) garante que o jogador não voa se olharmos para cima/baixo
        // .normalizeLocal() garante que andar na diagonal não é mais rápido
        // .mult(0.1f) define a velocidade (0.1f). Podes aumentar para 0.2f, 0.3f, etc.
        player.setWalkDirection(walkDirection.setY(0).normalizeLocal().mult(0.1f)); 

        // --- 2. Lógica da Câmara (COM O INTERRUPTOR) ---
        
        if (modoCameraCima == false) {
            // --- MODO 1ª PESSOA ---
            
            // 2.1. Obtém a posição e direção do CORPO (horizontal)
            Vector3f posCorpoJogador = player.getPhysicsLocation();
            Vector3f dirCorpoJogador = player.getViewDirection();
            
            // 2.2. Define a posição da câmara (os "olhos")
            cam.setLocation(posCorpoJogador.add(new Vector3f(0, 1.5f, 0))); 

            // 2.3. Calcula a Rotação COMPLETA (Corpo + Pescoço)
            Quaternion rotCorpo = tempRotation; 
            rotCorpo.lookAt(dirCorpoJogador, Vector3f.UNIT_Y);
            
            Quaternion rotPescoco = new Quaternion();
            rotPescoco.fromAngleAxis(camVerticalAngle, Vector3f.UNIT_X);
            
            Quaternion rotFinal = rotCorpo.mult(rotPescoco);
            
            // 2.4. Aplica a rotação final à câmara
            cam.setRotation(rotFinal);
            
        } else {
            // --- MODO VISTA DE CIMA "DIAGONAL" ---
            
            // 2.1. Obtém a posição do "chão" do jogador
            Vector3f posJogador = player.getPhysicsLocation();
            
            // 2.2. Define um "deslocamento" (offset) fixo para a câmara
            // (Vamos 15 unidades para CIMA (Y) e 12 unidades para TRÁS (Z))
            Vector3f offsetCamera = new Vector3f(0f, 150f, 0f);
            
            // 2.3. Calcula a nova Posição da Câmara
            Vector3f novaPosCamera = posJogador.add(offsetCamera);
            
            // 2.4. Define a Posição da Câmara
            cam.setLocation(novaPosCamera);
            
            // 2.5. Faz a câmara OLHAR SEMPRE para o jogador
            cam.lookAt(posJogador, Vector3f.UNIT_Y);
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        // Método para lógica de renderização (pode ficar vazio)
    }
}