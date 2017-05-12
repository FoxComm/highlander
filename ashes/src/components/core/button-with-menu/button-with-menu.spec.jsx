import React from 'react';
import { shallow } from 'enzyme';

describe('ButtonWithMenu', function () {
  const ButtonWithMenu = requireComponent('core/button-with-menu');

  it('should render ButtonWithMenu', function () {
    const button = shallow(
      <ButtonWithMenu title="Done" />
    );

    expect(button.find('.button')).to.equal('Done');
  });
});
