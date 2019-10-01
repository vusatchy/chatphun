import server.Server;

import java.io.IOException;

public class ServerApp {

    public static void main(String[] args) throws IOException {
        Server server = new Server("127.0.0.1" , 55286);
        server.run();
    }
}
