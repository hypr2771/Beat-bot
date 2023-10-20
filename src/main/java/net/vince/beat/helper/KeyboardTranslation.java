package net.vince.beat.helper;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

public class KeyboardTranslation {

  private final static BidiMap<String, String> EN_TH_KEYBOARD = initBidiMap();

  private static BidiMap<String, String> initBidiMap() {

    BidiMap<String, String> enThKeyboard = new DualHashBidiMap<>();

    enThKeyboard.put("A", "ฤ");
    enThKeyboard.put("a", "ฟ");
    enThKeyboard.put("B", "ฺ");
    enThKeyboard.put("b", "ิ");
    enThKeyboard.put("C", "ฉ");
    enThKeyboard.put("c", "แ");
    enThKeyboard.put("D", "ฏ");
    enThKeyboard.put("d", "ก");
    enThKeyboard.put("E", "ฎ");
    enThKeyboard.put("e", "ำ");
    enThKeyboard.put("F", "โ");
    enThKeyboard.put("f", "ด");
    enThKeyboard.put("G", "ฌ");
    enThKeyboard.put("g", "เ");
    enThKeyboard.put("H", "็");
    enThKeyboard.put("h", "้");
    enThKeyboard.put("I", "ณ");
    enThKeyboard.put("i", "ร");
    enThKeyboard.put("J", "๋");
    enThKeyboard.put("j", "่");
    enThKeyboard.put("K", "ษ");
    enThKeyboard.put("k", "า");
    enThKeyboard.put("L", "ศ");
    enThKeyboard.put("l", "ส");
    enThKeyboard.put("M", "?");
    enThKeyboard.put("m", "ท");
    enThKeyboard.put("N", "์");
    enThKeyboard.put("n", "ื");
    enThKeyboard.put("O", "ฯ");
    enThKeyboard.put("o", "น");
    enThKeyboard.put("P", "ญ");
    enThKeyboard.put("p", "ย");
    enThKeyboard.put("Q", "๐");
    enThKeyboard.put("q", "ๆ");
    enThKeyboard.put("R", "ฑ");
    enThKeyboard.put("r", "พ");
    enThKeyboard.put("S", "ฆ");
    enThKeyboard.put("s", "ห");
    enThKeyboard.put("T", "ธ");
    enThKeyboard.put("t", "ะ");
    enThKeyboard.put("U", "๊");
    enThKeyboard.put("u", "ี");
    enThKeyboard.put("V", "ฮ");
    enThKeyboard.put("v", "อ");
    enThKeyboard.put("W", "\"");
    enThKeyboard.put("w", "ไ");
    enThKeyboard.put("X", ")");
    enThKeyboard.put("x", "ป");
    enThKeyboard.put("Y", "ํ");
    enThKeyboard.put("y", "ั");
    enThKeyboard.put("Z", "(");
    enThKeyboard.put("z", "ผ");
    enThKeyboard.put("!", "+");
    enThKeyboard.put("1", "ๅ");
    enThKeyboard.put("@", "๑");
    enThKeyboard.put("2", "/");
    enThKeyboard.put("#", "๒");
    enThKeyboard.put("3", "-");
    enThKeyboard.put("$", "๓");
    enThKeyboard.put("4", "ภ");
    enThKeyboard.put("%", "๔");
    enThKeyboard.put("5", "ถ");
    enThKeyboard.put("^", "ู");
    enThKeyboard.put("6", "ุ");
    enThKeyboard.put("&", "฿");
    enThKeyboard.put("7", "ึ");
    enThKeyboard.put("*", "๕");
    enThKeyboard.put("8", "ค");
    enThKeyboard.put("(", "ต");
    enThKeyboard.put("9", "๖");
    enThKeyboard.put(")", "๗");
    enThKeyboard.put("0", "จ");
    enThKeyboard.put("_", "๘");
    enThKeyboard.put("-", "ข");
    enThKeyboard.put("+", "๙");
    enThKeyboard.put("=", "ช");
    enThKeyboard.put("{", "ฐ");
    enThKeyboard.put("[", "บ");
    enThKeyboard.put("}", ",");
    enThKeyboard.put("]", "ล");
    enThKeyboard.put("|", "ฅ");
    enThKeyboard.put("\\", "ฃ");
    enThKeyboard.put(":", "ซ");
    enThKeyboard.put(";", "ว");
    enThKeyboard.put("\"", ".");
    enThKeyboard.put("'", "ง");
    enThKeyboard.put("<", "ฒ");
    enThKeyboard.put(",", "ม");
    enThKeyboard.put(">", "ฬ");
    enThKeyboard.put(".", "ใ");
    enThKeyboard.put("?", "ฦ");
    enThKeyboard.put("/", "ฝ");

    return enThKeyboard;

  }

  public static String toEnglish(String thai) {

    if (thai == null) {
      return "";
    }

    return Arrays.stream(thai.split("(?<=.)"))
                 .map(original -> Optional.ofNullable(EN_TH_KEYBOARD.getKey(original)).orElse(original))
                 .collect(Collectors.joining());
  }

  public static String toThai(String english) {

    if (english == null) {
      return "";
    }

    return Arrays.stream(english.split("(?<=.)"))
                 .map(original -> Optional.ofNullable(EN_TH_KEYBOARD.get(original)).orElse(original))
                 .collect(Collectors.joining());
  }

  public static boolean equals(String first, String second) {

    if (first == null) {
      return second == null;
    }

    return first.equals(second) || first.equals(toThai(second)) || first.equals(toEnglish(second));
  }

}
