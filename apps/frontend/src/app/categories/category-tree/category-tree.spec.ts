import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { CategoryNodeResponse, CategoryType } from '../../../generated/api';
import { CategoryTree } from './category-tree';

function aNode(overrides: Partial<CategoryNodeResponse> = {}): CategoryNodeResponse {
  return {
    id: 'id-1',
    type: CategoryType.Expense,
    name: 'Groceries',
    color: '#1A2B3C',
    icon: 'shopping_cart',
    displayOrder: 0,
    archived: false,
    children: [],
    ...overrides,
  };
}

describe('CategoryTree', () => {
  let fixture: ComponentFixture<CategoryTree>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CategoryTree],
      providers: [provideTranslateService()], // the template imports TranslatePipe
    });
    fixture = TestBed.createComponent(CategoryTree);
  });

  afterEach(() => fixture.destroy());

  function render(nodes: CategoryNodeResponse[]): HTMLElement {
    fixture.componentRef.setInput('nodes', nodes);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  /** The row menus render in the CDK overlay, outside the fixture's DOM. */
  function openedMenuItems(row: Element): HTMLElement[] {
    (row.querySelector('.row-actions') as HTMLElement).click();
    fixture.detectChanges();
    return Array.from(document.querySelectorAll('[mat-menu-item]'));
  }

  it('renders one row per node, recursively, with the archived state', () => {
    const host = render([
      aNode({ children: [aNode({ id: 'id-2', name: 'Vegetables' })] }),
      aNode({ id: 'id-3', name: 'Old', archived: true }),
    ]);

    const rows = host.querySelectorAll('.category-row');
    expect(rows).toHaveLength(3);
    // The child renders inside the parent's nested tree.
    expect(host.querySelector('app-category-tree .category-row')?.textContent).toContain(
      'Vegetables',
    );
    // The archived node shows the chip and loses its actions menu.
    const archivedRow = rows[2];
    expect(archivedRow.querySelector('.archived-chip')).not.toBeNull();
    expect(archivedRow.querySelector('.row-actions')).toBeNull();
  });

  it('emits the node when a menu action is clicked', () => {
    const node = aNode();
    const host = render([node]);
    const component = fixture.componentInstance;
    const emitted: string[] = [];
    component.edit.subscribe(() => emitted.push('edit'));
    component.move.subscribe(() => emitted.push('move'));
    component.archive.subscribe(() => emitted.push('archive'));
    const row = host.querySelector('.category-row')!;

    // The menu closes after each pick: reopen it for every action.
    openedMenuItems(row)[0].click();
    fixture.detectChanges();
    openedMenuItems(row)[1].click();
    fixture.detectChanges();
    openedMenuItems(row)[2].click();

    expect(emitted).toEqual(['edit', 'move', 'archive']);
  });
});
