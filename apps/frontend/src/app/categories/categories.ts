import { Component, inject, OnInit } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CategoryNodeResponse, CategoryType } from '../../generated/api';
import { ConfirmDialog } from '../components/confirm-dialog/confirm-dialog';
import { CategoriesState } from '../services/categories-state';
import { NotificationService } from '../services/notification-service';
import { CategoryDialog, CategoryDialogData } from './category-dialog/category-dialog';
import { CategoryTree } from './category-tree/category-tree';
import {
  MoveCategoryDialog,
  MoveCategoryDialogData,
} from './move-category-dialog/move-category-dialog';

@Component({
  selector: 'app-categories',
  imports: [MatButton, MatIconModule, TranslatePipe, CategoryTree],
  templateUrl: './categories.html',
  styleUrl: './categories.scss',
})
export class Categories implements OnInit {
  private readonly categoriesState = inject(CategoriesState);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);
  private readonly translate = inject(TranslateService);

  protected readonly expenseRoots = this.categoriesState.expenseRoots;
  protected readonly incomeRoots = this.categoriesState.incomeRoots;
  protected readonly CategoryType = CategoryType;

  ngOnInit(): void {
    this.categoriesState.load();
  }

  protected openCreate(type: CategoryType): void {
    const data: CategoryDialogData = { mode: 'create', type };
    this.dialog.open(CategoryDialog, { data });
  }

  protected edit(category: CategoryNodeResponse): void {
    const data: CategoryDialogData = { mode: 'edit', category };
    this.dialog.open(CategoryDialog, { data });
  }

  protected move(category: CategoryNodeResponse): void {
    const data: MoveCategoryDialogData = { category };
    this.dialog.open(MoveCategoryDialog, { data });
  }

  protected archive(category: CategoryNodeResponse): void {
    const ref = this.dialog.open(ConfirmDialog, {
      data: {
        title: this.translate.instant('categories.archiveConfirmTitle'),
        message: this.translate.instant('categories.archiveConfirmMessage', {
          name: category.name,
        }),
      },
    });
    ref.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.categoriesState.archive(category.id).subscribe({
          error: (err) => this.notifications.error(err),
        });
      }
    });
  }
}
