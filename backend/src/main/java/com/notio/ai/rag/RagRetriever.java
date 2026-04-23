package com.notio.ai.rag;

import java.util.List;
import java.util.Optional;

public interface RagRetriever {

    List<RagDocument> retrieve(Long userId, String question, Optional<TimeRange> timeRange);
}
