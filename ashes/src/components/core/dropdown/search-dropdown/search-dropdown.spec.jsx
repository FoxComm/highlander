import React from 'react';
import sinon from 'sinon';
import { mount } from 'enzyme';

import SearchDropdown from './search-dropdown';

describe('SearchDropdown', function() {
  // @todo sinon and promises
  it.skip('should set list only for corresponding token', function() {
    const clock = sinon.useFakeTimers();
    const stub = sinon.stub();
    const items = ['one', 'two'];
    const resolver = sinon.spy((resolve, value) => resolve(value));

    function fetch(token) {
      console.log('fetch');
      return {};

      new Promise(function(resolve, reject) {
        setTimeout(() => {
          resolve({ items, token });
          console.log('resolve');
        }, 10);
      });
    }

    for (let i = 0; i < 25; i++) {
      setTimeout(() => console.log(i), i);
    }

    const searchDropdown = mount(<SearchDropdown fetch={fetch} />);

    const spySetState = sinon.spy(searchDropdown, 'setState');

    searchDropdown.find('.pivot').simulate('click');

    searchDropdown.find('.searchBarInput').simulate('change', { target: { value: 'foo' } });
    clock.tick(5);
    searchDropdown.find('.searchBarInput').simulate('change', { target: { value: 'bar' } });
    clock.tick(6); // foo response ended
    console.log('∑∑∑ foo response ended', searchDropdown.state('items').length);
    expect(searchDropdown.state('items').length).to.equal(0); // foo response must be cancelled
    clock.tick(15); // bar response ended
    console.log('∑∑∑ bar response ended');
    console.log(searchDropdown.state('items'));
    expect(searchDropdown.state('items').length).to.equal(2);

    clock.restore();
  });

  it('should open TextInput', function() {
    const searchDropdown = mount(<SearchDropdown />);

    searchDropdown.find('.pivot').simulate('click'); // open dropdown

    expect(searchDropdown.find('.searchBarInput').exists()).to.be.true;
    expect(searchDropdown.find('.searchBarInput').type()).to.equal('input');
  });

  it('should not call fetch method very often', function() {
    const clock = sinon.useFakeTimers();
    const fetch = sinon.spy(() => Promise.resolve([]));
    const searchDropdown = mount(<SearchDropdown fetch={fetch} />);

    searchDropdown.find('.pivot').simulate('click'); // open dropdown
    searchDropdown.find('.searchBarInput').simulate('change', { target: { value: 'foo' } }); // type `foo`
    clock.tick(399);
    searchDropdown.find('.searchBarInput').simulate('change', { target: { value: 'baz' } }); // type `baz`
    clock.tick(399);
    searchDropdown.find('.searchBarInput').simulate('change', { target: { value: 'foo' } }); // type `bar`
    clock.tick(400);

    expect(fetch.calledOnce).to.be.true;
    expect(searchDropdown.state('token')).to.equal('foo');

    clock.restore();
  });

  it('should be spinner if loading', function() {
    const fetch = sinon.spy(() => Promise.resolve([]));
    const searchDropdown = mount(<SearchDropdown fetch={fetch} />);

    searchDropdown.find('.pivot').simulate('click'); // open dropdown
    searchDropdown.find('.searchBarInput').simulate('change', { target: { value: 'foo' } }); // type `foo`

    expect(searchDropdown.state('isLoading')).to.be.true;
    expect(searchDropdown.find('.spinner').exists()).to.be.true;
  });

  it('should not be spinner if loading and there is non-empty result', function() {
    const fetch = sinon.spy(() => Promise.resolve([]));
    const searchDropdown = mount(<SearchDropdown fetch={fetch} items={['one']} />);

    searchDropdown.find('.pivot').simulate('click'); // open dropdown
    searchDropdown.find('.searchBarInput').simulate('change', { target: { value: 'foo' } }); // type `foo`

    expect(searchDropdown.state('isLoading')).to.be.true;
    expect(searchDropdown.find('.spinner').exists()).to.be.false;
  });
});
