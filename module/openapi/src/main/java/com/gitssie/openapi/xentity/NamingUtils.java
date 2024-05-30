package com.gitssie.openapi.xentity;

public class NamingUtils {

    public static String normalizeSQLName(String name) {
        if (name != null) {
            return name.replaceAll("\r", "").replaceAll("\n", " ");
        } else {
            return null;
        }
    }

    public static String reCamelCase(String source){
        return reCamelCase(new StringBuilder(source.length() + 6),source);
    }

    public static String reCamelCase(StringBuilder buf,String source){
        return reCamelCase(buf,source,'_');
    }

    public static String reCamelCase(StringBuilder buf,String source, char delimiter){
        char ch;
        for(int i=0;i<source.length();i++){
            if(i == 0){
                buf.append(Character.toLowerCase(source.charAt(i)));
                continue;
            }
            ch = source.charAt(i);
            if(ch == delimiter){
                buf.append(ch);
            }else if(Character.isUpperCase(ch)){
                if(Character.isLowerCase(source.charAt(i-1))){
                    buf.append(delimiter);
                }
                buf.append(Character.toLowerCase(ch));
            }else{
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    public static String toCamelCase(String source){
        return toCamelCase(new StringBuilder(source.length() + 6),source);
    }

    public static String toCamelCase(StringBuilder buf,String source){
        return toCamelCase(buf,source,'_');
    }

    public static String toCamelCase(StringBuilder buf,String source,char delimiter){
        char ch,ch2;
        for(int i=0;i<source.length();i++){
            if(i == 0){
                buf.append(Character.toLowerCase(source.charAt(i)));
                continue;
            }
            ch = source.charAt(i);
            if(ch == delimiter){
                if(i + 1 < source.length()){
                    ch2 = source.charAt(++i);
                    if(ch2 == delimiter){
                        buf.append(ch);
                        buf.append(ch2);
                    }else if(Character.isLowerCase(ch2)){
                        buf.append(Character.toUpperCase(ch2));
                    }else{
                        buf.append(ch2);
                    }
                }else{
                    buf.append(ch);
                }
            }else{
                buf.append(ch);
            }
        }
        return buf.toString();
    }
}
