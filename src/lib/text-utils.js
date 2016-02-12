export function inflect(count, singularForm, pluralForm) {
  return count === 1 ? singularForm : pluralForm;
}

export function capitalize(word) {
  return word[0].toUpperCase() + word.slice(1);
}
