
import _ from 'lodash';
import { isGiftCard } from 'paragons/sku';

export function trackPageView(page, fieldsObject) {
  ga('send', 'pageview', page, fieldsObject);
}

/**
 * See: https://developers.google.com/analytics/devguides/collection/analyticsjs/events
 * @method trackEvent(eventCategory, eventAction, eventLabel, ...)
 */
export function trackEvent(...args) {
  ga('send', 'event', ...args);
}

export function setUserId(userId) {
  if (userId != null) {
    try {
      ga('set', 'userId', userId);
    } catch (ex) {
      // ignore errors here
    }
  }
}

export function initTracker(userId) {
  ga('require', 'ec');
  setUserId(userId);
}

function baseProductData(product) {
  return {
    id: product.sku || _.get(product, 'skus.0'),
    name: product.title || product.name,
    category: isGiftCard(product) ? 'GiftCard' : _.get(product, 'tags.0'),
  };
}

export function addProduct(product, extraFields = {}) {
  const data = {
    ...baseProductData(product),
    ...extraFields,
  };

  ga('ec:addProduct', data);
}

export function addImpression(product, position, list = 'Product List') {
  ga('ec:addImpression', {
    ...baseProductData(product),
    position,
    list,
  });
}

export function sendImpressions(list = 'Product List') {
  ga('send', 'event', 'UX', 'impression', list);
}

export function viewDetails(product) {
  addProduct(product);
  ga('ec:setAction', 'detail');
  ga('send', 'event', 'UX', 'detail_view', 'Product');
}

export function addToCart(product, quantity) {
  addProduct(product, {
    price: _.get(product, 'price', product.salePrice),
    quantity,
  });
  ga('ec:setAction', 'add');
  ga('send', 'event', 'UX', 'click', 'add to cart');
}

export function removeFromCart(product, quantity) {
  addProduct(product, {
    price: _.get(product, 'price', product.salePrice),
    quantity,
  });
  ga('ec:setAction', 'remove');
  ga('send', 'event', 'UX', 'click', 'remove from cart');
}

export function clickPdp(product, position, list = 'Product List') {
  addProduct(product, {
    position,
  });
  ga('ec:setAction', 'click', {list});
  ga('send', 'event', 'UX', 'click', list);
}

export function addLineItems(lineItems) {
  _.each(lineItems.skus, sku => {
    addProduct(sku);
  });
}

export function checkoutStart(lineItems) {
  addLineItems(lineItems);
  ga('ec:setAction', 'checkout', {
    step: 1,
  });
  ga('send', 'event', 'Checkout', 'Start');
}


export function chooseShippingMethod(method) {
  ga('ec:setAction', 'checkout_option', {
    step: 2,
    option: method,
  });
  ga('send', 'event', 'Checkout', 'Option');
}
