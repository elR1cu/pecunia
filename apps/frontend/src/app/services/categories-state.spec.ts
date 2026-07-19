import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import {
  CategoryNodeResponse,
  CategoryResponse,
  CategoryService,
  CategoryType,
  CreateCategoryRequest,
} from '../../generated/api';
import { CategoriesState } from './categories-state';

function aNode(overrides: Partial<CategoryNodeResponse> = {}): CategoryNodeResponse {
  return {
    id: 'id-1',
    type: CategoryType.Expense,
    name: 'Groceries',
    color: '#1A2B3C',
    displayOrder: 0,
    archived: false,
    children: [],
    ...overrides,
  };
}

describe('CategoriesState', () => {
  let state: CategoriesState;
  let api: {
    listCategories: ReturnType<typeof vi.fn>;
    createCategory: ReturnType<typeof vi.fn>;
    updateCategory: ReturnType<typeof vi.fn>;
    moveCategory: ReturnType<typeof vi.fn>;
    archiveCategory: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    // fake CategoryService: each method returns an observable we control
    api = {
      listCategories: vi.fn(() => of([])),
      createCategory: vi.fn(() => of({} as CategoryResponse)),
      updateCategory: vi.fn(() => of({} as CategoryResponse)),
      moveCategory: vi.fn(() => of(undefined)),
      archiveCategory: vi.fn(() => of(undefined)),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: CategoryService, useValue: api }],
    });
    state = TestBed.inject(CategoriesState);
  });

  it('load() populates the tree signal from the API', () => {
    const tree = [aNode()];
    api.listCategories.mockReturnValue(of(tree));

    state.load();

    expect(state.tree()).toEqual(tree);
  });

  it('splits the roots by type', () => {
    const expense = aNode({ id: 'e-1' });
    const income = aNode({ id: 'i-1', type: CategoryType.Income, name: 'Salary' });
    api.listCategories.mockReturnValue(of([expense, income]));

    state.load();

    expect(state.expenseRoots()).toEqual([expense]);
    expect(state.incomeRoots()).toEqual([income]);
  });

  it('flat() flattens the hierarchy depth-first with depths', () => {
    const child = aNode({ id: 'id-2', name: 'Vegetables' });
    api.listCategories.mockReturnValue(of([aNode({ children: [child] })]));

    state.load();

    expect(state.flat()).toEqual([
      expect.objectContaining({ id: 'id-1', depth: 0 }),
      expect.objectContaining({ id: 'id-2', depth: 1 }),
    ]);
  });

  it('create() posts the request then refetches the tree', () => {
    const request = { type: CategoryType.Expense } as CreateCategoryRequest;

    state.create(request).subscribe();

    expect(api.createCategory).toHaveBeenCalledWith({ createCategoryRequest: request });
    expect(api.listCategories).toHaveBeenCalled();
  });

  it('update() patches the category then refetches the tree', () => {
    state.update('id-1', { name: 'Food' }).subscribe();

    expect(api.updateCategory).toHaveBeenCalledWith({
      categoryId: 'id-1',
      updateCategoryRequest: { name: 'Food' },
    });
    expect(api.listCategories).toHaveBeenCalled();
  });

  it('move() posts the new parent then refetches the tree', () => {
    state.move('id-1', null).subscribe();

    expect(api.moveCategory).toHaveBeenCalledWith({
      categoryId: 'id-1',
      moveCategoryRequest: { newParentId: null },
    });
    expect(api.listCategories).toHaveBeenCalled();
  });

  it('archive() deletes then refetches the tree (204 has no body)', () => {
    state.archive('id-1').subscribe();

    expect(api.archiveCategory).toHaveBeenCalledWith({ categoryId: 'id-1' });
    expect(api.listCategories).toHaveBeenCalled();
  });

  it('moveTargets() excludes the moved subtree, other types and archived categories', () => {
    const grandChild = aNode({ id: 'moved-child' });
    const moved = aNode({ id: 'moved', children: [grandChild] });
    const sibling = aNode({ id: 'sibling' });
    const archived = aNode({ id: 'archived', archived: true });
    const income = aNode({ id: 'income', type: CategoryType.Income });
    api.listCategories.mockReturnValue(
      of([aNode({ id: 'root', children: [moved, sibling] }), archived, income]),
    );
    state.load();

    const targets = state.moveTargets('moved');

    expect(targets.map((t) => t.id)).toEqual(['root', 'sibling']);
  });

  it('nextRootOrder() and nextChildOrder() append at the end of the siblings', () => {
    const parent = aNode({ id: 'parent', children: [aNode({ id: 'child' })] });
    api.listCategories.mockReturnValue(of([parent, aNode({ id: 'other' })]));
    state.load();

    expect(state.nextRootOrder(CategoryType.Expense)).toBe(2);
    expect(state.nextRootOrder(CategoryType.Income)).toBe(0);
    expect(state.nextChildOrder('parent')).toBe(1);
  });
});
