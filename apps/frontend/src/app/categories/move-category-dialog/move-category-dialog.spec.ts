import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';

import { CategoryNodeResponse, CategoryType } from '../../../generated/api';
import { CategoriesState, FlatCategory } from '../../services/categories-state';
import { NotificationService } from '../../services/notification-service';
import { MoveCategoryDialog } from './move-category-dialog';

const category: CategoryNodeResponse = {
  id: 'moved-1',
  type: CategoryType.Expense,
  name: 'Utilities',
  color: '#00838F',
  displayOrder: 0,
  archived: false,
  children: [],
};

describe('MoveCategoryDialog', () => {
  let fixture: ComponentFixture<MoveCategoryDialog>;
  let component: MoveCategoryDialog;
  let categoriesState: {
    moveTargets: ReturnType<typeof vi.fn>;
    move: ReturnType<typeof vi.fn>;
  };
  let dialogRef: { close: ReturnType<typeof vi.fn> };
  let notifications: { error: ReturnType<typeof vi.fn> };

  const targets: FlatCategory[] = [
    { id: 'e-1', name: 'Housing', type: CategoryType.Expense, depth: 0, archived: false },
  ];

  // targets/newParentId/submit are protected: reach them through a loose handle
  const asAny = () =>
    component as unknown as {
      targets: FlatCategory[];
      newParentId: FormControl<string | null>;
      submit: () => void;
    };

  beforeEach(() => {
    categoriesState = {
      moveTargets: vi.fn(() => targets),
      move: vi.fn(() => of(undefined)),
    };
    dialogRef = { close: vi.fn() };
    notifications = { error: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        { provide: CategoriesState, useValue: categoriesState },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: NotificationService, useValue: notifications },
        { provide: MAT_DIALOG_DATA, useValue: { category } },
        provideTranslateService(), // the template imports TranslatePipe
      ],
    });
    fixture = TestBed.createComponent(MoveCategoryDialog);
    component = fixture.componentInstance;
  });

  afterEach(() => fixture.destroy());

  it('renders the root option plus one option per target', () => {
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    (host.querySelector('mat-select') as HTMLElement).click();
    fixture.detectChanges();

    expect(document.querySelectorAll('mat-option')).toHaveLength(2);
  });

  it('asks the state for the valid targets of the moved category', () => {
    expect(categoriesState.moveTargets).toHaveBeenCalledWith('moved-1');
    expect(asAny().targets).toEqual(targets);
  });

  it('moves to the selected parent and closes', () => {
    asAny().newParentId.setValue('e-1');

    asAny().submit();

    expect(categoriesState.move).toHaveBeenCalledWith('moved-1', 'e-1');
    expect(dialogRef.close).toHaveBeenCalledOnce();
  });

  it('moves to the root when no parent is selected', () => {
    asAny().submit();

    expect(categoriesState.move).toHaveBeenCalledWith('moved-1', null);
  });

  it('keeps the dialog open and notifies on a failed move', () => {
    categoriesState.move.mockReturnValue(throwError(() => new Error('409')));

    asAny().submit();

    expect(notifications.error).toHaveBeenCalledOnce();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
