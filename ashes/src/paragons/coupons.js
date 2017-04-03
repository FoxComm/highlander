
export function createEmptyCoupon() {
  const usageRules = {
    isUnlimitedPerCode: false,
    usesPerCode: 1,
    isUnlimitedPerCustomer: false,
    usesPerCustomer: 1,
  };

  return {
    id: null,
    createdAt: null,
    attributes: {
      usageRules: {
        t: 'usageRules',
        v: usageRules,
      }
    },
    promotion: ''
  };
}
