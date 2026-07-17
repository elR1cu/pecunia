package com.pecunia.category.web;

import com.pecunia.category.application.port.in.ArchiveCategory;
import com.pecunia.category.application.port.in.ArchiveCategoryCommand;
import com.pecunia.category.application.port.in.CreateCategory;
import com.pecunia.category.application.port.in.CreateCategoryCommand;
import com.pecunia.category.application.port.in.GetCategory;
import com.pecunia.category.application.port.in.GetCategoryQuery;
import com.pecunia.category.application.port.in.ListCategories;
import com.pecunia.category.application.port.in.MoveCategoryToParent;
import com.pecunia.category.application.port.in.MoveCategoryToParentCommand;
import com.pecunia.category.application.port.in.RecolorCategory;
import com.pecunia.category.application.port.in.RecolorCategoryCommand;
import com.pecunia.category.application.port.in.ReiconCategory;
import com.pecunia.category.application.port.in.ReiconCategoryCommand;
import com.pecunia.category.application.port.in.RenameCategory;
import com.pecunia.category.application.port.in.RenameCategoryCommand;
import com.pecunia.category.application.port.in.ReorderCategory;
import com.pecunia.category.application.port.in.ReorderCategoryCommand;
import com.pecunia.category.application.readmodel.CategoryView;
import com.pecunia.category.domain.HexColor;
import com.pecunia.category.web.dto.CategoryNodeResponse;
import com.pecunia.category.web.dto.CategoryResponse;
import com.pecunia.category.web.dto.CreateCategoryRequest;
import com.pecunia.category.web.dto.MoveCategoryRequest;
import com.pecunia.category.web.dto.UpdateCategoryRequest;
import com.pecunia.category.web.generated.CategoryApi;
import com.pecunia.category.web.mapper.CategoryMapper;
import com.pecunia.sharedkernel.CategoryId;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class CategoryController implements CategoryApi {

    private final ArchiveCategory archiveCategory;
    private final CreateCategory createCategory;
    private final GetCategory getCategory;
    private final ListCategories listCategories;
    private final MoveCategoryToParent moveCategoryToParent;
    private final RenameCategory renameCategory;
    private final RecolorCategory recolorCategory;
    private final ReiconCategory reiconCategory;
    private final ReorderCategory reorderCategory;
    private final CategoryMapper categoryMapper;

    @Override
    public ResponseEntity<Void> archiveCategory(UUID categoryId) {
        archiveCategory.archive(new ArchiveCategoryCommand(CategoryId.of(categoryId)));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CategoryResponse> createCategory(CreateCategoryRequest createCategoryRequest) {
        CreateCategoryCommand createCommand = categoryMapper.toCreateCommand(createCategoryRequest);
        CategoryId categoryId = createCategory.create(createCommand);
        CategoryView categoryView = getCategory.getById(new GetCategoryQuery(categoryId));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{categoryId}")
                .buildAndExpand(categoryId.value())
                .toUri();
        return ResponseEntity.created(location).body(categoryMapper.toDto(categoryView));
    }

    @Override
    public ResponseEntity<CategoryResponse> getCategory(UUID categoryId) {
        CategoryView categoryView = getCategory.getById(new GetCategoryQuery(CategoryId.of(categoryId)));
        CategoryResponse dto = categoryMapper.toDto(categoryView);
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<List<CategoryNodeResponse>> listCategories() {
        return ResponseEntity.ok(
                listCategories.list().stream().map(categoryMapper::toNodeDto).toList());
    }

    @Override
    public ResponseEntity<Void> moveCategory(UUID categoryId, MoveCategoryRequest moveCategoryRequest) {
        moveCategoryToParent.moveToParent(new MoveCategoryToParentCommand(
                CategoryId.of(categoryId),
                Optional.ofNullable(moveCategoryRequest.getNewParentId()).map(CategoryId::of)));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CategoryResponse> updateCategory(
            UUID categoryId, UpdateCategoryRequest updateCategoryRequest) {
        CategoryId id = CategoryId.of(categoryId);

        Optional.ofNullable(updateCategoryRequest.getName())
                .ifPresent(name -> renameCategory.rename(new RenameCategoryCommand(id, name)));
        Optional.ofNullable(updateCategoryRequest.getColor())
                .ifPresent(color -> recolorCategory.recolor(new RecolorCategoryCommand(id, new HexColor(color))));
        Optional.ofNullable(updateCategoryRequest.getIcon())
                .ifPresent(icon -> reiconCategory.reicon(
                        new ReiconCategoryCommand(id, icon.isBlank() ? Optional.empty() : Optional.of(icon))));
        Optional.ofNullable(updateCategoryRequest.getDisplayOrder())
                .ifPresent(order -> reorderCategory.reorder(new ReorderCategoryCommand(id, order)));

        CategoryView categoryView = getCategory.getById(new GetCategoryQuery(id));
        CategoryResponse dto = categoryMapper.toDto(categoryView);

        return ResponseEntity.ok(dto);
    }
}
