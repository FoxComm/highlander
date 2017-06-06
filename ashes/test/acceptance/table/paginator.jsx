import React from 'react';
import { noop } from 'lodash';

describe('Paginator', function() {
  const Paginator = requireComponent('table/paginator.jsx');
  let paginator;

  afterEach(function() {
    if (paginator) {
      paginator.unmount();
      paginator = null;
    }
  });

  it(`should calc pageCount correctly, 25 size/25 total`, function*() {
    paginator = yield renderIntoDocument(<Paginator setState={noop} total={25} size={25} from={0} />);

    expect(paginator.container.querySelector('.fc-table-paginator__total-pages').textContent).to.equal('1');
  });

  it(`should calc pageCount correctly, 25 size/26 total`, function*() {
    paginator = yield renderIntoDocument(<Paginator setState={noop} total={26} size={25} from={0} />);

    expect(paginator.container.querySelector('.fc-table-paginator__total-pages').textContent).to.equal('2');
  });
});
