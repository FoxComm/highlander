'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react/addons');
const TestUtils = React.addons.TestUtils;
const path = require('path');

describe('Table', function() {
  let TableStore = require(path.resolve('test/acceptance/table/store.js'));
  let TableView = require(path.resolve('src/themes/admin/components/tables/tableview.jsx'));
  let container = null;
  let tableComponent = null;
  let tableNode = null;

  beforeEach(function() {
    container = document.createElement('div');
    TableStore.fetch();
    tableComponent = React.render(
      <TableView
        columns={TableStore.getColumns()}
        rows={TableStore.getState()}
        sort={TableStore.sort.bind(TableStore)}
      />
    , container);
    tableNode = TestUtils.findRenderedDOMComponentWithTag(tableComponent, 'table').getDOMNode();
  });

  afterEach(function(done) {
    React.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should render', function *() {
      //expect(tableNode.tagName).to.equal('DIV');
    expect(tableNode).to.be.instanceof(Object);
    expect(tableNode.querySelectorAll('th').length).to.be.equal(2);
    expect(tableNode.querySelector('th:nth-child(1)').innerHTML).to.be.equal('id');
    expect(tableNode.querySelector('th:nth-child(2)').innerHTML).to.be.equal('text');
    // rows (1 header + 3 data rows)
    expect(tableNode.querySelectorAll('tr').length).to.be.equal(4);
    // row 1: 1, foo
    expect(tableNode.querySelectorAll('tr:nth-child(1) > td').length).to.be.equal(2);
    expect(tableNode.querySelector('tr:nth-child(1) > td:nth-child(1) > div').innerHTML).to.be.equal('1');
    expect(tableNode.querySelector('tr:nth-child(1) > td:nth-child(2) > div').innerHTML).to.be.equal('foo');
    // row 2: 10, buzz
    expect(tableNode.querySelectorAll('tr:nth-child(2) > td').length).to.be.equal(2);
    expect(tableNode.querySelector('tr:nth-child(2) > td:nth-child(1) > div').innerHTML).to.be.equal('10');
    expect(tableNode.querySelector('tr:nth-child(2) > td:nth-child(2) > div').innerHTML).to.be.equal('buzz');
    // row 3: 2, bar
    expect(tableNode.querySelectorAll('tr:nth-child(3) > td').length).to.be.equal(2);
    expect(tableNode.querySelector('tr:nth-child(3) > td:nth-child(1) > div').innerHTML).to.be.equal('2');
    expect(tableNode.querySelector('tr:nth-child(3) > td:nth-child(2) > div').innerHTML).to.be.equal('bar');
  });
});
