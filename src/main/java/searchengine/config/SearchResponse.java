package searchengine.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchResponse {

    private boolean result;
    private List<PageResponse> data;
    private long count;

    public SearchResponse() {
    }

    public SearchResponse(boolean result, List<PageResponse> data, long count) {
        this.result = result;
        this.data = data;
        this.count = count;
    }

}
