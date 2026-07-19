import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';

import { CategoryNodeResponse, CategoryResponse, CategoryType } from '../../../generated/api';
import { CategoriesState, FlatCategory } from '../../services/categories-state';
import { NotificationService } from '../../services/notification-service';
import { CategoryDialog, CategoryDialogData } from './category-dialog';

function aNode(overrides: Partial<CategoryNodeResponse> = {}): CategoryNodeResponse {
  return {
    id: 'id-1',
    type: CategoryType.Expense,
    name: 'Groceries',
    color: '#1A2B3C',
    icon: 'shopping_cart',
    displayOrder: 3,
    archived: false,
    children: [],
    ...overrides,
  };
}

describe('CategoryDialog', () => {
  let categoriesState: {
    flat: ReturnType<typeof signal<FlatCategory[]>>;
    nextRootOrder: ReturnType<typeof vi.fn>;
    nextChildOrder: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let dialogRef: { close: ReturnType<typeof vi.fn> };
  let notifications: { error: ReturnType<typeof vi.fn> };

  // form/submit/pick* are protected: reach them through a loose handle
  const asAny = (component: CategoryDialog) =>
    component as unknown as {
      form: FormGroup;
      submit: () => void;
      pickColor: (color: string) => void;
      pickIcon: (icon: string) => void;
      parentOptions: () => FlatCategory[];
    };

  let fixture: ComponentFixture<CategoryDialog>;

  function newDialog(data: CategoryDialogData): CategoryDialog {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        { provide: CategoriesState, useValue: categoriesState },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: NotificationService, useValue: notifications },
        { provide: MAT_DIALOG_DATA, useValue: data },
        provideTranslateService(), // the template imports TranslatePipe
      ],
    });
    fixture = TestBed.createComponent(CategoryDialog);
    return fixture.componentInstance;
  }

  afterEach(() => fixture?.destroy());

  beforeEach(() => {
    categoriesState = {
      flat: signal<FlatCategory[]>([
        { id: 'e-1', name: 'Housing', type: CategoryType.Expense, depth: 0, archived: false },
        { id: 'e-2', name: 'Old', type: CategoryType.Expense, depth: 0, archived: true },
        { id: 'i-1', name: 'Salary', type: CategoryType.Income, depth: 0, archived: false },
      ]),
      nextRootOrder: vi.fn(() => 2),
      nextChildOrder: vi.fn(() => 5),
      create: vi.fn(() => of({} as CategoryResponse)),
      update: vi.fn(() => of({} as CategoryResponse)),
    };
    dialogRef = { close: vi.fn() };
    notifications = { error: vi.fn() };
  });

  it('offers only same-type, non-archived categories as parents', () => {
    const dialog = newDialog({ mode: 'create', type: CategoryType.Expense });

    expect(asAny(dialog).parentOptions()).toEqual([expect.objectContaining({ id: 'e-1' })]);
  });

  it('creates a root category with a trimmed name, uppercased color and appended order', () => {
    const dialog = newDialog({ mode: 'create', type: CategoryType.Expense });
    const handle = asAny(dialog);
    handle.form.controls['name'].setValue('  Groceries  ');
    handle.pickColor('#1a2b3c');

    handle.submit();

    expect(categoriesState.nextRootOrder).toHaveBeenCalledWith(CategoryType.Expense);
    expect(categoriesState.create).toHaveBeenCalledWith({
      type: CategoryType.Expense,
      name: 'Groceries',
      color: '#1A2B3C',
      icon: undefined,
      parentId: undefined,
      displayOrder: 2,
    });
    expect(dialogRef.close).toHaveBeenCalledOnce();
  });

  it('creates a child under the chosen parent with the sibling-appended order', () => {
    const dialog = newDialog({ mode: 'create', type: CategoryType.Expense });
    const handle = asAny(dialog);
    handle.form.controls['name'].setValue('Rent');
    handle.form.controls['parentId'].setValue('e-1');
    handle.pickIcon('payments');

    handle.submit();

    expect(categoriesState.nextChildOrder).toHaveBeenCalledWith('e-1');
    expect(categoriesState.create).toHaveBeenCalledWith(
      expect.objectContaining({ parentId: 'e-1', icon: 'payments', displayOrder: 5 }),
    );
  });

  it('does not submit an invalid form', () => {
    const dialog = newDialog({ mode: 'create', type: CategoryType.Expense });

    asAny(dialog).submit(); // name is empty → required violation

    expect(categoriesState.create).not.toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('pickIcon() toggles the same icon off', () => {
    const dialog = newDialog({ mode: 'create', type: CategoryType.Expense });
    const handle = asAny(dialog);

    handle.pickIcon('savings');
    expect(handle.form.controls['icon'].value).toBe('savings');

    handle.pickIcon('savings');
    expect(handle.form.controls['icon'].value).toBe('');
  });

  it('edit mode prefills the form and PATCHes only the dirty fields', () => {
    const dialog = newDialog({ mode: 'edit', category: aNode() });
    const handle = asAny(dialog);
    expect(handle.form.controls['color'].value).toBe('#1A2B3C');

    handle.form.controls['name'].setValue('Food');
    handle.form.controls['name'].markAsDirty();
    handle.submit();

    expect(categoriesState.update).toHaveBeenCalledWith('id-1', { name: 'Food' });
    expect(dialogRef.close).toHaveBeenCalledOnce();
  });

  it('edit mode sends an empty icon when it was cleared', () => {
    const dialog = newDialog({ mode: 'edit', category: aNode() });
    const handle = asAny(dialog);

    handle.pickIcon('shopping_cart'); // same as current → toggles to ''
    handle.submit();

    expect(categoriesState.update).toHaveBeenCalledWith('id-1', { icon: '' });
  });

  it('renders the create mode with swatches, icons and the parent select', () => {
    newDialog({ mode: 'create', type: CategoryType.Expense });
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelectorAll('.color-swatch')).toHaveLength(10);
    expect(host.querySelectorAll('.icon-choice')).toHaveLength(16);
    expect(host.querySelector('mat-select')).not.toBeNull();
    expect(host.querySelector('input[type="number"]')).toBeNull();

    // Opening the select renders its options in the CDK overlay: the "none"
    // option plus the one same-type, non-archived candidate.
    (host.querySelector('mat-select') as HTMLElement).click();
    fixture.detectChanges();
    expect(document.querySelectorAll('mat-option')).toHaveLength(2);
  });

  it('renders the edit mode with the order field instead of the parent select', () => {
    newDialog({ mode: 'edit', category: aNode() });
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('mat-select')).toBeNull();
    expect(host.querySelector('input[type="number"]')).not.toBeNull();
  });

  it('keeps the dialog open and notifies on a failed submit', () => {
    categoriesState.create.mockReturnValue(throwError(() => new Error('boom')));
    const dialog = newDialog({ mode: 'create', type: CategoryType.Income });
    const handle = asAny(dialog);
    handle.form.controls['name'].setValue('Salary');

    handle.submit();

    expect(notifications.error).toHaveBeenCalledOnce();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
