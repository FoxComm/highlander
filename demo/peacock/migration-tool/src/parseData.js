const fs = require('fs');
const xml2js = require('xml2js');
const parseCSV = require('csv-parse');
const _ = require('lodash');

const parser = new xml2js.Parser({
  explicitArray: false
});

function parseCustomers() {
  fs.readFile(__dirname + '/data/xml/CustomerExportMon-08-22-022537.xml', function(err, data) {
    parser.parseString(data, function (err, result) {

      const data = result.ReadCustomerResponse.CustomerList;

      fs.writeFile(__dirname + '/data/customers.json', JSON.stringify(data, null, 2), function(err) {
        if(err) {
          return console.log(err);
        }

        console.log("The Customers were parsed!");
      });

    })
  });
}

function parseProducts() {
  fs.readFile(__dirname + '/data/xml/ProductExportMon-08-22-025152.xml', function(err, data) {
    parser.parseString(data, function (err, result) {

      const data = result.ReadProductResponse.ProductList;

      fs.writeFile(__dirname + '/data/products.json', JSON.stringify(data, null, 2), function(err) {
        if(err) {
          return console.log(err);
        }

        console.log("The Products were parsed!");
      });

    })
  });
}

function parseOrders() {
  fs.readFile(__dirname + '/data/xml/OrderExportMon-08-22-024920.xml', function(err, data) {
    parser.parseString(data, function (err, result) {

      const data = result.ReadOrderResponse.OrderList;

      fs.writeFile(__dirname + '/data/orders.json', JSON.stringify(data, null, 2), function(err) {
        if(err) {
          return console.log(err);
        }

        console.log("The Orders were parsed!");
      });

    })
  });
}

function parseCategories() {
  fs.readFile(__dirname + '/data/csv/CategoryExportFri-08-26-105056.csv', function(err, data) {
    parseCSV(data, {}, function(err, output){
      const data = output;
      const categoryNames = output[0];
      let categories = {};

      output.shift();

      output.map((category) => {
        let newCategory = {};

        category.map((value, i) => {
          newCategory[categoryNames[i]] = value;
        });

        categories[newCategory.CategoryID] = newCategory;
      });

      fs.writeFile(__dirname + '/data/categories.json', JSON.stringify(categories, null, 2), function(err) {
        if(err) {
          return console.log(err);
        }

        console.log("The Categories were parsed!");
      });
    });
  });
}

function parseProductsNew() {
  fs.readFile(__dirname + '/data/csv/TPG_Seed_Product_Data.csv', function(err, data) {
    parseCSV(data, {}, function(err, output){
      const data = output;
      const columns = output[0];

      let products = [];

      output.shift();

      output.map((product, key) => {
        let newProduct = {};

        product.map((value, i) => {
          newProduct[columns[i]] = value;
        });

        newProduct['id'] = _.get(product, "id", key);

        products.push(newProduct);
      });

      fs.writeFile(__dirname + '/data/products_new.json', JSON.stringify(products, null, 2), function(err) {
        if(err) {
          return console.log(err);
        }

        console.log("New Products were parsed!");
      });
    });
  });
}

parseCustomers();
parseProducts();
parseOrders();
parseCategories();
parseProductsNew();