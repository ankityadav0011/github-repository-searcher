package com.freightfox.githubsearcher.service;

import com.freightfox.githubsearcher.dto.GithubRepoItem;
import com.freightfox.githubsearcher.dto.RepositoryDto;
import com.freightfox.githubsearcher.entity.GithubRepository;
import com.freightfox.githubsearcher.entity.SortField;
import com.freightfox.githubsearcher.exception.InvalidRequestException;
import com.freightfox.githubsearcher.repository.GithubRepositoryRepository;
import com.freightfox.githubsearcher.repository.RepositorySpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Everything that touches the database. */
@Service
public class RepositoryStoreService {

    private final GithubRepositoryRepository repository;

    public RepositoryStoreService(GithubRepositoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Upsert: one SELECT for all ids, update the ones that exist, insert the rest,
     * then one batched save. No duplicates are ever created.
     */
    @Transactional
    public List<GithubRepository> upsertAll(List<GithubRepoItem> items) {
        Map<Long, GithubRepoItem> unique = new LinkedHashMap<>();
        for (GithubRepoItem item : items) {
            unique.put(item.id(), item); // same id twice in one response -> keep the last one
        }

        Map<Long, GithubRepository> existing = repository.findAllById(unique.keySet()).stream()
                .collect(Collectors.toMap(GithubRepository::getId, Function.identity()));

        List<GithubRepository> toSave = new ArrayList<>();
        for (GithubRepoItem item : unique.values()) {
            GithubRepository entity = existing.get(item.id());
            if (entity == null) {
                entity = GithubRepository.from(item);
            } else {
                entity.applyFrom(item);
            }
            toSave.add(entity);
        }
        return repository.saveAll(toSave);
    }

    @Transactional(readOnly = true)
    public List<RepositoryDto> findStored(String language, Long minStars, String sort) {
        if (minStars != null && minStars < 0) {
            throw new InvalidRequestException("minStars must be 0 or greater");
        }
        SortField sortField = SortField.from(sort);

        Specification<GithubRepository> spec = Specification
                .where(RepositorySpecifications.hasLanguage(language))
                .and(RepositorySpecifications.hasMinStars(minStars));

        Sort order = Sort.by(Sort.Direction.DESC, sortField.getEntityProperty());
        return repository.findAll(spec, order).stream()
                .map(RepositoryMapper::toDto)
                .toList();
    }
}
