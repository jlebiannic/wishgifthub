export const DEFAULT_COLLECTIVE_GIFT_THRESHOLD = 50

/** Parse an unambiguous euro amount in cents without guessing from free-form text. */
export function parseEuroCents(value: string): number | null {
  const amount = value.trim().replace(/^(?:€|EUR)\s*|\s*(?:€|EUR)$/gi, '').trim()
  const normalized = amount.replace(/[\s\u00a0\u202f]/g, '')
  let decimal = normalized
  if (/^\d{1,3}(?:\.\d{3})+,\d{1,2}$/.test(normalized)) {
    decimal = normalized.replace(/\./g, '').replace(',', '.')
  } else if (/^\d{1,3}(?:,\d{3})+\.\d{1,2}$/.test(normalized)) {
    decimal = normalized.replace(/,/g, '')
  } else {
    decimal = normalized.replace(',', '.')
  }
  if (!/^\d+(?:\.\d{1,2})?$/.test(decimal)) return null
  const [whole, fraction = ''] = decimal.split('.')
  const cents = Number(whole) * 100 + Number(fraction.padEnd(2, '0'))
  return Number.isSafeInteger(cents) ? cents : null
}

export function thresholdRule(value: string): boolean | string {
  const cents = parseEuroCents(value)
  return (cents !== null && cents <= 9999999999)
    || 'Saisissez un montant entre 0 et 99 999 999,99 €, avec au maximum 2 décimales'
}
