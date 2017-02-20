
import catalogRoutes from './routes/catalog';
import customerRoutes from './routes/customers';
import devRoutes from './routes/dev';
import marketingRoutes from './routes/marketing';
import merchandisingRoutes from './routes/merchandising';
import ordersRoutes from './routes/orders';
import settingsRoutes from './routes/settings';
const clc = require('cli-color');

function printRoutes(routes) {
  if (typeof routes == 'object') {
    for (let name in routes) {
      const value = routes[name];
      console.log(clc.green(name));
      console.log(value);
    }
  } else {
    console.log(routes);
  }
}

export function print() {
  console.log(clc.red('----CATALOG-----'));
  printRoutes(catalogRoutes());
  console.log(clc.red('------CUSTOMER----'));
  printRoutes(customerRoutes());
  console.log(clc.red('--------DEV__-------'));
  printRoutes(devRoutes());
  console.log(clc.red('--------MARKETING-----------'));
  printRoutes(marketingRoutes());
  console.log(clc.red('---------MERCHADISING--------'));
  printRoutes(merchandisingRoutes());
  console.log(clc.red('--------ORDERS--------'));
  printRoutes(ordersRoutes())
  console.log(clc.red('---------SETTINGS-------'));
  printRoutes(settingsRoutes());
}


