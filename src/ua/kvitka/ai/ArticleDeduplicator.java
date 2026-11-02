package ua.kvitka.ai;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/** Conservatively merges RSS duplicates while preserving historical favorite IDs. */
public final class ArticleDeduplicator {
    private ArticleDeduplicator() {}
    private static final Set<String> TRACKERS = Set.of("fbclid", "gclid", "dclid", "msclkid", "mc_cid", "mc_eid", "igshid", "ref_src", "ref_url", "source", "campaign");
    private static final Pattern SPACE = Pattern.compile("\\s+");

    public static String canonicalUrl(String raw) {
        try {
            URI u = URI.create(raw.strip());
            String scheme = u.getScheme();
            String host = u.getHost();
            if (scheme == null || host == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) return raw;
            host = host.toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) host = host.substring(4);
            int port = u.getPort();
            if ((scheme.equalsIgnoreCase("http") && port == 80) || (scheme.equalsIgnoreCase("https") && port == 443)) port = -1;
            String path = u.getRawPath();
            if (path == null || path.isEmpty()) path = "/";
            while (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
            List<String> params = new ArrayList<>();
            if (u.getRawQuery() != null) for (String item : u.getRawQuery().split("&")) {
                if (item.isEmpty()) continue;
                String key = item.split("=", 2)[0].toLowerCase(Locale.ROOT);
                if (!key.startsWith("utm_") && !TRACKERS.contains(key)) params.add(item);
            }
            Collections.sort(params);
            String query = params.isEmpty() ? null : String.join("&", params);
            // Normalize HTTP(S) as one article; keep distinct non-tracking query parameters.
            return new URI("https", null, host, port, path, query, null).toASCIIString();
        } catch (Exception e) { return raw; }
    }

    private static String titleKey(NewsArticle a) {
        String host;
        try { host = URI.create(a.url()).getHost(); } catch (Exception e) { return ""; }
        if (host == null) return "";
        host = host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
        String title = SPACE.matcher(a.title().strip().toLowerCase(Locale.ROOT)).replaceAll(" ");
        // Exact title on the same website only; fuzzy matching risks hiding different articles.
        return title.length() < 24 ? "" : host + "\u0000" + title;
    }

    public static List<NewsArticle> merge(Collection<NewsArticle> oldArticles,
                                           Collection<NewsArticle> newlyFetched,
                                           Set<String> favoriteIds) {
        LinkedHashMap<String,NewsArticle> result = new LinkedHashMap<>();
        Map<String,String> byTitle = new HashMap<>();
        for (NewsArticle a : oldArticles) add(a, result, byTitle, favoriteIds);
        for (NewsArticle a : newlyFetched) add(a, result, byTitle, favoriteIds);
        return List.copyOf(result.values());
    }
    private static void add(NewsArticle a, Map<String,NewsArticle> result,
                            Map<String,String> byTitle, Set<String> favorites) {
        String key = canonicalUrl(a.url());
        String title = titleKey(a);
        String existingKey = result.containsKey(key) ? key : (title.isEmpty() ? null : byTitle.get(title));
        if (existingKey == null) {
            result.put(key, a);
            if (!title.isEmpty()) byTitle.put(title, key);
            return;
        }
        NewsArticle prior = result.get(existingKey);
        // Prefer an already favorited ID. Otherwise keep the earlier ID stable across refreshes.
        String stableId = favorites.contains(a.id()) && !favorites.contains(prior.id()) ? a.id() : prior.id();
        NewsArticle freshest = a.date().isAfter(prior.date()) ? a : prior;
        if (a.date().equals(prior.date()) && a.description().length() > prior.description().length()) freshest = a;
        NewsArticle merged = new NewsArticle(stableId, freshest.title(), freshest.description(), freshest.source(),
                freshest.category(), freshest.date(), freshest.url());
        result.put(existingKey, merged);
        if (!title.isEmpty()) byTitle.put(title, existingKey);
        String oldTitle = titleKey(prior);
        if (!oldTitle.isEmpty()) byTitle.put(oldTitle, existingKey);
    }
}
