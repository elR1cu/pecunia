import { Component, inject } from '@angular/core';
import { IdentityService } from '../../generated/api';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-dashboard',
  imports: [],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly identityService = inject(IdentityService);
  protected readonly user = toSignal(this.identityService.getCurrentUser());
}
