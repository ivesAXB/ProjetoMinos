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
import java.util.List;

/**
 * Classe principal do "Projeto Minos".
 * Esta classe inicializa o jogo, o mundo da física e os controlos.
 */
public class Main extends SimpleApplication implements ActionListener, AnalogListener {

    // --- Variáveis Globais do Jogo ---
    private Vector3f spawnPoint = new Vector3f(0, 3.0f, 0);
    private BulletAppState bulletAppState;
    private CharacterControl player;
    private Pathfinder pathfinder;
    
    // Flags de movimento
    private boolean left = false, right = false, up = false, down = false;
    
    // Objeto ajudante para rotação
    private final Quaternion tempRotation = new Quaternion();
    
    // Variável para "inclinar" a câmara (olhar cima/baixo)
    private float camVerticalAngle = 0f;
    
    // Flag para o "interruptor" da câmara (F)
    private boolean modoCameraCima = false;
    
    
    // O "mapa" 2D do nosso labirinto
    private final String[] mapaLabirinto = new String[]{
        "XXXXXXXXXXXXXXXXXXXXX",
        "XOOOOOOOOXOOOOOOOOOOX", // O Spawn é aqui (1,1)
        "XOXXXOXXOXOXXXOXXOXOX",
        "XOXOOOXOOXOXOOOXOOXOX",
        "XOXXOXXXOXOXXOXXXOXOX",
        "XOOXOOOXOOOOXOOOXOXOX",
        "XXOXXOXOXOXXOXXOXOXOX",
        "XOOOXOXOOOOOOOOXOXOOX",
        "XOXXXOXXXXXXOXXXOXOOX",
        "XOOOXOOOOOOXOOOXOOOOX",
        "XXXXXXXXXXXXXXXXXXXXX"
    };

    /**
     * Ponto de entrada do programa.
     * @param args
     */
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    /**
     * Método de inicialização (chamado uma vez).
     */
    @Override
    public void simpleInitApp() {

        // 1. Inicia a Física
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        // Descomente a linha abaixo para ver os "arames" azuis da física
        // bulletAppState.setDebugEnabled(true);

        // 2. Configura os Controlos
        inputManager.setCursorVisible(false);
        setupKeys(); // Chama o nosso método de mapeamento

        // 3. Constrói o Mundo
        construirLabirinto(); // Este método agora constrói o chão E as paredes
        
        // --- NOVO CÓDIGO (Ligar o "Cérebro" A*) ---
        
        // 1. Cria o Pathfinder (ele vai ler o mapa de 'X' e 'O')
        this.pathfinder = new Pathfinder(mapaLabirinto);
        
        // 2. PEDE-LHE PARA CALCULAR O CAMINHO MAIS LONGO!
        // (Isto vai definir o 'noInicio' e 'noFim' dentro dele)
        pathfinder.encontrarInicioEFimMaisLongos();

        // 3. Pede-lhe o caminho (que ele agora sabe qual é)
        List<Node> caminho = pathfinder.encontrarCaminho();

        // 4. Verifica se ele encontrou um caminho
        if (caminho != null) {
            // SUCESSO! Vamos "pintar" o caminho e definir o spawn
            desenharCaminho(caminho); // Pinta o caminho azul
            
            // Define o spawn do JOGADOR
            Node noSpawn = pathfinder.getNoInicio();
            float spawnX = noSpawn.x * 4.0f; // (4.0f é o nosso tamanhoBloco)
            float spawnZ = noSpawn.z * 4.0f;
            this.spawnPoint = new Vector3f(spawnX, 3.0f, spawnZ);
            
            // Pinta o bloco de Fim (Ciano)
            Node noFim = pathfinder.getNoFim();
            criarBlocoFim(noFim.x * 4.0f, noFim.z * 4.0f);
            
        } else {
            // FALHA! (O mapa não tem solução)
            System.out.println("ALERTA: O Algoritmo A* não encontrou um caminho!");
        }

        // 4. Cria o Jogador
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(0.5f, 2f);
        player = new CharacterControl(capsuleShape, 0.1f);
        player.setJumpSpeed(20);
        player.setFallSpeed(30);
        player.setGravity(30);

        // Define o Ponto de Spawn (Estático) - (Mapa 1,1) -> (Mundo 4,3,4)
        player.setPhysicsLocation(this.spawnPoint);

        // Adiciona o jogador ao mundo da física
        bulletAppState.getPhysicsSpace().add(player);
    }

