import { Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatIconButton } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { TranslatePipe } from '@ngx-translate/core';
import { CategoryNodeResponse } from '../../../generated/api';

/**
 * Recursive read-only rendering of a category subtree. Actions are emitted
 * upward (and re-emitted at each level) so only the page talks to the state.
 * A standalone component's own selector is available in its template, which
 * is what makes the recursion work without a self-import.
 */
@Component({
  selector: 'app-category-tree',
  imports: [MatIconModule, MatIconButton, MatMenuModule, TranslatePipe],
  templateUrl: './category-tree.html',
  styleUrl: './category-tree.scss',
})
export class CategoryTree {
  readonly nodes = input.required<CategoryNodeResponse[]>();

  readonly edit = output<CategoryNodeResponse>();
  readonly move = output<CategoryNodeResponse>();
  readonly archive = output<CategoryNodeResponse>();
}
