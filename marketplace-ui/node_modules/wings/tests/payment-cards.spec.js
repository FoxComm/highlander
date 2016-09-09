
import {detectCardType, cardMask, cvvLength, isCardNumberValid, isCvvValid} from '../src/payment-cards';
import assert from 'assert';

describe('payment-cards', function() {
  it('#isCardNumberValid', () => {
    assert.ok(isCardNumberValid('4242 4242 4242 4242'));
    assert.ok(isCardNumberValid('4242424242424242'));
    assert.ok(!isCardNumberValid('424242424242424'));
  });

  it('#detectCardType', () => {
    assert.equal(detectCardType('4242'), 'visa');
    assert.equal(detectCardType('5300'), 'master-card');
  });
});