    /**
     * Constrói UMA parede física e visual.
     * (Versão com a ORDEM DE POSICIONAMENTO CORRIGIDA)
     */
    private void criarParede(float x, float y, float z) {
        // Tamanhos: 4.0f largura x 3.0f altura x 4.0f profundidade
        float raioLargura = 2.0f;
        float raioAltura = 1.5f;
        float raioEspessura = 2.0f;

        // Visual
        Box formaParede = new Box(raioLargura, raioAltura, raioEspessura);
        Geometry geoParede = new Geometry("Parede", formaParede);
        Material matParede = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matParede.setColor("Color", ColorRGBA.Red); // Cor Vermelha
        geoParede.setMaterial(matParede);

        // Física
        CollisionShape fisicaFormaParede = CollisionShapeFactory.createBoxShape(geoParede);
        RigidBodyControl fisicaCorpoParede = new RigidBodyControl(fisicaFormaParede, 0);

        // --- LÓGICA DE POSICIONAMENTO CORRIGIDA (Plano F) ---
        // 1. "Cola" o visual à física (ambos em 0,0,0)
        geoParede.addControl(fisicaCorpoParede);
        
        // 2. Adiciona ambos aos seus mundos (ainda em 0,0,0)
        bulletAppState.getPhysicsSpace().add(fisicaCorpoParede);
        rootNode.attachChild(geoParede);
        
        // 3. AGORA move o "pacote" (física + visual) para o local final
        fisicaCorpoParede.setPhysicsLocation(new Vector3f(x, y, z));
    }

    /**
     * Constrói UM bloco de CHÃO físico e visual.
     * (Versão com a ORDEM DE POSICIONAMENTO CORRIGIDA)
     */
    private void criarBlocoChao(float x, float z) {
        // Tamanhos: 4.0f largura x 0.2f altura x 4.0f profundidade
        float raioLargura = 2.0f;
        float raioAltura = 0.1f;
        float raioEspessura = 2.0f;

        // Visual
        Box formaChao = new Box(raioLargura, raioAltura, raioEspessura);
        Geometry geoChao = new Geometry("Chao", formaChao);
        Material matChao = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matChao.setColor("Color", ColorRGBA.Green); // Cor Verde
        geoChao.setMaterial(matChao);

        // Física
        CollisionShape fisicaFormaChao = CollisionShapeFactory.createBoxShape(geoChao);
        RigidBodyControl fisicaCorpoChao = new RigidBodyControl(fisicaFormaChao, 0);

        // --- LÓGICA DE POSICIONAMENTO CORRIGIDA (Plano F) ---
        // (Exatamente a mesma ordem do criarParede, para consistência)
        // 1. "Cola"
        geoChao.addControl(fisicaCorpoChao);
        
        // 2. Adiciona
        bulletAppState.getPhysicsSpace().add(fisicaCorpoChao);
        rootNode.attachChild(geoChao);
        
        // 3. Move
        fisicaCorpoChao.setPhysicsLocation(new Vector3f(x, 0, z)); // Y=0 (no chão)
    }
    
    private void criarBlocoSpawn(float x, float z) {
        float raioLargura = 2.0f, raioAltura = 0.1f, raioEspessura = 2.0f;
        Box forma = new Box(raioLargura, raioAltura, raioEspessura);
        Geometry geo = new Geometry("Spawn", forma);
        
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Yellow); // <-- AMARELO
        geo.setMaterial(mat);

