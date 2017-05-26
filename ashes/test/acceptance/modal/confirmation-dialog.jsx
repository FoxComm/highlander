
import React from 'react';

describe('ConfirmationDialog', function() {
  const ConfirmationDialog = requireComponent('modal/confirmation-dialog.jsx');

  const defaultProps = {
    body: 'body',
    cancel: 'cancel',
    confirm: 'confirm',
    cancelAction: (f => f),
    confirmAction: (f => f)
  };

  it('should not render if isVisible is falsy', function *() {
    const { container } = yield renderIntoDocument(
      <div><ConfirmationDialog {...defaultProps} isVisible={false} /></div>
    );

    expect(container.querySelector('.fc-modal')).to.be.null;
    container.unmount();
  });

  it('should render if isVisible is truly', function *() {
    const { container } = yield renderIntoDocument(
      <div><ConfirmationDialog {...defaultProps} isVisible={true} /></div>
    );

    expect(container.querySelector('.fc-modal')).to.be.ok;
    container.unmount();
  });
});
