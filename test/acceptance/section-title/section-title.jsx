
import React from 'react';
import ShallowTestUtils from 'react-shallow-testutils';

describe('SectionTitle', function() {
  const SectionTitle = requireComponent('section-title/section-title.jsx');
  const PrimaryButton = requireComponent('common/buttons.jsx').PrimaryButton;

  it('should render', function *() {
    const sectionTitle = SectionTitle({title: 'Orders'});

    expect(sectionTitle.props.className).to.contain('fc-grid');
  });

  it('should not render button if handler is not set', function *() {
    const sectionTitle = SectionTitle({title: 'Orders'});

    const allButtons = ShallowTestUtils.findAllWithType(sectionTitle, PrimaryButton);
    expect(allButtons).to.be.empty;
  });

  it('should render button if handler is set', function *() {
    const sectionTitle = SectionTitle({title: 'Orders', buttonClickHandler: () => {}});

    expect(sectionTitle, 'to contain',
      <PrimaryButton></PrimaryButton>
    );
  });
});