        // Lógica de Física (igual ao criarBlocoChao)
        CollisionShape cs = CollisionShapeFactory.createBoxShape(geo);
        RigidBodyControl rbc = new RigidBodyControl(cs, 0);
        geo.addControl(rbc);
        bulletAppState.getPhysicsSpace().add(rbc);
        rootNode.attachChild(geo);
        rbc.setPhysicsLocation(new Vector3f(x, 0, z)); // Y=0
    }
    
    private void criarBlocoFim(float x, float z) {
        float raioLargura = 2.0f, raioAltura = 0.1f, raioEspessura = 2.0f;
        Box forma = new Box(raioLargura, raioAltura, raioEspessura);
        Geometry geo = new Geometry("Spawn", forma);
        
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Cyan); // <-- Ciano
        geo.setMaterial(mat);

        // Lógica de Física (igual ao criarBlocoChao)
        CollisionShape cs = CollisionShapeFactory.createBoxShape(geo);
        RigidBodyControl rbc = new RigidBodyControl(cs, 0);
        geo.addControl(rbc);
        bulletAppState.getPhysicsSpace().add(rbc);
        rootNode.attachChild(geo);
        rbc.setPhysicsLocation(new Vector3f(x, 0, z)); // Y=0
    }
    
    /**
     * Constrói UM bloco de CHÃO AZUL (para mostrar o caminho do A*).
     */
    private void criarBlocoCaminho(float x, float z) {
        float raioLargura = 2.0f, raioAltura = 0.1f, raioEspessura = 2.0f;
        Box forma = new Box(raioLargura, raioAltura, raioEspessura);
        Geometry geo = new Geometry("Caminho", forma);
        
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue); // <-- AZUL
        geo.setMaterial(mat);

        // A física é igual ao chão normal
        CollisionShape cs = CollisionShapeFactory.createBoxShape(geo);
        RigidBodyControl rbc = new RigidBodyControl(cs, 0);
        geo.addControl(rbc);
        bulletAppState.getPhysicsSpace().add(rbc);
        rootNode.attachChild(geo);
        rbc.setPhysicsLocation(new Vector3f(x, 0, z)); // Y=0
    }

    /**
     * Lê o mapa e constrói o chão e as paredes.
     */
    private void construirLabirinto() {
        float tamanhoBloco = 4.0f;
        float alturaParede = 3.0f;
        
        for (int z = 0; z < mapaLabirinto.length; z++) {
            String linha = mapaLabirinto[z];
            for (int x = 0; x < linha.length(); x++) {
                
                float posX = x * tamanhoBloco;
                float posZ = z * tamanhoBloco;
                
                // 1. Cria SEMPRE um chão verde
                criarBlocoChao(posX, posZ); 
                
                // 2. Se for 'X', cria uma parede
                char caractere = linha.charAt(x);
                if (caractere == 'X') {
                    float posY = alturaParede / 2;
                    criarParede(posX, posY, posZ);
                }
            }
        }
    }
    
    private void desenharCaminho(List<Node> caminho) {
        float tamanhoBloco = 4.0f;
        
        // Pega no nó de início e fim
        Node noInicio = pathfinder.getNoInicio();
        Node noFim = pathfinder.getNoFim();
        
        for (Node no : caminho) {
            float posX = no.x * tamanhoBloco;
            float posZ = no.z * tamanhoBloco;

            // Decide qual cor pintar
            if (no.equals(noInicio)) {
                criarBlocoSpawn(posX, posZ); // Pinta o Início de Amarelo
            } else if (no.equals(noFim)) {
                // (O Fim já foi pintado no simpleInitApp, mas não faz mal)
                // criarBlocoFim(posX, posZ); 
            } else {
                criarBlocoCaminho(posX, posZ); // Pinta o resto de Azul
            }
        }
    }

    /**
     * Mapeia as teclas e o rato para as "ações".
     */
    private void setupKeys() {
        // WASD
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addListener(this, "Left", "Right", "Up", "Down");
        
        // Rato (Eixos)
        // CORRIGIDO: "Right" é positivo (true), "Left" é negativo (false)
        inputManager.addMapping("LookRight", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("LookLeft", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        // CORRIGIDO: "Up" é positivo (true), "Down" é negativo (false)
        inputManager.addMapping("LookUp", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("LookDown", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addListener(this, "LookRight", "LookLeft", "LookUp", "LookDown");
        
        // Tecla 'F' (Interruptor da Câmara)
        inputManager.addMapping("ToggleCamera", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addListener(this, "ToggleCamera");
    }

    /**
     * "Ouvinte" do Teclado (Chamado ao premir/largar W,A,S,D,F)
     * (SINTAXE CORRIGIDA: Usa "colon" (:) e "break" para funcionar)
     * @param name
     * @param isPressed
     * @param tpf
     */
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        
        switch (name) {
            case "Left" -> left = isPressed;
            case "Right" -> right = isPressed;
            case "Up" -> up = isPressed;
            case "Down" -> down = isPressed;
            case "ToggleCamera" -> {
                if (isPressed) {
                    modoCameraCima = !modoCameraCima;
                }
            }
        }
    }

    /**
     * "Ouvinte" do Rato (Chamado a cada movimento do rato)
     * (SINTAXE CORRIGIDA: Usa "colon" (:) e "break" para funcionar)
     * @param name
     * @param value
     * @param tpf
     */
    @Override
    public void onAnalog(String name, float value, float tpf) {
        float rotationSpeed = 2.0f;
        float rotationAmount = value * rotationSpeed;

        switch (name) {
            case "LookRight" -> {
                tempRotation.fromAngleAxis(-rotationAmount, Vector3f.UNIT_Y);
                Vector3f oldDirectionR = player.getViewDirection();
                Vector3f newDirectionR = tempRotation.mult(oldDirectionR);
                player.setViewDirection(newDirectionR);
                // <-- BREAK ADICIONADO (CRÍTICO)
            }
            case "LookLeft" -> {
                tempRotation.fromAngleAxis(rotationAmount, Vector3f.UNIT_Y);
                Vector3f oldDirectionL = player.getViewDirection();
                Vector3f newDirectionL = tempRotation.mult(oldDirectionL);
                player.setViewDirection(newDirectionL);
            }

            case "LookUp" -> camVerticalAngle -= rotationAmount * 0.5f;

            case "LookDown" -> camVerticalAngle += rotationAmount * 0.5f;
        }

        // Limita o ângulo do "pescoço"
        float maxAngle = 1.396f; // 80 graus
        float minAngle = -1.396f; // -80 graus
        
        if (camVerticalAngle > maxAngle) {
            camVerticalAngle = maxAngle;
        } else if (camVerticalAngle < minAngle) {
            camVerticalAngle = minAngle;
        }
    }

    /**
     * O "Game Loop" (Chamado 60x por segundo)
     * @param tpf
     */
    @Override
    public void simpleUpdate(float tpf) {

        // --- 1. Lógica de Movimento (WASD) ---
        Vector3f camDir = cam.getDirection();
        Vector3f camLeft = cam.getLeft();
        Vector3f walkDirection = new Vector3f(0, 0, 0);

        if (up) { walkDirection.addLocal(camDir); }
        if (down) { walkDirection.addLocal(camDir.negate()); }
        if (left) { walkDirection.addLocal(camLeft); }
        if (right) { walkDirection.addLocal(camLeft.negate()); }

        player.setWalkDirection(walkDirection.setY(0).normalizeLocal().mult(0.1f));

        // --- 2. Lógica da Câmara (Interruptor F) ---
        if (modoCameraCima == false) {
            // --- MODO 1ª PESSOA ---
            Vector3f posCorpoJogador = player.getPhysicsLocation();
            Vector3f dirCorpoJogador = player.getViewDirection();
            cam.setLocation(posCorpoJogador.add(new Vector3f(0, 1.5f, 0)));
            Quaternion rotCorpo = tempRotation;
            rotCorpo.lookAt(dirCorpoJogador, Vector3f.UNIT_Y);
            Quaternion rotPescoco = new Quaternion();
            rotPescoco.fromAngleAxis(camVerticalAngle, Vector3f.UNIT_X);
            Quaternion rotFinal = rotCorpo.mult(rotPescoco);
            cam.setRotation(rotFinal);
        } else {
            // --- MODO VISTA DE CIMA (DIAGONAL) ---
            // (CORRIGIDO: 150f era muito alto e 0f no Z era "plano")
            Vector3f posJogador = player.getPhysicsLocation();
            // Podes brincar com estes valores! (Y=20, Z=-15)
            Vector3f offsetCamera = new Vector3f(0f, 100f, 0f); 
            Vector3f novaPosCamera = posJogador.add(offsetCamera);
            cam.setLocation(novaPosCamera);
            cam.lookAt(posJogador, Vector3f.UNIT_Y);
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        // Deixa vazio por agora
    }
}