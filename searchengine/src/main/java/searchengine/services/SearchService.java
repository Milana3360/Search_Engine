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
import java.util.*;

@Service
public class SearchService {

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private PageRepository pageRepository;

    @Transactional
    public List<PageResponse> searchPages(String query, int page, int size) {
        List<String> lemmas = extractLemmasFromQuery(query);

        List<Lemma> foundLemmas = lemmaRepository.findByLemmaIn(lemmas);

        if (foundLemmas.isEmpty()) {
            return Collections.emptyList();
        }

        PageRequest pageRequest = PageRequest.of(page, size);
        org.springframework.data.domain.Page<Page> pages = pageRepository.findPagesByLemmasIn(foundLemmas, pageRequest);

        List<PageResponse> responses = new ArrayList<>();
        for (Page pageItem : pages.getContent()) {
            PageResponse response = new PageResponse();

            String cleanUrl = pageItem.getUrl().replaceAll("[\"{}]+", "").replaceAll("url:\\s*", "").trim();
            response.setUrl(cleanUrl);

            String pageTitle = getTitleFromPageContent(pageItem.getContent());
            response.setTitle(pageTitle);

            response.setSnippet(generateSnippet(pageItem.getContent(), lemmas));

            response.setRelevance(calculateRelevance(pageItem, foundLemmas));

            responses.add(response);
        }
        return responses;
    }

    private List<String> extractLemmasFromQuery(String query) {
        String lemmatizedText = MystemLemmatizer.lemmatize(query);
        String[] lemmas = lemmatizedText.split("\\s+");
        return Arrays.asList(lemmas);
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