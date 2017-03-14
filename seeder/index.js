const system = require('system');
const request = require('superagent');
const config = require('./config.json');
const products = require('./fixtures/products.json');

async function login(config) {
    const { url, org, email, password } = config;
    const resp = await request
        .post(`${url}/api/v1/public/login`)
        .send({ org, email, password })
        .set('Content-Type', 'application/json');

    return resp.headers.jwt;
}

async function createProduct(url, jwt, idx, product) {
    const resp = await request
          .post(`${url}/api/v1/products/default`)
          .send(product)
          .set('Content-Type', 'application/json')
          .set('Jwt', jwt);

    return resp;
}
   

if (!config.url || config.url === '') {
    throw new Error('Key \'url\' must be set in config.json');
} else if (!config.org || config.org === '') {
    throw new Error('Key \'org\' must be set in config.json');
} else if (!config.email || config.email === '') {
    throw new Error('Key \'email\' must be set in config.json');
} else if (!config.password || config.password === '') {
    throw new Error('Key \'password\' must be set in config.json');
}



login(config).then(jwt => {
    products.map((idx, product) => {
        createProduct(config.url, jwt, product, idx)
            .then(resp => {
                console.log(resp);
            })
            .catch(err => {
                console.log(err.response.error.text);
            });
                
    });
});

