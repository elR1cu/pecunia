import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { provideTranslateService } from '@ngx-translate/core';

import { ConfirmDialog, ConfirmData } from './confirm-dialog';

describe('ConfirmDialog', () => {
  it('exposes the data injected through MAT_DIALOG_DATA', () => {
    const data: ConfirmData = { title: 'Archive account', message: 'Archive « UBS »?' };

    TestBed.configureTestingModule({
      imports: [ConfirmDialog],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: data },
        provideTranslateService(), // the template imports TranslatePipe
      ],
    });
    const component = TestBed.createComponent(ConfirmDialog).componentInstance;

    expect((component as unknown as { data: ConfirmData }).data).toBe(data);
  });
});
