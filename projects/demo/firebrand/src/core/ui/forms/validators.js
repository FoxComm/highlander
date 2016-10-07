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
