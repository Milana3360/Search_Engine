package searchengine.services;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import searchengine.components.MystemLemmatizer;
import searchengine.config.Lemma;
import searchengine.config.Page;
import searchengine.config.PageResponse;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import javax.transaction.Transactional;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class SearchService {

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private PageRepository pageRepository;

    @Transactional
    public List<PageResponse> searchPages(String query, int page, int size) {
        List<String> lemmas = extractLemmasFromQuery(query);
        System.out.println("Леммы из запроса: " + lemmas);

        List<Lemma> foundLemmas = lemmaRepository.findByLemmaIn(lemmas);
        System.out.println("Найденные леммы в базе данных: " + foundLemmas);

        if (foundLemmas.isEmpty()) {
            System.out.println("Не найдено лемм для запроса: " + query);
            return Collections.emptyList();
        }

        List<Integer> lemmaIds = foundLemmas.stream()
                .map(Lemma::getId)
                .collect(Collectors.toList());

        List<Integer> pageIds = pageRepository.findPageIdsByLemmaIds(lemmaIds);
        System.out.println("Найденные page_id: " + pageIds);

        if (pageIds.isEmpty()) {
            System.out.println("Не найдено страниц для лемм: " + lemmaIds);
            return Collections.emptyList();
        }

        List<Page> pages = pageRepository.findPagesByIds(pageIds, PageRequest.of(page, size));
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

        return responses;
    }


    private String normalizeUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            return parsedUrl.getProtocol() + "://" + parsedUrl.getHost() + parsedUrl.getPath();
        } catch (MalformedURLException e) {
            return url;
        }
    }

    private String extractSiteName(String url) {
        try {
            URL parsedUrl = new URL(url);
            return parsedUrl.getHost();
        } catch (MalformedURLException e) {
            return "Unknown Site";
        }
    }

    private List<String> extractLemmasFromQuery(String query) {
        String lemmatizedText = MystemLemmatizer.lemmatize(query);
        lemmatizedText = lemmatizedText.replaceAll("[^a-zA-Z0-9а-яА-Я]", " ").trim();

        String[] lemmas = lemmatizedText.split("\\s+");

        Set<String> uniqueLemmas = new HashSet<>(Arrays.asList(lemmas));
        return new ArrayList<>(uniqueLemmas);
    }

    private String getTitleFromPageContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "No Title";
        }

        try {
            org.jsoup.nodes.Document doc = Jsoup.parse(content);
            String title = doc.title();
            if (!title.isEmpty()) {
                return title;
            }
        } catch (Exception e) {
        }

        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }

    private String generateSnippet(String content, List<String> lemmas) {
        if (content == null || content.trim().isEmpty()) {
            return "No content available";
        }

        String snippet = content.length() > 200 ? content.substring(0, 200) : content;

        for (String lemma : lemmas) {
            snippet = snippet.replaceAll("(?i)(" + lemma + ")", "<b>$1</b>");
        }

        return snippet;
    }

    private double calculateRelevance(Page page, List<Lemma> lemmas) {
        double relevance = 0;
        for (Lemma lemma : lemmas) {
            int frequency = page.getContent().toLowerCase().split(lemma.getLemma().toLowerCase()).length - 1;
            relevance += frequency / (double) lemma.getFrequency();
        }
        return relevance;
    }
}
