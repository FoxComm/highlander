import apiUtils from `./utils/api`
import cartUtils from `./utils/cart`


class API {

  constuctor(args) {
    this.prefix = args.prefix || '/api';
    this.version = args.version || 'v1';
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

  getProduct(id) {}
  search(args) {}


  // Cart

  getCart() {
    return utils.get(this.uri(''))
  }
  addToCart(items) {
    // Weird. Actually now we have to hold or pass the full current cart state
    // so that we can marshall the data to the new desired state. TBD
    // cartUtils.marshallItems(items)  // eg.
    return utils.post(this.uri('my/cart/line-items'), payload)
  }
  removeFromCart(items) {}
  emptyCart() {}


  // Checkout

  checkout = {
    shippingMethods() {}
    setAddressData() {}
    setBillingData() {}
    reset() {}
    getCountries() {}
    getStates(country) {}
    getCityFromZip(zip) {}
  }


  // Utils

  uri(uri) {
    return `${this.prefix}/${this.version}${uri}`
  }

}

export default API
