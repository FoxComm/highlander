// this is for testing stuff before putting it other files
import webdriver = require('selenium-webdriver');
import { firstBinom, withProbability } from './randomUtils';
import { noop } from 'lodash';

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
    console.log(`Clicking product ${idx}`);
    return products[idx].click();
  } else {
    console.log('Not clicking a product');
    return driver.quit();
  }
}

function waitAndClickId(id: string) {
  return driver
    .wait(until.elementLocated(By.id(id)), 5000, `${id} was not located after 5 seconds`)
    .then((elmt) => {
      driver.sleep(200);
      console.log(`Found ${id}. Clicking it now.`);
      elmt.click();
    });
}

function doWithProbability(fun: () => any, p: number){
  if (withProbability(p)) {
    console.log('doing it');
    
    return fun();
  }

  console.log('not doing it');
  return noop();
}

function checkout() {
  waitAndClickId(CART_CHECKOUT);
  waitAndClickId(STANDARD_SHIPPING);
  waitAndClickId(SHIPPING_SUBMIT);
  waitAndClickId(BILLING_SUBMIT);
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

doWithProbability(() => waitAndClickId(ADD_TO_CART), .5);

doWithProbability(checkout, .5);

driver.quit();
