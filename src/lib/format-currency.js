import _ from 'lodash';
import getSymbol from 'currency-symbol-map';

const isNumber = n => !isNaN(parseFloat(n)) && isFinite(n);

function isInteger(n) {
  return isNumber(n) && n.toString().match(/[.,]/g) == null;
}

function _currentLocale() {
  if (global.navigator) {
    return global.navigator.userLanguage || global.navigator.language || undefined;
  }
}

const currentLocale = _.memoize(_currentLocale);

function intlFormatCurrency(amount, opts) {
  if (opts.fractionBase) {
    amount = amount / Math.pow(10, opts.fractionBase);
  }

  if (opts.currency) {
    opts.style = 'currency';
  } else {
    opts.useGrouping = true;
    opts.minimumFractionDigits = 2;
    opts.maximumFractionDigits = 2;
  }
  const formatter = global.Intl.NumberFormat(currentLocale(), opts);
  return formatter.format(amount);
}


function formatCurrency(amount, options) {
  amount = amount || '0';
  if (!isNumber(amount)) {
    return null;
  }
  const opts = _.assign({}, options);
  if (opts.fractionBase == null) {
    opts.fractionBase = 2;
  }

  if (opts.bigNumber && _.isString(amount) && isInteger(amount)) {
    return formatBigCurrency(amount, opts);
  }
  return intlFormatCurrency(amount, opts);
}

// variant of formatCurrency but for safe work with bug numbers
// mostly for usage without locale specific options like currency
function formatBigCurrency(amount, opts) {
  if (opts.groupDigits == null) {
    opts.groupDigits = true;
  }
  const currencySymbol = getSymbol(opts.currency).replace('?', '');

  let val = amount.toString();
  if (val[0] == '-') {
    val = val.slice(1);
  }

  let decimal = val.slice(0, val.length - opts.fractionBase);
  if (decimal == '') {
    decimal = '0';
  }

  let fract = val.slice(val.length-opts.fractionBase);
  if (fract.length < opts.fractionBase) {
    fract = _.repeat('0', opts.fractionBase - fract.length) + fract;
  }

  let delimited = decimal;
  if (opts.groupDigits) {
    delimited = decimal.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  }
  const sign = amount < 0 ? '-' : '';
  return `${sign}${currencySymbol}${delimited}.${fract}`;
}


// Convert float string like '153.759' to numeric format string '15376'
// safe to use with big numbers stored in strings
export function stringToCurrency(value, opts) {
  value = value.toString();
  const dotPos = _.indexOf(value, '.');
  opts = opts || {fractionBase: 2};

  if (dotPos > -1) {
    const fixedFract = Number('0.' + value.slice(dotPos + 1)).toFixed(opts.fractionBase);
    value = value.slice(0, dotPos);

    if (fixedFract >= 1) { // check overflow of fractional part
      value = value.slice(0, -1) + ((0 | value.slice(-1)) + 1).toString();
    }

    value = value + fixedFract.slice(2);
  } else {
    value = value + _.repeat('0', opts.fractionBase);
  }
  return value.replace(/^0+(\d+)/, '$1');
}

export default formatCurrency;
