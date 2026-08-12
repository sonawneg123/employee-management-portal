import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        System.out.println("Admin: " + encoder.encode("Admin@1234!"));
        System.out.println("HR: " + encoder.encode("HR@1234!"));
        System.out.println("Manager: " + encoder.encode("Manager@1234!"));
    }
}