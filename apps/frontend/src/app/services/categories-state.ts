import { computed, inject, Service, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import {
  CategoryNodeResponse,
  CategoryResponse,
  CategoryService,
  CategoryType,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from '../../generated/api';

/** A tree node flattened for selection lists, keeping its depth for indentation. */
export interface FlatCategory {
  id: string;
  name: string;
  type: CategoryType;
  depth: number;
  archived: boolean;
}

function flatten(nodes: CategoryNodeResponse[], depth: number): FlatCategory[] {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name, type: node.type, depth, archived: node.archived },
    ...flatten(node.children, depth + 1),
  ]);
}

function subtreeIds(node: CategoryNodeResponse): string[] {
  return [node.id, ...node.children.flatMap(subtreeIds)];
}

function findNode(nodes: CategoryNodeResponse[], id: string): CategoryNodeResponse | undefined {
  for (const node of nodes) {
    if (node.id === id) return node;
    const inChildren = findNode(node.children, id);
    if (inChildren) return inChildren;
  }
  return undefined;
}

@Service()
export class CategoriesState {
  private readonly categoryService = inject(CategoryService);

  private readonly _tree = signal<CategoryNodeResponse[]>([]);
  readonly tree = this._tree.asReadonly();

  readonly expenseRoots = computed(() =>
    this._tree().filter((node) => node.type === CategoryType.Expense),
  );
  readonly incomeRoots = computed(() =>
    this._tree().filter((node) => node.type === CategoryType.Income),
  );
  readonly flat = computed(() => flatten(this._tree(), 0));

  load(): void {
    this.categoryService.listCategories().subscribe((tree) => this._tree.set(tree));
  }

  create(request: CreateCategoryRequest): Observable<CategoryResponse> {
    return this.categoryService
      .createCategory({ createCategoryRequest: request })
      .pipe(tap(() => this.load()));
  }

  update(id: string, request: UpdateCategoryRequest): Observable<CategoryResponse> {
    return this.categoryService
      .updateCategory({ categoryId: id, updateCategoryRequest: request })
      .pipe(tap(() => this.load()));
  }

  move(id: string, newParentId: string | null): Observable<void> {
    return this.categoryService
      .moveCategory({ categoryId: id, moveCategoryRequest: { newParentId } })
      .pipe(tap(() => this.load()));
  }

  archive(id: string): Observable<void> {
    return this.categoryService.archiveCategory({ categoryId: id }).pipe(tap(() => this.load()));
  }

  /**
   * Valid move targets for a category: same type, not archived, and outside
   * the category's own subtree (the backend rejects cycles with 409; the UI
   * simply never offers them).
   */
  moveTargets(id: string): FlatCategory[] {
    const node = findNode(this._tree(), id);
    if (!node) return [];
    const excluded = new Set(subtreeIds(node));
    return this.flat().filter(
      (candidate) =>
        candidate.type === node.type && !candidate.archived && !excluded.has(candidate.id),
    );
  }

  /** Number of same-type root categories, used to append a new root at the end. */
  nextRootOrder(type: CategoryType): number {
    return this._tree().filter((node) => node.type === type).length;
  }

  /** Number of children under a parent, used to append a new child at the end. */
  nextChildOrder(parentId: string): number {
    return findNode(this._tree(), parentId)?.children.length ?? 0;
  }
}
