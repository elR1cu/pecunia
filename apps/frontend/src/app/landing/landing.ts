import { Component } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-landing',
  imports: [MatButton, TranslatePipe],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
})
export class Landing {}
