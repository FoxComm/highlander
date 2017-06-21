const API_CATEGORY_NAMES = [
  'customers', 'customerAddresses', 'customerCreditCards', 'customerGroups', 'skus',
  'products', 'productAlbums', 'giftCards', 'promotions', 'coupons', 'couponCodes', 'albums',
  'notes', 'dev', 'storeAdmins', 'cart', 'carts', 'inventories', 'orders', 'sharedSearches',
];

function getAllMethodNames(object) {
  if (!object) return [];
  const temp = {};
  for (const key of Object.getOwnPropertyNames(object)) {
    temp[key] = true;
  }
  const proto = Object.getPrototypeOf(object);
  if (proto && proto.constructor.name !== 'Object') {
    for (const methodName of getAllMethodNames(proto)) {
      temp[methodName] = true;
    }
  }
  return Object.keys(temp).filter(key =>
    typeof object[key] === 'function',
  );
}

/* eslint no-param-reassign: 0 */
/* eslint no-extra-bind: 0 */
/* eslint guard-for-in: 0 */
export default function wrapWithApiLogging(api) {
  api.testContext.apiLog = [];
  for (const apiCategoryName of API_CATEGORY_NAMES) {
    const apiCategory = api[apiCategoryName];
    for (const methodName of getAllMethodNames(apiCategory)) {
      if (methodName === 'constructor') continue;
      const fn = apiCategory[methodName].bind(apiCategory);
      apiCategory[methodName] = (...args) => {
        const apiLogEntry = { apiCategoryName, methodName, args };
        api.testContext.apiLog.push(apiLogEntry);
        return fn(...args).then((result) => {
          apiLogEntry.result = result;
          return result;
        });
      };
    }
  }
}
