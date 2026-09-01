import java.util.*;
import org.jetbrains.java.decompiler.main.extern.IIdentifierRenamer;
public class VFMinRenamer implements IIdentifierRenamer {
  static Map<String,Integer> F = new HashMap<String,Integer>();
  static Map<String,Integer> M = new HashMap<String,Integer>();
  static { F.put("AgeOfEmpires/AgeOfEmpires|a|LAgeOfEmpires/c;",0); }
  static { F.put("AgeOfEmpires/AgeOfEmpires|a|Z",1); }
  static { F.put("AgeOfEmpires/b|a|I",0); }
  static { F.put("AgeOfEmpires/b|a|Ljavax/microedition/media/Player;",1); }
  static { F.put("AgeOfEmpires/b|a|Ljavax/microedition/midlet/MIDlet;",2); }
  static { F.put("AgeOfEmpires/b|a|Z",3); }
  static { F.put("AgeOfEmpires/b|a|[B",4); }
  static { F.put("AgeOfEmpires/b|a|[I",5); }
  static { F.put("AgeOfEmpires/b|a|[J",6); }
  static { F.put("AgeOfEmpires/b|b|Z",0); }
  static { F.put("AgeOfEmpires/b|b|[I",1); }
  static { F.put("AgeOfEmpires/c|a|B",0); }
  static { F.put("AgeOfEmpires/c|a|I",1); }
  static { F.put("AgeOfEmpires/c|a|LAgeOfEmpires/AgeOfEmpires;",2); }
  static { F.put("AgeOfEmpires/c|a|LAgeOfEmpires/d;",3); }
  static { F.put("AgeOfEmpires/c|a|Ljava/lang/String;",4); }
  static { F.put("AgeOfEmpires/c|a|Ljavax/microedition/lcdui/Font;",5); }
  static { F.put("AgeOfEmpires/c|a|Ljavax/microedition/lcdui/Graphics;",6); }
  static { F.put("AgeOfEmpires/c|a|S",7); }
  static { F.put("AgeOfEmpires/c|a|Z",8); }
  static { F.put("AgeOfEmpires/c|a|[B",9); }
  static { F.put("AgeOfEmpires/c|a|[I",10); }
  static { F.put("AgeOfEmpires/c|a|[Ljava/lang/String;",11); }
  static { F.put("AgeOfEmpires/c|a|[Ljavax/microedition/lcdui/Image;",12); }
  static { F.put("AgeOfEmpires/c|a|[S",13); }
  static { F.put("AgeOfEmpires/c|a|[[I",14); }
  static { F.put("AgeOfEmpires/c|a|[[S",15); }
  static { F.put("AgeOfEmpires/c|j|I",0); }
  static { F.put("AgeOfEmpires/c|j|Z",1); }
  static { F.put("AgeOfEmpires/c|j|[B",2); }
  static { F.put("AgeOfEmpires/c|f|I",0); }
  static { F.put("AgeOfEmpires/c|f|Z",1); }
  static { F.put("AgeOfEmpires/c|f|[B",2); }
  static { F.put("AgeOfEmpires/c|b|I",0); }
  static { F.put("AgeOfEmpires/c|b|Ljava/lang/String;",1); }
  static { F.put("AgeOfEmpires/c|b|S",2); }
  static { F.put("AgeOfEmpires/c|b|Z",3); }
  static { F.put("AgeOfEmpires/c|b|[B",4); }
  static { F.put("AgeOfEmpires/c|b|[I",5); }
  static { F.put("AgeOfEmpires/c|b|[Ljava/lang/String;",6); }
  static { F.put("AgeOfEmpires/c|b|[[I",7); }
  static { F.put("AgeOfEmpires/c|b|[[S",8); }
  static { F.put("AgeOfEmpires/c|d|I",0); }
  static { F.put("AgeOfEmpires/c|d|Ljava/lang/String;",1); }
  static { F.put("AgeOfEmpires/c|d|Z",2); }
  static { F.put("AgeOfEmpires/c|d|[B",3); }
  static { F.put("AgeOfEmpires/c|d|[I",4); }
  static { F.put("AgeOfEmpires/c|c|I",0); }
  static { F.put("AgeOfEmpires/c|c|Ljava/lang/String;",1); }
  static { F.put("AgeOfEmpires/c|c|S",2); }
  static { F.put("AgeOfEmpires/c|c|Z",3); }
  static { F.put("AgeOfEmpires/c|c|[B",4); }
  static { F.put("AgeOfEmpires/c|c|[I",5); }
  static { F.put("AgeOfEmpires/c|k|I",0); }
  static { F.put("AgeOfEmpires/c|k|Z",1); }
  static { F.put("AgeOfEmpires/c|k|[B",2); }
  static { F.put("AgeOfEmpires/c|e|I",0); }
  static { F.put("AgeOfEmpires/c|e|Z",1); }
  static { F.put("AgeOfEmpires/c|e|[B",2); }
  static { F.put("AgeOfEmpires/c|e|[I",3); }
  static { F.put("AgeOfEmpires/c|h|I",0); }
  static { F.put("AgeOfEmpires/c|h|Z",1); }
  static { F.put("AgeOfEmpires/c|h|[B",2); }
  static { F.put("AgeOfEmpires/c|i|I",0); }
  static { F.put("AgeOfEmpires/c|i|Z",1); }
  static { F.put("AgeOfEmpires/c|i|[B",2); }
  static { F.put("AgeOfEmpires/c|g|I",0); }
  static { F.put("AgeOfEmpires/c|g|Z",1); }
  static { F.put("AgeOfEmpires/c|g|[B",2); }
  static { F.put("AgeOfEmpires/c|l|I",0); }
  static { F.put("AgeOfEmpires/c|l|Z",1); }
  static { F.put("AgeOfEmpires/d|c|I",0); }
  static { F.put("AgeOfEmpires/d|c|Z",1); }
  static { F.put("AgeOfEmpires/d|b|I",0); }
  static { F.put("AgeOfEmpires/d|b|Z",1); }
  static { F.put("AgeOfEmpires/d|b|[I",2); }
  static { F.put("AgeOfEmpires/d|a|I",0); }
  static { F.put("AgeOfEmpires/d|a|Z",1); }
  static { F.put("AgeOfEmpires/d|a|[I",2); }
  static { F.put("AgeOfEmpires/d|a|[S",3); }
  static { F.put("com/ulysseo/mad/a|a|Lcom/ulysseo/mad/b;",0); }
  static { F.put("com/ulysseo/mad/a|a|Ljavax/microedition/midlet/MIDlet;",1); }
  static { F.put("com/ulysseo/mad/b|a|I",0); }
  static { F.put("com/ulysseo/mad/b|a|Lcom/ulysseo/mad/a;",1); }
  static { F.put("com/ulysseo/mad/b|a|Ljava/util/Timer;",2); }
  static { F.put("com/ulysseo/mad/b|a|Ljava/util/TimerTask;",3); }
  static { F.put("com/ulysseo/mad/b|a|Ljavax/microedition/lcdui/Graphics;",4); }
  static { F.put("com/ulysseo/mad/b|a|Ljavax/microedition/lcdui/Image;",5); }
  static { F.put("com/ulysseo/mad/b|a|Z",6); }
  static { F.put("com/ulysseo/mad/b|b|Ljava/util/Timer;",0); }
  static { F.put("com/ulysseo/mad/b|b|Ljava/util/TimerTask;",1); }
  static { F.put("com/ulysseo/mad/b|b|Z",2); }
  static { F.put("com/ulysseo/mad/c|a|I",0); }
  static { F.put("com/ulysseo/mad/c|a|Ljava/io/DataInputStream;",1); }
  static { F.put("com/ulysseo/mad/c|a|[B",2); }
  static { M.put("AgeOfEmpires/b|a|()B",0); }
  static { M.put("AgeOfEmpires/b|a|()V",1); }
  static { M.put("AgeOfEmpires/b|b|(I)I",0); }
  static { M.put("AgeOfEmpires/b|b|(I)V",1); }
  static { M.put("AgeOfEmpires/b|a|([BII)I",0); }
  static { M.put("AgeOfEmpires/b|a|([BII)Ljavax/microedition/lcdui/Image;",1); }
  static { M.put("AgeOfEmpires/b|a|(I)I",0); }
  static { M.put("AgeOfEmpires/b|a|(I)V",1); }
  static { M.put("AgeOfEmpires/b|a|(I)[B",2); }
  static { M.put("AgeOfEmpires/c|b|()I",0); }
  static { M.put("AgeOfEmpires/c|b|()V",1); }
  static { M.put("AgeOfEmpires/c|b|()Z",2); }
  static { M.put("AgeOfEmpires/c|b|(I)I",0); }
  static { M.put("AgeOfEmpires/c|b|(I)V",1); }
  static { M.put("AgeOfEmpires/c|b|(I)Z",2); }
  static { M.put("AgeOfEmpires/c|a|(II)I",0); }
  static { M.put("AgeOfEmpires/c|a|(II)Ljavax/microedition/lcdui/Image;",1); }
  static { M.put("AgeOfEmpires/c|a|(II)S",2); }
  static { M.put("AgeOfEmpires/c|a|(II)V",3); }
  static { M.put("AgeOfEmpires/c|a|(II)Z",4); }
  static { M.put("AgeOfEmpires/c|a|()I",0); }
  static { M.put("AgeOfEmpires/c|a|()V",1); }
  static { M.put("AgeOfEmpires/c|a|()Z",2); }
  static { M.put("AgeOfEmpires/c|g|(I)I",0); }
  static { M.put("AgeOfEmpires/c|g|(I)Z",1); }
  static { M.put("AgeOfEmpires/c|d|(I)I",0); }
  static { M.put("AgeOfEmpires/c|d|(I)V",1); }
  static { M.put("AgeOfEmpires/c|d|(I)Z",2); }
  static { M.put("AgeOfEmpires/c|c|(I)I",0); }
  static { M.put("AgeOfEmpires/c|c|(I)V",1); }
  static { M.put("AgeOfEmpires/c|c|(I)Z",2); }
  static { M.put("AgeOfEmpires/c|j|(I)I",0); }
  static { M.put("AgeOfEmpires/c|j|(I)Z",1); }
  static { M.put("AgeOfEmpires/c|e|(I)I",0); }
  static { M.put("AgeOfEmpires/c|e|(I)Z",1); }
  static { M.put("AgeOfEmpires/c|i|(I)I",0); }
  static { M.put("AgeOfEmpires/c|i|(I)Z",1); }
  static { M.put("AgeOfEmpires/c|k|(I)I",0); }
  static { M.put("AgeOfEmpires/c|k|(I)Z",1); }
  static { M.put("AgeOfEmpires/c|f|(I)I",0); }
  static { M.put("AgeOfEmpires/c|f|(I)V",1); }
  static { M.put("AgeOfEmpires/c|f|(I)Z",2); }
  static { M.put("AgeOfEmpires/c|a|(I)I",0); }
  static { M.put("AgeOfEmpires/c|a|(I)Z",1); }
  static { M.put("AgeOfEmpires/c|a|(IIII)I",0); }
  static { M.put("AgeOfEmpires/c|a|(IIII)Ljavax/microedition/lcdui/Image;",1); }
  static { M.put("AgeOfEmpires/c|a|(IIII)V",2); }
  static { M.put("AgeOfEmpires/c|h|(I)I",0); }
  static { M.put("AgeOfEmpires/c|h|(I)Z",1); }
  static { M.put("AgeOfEmpires/c|a|(III)I",0); }
  static { M.put("AgeOfEmpires/c|a|(III)Ljavax/microedition/lcdui/Image;",1); }
  static { M.put("AgeOfEmpires/c|a|(III)V",2); }
  static { M.put("AgeOfEmpires/c|a|(III)Z",3); }
  static { M.put("AgeOfEmpires/c|d|(II)I",0); }
  static { M.put("AgeOfEmpires/c|d|(II)V",1); }
  static { M.put("AgeOfEmpires/c|c|(II)I",0); }
  static { M.put("AgeOfEmpires/c|c|(II)V",1); }
  static { M.put("AgeOfEmpires/c|b|(II)I",0); }
  static { M.put("AgeOfEmpires/c|b|(II)V",1); }
  static { M.put("AgeOfEmpires/c|b|(II)Z",2); }
  static { M.put("AgeOfEmpires/d|a|()V",0); }
  static { M.put("AgeOfEmpires/d|a|()Z",1); }
  static { M.put("com/ulysseo/mad/b|a|(Lcom/ulysseo/mad/b;)I",0); }
  static { M.put("com/ulysseo/mad/b|a|(Lcom/ulysseo/mad/b;)Lcom/ulysseo/mad/a;",1); }
  static { M.put("com/ulysseo/mad/b|a|(Lcom/ulysseo/mad/b;)Z",2); }
  static { M.put("com/ulysseo/mad/c|a|(I)V",0); }
  static { M.put("com/ulysseo/mad/c|a|(I)[B",1); }
  public boolean toBeRenamed(Type t, String className, String elementName, String descriptor) {
    if (t == Type.ELEMENT_CLASS || className == null || elementName == null || descriptor == null) return false;
    String key = className.replace('.','/') + "|" + elementName + "|" + descriptor;
    return F.containsKey(key) || M.containsKey(key);
  }
  public String getNextClassName(String fullName, String shortName) { return shortName; }
  public String getNextFieldName(String className, String elementName, String descriptor) {
    Integer idx = F.get(className.replace('.','/') + "|" + elementName + "|" + descriptor);
    return elementName + "_vf" + (idx==null?0:idx);
  }
  public String getNextMethodName(String className, String elementName, String descriptor) {
    Integer idx = M.get(className.replace('.','/') + "|" + elementName + "|" + descriptor);
    return elementName + "_vf" + (idx==null?0:idx);
  }
}