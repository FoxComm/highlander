// ==UserScript==
// @name         unsuppressor
// @namespace    http://tampermonkey.net/
// @version      1.0
// @description  Expand suppressed diffs for you
// @author       anru
// @match        https://github.com/*/pull/*/files
// @grant        none
// ==/UserScript==
/* jshint -W097 */
/* eslint-env greasemonkey, jquery */
'use strict';

function getSupressors() {
  return $('.js-details-container .js-details-target').filter(function() {
    return this.textContent.indexOf('suppressed') != -1;
  });
}

function unsupressAll() {
  console.info('Unsuppress difs...');
  getSupressors().map(function() {
    $(this).click();
  });
}

setTimeout(unsupressAll, 1200);
