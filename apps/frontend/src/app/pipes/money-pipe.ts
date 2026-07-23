import { Pipe, PipeTransform } from '@angular/core';
import { Money } from '../../generated/api';

/**
 * Formats a {@link Money} value for display, e.g. `{ amount: '8000.0000',
 * currency: 'CHF' }` renders as `CHF 8'000.00`.
 *
 * The backend serializes `amount` as a decimal string to preserve precision
 * (scale 4, banker's rounding). We deliberately format with `Intl.NumberFormat`
 * rather than Angular's `CurrencyPipe`: the latter routes through the
 * `DecimalPipe` and loses precision on large decimals (angular/angular#23893),
 * and it would require `registerLocaleData('de-CH')` for Swiss grouping. `Intl`
 * is native to the JS engine (ICU), so no locale registration is needed.
 *
 * Display collapses to 2 fraction digits (the CHF default). `Number(amount)` is
 * exact at MVP scale; beyond ~1e15 CHF a string-based formatter would be needed.
 */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  private static readonly LOCALE = 'de-CH';

  // Building an Intl.NumberFormat is comparatively expensive; cache one per
  // currency. The pipe is pure, so it also memoizes on input identity.
  private static readonly formatters = new Map<string, Intl.NumberFormat>();

  transform(value: Money | null | undefined): string {
    if (!value) {
      return '';
    }
    return MoneyPipe.formatterFor(value.currency).format(Number(value.amount));
  }

  private static formatterFor(currency: string): Intl.NumberFormat {
    let formatter = MoneyPipe.formatters.get(currency);
    if (!formatter) {
      formatter = new Intl.NumberFormat(MoneyPipe.LOCALE, { style: 'currency', currency });
      MoneyPipe.formatters.set(currency, formatter);
    }
    return formatter;
  }
}
