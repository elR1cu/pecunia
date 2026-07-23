import { Money } from '../../generated/api';
import { MoneyPipe } from './money-pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();
  const chf = (amount: string): Money => ({ amount, currency: 'CHF' });

  // Reference formatter mirrors the pipe's configuration, so the assertions
  // stay robust against ICU differences in the group/minus glyphs across engines.
  const reference = (amount: number, currency = 'CHF') =>
    new Intl.NumberFormat('de-CH', { style: 'currency', currency }).format(amount);

  it('renders a CHF amount the Swiss way with two decimals', () => {
    const result = pipe.transform(chf('8000.0000'));

    expect(result).toContain('CHF');
    expect(result).toMatch(/\.00$/); // the reported bug: 4 stored decimals → 2 shown
    expect(result).toBe(reference(8000));
  });

  it('rounds the fractional part down to two decimals', () => {
    expect(pipe.transform(chf('1234.5678'))).toMatch(/\.57$/);
  });

  it('formats negative amounts', () => {
    expect(pipe.transform(chf('-5210.35'))).toBe(reference(-5210.35));
  });

  it('honours the currency carried by the Money value', () => {
    expect(pipe.transform({ amount: '10.00', currency: 'EUR' })).toBe(reference(10, 'EUR'));
  });

  it('returns an empty string for null or undefined', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
  });
});
