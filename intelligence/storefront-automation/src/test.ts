// this is for testing stuff before putting it other files
import webdriver = require('selenium-webdriver');
import { firstBinom } from './randomUtils';

const BROWSER = 'firefox';
const DRIVER_URL = 'http://localhost';
const DRIVER_PORT = '4444';

const HOME_URL = 'http://hal.foxcommerce.com';
// const HOME_URL = 'http://localhost:5045'
const CATEGORY = 'all';

// element ids
const PRODUCTS_LIST = 'products-list';
const PRODUCT = 'product';
const LOGIN_EMAIL = 'login-email';
const LOGIN_PASS = 'login-password';
const LOGIN_SUBMIT = 'login-submit';
const ADD_TO_CART = 'add-to-cart';
const CART_CHECKOUT = 'cart-checkout';

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

// login
driver.navigate().to(`${HOME_URL}/${CATEGORY}?auth=login`);
driver.findElement(By.id(LOGIN_EMAIL)).sendKeys('robot01@robot.com');
driver.findElement(By.id(LOGIN_PASS)).sendKeys('password');
driver.findElement(By.id(LOGIN_SUBMIT)).click()
  .then(() => driver.sleep(1000));

// find a product
driver.findElement(By.id(PRODUCTS_LIST))
  .findElements(By.id(PRODUCT))
  .then((products) => pickProduct(products));

// add to cart
driver.wait(until.elementLocated(By.id(ADD_TO_CART)), 5000,
  'PDP should load within 5 seconds');
driver.findElement(By.id(ADD_TO_CART)).click();

// checkout
driver.wait(until.elementLocated(By.id(CART_CHECKOUT)), 5000,
  'cart should load within 5 seconds');
driver.sleep(1000)
driver.findElement(By.id(CART_CHECKOUT)).click();
driver.quit();
