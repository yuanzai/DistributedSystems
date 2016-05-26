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
    public Address(String host, int port, String name, String region){
        this.host = host;
        this.port = port;
        this.name = name;
        this.region = region;

    }

    @Override
    public boolean equals(Object obj) {
        if (name != null){
            if (((Address) obj).name != null){
                return (name.equals(((Address) obj).name));
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }
}
