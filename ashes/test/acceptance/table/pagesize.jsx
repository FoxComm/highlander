import React from 'react';
import { noop } from 'lodash';

describe('PageSize', function() {
  const PageSize = requireComponent('table/pagesize.jsx');
  let pageSize;

  afterEach(function() {
    if (pageSize) {
      pageSize.unmount();
      pageSize = null;
    }
  });

  it(`should render selected page size`, function*() {
    pageSize = yield renderIntoDocument(<PageSize setState={noop} value={25} />);

    expect(pageSize.container.querySelector('.dropdown').textContent).to.equal('View 25');
  });
});
