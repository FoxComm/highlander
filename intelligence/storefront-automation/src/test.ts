// this is for testing stuff before putting it other files
import webdriver = require('selenium-webdriver');
import { firstBinom } from './randomUtils';

const BROWSER = 'firefox';
const DRIVER_URL = 'http://localhost';
const DRIVER_PORT = '4444';

const HOME_URL = 'http://hal.foxcommerce.com';
// const HOME_URL = 'http://localhost:5045';
const CATEGORY = 'all';

// element ids
const PRODUCTS_LIST = 'products-list';
const PRODUCT = 'product';
const LOGIN_EMAIL = 'login-email';
const LOGIN_PASS = 'login-password';
const LOGIN_SUBMIT = 'login-submit';
const ADD_TO_CART = 'add-to-cart';
const CART_CHECKOUT = 'cart-checkout';
const STANDARD_SHIPPING = 'delivery2';
const SHIPPING_SUBMIT = 'delivery-method-submit';
const BILLING_SUBMIT = 'payment-method-submit';

const driver = new webdriver.Builder()
  .forBrowser(BROWSER)
  .usingServer(`${DRIVER_URL}:${DRIVER_PORT}/wd/hub`)
  .build();

const By = webdriver.By;
const until = webdriver.until;
type WebElement = webdriver.WebElement;

function pickProduct(products: Array<WebElement>) {
  let n: number = products.length;
  let idx: number = firstBinom(0.3, n);
  if(idx < n){
    console.log(`clicking product ${idx}`);
    return products[idx].click();
  } else {
    console.log('not clicking a product');
    return driver.quit();
  }
}

function waitAndClickId(id: string) {
  return driver
    .wait(until.elementLocated(By.id(id)), 5000, `${id} was not located after 5 seconds`)
    .then((elmt) => {
      console.log(`I think I've found ${id}. I'll try clicking it now.`);
      elmt.click();
    });
}

// login
driver.navigate().to(`${HOME_URL}/${CATEGORY}?auth=login`);
driver.findElement(By.id(LOGIN_EMAIL)).sendKeys('robot01@robot.com');
driver.findElement(By.id(LOGIN_PASS)).sendKeys('password');
driver.findElement(By.id(LOGIN_SUBMIT)).click()
  .then(() => driver.sleep(2000));

// find a product
driver.findElement(By.id(PRODUCTS_LIST))
  .findElements(By.id(PRODUCT))
  .then((products) => pickProduct(products));

// add to cart
waitAndClickId(ADD_TO_CART);

// checkout
waitAndClickId(CART_CHECKOUT);
waitAndClickId(STANDARD_SHIPPING);
waitAndClickId(SHIPPING_SUBMIT);
waitAndClickId(BILLING_SUBMIT);
driver.quit();
