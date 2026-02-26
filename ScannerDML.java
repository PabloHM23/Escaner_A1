import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ScannerDML extends JFrame {

    // Componentes de Interfaz
    private JTextArea txtEntrada;
    private JTable tablaTokens, tablaIds, tablaConsts;
    private DefaultTableModel modeloTokens, modeloIds, modeloConsts;
    private JTextArea txtErrores;
    private JTextArea txtSintaxis;
    private JTabbedPane panelPestanas;

    // Diccionarios Léxicos 
    private Map<String, Integer> palabrasReservadas;
    private Map<String, Integer> delimitadores;
    private Map<String, Integer> operadores;
    private Map<String, Integer> relacionales;

    // Tablas de Símbolos 
    private Map<String, IdEntry> tablaIdentificadores;
    private Map<String, ConstEntry> tablaConstantes;
    private List<TokenEntry> listaTokens;
    private List<String> listaErrores;

    //  Estructuras para el Algoritmo LL 
    private Map<Integer, Map<Integer, int[]>> tablaSintactica; 
    private Stack<Integer> pila;
    private final int LAMBDA = 99;
    private final int PESOS = 199;
    private final int SIMBOLO_INICIAL = 300; 

    //  Contadores Dinámicos 
    private int idCounter = 401;
    private int constCounter = 601;
    private int tokenNo = 1;

    public ScannerDML() {
        inicializarDiccionarios();
        inicializarTablaSintactica();
        configurarInterfaz();
    }

    private void inicializarDiccionarios() {
        palabrasReservadas = new HashMap<>();
        String[] pr = {"SELECT", "FROM", "WHERE", "IN", "AND", "OR", "CREATE", "TABLE", 
                       "CHAR", "NUMERIC", "NOT", "NULL", "CONSTRAINT", "KEY", "PRIMARY", 
                       "FOREIGN", "REFERENCES", "INSERT", "INTO", "VALUES"};
        for (int i = 0; i < pr.length; i++) {
            palabrasReservadas.put(pr[i], 10 + i);
        }

        delimitadores = new HashMap<>();
        delimitadores.put(",", 50); delimitadores.put(".", 51);
        delimitadores.put("(", 52); delimitadores.put(")", 53);
        delimitadores.put("'", 54);

        operadores = new HashMap<>();
        operadores.put("+", 70); operadores.put("-", 71);
        operadores.put("*", 72); operadores.put("/", 73);

        relacionales = new HashMap<>();
        relacionales.put(">", 81); relacionales.put("<", 82);
        relacionales.put("=", 83); relacionales.put(">=", 84); relacionales.put("<=", 85);

        tablaIdentificadores = new LinkedHashMap<>();
        tablaConstantes = new LinkedHashMap<>();
        listaTokens = new ArrayList<>();
        listaErrores = new ArrayList<>();
    }

    // GLC DEL DML DEL SQL
    private void inicializarTablaSintactica() {
        tablaSintactica = new HashMap<>();
        
        // Regla 300 (Q) -> 10 301 11 306 310
        agregarRegla(300, 10, new int[]{10, 301, 11, 306, 310});
        
        // Regla 301 (A) -> 302 | 72
        agregarRegla(301, 400, new int[]{302}); 
        agregarRegla(301, 72, new int[]{72});   
        
        // Regla 302 (B) -> 304 303
        agregarRegla(302, 400, new int[]{304, 303});
        
        // Regla 303 (D) -> 50 302 | 99
        agregarRegla(303, 50, new int[]{50, 302});
        agregarRegla(303, 11, new int[]{LAMBDA});
        
        // Regla 304 (C) -> 400 305
        agregarRegla(304, 400, new int[]{400, 305});
        
        // Regla 305 (E) -> 51 400 | 99
        agregarRegla(305, 51, new int[]{51, 400});
        int[] follow305 = {11, 13, 14, 15, 50, 53, 81, 82, 83, 84, 85, 199}; 
        for(int f : follow305) agregarRegla(305, f, new int[]{LAMBDA});

        // Regla 306 (F) -> 308 307
        agregarRegla(306, 400, new int[]{308, 307});
        
        // Regla 307 (H) -> 50 306 | 99
        agregarRegla(307, 50, new int[]{50, 306});
        int[] follow307 = {12, 53, 199};
        for(int f : follow307) agregarRegla(307, f, new int[]{LAMBDA});
        
        // Regla 308 (G) -> 400 309
        agregarRegla(308, 400, new int[]{400, 309});
        
        // Regla 309 (I) -> 400 | 99
        agregarRegla(309, 400, new int[]{400});
        int[] follow309 = {12, 50, 53, 199};
        for(int f : follow309) agregarRegla(309, f, new int[]{LAMBDA});
        
        // Regla 310 (J) -> 12 311 | 99
        agregarRegla(310, 12, new int[]{12, 311});
        int[] follow310 = {53, 199};
        for(int f : follow310) agregarRegla(310, f, new int[]{LAMBDA});
        
        // Regla 311 (K) -> 313 312
        agregarRegla(311, 400, new int[]{313, 312});
        
        // Regla 312 (V) -> 317 311 | 99
        agregarRegla(312, 14, new int[]{317, 311});
        agregarRegla(312, 15, new int[]{317, 311});
        int[] follow312 = {53, 199};
        for(int f : follow312) agregarRegla(312, f, new int[]{LAMBDA});
        
        // Regla 313 (L) -> 304 314
        agregarRegla(313, 400, new int[]{304, 314});
        
        // Regla 314 (M) -> 315 316 | 13 52 300 53
        for (int i = 81; i <= 85; i++) agregarRegla(314, i, new int[]{315, 316});
        agregarRegla(314, 13, new int[]{13, 52, 300, 53});
        
        // Regla 315 (N) -> Operadores Relacionales (81 a 85)
        for (int i = 81; i <= 85; i++) agregarRegla(315, i, new int[]{i});
        
        // Regla 316 (O) -> 304 | 54 318 54 | 319
        agregarRegla(316, 400, new int[]{304});
        agregarRegla(316, 54, new int[]{54, 318, 54});
        agregarRegla(316, 61, new int[]{319}); 
        
        // Regla 317 (P) -> 14 | 15 (AND | OR)
        agregarRegla(317, 14, new int[]{14});
        agregarRegla(317, 15, new int[]{15});
        
        // Regla 318 (R) -> 62 (Constante Alfanumérica)
        agregarRegla(318, 62, new int[]{62});
        
        // Regla 319 (T) -> 61 (Constante Numérica)
        agregarRegla(319, 61, new int[]{61});
    }
    
    private void agregarRegla(int noTerminal, int terminal, int[] produccion) {
        tablaSintactica.putIfAbsent(noTerminal, new HashMap<>());
        tablaSintactica.get(noTerminal).put(terminal, produccion);
    }

    private void configurarInterfaz() {
        setTitle("Compilador SQL DML");
        setSize(950, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panelNorte = new JPanel(new BorderLayout());
        panelNorte.setBorder(BorderFactory.createTitledBorder("Código DML"));
        txtEntrada = new JTextArea(8, 50);
        txtEntrada.setFont(new Font("Monospaced", Font.BOLD, 14));
        txtEntrada.setText("SELECT ALUMNOS.NOMBRE, CALIFICACION\n" +
                           "FROM ALUMNOS A, MATERIAS M\n" +
                           "WHERE CALIFICACION >= 70 AND TURNO = 'TM'"); 
        panelNorte.add(new JScrollPane(txtEntrada), BorderLayout.CENTER);

        JPanel panelBotones = new JPanel();
        JButton btnAnalizar = new JButton("Analizar Todo");
        JButton btnLimpiar = new JButton("Limpiar");
        panelBotones.add(btnAnalizar);
        panelBotones.add(btnLimpiar);
        panelNorte.add(panelBotones, BorderLayout.SOUTH);
        add(panelNorte, BorderLayout.NORTH);

        panelPestanas = new JTabbedPane();

        modeloTokens = new DefaultTableModel(new String[]{"No.", "Línea", "Lexema", "Tipo", "Código"}, 0);
        tablaTokens = new JTable(modeloTokens);
        panelPestanas.addTab("Tokens", new JScrollPane(tablaTokens));

        txtSintaxis = new JTextArea();
        txtSintaxis.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtSintaxis.setEditable(false);
        panelPestanas.addTab("Algoritmo LL", new JScrollPane(txtSintaxis));

        modeloIds = new DefaultTableModel(new String[]{"Identificador", "Valor (Tabla Dinámica)", "Líneas"}, 0);
        tablaIds = new JTable(modeloIds);
        panelPestanas.addTab("Tabla IDs", new JScrollPane(tablaIds));

        modeloConsts = new DefaultTableModel(new String[]{"No.", "Constante", "Tipo", "Valor (Tabla Dinámica)"}, 0);
        tablaConsts = new JTable(modeloConsts);
        panelPestanas.addTab("Tabla Constantes", new JScrollPane(tablaConsts));

        txtErrores = new JTextArea();
        txtErrores.setForeground(Color.RED);
        txtErrores.setEditable(false);
        panelPestanas.addTab("Errores", new JScrollPane(txtErrores));

        add(panelPestanas, BorderLayout.CENTER);

        btnAnalizar.addActionListener(e -> ejecutarAnalisis());
        btnLimpiar.addActionListener(e -> limpiarTodo());
    }

    private void ejecutarAnalisis() {
        limpiarResultados();
        if (analizarLexico(txtEntrada.getText())) {
            mostrarResultadosLexicos();
            analizarSintaxis();
        } else {
            mostrarResultadosLexicos();
            txtSintaxis.setText("Error léxico previo. No se ejecuta el sintáctico.");
            JOptionPane.showMessageDialog(this, "Errores Léxicos encontrados.", "Error", JOptionPane.ERROR_MESSAGE);
            panelPestanas.setSelectedIndex(4);
        }
    }

    private boolean analizarLexico(String codigo) {
        String[] lineas = codigo.split("\n");
        boolean sinErrores = true;
        
        for (int i = 0; i < lineas.length; i++) {
            int numLinea = i + 1;
            String linea = lineas[i];
            int pos = 0;

            while (pos < linea.length()) {
                char c = linea.charAt(pos);
                if (Character.isWhitespace(c)) { pos++; continue; }

                // Identificadores o Palabras Reservadas
                if (Character.isLetter(c)) {
                    StringBuilder sb = new StringBuilder();
                    while (pos < linea.length() && (Character.isLetterOrDigit(linea.charAt(pos)) || linea.charAt(pos) == '_')) {
                        sb.append(linea.charAt(pos)); pos++;
                    }
                    String palabra = sb.toString().toUpperCase();
                    if (palabrasReservadas.containsKey(palabra)) {
                        registrarToken(palabra, 1, palabrasReservadas.get(palabra), numLinea);
                    } else {
                        if (!tablaIdentificadores.containsKey(palabra)) 
                            tablaIdentificadores.put(palabra, new IdEntry(palabra, idCounter++, numLinea));
                        else tablaIdentificadores.get(palabra).agregarLinea(numLinea);
                        
                        registrarToken(palabra, 4, 400, numLinea);
                    }
                    continue;
                }

                // Constantes Numéricas
                if (Character.isDigit(c)) {
                    StringBuilder sb = new StringBuilder();
                    while (pos < linea.length() && Character.isDigit(linea.charAt(pos))) {
                        sb.append(linea.charAt(pos)); pos++;
                    }
                    String num = sb.toString();
                    if (!tablaConstantes.containsKey(num)) 
                        tablaConstantes.put(num, new ConstEntry(tokenNo, num, 61, constCounter++));
                    registrarToken(num, 6, 61, numLinea); // Se manda el código de Terminal 61
                    continue;
                }
                
                // Cadenas Alfanuméricas
                if (c == '\'') {
                    registrarToken("'", 5, 54, numLinea); // Abre comilla
                    pos++;
                    StringBuilder sb = new StringBuilder();
                    while (pos < linea.length() && linea.charAt(pos) != '\'') {
                        sb.append(linea.charAt(pos)); pos++;
                    }
                    String cad = sb.toString();
                    if (!tablaConstantes.containsKey(cad))
                        tablaConstantes.put(cad, new ConstEntry(tokenNo, cad, 62, constCounter++));
                    
                    registrarToken(cad, 6, 62, numLinea);
                    
                    if (pos < linea.length() && linea.charAt(pos) == '\'') {
                        registrarToken("'", 5, 54, numLinea);
                        pos++;
                    } else {
                        listaErrores.add("Línea " + numLinea + ": Cadena no cerrada.");
                        sinErrores = false;
                    }
                    continue;
                }

                // Relacionales
                String s = String.valueOf(c);
                if (relacionales.containsKey(s) || (pos+1 < linea.length() && relacionales.containsKey(s + linea.charAt(pos+1)))) {
                     if (pos+1 < linea.length() && relacionales.containsKey(s + linea.charAt(pos+1))) {
                         registrarToken(s + linea.charAt(pos+1), 8, relacionales.get(s + linea.charAt(pos+1)), numLinea);
                         pos += 2;
                     } else {
                         registrarToken(s, 8, relacionales.get(s), numLinea);
                         pos++;
                     }
                     continue;
                }
                
                // Delimitadores y Operadores
                if (delimitadores.containsKey(s)) {
                    registrarToken(s, 5, delimitadores.get(s), numLinea); pos++; continue;
                }
                if (operadores.containsKey(s)) {
                    registrarToken(s, 7, operadores.get(s), numLinea); pos++; continue;
                }

                listaErrores.add("Error Léxico Línea " + numLinea + ": Símbolo no reconocido '" + c + "'");
                sinErrores = false; pos++;
            }
        }
        return sinErrores;
    }

    private void analizarSintaxis() {
        StringBuilder log = new StringBuilder();
        log.append(String.format("%-60s | %-15s | %s\n", "PILA", "ENTRADA (K)", "ACCIÓN"));
        log.append("-".repeat(110)).append("\n");

        pila = new Stack<>();
        pila.push(PESOS);
        pila.push(SIMBOLO_INICIAL);

        List<TokenEntry> flujo = new ArrayList<>(listaTokens);
        flujo.add(new TokenEntry(0, 0, "$", 0, PESOS)); 

        int apun = 0;
        boolean error = false;
        boolean aceptacion = false;

        while (!pila.isEmpty() && !error && !aceptacion) {
            int X = pila.peek(); 
            TokenEntry token = flujo.get(apun);
            int K = token.codigo; 

            String snapshotPila = formatStack(pila);
            pila.pop(); // Pila

            if (esTerminal(X) || X == PESOS) {
                if (X == PESOS && K == PESOS) {
                    log.append(String.format("%-60s | %-15s | ACEPTACIÓN\n", snapshotPila, token.lexema));
                    aceptacion = true;
                } 
                else if (X == K) { 
                    log.append(String.format("%-60s | %-15s | Coincide terminal %d\n", snapshotPila, token.lexema, K));
                    apun++;
                } else {
                    log.append(String.format("%-60s | %-15s | ERROR: Esperaba %d\n", snapshotPila, token.lexema, X));
                    listaErrores.add("Error Sintáctico cerca de '" + token.lexema + "'. Se esperaba terminal: " + X);
                    error = true;
                }
            } 
            else { 
                if (tablaSintactica.containsKey(X) && tablaSintactica.get(X).containsKey(K)) {
                    int[] produccion = tablaSintactica.get(X).get(K);
                    
                    if (produccion.length == 1 && produccion[0] == LAMBDA) {
                        log.append(String.format("%-60s | %-15s | Expande %d -> λ (Lambda)\n", snapshotPila, token.lexema, X));
                    } else {
                        log.append(String.format("%-60s | %-15s | Expande %d -> %s\n", snapshotPila, token.lexema, X, Arrays.toString(produccion)));
                        for (int i = produccion.length - 1; i >= 0; i--) {
                            pila.push(produccion[i]); // Insertar Inverso
                        }
                    }
                } else {
                    log.append(String.format("%-60s | %-15s | ERROR: Tabla[%d, %d] Vacía\n", snapshotPila, token.lexema, X, K));
                    listaErrores.add("Error Sintáctico: No hay regla para el componente '" + token.lexema + "'.");
                    error = true;
                }
            }
        } 

        txtSintaxis.setText(log.toString());
        
        if (error) {
            mostrarResultadosLexicos(); // Actualiza tabla de errores
            JOptionPane.showMessageDialog(this, "Análisis fallido. Revisa la Pestaña Errores.", "Error Sintáctico", JOptionPane.ERROR_MESSAGE);
            panelPestanas.setSelectedIndex(4);
        } else {
            JOptionPane.showMessageDialog(this, "¡Sentencia SQL DML Correcta!", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            panelPestanas.setSelectedIndex(1); // Muestra algoritmo exitoso
        }
    }

    private boolean esTerminal(int codigo) { return codigo < 300; }

    private String formatStack(Stack<Integer> s) {
        StringBuilder sb = new StringBuilder();
        for(int n : s) sb.append(n).append(" ");
        String res = sb.toString().trim();
        return res.length() > 58 ? "..." + res.substring(res.length() - 55) : res;
    }

    private void registrarToken(String lexema, int tipo, int codigo, int linea) {
        listaTokens.add(new TokenEntry(tokenNo++, linea, lexema, tipo, codigo));
    }

    private void mostrarResultadosLexicos() {
        for (TokenEntry t : listaTokens) modeloTokens.addRow(new Object[]{t.no, t.linea, t.lexema, t.tipo, t.codigo});
        for (IdEntry id : tablaIdentificadores.values()) modeloIds.addRow(new Object[]{id.nombre, id.valor, id.getLineasComoString()});
        for (ConstEntry c : tablaConstantes.values()) modeloConsts.addRow(new Object[]{c.noAparicion, c.valorStr, c.tipo, c.valor});
        
        StringBuilder err = new StringBuilder();
        for (String e : listaErrores) err.append(e).append("\n");
        txtErrores.setText(err.toString());
    }

    private void limpiarResultados() {
        modeloTokens.setRowCount(0); modeloIds.setRowCount(0); modeloConsts.setRowCount(0);
        txtErrores.setText(""); txtSintaxis.setText("");
        listaTokens.clear(); tablaIdentificadores.clear(); tablaConstantes.clear(); listaErrores.clear();
        idCounter = 401; constCounter = 601; tokenNo = 1;
    }

    private void limpiarTodo() { txtEntrada.setText(""); limpiarResultados(); }

    class TokenEntry {
        int no, linea, tipo, codigo; String lexema;
        public TokenEntry(int n, int l, String lx, int t, int c) { no=n; linea=l; lexema=lx; tipo=t; codigo=c; }
    }
    class IdEntry {
        String nombre; int valor; Set<Integer> lineas = new LinkedHashSet<>();
        public IdEntry(String n, int v, int l) { nombre=n; valor=v; lineas.add(l); }
        public void agregarLinea(int l) { lineas.add(l); }
        public String getLineasComoString() { return lineas.toString(); }
    }
    class ConstEntry {
        int noAparicion, tipo, valor; String valorStr;
        public ConstEntry(int n, String s, int t, int v) { noAparicion=n; valorStr=s; tipo=t; valor=v; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ScannerDML().setVisible(true));
    }
}