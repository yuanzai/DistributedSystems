/**
 * Created by junyuanlau on 28/4/16.
 */
public class Address {
    int port;
    String host;
    String region;
    String name;
    public Address(String name, String region){
        this.name = name;
        this.region = region;
    }
    public Address(String host, int port){
        this.host = host;
        this.port = port;
    }


}
