
import React from 'react';
import ShallowTestUtils from 'react-shallow-testutils';

describe('ContentBox', function() {
  const ConfirmationDialog = requireComponent('modal/confirmation-dialog.jsx');

  const defaultProps = {
    body: 'body',
    cancel: 'cancel',
    confirm: 'confirm',
    cancelAction: (f => f),
    confirmAction: (f => f)
  };

  it('should not render if isVisible is falsy', function *() {
    const dialog = ConfirmationDialog({
      ...defaultProps,
      isVisible: false
    });

    expect(ShallowTestUtils.findAllWithClass(dialog, 'fc-modal')).to.be.empty;
  });

  it('should render if isVisible is truly', function *() {
    const dialog = ConfirmationDialog({
      ...defaultProps,
      isVisible: true
    });

    ShallowTestUtils.findWithClass(dialog, 'fc-modal');
  });
});
