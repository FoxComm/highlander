export const validateOperatorAppliance = (operator, type, criterion) => {
  if (!(operator in type.operators)) {
    throw new TypeError(`Operator ${operator} is not applicable for field "${criterion.label}" of type "${type.name}"`);
  }

  if (criterion.operators && !(criterion.operators.includes(operator))) {
    throw new TypeError(`Operator ${operator} is not applicable for field "${criterion.label}"`);
  }
};
