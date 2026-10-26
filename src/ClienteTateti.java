import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteTateti {
    private static final String IP_SERVIDOR = "130.10.2.10";
    private static final int PUERTO = 12345;

    public static void main(String[] args) {
        System.out.println("=== CLIENTE TA-TE-TI ===");
        
        try (Socket socket = new Socket(IP_SERVIDOR, PUERTO);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (linea.equals("TABLERO_INICIO")) {
                    limpiarConsola();
                    // Imprime las 5 líneas del tablero con sus colores correspondientes
                    for (int i = 0; i < 5; i++) {
                        System.out.println(entrada.readLine());
                    }
                    System.out.println();
                } 
                else if (linea.startsWith("INFO")) {
                    System.out.println(linea.substring(5));
                } 
                else if (linea.startsWith("ESPERA")) {
                    System.out.println(linea.substring(7));
                } 
                else if (linea.startsWith("TURNO")) {
                    System.out.print(linea.substring(6) + " ");
                    String jugada = scanner.nextLine();
                    salida.println(jugada);
                } 
                else if (linea.startsWith("ERROR")) {
                    System.out.print("❌ " + linea.substring(6) + " ");
                    String jugada = scanner.nextLine();
                    salida.println(jugada);
                } 
                else if (linea.startsWith("REVANCHA")) {
                    System.out.print("\n🔄 " + linea.substring(9) + " ");
                    String respuesta = scanner.nextLine();
                    salida.println(respuesta);
                }
                else if (linea.startsWith("FIN")) {
                    System.out.println("\n🏁 === JUEGO TERMINADO ===");
                    System.out.println(linea.substring(4));
                }
                else if (linea.startsWith("DESCONECTAR")) {
                    System.out.println("\n🔌 " + linea.substring(12));
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("\nConexión perdida con el servidor.");
        }
    }

    // Método para limpiar la consola según el sistema operativo (Windows / Linux / Mac)
    private static void limpiarConsola() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            System.out.println(); // Si falla, solo deja un espacio
        }
    }
}
