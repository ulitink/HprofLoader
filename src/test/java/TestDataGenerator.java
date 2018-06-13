import field_values.Main;

public class TestDataGenerator {

  public static void main(String[] args) throws InterruptedException {
    final Main testApp = new Main();

    System.out.println("Test application loaded");
    while (true) {
      //noinspection ConstantConditions to prevent GC
      if (testApp == null) return;
      Thread.sleep(1000);
    }
  }
}
