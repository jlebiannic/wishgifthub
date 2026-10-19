import {describe, expect, it} from 'vitest'
import {parseEuroCents, thresholdRule} from '../price'

describe('euro amounts', () => {
  it.each([
    ['50', 5000], ['50 €', 5000], ['50,01 €', 5001], ['EUR 50.01', 5001],
    ['1 234,56 €', 123456], ['1\u202f234,56', 123456], ['1.234,56', 123456],
    ['1,234.56', 123456], ['0', 0], ['0,01', 1], ['50.1', 5010]
  ])('parses %s exactly in cents', (value, cents) => {
    expect(parseEuroCents(value)).toBe(cents)
  })

  it.each(['', 'gratuit', 'à partir de 60 €', '50-70 €', '$60', '60 USD', '-10', 'NaN',
    'Infinity', '50,001', '1,234', '99999999999999999999'])('does not guess the value of %s', value => {
    expect(parseEuroCents(value)).toBeNull()
  })

  it('validates threshold bounds and decimal precision', () => {
    expect(thresholdRule('0')).toBe(true)
    expect(thresholdRule('99999999,99')).toBe(true)
    for (const value of ['-1', '100000000', '1.001', '', 'texte']) {
      expect(thresholdRule(value)).not.toBe(true)
    }
  })
})
