import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { CategoryNodeResponse, CategoryType } from '../../generated/api';
import { CategoriesState } from '../services/categories-state';
import { NotificationService } from '../services/notification-service';
import { Categories } from './categories';

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

describe('Categories', () => {
  let component: Categories;
  let categoriesState: {
    expenseRoots: ReturnType<typeof signal<CategoryNodeResponse[]>>;
    incomeRoots: ReturnType<typeof signal<CategoryNodeResponse[]>>;
    load: ReturnType<typeof vi.fn>;
    archive: ReturnType<typeof vi.fn>;
  };
  let dialog: { open: ReturnType<typeof vi.fn> };
  let notifications: { error: ReturnType<typeof vi.fn> };

  // handlers are protected: reach them through a loose handle
  const asAny = () =>
    component as unknown as {
      openCreate: (type: CategoryType) => void;
      edit: (c: CategoryNodeResponse) => void;
      move: (c: CategoryNodeResponse) => void;
      archive: (c: CategoryNodeResponse) => void;
    };

  beforeEach(() => {
    categoriesState = {
      expenseRoots: signal<CategoryNodeResponse[]>([]),
      incomeRoots: signal<CategoryNodeResponse[]>([]),
      load: vi.fn(),
      archive: vi.fn(() => of(undefined)),
    };
    dialog = { open: vi.fn() };
    notifications = { error: vi.fn() };

    TestBed.configureTestingModule({
      imports: [Categories],
      providers: [
        { provide: CategoriesState, useValue: categoriesState },
        { provide: MatDialog, useValue: dialog },
        { provide: NotificationService, useValue: notifications },
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
      ],
    });
    component = TestBed.createComponent(Categories).componentInstance;
  });

  it('loads the categories on init', () => {
    component.ngOnInit();
    expect(categoriesState.load).toHaveBeenCalledOnce();
  });

  it('openCreate() opens the dialog in create mode with the section type', () => {
    asAny().openCreate(CategoryType.Income);

    expect(dialog.open).toHaveBeenCalledWith(expect.anything(), {
      data: { mode: 'create', type: CategoryType.Income },
    });
  });

  it('edit() opens the dialog in edit mode with the category', () => {
    const category = aNode();

    asAny().edit(category);

    expect(dialog.open).toHaveBeenCalledWith(expect.anything(), {
      data: { mode: 'edit', category },
    });
  });

  it('move() opens the move dialog with the category', () => {
    const category = aNode();

    asAny().move(category);

    expect(dialog.open).toHaveBeenCalledWith(expect.anything(), { data: { category } });
  });

  it('archive() archives the category when the confirm dialog resolves true', () => {
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });

    asAny().archive(aNode({ id: 'id-42' }));

    expect(categoriesState.archive).toHaveBeenCalledWith('id-42');
  });

  it('archive() does nothing when the confirm dialog is dismissed', () => {
    dialog.open.mockReturnValue({ afterClosed: () => of(false) });

    asAny().archive(aNode());

    expect(categoriesState.archive).not.toHaveBeenCalled();
  });
});
