
import React from 'react';
import ReactTestUtils from 'react-addons-test-utils';

describe('FormField', function() {
  const Dropdown = requireComponent('dropdown/dropdown.jsx');
  let dropdown;

  afterEach(function() {
    if (dropdown) {
      dropdown.unmount();
      dropdown = null;
    }
  });

  it(`should don't fail for empty items`, function *() {
    dropdown = yield renderIntoDocument(
      <Dropdown
        value="someValue"
        items={[]}
      />,
      true
    );

    expect(dropdown.container.querySelector('.fc-dropdown__controls')).to.be.ok
  });
});
