package dev.solrlazarus.sample;

import dev.solrlazarus.autoconfigure.repository.SolrRepository;

public interface BookRepository extends SolrRepository<Book, String> {
}
