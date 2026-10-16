package com.freightfox.githubsearcher.repository;

import com.freightfox.githubsearcher.entity.GithubRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static com.freightfox.githubsearcher.TestData.item;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GithubRepositoryRepositoryTest {

    @Autowired
    private GithubRepositoryRepository repository;

    @BeforeEach
    void seed() {
        repository.saveAll(List.of(
                GithubRepository.from(item(1L, "a", "Java", 500, 10)),
                GithubRepository.from(item(2L, "b", "Java", 50, 90)),
                GithubRepository.from(item(3L, "c", "Python", 900, 20))));
    }

    private Specification<GithubRepository> spec(String language, Long minStars) {
        return Specification.where(RepositorySpecifications.hasLanguage(language))
                .and(RepositorySpecifications.hasMinStars(minStars));
    }

    @Test
    void noFiltersReturnsEverything() {
        assertThat(repository.findAll(spec(null, null), Sort.by("id"))).hasSize(3);
    }

    @Test
    void filtersByLanguageIgnoringCase() {
        assertThat(repository.findAll(spec("java", null), Sort.by("id")))
                .extracting(GithubRepository::getId).containsExactly(1L, 2L);
    }

    @Test
    void filtersByMinStarsInclusive() {
        assertThat(repository.findAll(spec(null, 500L), Sort.by("id")))
                .extracting(GithubRepository::getId).containsExactly(1L, 3L);
    }

    @Test
    void combinesFiltersAndSortsByForksDescending() {
        assertThat(repository.findAll(spec("Java", 10L), Sort.by(Sort.Direction.DESC, "forksCount")))
                .extracting(GithubRepository::getId).containsExactly(2L, 1L);
    }

    @Test
    void savingSameIdTwiceUpdatesInsteadOfDuplicating() {
        GithubRepository loaded = repository.findById(1L).orElseThrow();
        loaded.applyFrom(item(1L, "renamed", "Java", 777, 10));
        repository.saveAndFlush(loaded);

        assertThat(repository.count()).isEqualTo(3);
        assertThat(repository.findById(1L).orElseThrow().getStargazersCount()).isEqualTo(777);
    }

    @Test
    void emptyDatabaseResultIsEmptyList() {
        assertThat(repository.findAll(spec("Rust", null), Sort.by("id"))).isEmpty();
    }
}
