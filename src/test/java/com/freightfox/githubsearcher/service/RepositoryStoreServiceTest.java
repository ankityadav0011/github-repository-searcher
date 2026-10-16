package com.freightfox.githubsearcher.service;

import com.freightfox.githubsearcher.dto.GithubRepoItem;
import com.freightfox.githubsearcher.dto.RepositoryDto;
import com.freightfox.githubsearcher.entity.GithubRepository;
import com.freightfox.githubsearcher.exception.InvalidRequestException;
import com.freightfox.githubsearcher.repository.GithubRepositoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static com.freightfox.githubsearcher.TestData.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryStoreServiceTest {

    @Mock
    private GithubRepositoryRepository repository;

    @InjectMocks
    private RepositoryStoreService service;

    @Test
    void upsertUpdatesExistingAndInsertsNew() {
        GithubRepository existing = GithubRepository.from(item(1L, "old-name", "Java", 10, 1));
        when(repository.findAllById(anyIterable())).thenReturn(List.of(existing));
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<GithubRepository> saved = service.upsertAll(List.of(
                item(1L, "new-name", "Java", 99, 5),   // already stored -> updated
                item(2L, "brand-new", "Go", 7, 0)));   // new -> inserted

        assertThat(saved).hasSize(2);
        assertThat(saved.get(0)).isSameAs(existing);      // same object updated, not duplicated
        assertThat(existing.getName()).isEqualTo("new-name");
        assertThat(existing.getStargazersCount()).isEqualTo(99);
        assertThat(saved.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void upsertDeduplicatesSameIdInOneBatch() {
        when(repository.findAllById(anyIterable())).thenReturn(List.of());
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<GithubRepository> saved = service.upsertAll(List.of(
                item(5L, "a", "Java", 1, 1), item(5L, "a-latest", "Java", 2, 2)));

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getName()).isEqualTo("a-latest");
    }

    @Test
    void findStoredMapsEntitiesToDtos() {
        when(repository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(GithubRepository.from(item(1L, "spring-boot-example", "Java", 450, 120))));

        List<RepositoryDto> result = service.findStored("Java", 100L, "stars");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("spring-boot-example");
        assertThat(result.get(0).owner()).isEqualTo("owner");
        assertThat(result.get(0).stars()).isEqualTo(450);
    }

    @Test
    void findStoredSortsDescendingByRequestedField() {
        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        service.findStored(null, null, "forks");

        verify(repository).findAll(any(Specification.class), org.mockito.ArgumentMatchers.eq(
                Sort.by(Sort.Direction.DESC, "forksCount")));
    }

    @Test
    void findStoredRejectsInvalidSort() {
        assertThatThrownBy(() -> service.findStored(null, null, "popularity"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("stars, forks, updated");
        verify(repository, never()).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void findStoredRejectsNegativeMinStars() {
        assertThatThrownBy(() -> service.findStored(null, -1L, "stars"))
                .isInstanceOf(InvalidRequestException.class);
    }
}
