package Client;

public class Main {
    public static void main(String args[]) {
        GUI.setGUILook(new String[] { "Nimbus", "GTK+" });
        (new GUI()).start();
    }
}
