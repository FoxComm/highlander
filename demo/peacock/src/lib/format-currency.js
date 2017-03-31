
import _ from 'lodash';
import getSymbol from 'currency-symbol-map';

const isNumber = n => !isNaN(parseFloat(n)) && isFinite(n);

function isInteger(n) {
  return isNumber(n) && n.toString().match(/[.,]/g) == null;
}

function intlFormatCurrency(amount, opts) {
  const modifiedOpts = {...opts};
  let newAmount = amount;
  if (modifiedOpts.fractionBase) {
    newAmount = amount / Math.pow(10, modifiedOpts.fractionBase);
  }

  if (modifiedOpts.currency) {
    modifiedOpts.style = 'currency';
  } else {
    modifiedOpts.useGrouping = true;
    modifiedOpts.minimumFractionDigits = 2;
    modifiedOpts.maximumFractionDigits = 2;
  }
  const formatter = global.Intl.NumberFormat('en-US', modifiedOpts); // eslint-disable-line new-cap
  return formatter.format(newAmount);
}


// variant of formatCurrency but for safe work with bug numbers
// mostly for usage without locale specific options like currency
function formatBigCurrency(amount, opts) {
  const newOpts = {...opts};
  if (newOpts.groupDigits == null) {
    newOpts.groupDigits = true;
  }
  const currencySymbol = getSymbol(newOpts.currency).replace('?', '');

  let val = amount.toString();
  if (val[0] == '-') {
    val = val.slice(1);
  }

  let decimal = val.slice(0, val.length - newOpts.fractionBase);
  if (decimal == '') {
    decimal = '0';
  }

  let fract = val.slice(val.length - newOpts.fractionBase);
  if (fract.length < newOpts.fractionBase) {
    fract = _.repeat('0', newOpts.fractionBase - fract.length) + fract;
  }

  let delimited = decimal;
  if (newOpts.groupDigits) {
    delimited = decimal.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  }
  const sign = amount < 0 ? '-' : '';
  return `${sign}${currencySymbol}${delimited}.${fract}`;
}


function formatCurrency(amount, options) {
  const newAmount = amount || '0';
  if (!isNumber(newAmount)) {
    return null;
  }
  const newOpts = _.assign({}, options);
  if (newOpts.fractionBase == null) {
    newOpts.fractionBase = 2;
  }

  if (newOpts.bigNumber && _.isString(newAmount) && isInteger(newAmount)) {
    return formatBigCurrency(newAmount, newOpts);
  }
  return intlFormatCurrency(newAmount, newOpts);
}


// Convert float string like '153.759' to numeric format string '15376'
// safe to use with big numbers stored in strings
export function stringToCurrency(value, opts) {
  let stringValue = value.toString();
  const dotPos = _.indexOf(value, '.');
  const newOpts = {...opts} || {fractionBase: 2};

  if (dotPos > -1) {
    const fract = `0.${value.slice(dotPos + 1)}`;
    const fixedFract = Number(fract).toFixed(newOpts.fractionBase);
    stringValue = stringValue.slice(0, dotPos);

    if (fixedFract >= 1) { // check overflow of fractional part
      stringValue = stringValue.slice(0, -1) + ((0 | value.slice(-1)) + 1).toString();
    }

    stringValue = stringValue + fixedFract.slice(2);
  } else {
    stringValue = stringValue + _.repeat('0', newOpts.fractionBase);
  }
  return stringValue.replace(/^0+(\d+)/, '$1');
}

export default formatCurrency;
