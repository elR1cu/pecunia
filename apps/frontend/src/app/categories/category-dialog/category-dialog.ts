import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import {
  CategoryNodeResponse,
  CategoryResponse,
  CategoryType,
  UpdateCategoryRequest,
} from '../../../generated/api';
import { CategoriesState } from '../../services/categories-state';
import { NotificationService } from '../../services/notification-service';

export type CategoryDialogData =
  { mode: 'create'; type: CategoryType } | { mode: 'edit'; category: CategoryNodeResponse };

const HEX_COLOR = /^#[0-9A-Fa-f]{6}$/;

/** Curated Material Icons relevant to personal budgeting. */
const ICON_SUGGESTIONS = [
  'shopping_cart',
  'restaurant',
  'home',
  'directions_car',
  'commute',
  'bolt',
  'local_hospital',
  'school',
  'flight',
  'redeem',
  'savings',
  'payments',
  'work',
  'sports_esports',
  'pets',
  'checkroom',
];

const COLOR_PRESETS = [
  '#C62828',
  '#EF6C00',
  '#F9A825',
  '#2E7D32',
  '#00838F',
  '#1565C0',
  '#4527A0',
  '#AD1457',
  '#6D4C41',
  '#546E7A',
];

@Component({
  selector: 'app-category-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButton,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    TranslatePipe,
  ],
  templateUrl: './category-dialog.html',
  styleUrl: './category-dialog.scss',
})
export class CategoryDialog {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<CategoryDialog>);
  private readonly categoriesState = inject(CategoriesState);
  private readonly notifications = inject(NotificationService);

  protected readonly data = inject<CategoryDialogData>(MAT_DIALOG_DATA);
  protected readonly type = this.data.mode === 'create' ? this.data.type : this.data.category.type;
  protected readonly iconSuggestions = ICON_SUGGESTIONS;
  protected readonly colorPresets = COLOR_PRESETS;
  protected readonly submitting = signal(false);

  /** Same-type, non-archived categories offered as parents at creation. */
  protected readonly parentOptions = computed(() =>
    this.categoriesState
      .flat()
      .filter((category) => category.type === this.categoryType() && !category.archived),
  );

  protected readonly form = this.fb.group({
    name: this.fb.nonNullable.control(this.initial('name') ?? '', [
      Validators.required,
      Validators.maxLength(100),
    ]),
    color: this.fb.nonNullable.control(this.initial('color') ?? COLOR_PRESETS[0], [
      Validators.required,
      Validators.pattern(HEX_COLOR),
    ]),
    icon: this.fb.nonNullable.control(this.initial('icon') ?? '', Validators.maxLength(50)),
    parentId: this.fb.control<string | null>(null),
    displayOrder: this.fb.nonNullable.control(this.initial('displayOrder') ?? 0, [
      Validators.required,
      Validators.min(0),
    ]),
  });

  protected submit(): void {
    if (this.form.invalid) return;
    this.submitting.set(true);
    const call = this.data.mode === 'create' ? this.create() : this.update(this.data.category);
    call.subscribe({
      next: () => this.dialogRef.close(),
      error: (err) => {
        this.submitting.set(false);
        this.notifications.error(err);
      },
    });
  }

  protected pickColor(color: string): void {
    this.form.controls.color.setValue(color);
    this.form.controls.color.markAsDirty();
  }

  protected pickIcon(icon: string): void {
    const current = this.form.controls.icon.value;
    this.form.controls.icon.setValue(current === icon ? '' : icon);
    this.form.controls.icon.markAsDirty();
  }

  private categoryType(): CategoryType {
    return this.type;
  }

  private initial<K extends 'name' | 'color' | 'icon' | 'displayOrder'>(
    field: K,
  ): CategoryNodeResponse[K] | undefined {
    return this.data.mode === 'edit' ? this.data.category[field] : undefined;
  }

  private create(): Observable<CategoryResponse> {
    const raw = this.form.getRawValue();
    const parentId = raw.parentId ?? undefined;
    return this.categoriesState.create({
      type: this.categoryType(),
      name: raw.name.trim(),
      color: raw.color.toUpperCase(),
      icon: raw.icon.trim() || undefined,
      parentId,
      displayOrder: parentId
        ? this.categoriesState.nextChildOrder(parentId)
        : this.categoriesState.nextRootOrder(this.categoryType()),
    });
  }

  /** Partial update: only dirty fields are sent; a cleared icon is sent as ''. */
  private update(category: CategoryNodeResponse): Observable<CategoryResponse> {
    const controls = this.form.controls;
    const request: UpdateCategoryRequest = {
      ...(controls.name.dirty && { name: controls.name.value.trim() }),
      ...(controls.color.dirty && { color: controls.color.value.toUpperCase() }),
      ...(controls.icon.dirty && { icon: controls.icon.value.trim() }),
      ...(controls.displayOrder.dirty && { displayOrder: controls.displayOrder.value }),
    };
    return this.categoriesState.update(category.id, request);
  }
}
