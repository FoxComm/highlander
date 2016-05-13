import apiUtils from `./utils/api`
import cartUtils from `./utils/cart`


class API {

  constuctor(args) {
    if(!args.api_url) return new Error('you must specify an API URL')
    this.api_url = args.api_url.replace(/\/?$/, '/') // ensure trailing slash
    this.prefix = args.prefix || '/api'
    this.version = args.version || 'v1'
  }


  // Auth

  signup(credentials) {
    return utils.post(this.uri('/public/registrations/new'), credentials)
  }
  login(credentials) {
    return utils.post(this.uri('/public/login'), credentials)
  }
  loginWith(authProvider='google', credentials) {}
  logout() {}


  // Product Catalog

  getProduct(id) {
    // TODO: detect and normalize Int id or String slug, and get accordingly
  }
  search(args) {}


  // Cart

  getCart() {
    return utils.get(this.uri(''))
  }
  addToCart(items, cart) {
    // TODO: should handle items as either
    // { item }
    // or
    // [ { item }, { item }]
    // should detect & normalize data, same for `removeFromCart()`

    // Weird. We have to hold or pass the full current cart state
    // so that we can marshall the data to the new desired state. TBD
    // see: https://github.com/FoxComm/api-js/issues/2
    // cartUtils.marshallItems(items)  // eg.
    return utils.post(this.uri('my/cart/line-items'), payload)
  }
  removeFromCart(items, cart) {}
  emptyCart() {}


  // Checkout

  checkout = {
    getShippingMethods() {}
    setAddressData() {}
    setBillingData() {}
    reset() {}
    getCountries() {}
    getStates(country) {}
    getCityFromZip(zip) {}
    finish() {}
  }


  // Utils

  uri(uri) {
    return `${this.api_url}${this.prefix}/${this.version}${uri}`
  }

}

export default API
