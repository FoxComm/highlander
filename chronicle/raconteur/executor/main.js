const _ = require('lodash');
const vm = require('vm');
const fox = require('api-js');
const superagent = require('superagent');


function createApi(apiBaseUrl ,stripeKey, options = {}) {

  if (!apiBaseUrl) {
    throw new Error('API_URL is not defined in process.env');
  }

  let agent = superagent;

  if (options.preserveCookies) {
    agent = superagent.agent();
  }

  return new fox.default({
    api_url: apiBaseUrl + '/api',
    stripe_key: stripeKey,
    agent,
    handleResponse: options.handleResponse,
  });
}

//Parse args
let apiUrl = process.argv[2]; 
let stripeKey = process.argv[3]; 
let jwt = process.argv[4]; 
let type = process.argv[5]; 
let dataBase64 = process.argv[6];
let scriptBase64 = process.argv[7];

//decode arguments
let data = new Buffer(dataBase64, 'base64').toString('ascii')
let jsonData = JSON.parse(data);

let scriptText = new Buffer(scriptBase64, 'base64').toString('ascii')

//setup execution context
let api = createApi(apiUrl, stripeKey);

console.log("ACTIVITY: " + type);
console.log("DATA: " + data);
console.log("SCRIPT: " + scriptText);

const sandbox = {
  console: console,
  api: api,
  type: type,
  data: jsonData
};

//run script
const script = new vm.Script(scriptText);
script.runInNewContext(sandbox, {displayErrors: true});




