export const validateOperatorAppliance = (operator, type, field) => {
  if (!(operator in type.operators)) {
    throw new TypeError(`Operator ${operator} is not applicable for field "${field.label}" of type "${type.name}"`);
  }

  if (field.operators && !(operator in field.operators)) {
    throw new TypeError(`Operator ${operator} is not applicable for field "${field.label}"`);
  }
};
