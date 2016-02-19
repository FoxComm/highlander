export function inflect(count, [singularForm, pluralForm]) {
  return count === 1 ? singularForm : pluralForm;
}

export function getSingularForm([singularForm, pluralForm]) {
  return singularForm;
}

export function getPluralForm([singularForm, pluralForm]) {
  return pluralForm;
}

export function capitalize(word) {
  return word[0].toUpperCase() + word.slice(1);
}

export function toConstName(word) {
  const name = [];

  for (var i = 0; i < word.length; i++) {
    if (word[i] >= 'A' && word[i] <= 'Z') {
      name.push('_');
    }
    name.push(word[i].toUpperCase());
  }

  return name.join('');
}
