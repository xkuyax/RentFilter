package org.example.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AddressNormalizer {

    private static final Pattern ADDRESS_PATTERN =
            Pattern.compile("^(.+?),\\s*(\\d{4})\\s+(.+)$");

    public List<String> normalize(String address) {
        List<String> candidates = new ArrayList<>();
        candidates.add(address);

        Matcher m = ADDRESS_PATTERN.matcher(address);
        if (!m.find()) return candidates;

        String prefix = m.group(1).trim();
        String postalCity = m.group(2) + " " + m.group(3).trim();

        // Check if prefix contains a slash separating two different streets
        // e.g. "Humboldtstr. 1/Grabenstr. 2" → right side starts with a letter
        List<String> streets = splitBySlashIfDifferentStreets(prefix);

        for (String streetFragment : streets) {
            String street = extractStreetName(streetFragment);
            List<String> numbers = extractNumbers(streetFragment, street);

            if (numbers.isEmpty()) {
                if (!streetFragment.matches(".*\\d.*")) {
                    addCandidate(candidates, address, street + " 1, " + postalCity);
                }
            } else {
                for (String num : numbers) {
                    addCandidate(candidates, address, street + " " + num + ", " + postalCity);
                }
            }
        }

        return candidates;
    }

    // If prefix like "Humboldtstr. 1/Grabenstr. 2", split into each street segment.
    // If prefix like "Sandgasse 43/45/45a", keep as one (numbers only after slash).
    private List<String> splitBySlashIfDifferentStreets(String prefix) {
        String[] parts = prefix.split("/");
        if (parts.length < 2) return List.of(prefix);

        // If any part after the first starts with a letter (not a number or whitespace),
        // it's a different street name
        boolean differentStreets = false;
        for (int i = 1; i < parts.length; i++) {
            String trimmed = parts[i].trim();
            if (!trimmed.isEmpty() && Character.isLetter(trimmed.charAt(0))) {
                differentStreets = true;
                break;
            }
        }

        if (differentStreets) {
            List<String> result = new ArrayList<>();
            for (String part : parts) {
                result.add(part.trim());
            }
            return result;
        }

        return List.of(prefix);
    }

    // Extract street name (everything before the first number)
    private String extractStreetName(String fragment) {
        String name = fragment.replaceFirst("\\s+\\d.*$", "").trim();
        return name.isEmpty() ? fragment : name;
    }

    // Extract all number variants from the fragment (after the street name)
    // "3, 3a" → ["3", "3a"],  "43/45/45a" → ["43", "45", "45a"],  "27, 28" → ["27", "28"]
    private List<String> extractNumbers(String fragment, String streetName) {
        List<String> numbers = new ArrayList<>();
        String numPart = fragment;
        if (!streetName.isEmpty() && !streetName.equals(fragment)) {
            numPart = fragment.substring(streetName.length()).trim();
        }
        if (numPart.isEmpty()) return numbers;

        for (String token : numPart.split("[,/]")) {
            String n = token.trim();
            if (!n.isBlank() && n.matches(".*\\d.*")) {
                numbers.add(n);
            }
        }
        return numbers;
    }

    private void addCandidate(List<String> candidates, String original, String candidate) {
        if (!candidate.equals(original) && !candidates.contains(candidate)) {
            candidates.add(candidate);
        }
    }
}
