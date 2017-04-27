'use strict';

const path = require('path');
const GA_TRACKING_ID = process.env.GA_TRACKING_ID;

module.exports = function() {
  if (!GA_TRACKING_ID && process.env.NODE_ENV === 'production') {
    console.warn('WARNING. There is no google analytics tracking id configured.' +
      'Use GA_TRACKING_ID env variable for that.');
  }
  return {
    favicon: path.resolve('public/admin/favicon.ico'),
    publicDir: path.resolve('public'),
    pageConstants: {
      title: 'FoxCommerce',
      analytics: GA_TRACKING_ID,
      appStart: 'App.start();'
    }
  };
};
