import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteTateti {
    private static final String IP_SERVIDOR = "localhost";
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
                    // Imprime de forma exacta las 5 líneas limpias del tablero
                    System.out.println();
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
                else if (linea.startsWith("FIN")) {
                    System.out.println("\n🏁 === JUEGO TERMINADO ===");
                    System.out.println(linea.substring(4));
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("\nPartida terminada o desconectada del servidor.");
        }
    }
}
