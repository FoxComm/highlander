
import React from 'react';
import * as ShallowTestUtils from 'react-shallow-testutils';

describe('SectionTitle', function() {
  const SectionTitle = requireComponent('section-title/section-title.jsx');
  const PrimaryButton = requireComponent('common/buttons.jsx', false).PrimaryButton;

  it('should render', function *() {
    const sectionTitle = SectionTitle({title: 'Orders'});

    expect(sectionTitle.props.className).to.contain('fc-section-title');
  });

  it('should not render button if handler is not set', function *() {
    const sectionTitle = SectionTitle({title: 'Orders'});

    const allButtons = ShallowTestUtils.findAllWithType(sectionTitle, PrimaryButton);
    expect(allButtons).to.be.empty;
  });

  it('should render button if handler is set', function *() {
    const sectionTitle = SectionTitle({title: 'Orders', onAddClick: () => {}});

    expect(sectionTitle, 'to contain',
      <PrimaryButton></PrimaryButton>
    );
  });
});
