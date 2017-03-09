export function zipCode(value) {
  return /^\w{1,9}$/.test(value) ? null : this.t('Enter valid zip code');
}

export function ascii(value) {
  return /^[\x00-\x7F]+$/.test(value) ? null : this.t('%0 must contain only ASCII characters');
}

export function phoneNumber(value) {
  return /^[\d#\-\(\)\+\*) ]+$/.test(value) && value.replace(/[^\d]/g, '').length == 10
    ? null
    : this.t('Please enter a valid phone number');
}

export function email(address) {
  const strLocal = '^(([^<>()\\[\\]\\\\.,;:\\s@\"]+(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)*)|(\".+\"))';
  const strDomain = '@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$';

  return RegExp(strLocal + strDomain).test(address)
    ? null
    : 'Invalid email';
}
