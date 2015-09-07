'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react/addons');
const TestUtils = React.addons.TestUtils;
const path = require('path');

const TableStore = require(path.resolve('test/acceptance/table/store.js'));
const TableView = require(path.resolve('src/themes/admin/components/tables/tableview.jsx'));

class TestTable extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      rows: TableStore.getState()
    };
  }

  componentDidMount() {
    TableStore.listenToEvent('change', this);
    TableStore.fetch();
  }

  componentWillUnmount() {
    TableStore.stopListeningToEvent('change', this);
  }

  onChangeTableStore() {
    this.setState({rows: TableStore.getState()});
  }

  render() {
    return (
      <TableView
        columns={TableStore.getColumns()}
        rows={this.state.rows}
        sort={TableStore.sort.bind(TableStore)}
        />
    );
  }
}

describe('Table', function() {
  let container = null;
  let tableComponent = null;
  let tableNode = null;

  beforeEach(function() {
    container = document.createElement('div');
    tableComponent = React.render(<TestTable />, container);
    tableNode = TestUtils.findRenderedDOMComponentWithTag(tableComponent, 'table').getDOMNode();
  });

  afterEach(function(done) {
    React.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should render', function() {
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

  it('should sort', function() {
    // order by id
    let idHeaderNode = tableNode.querySelector('th:nth-child(1)');
    // desc
    React.addons.TestUtils.Simulate.click(idHeaderNode);
    expect(tableNode.querySelector('tr:nth-child(1) > td:nth-child(1) > div').innerHTML).to.be.equal('1');
    expect(tableNode.querySelector('tr:nth-child(2) > td:nth-child(1) > div').innerHTML).to.be.equal('2');
    expect(tableNode.querySelector('tr:nth-child(3) > td:nth-child(1) > div').innerHTML).to.be.equal('10');
    // asc
    React.addons.TestUtils.Simulate.click(idHeaderNode);
    expect(tableNode.querySelector('tr:nth-child(1) > td:nth-child(1) > div').innerHTML).to.be.equal('10');
    expect(tableNode.querySelector('tr:nth-child(2) > td:nth-child(1) > div').innerHTML).to.be.equal('2');
    expect(tableNode.querySelector('tr:nth-child(3) > td:nth-child(1) > div').innerHTML).to.be.equal('1');
    // order by text
    let textHeaderNode = tableNode.querySelector('th:nth-child(2)');
    // desc
    React.addons.TestUtils.Simulate.click(textHeaderNode);
    expect(tableNode.querySelector('tr:nth-child(1) > td:nth-child(2) > div').innerHTML).to.be.equal('bar');
    expect(tableNode.querySelector('tr:nth-child(2) > td:nth-child(2) > div').innerHTML).to.be.equal('buzz');
    expect(tableNode.querySelector('tr:nth-child(3) > td:nth-child(2) > div').innerHTML).to.be.equal('foo');
    // asc
    React.addons.TestUtils.Simulate.click(textHeaderNode);
    expect(tableNode.querySelector('tr:nth-child(1) > td:nth-child(2) > div').innerHTML).to.be.equal('foo');
    expect(tableNode.querySelector('tr:nth-child(2) > td:nth-child(2) > div').innerHTML).to.be.equal('buzz');
    expect(tableNode.querySelector('tr:nth-child(3) > td:nth-child(2) > div').innerHTML).to.be.equal('bar');
  });
});
