import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ScannerDML extends JFrame {

    // Componentes de Interfaz (Módulo de Resultados y Entrada)
    private JTextArea txtEntrada;
    private JTable tablaTokens, tablaIds, tablaConsts;
    private DefaultTableModel modeloTokens, modeloIds, modeloConsts;
    private JTextArea txtErrores;
    private JTabbedPane panelPestanas;

    // Diccionarios y estructuras de datos (Módulo de Análisis)
    private Map<String, Integer> palabrasReservadas;
    private Map<String, Integer> delimitadores;
    private Map<String, Integer> operadores;
    private Map<String, Integer> relacionales;

    // Tablas de Símbolos Dinámicas
    private Map<String, IdEntry> tablaIdentificadores;
    private Map<String, ConstEntry> tablaConstantes;
    private List<TokenEntry> listaTokens;
    private List<String> listaErrores;

    // Contadores para códigos dinámicos
    private int idCounter = 401;
    private int constCounter = 600;
    private int tokenNo = 1;

    public ScannerDML() {
        inicializarDiccionarios();
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

    private void configurarInterfaz() {
        setTitle("Módulo A1. Escáner DML SQL");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Módulo de Entrada ---
        JPanel panelNorte = new JPanel(new BorderLayout());
        panelNorte.setBorder(BorderFactory.createTitledBorder("Módulo de Entrada (Código DML)"));
        txtEntrada = new JTextArea(8, 50);
        txtEntrada.setFont(new Font("Monospaced", Font.PLAIN, 14));
        txtEntrada.setText("SELECT ANOMBRE, CALIFICACION, TURNO\n" +
                           "FROM ALUMNOS, INSCRITOS, MATERIAS, CARRERAS\n" +
                           "WHERE MNOMBRE='LENAUT2' AND TURNO = 'TM'\n" +
                           "AND CNOMBRE='ISC' AND SEMESTRE='2023I' AND CALIFICACION >= 70");
        panelNorte.add(new JScrollPane(txtEntrada), BorderLayout.CENTER);

        JPanel panelBotones = new JPanel();
        JButton btnAnalizar = new JButton("Analizar Código");
        JButton btnLimpiar = new JButton("Limpiar");
        panelBotones.add(btnAnalizar);
        panelBotones.add(btnLimpiar);
        panelNorte.add(panelBotones, BorderLayout.SOUTH);

        add(panelNorte, BorderLayout.NORTH);

        // --- Módulo de Resultados y Errores ---
        panelPestanas = new JTabbedPane();

        // Tabla Salida (Tokens)
        modeloTokens = new DefaultTableModel(new String[]{"No.", "Línea", "TOKEN", "Tipo", "Código"}, 0);
        tablaTokens = new JTable(modeloTokens);
        panelPestanas.addTab("Salida (Tokens)", new JScrollPane(tablaTokens));

        // Tabla Identificadores
        modeloIds = new DefaultTableModel(new String[]{"Identificador", "Valor", "Línea"}, 0);
        tablaIds = new JTable(modeloIds);
        panelPestanas.addTab("Tabla de Identificadores", new JScrollPane(tablaIds));

        // Tabla Constantes
        modeloConsts = new DefaultTableModel(new String[]{"No.", "Constante", "Tipo", "Valor"}, 0);
        tablaConsts = new JTable(modeloConsts);
        panelPestanas.addTab("Tabla de Constantes", new JScrollPane(tablaConsts));

        // Módulo de Errores
        txtErrores = new JTextArea();
        txtErrores.setForeground(Color.RED);
        txtErrores.setEditable(false);
        panelPestanas.addTab("Módulo de Errores", new JScrollPane(txtErrores));

        add(panelPestanas, BorderLayout.CENTER);

        // Eventos
        btnAnalizar.addActionListener(e -> ejecutarAnalisis());
        btnLimpiar.addActionListener(e -> limpiarTodo());
    }

    // --- Módulo de Análisis ---
    private void ejecutarAnalisis() {
        try {
            limpiarResultados();
            String codigo = txtEntrada.getText();
            analizarLexico(codigo);
            mostrarResultados();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error interno durante el análisis: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void analizarLexico(String codigo) {
        String[] lineas = codigo.split("\n");
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
                        sb.append(linea.charAt(pos));
                        pos++;
                    }
                    String palabra = sb.toString().toUpperCase();

                    if (palabrasReservadas.containsKey(palabra)) {
                        registrarToken(palabra, 1, palabrasReservadas.get(palabra), numLinea);
                    } else {
                        if (!tablaIdentificadores.containsKey(palabra)) {
                            tablaIdentificadores.put(palabra, new IdEntry(palabra, idCounter++, numLinea));
                        } else {
                            tablaIdentificadores.get(palabra).agregarLinea(numLinea);
                        }
                        registrarToken(palabra, 4, tablaIdentificadores.get(palabra).valor, numLinea);
                    }
                    continue;
                }

                // Constantes Numéricas
                if (Character.isDigit(c)) {
                    StringBuilder sb = new StringBuilder();
                    while (pos < linea.length() && Character.isDigit(linea.charAt(pos))) {
                        sb.append(linea.charAt(pos));
                        pos++;
                    }
                    String numero = sb.toString();
                    if (!tablaConstantes.containsKey(numero)) {
                        tablaConstantes.put(numero, new ConstEntry(tokenNo, numero, 61, constCounter++));
                    }
                    registrarToken(numero, 6, tablaConstantes.get(numero).valor, numLinea);
                    continue;
                }

                // Constantes Alfanuméricas (Cadenas)
                if (c == '\'') {
                    StringBuilder sb = new StringBuilder();
                    pos++; // saltar comilla de apertura
                    while (pos < linea.length() && linea.charAt(pos) != '\'') {
                        sb.append(linea.charAt(pos));
                        pos++;
                    }
                    if (pos < linea.length() && linea.charAt(pos) == '\'') {
                        pos++; // saltar comilla de cierre
                    } else {
                        listaErrores.add("Línea " + numLinea + ": Cadena no cerrada.");
                    }
                    String cadena = sb.toString();
                    if (!tablaConstantes.containsKey(cadena)) {
                        tablaConstantes.put(cadena, new ConstEntry(tokenNo, cadena, 62, constCounter++));
                    }
                    // Para coincidir con la imagen, las cadenas se listan como la palabra "CONSTANTE" en los tokens
                    registrarToken("CONSTANTE", 6, tablaConstantes.get(cadena).valor, numLinea);
                    continue;
                }

                // Operadores Relacionales
                if (c == '>' || c == '<' || c == '=') {
                    if (pos + 1 < linea.length() && linea.charAt(pos + 1) == '=') {
                        String op = "" + c + "=";
                        registrarToken(op, 8, relacionales.get(op), numLinea);
                        pos += 2;
                    } else {
                        String op = "" + c;
                        registrarToken(op, 8, relacionales.get(op), numLinea);
                        pos++;
                    }
                    continue;
                }

                // Delimitadores
                if (delimitadores.containsKey(String.valueOf(c))) {
                    registrarToken(String.valueOf(c), 5, delimitadores.get(String.valueOf(c)), numLinea);
                    pos++;
                    continue;
                }

                // Operadores Aritméticos
                if (operadores.containsKey(String.valueOf(c))) {
                    registrarToken(String.valueOf(c), 7, operadores.get(String.valueOf(c)), numLinea);
                    pos++;
                    continue;
                }

                // Carácter no reconocido -> Módulo de Errores
                listaErrores.add("Error Léxico en línea " + numLinea + ": Carácter no reconocido '" + c + "'");
                pos++;
            }
        }
    }

    private void registrarToken(String lexema, int tipo, int codigo, int linea) {
        listaTokens.add(new TokenEntry(tokenNo++, linea, lexema, tipo, codigo));
    }

    // --- Módulo de Resultados ---
    private void mostrarResultados() {
        // Llenar tabla de Salida (Tokens)
        for (TokenEntry t : listaTokens) {
            modeloTokens.addRow(new Object[]{t.no, t.linea, t.lexema, t.tipo, t.codigo});
        }

        // Llenar tabla de Identificadores
        for (IdEntry id : tablaIdentificadores.values()) {
            modeloIds.addRow(new Object[]{id.nombre, id.valor, id.getLineasComoString()});
        }

        // Llenar tabla de Constantes
        for (ConstEntry c : tablaConstantes.values()) {
            modeloConsts.addRow(new Object[]{c.noAparicion, c.valorStr, c.tipo, c.valor});
        }

        // Mostrar Errores si los hay
        if (!listaErrores.isEmpty()) {
            StringBuilder errStr = new StringBuilder("Se encontraron errores léxicos:\n");
            for (String err : listaErrores) {
                errStr.append("- ").append(err).append("\n");
            }
            txtErrores.setText(errStr.toString());
            panelPestanas.setSelectedIndex(3); // Saltar a la pestaña de errores
            JOptionPane.showMessageDialog(this, "El análisis finalizó con errores. Revisa el Módulo de Errores.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        } else {
            txtErrores.setText("No se encontraron errores léxicos. Análisis exitoso.");
            panelPestanas.setSelectedIndex(0); // Quedarse en la tabla de tokens
        }
    }

    private void limpiarResultados() {
        modeloTokens.setRowCount(0);
        modeloIds.setRowCount(0);
        modeloConsts.setRowCount(0);
        txtErrores.setText("");
        listaTokens.clear();
        tablaIdentificadores.clear();
        tablaConstantes.clear();
        listaErrores.clear();
        idCounter = 401;
        constCounter = 600;
        tokenNo = 1;
    }

    private void limpiarTodo() {
        txtEntrada.setText("");
        limpiarResultados();
    }

    // --- Clases Auxiliares para Estandarizar Tablas ---
    class TokenEntry {
        int no, linea, tipo, codigo;
        String lexema;
        public TokenEntry(int no, int linea, String lexema, int tipo, int codigo) {
            this.no = no; this.linea = linea; this.lexema = lexema; this.tipo = tipo; this.codigo = codigo;
        }
    }

    class IdEntry {
        String nombre;
        int valor;
        Set<Integer> lineas; // Usamos un Set para no repetir el número de línea
        public IdEntry(String nombre, int valor, int primeraLinea) {
            this.nombre = nombre; this.valor = valor;
            this.lineas = new LinkedHashSet<>();
            this.lineas.add(primeraLinea);
        }
        public void agregarLinea(int linea) { this.lineas.add(linea); }
        public String getLineasComoString() {
            List<String> strs = new ArrayList<>();
            for (Integer l : lineas) strs.add(String.valueOf(l));
            return String.join(", ", strs);
        }
    }

    class ConstEntry {
        int noAparicion, tipo, valor;
        String valorStr;
        public ConstEntry(int noAparicion, String valorStr, int tipo, int valor) {
            this.noAparicion = noAparicion; this.valorStr = valorStr; this.tipo = tipo; this.valor = valor;
        }
    }

    public static void main(String[] args) {
        try {
            // Darle un estilo nativo de Windows/Mac dependiendo del SO
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new ScannerDML().setVisible(true);
        });
    }
}