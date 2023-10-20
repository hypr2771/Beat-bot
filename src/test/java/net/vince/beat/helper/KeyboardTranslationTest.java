package net.vince.beat.helper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class KeyboardTranslationTest {

  @Test
  void toEnglish() {
    assertEquals("How to open both long and short simultaneously in Binance Futures", KeyboardTranslation.toEnglish("็นไ ะน นยำื ินะ้ สนืเ ฟืก ห้นพะ หรทีสะฟืำนีหสั รื ฺรืฟืแำ โีะีพำห"));
  }

  @Test
  void toThai() {
    assertEquals("็นไ ะน นยำื ินะ้ สนืเ ฟืก ห้นพะ หรทีสะฟืำนีหสั รื ฺรืฟืแำ โีะีพำห", KeyboardTranslation.toThai("How to open both long and short simultaneously in Binance Futures"));
  }

  @Test
  void equals() {
    assertTrue(KeyboardTranslation.equals(null, null));
    assertFalse(KeyboardTranslation.equals("ยสฟั", null));
    assertFalse(KeyboardTranslation.equals(null, "play"));

    assertTrue(KeyboardTranslation.equals("ยสฟั", "play"));
    assertTrue(KeyboardTranslation.equals("play", "ยสฟั"));
    assertTrue(KeyboardTranslation.equals("play", "play"));

    assertFalse(KeyboardTranslation.equals("play", "ยสฟ"));
    assertFalse(KeyboardTranslation.equals("ยสฟ", "play"));
    assertFalse(KeyboardTranslation.equals("play", "pla"));
    assertFalse(KeyboardTranslation.equals("pla", "play"));
  }
}