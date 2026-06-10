package work.nemonet.littlemaidneo.client.screen.component;

@FunctionalInterface
public interface FilterPredicate<T> {

    boolean test(T element, String filterText);

    static <T> FilterPredicate<T> containsIgnoreCase() {
        return (element, filterText) -> {
            if (filterText == null || filterText.trim().isEmpty()) {
                return true;
            }
            String elementText = element.toString().toLowerCase();
            String searchText = filterText.toLowerCase().trim();
            return elementText.contains(searchText);
        };
    }

    static <T> FilterPredicate<T> contains() {
        return (element, filterText) -> {
            if (filterText == null || filterText.trim().isEmpty()) {
                return true;
            }
            String elementText = element.toString();
            String searchText = filterText.trim();
            return elementText.contains(searchText);
        };
    }

    static <T> FilterPredicate<T> startsWithIgnoreCase() {
        return (element, filterText) -> {
            if (filterText == null || filterText.trim().isEmpty()) {
                return true;
            }
            String elementText = element.toString().toLowerCase();
            String searchText = filterText.toLowerCase().trim();
            return elementText.startsWith(searchText);
        };
    }

    static <T> FilterPredicate<T> regex() {
        return (element, filterText) -> {
            if (filterText == null || filterText.trim().isEmpty()) {
                return true;
            }
            try {
                String elementText = element.toString();
                return elementText.matches(filterText.trim());
            } catch (Exception e) {
                return false;
            }
        };
    }

    default FilterPredicate<T> and(FilterPredicate<T> other) {
        return (element, filterText) -> this.test(element, filterText) && other.test(element, filterText);
    }

    default FilterPredicate<T> or(FilterPredicate<T> other) {
        return (element, filterText) -> this.test(element, filterText) || other.test(element, filterText);
    }

    default FilterPredicate<T> negate() {
        return (element, filterText) -> !this.test(element, filterText);
    }
}
