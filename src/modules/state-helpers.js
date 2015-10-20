
export function modelIdentity(type, model) {
  switch (type) {
    case 'order':
      return model.referenceNumber;
    case 'gift-card':
      return model.code;
    default:
      return model.id;
  }
}
