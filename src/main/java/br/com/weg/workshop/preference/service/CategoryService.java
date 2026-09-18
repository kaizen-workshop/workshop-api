package br.com.weg.workshop.preference.service;

import br.com.weg.workshop.preference.domain.Category;
import br.com.weg.workshop.preference.dto.*;
import br.com.weg.workshop.preference.repository.CategoryRepository;
import br.com.weg.workshop.shared.error.ConflictException;
import br.com.weg.workshop.shared.error.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    public CategoryService(CategoryRepository categories) { this.categories = categories; }
    @Transactional public CategoryResponse create(TaxonomyRequest request) {
        if (categories.existsByName(request.name())) throw new ConflictException("Category name already exists.");
        return CategoryResponse.from(categories.save(Category.create(request.name(), request.description())));
    }
    @Transactional(readOnly = true) public List<CategoryResponse> active() { return categories.findByActiveTrue().stream().map(CategoryResponse::from).toList(); }
    @Transactional public CategoryResponse update(UUID id, UpdateTaxonomyRequest request) {
        if (!request.hasChanges()) throw new IllegalArgumentException("At least one taxonomy field must be provided.");
        Category category = categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found."));
        if (request.name() != null && !category.getName().equals(request.name()) && categories.existsByName(request.name())) throw new ConflictException("Category name already exists.");
        category.update(request.name(), request.description(), request.active());
        return CategoryResponse.from(category);
    }
}
