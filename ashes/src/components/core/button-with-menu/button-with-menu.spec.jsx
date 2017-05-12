import React from 'react';
import { shallow } from 'enzyme';


console.log(process.env.NODE_PATH);

import ButtonWithMenu from './button-with-menu';

describe('ButtonWithMenu', function () {
  it('should render ButtonWithMenu', function () {
    const button = shallow(
      <ButtonWithMenu
        title="Done"
        items={[
          ['id1', 'Save and Exit'],
          ['id2', 'Save and Duplicate'],
        ]}
      />
    );

    expect(button.find('.button')).to.equal('Done');
    expect(button.find('.menu')).to.have.length(0);

    button.find('.dropdownButton').simulate('click');
    expect(button.find('.menu')).to.have.length(1);
  });
});
