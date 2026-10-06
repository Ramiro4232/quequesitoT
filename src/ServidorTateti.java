import java.io.*;
import java.net.*;

public class ServidorTateti {
    private static final int PUERTO = 12345;
    private static char[][] tablero = {
        {'1', '2', '3'},
        {'4', '5', '6'},
        {'7', '8', '9'}
    };

    public static void main(String[] args) {
        System.out.println("=== SERVIDOR DE TA-TE-TI INICIADO ===");
        
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Esperando Jugador 1 (X)...");
            Socket jugador1 = serverSocket.accept();
            PrintWriter salidaJ1 = new PrintWriter(jugador1.getOutputStream(), true);
            BufferedReader entradaJ1 = new BufferedReader(new InputStreamReader(jugador1.getInputStream()));
            salidaJ1.println("INFO ¡Bienvenido! Sos el JUGADOR 1 (X). Esperando oponente...");

            System.out.println("Esperando Jugador 2 (O)...");
            Socket jugador2 = serverSocket.accept();
            PrintWriter salidaJ2 = new PrintWriter(jugador2.getOutputStream(), true);
            BufferedReader entradaJ2 = new BufferedReader(new InputStreamReader(jugador2.getInputStream()));
            salidaJ2.println("INFO ¡Bienvenido! Sos el JUGADOR 2 (O). Iniciando partida...");

            salidaJ1.println("INFO ¡Ambos conectados! La partida comienza ahora.");
            salidaJ2.println("INFO ¡Ambos conectados! La partida comienza ahora.");
            
            Thread partida = new Thread(() -> {
                try {
                    boolean juegoActivo = true;
                    char turnoActual = 'X';
                    int movimientos = 0;
                    
                    while (juegoActivo) {
                        // Enviamos señal de actualizar tablero (mandamos 5 líneas fijas)
                        enviarTablero(salidaJ1);
                        enviarTablero(salidaJ2);
                        
                        PrintWriter salidaTurno = (turnoActual == 'X') ? salidaJ1 : salidaJ2;
                        PrintWriter salidaEspera = (turnoActual == 'X') ? salidaJ2 : salidaJ1;
                        BufferedReader entradaTurno = (turnoActual == 'X') ? entradaJ1 : entradaJ2;
                        
                        salidaTurno.println("TURNO Escribí el número de casilla (1-9):");
                        salidaEspera.println("ESPERA Esperando que juegue el oponente...");
                        
                        boolean jugadaValida = false;
                        while (!jugadaValida) {
                            String jugadaStr = entradaTurno.readLine();
                            if (jugadaStr == null) return;
                            
                            try {
                                int casilla = Integer.parseInt(jugadaStr.trim());
                                if (marcarCasilla(casilla, turnoActual)) {
                                    movimientos++;
                                    jugadaValida = true;
                                    
                                    if (verificarGanador(turnoActual)) {
                                        enviarTablero(salidaJ1);
                                        enviarTablero(salidaJ2);
                                        salidaJ1.println("FIN ¡¡¡ GANÓ EL JUGADOR " + turnoActual + " !!!");
                                        salidaJ2.println("FIN ¡¡¡ GANÓ EL JUGADOR " + turnoActual + " !!!");
                                        juegoActivo = false;
                                    } else if (movimientos == 9) {
                                        enviarTablero(salidaJ1);
                                        enviarTablero(salidaJ2);
                                        salidaJ1.println("FIN ¡Es un EMPATE!");
                                        salidaJ2.println("FIN ¡Es un EMPATE!");
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
                } catch (Exception e) {
                    System.out.println("Partida interrumpida.");
                } finally {
                    try {
                        jugador1.close();
                        jugador2.close();
                    } catch (IOException e) {}
                }
            });
            
            partida.start();
            
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void enviarTablero(PrintWriter salida) {
        salida.println("TABLERO_INICIO");
        salida.println(" " + tablero[0][0] + " | " + tablero[0][1] + " | " + tablero[0][2]);
        salida.println("-----------");
        salida.println(" " + tablero[1][0] + " | " + tablero[1][1] + " | " + tablero[1][2]);
        salida.println("-----------");
        salida.println(" " + tablero[2][0] + " | " + tablero[2][1] + " | " + tablero[2][2]);
    }

    private static boolean marcarCasilla(int casilla, char jugador) {
        if (casilla < 1 || casilla > 9) return false;
        int fila = (casilla - 1) / 3;
        int col = (casilla - 1) % 3;
        if (tablero[fila][col] != 'X' && tablero[fila][col] != 'O') {
            tablero[fila][col] = jugador;
            return true;
        }
        return false;
    }

    private static boolean verificarGanador(char j) {
        for (int i = 0; i < 3; i++) {
            if (tablero[i][0] == j && tablero[i][1] == j && tablero[i][2] == j) return true;
            if (tablero[0][i] == j && tablero[1][i] == j && tablero[2][i] == j) return true;
        }
        if (tablero[0][0] == j && tablero[1][1] == j && tablero[2][2] == j) return true;
        if (tablero[0][2] == j && tablero[1][1] == j && tablero[2][0] == j) return true;
        return false;
    }
}
