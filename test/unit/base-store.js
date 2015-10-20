'use strict';

const path = require('path');
const assert = require('assert');
const Immutable = require('immutable');
const sinon = require('sinon');

describe('BaseStore', function () {
  const baseStore = require(path.resolve('src/stores/base-store'));

  beforeEach(function() {
    this.store = new baseStore();

    this.list = Immutable.List([{id: 1, foo: 'bar'}]);
    this.map = Immutable.Map({item1: {id: 1, foo: 'bar'}});
  });

  context('upsert', function() {
    it('should upsert into provided Immutable.List', function(){
      let newItem = {id: 1, foo: 'foo'};

      let newList = this.store.upsert(this.list, newItem, null);

      assert(newList.size === 1);
      assert(newList.first() === newItem);
    });

    it('should upsert into provided Immutable.Map', function(){
      let newItem = {id: 1, foo: 'foo'};

      let newMap = this.store.upsert(this.map, newItem, null);
      
      assert(newMap.size === 1);
      assert(newMap.get('item1').foo === 'foo');
    });

    it('should upsert into Immutable.List with custom predicate', function(){
      let newItem = {id: 1, foo: 'foo'};

      let newList = this.store.upsert(this.list, newItem, null, item => item.foo === 'bar');

      assert(newList.size === 1);
      assert(newList.first() === newItem);
    });

    it('should add new item to Immutable.List when predicate is not found', function() {
      let newItem = {id: 2, foo: 'foo'};

      let newList = this.store.upsert(this.list, newItem, null);

      assert(newList.size === 2);
    });

    it('should throw when predicate is not found for Immutable.Map', function(){
      let newItem = {id: 2, foo: 'foo'};

      expect(this.store.upsert.bind(this, this.map, newItem, null)).to.throw(Error);
    });

    it('should throw when iterator is not a correct type', function(){
      expect(this.store.upsert.bind(this, 'HELLO')).to.throw(Error);
    });
  });
});
