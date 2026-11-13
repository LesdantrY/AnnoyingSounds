package dev.annoyingsounds;

import java.util.Locale;

/** Readable, distinct labels even for resource packs without subtitle translations. */
public final class SoundNames {
    public static String display(String id) {
        String path=id.substring(id.indexOf(':')+1);
        String[] parts=path.split("\\.");
        int start=parts.length>1 && java.util.Set.of("block","entity","item","ui").contains(parts[0]) ? 1 : 0;
        var label=new StringBuilder();
        for(int i=start;i<parts.length;i++) {
            if(!label.isEmpty()) label.append(" — ");
            String words=parts[i].replace('_',' ').replace('/',' ').replace("levelup","level up");
            for(String word:words.split(" +")) {
                if(word.isEmpty()) continue;
                if(label.length()>0 && label.charAt(label.length()-1)!=' ') label.append(' ');
                label.append(word.substring(0,1).toUpperCase(Locale.ROOT)).append(word.substring(1));
            }
        }
        return label.toString();
    }
}

