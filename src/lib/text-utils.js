export function inflect(count, singlularForm, pluralForm) {
  return count % 10 === 1 ? singlularForm : pluralForm;
}

export function capitalize(word) {
  return word[0].toUpperCase() + word.slice(1);
}
