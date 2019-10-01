import client.Client;

import java.io.IOException;

public class ClientApp3 {

    public static void main(String[] args) throws IOException, InterruptedException {
        Client client = new Client("127.0.0.1", 55286);
        client.run();
    }
}
