const isNumber = n => !isNaN(parseFloat(n)) && isFinite(n);

const formatCurrency = (amount, base, currency) => {
  if (!(isNumber(amount) && isNumber(base))) {
    return null;
  }
  currency = currency || '';
  const sign = amount < 0 ? '-' : '';
  const parsed = (Math.abs(amount) / base).toFixed(2);
  const decimal = parsed.slice(0, -3);
  const fract = parsed.slice(-2);
  const delimited = decimal.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return `${sign}${currency}${delimited}.${fract}`;
};

export default formatCurrency;
