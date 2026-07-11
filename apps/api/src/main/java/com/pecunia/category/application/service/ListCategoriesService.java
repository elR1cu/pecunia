package com.pecunia.category.application.service;

import com.pecunia.category.application.port.in.ListCategories;
import com.pecunia.category.application.port.in.ListCategoriesQuery;
import com.pecunia.category.application.port.out.CategoryRepository;
import com.pecunia.category.application.readmodel.CategoryNode;
import com.pecunia.category.domain.Category;
import com.pecunia.sharedkernel.CategoryId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCategoriesService implements ListCategories {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryNode> list(ListCategoriesQuery query) {
        List<Category> allByOwner = categoryRepository.findAllByOwner(query.owner());

        Map<Boolean, List<Category>> hasParentPartition = allByOwner.stream()
                .collect(Collectors.partitioningBy(cat -> cat.parent().isPresent()));

        List<Category> roots = hasParentPartition.get(false);

        Map<CategoryId, List<Category>> childrenByParent = hasParentPartition.get(true).stream()
                .collect(Collectors.groupingBy(cat -> cat.parent().get()));

        return roots.stream()
                .map(cat -> buildNode(cat, childrenByParent))
                .sorted(Comparator.comparing(CategoryNode::type).thenComparingInt(CategoryNode::displayOrder))
                .toList();
    }

    private CategoryNode buildNode(Category category, Map<CategoryId, List<Category>> childrenByParent) {
        List<CategoryNode> children = childrenByParent.getOrDefault(category.id(), List.of()).stream()
                .map(cat -> buildNode(cat, childrenByParent))
                .sorted(Comparator.comparingInt(CategoryNode::displayOrder))
                .toList();

        return new CategoryNode(
                category.id(),
                category.name(),
                category.type(),
                category.color(),
                category.icon(),
                category.displayOrder(),
                category.archived(),
                children);
    }
}
