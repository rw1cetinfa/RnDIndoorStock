package com.example.rndlaboratorystock.Helpers;


public class StringHelper {

    public static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        String[] words = input.split("\\s+"); // Boşluklara göre ayır

        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0))) // İlk harf büyük
                        .append(word.substring(1).toLowerCase())      // Diğerleri küçük
                        .append(" ");
            }
        }

        return result.toString().trim(); // Fazla boşluğu kaldır
    }

    public static String replaceSubstring(String original, int start, int length, String replacement) {
        if (original == null) return null;
        if (start < 0 || start >= original.length()) return original;

        // Eğer length sınırı aşarsa düzelt
        int end = Math.min(start + length, original.length());

        StringBuilder sb = new StringBuilder();
        sb.append(original, 0, start);      // değişmeyecek kısmı ekle
        sb.append(replacement);             // yeni kısmı ekle
        if (end < original.length()) {
            sb.append(original.substring(end)); // geri kalan kısmı ekle
        }
        return sb.toString();
    }
}


