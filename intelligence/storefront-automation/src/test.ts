// this is for testing stuff before putting it other files
import webdriver = require('selenium-webdriver')
import { firstBinom } from './randomUtils'

const HOME_URL = 'http://hal.foxcommerce.com'
const PAGE_PATH = 'sunglasses'
const PRODUCTS_LIST = '//*[@id="app"]/div/div/div[2]/section/div'
const LOGIN_EMAIL = '//*[@id="app"]/div/div[1]/div/div/form/div[1]/input'
const LOGIN_PASS = '//*[@id="app"]/div/div[1]/div/div/form/div[2]/div/input'
const LOGIN_SUBMIT = '//*[@id="app"]/div/div[1]/div/div/form/button'

const driver = new webdriver.Builder()
  .forBrowser('firefox')
  .usingServer('http://localhost:4444/wd/hub')
  .build()

const By = webdriver.By
type WebElement = webdriver.WebElement

function pickProduct(products: Array<WebElement>) {
  let n: number = products.length
  let idx: number = firstBinom(0.3, n)
  if(idx > n){
    console.log('not clicking a product')
  } else {
    console.log(`clicking product ${idx}`)
    return products[idx].click()
  }
}

// login
driver.navigate().to(`${HOME_URL}/${PAGE_PATH}?auth=login`)
  .then(() => driver.findElement(By.xpath(LOGIN_EMAIL)).sendKeys('robot01@robot.com'))
  .then(() => driver.findElement(By.xpath(LOGIN_PASS)).sendKeys('password'))
  .then(() => driver.findElement(By.xpath(LOGIN_SUBMIT)).click())

// find a product
driver.navigate().to(`${HOME_URL}/${PAGE_PATH}`)
  .then(() => driver.findElement(By.xpath(PRODUCTS_LIST)))
  .then(list => list.findElements(By.tagName('img')))
  .then((products) => pickProduct(products))
  .then(() => driver.quit())
