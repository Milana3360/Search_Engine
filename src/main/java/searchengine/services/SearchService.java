package searchengine.services;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import searchengine.components.MystemLemmatizer;
import searchengine.config.Lemma;
import searchengine.config.Page;
import searchengine.config.PageResponse;
import searchengine.config.SearchResponse;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private PageRepository pageRepository;

    public SearchResponse searchPagesWithLemmas(String query, Integer offset, Integer limit) {
        if (offset == null) offset = 0;
        if (limit == null || limit <= 0) limit = 20;

        int maxLimit = 20 * 20;
        limit = Math.min(limit, maxLimit);

        int pageNumber = offset / limit;
        Pageable pageable = PageRequest.of(pageNumber, limit);

        List<String> lemmas = extractLemmasFromQuery(query);

        List<Lemma> foundLemmas = lemmaRepository.findByLemmaIn(lemmas);
        System.out.println("Найденные леммы в базе данных: " + foundLemmas);

        if (foundLemmas.isEmpty()) {
            System.out.println("Не найдено лемм для запроса: " + query);
            return new SearchResponse(false, Collections.emptyList(), 0);
        }

        List<Integer> lemmaIds = foundLemmas.stream()
                .map(Lemma::getId)
                .collect(Collectors.toList());

        List<Integer> pageIds = pageRepository.findPageIdsByLemmaIds(lemmaIds);
        System.out.println("Найденные page_id: " + pageIds);

        if (pageIds.isEmpty()) {
            System.out.println("Не найдено страниц для лемм: " + lemmaIds);
            return new SearchResponse(false, Collections.emptyList(), 0);
        }

        List<Page> pages = pageRepository.findPagesByIdsWithPagination(pageIds, pageable);
        System.out.println("Найденные страницы: " + pages);

        Set<Page> uniquePages = new HashSet<>(pages);

        List<PageResponse> responses = uniquePages.stream()
                .map(pageItem -> {
                    PageResponse response = new PageResponse();
                    String fullUrl = pageItem.getUrl().replaceAll("[\"{}]+", "").replaceAll("url:\\s*", "").trim();

                    try {
                        URL url = new URL(fullUrl);
                        String site = url.getProtocol() + "://" + url.getHost();
                        response.setSite(site);

                        String uri = url.getPath();
                        response.setUri(uri);

                        response.setSiteName(url.getHost());
                    } catch (MalformedURLException e) {
                        response.setSite(fullUrl);
                        response.setUri("/");
                        response.setSiteName("Unknown");
                    }

                    String pageTitle = getTitleFromPageContent(pageItem.getContent());
                    response.setTitle(pageTitle);

                    response.setSnippet(generateSnippet(pageItem.getContent(), lemmas));

                    response.setRelevance(calculateRelevance(pageItem, foundLemmas));

                    return response;
                })
                .collect(Collectors.toList());

        long totalCount = pageRepository.countPagesByQuery(query);
        return new SearchResponse(true, responses, totalCount);
    }

    private String getTitleFromPageContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "Без названия";
        }

        try {
            org.jsoup.nodes.Document doc = Jsoup.parse(content);

            String title = doc.title();

            if (title == null || title.isEmpty()) {
                Element h1 = doc.selectFirst("h1");
                title = h1 != null ? h1.text() : null;
            }

            if (title == null || title.isEmpty()) {
                title = doc.body().text();
                title = title.length() > 50 ? title.substring(0, 50) + "..." : title;
            }

            return title != null ? title.trim() : "Без названия";
        } catch (Exception ignored) {
            return "Без названия";
        }
    }

     private String generateSnippet(String content, List<String> lemmas) {
        if (content == null || content.trim().isEmpty()) {
            return "No content available";
        }

        String textContent = cleanHtml(content);

        String normalizedContent = textContent.toLowerCase();

        int bestMatchIndex = -1;
        String matchedLemma = null;
        Pattern pattern = null;

        for (String lemma : lemmas) {
            String normalizedLemma = lemma.toLowerCase();
            String regex = "\\b" + Pattern.quote(normalizedLemma) + "\\w*\\b";
            pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(normalizedContent);

            if (matcher.find()) {
                int lemmaIndex = matcher.start();
                if (bestMatchIndex == -1 || lemmaIndex < bestMatchIndex) {
                    bestMatchIndex = lemmaIndex;
                    matchedLemma = normalizedLemma;
                }
            }
        }

        if (bestMatchIndex == -1) {
            return "No relevant content found";
        }

        int snippetStart = Math.max(0, bestMatchIndex - 50);
        int snippetEnd = Math.min(textContent.length(), bestMatchIndex + 150);
        String snippet = textContent.substring(snippetStart, snippetEnd).trim();

        StringBuilder highlightedSnippet = new StringBuilder(snippet);
        String normalizedSnippet = snippet.toLowerCase();

        for (String lemma : lemmas) {
            String normalizedLemma = lemma.toLowerCase();
            String regex = "\\b" + Pattern.quote(normalizedLemma) + "\\w*\\b";
            pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(normalizedSnippet);

            int offset = 0;
            while (matcher.find()) {
                String foundWord = snippet.substring(matcher.start(), matcher.end());
                String replacement = "<b>" + foundWord + "</b>";

                highlightedSnippet.replace(
                        matcher.start() + offset,
                        matcher.end() + offset,
                        replacement
                );

                offset += replacement.length() - foundWord.length();
            }

            normalizedSnippet = highlightedSnippet.toString().toLowerCase();
        }

        return highlightedSnippet.toString();
    }

    private String cleanHtml(String content) {
        return Jsoup.parse(content).text();
    }

    private double calculateRelevance(Page page, List<Lemma> lemmas) {
        double relevance = 0;
        for (Lemma lemma : lemmas) {
            int frequency = page.getContent().toLowerCase().split(lemma.getLemma().toLowerCase()).length - 1;
            relevance += frequency / (double) lemma.getFrequency();
        }
        return relevance;
    }

    private List<String> extractLemmasFromQuery(String query) {
        String lemmatizedText = MystemLemmatizer.lemmatize(query);
        lemmatizedText = lemmatizedText.replaceAll("[^a-zA-Z0-9а-яА-Я]", " ").trim();

        String[] lemmas = lemmatizedText.split("\\s+");

        Set<String> uniqueLemmas = new HashSet<>(Arrays.asList(lemmas));
        return new ArrayList<>(uniqueLemmas);
    }
}