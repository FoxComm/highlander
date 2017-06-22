import React from 'react';
import sinon from 'sinon';

describe('Rmas', function() {
  const Rmas = requireComponent('rmas/rmas.jsx');
  const TableView = requireComponent('table/tableview.jsx');
  const rma = require('../../fixtures/rma.json');

  it('should render', function*() {
    const Wrapped = Rmas.WrappedComponent;
    const props = {
      rmas: {
        rows: [rma],
        total: 1,
        from: 0,
        size: 50,
      },
      fetchRmas: sinon.spy(),
    };
    const rmaList = shallowRender(<Wrapped {...props} />);

    expect(rmaList.props.className).to.equal('fc-list-page');
    expect(rmaList, 'to contain', <TableView data={props.rmas} />);
  });
});
