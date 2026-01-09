import java.util.*;
import java.time.*;
import java.time.temporal.ChronoUnit;

class Location {
    double lat, lon;
    Location(double a, double b) { lat = a; lon = b; }
}

enum UserType { DONOR, RECIPIENT }
enum Status { PENDING, MATCHED, COMPLETED }

class User {
    int id;
    String name, contact, medicine;
    Location location;
    UserType type;
    int quantity;
    LocalDate expiry;
    int urgency;
    Status status;
    User(int i) { id = i; status = Status.PENDING; }
}

class Match {
    User donor, recipient;
    double score;
    Match(User d, User r, double s) { donor = d; recipient = r; score = s; }
}

class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean end;
    String word;
}

class Trie {
    TrieNode root = new TrieNode();
    void insert(String s) {
        TrieNode cur = root;
        String l = s.toLowerCase();
        for (char c : l.toCharArray()) {
            cur.children.putIfAbsent(c, new TrieNode());
            cur = cur.children.get(c);
        }
        cur.end = true;
        cur.word = s;
    }
    List<String> search(String p) {
        TrieNode cur = root;
        List<String> res = new ArrayList<>();
        String l = p.toLowerCase();
        for (char c : l.toCharArray()) {
            if (!cur.children.containsKey(c)) return res;
            cur = cur.children.get(c);
        }
        dfs(cur, res);
        return res;
    }
    void dfs(TrieNode n, List<String> r) {
        if (n.end) r.add(n.word);
        for (TrieNode c : n.children.values()) dfs(c, r);
    }
}

public class MedSwap {
    static int idCounter = 1;
    static Map<Integer, User> users = new HashMap<>();
    static Map<String, List<User>> donors = new HashMap<>();
    static Map<String, List<User>> recipients = new HashMap<>();
    static Trie medicineTrie = new Trie();
    static int totalDonated = 0;

    static double distance(Location a, Location b) {
        double R = 6371;
        double dLat = Math.toRadians(b.lat - a.lat);
        double dLon = Math.toRadians(b.lon - a.lon);
        double x = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(a.lat))*Math.cos(Math.toRadians(b.lat))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return 2 * R * Math.atan2(Math.sqrt(x), Math.sqrt(1-x));
    }

    static double score(User d, User r) {
        double dist = distance(d.location, r.location);
        long days = ChronoUnit.DAYS.between(LocalDate.now(), d.expiry);
        double exp = 1.0 / (days + 1);
        double urg = 1.0 - r.urgency / 5.0;
        return 0.5 * dist + 0.3 * exp + 0.2 * urg;
    }

    static void register(Scanner sc, UserType type) {
        User u = new User(idCounter++);
        u.type = type;
        System.out.print("Name: "); u.name = sc.nextLine();
        System.out.print("Contact: "); u.contact = sc.nextLine();
        System.out.print("Lat Lon: "); u.location = new Location(sc.nextDouble(), sc.nextDouble());
        sc.nextLine();
        System.out.print("Medicine: "); u.medicine = sc.nextLine();
        medicineTrie.insert(u.medicine);
        System.out.print("Quantity: "); u.quantity = sc.nextInt();
        sc.nextLine();

        if (type == UserType.DONOR) {
            System.out.print("Expiry (yyyy-mm-dd): ");
            u.expiry = LocalDate.parse(sc.nextLine());
            donors.computeIfAbsent(u.medicine.toLowerCase(), k -> new ArrayList<>()).add(u);
        } else {
            System.out.print("Urgency (1-5): ");
            u.urgency = sc.nextInt();
            sc.nextLine();
            recipients.computeIfAbsent(u.medicine.toLowerCase(), k -> new ArrayList<>()).add(u);
        }
        users.put(u.id, u);
    }

    static void matchRecipient(User r) {
        List<User> list = donors.get(r.medicine.toLowerCase());
        if (list == null) return;

        PriorityQueue<Match> pq = new PriorityQueue<>(Comparator.comparingDouble(m -> m.score));
        for (User d : list)
            if (d.status == Status.PENDING && d.quantity > 0 && d.expiry.isAfter(LocalDate.now()))
                pq.add(new Match(d, r, score(d, r)));

        while (!pq.isEmpty() && r.quantity > 0) {
            Match m = pq.poll();
            User d = m.donor;
            int give = Math.min(d.quantity, r.quantity);
            d.quantity -= give;
            r.quantity -= give;
            totalDonated += give;
            if (d.quantity == 0) d.status = Status.COMPLETED;
            r.status = r.quantity == 0 ? Status.MATCHED : Status.PENDING;
            System.out.println("Matched: " + d.name + " -> " + r.name + " (" + give + ")");
        }
    }

    static void matchAll() {
        for (User u : users.values())
            if (u.type == UserType.RECIPIENT && u.status == Status.PENDING)
                matchRecipient(u);
    }

    static void viewAll() {
        for (User u : users.values()) {
            System.out.println(u.id + " " + u.name + " " + u.type + " " + u.medicine + " Qty:" + u.quantity + " Status:" + u.status);
        }
    }

    static void viewByMedicine(Scanner sc) {
        System.out.print("Medicine: ");
        String m = sc.nextLine().toLowerCase();
        System.out.println("Donors:");
        donors.getOrDefault(m, new ArrayList<>()).forEach(d -> System.out.println(d.id + " " + d.name + " Qty:" + d.quantity));
        System.out.println("Recipients:");
        recipients.getOrDefault(m, new ArrayList<>()).forEach(r -> System.out.println(r.id + " " + r.name + " Need:" + r.quantity));
    }

    static void stats() {
        long pd = users.values().stream().filter(u -> u.type == UserType.DONOR && u.status == Status.PENDING).count();
        long pr = users.values().stream().filter(u -> u.type == UserType.RECIPIENT && u.status == Status.PENDING).count();
        System.out.println("Users: " + users.size());
        System.out.println("Pending Donors: " + pd);
        System.out.println("Pending Recipients: " + pr);
        System.out.println("Total Donated Units: " + totalDonated);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n1.Register Donor 2.Register Recipient 3.Match by ID 4.Match All 5.View Users 6.View by Medicine 7.Stats 8.Exit");
            int ch = sc.nextInt(); sc.nextLine();
            if (ch == 1) register(sc, UserType.DONOR);
            else if (ch == 2) register(sc, UserType.RECIPIENT);
            else if (ch == 3) {
                System.out.print("Recipient ID: ");
                int id = sc.nextInt(); sc.nextLine();
                if (users.containsKey(id)) matchRecipient(users.get(id));
            }
            else if (ch == 4) matchAll();
            else if (ch == 5) viewAll();
            else if (ch == 6) viewByMedicine(sc);
            else if (ch == 7) stats();
            else break;
        }
    }
}
