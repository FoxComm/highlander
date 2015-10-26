'use strict';

const path = require('path');

describe('Buttons', function() {
  let Buttons = require(path.resolve('src/components/common/buttons.jsx'));

  it('should render DefaultButton', function() {
    const props = {
      children: 'Done'
    };
    const button = Buttons.DefaultButton(props);

    expect(button).to.be.instanceof(Object);
    expect(button.props.className).to.contain('fc-btn');
    expect(button.props.children).to.contain('Done');
  });

  it('should pass onClick to DefaultButton', function() {
    const props = {
      onClick: () => {return;}
    };
    const button = Buttons.DefaultButton(props);

    expect(button).to.be.instanceof(Object);
    expect(button.props.className).to.contain('fc-btn');
    expect(button.props.onClick).to.equal(props.onClick);
  });

  it('should render icon in EditButton', function() {
    const button = Buttons.EditButton();

    expect(button).to.be.instanceof(Object);
    expect(button.props.icon).to.equal('edit');
  });

  it('should render icon in DecrementButton', function() {
    const button = Buttons.DecrementButton();

    expect(button).to.be.instanceof(Object);
    expect(button.props.icon).to.equal('chevron-down');
  });

  it('should render icon in IncrementButton', function() {
    const button = Buttons.IncrementButton();

    expect(button).to.be.instanceof(Object);
    expect(button.props.icon).to.equal('chevron-up');
  });

  it('should render icon in DeleteButton', function() {
    const button = Buttons.DeleteButton();

    expect(button).to.be.instanceof(Object);
    expect(button.props.icon).to.equal('trash');
  });

  it('should render className in PrimaryButton', function() {
    const button = Buttons.PrimaryButton({});

    expect(button).to.be.instanceof(Object);
    expect(button.props.className).to.equal('fc-btn-primary');
  });
});
