import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { CategoryNodeResponse } from '../../../generated/api';
import { CategoriesState } from '../../services/categories-state';
import { NotificationService } from '../../services/notification-service';

export interface MoveCategoryDialogData {
  category: CategoryNodeResponse;
}

@Component({
  selector: 'app-move-category-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButton,
    MatFormFieldModule,
    MatSelectModule,
    TranslatePipe,
  ],
  templateUrl: './move-category-dialog.html',
})
export class MoveCategoryDialog {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<MoveCategoryDialog>);
  private readonly categoriesState = inject(CategoriesState);
  private readonly notifications = inject(NotificationService);

  protected readonly data = inject<MoveCategoryDialogData>(MAT_DIALOG_DATA);
  protected readonly submitting = signal(false);

  // Same-type categories outside the moved subtree; null means "to the root".
  protected readonly targets = this.categoriesState.moveTargets(this.data.category.id);
  protected readonly newParentId = this.fb.control<string | null>(null);

  protected submit(): void {
    this.submitting.set(true);
    this.categoriesState.move(this.data.category.id, this.newParentId.value).subscribe({
      next: () => this.dialogRef.close(),
      error: (err) => {
        this.submitting.set(false);
        this.notifications.error(err);
      },
    });
  }
}
