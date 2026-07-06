import { inject, Service } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from '../../generated/api';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';

function problemDetail(error: unknown): string | null {
  if (error instanceof HttpErrorResponse && error.error) {
    const problem = error.error as ProblemDetail;
    if (problem.detail) return problem.detail;
  }
  return null;
}

@Service()
export class NotificationService {
  private readonly translate = inject(TranslateService);
  private readonly snackBar = inject(MatSnackBar);

  error(error: unknown): void {
    const message = problemDetail(error) ?? this.translate.instant('common.error');
    this.snackBar.open(message, this.translate.instant('common.close'), { duration: 5000 });
  }
}
