package com.notio.ai.rag;

import java.util.List;

public interface RagRetriever {

    List<RagDocument> retrieve(Long userId, String question);
}
