
import React from 'react';

describe('Buttons', function() {
  const Buttons = requireComponent('core/button/button.jsx', false);

  it('should render Button', function() {
    const props = {
      children: 'Done'
    };
    const button = Buttons.Button(props);

    expect(button).to.be.instanceof(Object);
    expect(button.props.children, 'to contain', <span className="text">Done</span>);
  });

  it('should pass onClick to Button', function() {
    const props = {
      onClick: () => {return;}
    };
    const button = Buttons.Button(props);

    expect(button).to.be.instanceof(Object);
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
    const button = Buttons.DeleteButton({});

    expect(button).to.be.instanceof(Object);
    expect(button.props.icon).to.equal('trash');
  });

  it('should render className in PrimaryButton', function() {
    const props = {className: 'super-button', onClick: () => {}};
    const button = Buttons.PrimaryButton(props);

    expect(button).to.be.instanceof(Object);
    expect(button.props.className).to.equal('super-button');
    expect(button.props.className).to.equal('primary super-button');
    expect(button.props.onClick).to.equal(props.onClick);
  });
});
