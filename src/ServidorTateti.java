import java.io.*;
import java.net.*;

public class ServidorTateti {
    private static final int PUERTO = 12345;
    private static char[][] tablero;
    private static int movimientos;

    // Colores ANSI para la consola
    private static final String RESET = "\u001B[0m";
    private static final String ROJO = "\u001B[31m";
    private static final String AZUL = "\u001B[34m";

    public static void main(String[] args) {
        System.out.println("=== SERVIDOR DE TA-TE-TI INICIADO ===");
        
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            while (true) {
                System.out.println("Esperando Jugador 1...");
                Socket socket1 = serverSocket.accept();
                System.out.println("¡Jugador 1 conectado! Esperando oponente...");

                System.out.println("Esperando Jugador 2...");
                Socket socket2 = serverSocket.accept();
                System.out.println("¡Jugador 2 conectado!");

                // --- MEJORA 1: SORTEO RANDOM DE QUIÉN ES X Y QUIÉN ES O ---
                Socket jX, jO;
                if (Math.random() < 0.5) {
                    jX = socket1;
                    jO = socket2;
                    System.out.println("Sorteo de fichas: Jugador 1 es X, Jugador 2 es O");
                } else {
                    jX = socket2;
                    jO = socket1;
                    System.out.println("Sorteo de fichas: Jugador 2 es X, Jugador 1 es O");
                }

                PrintWriter salidaX = new PrintWriter(jX.getOutputStream(), true);
                BufferedReader entradaX = new BufferedReader(new InputStreamReader(jX.getInputStream()));
                salidaX.println("INFO 🎲 ¡Sorteo de fichas completado! Te tocó ser el JUGADOR 1 (" + ROJO + "X" + RESET + ").");

                PrintWriter salidaO = new PrintWriter(jO.getOutputStream(), true);
                BufferedReader entradaO = new BufferedReader(new InputStreamReader(jO.getInputStream()));
                salidaO.println("INFO 🎲 ¡Sorteo de fichas completado! Te tocó ser el JUGADOR 2 (" + AZUL + "O" + RESET + ").");

                salidaX.println("INFO ¡Ambos conectados! La partida comienza ahora.");
                salidaO.println("INFO ¡Ambos conectados! La partida comienza ahora.");
                
                // Iniciamos la partida pasando las referencias de quién es X y quién O
                Thread partida = new Thread(new ManejadorPartida(jX, jO, entradaX, salidaX, entradaO, salidaO));
                partida.start();
            }
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    private static class ManejadorPartida implements Runnable {
        private Socket jX, jO;
        private BufferedReader entradaX, entradaO;
        private PrintWriter salidaX, salidaO;

        public ManejadorPartida(Socket jX, Socket jO, BufferedReader eX, PrintWriter sX, BufferedReader eO, PrintWriter sO) {
            this.jX = jX; this.jO = jO;
            this.entradaX = eX; this.salidaX = sX;
            this.entradaO = eO; this.salidaO = sO;
        }

        @Override
        public void run() {
            try {
                boolean continuarJugando = true;

                while (continuarJugando) {
                    inicializarTablero();
                    boolean juegoActivo = true;
                    char turnoActual = 'X'; // Siempre arranca la X, pero la ficha ya fue sorteada al azar al inicio

                    while (juegoActivo) {
                        enviarTablero(salidaX);
                        enviarTablero(salidaO);

                        PrintWriter salidaTurno = (turnoActual == 'X') ? salidaX : salidaO;
                        PrintWriter salidaEspera = (turnoActual == 'X') ? salidaO : salidaX;
                        BufferedReader entradaTurno = (turnoActual == 'X') ? entradaX : entradaO;

                        String colorFicha = (turnoActual == 'X') ? ROJO + "X" + RESET : AZUL + "O" + RESET;
                        salidaTurno.println("TURNO Es tu turno (" + colorFicha + "). Escribí el número de casilla (1-9):");
                        salidaEspera.println("ESPERA Esperando que juegue el oponente...");

                        boolean jugadaValida = false;
                        while (!jugadaValida) {
                            String jugadaStr = entradaTurno.readLine();
                            if (jugadaStr == null) throw new IOException("Desconexión");

                            jugadaStr = jugadaStr.trim();
                            if (jugadaStr.isEmpty()) continue;

                            try {
                                int casilla = Integer.parseInt(jugadaStr);
                                if (marcarCasilla(casilla, turnoActual)) {
                                    movimientos++;
                                    jugadaValida = true;

                                    if (verificarGanador(turnoActual)) {
                                        enviarTablero(salidaX);
                                        enviarTablero(salidaO);
                                        salidaX.println("FIN ¡¡¡ GANÓ EL JUGADOR " + colorFicha + " !!!");
                                        salidaO.println("FIN ¡¡¡ GANÓ EL JUGADOR " + colorFicha + " !!!");
                                        juegoActivo = false;
                                    } else if (movimientos == 9) {
                                        enviarTablero(salidaX);
                                        enviarTablero(salidaO);
                                        salidaX.println("FIN ¡Es un EMPATE!");
                                        salidaO.println("FIN ¡Es un EMPATE!");
                                        juegoActivo = false;
                                    } else {
                                        turnoActual = (turnoActual == 'X') ? 'O' : 'X';
                                    }
                                } else {
                                    salidaTurno.println("ERROR ¡Casilla inválida u ocupada! Intentá de nuevo:");
                                }
                            } catch (NumberFormatException e) {
                                salidaTurno.println("ERROR Por favor ingresá un número válido (1-9):");
                            }
                        }
                    }

                    // --- MEJORA 2: SISTEMA DE REVANCHA ASÍNCRONO SIN ESPERAS INNECESARIAS ---
                    salidaX.println("REVANCHA ¿Querés jugar una revancha? (S/N):");
                    salidaO.println("REVANCHA ¿Querés jugar una revancha? (S/N):");

                    // El servidor lee las respuestas de forma independiente para evitar bloqueos
                    String rX = entradaX.readLine();
                    if (rX == null) throw new IOException("Desconexión");
                    rX = rX.trim().toUpperCase();

                    // Si el jugador X rechaza de inmediato, no esperamos al O
                    if (rX.equals("N")) {
                        salidaX.println("DESCONECTAR Gracias por jugar. ¡Adiós!");
                        salidaO.println("DESCONECTAR 🚫 Tu oponente no quiso tener una revancha. ¡Gracias por jugar!");
                        continuarJugando = false;
                        continue; 
                    }

                    String rO = entradaO.readLine();
                    if (rO == null) throw new IOException("Desconexión");
                    rO = rO.trim().toUpperCase();

                    // Si el jugador O rechaza, le avisamos al X
                    if (rO.equals("N")) {
                        salidaX.println("DESCONECTAR 🚫 Tu oponente no quiso tener una revancha. ¡Gracias por jugar!");
                        salidaO.println("DESCONECTAR Gracias por jugar. ¡Adiós!");
                        continuarJugando = false;
                        continue;
                    }

                    // Si ambos presionaron "S" u otra cosa que no sea N, la partida vuelve a iniciar
                    if (rX.equals("S") && rO.equals("S")) {
                        salidaX.println("INFO ¡Revancha aceptada! Reiniciando el tablero...");
                        salidaO.println("INFO ¡Revancha aceptada! Reiniciando el tablero...");
                    } else {
                        salidaX.println("DESCONECTAR Opción inválida detectada. Cerrando juego.");
                        salidaO.println("DESCONECTAR Opción inválida detectada. Cerrando juego.");
                        continuarJugando = false;
                    }
                }
            } catch (IOException e) {
                System.out.println("Un jugador se desconectó de forma repentina.");
                try {
                    salidaX.println("DESCONECTAR Tu oponente abandonó la partida o se cerró la conexión.");
                } catch (Exception ex) {}
                try {
                    salidaO.println("DESCONECTAR Tu oponente abandonó la partida o se cerró la conexión.");
                } catch (Exception ex) {}
            } finally {
                try { jX.close(); jO.close(); } catch (IOException e) {}
            }
        }

        private void inicializarTablero() {
            tablero = new char[][]{
                {'1', '2', '3'},
                {'4', '5', '6'},
                {'7', '8', '9'}
            };
            movimientos = 0;
        }

        private void enviarTablero(PrintWriter salida) {
            salida.println("TABLERO_INICIO");
            for (int i = 0; i < 3; i++) {
                String l = " " + formatearFicha(tablero[i][0]) + " | " + formatearFicha(tablero[i][1]) + " | " + formatearFicha(tablero[i][2]);
                salida.println(l);
                if (i < 2) salida.println("-----------");
            }
        }

        private String formatearFicha(char c) {

            if (c == 'X') return ROJO + "X" + RESET;
            if (c == 'O') return AZUL + "O" + RESET;
            return String.valueOf(c);
        }

        private boolean marcarCasilla(int casilla, char jugador) {
            if (casilla < 1 || casilla > 9) return false;
            int fila = (casilla - 1) / 3;
            int col = (casilla - 1) % 3;
            if (tablero[fila][col] != 'X' && tablero[fila][col] != 'O') {
                tablero[fila][col] = jugador;
                return true;
            }
            return false;
        }

        private boolean verificarGanador(char j) {
            for (int i = 0; i < 3; i++) {
                if (tablero[i][0] == j && tablero[i][1] == j && tablero[i][2] == j) return true;
                if (tablero[0][i] == j && tablero[1][i] == j && tablero[2][i] == j) return true;
            }
            if (tablero[0][0] == j && tablero[1][1] == j && tablero[2][2] == j) return true;
            if (tablero[0][2] == j && tablero[1][1] == j && tablero[2][0] == j) return true;
            return false;
        }
    }
}
